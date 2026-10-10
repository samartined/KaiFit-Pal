package com.tfg.kaifit_pal.data.llm

import android.app.DownloadManager
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import java.io.IOException

/**
 * The few operations of the system download manager that the model needs, behind an interface
 * so that the model manager can be tested on the JVM.
 */
interface SystemDownloads {

    /**
     * Queues a download and returns its id.
     *
     * @throws IOException if the destination cannot be used.
     * @throws IllegalArgumentException if the request is rejected.
     */
    fun enqueue(request: ModelDownloadRequest): Long

    /** The download with this id, or `null` if the system no longer has it. */
    fun query(id: Long): DownloadSnapshot?

    /** The newest download of this app from [url], in any status, or `null`. */
    fun findByUrl(url: String): DownloadSnapshot?

    /** Forgets the download; the system also deletes the file at its destination. */
    fun remove(id: Long)

    /** Emits once when collected and then every time the download changes. */
    fun changes(id: Long): Flow<Unit>
}

/**
 * A download to queue.
 *
 * @property destinationSubPath file name inside the models folder of the external files directory.
 * @property allowedOverMetered whether mobile data may be used.
 * @property title title of the system notification.
 * @property description text of the system notification.
 */
data class ModelDownloadRequest(
    val url: String,
    val destinationSubPath: String,
    val allowedOverMetered: Boolean,
    val title: String,
    val description: String,
)

/**
 * A download as the system reports it.
 *
 * @property totalBytes size of the file, or -1 while the server has not reported it.
 * @property reason `PAUSED_*` or `ERROR_*` constant of [DownloadManager], when the status has one.
 */
data class DownloadSnapshot(
    val id: Long,
    val status: DownloadStatus,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val reason: Int,
)

/** Status of a download, as in the `STATUS_*` constants of [DownloadManager]. */
enum class DownloadStatus { PENDING, RUNNING, PAUSED, SUCCESSFUL, FAILED }

/**
 * [SystemDownloads] on the [DownloadManager] of the device.
 *
 * Downloads go to the `models` folder of the app's external files directory, which needs no
 * storage permission, and show a system notification while they run and when they finish.
 */
class AndroidSystemDownloads(context: Context) : SystemDownloads {

    private val appContext: Context = context.applicationContext
    private val downloadManager: DownloadManager = appContext.getSystemService(DownloadManager::class.java)

    override fun enqueue(request: ModelDownloadRequest): Long {
        val systemRequest = try {
            DownloadManager.Request(request.url.toUri())
                .setDestinationInExternalFilesDir(appContext, ModelSpec.MODELS_DIR, request.destinationSubPath)
        } catch (e: IllegalStateException) {
            // Thrown when the external storage is not available.
            throw IOException("External files directory unavailable", e)
        }
        systemRequest
            .setAllowedOverMetered(request.allowedOverMetered)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setTitle(request.title)
            .setDescription(request.description)
        return downloadManager.enqueue(systemRequest)
    }

    override fun query(id: Long): DownloadSnapshot? =
        downloadManager.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
            if (cursor.moveToFirst()) cursor.toSnapshot() else null
        }

    override fun findByUrl(url: String): DownloadSnapshot? =
        downloadManager.query(DownloadManager.Query())?.use { cursor ->
            val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI)
            if (uriIndex < 0) return@use null
            var newest: DownloadSnapshot? = null
            while (cursor.moveToNext()) {
                if (cursor.getString(uriIndex) == url) {
                    val snapshot = cursor.toSnapshot()
                    if (snapshot != null && (newest == null || snapshot.id > newest.id)) newest = snapshot
                }
            }
            newest
        }

    override fun remove(id: Long) {
        downloadManager.remove(id)
    }

    override fun changes(id: Long): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        val uri = ContentUris.withAppendedId(MY_DOWNLOADS_URI, id)
        appContext.contentResolver.registerContentObserver(uri, false, observer)
        // A change between the caller's last query and the registration would be lost otherwise.
        trySend(Unit)
        awaitClose { appContext.contentResolver.unregisterContentObserver(observer) }
    }.conflate()

    private fun Cursor.toSnapshot(): DownloadSnapshot? {
        val idIndex = getColumnIndex(DownloadManager.COLUMN_ID)
        val statusIndex = getColumnIndex(DownloadManager.COLUMN_STATUS)
        if (idIndex < 0 || statusIndex < 0) return null
        val status = when (getInt(statusIndex)) {
            DownloadManager.STATUS_PENDING -> DownloadStatus.PENDING
            DownloadManager.STATUS_RUNNING -> DownloadStatus.RUNNING
            DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
            DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.SUCCESSFUL
            DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
            else -> return null
        }
        return DownloadSnapshot(
            id = getLong(idIndex),
            status = status,
            downloadedBytes = longOrDefault(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR, 0),
            totalBytes = longOrDefault(DownloadManager.COLUMN_TOTAL_SIZE_BYTES, -1),
            reason = if (status == DownloadStatus.PAUSED || status == DownloadStatus.FAILED) {
                longOrDefault(DownloadManager.COLUMN_REASON, 0).toInt()
            } else {
                0
            },
        )
    }

    private fun Cursor.longOrDefault(column: String, default: Long): Long {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getLong(index) else default
    }

    private companion object {
        /** Rows of the downloads of the calling app; each download is this URI plus its id. */
        val MY_DOWNLOADS_URI: Uri = "content://downloads/my_downloads".toUri()
    }
}
