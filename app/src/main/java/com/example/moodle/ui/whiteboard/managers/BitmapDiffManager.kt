package com.example.moodle.ui.whiteboard.managers

import android.graphics.Bitmap
import android.util.Log
import java.nio.ByteBuffer

/**
 * Manages bitmap diff calculation and application for efficient undo/redo
 * Stores only changed regions instead of full bitmaps to reduce memory usage
 */
class BitmapDiffManager {
    
    companion object {
        private const val TAG = "BitmapDiffManager"
        private const val FULL_BITMAP_THRESHOLD = 0.3f // If >30% changed, store full bitmap
    }
    
    // Performance mode
    private var performanceMode = PerformanceMode.BALANCED
    
    enum class PerformanceMode {
        FAST,      // Larger tiles, more sampling - fastest, less accurate
        BALANCED,  // Default settings
        QUALITY    // Smaller tiles, less sampling - slower, more accurate
    }
    
    fun setPerformanceMode(mode: PerformanceMode) {
        performanceMode = mode
    }
    
    private fun getTileSize(): Int {
        return when (performanceMode) {
            PerformanceMode.FAST -> 256
            PerformanceMode.BALANCED -> 128
            PerformanceMode.QUALITY -> 64
        }
    }
    
    private fun getSampleRate(): Int {
        return when (performanceMode) {
            PerformanceMode.FAST -> 16
            PerformanceMode.BALANCED -> 8
            PerformanceMode.QUALITY -> 4
        }
    }
    
    /**
     * Represents a bitmap diff containing only changed regions
     */
    data class BitmapDiff(
        val width: Int,
        val height: Int,
        val changedTiles: List<TileDiff>,
        val isFullBitmap: Boolean = false,
        val fullBitmapData: ByteArray? = null
    ) {
        /**
         * Get estimated memory usage in bytes
         */
        fun getMemoryUsage(): Long {
            return if (isFullBitmap && fullBitmapData != null) {
                fullBitmapData.size.toLong()
            } else {
                changedTiles.sumOf { it.data.size.toLong() }
            }
        }
    }
    
