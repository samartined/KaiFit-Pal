package com.tfg.kaifit_pal.data.llm

import android.app.DownloadManager
import com.tfg.kaifit_pal.testing.FakeDeviceInfo
import com.tfg.kaifit_pal.testing.FakeImportSource
import com.tfg.kaifit_pal.testing.FakeSystemDownloads
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The model manager against an in-memory download manager and device. Finished downloads are
 * sparse files of the real model size, so nothing is downloaded and almost no space is used.
 *
 * The manager watches downloads in `backgroundScope`, whose work `advanceUntilIdle()` skips, so
 * the tests let it run with `runCurrent()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DownloadModelManagerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var modelsDir: File
    private lateinit var downloads: FakeSystemDownloads
    private val device = FakeDeviceInfo()

    private val modelFile: File
        get() = File(modelsDir, "gemma-4-E2B-it.litertlm")
    private val partialFile: File
        get() = File(modelsDir, "gemma-4-E2B-it.litertlm.part")
    private val importFile: File
        get() = File(modelsDir, "gemma-4-E2B-it.litertlm.import")

    @Before
    fun setUp() {
        modelsDir = tmp.newFolder("models")
        downloads = FakeSystemDownloads(modelsDir)
    }

    private fun TestScope.newManager(
        ioDispatcher: CoroutineDispatcher = StandardTestDispatcher(testScheduler),
    ) = DownloadModelManager(
        modelsDir = modelsDir,
        downloads = downloads,
        deviceInfo = device,
        scope = backgroundScope,
        ioDispatcher = ioDispatcher,
        downloadTitle = "Modelo de Kai-Q",
        downloadDescription = "Gemma 4 E2B",
    )

    /** A manager on which a download has been queued and observed; returns its id. */
    private suspend fun TestScope.startedDownload(manager: DownloadModelManager): Long {
        manager.refresh()
        manager.startDownload(wifiOnly = true)
        runCurrent()
        return downloads.lastId
    }

    // MM1
    @Test
    fun refreshWithoutFileOrDownloadIsNotDownloaded() = runTest {
        val manager = newManager()
        assertEquals(ModelState.Checking, manager.state.value)

        manager.refresh()

        assertEquals(ModelState.NotDownloaded, manager.state.value)
    }

    // MM2
    @Test
    fun refreshWithTheModelFileIsDownloadedWithItsSize() = runTest {
        modelFile.writeBytes(FakeImportSource.modelBytes(1_000))
        val manager = newManager()

        manager.refresh()

        assertEquals(ModelState.Downloaded(modelFile, 1_008), manager.state.value)
    }

    // MM3
    @Test
    fun wifiOnlyDownloadIsQueuedOnceFromThePinnedRevisionToThePartialFile() = runTest {
        val manager = newManager()
        manager.refresh()

        manager.startDownload(wifiOnly = true)
        runCurrent()

        val request = downloads.enqueued.single()
        assertEquals(
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/" +
                "b3ca0d2f076785a8f4b2219ddbd2bdb99954eae1/gemma-4-E2B-it.litertlm",
            request.url,
        )
        assertEquals("gemma-4-E2B-it.litertlm.part", request.destinationSubPath)
        assertFalse(request.allowedOverMetered)
        assertEquals("Modelo de Kai-Q", request.title)
        assertEquals(ModelState.Downloading(0, 2_588_147_712, null), manager.state.value)
    }

    // MM3
    @Test
    fun downloadWithoutWifiOnlyMayUseMobileData() = runTest {
        val manager = newManager()
        manager.refresh()

        manager.startDownload(wifiOnly = false)
        runCurrent()

        assertTrue(downloads.enqueued.single().allowedOverMetered)
        assertEquals(ModelState.Downloading(0, 2_588_147_712, null), manager.state.value)
    }

    // MM4
    @Test
    fun startDownloadWithoutEnoughSpaceFailsWithoutQueueing() = runTest {
        device.freeBytes = 3_088_147_711
        val manager = newManager()
        manager.refresh()

        manager.startDownload(wifiOnly = true)

        assertEquals(ModelState.Failed(ModelError.INSUFFICIENT_STORAGE), manager.state.value)
        assertTrue(downloads.enqueued.isEmpty())
    }

    // MM4
    @Test
    fun startDownloadWithExactlyTheModelSizePlusTheMarginQueues() = runTest {
        device.freeBytes = 3_088_147_712
        val manager = newManager()
        manager.refresh()

        manager.startDownload(wifiOnly = true)

        assertEquals(1, downloads.enqueued.size)
    }

    // MM5
    @Test
    fun runningProgressAndWifiPauseAreReported() = runTest {
        val manager = newManager()
        val id = startedDownload(manager)

        downloads.update(id, DownloadStatus.RUNNING, downloadedBytes = 1_000_000_000)
        runCurrent()
        assertEquals(ModelState.Downloading(1_000_000_000, 2_588_147_712, null), manager.state.value)

        downloads.update(
            id,
            DownloadStatus.PAUSED,
            downloadedBytes = 1_000_000_000,
            totalBytes = 2_588_147_712,
            reason = DownloadManager.PAUSED_QUEUED_FOR_WIFI,
        )
        runCurrent()
        assertEquals(ModelState.Downloading(1_000_000_000, 2_588_147_712, DownloadWait.WIFI), manager.state.value)
    }

    // MM5
    @Test
    fun pausesForNetworkAndRetryAreReported() = runTest {
        val manager = newManager()
        val id = startedDownload(manager)

        downloads.update(id, DownloadStatus.PAUSED, 5_000, reason = DownloadManager.PAUSED_WAITING_FOR_NETWORK)
        runCurrent()
        assertEquals(ModelState.Downloading(5_000, 2_588_147_712, DownloadWait.NETWORK), manager.state.value)

        downloads.update(id, DownloadStatus.PAUSED, 5_000, reason = DownloadManager.PAUSED_WAITING_TO_RETRY)
        runCurrent()
        assertEquals(ModelState.Downloading(5_000, 2_588_147_712, DownloadWait.RETRY), manager.state.value)
    }

    // MM6
    @Test
    fun finishedDownloadIsVerifiedThenRenamedAndOnlyThenRemoved() = runTest {
        val manager = newManager()
        val id = startedDownload(manager)

        downloads.finish(id, sizeBytes = 2_588_147_712)
        runCurrent()

        assertEquals(ModelState.Downloaded(modelFile, 2_588_147_712), manager.state.value)
        assertTrue(modelFile.isFile)
        assertFalse(partialFile.exists())
        assertEquals(listOf(id), downloads.removed)
        // The system deletes the file at the download's destination: it must be gone already.
        assertEquals(listOf(false), downloads.destinationExistedOnRemove)
    }

    // MM7
    @Test
    fun downloadOneByteShortFailsVerificationAndLeavesNoFile() = runTest {
        val manager = newManager()
        val id = startedDownload(manager)

        downloads.finish(id, sizeBytes = 2_588_147_711)
        runCurrent()

        assertEquals(ModelState.Failed(ModelError.VERIFICATION_FAILED), manager.state.value)
        assertFalse(modelFile.exists())
        assertFalse(partialFile.exists())
        assertEquals(listOf(id), downloads.removed)
    }

    // MM7
    @Test
    fun downloadOfTheRightSizeWithAnotherHeaderFailsVerification() = runTest {
        val manager = newManager()
        val id = startedDownload(manager)

        downloads.finish(id, sizeBytes = 2_588_147_712, header = "<!DOCTYPE html>")
        runCurrent()

        assertEquals(ModelState.Failed(ModelError.VERIFICATION_FAILED), manager.state.value)
        assertFalse(modelFile.exists())
        assertFalse(partialFile.exists())
    }

    // MM8
    @Test
    fun failedDownloadIsReportedAndForgotten() = runTest {
        val manager = newManager()
        val id = startedDownload(manager)
        partialFile.writeBytes(ByteArray(100))

        downloads.fail(id, reason = DownloadManager.ERROR_HTTP_DATA_ERROR)
        runCurrent()

        assertEquals(ModelState.Failed(ModelError.DOWNLOAD_FAILED), manager.state.value)
        assertEquals(listOf(id), downloads.removed)
        assertFalse(partialFile.exists())
    }

    // MM8
    @Test
    fun downloadFailedForLackOfSpaceFoundAfterRestartIsInsufficientStorage() = runTest {
        downloads.addModelDownload(DownloadStatus.FAILED, 2_000_000, reason = DownloadManager.ERROR_INSUFFICIENT_SPACE)
        val manager = newManager()

        manager.refresh()

        assertEquals(ModelState.Failed(ModelError.INSUFFICIENT_STORAGE), manager.state.value)
        assertEquals(listOf(1L), downloads.removed)
    }

    // MM9
    @Test
    fun cancelDuringDownloadRemovesItAndThePartialFile() = runTest {
        val manager = newManager()
        val id = startedDownload(manager)
        downloads.update(id, DownloadStatus.RUNNING, 300_000_000)
        runCurrent()
        partialFile.writeBytes(ByteArray(100))

        manager.cancel()
        runCurrent()

        assertEquals(ModelState.NotDownloaded, manager.state.value)
        assertEquals(listOf(id), downloads.removed)
        assertFalse(partialFile.exists())
    }

    // MM10
    @Test
    fun refreshAfterRestartFollowsTheRunningDownloadWithoutQueueingAgain() = runTest {
        val id = downloads.addModelDownload(DownloadStatus.RUNNING, 500_000_000, totalBytes = 2_588_147_712)
        val manager = newManager()

        manager.refresh()
        runCurrent()
        assertEquals(ModelState.Downloading(500_000_000, 2_588_147_712, null), manager.state.value)

        downloads.update(id, DownloadStatus.RUNNING, 600_000_000, totalBytes = 2_588_147_712)
        runCurrent()
        assertEquals(ModelState.Downloading(600_000_000, 2_588_147_712, null), manager.state.value)

        manager.startDownload(wifiOnly = true)
        assertTrue(downloads.enqueued.isEmpty())
    }

    // MM10
    @Test
    fun downloadFinishedWhileTheAppWasClosedIsCompletedOnRefresh() = runTest {
        val id = downloads.addModelDownload(DownloadStatus.RUNNING, 500_000_000)
        downloads.finish(id, sizeBytes = 2_588_147_712)
        val manager = newManager()

        manager.refresh()

        assertEquals(ModelState.Downloaded(modelFile, 2_588_147_712), manager.state.value)
        assertEquals(listOf(false), downloads.destinationExistedOnRemove)
    }

    // MM11
    @Test
    fun validImportGoesThroughImportingAndEndsWithTheSameContent() = runTest {
        val content = FakeImportSource.modelBytes(3 * 1024 * 1024)
        val manager = newManager()
        val states = mutableListOf<ModelState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { manager.state.toList(states) }
        manager.refresh()

        manager.importModel(FakeImportSource(content))

        assertTrue(states.any { it is ModelState.Importing && it.totalBytes == content.size.toLong() && it.copiedBytes > 0 })
        assertEquals(ModelState.Downloaded(modelFile, content.size.toLong()), manager.state.value)
        assertArrayEquals(content, modelFile.readBytes())
        assertFalse(importFile.exists())
    }

    // MM11
    @Test
    fun importOfUnknownSizeIsAcceptedWithAnySize() = runTest {
        val content = FakeImportSource.modelBytes(10)
        val manager = newManager()
        manager.refresh()

        manager.importModel(FakeImportSource(content, sizeBytes = null, displayName = null))

        assertEquals(ModelState.Downloaded(modelFile, 18), manager.state.value)
    }

    // MM12
    @Test
    fun importWithoutTheHeaderIsRejectedAndLeavesNoFile() = runTest {
        val manager = newManager()
        manager.refresh()

        manager.importModel(FakeImportSource("PK\u0003\u0004 a zip file, not a model".toByteArray()))

        assertEquals(ModelState.Failed(ModelError.INVALID_FILE), manager.state.value)
        assertFalse(modelFile.exists())
        assertFalse(importFile.exists())
    }

    // MM12
    @Test
    fun importShorterThanTheHeaderIsRejected() = runTest {
        val manager = newManager()
        manager.refresh()

        manager.importModel(FakeImportSource("LITERT".toByteArray()))

        assertEquals(ModelState.Failed(ModelError.INVALID_FILE), manager.state.value)
        assertFalse(modelFile.exists())
        assertFalse(importFile.exists())
    }

    // MM13
    @Test
    fun importLargerThanTheFreeSpaceFailsBeforeOpeningTheFile() = runTest {
        device.freeBytes = 10_499_999_999
        val source = FakeImportSource(FakeImportSource.modelBytes(10), sizeBytes = 10_000_000_000)
        val manager = newManager()
        manager.refresh()

        manager.importModel(source)

        assertEquals(ModelState.Failed(ModelError.INSUFFICIENT_STORAGE), manager.state.value)
        assertEquals(0, source.openCount)
        assertFalse(importFile.exists())
    }

    // MM14
    @Test
    fun readErrorHalfwayFailsTheImportAndDeletesTheCopy() = runTest {
        val content = FakeImportSource.modelBytes(3 * 1024 * 1024)
        val manager = newManager()
        manager.refresh()

        manager.importModel(FakeImportSource(content, failAfterBytes = 1_500_000))

        assertEquals(ModelState.Failed(ModelError.IMPORT_FAILED), manager.state.value)
        assertFalse(importFile.exists())
        assertFalse(modelFile.exists())
    }

    // MM14: cancel() during an import (the spec of cancel(); not in the table)
    @Test
    fun cancelDuringImportStopsTheCopyAndDeletesIt() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val manager = newManager(io)
        manager.refresh()
        var cancelRequested = false
        val source = FakeImportSource(FakeImportSource.modelBytes(4 * 1024 * 1024)) { readSoFar ->
            if (!cancelRequested && readSoFar >= 1024 * 1024) {
                cancelRequested = true
                // Runs cancel() right away, inside the read, up to its join of the copy job.
                backgroundScope.launch(io, start = CoroutineStart.UNDISPATCHED) { manager.cancel() }
            }
        }

        manager.importModel(source)
        runCurrent()

        assertTrue(cancelRequested)
        assertEquals(ModelState.NotDownloaded, manager.state.value)
        assertFalse(importFile.exists())
        assertFalse(modelFile.exists())
    }

    // MM15
    @Test
    fun deleteRemovesTheModelAndAnyLeftover() = runTest {
        modelFile.writeBytes(FakeImportSource.modelBytes(100))
        partialFile.writeBytes(ByteArray(10))
        importFile.writeBytes(ByteArray(10))
        val manager = newManager()
        manager.refresh()
        assertTrue(manager.state.value is ModelState.Downloaded)

        manager.delete()

        assertEquals(ModelState.NotDownloaded, manager.state.value)
        assertFalse(modelFile.exists())
        assertFalse(partialFile.exists())
        assertFalse(importFile.exists())
    }

    // MM16
    @Test
    fun errorStaysUntilDismissed() = runTest {
        device.freeBytes = 1_000_000_000
        val manager = newManager()
        manager.refresh()
        manager.startDownload(wifiOnly = true)
        assertEquals(ModelState.Failed(ModelError.INSUFFICIENT_STORAGE), manager.state.value)

        manager.refresh()
        assertEquals(ModelState.Failed(ModelError.INSUFFICIENT_STORAGE), manager.state.value)

        manager.dismissError()
        runCurrent()
        assertEquals(ModelState.NotDownloaded, manager.state.value)
    }

    // MM17
    @Test
    fun deviceWithout64BitAbiCanNeitherDownloadNorImport() = runTest {
        device.is64Bit = false
        val source = FakeImportSource(FakeImportSource.modelBytes(100))
        val manager = newManager()
        manager.refresh()

        manager.startDownload(wifiOnly = true)
        assertEquals(ModelState.Failed(ModelError.UNSUPPORTED_DEVICE), manager.state.value)
        assertTrue(downloads.enqueued.isEmpty())

        manager.dismissError()
        runCurrent()
        manager.importModel(source)
        assertEquals(ModelState.Failed(ModelError.UNSUPPORTED_DEVICE), manager.state.value)
        assertEquals(0, source.openCount)
        assertFalse(modelFile.exists())
    }

    // MM18
    @Test
    fun startDownloadWhileDownloadingDoesNotQueueAgain() = runTest {
        val manager = newManager()
        startedDownload(manager)

        manager.startDownload(wifiOnly = false)

        assertEquals(1, downloads.enqueued.size)
    }

    // MM18
    @Test
    fun startDownloadWithTheModelDownloadedDoesNotQueue() = runTest {
        modelFile.writeBytes(FakeImportSource.modelBytes(100))
        val manager = newManager()

        manager.startDownload(wifiOnly = true)

        assertTrue(downloads.enqueued.isEmpty())
        assertEquals(ModelState.Downloaded(modelFile, 108), manager.state.value)
    }

    @Test
    fun deviceCheckReportsTheDevice() = runTest {
        device.ramBytes = 4_000_000_000
        device.freeBytes = 2_000_000_000
        val manager = newManager()

        assertEquals(DeviceCheck(4_000_000_000, 2_000_000_000, supports64Bit = true), manager.deviceCheck())
    }
}
