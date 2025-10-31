package com.example.moodle.ui.whiteboard.models

import android.graphics.*
import kotlin.math.*

/**
 * Data class representing a shape object on the whiteboard
 */
data class ShapeObject(
    var shapeType: DrawingTool,
    var startX: Float,
    var startY: Float,
    var endX: Float,
    var endY: Float,
    var color: Int,
    var strokeWidth: Float,
    var rotation: Float = 0f,
    var scaleX: Float = 1f,
    var scaleY: Float = 1f,
    var isSelected: Boolean = false
) {
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
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
        paint.strokeWidth = strokeWidth
    }
    
    fun getBounds(): RectF {
        val left = min(startX, endX)
        val top = min(startY, endY)
        val right = max(startX, endX)
        val bottom = max(startY, endY)
        return RectF(left - 20f, top - 20f, right + 20f, bottom + 20f)
    }
    
    fun getCenterX(): Float = (startX + endX) / 2f
    fun getCenterY(): Float = (startY + endY) / 2f
    
    /**
     * Transform touch coordinates to account for shape rotation
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
        updatePaint()
        
        canvas.save()
        
        // Apply transformations
        val centerX = getCenterX()
        val centerY = getCenterY()
        
        canvas.translate(centerX, centerY)
        canvas.rotate(rotation)
        canvas.scale(scaleX, scaleY)
        canvas.translate(-centerX, -centerY)
        
        // Create and draw the shape path
        val shapePath = createShapePath()
        canvas.drawPath(shapePath, paint)
        
        canvas.restore()
        
        if (isSelected) {
            drawSelectionBounds(canvas)
        }
    }
    
    private fun createShapePath(): Path {
        val path = Path()
        
        when (shapeType) {
            DrawingTool.LINE -> {
                path.moveTo(startX, startY)
                path.lineTo(endX, endY)
            }
            
            DrawingTool.RECTANGLE -> {
                val left = min(startX, endX)
                val top = min(startY, endY)
                val right = max(startX, endX)
                val bottom = max(startY, endY)
                path.addRect(left, top, right, bottom, Path.Direction.CW)
            }
            
            DrawingTool.CIRCLE -> {
                val centerX = (startX + endX) / 2
                val centerY = (startY + endY) / 2
                val radius = sqrt(
                    (endX - startX) * (endX - startX) + (endY - startY) * (endY - startY)
                ) / 2
                path.addCircle(centerX, centerY, radius, Path.Direction.CW)
            }
            
            DrawingTool.TRIANGLE -> {
                val centerX = (startX + endX) / 2
                val centerY = (startY + endY) / 2
                val width = abs(endX - startX)
                val height = abs(endY - startY)
                val size = max(width, height)
                
                val topX = centerX
                val topY = centerY - size / 2
                val leftX = centerX - size / 2
                val leftY = centerY + size / 2
                val rightX = centerX + size / 2
                val rightY = centerY + size / 2
                
                path.moveTo(topX, topY)
                path.lineTo(leftX, leftY)
                path.lineTo(rightX, rightY)
                path.close()
            }
            
            else -> {
                // Default to line
                path.moveTo(startX, startY)
                path.lineTo(endX, endY)
            }
        }
        
        return path
    }
    
    private fun drawSelectionBounds(canvas: Canvas) {
        if (shapeType == DrawingTool.LINE) {
            drawLineEndpointHandles(canvas)
        } else {
            drawNormalSelectionBounds(canvas)
        }
    }
    
    private fun drawLineEndpointHandles(canvas: Canvas) {
        val handleSize = 20f
        
        // Draw start point handle (blue circle)
        canvas.drawCircle(startX, startY, handleSize/2, handlePaint)
        
        // Draw end point handle (blue circle)
        canvas.drawCircle(endX, endY, handleSize/2, handlePaint)
        
        // Draw selection line (dashed line along the actual line)
        canvas.drawLine(startX, startY, endX, endY, selectionPaint)
    }
    
    private fun drawNormalSelectionBounds(canvas: Canvas) {
        val bounds = getBounds()
        
        canvas.save()
        
        // Apply rotation for selection bounds
        val centerX = getCenterX()
        val centerY = getCenterY()
        if (rotation != 0f) {
            canvas.rotate(rotation, centerX, centerY)
        }
        
        // Draw selection rectangle
        canvas.drawRect(bounds, selectionPaint)
        
        // Draw resize handles (corners)
        val handleSize = 20f
        val handles = arrayOf(
            Pair(bounds.left, bounds.top), // Top-left
            Pair(bounds.right, bounds.top), // Top-right
            Pair(bounds.left, bounds.bottom), // Bottom-left
            Pair(bounds.right, bounds.bottom) // Bottom-right
        )
        
        for ((x, y) in handles) {
            canvas.drawRect(
                x - handleSize/2, y - handleSize/2,
                x + handleSize/2, y + handleSize/2,
                handlePaint
            )
        }
        
        // Draw delete button
        val deleteButtonX = bounds.right + 20f
        val deleteButtonY = bounds.top - 20f
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
        
        // Draw rotation handle
        val rotationHandleX = centerX
        val rotationHandleY = bounds.top - 40f
        canvas.drawCircle(rotationHandleX, rotationHandleY, 15f, rotationHandlePaint)
    }
    
    fun contains(touchX: Float, touchY: Float): Boolean {
        val bounds = getBounds()
        return bounds.contains(touchX, touchY)
    }
    
    fun isResizeHandleTouched(touchX: Float, touchY: Float): ResizeHandle? {
        if (!isSelected) return null
        
        if (shapeType == DrawingTool.LINE) {
            return getLineEndpointTouched(touchX, touchY)
        }
        
        val bounds = getBounds()
        val handleSize = 20f
        val touchRadius = handleSize
        
        val (transformedTouchX, transformedTouchY) = transformTouchPoint(touchX, touchY)
        
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
    
    private fun getLineEndpointTouched(touchX: Float, touchY: Float): ResizeHandle? {
        val handleSize = 20f
        
        val startDistance = sqrt((touchX - startX) * (touchX - startX) + (touchY - startY) * (touchY - startY))
        if (startDistance <= handleSize) {
            return ResizeHandle.LINE_START
        }
        
        val endDistance = sqrt((touchX - endX) * (touchX - endX) + (touchY - endY) * (touchY - endY))
        if (endDistance <= handleSize) {
            return ResizeHandle.LINE_END
        }
        
        return null
    }
    
    fun isRotationHandleTouched(touchX: Float, touchY: Float): Boolean {
        if (!isSelected || shapeType == DrawingTool.LINE) return false
        
        val bounds = getBounds()
        val handleX = getCenterX()
        val handleY = bounds.top - 40f
        val handleRadius = 20f
        
        val distance = sqrt((touchX - handleX) * (touchX - handleX) + (touchY - handleY) * (touchY - handleY))
        return distance <= handleRadius
    }
    
    fun isDeleteButtonTouched(touchX: Float, touchY: Float): Boolean {
        if (!isSelected || shapeType == DrawingTool.LINE) return false
        
        val bounds = getBounds()
        val deleteButtonX = bounds.right + 20f
        val deleteButtonY = bounds.top - 20f
        val deleteButtonRadius = 20f
        
        if (rotation != 0f) {
            val centerX = getCenterX()
            val centerY = getCenterY()
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
    
    enum class ResizeHandle {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, LINE_START, LINE_END
    }
}

