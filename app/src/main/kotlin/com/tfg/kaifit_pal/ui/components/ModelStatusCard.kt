package com.tfg.kaifit_pal.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.data.llm.DeviceCheck
import com.tfg.kaifit_pal.data.llm.DownloadWait
import com.tfg.kaifit_pal.data.llm.ModelError
import com.tfg.kaifit_pal.data.llm.ModelSpec
import com.tfg.kaifit_pal.data.llm.ModelState
import com.tfg.kaifit_pal.ui.theme.KaiFitTheme
import com.tfg.kaifit_pal.ui.theme.Spacing
import com.tfg.kaifit_pal.ui.theme.withTabularFigures
import java.io.File
import java.util.Locale

/**
 * State of the on-device model with the actions that fit it: download or import it, follow and
 * cancel a transfer, or read an error and dismiss it.
 *
 * Before a download it explains the model and checks the device: a 32-bit device cannot use
 * it, too little free space disables "Descargar", and little RAM only gets a warning.
 *
 * @param device RAM, free space and ABI; while `null` the checks are not shown.
 * @param downloadOnWifiOnly whether the download waits for Wi-Fi, for the note under the buttons.
 */
@Composable
fun ModelStatusCard(
    state: ModelState,
    device: DeviceCheck?,
    downloadOnWifiOnly: Boolean,
    onDownload: () -> Unit,
    onImport: () -> Unit,
    onCancel: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            when (state) {
                ModelState.Checking -> Checking()
                ModelState.NotDownloaded -> NotDownloaded(device, downloadOnWifiOnly, locale, onDownload, onImport)
                is ModelState.Downloading -> Downloading(state, locale, onCancel)
                is ModelState.Importing -> Importing(state, locale, onCancel)
                is ModelState.Downloaded -> {
                    CardTitle(R.string.model_card_title)
                    Text(
                        text = stringResource(R.string.model_downloaded, formatGigabytes(state.sizeBytes, locale)),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                is ModelState.Failed -> Failed(state.error, onDismissError)
            }
        }
    }
}

@Composable
private fun Checking() {
    val description = stringResource(R.string.model_checking)
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
    }
}

