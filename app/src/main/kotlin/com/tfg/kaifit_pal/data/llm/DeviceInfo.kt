package com.tfg.kaifit_pal.data.llm

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import java.io.File

/** Hardware facts that decide whether the model can run, behind an interface for JVM tests. */
interface DeviceInfo {

    /** Total RAM reported to apps, in bytes. */
    fun totalRamBytes(): Long

    /** Free space, in bytes, of the file system that holds [dir]. */
    fun availableBytes(dir: File): Long

    /** Whether the device runs an ABI that the inference library ships: arm64-v8a or x86_64. */
    fun supports64Bit(): Boolean
}

/** [DeviceInfo] of the running device. */
class AndroidDeviceInfo(context: Context) : DeviceInfo {

    private val activityManager: ActivityManager =
        context.applicationContext.getSystemService(ActivityManager::class.java)

    override fun totalRamBytes(): Long =
        ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }.totalMem

    override fun availableBytes(dir: File): Long {
        // StatFs needs an existing path: measure the closest folder that exists.
        val existing = generateSequence(dir) { it.parentFile }.firstOrNull { it.exists() } ?: return 0
        return StatFs(existing.path).availableBytes
    }

    override fun supports64Bit(): Boolean =
        Build.SUPPORTED_64_BIT_ABIS.any { it == "arm64-v8a" || it == "x86_64" }
}
