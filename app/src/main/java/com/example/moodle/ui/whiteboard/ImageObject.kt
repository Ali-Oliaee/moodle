package com.example.moodle.ui.whiteboard

import android.graphics.*
import kotlin.math.*

/**
 * Data class representing an image object on the whiteboard
 */
data class ImageObject(
    var bitmap: Bitmap,
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var rotation: Float = 0f,
    var scaleX: Float = 1f,
    var scaleY: Float = 1f,
    var isSelected: Boolean = false
) {
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    
    private val selectionPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
    }
    
    private val handlePaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }
    
    private val rotationHandlePaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }
    
    private val deleteButtonPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }
    
    private val deleteIconPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }
    
    fun getBounds(): RectF {
        val scaledWidth = width * scaleX
        val scaledHeight = height * scaleY
        return RectF(x, y, x + scaledWidth, y + scaledHeight)
    }
    
    fun getCenterX(): Float {
        val scaledWidth = width * scaleX
        return x + scaledWidth / 2f
    }
    
    fun getCenterY(): Float {
        val scaledHeight = height * scaleY
        return y + scaledHeight / 2f
    }
    
    /**
     * Transform touch coordinates to account for image rotation
     */
    private fun transformTouchPoint(touchX: Float, touchY: Float): Pair<Float, Float> {
        if (rotation == 0f) {
            return Pair(touchX, touchY)
        }
        
        val centerX = getCenterX()
        val centerY = getCenterY()
        val angle = -Math.toRadians(rotation.toDouble())
        val cos = cos(angle).toFloat()
        val sin = sin(angle).toFloat()
        
        val dx = touchX - centerX
        val dy = touchY - centerY
        
        val transformedX = centerX + dx * cos - dy * sin
        val transformedY = centerY + dx * sin + dy * cos
        
        return Pair(transformedX, transformedY)
    }
    
    fun draw(canvas: Canvas) {
        canvas.save()
        
        // Apply rotation around center
        if (rotation != 0f) {
            canvas.rotate(rotation, getCenterX(), getCenterY())
        }
        
        // Draw the bitmap with scaling
        if (!bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0) {
            try {
                val destRect = RectF(x, y, x + width * scaleX, y + height * scaleY)
                canvas.drawBitmap(bitmap, null, destRect, paint)
            } catch (e: Exception) {
                android.util.Log.e("WhiteboardView", "Error drawing image object bitmap", e)
            }
        }
        
        canvas.restore()
        
        if (isSelected) {
            drawSelectionBounds(canvas)
        }
    }
    
    private fun drawSelectionBounds(canvas: Canvas) {
        val bounds = getBounds()
        
        canvas.save()
        
        // Apply rotation for selection bounds
        if (rotation != 0f) {
            canvas.rotate(rotation, getCenterX(), getCenterY())
        }
        
        // Draw selection rectangle
        canvas.drawRect(bounds, selectionPaint)
        
        // Draw corner resize handles
        val handleSize = 20f
        
        // Top-left handle
        canvas.drawRect(bounds.left - handleSize/2, bounds.top - handleSize/2,
                      bounds.left + handleSize/2, bounds.top + handleSize/2, handlePaint)
        
        // Top-right handle
        canvas.drawRect(bounds.right - handleSize/2, bounds.top - handleSize/2,
                      bounds.right + handleSize/2, bounds.top + handleSize/2, handlePaint)
        
        // Bottom-left handle
        canvas.drawRect(bounds.left - handleSize/2, bounds.bottom - handleSize/2,
                      bounds.left + handleSize/2, bounds.bottom + handleSize/2, handlePaint)
        
        // Bottom-right handle
        canvas.drawRect(bounds.right - handleSize/2, bounds.bottom - handleSize/2,
                      bounds.right + handleSize/2, bounds.bottom + handleSize/2, handlePaint)
        
        // Draw rotation handle (green circle at top-center, inside rotation)
        val rotationHandleX = getCenterX()
        val rotationHandleY = bounds.top - 40f
        canvas.drawCircle(rotationHandleX, rotationHandleY, 10f, rotationHandlePaint)
        
        // Draw delete button (red circle with X at top-right corner, inside rotation)
        val deleteButtonX = bounds.right + 20f
        val deleteButtonY = bounds.top - 20f
        val deleteButtonRadius = 15f
        
        // Draw red circle
        canvas.drawCircle(deleteButtonX, deleteButtonY, deleteButtonRadius, deleteButtonPaint)
        
        // Draw white X
        val xSize = 8f
        canvas.drawLine(
            deleteButtonX - xSize, deleteButtonY - xSize,
            deleteButtonX + xSize, deleteButtonY + xSize,
            deleteIconPaint
        )
        canvas.drawLine(
            deleteButtonX + xSize, deleteButtonY - xSize,
            deleteButtonX - xSize, deleteButtonY + xSize,
            deleteIconPaint
        )
        
        canvas.restore()
    }
    
    fun contains(touchX: Float, touchY: Float): Boolean {
        val bounds = getBounds()
        
        // For rotated images, transform the touch point
        if (rotation != 0f) {
            val centerX = getCenterX()
            val centerY = getCenterY()
            
            val angle = -Math.toRadians(rotation.toDouble())
            val cos = cos(angle).toFloat()
            val sin = sin(angle).toFloat()
            
            val dx = touchX - centerX
            val dy = touchY - centerY
            
            val rotatedX = centerX + dx * cos - dy * sin
            val rotatedY = centerY + dx * sin + dy * cos
            
            return rotatedX >= bounds.left && rotatedX <= bounds.right &&
                   rotatedY >= bounds.top && rotatedY <= bounds.bottom
        }
        
        return touchX >= bounds.left && touchX <= bounds.right &&
               touchY >= bounds.top && touchY <= bounds.bottom
    }
    
    fun isResizeHandleTouched(touchX: Float, touchY: Float): ResizeHandle? {
        if (!isSelected) return null
        
        val bounds = getBounds()
        val handleSize = 20f
        val touchRadius = handleSize
        
        // Transform touch point to account for rotation
        val (transformedTouchX, transformedTouchY) = transformTouchPoint(touchX, touchY)
        
        // Check each corner handle in the rotated space
        val handles = listOf(
            Pair(ResizeHandle.TOP_LEFT, Pair(bounds.left, bounds.top)),
            Pair(ResizeHandle.TOP_RIGHT, Pair(bounds.right, bounds.top)),
            Pair(ResizeHandle.BOTTOM_LEFT, Pair(bounds.left, bounds.bottom)),
            Pair(ResizeHandle.BOTTOM_RIGHT, Pair(bounds.right, bounds.bottom))
        )
        
        for ((handle, pos) in handles) {
            val distance = sqrt(
                (transformedTouchX - pos.first) * (transformedTouchX - pos.first) + 
                (transformedTouchY - pos.second) * (transformedTouchY - pos.second)
            )
            if (distance <= touchRadius) {
                return handle
            }
        }
        
        return null
    }
    
    fun isRotationHandleTouched(touchX: Float, touchY: Float): Boolean {
        if (!isSelected) return false
        
        val bounds = getBounds()
        val handleX = getCenterX()
        val handleY = bounds.top - 40f
        val handleRadius = 20f
        
        val (transformedTouchX, transformedTouchY) = transformTouchPoint(touchX, touchY)
        
        val distance = sqrt(
            (transformedTouchX - handleX) * (transformedTouchX - handleX) + 
            (transformedTouchY - handleY) * (transformedTouchY - handleY)
        )
        return distance <= handleRadius
    }
    
    fun isDeleteButtonTouched(touchX: Float, touchY: Float): Boolean {
        if (!isSelected) return false
        
        val bounds = getBounds()
        val deleteButtonX = bounds.right + 20f
        val deleteButtonY = bounds.top - 20f
        val deleteButtonRadius = 20f
        
        val (transformedTouchX, transformedTouchY) = transformTouchPoint(touchX, touchY)
        
        val distance = sqrt(
            (transformedTouchX - deleteButtonX) * (transformedTouchX - deleteButtonX) + 
            (transformedTouchY - deleteButtonY) * (transformedTouchY - deleteButtonY)
        )
        return distance <= deleteButtonRadius
    }
    
    enum class ResizeHandle {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }
}

