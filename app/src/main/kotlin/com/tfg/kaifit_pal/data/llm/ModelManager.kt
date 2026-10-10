package com.tfg.kaifit_pal.data.llm

import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream

/**
 * Gets the on-device model onto the device and keeps track of it: download, import, cancel and
 * delete. The state is derived from the disk and the system downloads, never stored apart.
 */
interface ModelManager {

    /** Current state; [ModelState.Checking] until the first [refresh]. */
    val state: StateFlow<ModelState>

    /**
     * Derives the state again from the disk and the system downloads, and resumes watching a
     * download that is still running. A [ModelState.Failed] state stays until [dismissError].
     */
    suspend fun refresh()

    /** RAM, free space and ABI of the device, to show before a download. */
    suspend fun deviceCheck(): DeviceCheck

    /**
     * Starts downloading the model, unless it is already downloading, importing or downloaded.
     *
     * @param wifiOnly whether the download must wait for an unmetered network.
     */
    suspend fun startDownload(wifiOnly: Boolean)

    /**
     * Copies a `.litertlm` file chosen by the user into the app, and returns when the copy ends.
     * The copy belongs to the manager: it goes on if the caller is cancelled.
     */
    suspend fun importModel(source: ImportSource)

    /** Cancels a download or an import in progress; the state becomes [ModelState.NotDownloaded]. */
    suspend fun cancel()

    /**
     * Removes the model file and any partial one. It does not close the engine that may have the
     * file open: the caller must close it first.
     */
    suspend fun delete()

    /** Leaves [ModelState.Failed] and derives the state again. */
    fun dismissError()
}

/** A file the user picked to import, such as a document of the system file picker. */
interface ImportSource {

    /** File name, when known. */
    val displayName: String?

    /** File size in bytes, when known. */
    val sizeBytes: Long?

    /** Opens the file for reading; the caller closes the stream. */
    fun open(): InputStream
}
