package com.tfg.kaifit_pal.testing

import com.tfg.kaifit_pal.data.llm.DeviceInfo
import java.io.File

/** A device with the given RAM, free space and ABI support; by default 8 GB of RAM and 20 GB free. */
class FakeDeviceInfo(
    var ramBytes: Long = 8_000_000_000L,
    var freeBytes: Long = 20_000_000_000L,
    var is64Bit: Boolean = true,
) : DeviceInfo {

    override fun totalRamBytes(): Long = ramBytes

    override fun availableBytes(dir: File): Long = freeBytes

    override fun supports64Bit(): Boolean = is64Bit
}
