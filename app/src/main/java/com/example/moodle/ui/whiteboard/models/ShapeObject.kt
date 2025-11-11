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
            
            DrawingTool.CUBE -> {
                val centerX = (startX + endX) / 2
                val centerY = (startY + endY) / 2
                val width = abs(endX - startX)
                val height = abs(endY - startY)
                val size = min(width, height) / 2
                
                val offset = size / 3
                
                // Front face
                path.addRect(
                    centerX - size, centerY - size / 2,
                    centerX + size / 2, centerY + size * 1.5f,
                    Path.Direction.CW
                )
                
                // Top face (parallelogram)
                path.moveTo(centerX - size, centerY - size / 2)
                path.lineTo(centerX - size + offset, centerY - size / 2 - offset)
                path.lineTo(centerX + size / 2 + offset, centerY - size / 2 - offset)
                path.lineTo(centerX + size / 2, centerY - size / 2)
                path.close()
                
                // Right face
                path.moveTo(centerX + size / 2, centerY - size / 2)
                path.lineTo(centerX + size / 2 + offset, centerY - size / 2 - offset)
                path.lineTo(centerX + size / 2 + offset, centerY + size * 1.5f - offset)
                path.lineTo(centerX + size / 2, centerY + size * 1.5f)
                path.close()
            }
            
            DrawingTool.CYLINDER -> {
                val centerX = (startX + endX) / 2
                val centerY = (startY + endY) / 2
                val width = abs(endX - startX)
                val height = abs(endY - startY)
                val radiusX = width / 3
                val radiusY = radiusX / 3
                val cylinderHeight = max(height, width / 2)
                
                val top = centerY - cylinderHeight / 2
                val bottom = centerY + cylinderHeight / 2
                
                // Top ellipse
                path.addOval(
                    RectF(centerX - radiusX, top - radiusY, centerX + radiusX, top + radiusY),
                    Path.Direction.CW
                )
                
                // Left vertical line
                path.moveTo(centerX - radiusX, top)
                path.lineTo(centerX - radiusX, bottom)
                
                // Right vertical line
                path.moveTo(centerX + radiusX, top)
                path.lineTo(centerX + radiusX, bottom)
                
                // Bottom ellipse (curved arc)
                val bottomRect = RectF(centerX - radiusX, bottom - radiusY, centerX + radiusX, bottom + radiusY)
                path.arcTo(bottomRect, 0f, 180f, false)
            }
            
            DrawingTool.PYRAMID -> {
                val centerX = (startX + endX) / 2
                val centerY = (startY + endY) / 2
                val width = abs(endX - startX)
                val height = abs(endY - startY)
                val size = max(width, height) / 2
                
                val topX = centerX
                val topY = centerY - size
                val bottomY = centerY + size
                
                // Front face
                path.moveTo(topX, topY)
                path.lineTo(centerX - size, bottomY)
                path.lineTo(centerX + size / 2, bottomY)
                path.close()
                
                // Right face (visible side)
                path.moveTo(topX, topY)
                path.lineTo(centerX + size / 2, bottomY)
                path.lineTo(centerX + size, bottomY - size / 3)
                path.close()
                
                // Base line for depth
                path.moveTo(centerX - size, bottomY)
                path.lineTo(centerX + size, bottomY - size / 3)
            }
            
            DrawingTool.SPHERE -> {
                val centerX = (startX + endX) / 2
                val centerY = (startY + endY) / 2
                val width = abs(endX - startX)
                val height = abs(endY - startY)
                val radius = min(width, height) / 2
                
                // Outer circle
                path.addCircle(centerX, centerY, radius, Path.Direction.CW)
                
                // Horizontal ellipse (equator)
                val equatorRadiusX = radius * 0.8f
                val equatorRadiusY = radius * 0.25f
                path.addOval(
                    RectF(
                        centerX - equatorRadiusX, centerY - equatorRadiusY,
                        centerX + equatorRadiusX, centerY + equatorRadiusY
                    ),
                    Path.Direction.CW
                )
                
                // Vertical ellipse (meridian)
                val meridianRadiusX = radius * 0.25f
                val meridianRadiusY = radius * 0.8f
                path.addOval(
                    RectF(
                        centerX - meridianRadiusX, centerY - meridianRadiusY,
                        centerX + meridianRadiusX, centerY + meridianRadiusY
                    ),
                    Path.Direction.CW
                )
            }
            
            DrawingTool.CONE -> {
                val centerX = (startX + endX) / 2
                val centerY = (startY + endY) / 2
                val width = abs(endX - startX)
                val height = abs(endY - startY)
                val size = max(width, height) / 2
                
                val topX = centerX
                val topY = centerY - size
                val bottomY = centerY + size
                val radiusX = size * 0.6f
                val radiusY = radiusX / 3
                
                // Left side of cone
                path.moveTo(topX, topY)
                path.lineTo(centerX - radiusX, bottomY)
                
                // Right side of cone
                path.moveTo(topX, topY)
                path.lineTo(centerX + radiusX, bottomY)
                
                // Base ellipse
                path.addOval(
                    RectF(centerX - radiusX, bottomY - radiusY, centerX + radiusX, bottomY + radiusY),
                    Path.Direction.CW
                )
            }
            
            DrawingTool.RIGHT_TRIANGLE -> {
                val left = min(startX, endX)
                val top = min(startY, endY)
                val right = max(startX, endX)
                val bottom = max(startY, endY)
                
                path.moveTo(left, bottom)
                path.lineTo(right, bottom)
                path.lineTo(right, top)
                path.close()
            }
            
            DrawingTool.ISOSCELES_TRIANGLE -> {
                val centerX = (startX + endX) / 2
                val centerY = (startY + endY) / 2
                val width = abs(endX - startX)
                val height = abs(endY - startY)
                
                val topX = centerX
                val topY = centerY - height / 2
                val leftX = centerX - width / 2
                val leftY = centerY + height / 2
                val rightX = centerX + width / 2
                val rightY = centerY + height / 2
                
                path.moveTo(topX, topY)
                path.lineTo(leftX, leftY)
                path.lineTo(rightX, rightY)
                path.close()
            }
            
            DrawingTool.PARALLELOGRAM -> {
                val centerX = (startX + endX) / 2
                val centerY = (startY + endY) / 2
                val width = abs(endX - startX)
                val height = abs(endY - startY)
                
                val offset = width * 0.25f
                
                path.moveTo(centerX - width / 2 + offset, centerY - height / 2)
                path.lineTo(centerX + width / 2 + offset, centerY - height / 2)
                path.lineTo(centerX + width / 2 - offset, centerY + height / 2)
                path.lineTo(centerX - width / 2 - offset, centerY + height / 2)
                path.close()
            }
            
            DrawingTool.TRAPEZOID -> {
                val centerX = (startX + endX) / 2
                val centerY = (startY + endY) / 2
                val width = abs(endX - startX)
                val height = abs(endY - startY)
                
                val topWidth = width * 0.6f
                val bottomWidth = width
                
                path.moveTo(centerX - topWidth / 2, centerY - height / 2)
                path.lineTo(centerX + topWidth / 2, centerY - height / 2)
                path.lineTo(centerX + bottomWidth / 2, centerY + height / 2)
                path.lineTo(centerX - bottomWidth / 2, centerY + height / 2)
                path.close()
            }
            
            DrawingTool.PENTAGON -> {
                val centerX = (startX + endX) / 2
                val centerY = (startY + endY) / 2
                val width = abs(endX - startX)
                val height = abs(endY - startY)
                val radius = min(width, height) / 2
                
                for (i in 0..5) {
                    val angle = Math.toRadians((i * 72 - 90).toDouble())
                    val x = centerX + radius * cos(angle).toFloat()
                    val y = centerY + radius * sin(angle).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
            }
            
            DrawingTool.HEXAGON -> {
                val centerX = (startX + endX) / 2
                val centerY = (startY + endY) / 2
                val width = abs(endX - startX)
                val height = abs(endY - startY)
                val radius = min(width, height) / 2
                
                for (i in 0..6) {
                    val angle = Math.toRadians((i * 60 - 90).toDouble())
                    val x = centerX + radius * cos(angle).toFloat()
                    val y = centerY + radius * sin(angle).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
            }
            
            DrawingTool.STAR -> {
                val centerX = (startX + endX) / 2
                val centerY = (startY + endY) / 2
                val width = abs(endX - startX)
                val height = abs(endY - startY)
                val outerRadius = min(width, height) / 2
                val innerRadius = outerRadius * 0.4f
                
                for (i in 0..10) {
                    val angle = Math.toRadians((i * 36 - 90).toDouble())
                    val radius = if (i % 2 == 0) outerRadius else innerRadius
                    val x = centerX + radius * cos(angle).toFloat()
                    val y = centerY + radius * sin(angle).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
            }
            
            DrawingTool.DIAMOND -> {
                val centerX = (startX + endX) / 2
                val centerY = (startY + endY) / 2
                val width = abs(endX - startX)
                val height = abs(endY - startY)
                
                path.moveTo(centerX, centerY - height / 2)
                path.lineTo(centerX + width / 2, centerY)
                path.lineTo(centerX, centerY + height / 2)
                path.lineTo(centerX - width / 2, centerY)
                path.close()
            }
            
            DrawingTool.ELLIPSE -> {
                val left = min(startX, endX)
                val top = min(startY, endY)
                val right = max(startX, endX)
                val bottom = max(startY, endY)
                path.addOval(RectF(left, top, right, bottom), Path.Direction.CW)
            }
            
            DrawingTool.ARROW -> {
                val centerY = (startY + endY) / 2
                val width = abs(endX - startX)
                val height = abs(endY - startY)
                
                val shaftWidth = height * 0.3f
                val headWidth = height * 0.7f
                val headLength = width * 0.3f
                
                val left = min(startX, endX)
                val right = max(startX, endX)
                
                // Arrow shaft
                path.moveTo(left, centerY - shaftWidth / 2)
                path.lineTo(right - headLength, centerY - shaftWidth / 2)
                
                // Arrow head
                path.lineTo(right - headLength, centerY - headWidth / 2)
                path.lineTo(right, centerY)
                path.lineTo(right - headLength, centerY + headWidth / 2)
                path.lineTo(right - headLength, centerY + shaftWidth / 2)
                
                // Back to start
                path.lineTo(left, centerY + shaftWidth / 2)
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

