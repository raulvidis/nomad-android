package com.nomad.android.data.ai

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GgufMetadataTest {

    /** Build a minimal valid GGUF v3 with one string metadata key. */
    private fun writeGguf(file: File, arch: String) {
        val key = "general.architecture".toByteArray(Charsets.UTF_8)
        val value = arch.toByteArray(Charsets.UTF_8)
        val buf = ByteBuffer.allocate(4 + 4 + 8 + 8 + (8 + key.size) + 4 + (8 + value.size))
            .order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(0x46554747)          // "GGUF"
        buf.putInt(3)                   // version
        buf.putLong(0)                  // tensor_count
        buf.putLong(1)                  // metadata_count
        buf.putLong(key.size.toLong()); buf.put(key)
        buf.putInt(8)                   // T_STRING
        buf.putLong(value.size.toLong()); buf.put(value)
        file.writeBytes(buf.array())
    }

    @Test
    fun `parses architecture from a valid gguf header`() {
        val f = File.createTempFile("model", ".gguf").apply { deleteOnExit() }
        writeGguf(f, "llama")
        val header = GgufMetadata.read(f)
        assertNotNull(header)
        assertEquals("llama", header!!.architecture)
        assertEquals(GgufMetadata.Verdict.Ok, GgufMetadata.verdict(header))
    }

    @Test
    fun `rejects a file with a bad magic`() {
        val f = File.createTempFile("bad", ".gguf").apply { deleteOnExit() }
        f.writeBytes(ByteArray(64) { 0 })
        assertNull(GgufMetadata.read(f))
    }
}
