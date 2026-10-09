package com.tfg.kaifit_pal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tfg.kaifit_pal.data.close
import com.tfg.kaifit_pal.data.closeableScope
import com.tfg.kaifit_pal.data.settings.ThemeMode
import com.tfg.kaifit_pal.domain.ActivityLevel
import com.tfg.kaifit_pal.domain.BodyMeasurements
import com.tfg.kaifit_pal.domain.Goal
import com.tfg.kaifit_pal.domain.GoalModifier
import com.tfg.kaifit_pal.domain.MacroPlan
import com.tfg.kaifit_pal.domain.Sex
import com.tfg.kaifit_pal.domain.TdeeCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AppContainerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Only checks instances: writing here would leave a DataStore open on the app-wide scope. */
    @Test
    fun applicationKeepsOneContainerWithOneInstanceOfEachRepository() {
        assertTrue(context is KaiFitApp)
        val app = context as KaiFitApp

        val container = app.container

        assertSame(container, app.container)
        assertSame(container.settingsRepository, container.settingsRepository)
        assertSame(container.profileRepository, container.profileRepository)
    }

    @Test
    fun clearingTheProfileKeepsTheSettingsInTheirOwnFile() = runTest {
        val scope = closeableScope()
        try {
            val container = AppContainer(context, scope)
            val measurements = BodyMeasurements(Sex.MALE, ageYears = 30, heightCm = 180.0, weightKg = 80.0)
            val tdee = TdeeCalculator.calculate(measurements, ActivityLevel.MODERATE)!!
            container.settingsRepository.setThemeMode(ThemeMode.DARK)
            container.profileRepository.save(
                measurements,
                ActivityLevel.MODERATE,
                tdee,
                GoalModifier.MAINTENANCE,
                MacroPlan.forGoal(tdee.tdeeKcal, Goal.MAINTENANCE),
            )
            assertNotNull(container.profileRepository.profile.first())

            container.profileRepository.clear()

            assertNull(container.profileRepository.profile.first())
            assertEquals(ThemeMode.DARK, container.settingsRepository.settings.first().themeMode)
            assertTrue(File(context.filesDir, "datastore/profile.preferences_pb").isFile)
            assertTrue(File(context.filesDir, "datastore/settings.preferences_pb").isFile)
        } finally {
            scope.close()
        }
    }
}