@Composable
private fun NotDownloaded(
    device: DeviceCheck?,
    downloadOnWifiOnly: Boolean,
    locale: Locale,
    onDownload: () -> Unit,
    onImport: () -> Unit,
) {
    CardTitle(R.string.model_card_title)
    if (device != null && !device.supports64Bit) {
        Text(
            text = stringResource(R.string.model_unsupported_device),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        return
    }
    Text(
        text = stringResource(R.string.model_intro, formatGigabytes(ModelSpec.SIZE_BYTES, locale)),
        style = MaterialTheme.typography.bodyLarge,
    )
    if (device != null) {
        Text(
            text = stringResource(R.string.model_ram_recommended),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (device.lowRam) {
            Text(
                text = stringResource(R.string.model_ram_low, formatGigabytes(device.totalRamBytes, locale)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = stringResource(R.string.model_free_space, formatGigabytes(device.availableStorageBytes, locale)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!device.enoughStorageForDownload) {
            Text(
                text = stringResource(R.string.model_free_space_missing, formatGigabytes(device.missingStorageBytes, locale)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
    Text(
        text = stringResource(if (downloadOnWifiOnly) R.string.model_wifi_only_note else R.string.model_metered_note),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(
        onClick = onDownload,
        enabled = device?.enoughStorageForDownload ?: true,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Text(stringResource(R.string.model_download))
    }
    OutlinedButton(
        onClick = onImport,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Text(stringResource(R.string.model_import))
    }
}

@Composable
private fun Downloading(state: ModelState.Downloading, locale: Locale, onCancel: () -> Unit) {
    val percent = progressPercent(state.downloadedBytes, state.totalBytes)
    CardTitle(R.string.model_downloading_title)
    LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
    Text(
        text = stringResource(
            R.string.model_progress,
            percent,
            formatGigabytesValue(state.downloadedBytes, locale),
            formatGigabytes(state.totalBytes, locale),
        ),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyLarge.withTabularFigures(),
    )
    state.waiting?.let { waiting ->
        Text(
            text = stringResource(waiting.labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    CancelButton(onCancel)
}

@Composable
private fun Importing(state: ModelState.Importing, locale: Locale, onCancel: () -> Unit) {
    CardTitle(R.string.model_importing_title)
    val total = state.totalBytes
    val progressText = if (total != null && total > 0) {
        val percent = progressPercent(state.copiedBytes, total)
        LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
        stringResource(
            R.string.model_progress,
            percent,
            formatGigabytesValue(state.copiedBytes, locale),
            formatGigabytes(total, locale),
        )
    } else {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        stringResource(R.string.model_imported_bytes, formatGigabytes(state.copiedBytes, locale))
    }
    Text(
        text = progressText,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyLarge.withTabularFigures(),
    )
    CancelButton(onCancel)
}

@Composable
private fun Failed(error: ModelError, onDismissError: () -> Unit) {
    CardTitle(R.string.model_error_title)
    Text(
        text = stringResource(error.messageRes),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
    )
    Button(
        onClick = onDismissError,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Text(stringResource(R.string.model_ok))
    }
}

@Composable
private fun CardTitle(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun CancelButton(onCancel: () -> Unit) {
    OutlinedButton(
        onClick = onCancel,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Text(stringResource(R.string.model_cancel))
    }
}

/** Why a download is waiting, as shown under its progress. */
@get:StringRes
internal val DownloadWait.labelRes: Int
    get() = when (this) {
        DownloadWait.WIFI -> R.string.model_waiting_wifi
        DownloadWait.NETWORK -> R.string.model_waiting_network
        DownloadWait.RETRY -> R.string.model_waiting_retry
    }

/** Message for each model error. */
@get:StringRes
internal val ModelError.messageRes: Int
    get() = when (this) {
        ModelError.UNSUPPORTED_DEVICE -> R.string.model_error_unsupported_device
        ModelError.INSUFFICIENT_STORAGE -> R.string.model_error_insufficient_storage
        ModelError.DOWNLOAD_FAILED -> R.string.model_error_download_failed
        ModelError.VERIFICATION_FAILED -> R.string.model_error_verification_failed
        ModelError.INVALID_FILE -> R.string.model_error_invalid_file
        ModelError.IMPORT_FAILED -> R.string.model_error_import_failed
    }

/** Fixed states for previews and screenshots: 8 GB of RAM and 20 GB free. */
internal object ModelPreviewStates {
    val roomyDevice = DeviceCheck(totalRamBytes = 8_000_000_000, availableStorageBytes = 20_000_000_000, supports64Bit = true)
    val crampedDevice = DeviceCheck(totalRamBytes = 4_000_000_000, availableStorageBytes = 2_000_000_000, supports64Bit = true)
    val unsupportedDevice = DeviceCheck(totalRamBytes = 3_000_000_000, availableStorageBytes = 20_000_000_000, supports64Bit = false)

    /** 42 %: 1,09 of 2,59 GB. */
    val downloading = ModelState.Downloading(downloadedBytes = 1_090_000_000, totalBytes = ModelSpec.SIZE_BYTES, waiting = null)
    val waitingForWifi = ModelState.Downloading(downloadedBytes = 260_000_000, totalBytes = ModelSpec.SIZE_BYTES, waiting = DownloadWait.WIFI)
    val importing = ModelState.Importing(copiedBytes = 780_000_000, totalBytes = ModelSpec.SIZE_BYTES)
    val downloaded = ModelState.Downloaded(File("/models/${ModelSpec.FILE_NAME}"), ModelSpec.SIZE_BYTES)
}

@PreviewLightDark
@Composable
private fun ModelStatusCardNotDownloadedPreview() {
    KaiFitTheme {
        ModelStatusCard(
            state = ModelState.NotDownloaded,
            device = ModelPreviewStates.roomyDevice,
            downloadOnWifiOnly = true,
            onDownload = {},
            onImport = {},
            onCancel = {},
            onDismissError = {},
            modifier = Modifier.padding(Spacing.l),
        )
    }
}

@PreviewLightDark
@Composable
private fun ModelStatusCardDownloadingPreview() {
    KaiFitTheme {
        ModelStatusCard(
            state = ModelPreviewStates.downloading,
            device = ModelPreviewStates.roomyDevice,
            downloadOnWifiOnly = true,
            onDownload = {},
            onImport = {},
            onCancel = {},
            onDismissError = {},
            modifier = Modifier.padding(Spacing.l),
        )
    }
}
