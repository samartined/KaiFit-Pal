package com.tfg.kaifit_pal.data.llm

import com.tfg.kaifit_pal.data.profile.SavedProfile
import com.tfg.kaifit_pal.domain.ActivityLevel
import com.tfg.kaifit_pal.domain.BodyMeasurements
import com.tfg.kaifit_pal.domain.GoalModifier
import com.tfg.kaifit_pal.domain.MacroPlan
import com.tfg.kaifit_pal.domain.Sex
import com.tfg.kaifit_pal.domain.TdeeCalculator
import com.tfg.kaifit_pal.testing.ReferenceData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Locale

class KaiQPromptTest {

    /** Saves [measurements] at [activityLevel] with the default macros of [goalPercent]. */
    private fun profileOf(measurements: BodyMeasurements, activityLevel: ActivityLevel, goalPercent: Int): SavedProfile {
        val tdee = checkNotNull(TdeeCalculator.calculate(measurements, activityLevel))
        val modifier = GoalModifier(goalPercent)
        val plan = MacroPlan.forGoal(modifier.applyTo(tdee.tdeeKcal), modifier.goal)
        return SavedProfile(measurements, activityLevel, tdee, modifier, plan, Instant.parse("2026-10-09T19:30:00Z"))
    }

    /** Reference man at −15 %: 16.15 % body fat, TDEE 2819, target 2396 kcal, 210 / 67 / 238 g. */
    private val referenceMan = profileOf(ReferenceData.man, ReferenceData.manActivity, goalPercent = -15)

    // KP1
    @Test
    fun withoutProfileThePersonaAsksToUseTheCalculator() {
        val prompt = KaiQPrompt.systemInstruction(null)

        assertTrue(prompt.contains("Kai-Q"))
        assertTrue(prompt.contains("español"))
        assertTrue(prompt.contains("Calculadora"))
        assertFalse(prompt.contains("kcal/día"))
        assertFalse(prompt.contains("Datos del usuario"))
    }

    // KP1
    @Test
    fun personaSetsTheMedicalLimitsAndPlainText() {
        val prompt = KaiQPrompt.systemInstruction(null)

        assertTrue(prompt.contains("No eres médico"))
        assertTrue(prompt.contains("embarazo"))
        assertTrue(prompt.contains("trastorno de la conducta alimentaria"))
        assertTrue(prompt.contains("sin formato"))
        assertTrue(prompt.contains("Si no sabes algo"))
    }

    // KP2
    @Test
    fun referenceManAtModerateHighDeficitGivesAllHisData() {
        val prompt = KaiQPrompt.systemInstruction(referenceMan)

        assertTrue(prompt.contains("Datos del usuario"))
        assertTrue(prompt.contains("Hombre"))
        assertTrue(prompt.contains("30 años"))
        assertTrue(prompt.contains("180 cm"))
        assertTrue(prompt.contains("80 kg"))
        assertTrue(prompt.contains("16,2 %"))
        assertTrue(prompt.contains("US Navy"))
        assertTrue(prompt.contains("factor 1,55"))
        assertTrue(prompt.contains("2819 kcal/día"))
        assertTrue(prompt.contains("Katch-McArdle"))
        assertTrue(prompt.contains("déficit, -15 %"))
        assertTrue(prompt.contains("2396 kcal/día"))
        assertTrue(prompt.contains("210 g de proteína"))
        assertTrue(prompt.contains("67 g de grasa"))
        assertTrue(prompt.contains("238 g de carbohidratos"))
        assertTrue(prompt.contains("personalizar"))
    }

    // KP3
    @Test
    fun womanWithoutCircumferencesHasNoBodyFatAndUsesMifflin() {
        val woman = BodyMeasurements(Sex.FEMALE, ageYears = 30, heightCm = 165.0, weightKg = 60.5)
        val prompt = KaiQPrompt.systemInstruction(profileOf(woman, ActivityLevel.SEDENTARY, goalPercent = 10))

        assertTrue(prompt.contains("Mujer"))
        assertTrue(prompt.contains("60,5 kg"))
        assertTrue(prompt.contains("Grasa corporal: sin estimar"))
        assertTrue(prompt.contains("Mifflin-St Jeor"))
        assertTrue(prompt.contains("superávit, +10 %"))
        assertTrue(prompt.contains("factor 1,2"))
    }

    // KP4
    @Test
    fun textIsTheSameWhateverTheDefaultLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("es-ES"))
            val spanish = KaiQPrompt.systemInstruction(referenceMan)
            Locale.setDefault(Locale.US)
            val english = KaiQPrompt.systemInstruction(referenceMan)

            assertEquals(spanish, english)
            assertFalse(english.contains("16.2"))
            assertFalse(english.contains("1.55"))
        } finally {
            Locale.setDefault(original)
        }
    }
}
