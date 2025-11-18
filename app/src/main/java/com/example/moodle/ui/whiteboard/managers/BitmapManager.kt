package com.example.moodle.ui.whiteboard.managers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color

/**
 * Manager responsible for bitmap lifecycle and memory management
 */
class BitmapManager {
    
    var drawingBitmap: Bitmap? = null
        private set
    var drawingCanvas: Canvas? = null
        private set

    // Callbacks
    var onBitmapInitialized: ((Int, Int) -> Unit)? = null
    
    /**
     * Handle view size changes and create/recreate bitmap
     */
    fun onSizeChanged(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        
        try {
            // Use smaller canvas multiplier on Android 8+ to avoid hardware acceleration limits
            val multiplier = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                2f  // 2x screen size for Android 8+
            } else {
                3f  // 3x for older versions
            }
            
            val canvasWidth = (w * multiplier).toInt()
            val canvasHeight = (h * multiplier).toInt()
            
            // Calculate bitmap size in bytes
            val bitmapSizeBytes = canvasWidth.toLong() * canvasHeight.toLong() * 4L
            val maxSizeBytes = 100_000_000L // 100MB limit
            
            if (bitmapSizeBytes > maxSizeBytes) {
                android.util.Log.w("BitmapManager", "Bitmap too large ($bitmapSizeBytes bytes), using screen size")
                createScreenSizeBitmap(w, h)
            } else {
                // Clean up previous bitmap
                drawingBitmap?.recycle()
                
                drawingBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
                drawingCanvas = Canvas(drawingBitmap!!)
                drawingCanvas?.drawColor(Color.WHITE)
                
                onBitmapInitialized?.invoke(canvasWidth, canvasHeight)
            }
        } catch (e: OutOfMemoryError) {
            android.util.Log.e("BitmapManager", "OutOfMemoryError creating bitmap, falling back to screen size", e)
            createScreenSizeBitmap(w, h)
        } catch (e: RuntimeException) {
            android.util.Log.e("BitmapManager", "RuntimeException creating bitmap, falling back to screen size", e)
            createScreenSizeBitmap(w, h)
        }
    }
    
    /**
     * Create a fallback bitmap at screen size
     */
    private fun createScreenSizeBitmap(w: Int, h: Int) {
        drawingBitmap?.recycle()
        drawingBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawingCanvas = Canvas(drawingBitmap!!)
        drawingCanvas?.drawColor(Color.WHITE)
        
        onBitmapInitialized?.invoke(w, h)
    }
    
    /**
     * Get the drawing bitmap
     */
    fun getBitmap(): Bitmap? = drawingBitmap
    
    /**
     * Get the drawing canvas
     */
    fun getCanvas(): Canvas? = drawingCanvas
    
    /**
     * Get bitmap width
     */
    fun getWidth(): Float = drawingBitmap?.width?.toFloat() ?: 0f
    
    /**
     * Get bitmap height
     */
    fun getHeight(): Float = drawingBitmap?.height?.toFloat() ?: 0f
    
    /**
     * Check if bitmap is valid
     */
    fun isValid(): Boolean {
        return drawingBitmap != null && !drawingBitmap!!.isRecycled && 
               drawingBitmap!!.width > 0 && drawingBitmap!!.height > 0
    }
    
    /**
     * Clean up and recycle bitmap
     */
    fun cleanup() {
        drawingBitmap?.recycle()
        drawingBitmap = null
        drawingCanvas = null
    }
}

