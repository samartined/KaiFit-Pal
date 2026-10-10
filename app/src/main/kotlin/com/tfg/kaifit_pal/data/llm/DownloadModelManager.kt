package com.tfg.kaifit_pal.data.llm

import android.app.DownloadManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * [ModelManager] that downloads the model with the system download manager into [modelsDir]
 * and imports files picked by the user into the same folder.
 *
 * Nothing about the model is stored apart from the files themselves:
 * - the system downloads to `FILE_NAME.part`, and an import copies to `FILE_NAME.import`;
 * - a finished file is checked and then renamed to [ModelSpec.FILE_NAME] in one atomic step, so
 *   that file only exists once it is complete and valid;
 * - after a restart, [refresh] finds this app's download of [ModelSpec.DOWNLOAD_URL] again in
 *   the system download manager, so a running download is resumed and never queued twice.
 *
 * A download is checked by its exact size ([ModelSpec.SIZE_BYTES]) and its header
 * ([ModelSpec.MAGIC]). An import only needs the header, so that another revision or variant of
 * the model can be used.
 *
 * @param scope where downloads are watched and imports run; it must outlive the screens.
 * @param ioDispatcher dispatcher for disk and download manager work.
 * @param downloadTitle title of the system download notification.
 * @param downloadDescription text of the system download notification.
 */
