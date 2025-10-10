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
        PEN, ERASER, HIGHLIGHTER, TEXT, SELECTIVE_ERASER
    }

    // Drawing state
    private var currentTool = DrawingTool.PEN
    private var currentColor = Color.BLACK
    private var currentStrokeWidth = 8f
    private var isDrawingMode = true
    private var isPinchModeEnabled = false
    
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

    private val highlighterPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        alpha = 100 // Semi-transparent
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

    data class DrawingPath(
        val path: Path,
        val paint: Paint,
        val tool: DrawingTool,
        val text: String? = null,
        val textX: Float = 0f,
        val textY: Float = 0f
    )

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
            
            canvas.restore()
            
            // Draw rotation handle (green circle at top-center, outside rotation)
            val rotationHandleX = x + bounds.width() / 2f
            val rotationHandleY = top - 30f
            canvas.drawCircle(rotationHandleX, rotationHandleY, 10f, rotationHandlePaint)
        }
        
        fun contains(touchX: Float, touchY: Float): Boolean {
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            
            return touchX >= x - 10f && touchX <= x + bounds.width() + 10f &&
                   touchY >= y + bounds.top - 10f && touchY <= y + bounds.bottom + 10f
        }
        
        fun isResizeHandleTouched(touchX: Float, touchY: Float): Boolean {
            if (!isSelected) return false
            
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            
            val handleX = x + bounds.width() + 10f
            val handleY = y + bounds.bottom + 10f
            val handleSize = 20f
            
            return touchX >= handleX - handleSize/2 && touchX <= handleX + handleSize/2 &&
                   touchY >= handleY - handleSize/2 && touchY <= handleY + handleSize/2
        }
        
        fun isRotationHandleTouched(touchX: Float, touchY: Float): Boolean {
            if (!isSelected) return false
            
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            
            val handleX = x + bounds.width() / 2f
            val handleY = y + bounds.top - 40f // 30f offset + 10f radius
            val handleRadius = 15f // Slightly larger touch area
            
            val distance = kotlin.math.sqrt((touchX - handleX) * (touchX - handleX) + (touchY - handleY) * (touchY - handleY))
            return distance <= handleRadius
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
        } catch (e: OutOfMemoryError) {
            // Fallback to screen size if memory is insufficient
            drawingBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            drawingCanvas = Canvas(drawingBitmap!!)
            drawingCanvas?.drawColor(Color.WHITE)
            focusX = 0f
            focusY = 0f
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        
        canvas?.let { c ->
            c.save()
            
            // Apply transformations
            c.translate(-focusX, -focusY)
            c.scale(scaleFactor, scaleFactor)
            
            // Draw the infinite bitmap with all previous strokes
            drawingBitmap?.let { bitmap ->
                c.drawBitmap(bitmap, 0f, 0f, null)
            }
            
            // Draw current active paths being drawn
            val paint = getCurrentPaint()
            for (path in activePaths.values) {
                if (!path.isEmpty) {
                    c.drawPath(path, paint)
                }
            }
            
            // Draw text objects
            for (textObject in textObjects) {
                textObject.draw(c)
            }
            
            // Draw selection area for selective eraser
            selectionPath?.let { path ->
                c.drawPath(path, selectionPaint)
            }
            
            c.restore()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Handle scale gesture only if pinch mode is enabled
        if (isPinchModeEnabled) {
            scaleGestureDetector.onTouchEvent(event)
            
            // Don't process drawing if scaling
            if (isScaling) {
                return true
            }
        }
        
        // Handle gesture detection for double tap (zoom reset)
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
        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val x = (event.getX(pointerIndex) + focusX) / scaleFactor
                val y = (event.getY(pointerIndex) + focusY) / scaleFactor
                
                // Handle special tools differently
                when (currentTool) {
                    DrawingTool.TEXT -> {
                        handleTextTouch(x, y, action)
                    }
                    DrawingTool.SELECTIVE_ERASER -> {
                        handleSelectiveEraserTouch(x, y, action)
                    }
                    else -> {
                        // Create new path for this pointer
                        val newPath = Path()
                        newPath.moveTo(x, y)
                        activePaths[pointerId] = newPath
                        activePathsLastX[pointerId] = x
                        activePathsLastY[pointerId] = y
                    }
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                when (currentTool) {
                    DrawingTool.TEXT -> {
                        val x = (event.x + focusX) / scaleFactor
                        val y = (event.y + focusY) / scaleFactor
                        handleTextTouch(x, y, action)
                    }
                    DrawingTool.SELECTIVE_ERASER -> {
                        val x = (event.x + focusX) / scaleFactor
                        val y = (event.y + focusY) / scaleFactor
                        handleSelectiveEraserTouch(x, y, action)
                    }
                    else -> {
                        // Handle all active pointers
                        for (i in 0 until event.pointerCount) {
                            val id = event.getPointerId(i)
                            val path = activePaths[id]
                            val lastX = activePathsLastX[id]
                            val lastY = activePathsLastY[id]
                            
                            if (path != null && lastX != null && lastY != null) {
                                val x = (event.getX(i) + focusX) / scaleFactor
                                val y = (event.getY(i) + focusY) / scaleFactor
                                
                                val dx = abs(x - lastX)
                                val dy = abs(y - lastY)
                                
                                if (dx >= 4 || dy >= 4) {
                                    path.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2)
                                    activePathsLastX[id] = x
                                    activePathsLastY[id] = y
                                }
                            }
                        }
                    }
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                when (currentTool) {
                    DrawingTool.TEXT -> {
                        val x = (event.getX(pointerIndex) + focusX) / scaleFactor
                        val y = (event.getY(pointerIndex) + focusY) / scaleFactor
                        handleTextTouch(x, y, action)
                    }
                    DrawingTool.SELECTIVE_ERASER -> {
                        val x = (event.getX(pointerIndex) + focusX) / scaleFactor
                        val y = (event.getY(pointerIndex) + focusY) / scaleFactor
                        handleSelectiveEraserTouch(x, y, action)
                    }
                    else -> {
                        val path = activePaths[pointerId]
                        val lastX = activePathsLastX[pointerId]
                        val lastY = activePathsLastY[pointerId]
                        
                        if (path != null && lastX != null && lastY != null) {
                            // Finish the path
                            path.lineTo(lastX, lastY)
                            
                            // Save the completed path
                            val pathCopy = Path(path)
                            val paintCopy = Paint(getCurrentPaint())
                            paths.add(DrawingPath(pathCopy, paintCopy, currentTool))
                            
                            // Draw to bitmap
                            drawingCanvas?.drawPath(pathCopy, paintCopy)
                            
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
            }
        }
        
        invalidate()
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
            
            // Clear selected text if it was removed
            if (selectedTextObject in textObjectsToRemove) {
                selectedTextObject = null
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
            DrawingTool.TEXT -> textPaint.apply {
                color = currentColor
                textSize = currentStrokeWidth * 6 // Scale text size with stroke width
            }
            DrawingTool.SELECTIVE_ERASER -> selectionPaint // Use selection paint for drawing selection area
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
            
            // Add to text objects list
            textObjects.add(textObject)
            
            // Select the new text object
            selectedTextObject?.isSelected = false
            selectedTextObject = textObject
            textObject.isSelected = true
            
            // Notify listener and redraw
            drawingChangeListener?.onDrawingChanged()
            invalidate()
        }
    }

    fun setPinchModeEnabled(enabled: Boolean) {
        isPinchModeEnabled = enabled
    }

    fun isPinchModeEnabled(): Boolean {
        return isPinchModeEnabled
    }

    fun undo() {
        if (canUndo()) {
            val lastPath = paths.removeAt(paths.size - 1)
            undoStack.add(lastPath)
            redrawCanvas()
            drawingChangeListener?.onDrawingChanged()
        }
    }

    fun redo() {
        if (canRedo()) {
            val pathToRedo = undoStack.removeAt(undoStack.size - 1)
            paths.add(pathToRedo)
            redrawCanvas()
            drawingChangeListener?.onDrawingChanged()
        }
    }

    fun clear() {
        paths.clear()
        undoStack.clear()
        activePaths.clear()
        activePathsLastX.clear()
        activePathsLastY.clear()
        textObjects.clear()
        selectedTextObject = null
        selectionPath = null
        isDrawingSelection = false
        redrawCanvas()
        drawingChangeListener?.onDrawingChanged()
    }

    private fun redrawCanvas() {
        // Clear the entire canvas
        drawingCanvas?.drawColor(Color.WHITE, PorterDuff.Mode.SRC)
        
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

    // Cleanup method
    fun cleanup() {
        drawingBitmap?.recycle()
        drawingBitmap = null
        drawingCanvas = null
    }

    // Getters for current state
    fun getCurrentTool(): DrawingTool = currentTool
    fun getCurrentColor(): Int = currentColor
    fun getCurrentStrokeWidth(): Float = currentStrokeWidth
    fun canUndo(): Boolean = paths.isNotEmpty()
    fun canRedo(): Boolean = undoStack.isNotEmpty()
}
