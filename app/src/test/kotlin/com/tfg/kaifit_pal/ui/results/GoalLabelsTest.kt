package com.tfg.kaifit_pal.ui.results

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tfg.kaifit_pal.domain.GoalModifier
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoalLabelsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun everyModifierHasItsOwnLabel() {
        val labels = GoalModifier.all.map { context.getString(goalLabelRes(it)) }

        assertEquals(
            listOf(
                "Déficit intenso",
                "Déficit moderado-intenso",
                "Déficit moderado",
                "Déficit ligero",
                "Mantenimiento",
                "Volumen ligero",
                "Volumen moderado",
                "Volumen moderado-intenso",
                "Volumen intenso",
            ),
            labels,
        )
        assertEquals(9, GoalModifier.all.map(::goalLabelRes).toSet().size)
        assertEquals("Déficit moderado-intenso", context.getString(goalLabelRes(GoalModifier(-15))))
        assertEquals("Mantenimiento", context.getString(goalLabelRes(GoalModifier(0))))
        assertEquals("Volumen intenso", context.getString(goalLabelRes(GoalModifier(20))))
    }
}
