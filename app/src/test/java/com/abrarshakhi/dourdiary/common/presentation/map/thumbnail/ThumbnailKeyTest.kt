package com.abrarshakhi.dourdiary.common.presentation.map.thumbnail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThumbnailKeyTest {

    @Test
    fun `the same card asks for the same image twice`() {
        assertEquals(
            thumbnailKey("7", isDark = false, widthPx = 672, heightPx = 420),
            thumbnailKey("7", isDark = false, widthPx = 672, heightPx = 420),
        )
    }

    @Test
    fun `different runs never share an image`() {
        assertNotEquals(
            thumbnailKey("7", isDark = false, widthPx = 672, heightPx = 420),
            thumbnailKey("8", isDark = false, widthPx = 672, heightPx = 420),
        )
    }

    @Test
    fun `a theme change renders a new image rather than reusing the old one`() {
        assertNotEquals(
            thumbnailKey("7", isDark = false, widthPx = 672, heightPx = 420),
            thumbnailKey("7", isDark = true, widthPx = 672, heightPx = 420),
        )
    }

    @Test
    fun `sizes a few pixels apart share one image`() {
        val key = thumbnailKey("7", isDark = false, widthPx = 660, heightPx = 420)

        listOf(641, 650, 660, 672).forEach { width ->
            assertEquals(
                "width $width should share a bucket with 660",
                key,
                thumbnailKey("7", isDark = false, widthPx = width, heightPx = 420),
            )
        }
    }

    @Test
    fun `a genuinely different size gets its own image`() {
        assertNotEquals(
            thumbnailKey("7", isDark = false, widthPx = 660, heightPx = 420),
            thumbnailKey("7", isDark = false, widthPx = 1_080, heightPx = 420),
        )
    }

    @Test
    fun `buckets round up and never shrink a size`() {
        assertEquals(0, bucket(0))
        assertEquals(32, bucket(1))
        assertEquals(32, bucket(32))
        assertEquals(64, bucket(33))
        (1..500).forEach { pixels ->
            assertTrue("bucket($pixels) shrank", bucket(pixels) >= pixels)
        }
    }

    @Test
    fun `a negative size does not produce a negative bucket`() {
        assertEquals(0, bucket(-10))
    }

    @Test
    fun `the key is safe to use as a file name`() {
        val key = thumbnailKey("42", isDark = true, widthPx = 672, heightPx = 420)

        assertTrue("Unsafe characters in $key", key.all { it.isLetterOrDigit() || it in "-_" })
    }

    @Test
    fun `the format version is part of the key so old images can be retired`() {
        assertTrue(thumbnailKey("1", false, 100, 100).startsWith("v"))
    }
}
