package com.tfg.kaifit_pal.data.llm

import com.tfg.kaifit_pal.data.settings.InferenceBackend

/**
 * Runs [load] on the [preferred] backend and returns its result with the backend that worked.
 *
 * - With the GPU preferred, a [LlmException.LoadFailed] on the GPU (no OpenCL, a driver error)
 *   is followed by a second try on the CPU. If that one fails too, its exception is thrown.
 * - With the CPU preferred, only the CPU is tried.
 * - [LlmException.UnsupportedDevice] means no backend can work, so it is thrown at once.
 */
internal suspend fun <T> loadWithFallback(
    preferred: InferenceBackend,
    load: suspend (InferenceBackend) -> T,
): Pair<T, InferenceBackend> {
    if (preferred == InferenceBackend.GPU) {
        try {
            return load(InferenceBackend.GPU) to InferenceBackend.GPU
        } catch (e: LlmException.LoadFailed) {
            // The GPU cannot run the model here: the CPU is tried next.
        }
    }
    return load(InferenceBackend.CPU) to InferenceBackend.CPU
}
