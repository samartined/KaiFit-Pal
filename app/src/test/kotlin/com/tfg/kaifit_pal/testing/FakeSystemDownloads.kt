package com.tfg.kaifit_pal.testing

import com.tfg.kaifit_pal.data.llm.DownloadSnapshot
import com.tfg.kaifit_pal.data.llm.DownloadStatus
import com.tfg.kaifit_pal.data.llm.ModelDownloadRequest
import com.tfg.kaifit_pal.data.llm.ModelSpec
import com.tfg.kaifit_pal.data.llm.SystemDownloads
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.RandomAccessFile

/**
 * In-memory system download manager. Like the real one, it writes the file at the destination
 * of the request inside [modelsDir], and deletes that file when a download is removed.
 *
 * Finished downloads are sparse files: they report the full size without taking the space.
 */
class FakeSystemDownloads(private val modelsDir: File) : SystemDownloads {

    private class Record(
        val id: Long,
        val url: String,
        val destination: File,
        var status: DownloadStatus,
        var downloadedBytes: Long,
        var totalBytes: Long,
        var reason: Int,
    ) {
        fun snapshot() = DownloadSnapshot(id, status, downloadedBytes, totalBytes, reason)
    }

    private val records = linkedMapOf<Long, Record>()
    private val version = MutableStateFlow(0)
    private var nextId = 1L

    /** Every request queued, in order. */
    val enqueued = mutableListOf<ModelDownloadRequest>()

    /** Ids passed to [remove], in order. */
    val removed = mutableListOf<Long>()

    /** For each call to [remove], whether the file at the download's destination still existed. */
    val destinationExistedOnRemove = mutableListOf<Boolean>()

    /** Id of the last download, queued or added. */
    val lastId: Long
        get() = nextId - 1

    override fun enqueue(request: ModelDownloadRequest): Long {
        enqueued += request
        return add(request.url, File(modelsDir, request.destinationSubPath), DownloadStatus.PENDING, 0, -1, 0)
    }

    override fun query(id: Long): DownloadSnapshot? = records[id]?.snapshot()

    override fun findByUrl(url: String): DownloadSnapshot? =
        records.values.filter { it.url == url }.maxByOrNull { it.id }?.snapshot()

    override fun remove(id: Long) {
        removed += id
        val record = records.remove(id)
        destinationExistedOnRemove += record?.destination?.exists() == true
        record?.destination?.delete()
        changed()
    }

    override fun changes(id: Long): Flow<Unit> = version.map { }

    /** A download of the model that a previous run of the app left in the system. */
    fun addModelDownload(status: DownloadStatus, downloadedBytes: Long, totalBytes: Long = -1, reason: Int = 0): Long =
        add(ModelSpec.DOWNLOAD_URL, partialFile(), status, downloadedBytes, totalBytes, reason)

    /** Changes the download [id] and notifies its observers. */
    fun update(id: Long, status: DownloadStatus, downloadedBytes: Long, totalBytes: Long = -1, reason: Int = 0) {
        val record = records.getValue(id)
        record.status = status
        record.downloadedBytes = downloadedBytes
        record.totalBytes = totalBytes
        record.reason = reason
        changed()
    }

    /** Finishes the download [id] with a file of [sizeBytes] that starts with [header]. */
    fun finish(id: Long, sizeBytes: Long, header: String = ModelSpec.MAGIC) {
        writeSparseFile(records.getValue(id).destination, sizeBytes, header)
        update(id, DownloadStatus.SUCCESSFUL, sizeBytes, sizeBytes)
    }

    /** Fails the download [id] with an `ERROR_*` [reason]. */
    fun fail(id: Long, reason: Int) {
        update(id, DownloadStatus.FAILED, records.getValue(id).downloadedBytes, reason = reason)
    }

    private fun add(
        url: String,
        destination: File,
        status: DownloadStatus,
        downloadedBytes: Long,
        totalBytes: Long,
        reason: Int,
    ): Long {
        val id = nextId++
        records[id] = Record(id, url, destination, status, downloadedBytes, totalBytes, reason)
        changed()
        return id
    }

    private fun partialFile() = File(modelsDir, ModelSpec.FILE_NAME + ModelSpec.PARTIAL_SUFFIX)

    private fun changed() {
        version.update { it + 1 }
    }
}

/** Writes a file of [sizeBytes] that starts with [header] and holds no data blocks after it. */
fun writeSparseFile(file: File, sizeBytes: Long, header: String) {
    file.parentFile?.mkdirs()
    RandomAccessFile(file, "rw").use { raf ->
        raf.setLength(0)
        val bytes = header.toByteArray(Charsets.US_ASCII)
        raf.write(bytes, 0, minOf(bytes.size.toLong(), sizeBytes).toInt())
        raf.setLength(sizeBytes)
    }
}
