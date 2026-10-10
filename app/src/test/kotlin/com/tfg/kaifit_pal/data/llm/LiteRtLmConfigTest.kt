package com.tfg.kaifit_pal.data.llm

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.LiteRtLmJniException
import com.google.ai.edge.litertlm.SamplerConfig
import com.tfg.kaifit_pal.data.settings.InferenceBackend
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The real LiteRT-LM settings and error mapping, on the JVM. The native library does not load
 * here, so nothing calls the engine: only its configuration classes are built.
 */
class LiteRtLmConfigTest {

    @get:Rule
    val tmp = TemporaryFolder()

    // LC1
    @Test
    fun gpuEngineUsesTheGpuWithoutVisionOrAudioAndA4096TokenContext() {
        val config = engineConfig("/models/gemma-4-E2B-it.litertlm", InferenceBackend.GPU, "/cache")

        assertEquals("/models/gemma-4-E2B-it.litertlm", config.modelPath)
        assertTrue(config.backend is Backend.GPU)
        assertNull(config.visionBackend)
        assertNull(config.audioBackend)
        assertEquals(4096, config.maxNumTokens)
        assertEquals("/cache", config.cacheDir)
    }

    // LC1
    @Test
    fun cpuEngineUsesTheCpu() {
        val config = engineConfig("/models/gemma-4-E2B-it.litertlm", InferenceBackend.CPU, "/cache")

        assertTrue(config.backend is Backend.CPU)
        assertNull(config.visionBackend)
        assertNull(config.audioBackend)
    }

    // LC2
    @Test
    fun conversationHasTheSystemPromptTheSamplerAndTheReplyLimitWithoutThinking() {
        val config = conversationConfig("p")

        assertEquals("p", config.systemInstruction.toString())
        assertEquals(SamplerConfig(40, 0.95, 0.3), config.samplerConfig)
        assertEquals(1024, config.maxOutputToken)
        assertNull(config.thinkingConfig)
        assertTrue(config.extraContext.isEmpty())
    }

    // LC3
    @Test
    fun engineErrorsKeepTheirMessageWithoutTheNativeTrace() {
        val native = LiteRtLmJniException("Status Code: 13. Message: x\n=== Source Location Trace ===\nengine.cc:42")

        val onLoad = native.toLlmException()
        val onGeneration = native.toLlmException(duringGeneration = true)

        assertTrue(onLoad is LlmException.LoadFailed)
        assertEquals("Status Code: 13. Message: x", onLoad.message)
        assertTrue(onGeneration is LlmException.GenerationFailed)
        assertEquals("Status Code: 13. Message: x", onGeneration.message)
    }

    // LC3
    @Test
    fun missingNativeLibraryIsAnUnsupportedDeviceAndMisuseIsNotReady() {
        assertTrue(UnsatisfiedLinkError("Failed to load native library litertlm_jni").toLlmException() is LlmException.UnsupportedDevice)
        assertTrue(NoClassDefFoundError("Could not initialize class LiteRtLmJni").toLlmException() is LlmException.UnsupportedDevice)
        assertTrue(IllegalStateException("Conversation is not alive.").toLlmException() is LlmException.NotReady)
    }

    @Test
    fun repositoryWithoutEngineIsNotReadyAndClosesTwiceSafely() = runTest {
        val repository = LiteRtLmRepository(tmp.newFolder("cache"), StandardTestDispatcher(testScheduler))

        val startError = runCatching { repository.startConversation("p") }.exceptionOrNull()
        val sendError = runCatching { repository.sendMessage("hola").collect() }.exceptionOrNull()
        repository.close()
        repository.close()

        assertTrue(startError is LlmException.NotReady)
        assertTrue(sendError is LlmException.NotReady)
        assertEquals(0, repository.usedTokens())
        assertFalse(repository.isLoaded.value)
    }
}
