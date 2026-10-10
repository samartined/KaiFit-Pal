package com.tfg.kaifit_pal.data.llm

/**
 * The on-device language model of Kai-Q, Gemma 4 E2B in the LiteRT-LM format, and where it lives.
 *
 * The download URL is pinned to one revision of the model repository, so its content never
 * changes: that is what makes the exact size a valid check of a finished download.
 */
object ModelSpec {

    /** Name of the verified model file inside [MODELS_DIR]. It only exists once it is complete. */
    const val FILE_NAME = "gemma-4-E2B-it.litertlm"

    /** Generic (CPU and GPU) Gemma 4 E2B file, at a fixed revision of the model repository. */
    const val DOWNLOAD_URL =
        "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/" +
            "b3ca0d2f076785a8f4b2219ddbd2bdb99954eae1/gemma-4-E2B-it.litertlm"

    /** Exact size of the file at [DOWNLOAD_URL], in bytes (about 2.6 GB). */
    const val SIZE_BYTES = 2_588_147_712L

    /** First 8 bytes, in ASCII, of every `.litertlm` file. */
    const val MAGIC = "LITERTLM"

    /** Folder of the model inside the app's external files directory. */
    const val MODELS_DIR = "models"

    /** Suffix of the file the system writes while downloading. */
    const val PARTIAL_SUFFIX = ".part"

    /** Suffix of the copy written while importing a file chosen by the user. */
    const val IMPORT_SUFFIX = ".import"

    /** Free space required on top of the model itself before downloading or importing it. */
    const val STORAGE_MARGIN_BYTES = 500_000_000L

    /**
     * Total RAM below which the device gets a warning (not a block). A phone sold with 6 GB
     * reports about 5.6 to 5.8 GB to apps.
     */
    const val RECOMMENDED_RAM_BYTES = 5_500_000_000L
}
