package com.tfg.kaifit_pal.testing

import com.tfg.kaifit_pal.data.llm.ImportSource
import com.tfg.kaifit_pal.data.llm.ModelSpec
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.random.Random

/**
 * A file to import made of [bytes] in memory.
 *
 * @param failAfterBytes when not null, reading fails with an [IOException] once this many bytes
 *   have been read, as a storage provider that goes away halfway.
 * @param onRead called after each read with the bytes read so far.
 */
class FakeImportSource(
    private val bytes: ByteArray,
    override val sizeBytes: Long? = bytes.size.toLong(),
    override val displayName: String? = "gemma.litertlm",
    private val failAfterBytes: Long? = null,
    private val onRead: (Long) -> Unit = {},
) : ImportSource {

    /** How many times the file was opened. */
    var openCount = 0
        private set

    override fun open(): InputStream {
        openCount++
        return SourceStream(ByteArrayInputStream(bytes))
    }

    private inner class SourceStream(private val input: ByteArrayInputStream) : InputStream() {

        private var readSoFar = 0L

        override fun read(): Int {
            val one = ByteArray(1)
            return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val limit = failAfterBytes
            if (limit != null && readSoFar >= limit) throw IOException("The provider went away")
            val allowed = if (limit == null) len else minOf(len.toLong(), limit - readSoFar).toInt()
            val count = input.read(b, off, allowed)
            if (count > 0) readSoFar += count
            onRead(readSoFar)
            return count
        }
    }

    companion object {
        /** A valid `.litertlm` header followed by [payloadBytes] pseudo-random bytes. */
        fun modelBytes(payloadBytes: Int): ByteArray =
            ModelSpec.MAGIC.toByteArray(Charsets.US_ASCII) + Random(seed = 42).nextBytes(payloadBytes)
    }
}
