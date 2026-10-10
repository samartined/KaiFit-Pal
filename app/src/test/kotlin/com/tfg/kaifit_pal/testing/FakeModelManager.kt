package com.tfg.kaifit_pal.testing

import com.tfg.kaifit_pal.data.llm.DeviceCheck
import com.tfg.kaifit_pal.data.llm.ImportSource
import com.tfg.kaifit_pal.data.llm.ModelManager
import com.tfg.kaifit_pal.data.llm.ModelState
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * Model manager for tests: the state is whatever the test sets, and every call is logged.
 *
 * @param calls shared call log, so that a test can check the order against other fakes.
 */
class FakeModelManager(
    initial: ModelState = ModelState.NotDownloaded,
    val calls: MutableList<String> = mutableListOf(),
) : ModelManager {

    override val state = MutableStateFlow(initial)

    var device = DeviceCheck(totalRamBytes = 8_000_000_000, availableStorageBytes = 20_000_000_000, supports64Bit = true)

    /** The `wifiOnly` argument of every download started, as a flow a test can wait on. */
    val downloadRequests = MutableStateFlow<List<Boolean>>(emptyList())

    /** Every source imported. */
    val imports = mutableListOf<ImportSource>()

    override suspend fun refresh() {
        calls += "model.refresh"
    }

    override suspend fun deviceCheck(): DeviceCheck = device

    override suspend fun startDownload(wifiOnly: Boolean) {
        calls += "model.startDownload"
        downloadRequests.value += wifiOnly
    }

    override suspend fun importModel(source: ImportSource) {
        calls += "model.importModel"
        imports += source
    }

    override suspend fun cancel() {
        calls += "model.cancel"
    }

    override suspend fun delete() {
        calls += "model.delete"
        state.value = ModelState.NotDownloaded
    }

    override fun dismissError() {
        calls += "model.dismissError"
        state.value = ModelState.NotDownloaded
    }

    companion object {
        /** The model as if it had been downloaded. */
        val downloaded = ModelState.Downloaded(File("/models/gemma-4-E2B-it.litertlm"), 2_588_147_712)
    }
}