class DownloadModelManager(
    private val modelsDir: File,
    private val downloads: SystemDownloads,
    private val deviceInfo: DeviceInfo,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val downloadTitle: String,
    private val downloadDescription: String,
) : ModelManager {

    private val _state = MutableStateFlow<ModelState>(ModelState.Checking)
    override val state: StateFlow<ModelState> = _state.asStateFlow()

    /** Guards every state change and the fields below. Never held while copying or joining a job. */
    private val mutex = Mutex()
    private var downloadId: Long? = null
    private var watchJob: Job? = null
    private var importJob: Job? = null

    private val modelFile: File
        get() = File(modelsDir, ModelSpec.FILE_NAME)
    private val partialFile: File
        get() = File(modelsDir, ModelSpec.FILE_NAME + ModelSpec.PARTIAL_SUFFIX)
    private val importFile: File
        get() = File(modelsDir, ModelSpec.FILE_NAME + ModelSpec.IMPORT_SUFFIX)

    override suspend fun refresh() {
        withContext(ioDispatcher) {
            mutex.withLock {
                if (_state.value !is ModelState.Failed) deriveStateLocked()
            }
        }
    }

    override suspend fun deviceCheck(): DeviceCheck = withContext(ioDispatcher) {
        DeviceCheck(
            totalRamBytes = deviceInfo.totalRamBytes(),
            availableStorageBytes = availableBytes(),
            supports64Bit = deviceInfo.supports64Bit(),
        )
    }

    override suspend fun startDownload(wifiOnly: Boolean) {
        withContext(ioDispatcher) {
            mutex.withLock {
                // Never queue a second download because the first refresh has not run yet.
                if (_state.value == ModelState.Checking) deriveStateLocked()
                if (_state.value.isBusyOrDone()) return@withLock
                if (!deviceInfo.supports64Bit()) {
                    _state.value = ModelState.Failed(ModelError.UNSUPPORTED_DEVICE)
                    return@withLock
                }
                if (availableBytes() < ModelSpec.SIZE_BYTES + ModelSpec.STORAGE_MARGIN_BYTES) {
                    _state.value = ModelState.Failed(ModelError.INSUFFICIENT_STORAGE)
                    return@withLock
                }
                try {
                    enqueueLocked(wifiOnly)
                } catch (e: IOException) {
                    _state.value = ModelState.Failed(ModelError.DOWNLOAD_FAILED)
                } catch (e: SecurityException) {
                    _state.value = ModelState.Failed(ModelError.DOWNLOAD_FAILED)
                } catch (e: IllegalArgumentException) {
                    _state.value = ModelState.Failed(ModelError.DOWNLOAD_FAILED)
                }
            }
        }
    }

    override suspend fun importModel(source: ImportSource) {
        withContext(ioDispatcher) {
            val job = mutex.withLock {
                if (_state.value == ModelState.Checking) deriveStateLocked()
                if (_state.value.isBusyOrDone()) return@withLock null
                if (!deviceInfo.supports64Bit()) {
                    _state.value = ModelState.Failed(ModelError.UNSUPPORTED_DEVICE)
                    return@withLock null
                }
                _state.value = ModelState.Importing(copiedBytes = 0, totalBytes = null)
                scope.launch(ioDispatcher) { copyIntoModelsDir(source) }.also { importJob = it }
            }
            job?.join()
        }
    }

    override suspend fun cancel() {
        withContext(ioDispatcher) {
            val runningImport = mutex.withLock {
                when (_state.value) {
                    is ModelState.Downloading -> {
                        cancelDownloadLocked()
                        _state.value = ModelState.NotDownloaded
                        null
                    }
                    // The copy deletes its file when it stops; the state is derived after that.
                    is ModelState.Importing -> importJob.also { importJob = null }
                    else -> null
                }
            } ?: return@withContext
            runningImport.cancelAndJoin()
            mutex.withLock { deriveStateLocked() }
        }
    }

    override suspend fun delete() {
        withContext(ioDispatcher) {
            val runningImport = mutex.withLock {
                if (_state.value is ModelState.Downloading) cancelDownloadLocked()
                importJob.also { importJob = null }
            }
            runningImport?.cancelAndJoin()
            mutex.withLock {
                modelFile.delete()
                partialFile.delete()
                importFile.delete()
                deriveStateLocked()
            }
        }
    }

    override fun dismissError() {
        if (_state.value !is ModelState.Failed) return
        scope.launch(ioDispatcher) {
            mutex.withLock {
                if (_state.value is ModelState.Failed) deriveStateLocked()
            }
        }
    }

    /**
     * Sets the state from what exists now: an import of this process, the model file, the
     * system download of the model, or nothing.
     */
    private fun deriveStateLocked() {
        if (importJob?.isActive == true) return
        if (modelFile.isFile) {
            stopWatchingLocked()
            _state.value = ModelState.Downloaded(modelFile, modelFile.length())
            return
        }
        val snapshot = try {
            downloads.findByUrl(ModelSpec.DOWNLOAD_URL)
        } catch (e: SecurityException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
        if (snapshot == null) {
            stopWatchingLocked()
            _state.value = ModelState.NotDownloaded
        } else {
            applySnapshotLocked(snapshot)
        }
    }

    private fun enqueueLocked(wifiOnly: Boolean) {
        modelsDir.mkdirs()
        // The system refuses to download over an existing file.
        partialFile.delete()
        val id = downloads.enqueue(
            ModelDownloadRequest(
                url = ModelSpec.DOWNLOAD_URL,
                destinationSubPath = ModelSpec.FILE_NAME + ModelSpec.PARTIAL_SUFFIX,
                allowedOverMetered = !wifiOnly,
                title = downloadTitle,
                description = downloadDescription,
            ),
        )
        _state.value = ModelState.Downloading(downloadedBytes = 0, totalBytes = ModelSpec.SIZE_BYTES, waiting = null)
        watchLocked(id)
    }

    private fun watchLocked(id: Long) {
        if (downloadId == id && watchJob?.isActive == true) return
        watchJob?.cancel()
        downloadId = id
        watchJob = scope.launch(ioDispatcher) {
            downloads.changes(id).collect { onDownloadChanged(id) }
        }
    }

    private fun stopWatchingLocked() {
        watchJob?.cancel()
        watchJob = null
        downloadId = null
    }

    private suspend fun onDownloadChanged(id: Long) {
        mutex.withLock {
            if (downloadId != id) return@withLock
            val snapshot = try {
                downloads.query(id)
            } catch (e: SecurityException) {
                return@withLock
            } catch (e: IllegalArgumentException) {
                return@withLock
            }
            if (snapshot == null) {
                // Removed outside the app, for example from the system Downloads app.
                stopWatchingLocked()
                partialFile.delete()
                _state.value = ModelState.NotDownloaded
            } else {
                applySnapshotLocked(snapshot)
            }
        }
    }

    private fun applySnapshotLocked(snapshot: DownloadSnapshot) {
        when (snapshot.status) {
            DownloadStatus.PENDING, DownloadStatus.RUNNING, DownloadStatus.PAUSED -> {
                _state.value = snapshot.toDownloadingState()
                watchLocked(snapshot.id)
            }

            DownloadStatus.SUCCESSFUL -> finishDownloadLocked(snapshot.id)

            DownloadStatus.FAILED -> {
                stopWatchingLocked()
                removeDownloadLocked(snapshot.id)
                partialFile.delete()
                _state.value = ModelState.Failed(
                    if (snapshot.reason == DownloadManager.ERROR_INSUFFICIENT_SPACE) {
                        ModelError.INSUFFICIENT_STORAGE
                    } else {
                        ModelError.DOWNLOAD_FAILED
                    },
                )
            }
        }
    }

    /** Verifies the finished download and moves it to its final name. */
    private fun finishDownloadLocked(id: Long) {
        stopWatchingLocked()
        val partial = partialFile
        if (!(partial.length() == ModelSpec.SIZE_BYTES && startsWithMagic(partial))) {
            partial.delete()
            removeDownloadLocked(id)
            _state.value = ModelState.Failed(ModelError.VERIFICATION_FAILED)
            return
        }
        // Rename before removing the download: removing it deletes the file at its destination.
        if (!partial.renameTo(modelFile)) {
            partial.delete()
            removeDownloadLocked(id)
            _state.value = ModelState.Failed(ModelError.DOWNLOAD_FAILED)
            return
        }
        removeDownloadLocked(id)
        _state.value = ModelState.Downloaded(modelFile, modelFile.length())
    }

    private fun cancelDownloadLocked() {
        val id = downloadId
        stopWatchingLocked()
        if (id != null) removeDownloadLocked(id)
        partialFile.delete()
    }

    private fun removeDownloadLocked(id: Long) {
        try {
            downloads.remove(id)
        } catch (e: SecurityException) {
            // Nothing else to do: the download is forgotten either way.
        } catch (e: IllegalArgumentException) {
            // Same: the system no longer has it.
        }
    }

    /**
     * Body of the import job: copies [source] to the `.import` file, checking the header as soon
     * as it arrives, and renames it to the model file. The copy is deleted unless it becomes
     * the model, also when the job is cancelled; cancellation is not caught.
     */
    private suspend fun copyIntoModelsDir(source: ImportSource) {
        val copy = importFile
        var becameModel = false
        try {
            val totalBytes = source.sizeBytes
            if (totalBytes != null && availableBytes() < totalBytes + ModelSpec.STORAGE_MARGIN_BYTES) {
                endImport(ModelState.Failed(ModelError.INSUFFICIENT_STORAGE))
                return
            }
            modelsDir.mkdirs()
            _state.value = ModelState.Importing(copiedBytes = 0, totalBytes = totalBytes)
            val isModel = source.open().use { input ->
                FileOutputStream(copy).use { output -> copyCheckingHeader(input, output, totalBytes) }
            }
            if (!isModel) {
                endImport(ModelState.Failed(ModelError.INVALID_FILE))
                return
            }
            mutex.withLock {
                if (importJob !== currentCoroutineContext().job) return
                importJob = null
                if (copy.renameTo(modelFile)) {
                    becameModel = true
                    _state.value = ModelState.Downloaded(modelFile, modelFile.length())
                } else {
                    _state.value = ModelState.Failed(ModelError.IMPORT_FAILED)
                }
            }
        } catch (e: IOException) {
            endImport(ModelState.Failed(ModelError.IMPORT_FAILED))
        } catch (e: SecurityException) {
            endImport(ModelState.Failed(ModelError.IMPORT_FAILED))
        } finally {
            if (!becameModel) copy.delete()
        }
    }

    /** Ends the import with [state], unless it was cancelled meanwhile. */
    private suspend fun endImport(state: ModelState) {
        mutex.withLock {
            if (importJob === currentCoroutineContext().job) {
                importJob = null
                _state.value = state
            }
        }
    }

    /**
     * Copies [input] to [output] in 1 MiB blocks, reporting progress every 1 % of [totalBytes]
     * (every 8 MiB when the size is unknown). Returns `false`, before copying the rest, as soon as
     * the first bytes are not [ModelSpec.MAGIC], or if the file is shorter than that.
     */
    private suspend fun copyCheckingHeader(input: InputStream, output: OutputStream, totalBytes: Long?): Boolean {
        val buffer = ByteArray(COPY_BUFFER_BYTES)
        val header = ByteArray(MAGIC_BYTES.size)
        var headerBytes = 0
        var copied = 0L
        var reported = 0L
        val step = if (totalBytes != null && totalBytes > 0) (totalBytes / 100).coerceAtLeast(1) else UNKNOWN_SIZE_STEP_BYTES
        while (true) {
            currentCoroutineContext().ensureActive()
            val read = input.read(buffer)
            if (read < 0) break
            if (headerBytes < header.size) {
                val count = minOf(read, header.size - headerBytes)
                buffer.copyInto(header, destinationOffset = headerBytes, startIndex = 0, endIndex = count)
                headerBytes += count
                if (headerBytes == header.size && !header.contentEquals(MAGIC_BYTES)) return false
            }
            output.write(buffer, 0, read)
            copied += read
            if (copied - reported >= step) {
                reported = copied
                _state.value = ModelState.Importing(copiedBytes = copied, totalBytes = totalBytes)
            }
        }
        return headerBytes == header.size
    }

    private fun startsWithMagic(file: File): Boolean = try {
        file.inputStream().use { input ->
            val header = ByteArray(MAGIC_BYTES.size)
            var read = 0
            while (read < header.size) {
                val count = input.read(header, read, header.size - read)
                if (count < 0) break
                read += count
            }
            read == header.size && header.contentEquals(MAGIC_BYTES)
        }
    } catch (e: IOException) {
        false
    }

    private fun availableBytes(): Long = try {
        deviceInfo.availableBytes(modelsDir)
    } catch (e: IllegalArgumentException) {
        0
    }

    private fun ModelState.isBusyOrDone(): Boolean =
        this is ModelState.Downloading || this is ModelState.Importing || this is ModelState.Downloaded

    private fun DownloadSnapshot.toDownloadingState() = ModelState.Downloading(
        downloadedBytes = downloadedBytes.coerceAtLeast(0),
        totalBytes = if (totalBytes > 0) totalBytes else ModelSpec.SIZE_BYTES,
        waiting = if (status == DownloadStatus.PAUSED) {
            when (reason) {
                DownloadManager.PAUSED_QUEUED_FOR_WIFI -> DownloadWait.WIFI
                DownloadManager.PAUSED_WAITING_FOR_NETWORK -> DownloadWait.NETWORK
                else -> DownloadWait.RETRY
            }
        } else {
            null
        },
    )

    private companion object {
        const val COPY_BUFFER_BYTES = 1024 * 1024
        const val UNKNOWN_SIZE_STEP_BYTES = 8L * 1024 * 1024
        val MAGIC_BYTES: ByteArray = ModelSpec.MAGIC.toByteArray(Charsets.US_ASCII)
    }
}
