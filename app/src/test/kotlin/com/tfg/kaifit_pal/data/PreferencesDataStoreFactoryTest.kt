package com.tfg.kaifit_pal.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PreferencesDataStoreFactoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun corruptedFileIsReadAsEmptyPreferences() = runTest {
        val file = tmp.newPreferencesFile()
        file.writeBytes("not a protobuf".toByteArray())

        val preferences = backgroundScope.preferencesDataStoreOn(file).data.first()

        assertTrue(preferences.asMap().isEmpty())
    }

    @Test
    fun corruptedFileIsReplacedAndWritableAgain() = runTest {
        val file = tmp.newPreferencesFile()
        file.writeBytes("not a protobuf".toByteArray())
        val key = intPreferencesKey("answer")
        val firstScope = closeableScope()
        firstScope.preferencesDataStoreOn(file).edit { it[key] = 42 }
        firstScope.close()

        val reopened = backgroundScope.preferencesDataStoreOn(file).data.first()

        assertEquals(42, reopened[key])
    }
}
