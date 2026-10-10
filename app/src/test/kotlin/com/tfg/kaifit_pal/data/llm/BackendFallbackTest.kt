package com.tfg.kaifit_pal.data.llm

import com.tfg.kaifit_pal.data.settings.InferenceBackend
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendFallbackTest {

    private val tried = mutableListOf<InferenceBackend>()

    // BF1
    @Test
    fun workingGpuIsUsedWithoutTryingTheCpu() = runTest {
        val result = loadWithFallback(InferenceBackend.GPU) { backend ->
            tried += backend
            "engine on $backend"
        }

        assertEquals("engine on GPU" to InferenceBackend.GPU, result)
        assertEquals(listOf(InferenceBackend.GPU), tried)
    }

    // BF2
    @Test
    fun gpuLoadFailureFallsBackToTheCpu() = runTest {
        val result = loadWithFallback(InferenceBackend.GPU) { backend ->
            tried += backend
            if (backend == InferenceBackend.GPU) throw LlmException.LoadFailed("Status Code: 13. Message: no OpenCL")
            "engine on $backend"
        }

        assertEquals("engine on CPU" to InferenceBackend.CPU, result)
        assertEquals(listOf(InferenceBackend.GPU, InferenceBackend.CPU), tried)
    }

    // BF3
    @Test
    fun preferredCpuOnlyTriesTheCpu() = runTest {
        val result = loadWithFallback(InferenceBackend.CPU) { backend ->
            tried += backend
            "engine on $backend"
        }

        assertEquals("engine on CPU" to InferenceBackend.CPU, result)
        assertEquals(listOf(InferenceBackend.CPU), tried)
    }

    // BF4
    @Test
    fun unsupportedDeviceOnTheGpuIsThrownWithoutTryingTheCpu() = runTest {
        val error = runCatching {
            loadWithFallback(InferenceBackend.GPU) { backend ->
                tried += backend
                throw LlmException.UnsupportedDevice("no arm64-v8a library")
            }
        }.exceptionOrNull()

        assertTrue(error is LlmException.UnsupportedDevice)
        assertEquals(listOf(InferenceBackend.GPU), tried)
    }

    // BF4
    @Test
    fun whenGpuAndCpuBothFailTheCpuErrorIsThrown() = runTest {
        val error = runCatching {
            loadWithFallback(InferenceBackend.GPU) { backend ->
                tried += backend
                throw LlmException.LoadFailed("failed on $backend")
            }
        }.exceptionOrNull()

        assertTrue(error is LlmException.LoadFailed)
        assertEquals("failed on CPU", error?.message)
        assertEquals(listOf(InferenceBackend.GPU, InferenceBackend.CPU), tried)
    }
}
