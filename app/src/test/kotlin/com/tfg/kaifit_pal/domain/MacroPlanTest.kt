package com.tfg.kaifit_pal.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MacroPlanTest {

    private val maintenance2500 = MacroPlan.forGoal(2500, Goal.MAINTENANCE)

    @Test
    fun maintenance2500Is188_69_282With2501Kcal() {
        val plan = MacroPlan.forGoal(2500, Goal.MAINTENANCE)
        assertEquals(188, plan.proteinGrams)
        assertEquals(69, plan.fatGrams)
        assertEquals(282, plan.carbGrams)
        assertEquals(2501, plan.totalKcal)
    }

    @Test
    fun deficit2500Is219_69_251With2501Kcal() {
        val plan = MacroPlan.forGoal(2500, Goal.DEFICIT)
        assertEquals(219, plan.proteinGrams)
        assertEquals(69, plan.fatGrams)
        assertEquals(251, plan.carbGrams)
        assertEquals(2501, plan.totalKcal)
    }

    @Test
    fun surplus2500Is156_56_343With2500Kcal() {
        val plan = MacroPlan.forGoal(2500, Goal.SURPLUS)
        assertEquals(156, plan.proteinGrams)
        assertEquals(56, plan.fatGrams)
        assertEquals(343, plan.carbGrams)
        assertEquals(2500, plan.totalKcal)
    }

    @Test
    fun forGoalKeepsTargetKcal() {
        assertEquals(2500, maintenance2500.targetKcal)
    }

    @Test
    fun maintenance2500WithProtein250Has220CarbGrams() {
        val plan = maintenance2500.withProteinGrams(250)
        assertEquals(250, plan.proteinGrams)
        assertEquals(69, plan.fatGrams)
        assertEquals(220, plan.carbGrams)
    }

    @Test
    fun maxGramsOfMaintenance2500Are469ProteinAnd194Fat() {
        assertEquals(469, maintenance2500.maxProteinGrams)
        assertEquals(194, maintenance2500.maxFatGrams)
    }

    @Test
    fun withProteinGramsAboveMaximumIsCappedAt469() {
        val plan = maintenance2500.withProteinGrams(10_000)
        assertEquals(469, plan.proteinGrams)
        assertEquals(69, plan.fatGrams)
        assertTrue("carbs ${plan.carbGrams}", plan.carbGrams >= 0)
        assertTrue("total ${plan.totalKcal}", abs(plan.totalKcal - 2500) <= 2)
    }

    @Test
    fun withFatGramsAboveMaximumIsCappedAt194() {
        val plan = maintenance2500.withFatGrams(10_000)
        assertEquals(194, plan.fatGrams)
        assertEquals(188, plan.proteinGrams)
        assertTrue("carbs ${plan.carbGrams}", plan.carbGrams >= 0)
        assertTrue("total ${plan.totalKcal}", abs(plan.totalKcal - 2500) <= 2)
    }

    @Test
    fun withProteinGramsNegativeIsClampedToZero() {
        val plan = maintenance2500.withProteinGrams(-50)
        assertEquals(0, plan.proteinGrams)
        assertEquals(69, plan.fatGrams)
        assertEquals(470, plan.carbGrams)
    }

    @Test
    fun withFatGramsNegativeIsClampedToZero() {
        val plan = maintenance2500.withFatGrams(-50)
        assertEquals(0, plan.fatGrams)
        assertEquals(188, plan.proteinGrams)
        assertEquals(437, plan.carbGrams)
    }

    @Test
    fun forGoalCarbsAreNeverNegativeAndTotalIsWithin2KcalFrom1000To5000() {
        for (targetKcal in 1000..5000) {
            for (goal in Goal.entries) {
                val plan = MacroPlan.forGoal(targetKcal, goal)
                val case = "targetKcal=$targetKcal goal=$goal plan=$plan"
                assertTrue("carbs < 0: $case", plan.carbGrams >= 0)
                assertTrue("total ${plan.totalKcal}: $case", abs(plan.totalKcal - targetKcal) <= 2)
            }
        }
    }

    @Test
    fun cappedProteinOrFatKeepCarbsNonNegativeAndTotalWithin2KcalFrom1000To5000() {
        for (targetKcal in 1000..5000) {
            for (goal in Goal.entries) {
                val base = MacroPlan.forGoal(targetKcal, goal)
                for (plan in listOf(base.withProteinGrams(10_000), base.withFatGrams(10_000))) {
                    val case = "targetKcal=$targetKcal goal=$goal plan=$plan"
                    assertTrue("carbs < 0: $case", plan.carbGrams >= 0)
                    assertTrue("total ${plan.totalKcal}: $case", abs(plan.totalKcal - targetKcal) <= 2)
                }
            }
        }
    }

    @Test
    fun zeroTargetKcalIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { MacroPlan(0, 0, 0, 0) }
    }

    @Test
    fun negativeGramsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { MacroPlan(2500, -1, 69, 282) }
    }
}
