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

    // Drawing tools
    enum class DrawingTool {
        PEN, ERASER, HIGHLIGHTER
    }

    // Drawing state
    private var currentTool = DrawingTool.PEN
    private var currentColor = Color.BLACK
    private var currentStrokeWidth = 8f
    private var isDrawingMode = true

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

    private val highlighterPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        alpha = 100 // Semi-transparent
    }

    // Canvas and bitmap for drawing
    private var drawingBitmap: Bitmap? = null
    private var drawingCanvas: Canvas? = null
    private val backgroundPaint = Paint().apply {
        color = Color.WHITE
    }

    // Path tracking
    private val currentPath = Path()
    private val paths = mutableListOf<DrawingPath>()
    private val undoStack = mutableListOf<DrawingPath>()

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

    data class DrawingPath(
        val path: Path,
        val paint: Paint,
        val tool: DrawingTool
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        drawingBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawingCanvas = Canvas(drawingBitmap!!)
        drawingCanvas?.drawColor(Color.WHITE)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        
        canvas?.let { c ->
            c.save()
            
            // Apply transformations
            c.translate(focusX, focusY)
            c.scale(scaleFactor, scaleFactor)
            
            // Draw background
            c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
            
            // Draw the bitmap with all previous strokes
            drawingBitmap?.let { bitmap ->
                c.drawBitmap(bitmap, 0f, 0f, null)
            }
            
            // Draw current path being drawn
            if (!currentPath.isEmpty) {
                val paint = getCurrentPaint()
                c.drawPath(currentPath, paint)
            }
            
            c.restore()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Handle scale gesture first
        scaleGestureDetector.onTouchEvent(event)
        
        // Don't process drawing if scaling
        if (isScaling) {
            return true
        }
        
        // Handle gesture detection
        gestureDetector.onTouchEvent(event)
        
        // Handle drawing only if in drawing mode
        if (isDrawingMode) {
            handleDrawingTouch(event)
        } else {
            handlePanningTouch(event)
        }
        
        return true
    }

    private fun handleDrawingTouch(event: MotionEvent) {
        val x = (event.x - focusX) / scaleFactor
        val y = (event.y - focusY) / scaleFactor
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath.reset()
                currentPath.moveTo(x, y)
                lastTouchX = x
                lastTouchY = y
            }
            
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(x - lastTouchX)
                val dy = abs(y - lastTouchY)
                
                if (dx >= 4 || dy >= 4) {
                    currentPath.quadTo(lastTouchX, lastTouchY, (x + lastTouchX) / 2, (y + lastTouchY) / 2)
                    lastTouchX = x
                    lastTouchY = y
                }
            }
            
            MotionEvent.ACTION_UP -> {
                currentPath.lineTo(lastTouchX, lastTouchY)
                
                // Save the path
                val pathCopy = Path(currentPath)
                val paintCopy = Paint(getCurrentPaint())
                paths.add(DrawingPath(pathCopy, paintCopy, currentTool))
                
                // Draw to bitmap
                drawingCanvas?.drawPath(pathCopy, paintCopy)
                
                // Clear current path
                currentPath.reset()
                
                // Clear undo stack when new stroke is made
                undoStack.clear()
            }
        }
        
        invalidate()
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
                        
                        focusX += dx
                        focusY += dy
                        
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
            DrawingTool.HIGHLIGHTER -> highlighterPaint.apply {
                color = currentColor
                strokeWidth = currentStrokeWidth * 3
                xfermode = null
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

    fun undo() {
        if (paths.isNotEmpty()) {
            val lastPath = paths.removeAt(paths.size - 1)
            undoStack.add(lastPath)
            redrawCanvas()
        }
    }

    fun redo() {
        if (undoStack.isNotEmpty()) {
            val pathToRedo = undoStack.removeAt(undoStack.size - 1)
            paths.add(pathToRedo)
            redrawCanvas()
        }
    }

    fun clear() {
        paths.clear()
        undoStack.clear()
        currentPath.reset()
        redrawCanvas()
    }

    private fun redrawCanvas() {
        drawingCanvas?.drawColor(Color.WHITE)
        
        for (drawingPath in paths) {
            drawingCanvas?.drawPath(drawingPath.path, drawingPath.paint)
        }
        
        invalidate()
    }

    // Gesture listeners
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Reset zoom and pan
            scaleFactor = 1.0f
            focusX = 0f
            focusY = 0f
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
            scaleFactor = max(0.1f, min(scaleFactor, 5.0f))
            
            // Adjust focus point
            focusX = detector.focusX - (detector.focusX - focusX) * detector.scaleFactor
            focusY = detector.focusY - (detector.focusY - focusY) * detector.scaleFactor
            
            invalidate()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isScaling = false
        }
    }

    // Getters for current state
    fun getCurrentTool(): DrawingTool = currentTool
    fun getCurrentColor(): Int = currentColor
    fun getCurrentStrokeWidth(): Float = currentStrokeWidth
    fun canUndo(): Boolean = paths.isNotEmpty()
    fun canRedo(): Boolean = undoStack.isNotEmpty()
}
