package com.example.moodle.ui.whiteboard.models

import android.graphics.*
import kotlin.math.*

/**
 * Data class representing a text object on the whiteboard
 */
data class TextObject(
    var text: String,
    var x: Float,
    var y: Float,
    var textSize: Float,
    var color: Int,
    var rotation: Float = 0f,
    var isSelected: Boolean = false
) {
    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        textAlign = Paint.Align.LEFT
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
    
    fun updatePaint() {
        paint.color = color
        paint.textSize = textSize
    }
    
    fun draw(canvas: Canvas) {
        updatePaint()
        
        canvas.save()
        
        // Apply rotation around text center
        if (rotation != 0f) {
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            val centerX = x + bounds.width() / 2f
            val centerY = y + bounds.height() / 2f
            canvas.rotate(rotation, centerX, centerY)
        }
        
        canvas.drawText(text, x, y, paint)
        canvas.restore()
        
        if (isSelected) {
            drawSelectionBounds(canvas)
        }
    }
    
    private fun drawSelectionBounds(canvas: Canvas) {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        
        val left = x - 10f
        val top = y + bounds.top - 10f
        val right = x + bounds.width() + 10f
        val bottom = y + bounds.bottom + 10f
        
        canvas.save()
        
        // Apply rotation for selection bounds
        if (rotation != 0f) {
            val centerX = x + bounds.width() / 2f
            val centerY = y + bounds.height() / 2f
            canvas.rotate(rotation, centerX, centerY)
        }
        
        // Draw selection rectangle
        canvas.drawRect(left, top, right, bottom, selectionPaint)
        
        // Draw resize handle (blue square at bottom-right)
        val handleSize = 20f
        canvas.drawRect(right - handleSize/2, bottom - handleSize/2, 
                      right + handleSize/2, bottom + handleSize/2, handlePaint)
        
        // Draw delete button (red circle with X at top-right corner)
        val deleteButtonX = right + 20f
        val deleteButtonY = top - 20f
        val deleteButtonRadius = 15f
        
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
        
        // Draw rotation handle (green circle at top-center, outside rotation)
        val rotationHandleX = x + bounds.width() / 2f
        val rotationHandleY = top - 30f
        canvas.drawCircle(rotationHandleX, rotationHandleY, 10f, rotationHandlePaint)
    }
    
    fun contains(touchX: Float, touchY: Float): Boolean {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        
        // For rotated text, transform the touch point
        if (rotation != 0f) {
            val centerX = x + bounds.width() / 2f
            val centerY = y + bounds.height() / 2f
            
            val angle = -Math.toRadians(rotation.toDouble())
            val cos = cos(angle).toFloat()
            val sin = sin(angle).toFloat()
            
            val dx = touchX - centerX
            val dy = touchY - centerY
            
            val rotatedX = centerX + dx * cos - dy * sin
            val rotatedY = centerY + dx * sin + dy * cos
            
            return rotatedX >= x - 10f && rotatedX <= x + bounds.width() + 10f &&
                   rotatedY >= y + bounds.top - 10f && rotatedY <= y + bounds.bottom + 10f
        }
        
        return touchX >= x - 10f && touchX <= x + bounds.width() + 10f &&
               touchY >= y + bounds.top - 10f && touchY <= y + bounds.bottom + 10f
    }
    
    fun isResizeHandleTouched(touchX: Float, touchY: Float): Boolean {
        if (!isSelected) return false
        
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        
        val handleX = x + bounds.width() + 10f
        val handleY = y + bounds.bottom + 10f
        val handleRadius = 20f
        
        // For rotated text, check in rotated space
        if (rotation != 0f) {
            val centerX = x + bounds.width() / 2f
            val centerY = y + bounds.height() / 2f
            val angle = -Math.toRadians(rotation.toDouble())
            val cos = cos(angle).toFloat()
            val sin = sin(angle).toFloat()
            
            val dx = touchX - centerX
            val dy = touchY - centerY
            
            val transformedX = centerX + dx * cos - dy * sin
            val transformedY = centerY + dx * sin + dy * cos
            
            val distance = sqrt(
                (transformedX - handleX) * (transformedX - handleX) + 
                (transformedY - handleY) * (transformedY - handleY)
            )
            return distance <= handleRadius
        }
        
        val distance = sqrt((touchX - handleX) * (touchX - handleX) + (touchY - handleY) * (touchY - handleY))
        return distance <= handleRadius
    }
    
    fun isRotationHandleTouched(touchX: Float, touchY: Float): Boolean {
        if (!isSelected) return false
        
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        
        val handleX = x + bounds.width() / 2f
        val handleY = y + bounds.top - 40f
        val handleRadius = 15f
        
        val distance = sqrt((touchX - handleX) * (touchX - handleX) + (touchY - handleY) * (touchY - handleY))
        return distance <= handleRadius
    }
    
    fun isDeleteButtonTouched(touchX: Float, touchY: Float): Boolean {
        if (!isSelected) return false
        
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        
        val left = x - 10f
        val top = y + bounds.top - 10f
        val right = x + bounds.width() + 10f
        
        val deleteButtonX = right + 20f
        val deleteButtonY = top - 20f
        val deleteButtonRadius = 20f
        
        // For rotated text, check in rotated space
        if (rotation != 0f) {
            val centerX = x + bounds.width() / 2f
            val centerY = y + bounds.height() / 2f
            val angle = -Math.toRadians(rotation.toDouble())
            val cos = cos(angle).toFloat()
            val sin = sin(angle).toFloat()
            
            val dx = touchX - centerX
            val dy = touchY - centerY
            
            val transformedX = centerX + dx * cos - dy * sin
            val transformedY = centerY + dx * sin + dy * cos
            
            val distance = sqrt(
                (transformedX - deleteButtonX) * (transformedX - deleteButtonX) + 
                (transformedY - deleteButtonY) * (transformedY - deleteButtonY)
            )
            return distance <= deleteButtonRadius
        }
        
        val distance = sqrt((touchX - deleteButtonX) * (touchX - deleteButtonX) + (touchY - deleteButtonY) * (touchY - deleteButtonY))
        return distance <= deleteButtonRadius
    }
    
    /**
     * Create a deep copy of this TextObject
     */
    fun copy(): TextObject {
        return TextObject(
            text = text,
            x = x,
            y = y,
            textSize = textSize,
            color = color,
            rotation = rotation,
            isSelected = isSelected
        )
    }
}

