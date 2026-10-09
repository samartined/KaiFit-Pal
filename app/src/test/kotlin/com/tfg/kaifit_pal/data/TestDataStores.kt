package com.tfg.kaifit_pal.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestScope
import org.junit.rules.TemporaryFolder
import java.io.File

/** A file for a Preferences DataStore in a new, empty folder; it does not exist yet. */
internal fun TemporaryFolder.newPreferencesFile(): File = File(newFolder(), "test.preferences_pb")

/** A Preferences DataStore on [file] that lives as long as this scope. */
internal fun CoroutineScope.preferencesDataStoreOn(file: File): DataStore<Preferences> =
    createPreferencesDataStore(this) { file }

/**
 * A scope that the test closes with [close] before opening the same file again. It runs on the
 * test scheduler and is a child of the background scope, so it never outlives the test.
 */
internal fun TestScope.closeableScope(): CoroutineScope =
    CoroutineScope(backgroundScope.coroutineContext + Job(parent = backgroundScope.coroutineContext.job))

/** Cancels this scope and waits until its DataStores have released their files. */
internal suspend fun CoroutineScope.close() {
    coroutineContext.job.cancelAndJoin()
}

/** A DataStore whose reads and writes always fail with [error]. */
internal class FailingDataStore(private val error: Throwable) : DataStore<Preferences> {

    override val data: Flow<Preferences> = flow { throw error }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw error
}
