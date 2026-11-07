// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.data.session.resolver

import java.io.IOException
import java.util.LinkedHashMap
import kotlin.math.min

private const val RANGE_CACHE_AVAILABLE_HEAP_DIVISOR = 8L

/**
 * Coalesces small positional reads into bounded, reusable in-memory blocks.
 */
internal class SeekableBlockCache(
    private val contentLength: Long,
    private val blockSize: Int,
    maxCachedBytes: Int,
    private val loadBlock: (offset: Long, size: Int) -> ByteArray,
) {
    private val maxCachedBlocks = (maxCachedBytes / blockSize).coerceAtLeast(1)

    /** Guards [blocks] and [blockLocks]; held only for map operations, never during a load. */
    private val mapLock = Any()
    private val blocks = LinkedHashMap<Long, ByteArray>(maxCachedBlocks, 0.75f, true)

    /** One monitor per block offset so a slow fetch only blocks readers of that same block. */
    private val blockLocks = HashMap<Long, Any>()

    init {
        require(contentLength > 0L) { "contentLength must be positive" }
        require(blockSize > 0) { "blockSize must be positive" }
        require(maxCachedBytes > 0) { "maxCachedBytes must be positive" }
    }

    fun read(offset: Long, requestedSize: Int, destination: ByteArray): Int {
        require(offset >= 0L) { "offset must be non-negative" }
        require(requestedSize >= 0) { "requestedSize must be non-negative" }
        require(destination.size >= requestedSize) { "destination is smaller than requestedSize" }
        if (offset >= contentLength || requestedSize == 0) return 0

        val targetSize = min(requestedSize.toLong(), contentLength - offset).toInt()
        var copied = 0
        while (copied < targetSize) {
            val position = offset + copied
            val blockOffset = position % blockSize
            val blockStart = position - blockOffset
            val block = blockAt(blockStart)
            val count = min(targetSize - copied, block.size - blockOffset.toInt())
            if (count <= 0) throw IOException("Cached range block cannot satisfy offset $position")
            block.copyInto(destination, copied, blockOffset.toInt(), blockOffset.toInt() + count)
            copied += count
        }
        return copied
    }

    fun clear() {
        synchronized(mapLock) {
            blocks.clear()
            blockLocks.clear()
        }
    }

    /**
     * Returns the cached block at [offset], loading it while holding only that block's monitor.
     *
     * The previous implementation synchronised the whole [read], so one 1 MiB network fetch blocked
     * every other reader (and the UI's random access pattern serialised completely).
     */
    private fun blockAt(offset: Long): ByteArray {
        synchronized(mapLock) { blocks[offset] }?.let { return it }
        val blockLock = synchronized(mapLock) {
            // Keep the per-offset monitors proportional to the block cache, never to the file size.
            if (blockLocks.size > maxCachedBlocks * 4) {
                blockLocks.keys.retainAll(blocks.keys)
            }
            blockLocks.getOrPut(offset) { Any() }
        }
        synchronized(blockLock) {
            var cached: ByteArray? = null
            synchronized(mapLock) { cached = blocks[offset] }
            cached?.let { return it }
            val loaded = fetchBlock(offset)
            synchronized(mapLock) { cache(offset, loaded) }
            return loaded
        }
    }

    /** Must be called while holding [mapLock]. */
    private fun cache(offset: Long, block: ByteArray) {
        if (blocks.size >= maxCachedBlocks) {
            val eldest = blocks.entries.iterator()
            if (eldest.hasNext()) {
                eldest.next()
                eldest.remove()
            }
        }
        blocks[offset] = block
    }

    private fun fetchBlock(offset: Long): ByteArray {
        val expectedSize = min(blockSize.toLong(), contentLength - offset).toInt()
        val data = loadBlock(offset, expectedSize)
        if (data.size != expectedSize) {
            throw IOException(
                "Incomplete range block: offset=$offset, expected=$expectedSize, actual=${data.size}",
            )
        }
        return data
    }
}

internal fun calculateRuntimeRangeCacheBudget(
    maximumBytes: Int,
    maxHeapBytes: Long,
    allocatedHeapBytes: Long,
    blockSize: Int,
): Int {
    require(maximumBytes > 0) { "maximumBytes must be positive" }
    require(maxHeapBytes > 0L) { "maxHeapBytes must be positive" }
    require(allocatedHeapBytes >= 0L) { "allocatedHeapBytes must be non-negative" }
    require(blockSize > 0) { "blockSize must be positive" }
    require(maximumBytes >= blockSize) { "maximumBytes must cover one block" }

    val availableHeapBytes = (maxHeapBytes - allocatedHeapBytes).coerceAtLeast(blockSize.toLong())
    // Asset parsing, decoded resources, networking, and UI retain the rest of the available heap.
    val safeBudgetBytes = (availableHeapBytes / RANGE_CACHE_AVAILABLE_HEAP_DIVISOR)
        .coerceAtLeast(blockSize.toLong())
    return min(maximumBytes.toLong(), safeBudgetBytes)
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()
}
