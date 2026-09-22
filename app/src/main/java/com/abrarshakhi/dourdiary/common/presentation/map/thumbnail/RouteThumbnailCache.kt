package com.abrarshakhi.dourdiary.common.presentation.map.thumbnail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

class RouteThumbnailCache(
    private val directory: File,
    private val ioDispatcher: CoroutineDispatcher,
    maxMemoryBytes: Int = defaultMemoryBudget(),
) {
    private val memory = object : LruCache<String, Bitmap>(maxMemoryBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private val renderSlots = Semaphore(MaxConcurrentRenders)

    private val inFlight = mutableMapOf<String, Mutex>()
    private val inFlightGuard = Mutex()

    fun peek(key: String): Bitmap? = memory.get(key)

    suspend fun getOrRender(key: String, render: suspend () -> Bitmap?): Bitmap? {
        memory.get(key)?.let { return it }

        val lock = inFlightGuard.withLock { inFlight.getOrPut(key) { Mutex() } }

        return lock.withLock {
            memory.get(key)?.let { return@withLock it }

            readFromDisk(key)?.let { fromDisk ->
                memory.put(key, fromDisk)
                return@withLock fromDisk
            }

            val rendered = renderSlots.withPermit { render() } ?: return@withLock null
            memory.put(key, rendered)
            writeToDisk(key, rendered)
            rendered
        }.also {
            inFlightGuard.withLock { inFlight.remove(key) }
        }
    }

    suspend fun evictRoute(routeId: String) = withContext(ioDispatcher) {
        val marker = "-$routeId-"
        memory.snapshot().keys.filter { it.contains(marker) }.forEach { memory.remove(it) }
        runCatching {
            directory.listFiles()?.filter { it.name.contains(marker) }?.forEach { it.delete() }
        }
        Unit
    }

    suspend fun clear() = withContext(ioDispatcher) {
        memory.evictAll()
        runCatching { directory.listFiles()?.forEach { it.delete() } }
        Unit
    }

    private suspend fun readFromDisk(key: String): Bitmap? = withContext(ioDispatcher) {
        runCatching {
            val file = fileFor(key)
            if (!file.exists()) return@runCatching null
            BitmapFactory.decodeFile(file.absolutePath)
        }.getOrNull()
    }

    private suspend fun writeToDisk(key: String, bitmap: Bitmap) = withContext(ioDispatcher) {
        runCatching {
            if (!directory.exists()) directory.mkdirs()
            val temporary = File(directory, "${fileFor(key).name}.tmp")
            temporary.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            temporary.renameTo(fileFor(key))
        }
        Unit
    }

    private fun fileFor(key: String) = File(directory, "$key.png")

    companion object {
        private const val MaxConcurrentRenders = 2

        fun defaultMemoryBudget(): Int =
            (Runtime.getRuntime().maxMemory() / MemoryBudgetDivisor).toInt()

        private const val MemoryBudgetDivisor = 12L
    }
}
