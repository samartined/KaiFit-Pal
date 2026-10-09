package com.tfg.kaifit_pal.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GoalModifierTest {

    @Test
    fun goalModifierMinus20FromTdee2716Is2173() {
        assertEquals(2173, GoalModifier(-20).applyTo(2716))
    }

    @Test
    fun goalModifierMinus15FromTdee2716Is2309() {
        assertEquals(2309, GoalModifier(-15).applyTo(2716))
    }

    @Test
    fun goalModifierMinus10FromTdee2716Is2444() {
        assertEquals(2444, GoalModifier(-10).applyTo(2716))
    }

    @Test
    fun goalModifierMinus5FromTdee2716Is2580() {
        assertEquals(2580, GoalModifier(-5).applyTo(2716))
    }

    @Test
    fun goalModifier0FromTdee2716Is2716() {
        assertEquals(2716, GoalModifier(0).applyTo(2716))
    }

    @Test
    fun goalModifierPlus5FromTdee2716Is2852() {
        assertEquals(2852, GoalModifier(5).applyTo(2716))
    }

    @Test
    fun goalModifierPlus10FromTdee2716Is2988() {
        assertEquals(2988, GoalModifier(10).applyTo(2716))
    }

    @Test
    fun goalModifierPlus15FromTdee2716Is3123() {
        assertEquals(3123, GoalModifier(15).applyTo(2716))
    }

    @Test
    fun goalModifierPlus20FromTdee2716Is3259() {
        assertEquals(3259, GoalModifier(20).applyTo(2716))
    }

    @Test
    fun goalModifierMinus15IsModerateHighDeficit() {
        // "Déficit moderado-intenso".
        assertEquals(Goal.DEFICIT, GoalModifier(-15).goal)
        assertEquals(GoalIntensity.MODERATE_HIGH, GoalModifier(-15).intensity)
    }

    @Test
    fun everyModifierMapsToItsGoalAndIntensity() {
        val expected = listOf(
            Triple(-20, Goal.DEFICIT, GoalIntensity.HIGH),
            Triple(-15, Goal.DEFICIT, GoalIntensity.MODERATE_HIGH),
            Triple(-10, Goal.DEFICIT, GoalIntensity.MODERATE),
            Triple(-5, Goal.DEFICIT, GoalIntensity.LIGHT),
            Triple(0, Goal.MAINTENANCE, GoalIntensity.NONE),
            Triple(5, Goal.SURPLUS, GoalIntensity.LIGHT),
            Triple(10, Goal.SURPLUS, GoalIntensity.MODERATE),
            Triple(15, Goal.SURPLUS, GoalIntensity.MODERATE_HIGH),
            Triple(20, Goal.SURPLUS, GoalIntensity.HIGH),
        )
        for ((percent, goal, intensity) in expected) {
            val modifier = GoalModifier(percent)
            assertEquals("goal of $percent %", goal, modifier.goal)
            assertEquals("intensity of $percent %", intensity, modifier.intensity)
        }
    }

    @Test
    fun threeDecrementsFromMaintenanceAreExactlyMinus15() {
        // Regression: the original app added -0.05 in floating point and showed "Déficit intenso" here.
        val modifier = GoalModifier.MAINTENANCE.decrement().decrement().decrement()
        assertEquals(GoalModifier(-15), modifier)
        assertEquals(GoalIntensity.MODERATE_HIGH, modifier.intensity)
    }

    @Test
    fun allHasTheNineModifiersInAscendingOrder() {
        assertEquals(listOf(-20, -15, -10, -5, 0, 5, 10, 15, 20), GoalModifier.all.map { it.percent })
    }

    @Test
    fun maintenanceIsZeroPercent() {
        assertEquals(0, GoalModifier.MAINTENANCE.percent)
        assertEquals(Goal.MAINTENANCE, GoalModifier.MAINTENANCE.goal)
        assertEquals(GoalIntensity.NONE, GoalModifier.MAINTENANCE.intensity)
    }

    @Test
    fun incrementAddsOneStep() {
        assertEquals(GoalModifier(5), GoalModifier.MAINTENANCE.increment())
        assertEquals(GoalModifier(-10), GoalModifier(-15).increment())
    }

    @Test
    fun incrementSaturatesAtPlus20() {
        assertEquals(GoalModifier(20), GoalModifier(20).increment())
    }

    @Test
    fun decrementSaturatesAtMinus20() {
        assertEquals(GoalModifier(-20), GoalModifier(-20).decrement())
    }

    @Test
    fun goalModifier7IsRejected() {
        assertThrows(IllegalArgumentException::class.java) { GoalModifier(7) }
    }

    @Test
    fun goalModifier25IsRejected() {
        assertThrows(IllegalArgumentException::class.java) { GoalModifier(25) }
    }

    @Test
    fun goalModifierMinus25IsRejected() {
        assertThrows(IllegalArgumentException::class.java) { GoalModifier(-25) }
    }

    @Test
    fun goalModifierMinus7IsRejected() {
        assertThrows(IllegalArgumentException::class.java) { GoalModifier(-7) }
    }
}
