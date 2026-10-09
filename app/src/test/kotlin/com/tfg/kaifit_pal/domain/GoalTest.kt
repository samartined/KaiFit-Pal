package com.tfg.kaifit_pal.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalTest {

    private fun sumOf(goal: Goal) = goal.proteinPercent + goal.fatPercent + goal.carbPercent

    @Test
    fun deficitSplitIs35_25_40() {
        assertEquals(35, Goal.DEFICIT.proteinPercent)
        assertEquals(25, Goal.DEFICIT.fatPercent)
        assertEquals(40, Goal.DEFICIT.carbPercent)
    }

    @Test
    fun maintenanceSplitIs30_25_45() {
        assertEquals(30, Goal.MAINTENANCE.proteinPercent)
        assertEquals(25, Goal.MAINTENANCE.fatPercent)
        assertEquals(45, Goal.MAINTENANCE.carbPercent)
    }

    @Test
    fun surplusSplitIs25_20_55() {
        assertEquals(25, Goal.SURPLUS.proteinPercent)
        assertEquals(20, Goal.SURPLUS.fatPercent)
        assertEquals(55, Goal.SURPLUS.carbPercent)
    }

    @Test
    fun deficitAddsUpTo100() {
        assertEquals(100, sumOf(Goal.DEFICIT))
    }

    @Test
    fun maintenanceAddsUpTo100() {
        // Regression: the original app used 35/25/45, which adds up to 105 %.
        assertEquals(100, sumOf(Goal.MAINTENANCE))
    }

    @Test
    fun surplusAddsUpTo100() {
        assertEquals(100, sumOf(Goal.SURPLUS))
    }
}