    /**
     * Represents a single changed tile in the bitmap
     */
    data class TileDiff(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val data: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as TileDiff
            
            if (x != other.x) return false
            if (y != other.y) return false
            if (width != other.width) return false
            if (height != other.height) return false
            if (!data.contentEquals(other.data)) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = x
            result = 31 * result + y
            result = 31 * result + width
            result = 31 * result + height
            result = 31 * result + data.contentHashCode()
            return result
        }
    }
    
    /**
     * Calculate diff between two bitmaps
     * Returns null if bitmaps are identical or incompatible
     */
    fun calculateDiff(oldBitmap: Bitmap?, newBitmap: Bitmap?): BitmapDiff? {
        // Handle null cases
        if (newBitmap == null) return null
        if (oldBitmap == null) {
            // First bitmap, store as full
            return createFullBitmapDiff(newBitmap)
        }
        
        // Check if bitmaps are compatible
        if (oldBitmap.width != newBitmap.width || oldBitmap.height != newBitmap.height) {
            Log.w(TAG, "Bitmap dimensions don't match, storing full bitmap")
            return createFullBitmapDiff(newBitmap)
        }
        
        if (oldBitmap.config != newBitmap.config) {
            Log.w(TAG, "Bitmap configs don't match, storing full bitmap")
            return createFullBitmapDiff(newBitmap)
        }
        
        try {
            val width = newBitmap.width
            val height = newBitmap.height
            
            val tileSize = getTileSize()
            
            // Calculate number of tiles
            val tilesX = (width + tileSize - 1) / tileSize
            val tilesY = (height + tileSize - 1) / tileSize
            val totalTiles = tilesX * tilesY
            
            val changedTiles = mutableListOf<TileDiff>()
            
            // Compare tiles
            for (tileY in 0 until tilesY) {
                for (tileX in 0 until tilesX) {
                    val x = tileX * tileSize
                    val y = tileY * tileSize
                    val tileWidth = minOf(tileSize, width - x)
                    val tileHeight = minOf(tileSize, height - y)
                    
                    if (isTileChanged(oldBitmap, newBitmap, x, y, tileWidth, tileHeight)) {
                        val tileDiff = extractTile(newBitmap, x, y, tileWidth, tileHeight)
                        if (tileDiff != null) {
                            changedTiles.add(tileDiff)
                        }
                    }
                }
            }
            
            // If too many tiles changed, store full bitmap instead
            val changeRatio = changedTiles.size.toFloat() / totalTiles
            if (changeRatio > FULL_BITMAP_THRESHOLD) {
                Log.d(TAG, "Change ratio $changeRatio > threshold, storing full bitmap")
                return createFullBitmapDiff(newBitmap)
            }
            
            Log.d(TAG, "Diff calculated: ${changedTiles.size}/$totalTiles tiles changed (${String.format("%.1f", changeRatio * 100)}%)")
            
            return BitmapDiff(
                width = width,
                height = height,
                changedTiles = changedTiles,
                isFullBitmap = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating diff", e)
            return createFullBitmapDiff(newBitmap)
        }
    }
    
    /**
     * Apply diff to a bitmap
     * Returns a new bitmap with the diff applied
     */
    fun applyDiff(baseBitmap: Bitmap?, diff: BitmapDiff?): Bitmap? {
        if (diff == null) return null
        
        try {
            // Handle full bitmap case
            if (diff.isFullBitmap && diff.fullBitmapData != null) {
                return decodeBitmapFromBytes(diff.fullBitmapData, diff.width, diff.height)
            }
            
            // Create a mutable copy of the base bitmap or a new blank one
            val resultBitmap = if (baseBitmap != null && 
                                   baseBitmap.width == diff.width && 
                                   baseBitmap.height == diff.height) {
                baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                Bitmap.createBitmap(diff.width, diff.height, Bitmap.Config.ARGB_8888)
            }
            
            // Apply each tile diff
            for (tileDiff in diff.changedTiles) {
                applyTileDiff(resultBitmap, tileDiff)
            }
            
            return resultBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error applying diff", e)
            return null
        }
    }
    
    /**
     * Check if a tile has changed between two bitmaps
     */
    private fun isTileChanged(
        oldBitmap: Bitmap,
        newBitmap: Bitmap,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): Boolean {
        try {
            val sampleRate = getSampleRate()
            // Sample pixels for quick comparison
            for (py in y until y + height step sampleRate) {
                for (px in x until x + width step sampleRate) {
                    if (px < oldBitmap.width && py < oldBitmap.height &&
                        px < newBitmap.width && py < newBitmap.height) {
                        if (oldBitmap.getPixel(px, py) != newBitmap.getPixel(px, py)) {
                            return true
                        }
                    }
                }
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking tile change", e)
            return true // Assume changed on error
        }
    }
    
    /**
     * Extract a tile from a bitmap as TileDiff
     */
    private fun extractTile(
        bitmap: Bitmap,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): TileDiff? {
        try {
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, x, y, width, height)
            
            // Convert to byte array
            val buffer = ByteBuffer.allocate(pixels.size * 4)
            for (pixel in pixels) {
                buffer.putInt(pixel)
            }
            
            return TileDiff(
                x = x,
                y = y,
                width = width,
                height = height,
                data = buffer.array()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting tile", e)
            return null
        }
    }
    
    /**
     * Apply a tile diff to a bitmap
     */
    private fun applyTileDiff(bitmap: Bitmap, tileDiff: TileDiff) {
        try {
            val buffer = ByteBuffer.wrap(tileDiff.data)
            val pixels = IntArray(tileDiff.width * tileDiff.height)
            
            for (i in pixels.indices) {
                pixels[i] = buffer.getInt()
            }
            
            bitmap.setPixels(
                pixels,
                0,
                tileDiff.width,
                tileDiff.x,
                tileDiff.y,
                tileDiff.width,
                tileDiff.height
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error applying tile diff", e)
        }
    }
    
    /**
     * Create a full bitmap diff
     */
    private fun createFullBitmapDiff(bitmap: Bitmap): BitmapDiff {
        try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            
            val buffer = ByteBuffer.allocate(pixels.size * 4)
            for (pixel in pixels) {
                buffer.putInt(pixel)
            }
            
            return BitmapDiff(
                width = bitmap.width,
                height = bitmap.height,
                changedTiles = emptyList(),
                isFullBitmap = true,
                fullBitmapData = buffer.array()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating full bitmap diff", e)
            return BitmapDiff(
                width = bitmap.width,
                height = bitmap.height,
                changedTiles = emptyList(),
                isFullBitmap = true,
                fullBitmapData = null
            )
        }
    }
    
    /**
     * Decode bitmap from byte array
     */
    private fun decodeBitmapFromBytes(data: ByteArray, width: Int, height: Int): Bitmap? {
        try {
            val buffer = ByteBuffer.wrap(data)
            val pixels = IntArray(width * height)
            
            for (i in pixels.indices) {
                pixels[i] = buffer.getInt()
            }
            
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding bitmap from bytes", e)
            return null
        }
    }
}

