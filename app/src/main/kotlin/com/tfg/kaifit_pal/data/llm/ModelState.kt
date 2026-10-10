package com.tfg.kaifit_pal.data.llm

import java.io.File

/** Where the on-device model is: missing, on its way, ready or failed. */
sealed interface ModelState {

    /** Not known yet: the disk and the downloads have not been checked. */
    data object Checking : ModelState

    /** There is no model and nothing is being downloaded or imported. */
    data object NotDownloaded : ModelState

    /**
     * The system is downloading the model.
     *
     * @property downloadedBytes bytes received so far.
     * @property totalBytes size of the whole file.
     * @property waiting why the download is paused, or `null` while it runs.
     */
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long, val waiting: DownloadWait?) : ModelState

    /**
     * A file chosen by the user is being copied into the app.
     *
     * @property copiedBytes bytes copied so far.
     * @property totalBytes size of the chosen file, or `null` when the system does not report it.
     */
    data class Importing(val copiedBytes: Long, val totalBytes: Long?) : ModelState

    /** The verified model file, ready to be loaded. */
    data class Downloaded(val file: File, val sizeBytes: Long) : ModelState

    /** The last download or import failed; it stays until the user dismisses it. */
    data class Failed(val error: ModelError) : ModelState
}

/** Why a download is paused, from the `PAUSED_*` reasons of the system download manager. */
enum class DownloadWait {
    /** Waiting for an unmetered network, because the download is Wi-Fi only. */
    WIFI,

    /** Waiting for any network connection. */
    NETWORK,

    /** Waiting to retry after a network error, or paused for another reason. */
    RETRY,
}

/** What went wrong with the model. */
enum class ModelError {
    /** The device has no 64-bit ABI supported by the inference library. */
    UNSUPPORTED_DEVICE,

    /** Not enough free space for the model and its margin. */
    INSUFFICIENT_STORAGE,

    /** The system could not download the file. */
    DOWNLOAD_FAILED,

    /** The downloaded file does not have the expected size or header. */
    VERIFICATION_FAILED,

    /** The imported file is not a `.litertlm` model. */
    INVALID_FILE,

    /** The chosen file could not be read or copied. */
    IMPORT_FAILED,
}

/**
 * What the device offers for the model, checked before a download.
 *
 * @property totalRamBytes total RAM reported to apps.
 * @property availableStorageBytes free space where the model is stored.
 * @property supports64Bit whether the device has an ABI of the inference library (arm64-v8a or
 *   x86_64).
 */
data class DeviceCheck(
    val totalRamBytes: Long,
    val availableStorageBytes: Long,
    val supports64Bit: Boolean,
) {

    /** Less RAM than recommended: the model may run slowly or be closed by the system. */
    val lowRam: Boolean
        get() = totalRamBytes < ModelSpec.RECOMMENDED_RAM_BYTES

    /** Enough free space for the model plus [ModelSpec.STORAGE_MARGIN_BYTES]. */
    val enoughStorageForDownload: Boolean
        get() = availableStorageBytes >= ModelSpec.SIZE_BYTES + ModelSpec.STORAGE_MARGIN_BYTES

    /** Free space still needed before the download is possible, or 0 when there is enough. */
    val missingStorageBytes: Long
        get() = (ModelSpec.SIZE_BYTES + ModelSpec.STORAGE_MARGIN_BYTES - availableStorageBytes).coerceAtLeast(0)
}
