package com.abrarshakhi.dourdiary.common.presentation

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abrarshakhi.dourdiary.common.presentation.map.thumbnail.RouteThumbnailCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class RouteThumbnailCacheTest {

    private lateinit var directory: File
    private lateinit var cache: RouteThumbnailCache

    @Before
    fun setUp() {
        directory = File.createTempFile("thumbs", "").let { file ->
            file.delete()
            file.also { it.mkdirs() }
        }
        cache = RouteThumbnailCache(directory = directory, ioDispatcher = Dispatchers.IO)
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun bitmap(color: Int = Color.RED): Bitmap =
        Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    @Test
    fun a_rendered_image_is_only_rendered_once() = runBlocking {
        val renders = AtomicInteger()

        repeat(3) {
            cache.getOrRender("key-a") { renders.incrementAndGet(); bitmap() }
        }

        assertEquals("Rendered more than once for the same key", 1, renders.get())
    }

    @Test
    fun a_cached_image_survives_a_new_cache_over_the_same_directory() = runBlocking {
        cache.getOrRender("key-b") { bitmap() }

        val reopened = RouteThumbnailCache(directory = directory, ioDispatcher = Dispatchers.IO)
        val renders = AtomicInteger()
        val fromDisk = reopened.getOrRender("key-b") { renders.incrementAndGet(); bitmap() }

        assertNotNull("Nothing came back from disk", fromDisk)
        assertEquals("Re-rendered instead of reading the file", 0, renders.get())
    }

    @Test
    fun two_cards_asking_at_once_only_render_one_image() = runBlocking {
        val renders = AtomicInteger()

        coroutineScope {
            val requests = (1..6).map {
                async {
                    cache.getOrRender("key-c") {
                        renders.incrementAndGet()
                        delay(150)
                        bitmap()
                    }
                }
            }
            requests.forEach { assertNotNull(it.await()) }
        }

        assertEquals("Duplicated work for one key", 1, renders.get())
    }

    @Test
    fun a_failed_render_returns_null_so_the_caller_can_fall_back() = runBlocking {
        val result = cache.getOrRender("key-d") { null }

        assertNull(result)
    }

    @Test
    fun a_failed_render_is_not_cached_as_a_failure() = runBlocking {
        cache.getOrRender("key-e") { null }

        val second = cache.getOrRender("key-e") { bitmap() }

        assertNotNull("A transient failure was remembered forever", second)
    }

    @Test
    fun peeking_finds_an_image_already_in_memory() = runBlocking {
        assertNull(cache.peek("key-f"))

        cache.getOrRender("key-f") { bitmap() }

        assertNotNull("A scroll back up would re-render", cache.peek("key-f"))
    }

    @Test
    fun clearing_removes_both_levels() = runBlocking {
        cache.getOrRender("key-g") { bitmap() }
        assertTrue(directory.listFiles().orEmpty().isNotEmpty())

        cache.clear()

        assertNull(cache.peek("key-g"))
        assertTrue(
            "Files outlived the clear",
            directory.listFiles().orEmpty().none { it.name.startsWith("key-g") },
        )
    }

    @Test
    fun deleting_a_run_takes_its_images_with_it_in_every_theme() = runBlocking {
        cache.getOrRender("v1-42-light-640x288") { bitmap() }
        cache.getOrRender("v1-42-dark-640x288") { bitmap() }
        cache.getOrRender("v1-43-light-640x288") { bitmap() }

        cache.evictRoute("42")

        assertNull(cache.peek("v1-42-light-640x288"))
        assertNull(cache.peek("v1-42-dark-640x288"))
        assertNotNull("Evicted an unrelated run", cache.peek("v1-43-light-640x288"))
        assertTrue(
            "Files for the deleted run outlived it",
            directory.listFiles().orEmpty().none { it.name.contains("-42-") },
        )
        assertTrue(
            "Evicting one run removed another run's file",
            directory.listFiles().orEmpty().any { it.name.contains("-43-") },
        )
    }

    @Test
    fun no_partial_files_are_left_behind_to_be_served_later() = runBlocking {
        cache.getOrRender("key-h") { bitmap() }

        assertTrue(
            "A temporary write file was left where it could be decoded as real",
            directory.listFiles().orEmpty().none { it.name.endsWith(".tmp") },
        )
    }
}
