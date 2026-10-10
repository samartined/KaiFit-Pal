package com.tfg.kaifit_pal.data.llm

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * A document picked with the system file picker (Storage Access Framework).
 *
 * The name and size are read from the provider the first time they are asked for, which the
 * model manager does on a background thread. Providers may not report them, so both can be
 * `null`.
 */
class UriImportSource(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
) : ImportSource {

    private val metadata: Pair<String?, Long?> by lazy { queryMetadata() }

    override val displayName: String?
        get() = metadata.first

    override val sizeBytes: Long?
        get() = metadata.second

    /** @throws FileNotFoundException if the provider cannot open the document. */
    override fun open(): InputStream =
        contentResolver.openInputStream(uri) ?: throw FileNotFoundException("Cannot open $uri")

    private fun queryMetadata(): Pair<String?, Long?> {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return null to null
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            val name = if (nameIndex >= 0 && !cursor.isNull(nameIndex)) cursor.getString(nameIndex) else null
            val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
            return name to size?.takeIf { it >= 0 }
        }
        return null to null
    }
}
