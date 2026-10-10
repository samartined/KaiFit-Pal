package com.tfg.kaifit_pal.data.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCheckTest {

    // DC1
    @Test
    fun ramBelowFiveAndAHalfGigabytesIsLow() {
        assertTrue(DeviceCheck(totalRamBytes = 5_499_999_999, availableStorageBytes = 0, supports64Bit = true).lowRam)
        assertFalse(DeviceCheck(totalRamBytes = 5_500_000_000, availableStorageBytes = 0, supports64Bit = true).lowRam)
    }

    // DC1
    @Test
    fun downloadNeedsTheModelSizePlusTheMargin() {
        val oneByteShort = DeviceCheck(totalRamBytes = 8_000_000_000, availableStorageBytes = 3_088_147_711, supports64Bit = true)
        val exact = DeviceCheck(totalRamBytes = 8_000_000_000, availableStorageBytes = 3_088_147_712, supports64Bit = true)

        assertFalse(oneByteShort.enoughStorageForDownload)
        assertTrue(exact.enoughStorageForDownload)
    }

    // DC1
    @Test
    fun missingStorageIsWhatTheDownloadStillNeeds() {
        assertEquals(1_088_147_712, DeviceCheck(8_000_000_000, availableStorageBytes = 2_000_000_000, supports64Bit = true).missingStorageBytes)
        assertEquals(0, DeviceCheck(8_000_000_000, availableStorageBytes = 20_000_000_000, supports64Bit = true).missingStorageBytes)
    }
}
