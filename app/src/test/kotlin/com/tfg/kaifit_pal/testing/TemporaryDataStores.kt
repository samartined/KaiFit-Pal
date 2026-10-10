package com.tfg.kaifit_pal.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.tfg.kaifit_pal.data.close
import com.tfg.kaifit_pal.data.newPreferencesFile
import com.tfg.kaifit_pal.data.preferencesDataStoreOn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Preferences DataStores on new temporary files, for tests that run outside `runTest`, such as
 * the Robolectric screen tests.
 *
 * They work on [Dispatchers.IO], as in the app. After each test their scope is cancelled, so no
 * DataStore stays open on its file, and then the files are deleted.
 */
class TemporaryDataStores : TestRule {

    private val folder = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** A DataStore on a file of its own. */
    fun newDataStore(): DataStore<Preferences> = scope.preferencesDataStoreOn(folder.newPreferencesFile())

    override fun apply(base: Statement, description: Description): Statement {
        val closingStatement = object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } finally {
                    runBlocking { scope.close() }
                }
            }
        }
        return folder.apply(closingStatement, description)
    }
}
