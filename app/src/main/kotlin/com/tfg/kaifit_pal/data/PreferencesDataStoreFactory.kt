package com.tfg.kaifit_pal.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import java.io.File

/**
 * Creates a Preferences [DataStore] that lives in [scope] and stores its data in the file given
 * by [produceFile], whose name must end in `.preferences_pb`.
 *
 * A damaged file must not stop the app from starting, so it is replaced with empty preferences:
 * its content is lost and every repository falls back to its defaults. There are no migrations,
 * because the original app did not store anything.
 *
 * Only one instance may be active per file and process: create each one once, in the app
 * container, and cancel [scope] before opening the same file again.
 */
internal fun createPreferencesDataStore(scope: CoroutineScope, produceFile: () -> File): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope,
        produceFile = produceFile,
    )

/**
 * The value stored under [key], or `null` when there is none or it has another type.
 *
 * `preferences[key]` would fail with a [ClassCastException] on a value of another type, written
 * for example by an older version of the app under the same name; stored data must never crash
 * the app.
 */
internal inline fun <reified T : Any> Preferences.getOrNull(key: Preferences.Key<T>): T? = asMap()[key] as? T
