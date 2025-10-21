package com.example.moodle.ui.whiteboard

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class WhiteboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Interface for drawing events
    interface OnDrawingChangeListener {
        fun onDrawingChanged()
    }

    // Drawing tools
    enum class DrawingTool {
        PEN, ERASER, TEXT, SELECTIVE_ERASER, LINE, CIRCLE, TRIANGLE, RECTANGLE, SELECTION
    }

    // Drawing state
    private var currentTool = DrawingTool.PEN
    private var currentColor = Color.BLACK
    private var currentStrokeWidth = 8f
    private var isDrawingMode = true
    private var isPinchModeEnabled = false
    private var currentBackgroundPattern = BackgroundPattern.PLAIN
    
    // Listener for drawing changes
    private var drawingChangeListener: OnDrawingChangeListener? = null

    // Paint objects
    private val drawPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val eraserPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }


    private val textPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        textAlign = Paint.Align.LEFT
        textSize = 48f
    }

    // Text-related properties
    private var textInputListener: OnTextInputListener? = null
    
    // Interface for text input events
    interface OnTextInputListener {
        fun onTextInputRequested(x: Float, y: Float)
    }

    // Canvas and bitmap for drawing
    private var drawingBitmap: Bitmap? = null
    private var drawingCanvas: Canvas? = null
    private val backgroundPaint = Paint().apply {
        color = Color.WHITE
    }

    // Path tracking
    private val paths = mutableListOf<DrawingPath>()
    private val undoStack = mutableListOf<DrawingPath>()
    
    // Text objects tracking
    private val textObjects = mutableListOf<TextObject>()
    private var selectedTextObject: TextObject? = null
    private var isDraggingText = false
    private var isResizingText = false
    private var isRotatingText = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var initialTextSize = 0f
    private var initialRotation = 0f
    
    // Shape objects tracking
    private val shapeObjects = mutableListOf<ShapeObject>()
    private var selectedShapeObject: ShapeObject? = null
    private var isDraggingShape = false
    private var isResizingShape = false
    private var isRotatingShape = false
    private var currentResizeHandle: ShapeObject.ResizeHandle? = null
    private var initialShapeStartX = 0f
    private var initialShapeStartY = 0f
    private var initialShapeEndX = 0f
    private var initialShapeEndY = 0f
    private var initialShapeRotation = 0f
    
    // Image objects tracking
    private val imageObjects = mutableListOf<ImageObject>()
    private var selectedImageObject: ImageObject? = null
    private var isDraggingImage = false
    private var isResizingImage = false
    private var isRotatingImage = false
    private var currentImageResizeHandle: ImageObject.ResizeHandle? = null
    private var initialImageX = 0f
    private var initialImageY = 0f
    private var initialImageWidth = 0f
    private var initialImageHeight = 0f
    private var initialImageScaleX = 1f
    private var initialImageScaleY = 1f
    private var initialImageRotation = 0f
    
    // Selective eraser tracking
    private var selectionPath: Path? = null
    private var isDrawingSelection = false
    private val selectionPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
        alpha = 150
    }

    // Multi-touch path tracking
    private val activePaths = mutableMapOf<Int, Path>()
    private val activePathsLastX = mutableMapOf<Int, Float>()
    private val activePathsLastY = mutableMapOf<Int, Float>()

    // Gesture detection
    private val gestureDetector = GestureDetector(context, GestureListener())
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())

    // Zoom and pan variables
    private var scaleFactor = 1.0f
    private var focusX = 0f
    private var focusY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    // Multi-touch tracking
    private var isScaling = false
    private var isPanning = false
    
    // Boundary constraints
    private var maxFocusX = 0f
    private var maxFocusY = 0f
    private var minScaleFactor = 0.1f  // Minimum zoom level to prevent seeing outside bitmap
    
    // Section management for split mode
    private val sectionManager = SectionManager()
    private var currentActiveSection: SectionManager.Section? = null
    
    // Multi-section drawing support - track which section each pointer is drawing in
    private val pointerSectionMap = mutableMapOf<Int, SectionManager.Section>()
    
    // Shape drawing state
    private var isDrawingShape = false
    private var shapeStartX = 0f
    private var shapeStartY = 0f
    private var shapeEndX = 0f
    private var shapeEndY = 0f
    private var previewPath: Path? = null

    data class DrawingPath(
        val path: Path,
        val paint: Paint,
        val tool: DrawingTool,
        val text: String? = null,
        val textX: Float = 0f,
        val textY: Float = 0f,
        val shapeStartX: Float = 0f,
        val shapeStartY: Float = 0f,
        val shapeEndX: Float = 0f,
        val shapeEndY: Float = 0f
    )

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
            val left = kotlin.math.min(startX, endX)
            val top = kotlin.math.min(startY, endY)
            val right = kotlin.math.max(startX, endX)
            val bottom = kotlin.math.max(startY, endY)
            return RectF(left - 20f, top - 20f, right + 20f, bottom + 20f)
        }
        
        fun getCenterX(): Float = (startX + endX) / 2f
        fun getCenterY(): Float = (startY + endY) / 2f
        
        /**
         * Transform touch coordinates to account for shape rotation
         * Returns a Pair of (transformedX, transformedY)
         */
        private fun transformTouchPoint(touchX: Float, touchY: Float): Pair<Float, Float> {
            if (rotation == 0f) {
                return Pair(touchX, touchY)
            }
            
            val centerX = getCenterX()
            val centerY = getCenterY()
            val angle = -Math.toRadians(rotation.toDouble())
            val cos = kotlin.math.cos(angle).toFloat()
            val sin = kotlin.math.sin(angle).toFloat()
            
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
                    val left = kotlin.math.min(startX, endX)
                    val top = kotlin.math.min(startY, endY)
                    val right = kotlin.math.max(startX, endX)
                    val bottom = kotlin.math.max(startY, endY)
                    path.addRect(left, top, right, bottom, Path.Direction.CW)
                }
                
                DrawingTool.CIRCLE -> {
                    val centerX = (startX + endX) / 2
                    val centerY = (startY + endY) / 2
                    val radius = kotlin.math.sqrt(
                        (endX - startX) * (endX - startX) + (endY - startY) * (endY - startY)
                    ) / 2
                    path.addCircle(centerX, centerY, radius, Path.Direction.CW)
                }
                
                DrawingTool.TRIANGLE -> {
                    val centerX = (startX + endX) / 2
                    val centerY = (startY + endY) / 2
                    val width = kotlin.math.abs(endX - startX)
                    val height = kotlin.math.abs(endY - startY)
                    val size = kotlin.math.max(width, height)
                    
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
                // For lines, draw endpoint handles instead of bounding box
                drawLineEndpointHandles(canvas)
            } else {
                // For other shapes, draw normal bounding box with resize handles
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
            
            // Draw rotation handle (green circle at top-center, outside rotation)
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
            
            // For lines, check endpoint handles instead
            if (shapeType == DrawingTool.LINE) {
                return getLineEndpointTouched(touchX, touchY)
            }
            
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
                val distance = kotlin.math.sqrt(
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
            
            // Check start point
            val startDistance = kotlin.math.sqrt((touchX - startX) * (touchX - startX) + (touchY - startY) * (touchY - startY))
            if (startDistance <= handleSize) {
                return ResizeHandle.LINE_START
            }
            
            // Check end point
            val endDistance = kotlin.math.sqrt((touchX - endX) * (touchX - endX) + (touchY - endY) * (touchY - endY))
            if (endDistance <= handleSize) {
                return ResizeHandle.LINE_END
            }
            
            return null
        }
        
        fun isRotationHandleTouched(touchX: Float, touchY: Float): Boolean {
            if (!isSelected) return false
            
            // Lines don't have rotation handles - they rotate by dragging endpoints
            if (shapeType == DrawingTool.LINE) return false
            
            val bounds = getBounds()
            val handleX = getCenterX()
            val handleY = bounds.top - 40f
            val handleRadius = 20f
            
            val distance = kotlin.math.sqrt((touchX - handleX) * (touchX - handleX) + (touchY - handleY) * (touchY - handleY))
            return distance <= handleRadius
        }
        
        fun isDeleteButtonTouched(touchX: Float, touchY: Float): Boolean {
            if (!isSelected) return false
            
            // Lines don't have delete button - they can be deleted using selective eraser
            if (shapeType == DrawingTool.LINE) return false
            
            val bounds = getBounds()
            val deleteButtonX = bounds.right + 20f
            val deleteButtonY = bounds.top - 20f
            val deleteButtonRadius = 20f // Slightly larger touch area
            
            // For rotated shapes, we need to check in rotated space
            // Transform touch point to account for rotation
            if (rotation != 0f) {
                val centerX = getCenterX()
                val centerY = getCenterY()
                val angle = -Math.toRadians(rotation.toDouble())
                val cos = kotlin.math.cos(angle).toFloat()
                val sin = kotlin.math.sin(angle).toFloat()
                
                val dx = touchX - centerX
                val dy = touchY - centerY
                
                val transformedX = centerX + dx * cos - dy * sin
                val transformedY = centerY + dx * sin + dy * cos
                
                val distance = kotlin.math.sqrt(
                    (transformedX - deleteButtonX) * (transformedX - deleteButtonX) + 
                    (transformedY - deleteButtonY) * (transformedY - deleteButtonY)
                )
                return distance <= deleteButtonRadius
            }
            
            val distance = kotlin.math.sqrt((touchX - deleteButtonX) * (touchX - deleteButtonX) + (touchY - deleteButtonY) * (touchY - deleteButtonY))
            return distance <= deleteButtonRadius
        }
        
        enum class ResizeHandle {
            TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, LINE_START, LINE_END
        }
    }

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
            
            // Draw delete button (red circle with X at top-right corner, inside rotation)
            val deleteButtonX = right + 20f
            val deleteButtonY = top - 20f
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
            
            // Draw rotation handle (green circle at top-center, outside rotation)
            val rotationHandleX = x + bounds.width() / 2f
            val rotationHandleY = top - 30f
            canvas.drawCircle(rotationHandleX, rotationHandleY, 10f, rotationHandlePaint)
        }
        
        fun contains(touchX: Float, touchY: Float): Boolean {
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            
            // For rotated text, we need to transform the touch point
            if (rotation != 0f) {
                val centerX = x + bounds.width() / 2f
                val centerY = y + bounds.height() / 2f
                
                // Rotate touch point back to unrotated space
                val angle = -Math.toRadians(rotation.toDouble())
                val cos = kotlin.math.cos(angle).toFloat()
                val sin = kotlin.math.sin(angle).toFloat()
                
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
            val handleRadius = 20f // Slightly larger touch area
            
            // For rotated text, we need to check in rotated space
            // Transform touch point to account for rotation
            if (rotation != 0f) {
                val centerX = x + bounds.width() / 2f
                val centerY = y + bounds.height() / 2f
                val angle = -Math.toRadians(rotation.toDouble())
                val cos = kotlin.math.cos(angle).toFloat()
                val sin = kotlin.math.sin(angle).toFloat()
                
                val dx = touchX - centerX
                val dy = touchY - centerY
                
                val transformedX = centerX + dx * cos - dy * sin
                val transformedY = centerY + dx * sin + dy * cos
                
                val distance = kotlin.math.sqrt(
                    (transformedX - handleX) * (transformedX - handleX) + 
                    (transformedY - handleY) * (transformedY - handleY)
                )
                return distance <= handleRadius
            }
            
            // For non-rotated text, use simple bounding box check
            val distance = kotlin.math.sqrt((touchX - handleX) * (touchX - handleX) + (touchY - handleY) * (touchY - handleY))
            return distance <= handleRadius
        }
        
        fun isRotationHandleTouched(touchX: Float, touchY: Float): Boolean {
            if (!isSelected) return false
            
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            
            val handleX = x + bounds.width() / 2f
            val handleY = y + bounds.top - 40f // 30f offset + 10f radius
            val handleRadius = 15f // Slightly larger touch area
            
            // Rotation handle is outside the rotation, so no transformation needed
            val distance = kotlin.math.sqrt((touchX - handleX) * (touchX - handleX) + (touchY - handleY) * (touchY - handleY))
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
            val deleteButtonRadius = 20f // Slightly larger touch area
            
            // For rotated text, we need to check in rotated space
            // Transform touch point to account for rotation
            if (rotation != 0f) {
                val centerX = x + bounds.width() / 2f
                val centerY = y + bounds.height() / 2f
                val angle = -Math.toRadians(rotation.toDouble())
                val cos = kotlin.math.cos(angle).toFloat()
                val sin = kotlin.math.sin(angle).toFloat()
                
                val dx = touchX - centerX
                val dy = touchY - centerY
                
                val transformedX = centerX + dx * cos - dy * sin
                val transformedY = centerY + dx * sin + dy * cos
                
                val distance = kotlin.math.sqrt(
                    (transformedX - deleteButtonX) * (transformedX - deleteButtonX) + 
                    (transformedY - deleteButtonY) * (transformedY - deleteButtonY)
                )
                return distance <= deleteButtonRadius
            }
            
            val distance = kotlin.math.sqrt((touchX - deleteButtonX) * (touchX - deleteButtonX) + (touchY - deleteButtonY) * (touchY - deleteButtonY))
            return distance <= deleteButtonRadius
        }
    }

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
         * Returns a Pair of (transformedX, transformedY)
         */
        private fun transformTouchPoint(touchX: Float, touchY: Float): Pair<Float, Float> {
            if (rotation == 0f) {
                return Pair(touchX, touchY)
            }
            
            val centerX = getCenterX()
            val centerY = getCenterY()
            val angle = -Math.toRadians(rotation.toDouble())
            val cos = kotlin.math.cos(angle).toFloat()
            val sin = kotlin.math.sin(angle).toFloat()
            
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
            val destRect = RectF(x, y, x + width * scaleX, y + height * scaleY)
            canvas.drawBitmap(bitmap, null, destRect, paint)
            
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
            
            // For rotated images, we need to transform the touch point
            if (rotation != 0f) {
                val centerX = getCenterX()
                val centerY = getCenterY()
                
                // Rotate touch point back to unrotated space
                val angle = -Math.toRadians(rotation.toDouble())
                val cos = kotlin.math.cos(angle).toFloat()
                val sin = kotlin.math.sin(angle).toFloat()
                
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
                val distance = kotlin.math.sqrt(
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
            
            // Rotation handle is drawn in rotated space (before canvas.restore)
            // so we need to transform touch coordinates
            val bounds = getBounds()
            val handleX = getCenterX()
            val handleY = bounds.top - 40f
            val handleRadius = 20f
            
            val (transformedTouchX, transformedTouchY) = transformTouchPoint(touchX, touchY)
            
            val distance = kotlin.math.sqrt(
                (transformedTouchX - handleX) * (transformedTouchX - handleX) + 
                (transformedTouchY - handleY) * (transformedTouchY - handleY)
            )
            return distance <= handleRadius
        }
        
        fun isDeleteButtonTouched(touchX: Float, touchY: Float): Boolean {
            if (!isSelected) return false
            
            // Delete button is drawn in rotated space (before canvas.restore)
            // so we need to transform touch coordinates
            val bounds = getBounds()
            val deleteButtonX = bounds.right + 20f
            val deleteButtonY = bounds.top - 20f
            val deleteButtonRadius = 20f // Slightly larger touch area
            
            val (transformedTouchX, transformedTouchY) = transformTouchPoint(touchX, touchY)
            
            val distance = kotlin.math.sqrt(
                (transformedTouchX - deleteButtonX) * (transformedTouchX - deleteButtonX) + 
                (transformedTouchY - deleteButtonY) * (transformedTouchY - deleteButtonY)
            )
            return distance <= deleteButtonRadius
        }
        
        enum class ResizeHandle {
            TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        if (w <= 0 || h <= 0) return
        
        try {
            // Create a larger bitmap for extended canvas (3x screen size for better memory management)
            val canvasWidth = w * 3
            val canvasHeight = h * 3
            
            // Clean up previous bitmap
            drawingBitmap?.recycle()
            
            drawingBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
            drawingCanvas = Canvas(drawingBitmap!!)
            drawingCanvas?.drawColor(Color.WHITE)
            
            // Center the initial view on the large canvas
            focusX = (canvasWidth - w) / 2f
            focusY = (canvasHeight - h) / 2f
            
            // Set maximum focus boundaries (bitmap size - view size)
            maxFocusX = (canvasWidth - w).toFloat()
            maxFocusY = (canvasHeight - h).toFloat()
            
            // Calculate minimum scale factor to prevent seeing empty space
            // Min scale is when entire bitmap just fits in the view
            minScaleFactor = max(
                w.toFloat() / canvasWidth.toFloat(),
                h.toFloat() / canvasHeight.toFloat()
            )
        } catch (e: OutOfMemoryError) {
            // Fallback to screen size if memory is insufficient
            drawingBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            drawingCanvas = Canvas(drawingBitmap!!)
            drawingCanvas?.drawColor(Color.WHITE)
            focusX = 0f
            focusY = 0f
            maxFocusX = 0f
            maxFocusY = 0f
            minScaleFactor = 1.0f  // No zoom out for fallback case
        }
        
        // Update section manager with new dimensions
        sectionManager.setBoardDimensions(w.toFloat(), h.toFloat())
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        
        canvas?.let { c ->
            android.util.Log.d("WhiteboardView", "onDraw called, splitMode=${sectionManager.isSplitModeEnabled()}, sections=${sectionManager.getAllSections().size}")
            
            if (sectionManager.isSplitModeEnabled()) {
                android.util.Log.d("WhiteboardView", "Drawing in SPLIT mode")
                // In split mode: draw without transformations
                // Draw white background (no patterns in split mode)
                c.drawColor(Color.WHITE)
                
                // IMPORTANT: Draw section backgrounds/borders FIRST, then content on top
                sectionManager.drawSections(c)
                
                // Draw section content
                for (section in sectionManager.getAllSections()) {
                    android.util.Log.d("WhiteboardView", "Drawing section ${section.id}: ${section.paths.size} paths, bounds=${section.drawingBounds}")
                    c.save()
                    
                    // Clip to drawing bounds only (not control area)
                    c.clipRect(section.drawingBounds)
                    
                    // Draw section's paths
                    for ((index, drawingPath) in section.paths.withIndex()) {
                        val pathBounds = RectF()
                        drawingPath.path.computeBounds(pathBounds, true)
                        android.util.Log.d("WhiteboardView", "Drawing path $index in section ${section.id}: bounds=$pathBounds, paint color=${drawingPath.paint.color}, strokeWidth=${drawingPath.paint.strokeWidth}")
                        
                        if (drawingPath.tool == DrawingTool.TEXT && drawingPath.text != null) {
                            c.drawText(drawingPath.text, drawingPath.textX, drawingPath.textY, drawingPath.paint)
                        } else {
                            c.drawPath(drawingPath.path, drawingPath.paint)
                        }
                    }
                    
                    // Draw section's text objects
                    for (textObject in section.textObjects) {
                        textObject.draw(c)
                    }
                    
                    c.restore()
                }
                
                // Draw current active paths being drawn
                for ((pointerId, path) in activePaths) {
                    if (!path.isEmpty) {
                        val section = pointerSectionMap[pointerId]
                        if (section != null) {
                            val pathBounds = RectF()
                            path.computeBounds(pathBounds, true)
                            android.util.Log.d("WhiteboardView", "Drawing ACTIVE path for pointer $pointerId in section ${section.id}: bounds=$pathBounds")
                            c.save()
                            c.clipRect(section.drawingBounds)
                            c.drawPath(path, getCurrentPaintForSection(section))
                            c.restore()
                        }
                    }
                }
                
                // Draw shape preview in split mode
                previewPath?.let { path ->
                    currentActiveSection?.let { section ->
                        c.save()
                        c.clipRect(section.drawingBounds)
                        c.drawPath(path, getCurrentPaintForSection(section))
                        c.restore()
                    }
                }
                
                // Draw section control toolbars on top
                drawSectionControls(c)
            } else {
                // In normal mode: use transformations for infinite canvas
                c.save()
                
                // Apply transformations
                c.translate(-focusX, -focusY)
                c.scale(scaleFactor, scaleFactor)
                
                // Draw background pattern
                drawBackgroundPattern(c)
                
                // Draw the infinite bitmap with all previous strokes
                drawingBitmap?.let { bitmap ->
                    c.drawBitmap(bitmap, 0f, 0f, null)
                }
                
                // Draw current active paths being drawn
                for ((pointerId, path) in activePaths) {
                    if (!path.isEmpty) {
                        c.drawPath(path, getCurrentPaint())
                    }
                }
                
                // Draw text objects
                for (textObject in textObjects) {
                    textObject.draw(c)
                }
                
                // Draw shape objects
                for (shapeObject in shapeObjects) {
                    shapeObject.draw(c)
                }
                
                // Draw image objects
                for (imageObject in imageObjects) {
                    imageObject.draw(c)
                }
                
                // Draw selection area for selective eraser
                selectionPath?.let { path ->
                    c.drawPath(path, selectionPaint)
                }
                
                // Draw shape preview
                previewPath?.let { path ->
                    c.drawPath(path, getCurrentPaint())
                }
                
                c.restore()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        android.util.Log.d("WhiteboardView", "onTouchEvent: action=${event.actionMasked}, splitMode=${sectionManager.isSplitModeEnabled()}, isDrawingMode=$isDrawingMode")
        
        // In split mode, disable pinch/pan features
        if (sectionManager.isSplitModeEnabled()) {
            android.util.Log.d("WhiteboardView", "Split mode active, calling handleDrawingTouch")
            // Always handle as drawing in split mode
            handleDrawingTouch(event)
            return true
        }
        
        // Normal mode: Handle scale gesture only if pinch mode is enabled
        if (isPinchModeEnabled) {
            scaleGestureDetector.onTouchEvent(event)
            
            // Don't process drawing if scaling
            if (isScaling) {
                android.util.Log.d("WhiteboardView", "Currently scaling, skipping drawing")
                return true
            }
        }
        
        // Handle gesture detection for double tap (zoom reset)
        gestureDetector.onTouchEvent(event)
        
        // Handle drawing only if in drawing mode
        if (isDrawingMode) {
            android.util.Log.d("WhiteboardView", "Drawing mode active, calling handleDrawingTouch")
            handleDrawingTouch(event)
        } else {
            android.util.Log.d("WhiteboardView", "Pan mode active, calling handlePanningTouch")
            handlePanningTouch(event)
        }
        
        return true
    }

    private fun handleDrawingTouch(event: MotionEvent) {
        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        
        android.util.Log.d("WhiteboardView", "handleDrawingTouch: action=$action, pointer=$pointerId, splitMode=${sectionManager.isSplitModeEnabled()}")
        
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // Get screen coordinates
                val screenX = event.getX(pointerIndex)
                val screenY = event.getY(pointerIndex)
                
                android.util.Log.d("WhiteboardView", "ACTION_DOWN at ($screenX, $screenY)")
                
                // In split mode, determine which section this pointer is drawing in
                if (sectionManager.isSplitModeEnabled()) {
                    val touchedSection = sectionManager.getSectionForDrawing(screenX, screenY)
                    android.util.Log.d("WhiteboardView", "Touch at ($screenX, $screenY), section: ${touchedSection?.id}")
                    
                    if (touchedSection != null) {
                        // Check if touch is in control area
                        if (touchedSection.containsInControlArea(screenX, screenY)) {
                            android.util.Log.d("WhiteboardView", "Touch in control area")
                            handleControlAreaTouch(touchedSection, screenX, screenY)
                            return
                        }
                        
                        // Check if touch is in drawing area
                        if (!touchedSection.containsInDrawingArea(screenX, screenY)) {
                            android.util.Log.d("WhiteboardView", "Touch outside drawing area - bounds: ${touchedSection.drawingBounds}")
                            // Touch outside drawing area, ignore (could be in border area)
                            return
                        }
                        
                        android.util.Log.d("WhiteboardView", "Touch in drawing area, tool: ${touchedSection.currentTool}, creating path...")
                        
                        // Map this pointer to the touched section
                        pointerSectionMap[pointerId] = touchedSection
                        
                        // Set as active section for UI feedback
                        currentActiveSection = touchedSection
                        sectionManager.setActiveSection(touchedSection)
                    } else {
                        // Touch outside any section, ignore
                        return
                    }
                }
                
                // Get coordinates for actual drawing
                // In split mode, use screen coordinates directly (no transformation)
                // In normal mode, use transformed coordinates
                var x = if (sectionManager.isSplitModeEnabled()) screenX else (screenX + focusX) / scaleFactor
                var y = if (sectionManager.isSplitModeEnabled()) screenY else (screenY + focusY) / scaleFactor
                
                android.util.Log.d("WhiteboardView", "Drawing coords: x=$x, y=$y (screen: $screenX, $screenY, focus: $focusX, $focusY, scale: $scaleFactor)")
                
                // Clamp coordinates to bitmap bounds in normal mode
                if (!sectionManager.isSplitModeEnabled()) {
                    val clamped = clampToBitmapBounds(x, y)
                    x = clamped.first
                    y = clamped.second
                    android.util.Log.d("WhiteboardView", "After clamp: x=$x, y=$y")
                }
                
                // Handle special tools differently
                // In split mode, use section's tool; otherwise use global tool
                val toolToUse = if (sectionManager.isSplitModeEnabled() && pointerSectionMap[pointerId] != null) {
                    pointerSectionMap[pointerId]!!.currentTool
                } else {
                    currentTool
                }
                
                android.util.Log.d("WhiteboardView", "Tool to use: $toolToUse, isDrawingMode: $isDrawingMode")
                
                when (toolToUse) {
                    DrawingTool.TEXT -> {
                        handleTextTouch(x, y, action)
                    }
                    DrawingTool.SELECTION -> {
                        handleSelectionTouch(x, y, action)
                    }
                    DrawingTool.SELECTIVE_ERASER -> {
                        handleSelectiveEraserTouch(x, y, action)
                    }
                    DrawingTool.LINE, DrawingTool.CIRCLE, DrawingTool.TRIANGLE, DrawingTool.RECTANGLE -> {
                        handleShapeTouch(x, y, action, toolToUse, pointerId)
                    }
                    else -> {
                        // Create new path for this pointer
                        android.util.Log.d("WhiteboardView", "Creating new path at ($x, $y) with tool: $toolToUse")
                        
                        // CRITICAL CHECK: Are coordinates within section bounds?
                        if (sectionManager.isSplitModeEnabled()) {
                            val section = pointerSectionMap[pointerId]
                            if (section != null) {
                                android.util.Log.d("WhiteboardView", "Section ${section.id} drawing bounds: ${section.drawingBounds}")
                                android.util.Log.d("WhiteboardView", "Touch point ($x, $y) in bounds? ${section.drawingBounds.contains(x, y)}")
                            }
                        }
                        
                        val newPath = Path()
                        newPath.moveTo(x, y)
                        activePaths[pointerId] = newPath
                        activePathsLastX[pointerId] = x
                        activePathsLastY[pointerId] = y
                        android.util.Log.d("WhiteboardView", "Path created, activePaths size: ${activePaths.size}")
                    }
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                // Handle TEXT and SELECTIVE_ERASER tools specially for first pointer only
                if (event.pointerCount == 1) {
                    val id = event.getPointerId(0)
                    val toolToUse = if (sectionManager.isSplitModeEnabled() && pointerSectionMap[id] != null) {
                        pointerSectionMap[id]!!.currentTool
                    } else {
                        currentTool
                    }
                    
                    when (toolToUse) {
                        DrawingTool.TEXT -> {
                            var x = if (sectionManager.isSplitModeEnabled()) event.x else (event.x + focusX) / scaleFactor
                            var y = if (sectionManager.isSplitModeEnabled()) event.y else (event.y + focusY) / scaleFactor
                            if (!sectionManager.isSplitModeEnabled()) {
                                val clamped = clampToBitmapBounds(x, y)
                                x = clamped.first
                                y = clamped.second
                            }
                            handleTextTouch(x, y, action)
                            return@handleDrawingTouch
                        }
                        DrawingTool.SELECTION -> {
                            var x = if (sectionManager.isSplitModeEnabled()) event.x else (event.x + focusX) / scaleFactor
                            var y = if (sectionManager.isSplitModeEnabled()) event.y else (event.y + focusY) / scaleFactor
                            if (!sectionManager.isSplitModeEnabled()) {
                                val clamped = clampToBitmapBounds(x, y)
                                x = clamped.first
                                y = clamped.second
                            }
                            handleSelectionTouch(x, y, action)
                            return@handleDrawingTouch
                        }
                        DrawingTool.SELECTIVE_ERASER -> {
                            var x = if (sectionManager.isSplitModeEnabled()) event.x else (event.x + focusX) / scaleFactor
                            var y = if (sectionManager.isSplitModeEnabled()) event.y else (event.y + focusY) / scaleFactor
                            if (!sectionManager.isSplitModeEnabled()) {
                                val clamped = clampToBitmapBounds(x, y)
                                x = clamped.first
                                y = clamped.second
                            }
                            handleSelectiveEraserTouch(x, y, action)
                            return@handleDrawingTouch
                        }
                        DrawingTool.LINE, DrawingTool.CIRCLE, DrawingTool.TRIANGLE, DrawingTool.RECTANGLE -> {
                            var x = if (sectionManager.isSplitModeEnabled()) event.x else (event.x + focusX) / scaleFactor
                            var y = if (sectionManager.isSplitModeEnabled()) event.y else (event.y + focusY) / scaleFactor
                            if (!sectionManager.isSplitModeEnabled()) {
                                val clamped = clampToBitmapBounds(x, y)
                                x = clamped.first
                                y = clamped.second
                            }
                            handleShapeTouch(x, y, action, toolToUse, id)
                            return@handleDrawingTouch
                        }
                        else -> {
                            // Continue to handle drawing below
                        }
                    }
                }
                
                // Handle all active pointers for drawing tools
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val path = activePaths[id]
                    val lastX = activePathsLastX[id]
                    val lastY = activePathsLastY[id]
                    
                    if (path != null && lastX != null && lastY != null) {
                        val screenX = event.getX(i)
                        val screenY = event.getY(i)
                        
                        // In split mode, check if still within the same section's drawing area (use screen coords)
                        if (sectionManager.isSplitModeEnabled()) {
                            val pointerSection = pointerSectionMap[id]
                            if (pointerSection != null && !pointerSection.containsInDrawingArea(screenX, screenY)) {
                                // Pointer moved outside its section's drawing area, stop drawing
                                continue
                            }
                        }
                        
                        // Transform to drawing coordinates
                        // In split mode use screen coords directly, in normal mode use transformed coords
                        var x = if (sectionManager.isSplitModeEnabled()) screenX else (screenX + focusX) / scaleFactor
                        var y = if (sectionManager.isSplitModeEnabled()) screenY else (screenY + focusY) / scaleFactor
                        
                        // Clamp coordinates to bitmap bounds in normal mode
                        if (!sectionManager.isSplitModeEnabled()) {
                            val clamped = clampToBitmapBounds(x, y)
                            x = clamped.first
                            y = clamped.second
                        }
                        
                        val dx = abs(x - lastX)
                        val dy = abs(y - lastY)
                        
                        if (dx >= 4 || dy >= 4) {
                            path.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2)
                            activePathsLastX[id] = x
                            activePathsLastY[id] = y
                            
                            // Invalidate to show drawing in real-time
                            invalidate()
                        }
                    }
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val toolToUse = if (sectionManager.isSplitModeEnabled() && pointerSectionMap[pointerId] != null) {
                    pointerSectionMap[pointerId]!!.currentTool
                } else {
                    currentTool
                }
                
                when (toolToUse) {
                    DrawingTool.TEXT -> {
                        val screenX = event.getX(pointerIndex)
                        val screenY = event.getY(pointerIndex)
                        var x = if (sectionManager.isSplitModeEnabled()) screenX else (screenX + focusX) / scaleFactor
                        var y = if (sectionManager.isSplitModeEnabled()) screenY else (screenY + focusY) / scaleFactor
                        if (!sectionManager.isSplitModeEnabled()) {
                            val clamped = clampToBitmapBounds(x, y)
                            x = clamped.first
                            y = clamped.second
                        }
                        handleTextTouch(x, y, action)
                    }
                    DrawingTool.SELECTION -> {
                        val screenX = event.getX(pointerIndex)
                        val screenY = event.getY(pointerIndex)
                        var x = if (sectionManager.isSplitModeEnabled()) screenX else (screenX + focusX) / scaleFactor
                        var y = if (sectionManager.isSplitModeEnabled()) screenY else (screenY + focusY) / scaleFactor
                        if (!sectionManager.isSplitModeEnabled()) {
                            val clamped = clampToBitmapBounds(x, y)
                            x = clamped.first
                            y = clamped.second
                        }
                        handleSelectionTouch(x, y, action)
                    }
                    DrawingTool.SELECTIVE_ERASER -> {
                        val screenX = event.getX(pointerIndex)
                        val screenY = event.getY(pointerIndex)
                        var x = if (sectionManager.isSplitModeEnabled()) screenX else (screenX + focusX) / scaleFactor
                        var y = if (sectionManager.isSplitModeEnabled()) screenY else (screenY + focusY) / scaleFactor
                        if (!sectionManager.isSplitModeEnabled()) {
                            val clamped = clampToBitmapBounds(x, y)
                            x = clamped.first
                            y = clamped.second
                        }
                        handleSelectiveEraserTouch(x, y, action)
                    }
                    DrawingTool.LINE, DrawingTool.CIRCLE, DrawingTool.TRIANGLE, DrawingTool.RECTANGLE -> {
                        val screenX = event.getX(pointerIndex)
                        val screenY = event.getY(pointerIndex)
                        var x = if (sectionManager.isSplitModeEnabled()) screenX else (screenX + focusX) / scaleFactor
                        var y = if (sectionManager.isSplitModeEnabled()) screenY else (screenY + focusY) / scaleFactor
                        if (!sectionManager.isSplitModeEnabled()) {
                            val clamped = clampToBitmapBounds(x, y)
                            x = clamped.first
                            y = clamped.second
                        }
                        handleShapeTouch(x, y, action, toolToUse, pointerId)
                    }
                    else -> {
                        val path = activePaths[pointerId]
                        val lastX = activePathsLastX[pointerId]
                        val lastY = activePathsLastY[pointerId]
                        
                        if (path != null && lastX != null && lastY != null) {
                            // Finish the path
                            path.lineTo(lastX, lastY)
                            
                            android.util.Log.d("WhiteboardView", "Finishing path, isSplitMode: ${sectionManager.isSplitModeEnabled()}")
                            
                            // Save the completed path
                            val pathCopy = Path(path)
                            
                            // In split mode, add to the section this pointer was drawing in
                            if (sectionManager.isSplitModeEnabled()) {
                                val pointerSection = pointerSectionMap[pointerId]
                                android.util.Log.d("WhiteboardView", "Pointer section: ${pointerSection?.id}")
                                
                                if (pointerSection != null) {
                                    // Use section's tool settings for this path
                                    val sectionPaint = Paint(getCurrentPaintForSection(pointerSection))
                                    val sectionDrawingPath = DrawingPath(pathCopy, sectionPaint, pointerSection.currentTool)
                                    
                                    pointerSection.paths.add(sectionDrawingPath)
                                    pointerSection.undoStack.clear() // Clear undo stack when new stroke is made
                                    
                                    android.util.Log.d("WhiteboardView", "Path added to section ${pointerSection.id}, total paths: ${pointerSection.paths.size}")
                                } else {
                                    android.util.Log.e("WhiteboardView", "Pointer section is null!")
                                }
                                // Remove pointer from section map
                                pointerSectionMap.remove(pointerId)
                            } else {
                                val paintCopy = Paint(getCurrentPaint())
                                val drawingPath = DrawingPath(pathCopy, paintCopy, currentTool)
                                paths.add(drawingPath)
                                // Draw to bitmap
                                drawingCanvas?.drawPath(pathCopy, paintCopy)
                                android.util.Log.d("WhiteboardView", "Path added to global paths, total: ${paths.size}")
                            }
                            
                            // Remove from active paths
                            activePaths.remove(pointerId)
                            activePathsLastX.remove(pointerId)
                            activePathsLastY.remove(pointerId)
                            
                            // Clear undo stack when new stroke is made (only for the last finger)
                            if (activePaths.isEmpty()) {
                                undoStack.clear()
                                // Notify listener of drawing change
                                drawingChangeListener?.onDrawingChanged()
                            }
                        }
                    }
                }
            }
            
            MotionEvent.ACTION_CANCEL -> {
                // Clear all active paths on cancel
                activePaths.clear()
                activePathsLastX.clear()
                activePathsLastY.clear()
                pointerSectionMap.clear()
            }
        }
        
        invalidate()
    }

    private fun handleSelectionTouch(x: Float, y: Float, action: Int) {
        // Handle selection mode - can select, drag, resize, and rotate text, shapes, and images
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if touching handles of already selected text first
                selectedTextObject?.let { textObj ->
                    // Check delete button first
                    if (textObj.isDeleteButtonTouched(x, y)) {
                        // Remove the text from the list
                        textObjects.remove(textObj)
                        selectedTextObject = null
                        drawingChangeListener?.onDrawingChanged()
                        invalidate()
                        return
                    }
                    
                    if (textObj.isRotationHandleTouched(x, y)) {
                        isRotatingText = true
                        dragStartX = x
                        dragStartY = y
                        initialRotation = textObj.rotation
                        return
                    }
                    
                    if (textObj.isResizeHandleTouched(x, y)) {
                        isResizingText = true
                        dragStartX = x
                        dragStartY = y
                        initialTextSize = textObj.textSize
                        return
                    }
                }
                
                // Check if touching handles of already selected shape
                selectedShapeObject?.let { shapeObj ->
                    // Check delete button first
                    if (shapeObj.isDeleteButtonTouched(x, y)) {
                        // Remove the shape from the list
                        shapeObjects.remove(shapeObj)
                        selectedShapeObject = null
                        drawingChangeListener?.onDrawingChanged()
                        invalidate()
                        return
                    }
                    
                    if (shapeObj.isRotationHandleTouched(x, y)) {
                        isRotatingShape = true
                        dragStartX = x
                        dragStartY = y
                        initialShapeRotation = shapeObj.rotation
                        return
                    }
                    
                    val handle = shapeObj.isResizeHandleTouched(x, y)
                    if (handle != null) {
                        isResizingShape = true
                        currentResizeHandle = handle
                        dragStartX = x
                        dragStartY = y
                        initialShapeStartX = shapeObj.startX
                        initialShapeStartY = shapeObj.startY
                        initialShapeEndX = shapeObj.endX
                        initialShapeEndY = shapeObj.endY
                        return
                    }
                }
                
                // Check if touching handles of already selected image
                selectedImageObject?.let { imageObj ->
                    // Check delete button first
                    if (imageObj.isDeleteButtonTouched(x, y)) {
                        // Remove the image from the list
                        imageObjects.remove(imageObj)
                        selectedImageObject = null
                        drawingChangeListener?.onDrawingChanged()
                        invalidate()
                        return
                    }
                    
                    if (imageObj.isRotationHandleTouched(x, y)) {
                        isRotatingImage = true
                        dragStartX = x
                        dragStartY = y
                        initialImageRotation = imageObj.rotation
                        return
                    }
                    
                    val handle = imageObj.isResizeHandleTouched(x, y)
                    if (handle != null) {
                        isResizingImage = true
                        currentImageResizeHandle = handle
                        dragStartX = x
                        dragStartY = y
                        initialImageX = imageObj.x
                        initialImageY = imageObj.y
                        initialImageWidth = imageObj.width
                        initialImageHeight = imageObj.height
                        initialImageScaleX = imageObj.scaleX
                        initialImageScaleY = imageObj.scaleY
                        return
                    }
                }
                
                // Check if touching text objects
                var touchedText: TextObject? = null
                for (textObj in textObjects.reversed()) {
                    if (textObj.contains(x, y)) {
                        touchedText = textObj
                        break
                    }
                }
                
                // Check if touching shape objects
                var touchedShape: ShapeObject? = null
                for (shapeObj in shapeObjects.reversed()) {
                    if (shapeObj.contains(x, y)) {
                        touchedShape = shapeObj
                        break
                    }
                }
                
                // Check if touching image objects
                var touchedImage: ImageObject? = null
                for (imageObj in imageObjects.reversed()) {
                    if (imageObj.contains(x, y)) {
                        touchedImage = imageObj
                        break
                    }
                }
                
                if (touchedText != null) {
                    // Deselect previous selections
                    selectedTextObject?.isSelected = false
                    selectedShapeObject?.isSelected = false
                    selectedImageObject?.isSelected = false
                    
                    // Select touched text
                    selectedTextObject = touchedText
                    touchedText.isSelected = true
                    isDraggingText = true
                    dragStartX = x
                    dragStartY = y
                } else if (touchedShape != null) {
                    // Deselect previous selections
                    selectedTextObject?.isSelected = false
                    selectedShapeObject?.isSelected = false
                    selectedImageObject?.isSelected = false
                    
                    // Select touched shape
                    selectedShapeObject = touchedShape
                    touchedShape.isSelected = true
                    isDraggingShape = true
                    dragStartX = x
                    dragStartY = y
                } else if (touchedImage != null) {
                    // Deselect previous selections
                    selectedTextObject?.isSelected = false
                    selectedShapeObject?.isSelected = false
                    selectedImageObject?.isSelected = false
                    
                    // Select touched image
                    selectedImageObject = touchedImage
                    touchedImage.isSelected = true
                    isDraggingImage = true
                    dragStartX = x
                    dragStartY = y
                } else {
                    // Deselect everything
                    selectedTextObject?.isSelected = false
                    selectedShapeObject?.isSelected = false
                    selectedImageObject?.isSelected = false
                    selectedTextObject = null
                    selectedShapeObject = null
                    selectedImageObject = null
                }
                
                invalidate()
            }
            
            MotionEvent.ACTION_MOVE -> {
                // Handle text transformations
                selectedTextObject?.let { textObj ->
                    if (isRotatingText) {
                        val bounds = Rect()
                        textObj.updatePaint()
                        textObj.paint.getTextBounds(textObj.text, 0, textObj.text.length, bounds)
                        val centerX = textObj.x + bounds.width() / 2f
                        val centerY = textObj.y + bounds.height() / 2f
                        
                        val startAngle = kotlin.math.atan2((dragStartY - centerY).toDouble(), (dragStartX - centerX).toDouble())
                        val currentAngle = kotlin.math.atan2((y - centerY).toDouble(), (x - centerX).toDouble())
                        val deltaAngle = Math.toDegrees(currentAngle - startAngle).toFloat()
                        
                        textObj.rotation = (initialRotation + deltaAngle) % 360f
                        invalidate()
                        return
                    } else if (isResizingText) {
                        val distance = kotlin.math.sqrt((x - dragStartX) * (x - dragStartX) + (y - dragStartY) * (y - dragStartY))
                        val scaleFactor = max(0.5f, min(3.0f, distance / 100f))
                        textObj.textSize = initialTextSize * scaleFactor
                        invalidate()
                        return
                    } else if (isDraggingText) {
                        val dx = x - dragStartX
                        val dy = y - dragStartY
                        textObj.x += dx
                        textObj.y += dy
                        dragStartX = x
                        dragStartY = y
                        invalidate()
                        return
                    }
                }
                
                // Handle shape transformations
                selectedShapeObject?.let { shapeObj ->
                    if (isRotatingShape) {
                        val centerX = (shapeObj.startX + shapeObj.endX) / 2f
                        val centerY = (shapeObj.startY + shapeObj.endY) / 2f
                        
                        val startAngle = kotlin.math.atan2((dragStartY - centerY).toDouble(), (dragStartX - centerX).toDouble())
                        val currentAngle = kotlin.math.atan2((y - centerY).toDouble(), (x - centerX).toDouble())
                        val deltaAngle = Math.toDegrees(currentAngle - startAngle).toFloat()
                        
                        shapeObj.rotation = (initialShapeRotation + deltaAngle) % 360f
                        invalidate()
                        return
                    } else if (isResizingShape) {
                        // For LINE shapes, allow direct endpoint manipulation
                        if (shapeObj.shapeType == DrawingTool.LINE) {
                            when (currentResizeHandle) {
                                ShapeObject.ResizeHandle.LINE_START -> {
                                    shapeObj.startX = x
                                    shapeObj.startY = y
                                }
                                ShapeObject.ResizeHandle.LINE_END -> {
                                    shapeObj.endX = x
                                    shapeObj.endY = y
                                }
                                else -> {}
                            }
                        } else {
                            // Use scale-based resizing for better handling of rotated shapes
                            val centerX = (initialShapeStartX + initialShapeEndX) / 2f
                            val centerY = (initialShapeStartY + initialShapeEndY) / 2f
                            
                            val initialDistance = kotlin.math.sqrt(
                                (dragStartX - centerX) * (dragStartX - centerX) + 
                                (dragStartY - centerY) * (dragStartY - centerY)
                            )
                            val currentDistance = kotlin.math.sqrt(
                                (x - centerX) * (x - centerX) + 
                                (y - centerY) * (y - centerY)
                            )
                            
                            val scaleFactor = if (initialDistance > 0) currentDistance / initialDistance else 1f
                            shapeObj.scaleX = max(0.1f, min(5.0f, scaleFactor))
                            shapeObj.scaleY = max(0.1f, min(5.0f, scaleFactor))
                        }
                        
                        invalidate()
                        return
                    } else if (isDraggingShape) {
                        val dx = x - dragStartX
                        val dy = y - dragStartY
                        shapeObj.startX += dx
                        shapeObj.startY += dy
                        shapeObj.endX += dx
                        shapeObj.endY += dy
                        dragStartX = x
                        dragStartY = y
                        invalidate()
                        return
                    }
                }
                
                // Handle image transformations
                selectedImageObject?.let { imageObj ->
                    if (isRotatingImage) {
                        val centerX = imageObj.getCenterX()
                        val centerY = imageObj.getCenterY()
                        
                        val startAngle = kotlin.math.atan2((dragStartY - centerY).toDouble(), (dragStartX - centerX).toDouble())
                        val currentAngle = kotlin.math.atan2((y - centerY).toDouble(), (x - centerX).toDouble())
                        val deltaAngle = Math.toDegrees(currentAngle - startAngle).toFloat()
                        
                        imageObj.rotation = (initialImageRotation + deltaAngle) % 360f
                        invalidate()
                        return
                    } else if (isResizingImage) {
                        // Calculate scale based on handle being dragged
                        val centerX = initialImageX + (initialImageWidth * initialImageScaleX) / 2f
                        val centerY = initialImageY + (initialImageHeight * initialImageScaleY) / 2f
                        
                        val initialDistance = kotlin.math.sqrt(
                            (dragStartX - centerX) * (dragStartX - centerX) + 
                            (dragStartY - centerY) * (dragStartY - centerY)
                        )
                        val currentDistance = kotlin.math.sqrt(
                            (x - centerX) * (x - centerX) + 
                            (y - centerY) * (y - centerY)
                        )
                        
                        val scaleFactor = if (initialDistance > 0) currentDistance / initialDistance else 1f
                        imageObj.scaleX = max(0.1f, min(5.0f, initialImageScaleX * scaleFactor))
                        imageObj.scaleY = max(0.1f, min(5.0f, initialImageScaleY * scaleFactor))
                        
                        // Adjust position to keep center point fixed
                        val newWidth = initialImageWidth * imageObj.scaleX
                        val newHeight = initialImageHeight * imageObj.scaleY
                        imageObj.x = centerX - newWidth / 2f
                        imageObj.y = centerY - newHeight / 2f
                        
                        invalidate()
                        return
                    } else if (isDraggingImage) {
                        val dx = x - dragStartX
                        val dy = y - dragStartY
                        imageObj.x += dx
                        imageObj.y += dy
                        dragStartX = x
                        dragStartY = y
                        invalidate()
                        return
                    }
                }
            }
            
            MotionEvent.ACTION_UP -> {
                isDraggingText = false
                isDraggingShape = false
                isDraggingImage = false
                isResizingText = false
                isRotatingText = false
                isResizingShape = false
                isRotatingShape = false
                isResizingImage = false
                isRotatingImage = false
                currentResizeHandle = null
                currentImageResizeHandle = null
                
                if (selectedTextObject != null || selectedShapeObject != null || selectedImageObject != null) {
                    drawingChangeListener?.onDrawingChanged()
                }
            }
        }
    }
    
    private fun handleTextTouch(x: Float, y: Float, action: Int) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if touching a rotation handle first
                selectedTextObject?.let { textObj ->
                    if (textObj.isRotationHandleTouched(x, y)) {
                        isRotatingText = true
                        dragStartX = x
                        dragStartY = y
                        initialRotation = textObj.rotation
                        return
                    }
                    
                    // Check if touching a resize handle
                    if (textObj.isResizeHandleTouched(x, y)) {
                        isResizingText = true
                        dragStartX = x
                        dragStartY = y
                        initialTextSize = textObj.textSize
                        return
                    }
                }
                
                // Check if touching any text object
                var touchedText: TextObject? = null
                for (textObj in textObjects.reversed()) { // Check from top to bottom
                    if (textObj.contains(x, y)) {
                        touchedText = textObj
                        break
                    }
                }
                
                if (touchedText != null) {
                    // Deselect previous selection
                    selectedTextObject?.isSelected = false
                    
                    // Select touched text
                    selectedTextObject = touchedText
                    touchedText.isSelected = true
                    isDraggingText = true
                    dragStartX = x
                    dragStartY = y
                } else {
                    // Deselect any selected text
                    selectedTextObject?.isSelected = false
                    selectedTextObject = null
                    
                    // Request new text input at this position
                    textInputListener?.onTextInputRequested(x, y)
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                selectedTextObject?.let { textObj ->
                    if (isRotatingText) {
                        // Calculate rotation based on angle from text center
                        val bounds = Rect()
                        textObj.updatePaint()
                        textObj.paint.getTextBounds(textObj.text, 0, textObj.text.length, bounds)
                        val centerX = textObj.x + bounds.width() / 2f
                        val centerY = textObj.y + bounds.height() / 2f
                        
                        val startAngle = kotlin.math.atan2((dragStartY - centerY).toDouble(), (dragStartX - centerX).toDouble())
                        val currentAngle = kotlin.math.atan2((y - centerY).toDouble(), (x - centerX).toDouble())
                        val deltaAngle = Math.toDegrees(currentAngle - startAngle).toFloat()
                        
                        textObj.rotation = (initialRotation + deltaAngle) % 360f
                    } else if (isResizingText) {
                        // Calculate resize based on distance from start point
                        val distance = kotlin.math.sqrt((x - dragStartX) * (x - dragStartX) + (y - dragStartY) * (y - dragStartY))
                        val scaleFactor = max(0.5f, min(3.0f, distance / 100f))
                        textObj.textSize = initialTextSize * scaleFactor
                    } else if (isDraggingText) {
                        // Move text object
                        val deltaX = x - dragStartX
                        val deltaY = y - dragStartY
                        textObj.x += deltaX
                        textObj.y += deltaY
                        dragStartX = x
                        dragStartY = y
                    }
                }
            }
            
            MotionEvent.ACTION_UP -> {
                isDraggingText = false
                isResizingText = false
                isRotatingText = false
                if (selectedTextObject != null) {
                    drawingChangeListener?.onDrawingChanged()
                }
            }
        }
    }

    private fun handleSelectiveEraserTouch(x: Float, y: Float, action: Int) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                // Start drawing selection area
                selectionPath = Path().apply {
                    moveTo(x, y)
                }
                isDrawingSelection = true
                invalidate() // Force redraw to show selection path
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isDrawingSelection) {
                    selectionPath?.lineTo(x, y)
                    invalidate() // Force redraw to show selection path
                }
            }
            
            MotionEvent.ACTION_UP -> {
                if (isDrawingSelection) {
                    selectionPath?.close() // Close the selection area
                    eraseElementsInSelection()
                    
                    // Clear selection path after erasing
                    selectionPath = null
                    isDrawingSelection = false
                    
                    drawingChangeListener?.onDrawingChanged()
                    invalidate() // Force redraw after erasing
                }
            }
        }
    }

    private fun handleShapeTouch(x: Float, y: Float, action: Int, tool: DrawingTool, pointerId: Int) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if touching a shape object first
                if (handleShapeObjectTouch(x, y, action)) {
                    return
                }
                
                // Start creating new shape
                isDrawingShape = true
                shapeStartX = x
                shapeStartY = y
                shapeEndX = x
                shapeEndY = y
                
                // Create preview path
                previewPath = createShapePath(tool, shapeStartX, shapeStartY, shapeEndX, shapeEndY)
                invalidate()
            }
            
            MotionEvent.ACTION_MOVE -> {
                // Handle shape object interactions
                if (handleShapeObjectTouch(x, y, action)) {
                    return
                }
                
                if (isDrawingShape) {
                    shapeEndX = x
                    shapeEndY = y
                    
                    // Update preview path
                    previewPath = createShapePath(tool, shapeStartX, shapeStartY, shapeEndX, shapeEndY)
                    invalidate()
                }
            }
            
            MotionEvent.ACTION_UP -> {
                // Handle shape object interactions
                if (handleShapeObjectTouch(x, y, action)) {
                    return
                }
                
                if (isDrawingShape) {
                    shapeEndX = x
                    shapeEndY = y
                    
                    // Create new shape object
                    val shapeObject = ShapeObject(
                        shapeType = tool,
                        startX = shapeStartX,
                        startY = shapeStartY,
                        endX = shapeEndX,
                        endY = shapeEndY,
                        color = currentColor,
                        strokeWidth = currentStrokeWidth
                    )
                    
                    // Add to shape objects list
                    shapeObjects.add(shapeObject)
                    
                    // Select the new shape
                    selectedShapeObject?.isSelected = false
                    selectedShapeObject = shapeObject
                    shapeObject.isSelected = true
                    
                    // Reset shape drawing state
                    isDrawingShape = false
                    previewPath = null
                    
                    // Clear undo stack and notify listener
                    undoStack.clear()
                    drawingChangeListener?.onDrawingChanged()
                    invalidate()
                }
            }
        }
    }

    private fun createShapePath(tool: DrawingTool, startX: Float, startY: Float, endX: Float, endY: Float): Path {
        val path = Path()
        
        when (tool) {
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
                val radius = kotlin.math.sqrt(
                    (endX - startX) * (endX - startX) + (endY - startY) * (endY - startY)
                ) / 2
                path.addCircle(centerX, centerY, radius, Path.Direction.CW)
            }
            
            DrawingTool.TRIANGLE -> {
                // Create an equilateral triangle
                val centerX = (startX + endX) / 2
                val centerY = (startY + endY) / 2
                val width = abs(endX - startX)
                val height = abs(endY - startY)
                val size = max(width, height)
                
                // Triangle points
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
                // Default to line for unsupported shapes
                path.moveTo(startX, startY)
                path.lineTo(endX, endY)
            }
        }
        
        return path
    }

    private fun handleShapeObjectTouch(x: Float, y: Float, action: Int): Boolean {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if touching a rotation handle first
                selectedShapeObject?.let { shapeObj ->
                    if (shapeObj.isRotationHandleTouched(x, y)) {
                        isRotatingShape = true
                        dragStartX = x
                        dragStartY = y
                        initialShapeRotation = shapeObj.rotation
                        return true
                    }
                    
                    // Check if touching a resize handle
                    val resizeHandle = shapeObj.isResizeHandleTouched(x, y)
                    if (resizeHandle != null) {
                        isResizingShape = true
                        currentResizeHandle = resizeHandle
                        dragStartX = x
                        dragStartY = y
                        initialShapeStartX = shapeObj.startX
                        initialShapeStartY = shapeObj.startY
                        initialShapeEndX = shapeObj.endX
                        initialShapeEndY = shapeObj.endY
                        return true
                    }
                }
                
                // Check if touching any shape object
                var touchedShape: ShapeObject? = null
                for (shapeObj in shapeObjects.reversed()) { // Check from top to bottom
                    if (shapeObj.contains(x, y)) {
                        touchedShape = shapeObj
                        break
                    }
                }
                
                if (touchedShape != null) {
                    // Deselect previous selections
                    selectedShapeObject?.isSelected = false
                    selectedTextObject?.isSelected = false
                    
                    // Select touched shape
                    selectedShapeObject = touchedShape
                    touchedShape.isSelected = true
                    selectedTextObject = null
                    isDraggingShape = true
                    dragStartX = x
                    dragStartY = y
                    return true
                } else {
                    // Deselect any selected shape
                    selectedShapeObject?.isSelected = false
                    selectedShapeObject = null
                    return false
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                selectedShapeObject?.let { shapeObj ->
                    if (isRotatingShape) {
                        // Calculate rotation based on angle from shape center
                        val centerX = shapeObj.getCenterX()
                        val centerY = shapeObj.getCenterY()
                        
                        val startAngle = kotlin.math.atan2((dragStartY - centerY).toDouble(), (dragStartX - centerX).toDouble())
                        val currentAngle = kotlin.math.atan2((y - centerY).toDouble(), (x - centerX).toDouble())
                        val deltaAngle = Math.toDegrees(currentAngle - startAngle).toFloat()
                        
                        shapeObj.rotation = (initialShapeRotation + deltaAngle) % 360f
                        invalidate()
                        return true
                    } else if (isResizingShape && currentResizeHandle != null) {
                        // Handle resizing based on which handle is being dragged
                        when (currentResizeHandle) {
                            ShapeObject.ResizeHandle.LINE_START -> {
                                // Drag start point of line
                                shapeObj.startX = x
                                shapeObj.startY = y
                            }
                            ShapeObject.ResizeHandle.LINE_END -> {
                                // Drag end point of line
                                shapeObj.endX = x
                                shapeObj.endY = y
                            }
                            ShapeObject.ResizeHandle.TOP_LEFT -> {
                                val deltaX = x - dragStartX
                                val deltaY = y - dragStartY
                                shapeObj.startX = initialShapeStartX + deltaX
                                shapeObj.startY = initialShapeStartY + deltaY
                            }
                            ShapeObject.ResizeHandle.TOP_RIGHT -> {
                                val deltaX = x - dragStartX
                                val deltaY = y - dragStartY
                                shapeObj.endX = initialShapeEndX + deltaX
                                shapeObj.startY = initialShapeStartY + deltaY
                            }
                            ShapeObject.ResizeHandle.BOTTOM_LEFT -> {
                                val deltaX = x - dragStartX
                                val deltaY = y - dragStartY
                                shapeObj.startX = initialShapeStartX + deltaX
                                shapeObj.endY = initialShapeEndY + deltaY
                            }
                            ShapeObject.ResizeHandle.BOTTOM_RIGHT -> {
                                val deltaX = x - dragStartX
                                val deltaY = y - dragStartY
                                shapeObj.endX = initialShapeEndX + deltaX
                                shapeObj.endY = initialShapeEndY + deltaY
                            }
                        }
                        invalidate()
                        return true
                    } else if (isDraggingShape) {
                        // Move shape object
                        val deltaX = x - dragStartX
                        val deltaY = y - dragStartY
                        shapeObj.startX += deltaX
                        shapeObj.startY += deltaY
                        shapeObj.endX += deltaX
                        shapeObj.endY += deltaY
                        dragStartX = x
                        dragStartY = y
                        invalidate()
                        return true
                    }
                }
                return false
            }
            
            MotionEvent.ACTION_UP -> {
                val wasInteracting = isDraggingShape || isResizingShape || isRotatingShape
                isDraggingShape = false
                isResizingShape = false
                isRotatingShape = false
                currentResizeHandle = null
                
                if (wasInteracting && selectedShapeObject != null) {
                    drawingChangeListener?.onDrawingChanged()
                }
                return wasInteracting
            }
        }
        return false
    }

    private fun eraseElementsInSelection() {
        selectionPath?.let { selectionPath ->
            // Create a region from the selection path for hit testing
            val region = Region()
            val rectF = RectF()
            selectionPath.computeBounds(rectF, true)
            region.setPath(selectionPath, Region(rectF.left.toInt(), rectF.top.toInt(), 
                                               rectF.right.toInt(), rectF.bottom.toInt()))
            
            // Debug: Log selection area
            android.util.Log.d("SelectiveEraser", "Selection bounds: ${rectF.left}, ${rectF.top}, ${rectF.right}, ${rectF.bottom}")
            android.util.Log.d("SelectiveEraser", "Total paths to check: ${paths.size}")
            android.util.Log.d("SelectiveEraser", "Total text objects to check: ${textObjects.size}")
            
            // Remove drawing paths that are completely within selection
            val pathsToRemove = mutableListOf<DrawingPath>()
            for (drawingPath in paths) {
                if (isPathCompletelyWithinRegion(drawingPath.path, region)) {
                    pathsToRemove.add(drawingPath)
                }
            }
            android.util.Log.d("SelectiveEraser", "Paths to remove: ${pathsToRemove.size}")
            paths.removeAll(pathsToRemove)
            
            // Remove text objects that are completely within selection
            val textObjectsToRemove = mutableListOf<TextObject>()
            for (textObject in textObjects) {
                if (isTextObjectCompletelyWithinRegion(textObject, region)) {
                    textObjectsToRemove.add(textObject)
                }
            }
            android.util.Log.d("SelectiveEraser", "Text objects to remove: ${textObjectsToRemove.size}")
            textObjects.removeAll(textObjectsToRemove)
            
            // Remove shape objects that are completely within selection
            val shapeObjectsToRemove = mutableListOf<ShapeObject>()
            for (shapeObject in shapeObjects) {
                if (isShapeObjectCompletelyWithinRegion(shapeObject, region)) {
                    shapeObjectsToRemove.add(shapeObject)
                }
            }
            android.util.Log.d("SelectiveEraser", "Shape objects to remove: ${shapeObjectsToRemove.size}")
            shapeObjects.removeAll(shapeObjectsToRemove)
            
            // Clear selected objects if they were removed
            if (selectedTextObject in textObjectsToRemove) {
                selectedTextObject = null
            }
            if (selectedShapeObject in shapeObjectsToRemove) {
                selectedShapeObject = null
            }
            
            // Redraw canvas without removed elements
            redrawCanvas()
        }
    }

    private fun isPathCompletelyWithinRegion(path: Path, region: Region): Boolean {
        val pathMeasure = PathMeasure(path, false)
        val length = pathMeasure.length
        
        if (length <= 0) return false
        
        val coords = FloatArray(2)
        
        // Sample points along the path and check if ALL are in the region
        val sampleCount = max(10, (length / 10).toInt()) // Reasonable sampling
        var pointsInside = 0
        var totalPoints = 0
        
        for (i in 0..sampleCount) {
            val distance = (i.toFloat() / sampleCount) * length
            pathMeasure.getPosTan(distance, coords, null)
            totalPoints++
            if (region.contains(coords[0].toInt(), coords[1].toInt())) {
                pointsInside++
            }
        }
        
        // Path is completely within if at least 80% of points are inside
        // This allows for some edge cases while still being selective
        return totalPoints > 0 && (pointsInside.toFloat() / totalPoints) >= 0.8f
    }

    private fun isTextObjectCompletelyWithinRegion(textObject: TextObject, region: Region): Boolean {
        // Get text bounds
        val bounds = Rect()
        textObject.updatePaint()
        textObject.paint.getTextBounds(textObject.text, 0, textObject.text.length, bounds)
        
        // Check all corners of the text bounding box
        val left = textObject.x.toInt()
        val top = (textObject.y + bounds.top).toInt()
        val right = (textObject.x + bounds.width()).toInt()
        val bottom = (textObject.y + bounds.bottom).toInt()
        
        // Text is completely within region only if all corners are inside
        return region.contains(left, top) && 
               region.contains(right, top) && 
               region.contains(left, bottom) && 
               region.contains(right, bottom)
    }

    private fun isShapeObjectCompletelyWithinRegion(shapeObject: ShapeObject, region: Region): Boolean {
        // Get shape bounds
        val bounds = shapeObject.getBounds()
        
        // Check all corners of the shape bounding box
        val left = bounds.left.toInt()
        val top = bounds.top.toInt()
        val right = bounds.right.toInt()
        val bottom = bounds.bottom.toInt()
        
        // Shape is completely within region only if all corners are inside
        return region.contains(left, top) && 
               region.contains(right, top) && 
               region.contains(left, bottom) && 
               region.contains(right, bottom)
    }

    private fun handlePanningTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                activePointerId = event.getPointerId(0)
                isPanning = true
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isPanning && !isScaling) {
                    val pointerIndex = event.findPointerIndex(activePointerId)
                    if (pointerIndex != -1) {
                        val x = event.getX(pointerIndex)
                        val y = event.getY(pointerIndex)
                        
                        val dx = x - lastTouchX
                        val dy = y - lastTouchY
                        
                        // Reverse directions: swipe up moves canvas down, swipe left moves canvas right
                        focusX -= dx
                        focusY -= dy
                        
                        // Constrain focus to bitmap boundaries
                        constrainFocusToBounds()
                        
                        lastTouchX = x
                        lastTouchY = y
                        
                        invalidate()
                    }
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPanning = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }
        }
    }
    
    /**
     * Constrains focus position to stay within bitmap boundaries
     * This prevents panning outside the drawable area
     */
    private fun constrainFocusToBounds() {
        // Get bitmap dimensions
        val bitmapWidth = drawingBitmap?.width?.toFloat() ?: width.toFloat()
        val bitmapHeight = drawingBitmap?.height?.toFloat() ?: height.toFloat()
        
        // Calculate the scaled bitmap size
        val scaledBitmapWidth = bitmapWidth * scaleFactor
        val scaledBitmapHeight = bitmapHeight * scaleFactor
        
        // If bitmap fits entirely in view, center it
        if (scaledBitmapWidth <= width) {
            focusX = -(width - scaledBitmapWidth) / 2f
        } else {
            // Calculate max focus values to keep bitmap edges within view
            val maxX = bitmapWidth - width / scaleFactor
            focusX = focusX.coerceIn(0f, maxX * scaleFactor)
        }
        
        if (scaledBitmapHeight <= height) {
            focusY = -(height - scaledBitmapHeight) / 2f
        } else {
            // Calculate max focus values to keep bitmap edges within view
            val maxY = bitmapHeight - height / scaleFactor
            focusY = focusY.coerceIn(0f, maxY * scaleFactor)
        }
    }
    
    /**
     * Clamps drawing coordinates to stay within bitmap boundaries
     */
    private fun clampToBitmapBounds(x: Float, y: Float): Pair<Float, Float> {
        val bitmapWidth = drawingBitmap?.width?.toFloat() ?: width.toFloat()
        val bitmapHeight = drawingBitmap?.height?.toFloat() ?: height.toFloat()
        
        val clampedX = x.coerceIn(0f, bitmapWidth)
        val clampedY = y.coerceIn(0f, bitmapHeight)
        
        return Pair(clampedX, clampedY)
    }

    private fun getCurrentPaint(): Paint {
        return when (currentTool) {
            DrawingTool.PEN -> drawPaint.apply {
                color = currentColor
                strokeWidth = currentStrokeWidth
                xfermode = null
            }
            DrawingTool.ERASER -> eraserPaint.apply {
                strokeWidth = currentStrokeWidth * 2
            }
            DrawingTool.TEXT -> textPaint.apply {
                color = currentColor
                textSize = currentStrokeWidth * 6 // Scale text size with stroke width
            }
            DrawingTool.SELECTION -> drawPaint // Selection mode doesn't draw, but return a paint anyway
            DrawingTool.SELECTIVE_ERASER -> selectionPaint // Use selection paint for drawing selection area
            DrawingTool.LINE, DrawingTool.CIRCLE, DrawingTool.TRIANGLE, DrawingTool.RECTANGLE -> drawPaint.apply {
                color = currentColor
                strokeWidth = currentStrokeWidth
                xfermode = null
                style = Paint.Style.STROKE
            }
        }
    }
    
    private fun getCurrentPaintForSection(section: SectionManager.Section): Paint {
        return when (section.currentTool) {
            DrawingTool.PEN -> Paint(drawPaint).apply {
                color = section.currentColor
                strokeWidth = section.currentStrokeWidth
                xfermode = null
            }
            DrawingTool.ERASER -> Paint(eraserPaint).apply {
                strokeWidth = section.currentStrokeWidth * 2
            }
            DrawingTool.TEXT -> Paint(textPaint).apply {
                color = section.currentColor
                textSize = section.currentStrokeWidth * 6
            }
            DrawingTool.SELECTION -> Paint(drawPaint) // Selection mode doesn't draw, but return a paint anyway
            DrawingTool.SELECTIVE_ERASER -> Paint(selectionPaint)
            DrawingTool.LINE, DrawingTool.CIRCLE, DrawingTool.TRIANGLE, DrawingTool.RECTANGLE -> Paint(drawPaint).apply {
                color = section.currentColor
                strokeWidth = section.currentStrokeWidth
                xfermode = null
                style = Paint.Style.STROKE
            }
        }
    }

    // Public methods for tool management
    fun setDrawingTool(tool: DrawingTool) {
        currentTool = tool
    }

    fun setColor(color: Int) {
        currentColor = color
    }

    fun setStrokeWidth(width: Float) {
        currentStrokeWidth = width
    }

    fun setDrawingMode(enabled: Boolean) {
        isDrawingMode = enabled
    }

    fun setOnDrawingChangeListener(listener: OnDrawingChangeListener?) {
        drawingChangeListener = listener
    }

    fun setOnTextInputListener(listener: OnTextInputListener?) {
        textInputListener = listener
    }

    /**
     * Converts screen coordinates to canvas coordinates, accounting for zoom and pan
     */
    private fun screenToCanvasX(screenX: Float): Float {
        return (screenX / scaleFactor) + focusX
    }
    
    private fun screenToCanvasY(screenY: Float): Float {
        return (screenY / scaleFactor) + focusY
    }
    
    /**
     * Gets the center of the current visible canvas area (accounting for zoom and pan)
     */
    fun getVisibleCanvasCenterX(): Float {
        return screenToCanvasX(width / 2f)
    }
    
    fun getVisibleCanvasCenterY(): Float {
        return screenToCanvasY(height / 2f)
    }

    fun addText(text: String, x: Float, y: Float) {
        if (text.isNotEmpty()) {
            // Create new text object
            val textObject = TextObject(
                text = text,
                x = x,
                y = y,
                textSize = currentStrokeWidth * 6, // Scale with stroke width
                color = currentColor
            )
            
            // In split mode, add to the section containing the text position
            if (sectionManager.isSplitModeEnabled()) {
                val targetSection = sectionManager.getSectionAt(x, y)
                if (targetSection != null) {
                    targetSection.textObjects.add(textObject)
                    targetSection.selectedTextObject?.isSelected = false
                    targetSection.selectedTextObject = textObject
                    textObject.isSelected = true
                }
            } else {
                // Add to global text objects list
                textObjects.add(textObject)
                
                // Select the new text object
                selectedTextObject?.isSelected = false
                selectedTextObject = textObject
                textObject.isSelected = true
            }
            
            // Notify listener and redraw
            drawingChangeListener?.onDrawingChanged()
            invalidate()
        }
    }

    fun addImage(bitmap: Bitmap, x: Float, y: Float) {
        // Create new image object centered at the given position
        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()
        
        // Position the image so that its center is at (x, y)
        val imageX = x - imageWidth / 2f
        val imageY = y - imageHeight / 2f
        
        val imageObject = ImageObject(
            bitmap = bitmap,
            x = imageX,
            y = imageY,
            width = imageWidth,
            height = imageHeight
        )
        
        // Add to global image objects list
        imageObjects.add(imageObject)
        
        // Select the new image object
        selectedImageObject?.isSelected = false
        selectedTextObject?.isSelected = false
        selectedShapeObject?.isSelected = false
        selectedImageObject = imageObject
        imageObject.isSelected = true
        
        // Notify listener and redraw
        drawingChangeListener?.onDrawingChanged()
        invalidate()
    }

    fun setPinchModeEnabled(enabled: Boolean) {
        isPinchModeEnabled = enabled
    }

    fun isPinchModeEnabled(): Boolean {
        return isPinchModeEnabled
    }
    
    fun setBackgroundPattern(pattern: BackgroundPattern) {
        currentBackgroundPattern = pattern
        redrawCanvas()
    }
    
    fun getBackgroundPattern(): BackgroundPattern {
        return currentBackgroundPattern
    }
    
    /**
     * Sets the zoom level programmatically
     * @param zoomLevel value between minScaleFactor and 5.0f
     */
    fun setZoomLevel(zoomLevel: Float) {
        scaleFactor = zoomLevel.coerceIn(minScaleFactor, 5.0f)
        constrainFocusToBounds()
        invalidate()
    }
    
    /**
     * Gets the current zoom level
     * @return current scale factor
     */
    fun getZoomLevel(): Float {
        return scaleFactor
    }
    
    /**
     * Gets the minimum allowed zoom level
     * @return minimum scale factor
     */
    fun getMinZoomLevel(): Float {
        return minScaleFactor
    }
    
    /**
     * Gets the maximum allowed zoom level
     * @return maximum scale factor (5.0f)
     */
    fun getMaxZoomLevel(): Float {
        return 5.0f
    }

    fun undo() {
        if (sectionManager.isSplitModeEnabled() && currentActiveSection != null) {
            currentActiveSection!!.undo()
            drawingChangeListener?.onDrawingChanged()
            invalidate()
        } else if (canUndo()) {
            val lastPath = paths.removeAt(paths.size - 1)
            undoStack.add(lastPath)
            redrawCanvas()
            drawingChangeListener?.onDrawingChanged()
        }
    }

    fun redo() {
        if (sectionManager.isSplitModeEnabled() && currentActiveSection != null) {
            currentActiveSection!!.redo()
            drawingChangeListener?.onDrawingChanged()
            invalidate()
        } else if (canRedo()) {
            val pathToRedo = undoStack.removeAt(undoStack.size - 1)
            paths.add(pathToRedo)
            redrawCanvas()
            drawingChangeListener?.onDrawingChanged()
        }
    }

    fun clear() {
        if (sectionManager.isSplitModeEnabled() && currentActiveSection != null) {
            currentActiveSection!!.clear()
            drawingChangeListener?.onDrawingChanged()
            invalidate()
        } else {
            paths.clear()
            undoStack.clear()
            activePaths.clear()
            activePathsLastX.clear()
            activePathsLastY.clear()
            textObjects.clear()
            selectedTextObject = null
            shapeObjects.clear()
            selectedShapeObject = null
            imageObjects.clear()
            selectedImageObject = null
            selectionPath = null
            isDrawingSelection = false
            // Reset shape drawing state
            isDrawingShape = false
            previewPath = null
            // Reset shape interaction state
            isDraggingShape = false
            isResizingShape = false
            isRotatingShape = false
            // Reset image interaction state
            isDraggingImage = false
            isResizingImage = false
            isRotatingImage = false
            redrawCanvas()
            drawingChangeListener?.onDrawingChanged()
        }
    }

    private fun redrawCanvas() {
        // Clear the entire canvas
        drawingCanvas?.drawColor(Color.WHITE, PorterDuff.Mode.SRC)
        
        // Draw background pattern on canvas
        drawingCanvas?.let { canvas ->
            drawBackgroundPattern(canvas)
        }
        
        // Redraw all paths and text
        for (drawingPath in paths) {
            if (drawingPath.tool == DrawingTool.TEXT && drawingPath.text != null) {
                // Draw text
                drawingCanvas?.drawText(drawingPath.text, drawingPath.textX, drawingPath.textY, drawingPath.paint)
            } else {
                // Draw path
                drawingCanvas?.drawPath(drawingPath.path, drawingPath.paint)
            }
        }
        
        // Force a redraw of the view
        invalidate()
    }
    
    private fun drawBackgroundPattern(canvas: Canvas) {
        if (currentBackgroundPattern == BackgroundPattern.PLAIN) {
            return // No pattern to draw
        }
        
        val patternPaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        
        val canvasWidth = drawingBitmap?.width?.toFloat() ?: width.toFloat()
        val canvasHeight = drawingBitmap?.height?.toFloat() ?: height.toFloat()
        
        when (currentBackgroundPattern) {
            BackgroundPattern.GRID_SMALL -> {
                val spacing = 30f
                // Draw vertical lines
                var x = 0f
                while (x <= canvasWidth) {
                    canvas.drawLine(x, 0f, x, canvasHeight, patternPaint)
                    x += spacing
                }
                // Draw horizontal lines
                var y = 0f
                while (y <= canvasHeight) {
                    canvas.drawLine(0f, y, canvasWidth, y, patternPaint)
                    y += spacing
                }
            }
            BackgroundPattern.GRID_LARGE -> {
                val spacing = 60f
                // Draw vertical lines
                var x = 0f
                while (x <= canvasWidth) {
                    canvas.drawLine(x, 0f, x, canvasHeight, patternPaint)
                    x += spacing
                }
                // Draw horizontal lines
                var y = 0f
                while (y <= canvasHeight) {
                    canvas.drawLine(0f, y, canvasWidth, y, patternPaint)
                    y += spacing
                }
            }
            BackgroundPattern.LINES_HORIZONTAL -> {
                val spacing = 40f
                var y = 0f
                while (y <= canvasHeight) {
                    canvas.drawLine(0f, y, canvasWidth, y, patternPaint)
                    y += spacing
                }
            }
            BackgroundPattern.LINES_VERTICAL -> {
                val spacing = 40f
                var x = 0f
                while (x <= canvasWidth) {
                    canvas.drawLine(x, 0f, x, canvasHeight, patternPaint)
                    x += spacing
                }
            }
            BackgroundPattern.LINES_BOTH -> {
                val spacing = 40f
                // Draw vertical lines
                var x = 0f
                while (x <= canvasWidth) {
                    canvas.drawLine(x, 0f, x, canvasHeight, patternPaint)
                    x += spacing
                }
                // Draw horizontal lines
                var y = 0f
                while (y <= canvasHeight) {
                    canvas.drawLine(0f, y, canvasWidth, y, patternPaint)
                    y += spacing
                }
            }
            BackgroundPattern.DOT_GRID -> {
                val spacing = 30f
                val dotRadius = 2f
                patternPaint.style = Paint.Style.FILL
                var y = 0f
                while (y <= canvasHeight) {
                    var x = 0f
                    while (x <= canvasWidth) {
                        canvas.drawCircle(x, y, dotRadius, patternPaint)
                        x += spacing
                    }
                    y += spacing
                }
            }
            BackgroundPattern.RULED_PAPER -> {
                val spacing = 40f
                patternPaint.color = Color.parseColor("#B0BEC5")
                var y = 0f
                while (y <= canvasHeight) {
                    canvas.drawLine(0f, y, canvasWidth, y, patternPaint)
                    y += spacing
                }
            }
            BackgroundPattern.GRAPH_PAPER -> {
                val smallSpacing = 20f
                val largeSpacing = 100f
                
                // Draw small grid
                patternPaint.color = Color.parseColor("#EEEEEE")
                patternPaint.strokeWidth = 0.5f
                var x = 0f
                while (x <= canvasWidth) {
                    canvas.drawLine(x, 0f, x, canvasHeight, patternPaint)
                    x += smallSpacing
                }
                var y = 0f
                while (y <= canvasHeight) {
                    canvas.drawLine(0f, y, canvasWidth, y, patternPaint)
                    y += smallSpacing
                }
                
                // Draw large grid
                patternPaint.color = Color.parseColor("#BDBDBD")
                patternPaint.strokeWidth = 1.5f
                x = 0f
                while (x <= canvasWidth) {
                    canvas.drawLine(x, 0f, x, canvasHeight, patternPaint)
                    x += largeSpacing
                }
                y = 0f
                while (y <= canvasHeight) {
                    canvas.drawLine(0f, y, canvasWidth, y, patternPaint)
                    y += largeSpacing
                }
            }
            BackgroundPattern.PLAIN -> {
                // Do nothing - already handled at the start
            }
        }
    }

    // Gesture listeners
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Reset zoom and pan to center of bitmap
            scaleFactor = 1.0f
            
            // Center the view on the bitmap
            val bitmapWidth = drawingBitmap?.width?.toFloat() ?: width.toFloat()
            val bitmapHeight = drawingBitmap?.height?.toFloat() ?: height.toFloat()
            focusX = (bitmapWidth - width) / 2f
            focusY = (bitmapHeight - height) / 2f
            
            // Ensure we're within bounds
            constrainFocusToBounds()
            
            invalidate()
            return true
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isScaling = true
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            
            // Constrain scale factor to prevent seeing empty space outside bitmap
            // Use dynamic minimum based on bitmap/view ratio, and max 5x zoom
            scaleFactor = max(minScaleFactor, min(scaleFactor, 5.0f))
            
            // Adjust focus point
            focusX = detector.focusX - (detector.focusX - focusX) * detector.scaleFactor
            focusY = detector.focusY - (detector.focusY - focusY) * detector.scaleFactor
            
            // Constrain focus to bitmap boundaries after zooming
            constrainFocusToBounds()
            
            invalidate()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isScaling = false
        }
    }

    // Cleanup method
    fun cleanup() {
        drawingBitmap?.recycle()
        drawingBitmap = null
        drawingCanvas = null
    }
    
    // Handle touch in control toolbar - using exact same buttons as main toolbar
    private fun handleControlAreaTouch(section: SectionManager.Section, x: Float, y: Float) {
        val controlArea = section.controlBounds
        val sectionWidth = section.bounds.width()
        
        // Button dimensions matching main toolbar style
        val buttonSize = 32f
        val buttonSpacing = 4f
        val padding = 8f
        
        // Calculate total buttons and spacing
        val buttonCount = 7 // Pen, Eraser, Undo, Redo, Clear, Color, Stroke
        val centerY = controlArea.centerY()
        val buttonTop = centerY - buttonSize / 2
        val buttonBottom = centerY + buttonSize / 2
        
        // Check if touch is within button row
        if (y < buttonTop || y > buttonBottom) return
        
        // Calculate button positions
        val startX = controlArea.left + (sectionWidth - (buttonCount * buttonSize + (buttonCount - 1) * buttonSpacing)) / 2
        
        // Determine which button was pressed
        val relativeX = x - startX
        if (relativeX < 0) return
        
        val buttonIndex = (relativeX / (buttonSize + buttonSpacing)).toInt()
        if (buttonIndex >= buttonCount) return
        
        val buttonLeft = startX + buttonIndex * (buttonSize + buttonSpacing)
        val buttonRight = buttonLeft + buttonSize
        
        // Verify touch is within button bounds
        if (x < buttonLeft || x > buttonRight) return
        
        // Handle button press based on index
        when (buttonIndex) {
            0 -> {
                // Pen button
                section.currentTool = DrawingTool.PEN
                invalidate()
            }
            1 -> {
                // Eraser button
                section.currentTool = DrawingTool.ERASER
                invalidate()
            }
            2 -> {
                // Undo button
                if (section.canUndo()) {
                    section.undo()
                    invalidate()
                }
            }
            3 -> {
                // Redo button
                if (section.canRedo()) {
                    section.redo()
                    invalidate()
                }
            }
            4 -> {
                // Clear button
                section.clear()
                invalidate()
            }
            5 -> {
                // Color button - show popup
                showSectionColorPopup(section, buttonLeft, buttonTop)
            }
            6 -> {
                // Stroke width button - show popup
                showSectionStrokePopup(section, buttonLeft, buttonTop)
            }
        }
    }
    
    // Show color picker popup for a specific section
    private fun showSectionColorPopup(section: SectionManager.Section, x: Float, y: Float) {
        // Cycle through colors for now (popup implementation needed in Activity)
        val colors = listOf(
            Color.BLACK,
            Color.parseColor("#F44336"), // Red
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#FFEB3B"), // Yellow
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#FF9800")  // Orange
        )
        val currentIndex = colors.indexOf(section.currentColor)
        section.currentColor = colors[(currentIndex + 1) % colors.size]
        invalidate()
    }
    
    // Show stroke width popup for a specific section
    private fun showSectionStrokePopup(section: SectionManager.Section, x: Float, y: Float) {
        // Cycle through widths for now (popup implementation needed in Activity)
        val widths = listOf(2f, 5f, 8f, 12f, 16f, 20f)
        val currentIndex = widths.indexOfFirst { kotlin.math.abs(it - section.currentStrokeWidth) < 1f }
        section.currentStrokeWidth = widths[(if (currentIndex >= 0) currentIndex + 1 else 0) % widths.size]
        invalidate()
    }
    
    // Draw control toolbar using actual drawable icons like main toolbar
    private fun drawSectionControls(canvas: Canvas) {
        // Load drawable resources
        val iconPen = context.getDrawable(android.support.v7.appcompat.R.drawable.abc_ic_menu_cut_mtrl_alpha)
        val iconEraser = context.getDrawable(android.support.v7.appcompat.R.drawable.abc_ic_clear_material)
        val iconUndo = context.getDrawable(android.support.v7.appcompat.R.drawable.abc_ic_ab_back_material)
        val iconRedo = context.getDrawable(android.support.v7.appcompat.R.drawable.abc_ic_go_search_api_material)
        val iconClear = context.getDrawable(android.support.v7.appcompat.R.drawable.abc_ic_clear_material)
        
        // Button background paint
        val buttonBg = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.parseColor("#F5F5F5")
        }
        
        val selectedBg = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.parseColor("#E0E0E0")
        }
        
        val borderPaint = Paint().apply {
            color = Color.parseColor("#CCCCCC")
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        
        for (section in sectionManager.getAllSections()) {
            val controlArea = section.controlBounds
            val sectionWidth = section.bounds.width()
            
            // Button dimensions matching main toolbar
            val buttonSize = 32f
            val buttonSpacing = 4f
            val buttonCount = 7
            
            // Center buttons in control area
            val totalWidth = buttonCount * buttonSize + (buttonCount - 1) * buttonSpacing
            val startX = controlArea.left + (sectionWidth - totalWidth) / 2
            val centerY = controlArea.centerY()
            val buttonTop = centerY - buttonSize / 2
            
            // Draw buttons: Pen, Eraser, Undo, Redo, Clear, Color, Stroke
            for (i in 0 until buttonCount) {
                val buttonLeft = startX + i * (buttonSize + buttonSpacing)
                val buttonRight = buttonLeft + buttonSize
                val buttonBottom = buttonTop + buttonSize
                val rect = RectF(buttonLeft, buttonTop, buttonRight, buttonBottom)
                
                // Determine if button is active/selected
                val isSelected = when (i) {
                    0 -> section.currentTool == DrawingTool.PEN
                    1 -> section.currentTool == DrawingTool.ERASER
                    else -> false
                }
                
                val isEnabled = when (i) {
                    2 -> section.canUndo()
                    3 -> section.canRedo()
                    else -> true
                }
                
                // Draw button background
                if (isSelected) {
                    canvas.drawRoundRect(rect, 4f, 4f, selectedBg)
                } else {
                    canvas.drawRoundRect(rect, 4f, 4f, buttonBg)
                }
                canvas.drawRoundRect(rect, 4f, 4f, borderPaint)
                
                // Draw button icon/content
                when (i) {
                    0 -> drawIcon(canvas, "✎", rect, isSelected) // Pen
                    1 -> drawIcon(canvas, "⌫", rect, isSelected) // Eraser
                    2 -> drawIcon(canvas, "↶", rect, isEnabled) // Undo
                    3 -> drawIcon(canvas, "↷", rect, isEnabled) // Redo
                    4 -> drawIcon(canvas, "✕", rect, true) // Clear
                    5 -> drawColorButton(canvas, rect, section.currentColor) // Color
                    6 -> drawStrokeButton(canvas, rect, section.currentStrokeWidth) // Stroke
                }
            }
        }
    }
    
    // Helper to draw icon text
    private fun drawIcon(canvas: Canvas, icon: String, rect: RectF, enabled: Boolean) {
        val paint = Paint().apply {
            isAntiAlias = true
            color = if (enabled) Color.parseColor("#333333") else Color.parseColor("#AAAAAA")
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText(icon, rect.centerX(), rect.centerY() + 7f, paint)
    }
    
    // Helper to draw color button
    private fun drawColorButton(canvas: Canvas, rect: RectF, color: Int) {
        val colorPaint = Paint().apply {
            style = Paint.Style.FILL
            this.color = color
            isAntiAlias = true
        }
        val innerRect = RectF(
            rect.left + 6f,
            rect.top + 6f,
            rect.right - 6f,
            rect.bottom - 6f
        )
        canvas.drawRoundRect(innerRect, 2f, 2f, colorPaint)
        
        // Draw border
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            this.color = Color.parseColor("#888888")
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRoundRect(innerRect, 2f, 2f, borderPaint)
    }
    
    // Helper to draw stroke width button
    private fun drawStrokeButton(canvas: Canvas, rect: RectF, strokeWidth: Float) {
        val strokePaint = Paint().apply {
            color = Color.parseColor("#333333")
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth.coerceIn(2f, 10f)
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        val margin = rect.width() * 0.25f
        canvas.drawLine(
            rect.left + margin,
            rect.centerY(),
            rect.right - margin,
            rect.centerY(),
            strokePaint
        )
    }

    // Section management methods
    fun enableSplitMode(numberOfSections: Int) {
        sectionManager.enableSplitMode(numberOfSections)
        invalidate()
    }
    
    fun disableSplitMode() {
        sectionManager.disableSplitMode()
        currentActiveSection = null
        invalidate()
    }
    
    fun isSplitModeEnabled(): Boolean = sectionManager.isSplitModeEnabled()
    
    fun getAllSections(): List<SectionManager.Section> = sectionManager.getAllSections()
    
    fun setActiveSection(sectionId: Int) {
        val section = sectionManager.getAllSections().find { it.id == sectionId }
        currentActiveSection = section
        sectionManager.setActiveSection(section)
        
        // Update current tool settings from the section
        section?.let {
            currentTool = it.currentTool
            currentColor = it.currentColor
            currentStrokeWidth = it.currentStrokeWidth
        }
        invalidate()
    }
    
    fun getActiveSection(): SectionManager.Section? = currentActiveSection
    
    fun updateSectionTool(sectionId: Int, tool: DrawingTool) {
        val section = sectionManager.getAllSections().find { it.id == sectionId }
        section?.currentTool = tool
        if (section == currentActiveSection) {
            currentTool = tool
        }
    }
    
    fun updateSectionColor(sectionId: Int, color: Int) {
        val section = sectionManager.getAllSections().find { it.id == sectionId }
        section?.currentColor = color
        if (section == currentActiveSection) {
            currentColor = color
        }
    }
    
    fun updateSectionStrokeWidth(sectionId: Int, width: Float) {
        val section = sectionManager.getAllSections().find { it.id == sectionId }
        section?.currentStrokeWidth = width
        if (section == currentActiveSection) {
            currentStrokeWidth = width
        }
    }
    
    fun undoSection(sectionId: Int) {
        val section = sectionManager.getAllSections().find { it.id == sectionId }
        section?.undo()
        drawingChangeListener?.onDrawingChanged()
        invalidate()
    }
    
    fun redoSection(sectionId: Int) {
        val section = sectionManager.getAllSections().find { it.id == sectionId }
        section?.redo()
        drawingChangeListener?.onDrawingChanged()
        invalidate()
    }
    
    fun clearSection(sectionId: Int) {
        val section = sectionManager.getAllSections().find { it.id == sectionId }
        section?.clear()
        drawingChangeListener?.onDrawingChanged()
        invalidate()
    }
    
    fun clearAllSections() {
        sectionManager.clearAllSections()
        drawingChangeListener?.onDrawingChanged()
        invalidate()
    }

    // Getters for current state
    fun getCurrentTool(): DrawingTool = currentTool
    fun getCurrentColor(): Int = currentColor
    fun getCurrentStrokeWidth(): Float = currentStrokeWidth
    fun canUndo(): Boolean = if (sectionManager.isSplitModeEnabled() && currentActiveSection != null) {
        currentActiveSection!!.canUndo()
    } else {
        paths.isNotEmpty()
    }
    fun canRedo(): Boolean = if (sectionManager.isSplitModeEnabled() && currentActiveSection != null) {
        currentActiveSection!!.canRedo()
    } else {
        undoStack.isNotEmpty()
    }
    
    // Page management methods
    fun getAllPaths(): List<DrawingPath> = paths.toList()
    
    fun getAllTextObjects(): List<TextObject> = textObjects.toList()
    
    fun getAllShapeObjects(): List<ShapeObject> = shapeObjects.toList()
    
    fun getAllImageObjects(): List<ImageObject> = imageObjects.toList()
    
    fun loadPageData(
        pagePaths: List<DrawingPath>,
        pageTextObjects: List<TextObject>,
        pageShapeObjects: List<ShapeObject>,
        pageImageObjects: List<ImageObject> = emptyList()
    ) {
        // Clear current state
        paths.clear()
        undoStack.clear()
        textObjects.clear()
        shapeObjects.clear()
        imageObjects.clear()
        selectedTextObject = null
        selectedShapeObject = null
        selectedImageObject = null
        
        // Load page data
        paths.addAll(pagePaths)
        textObjects.addAll(pageTextObjects)
        shapeObjects.addAll(pageShapeObjects)
        imageObjects.addAll(pageImageObjects)
        
        // Redraw canvas
        redrawCanvas()
        invalidate()
    }
}
