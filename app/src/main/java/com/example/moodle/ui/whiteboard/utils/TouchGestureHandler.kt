package com.example.moodle.ui.whiteboard.utils
import com.example.moodle.ui.whiteboard.models.*
import com.example.moodle.ui.whiteboard.managers.*
import com.example.moodle.ui.whiteboard.controllers.*

import android.graphics.*
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Handles all touch events and gestures for the whiteboard
 * Manages drawing, selection, panning, and special tool interactions
 */
class TouchGestureHandler(
    private val stateManager: DrawingStateManager,
    private val zoomPanController: ZoomPanController,
    private val paintHelper: PaintHelper,
    private val sectionManager: SectionManager
) {
    // Multi-touch path tracking
    private val activePaths = mutableMapOf<Int, Path>()
    private val activePathsLastX = mutableMapOf<Int, Float>()
    private val activePathsLastY = mutableMapOf<Int, Float>()
    
    // Multi-section drawing support - track which section each pointer is drawing in
    private val pointerSectionMap = mutableMapOf<Int, SectionManager.Section>()
    
    // Shape drawing state
    private var isDrawingShape = false
    private var shapeStartX = 0f
    private var shapeStartY = 0f
    private var shapeEndX = 0f
    private var shapeEndY = 0f
    var previewPath: Path? = null
        private set
    
    // Selection/eraser tracking
    var selectionPath: Path? = null
        private set
    private var isDrawingSelection = false
    
    // Text input callback
    var onTextInputRequested: ((x: Float, y: Float) -> Unit)? = null
    
    // Invalidate callback
    var onInvalidate: (() -> Unit)? = null
    
    // Drawing change callback
    var onDrawingChanged: (() -> Unit)? = null
    
    // Active section tracking
    var currentActiveSection: SectionManager.Section? = null
    
    // View dimensions callback
    var getViewWidth: (() -> Int)? = null
    
    // Section control button click callback
    var onSectionControlButtonClick: ((section: SectionManager.Section, buttonIndex: Int) -> Unit)? = null
    var getViewHeight: (() -> Int)? = null
    
    // Bitmap bounds callback
    var getBitmapBounds: (() -> Pair<Float, Float>)? = null
    
    // Drawing canvas callback for normal mode
    var getDrawingCanvas: (() -> Canvas?)? = null
    
    // Background color callback
    var getBackgroundColor: (() -> BackgroundColor)? = null
    
    // Render manager callback for proper canvas redrawing
    var onRedrawCanvas: (() -> Unit)? = null
    
    fun handleDrawingTouch(event: MotionEvent, isDrawingMode: Boolean) {
        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                handleTouchDown(event, pointerIndex, pointerId)
            }
            
            MotionEvent.ACTION_MOVE -> {
                handleTouchMove(event)
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                handleTouchUp(event, pointerIndex, pointerId)
            }
            
            MotionEvent.ACTION_CANCEL -> {
                activePaths.clear()
                activePathsLastX.clear()
                activePathsLastY.clear()
                pointerSectionMap.clear()
            }
        }
        
        onInvalidate?.invoke()
    }
    
    private fun handleTouchDown(event: MotionEvent, pointerIndex: Int, pointerId: Int) {
        val screenX = event.getX(pointerIndex)
        val screenY = event.getY(pointerIndex)
        
        // In split mode, determine which section this pointer is drawing in
        if (sectionManager.isSplitModeEnabled()) {
            val touchedSection = sectionManager.getSectionForDrawing(screenX, screenY)
            
            if (touchedSection != null) {
                // Check if touch is in control area
                if (touchedSection.containsInControlArea(screenX, screenY)) {
                    // Handle control button clicks
                    handleSectionControlClick(touchedSection, screenX, screenY)
                    return
                }
                
                // Check if touch is in drawing area
                if (!touchedSection.containsInDrawingArea(screenX, screenY)) {
                    return
                }
                
                // Map this pointer to the touched section
                pointerSectionMap[pointerId] = touchedSection
                currentActiveSection = touchedSection
                sectionManager.setActiveSection(touchedSection)
            } else {
                return
            }
        }
        
        // Get coordinates for actual drawing
        val (x, y) = transformScreenToDrawing(screenX, screenY)
        
        // Get the tool to use
        val toolToUse = getToolForPointer(pointerId)
        
        when (toolToUse) {
            DrawingTool.TEXT -> {
                handleTextTouchDown(x, y)
            }
            DrawingTool.SELECTION -> {
                handleSelectionTouchDown(x, y)
            }
            DrawingTool.SELECTIVE_ERASER -> {
                handleSelectiveEraserTouchDown(x, y)
            }
            DrawingTool.LINE, DrawingTool.CIRCLE, DrawingTool.TRIANGLE, DrawingTool.RECTANGLE,
            DrawingTool.CUBE, DrawingTool.CYLINDER, DrawingTool.PYRAMID, DrawingTool.SPHERE, DrawingTool.CONE,
            DrawingTool.RIGHT_TRIANGLE, DrawingTool.ISOSCELES_TRIANGLE, DrawingTool.PARALLELOGRAM, DrawingTool.TRAPEZOID,
            DrawingTool.PENTAGON, DrawingTool.HEXAGON, DrawingTool.STAR, DrawingTool.DIAMOND, DrawingTool.ELLIPSE, DrawingTool.ARROW -> {
                handleShapeTouchDown(x, y, toolToUse, pointerId)
            }

            else -> {
                // Create new path for drawing
                val newPath = Path()
                newPath.moveTo(x, y)
                activePaths[pointerId] = newPath
                activePathsLastX[pointerId] = x
                activePathsLastY[pointerId] = y
            }
        }
    }
    
    private fun handleTouchMove(event: MotionEvent) {
        // Handle special tools for first pointer only
        if (event.pointerCount == 1) {
            val id = event.getPointerId(0)
            val toolToUse = getToolForPointer(id)
            
            when (toolToUse) {
                DrawingTool.TEXT -> {
                    val (x, y) = transformScreenToDrawing(event.x, event.y)
                    handleTextTouchMove(x, y)
                    return
                }
                DrawingTool.SELECTION -> {
                    val (x, y) = transformScreenToDrawing(event.x, event.y)
                    handleSelectionTouchMove(x, y)
                    return
                }
                DrawingTool.SELECTIVE_ERASER -> {
                    val (x, y) = transformScreenToDrawing(event.x, event.y)
                    handleSelectiveEraserTouchMove(x, y)
                    return
                }
                DrawingTool.LINE, DrawingTool.CIRCLE, DrawingTool.TRIANGLE, DrawingTool.RECTANGLE,
                DrawingTool.CUBE, DrawingTool.CYLINDER, DrawingTool.PYRAMID, DrawingTool.SPHERE, DrawingTool.CONE,
                DrawingTool.RIGHT_TRIANGLE, DrawingTool.ISOSCELES_TRIANGLE, DrawingTool.PARALLELOGRAM, DrawingTool.TRAPEZOID,
                DrawingTool.PENTAGON, DrawingTool.HEXAGON, DrawingTool.STAR, DrawingTool.DIAMOND, DrawingTool.ELLIPSE, DrawingTool.ARROW -> {
                    val (x, y) = transformScreenToDrawing(event.x, event.y)
                    handleShapeTouchMove(x, y, toolToUse, id)
                    return
                }
                else -> {}
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
                
                // In split mode, check if still within section's drawing area
                if (sectionManager.isSplitModeEnabled()) {
                    val pointerSection = pointerSectionMap[id]
                    if (pointerSection != null && !pointerSection.containsInDrawingArea(screenX, screenY)) {
                        continue
                    }
                }
                
                val (x, y) = transformScreenToDrawing(screenX, screenY)
                
                val dx = abs(x - lastX)
                val dy = abs(y - lastY)
                
                if (dx >= 4 || dy >= 4) {
                    path.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2)
                    activePathsLastX[id] = x
                    activePathsLastY[id] = y
                    onInvalidate?.invoke()
                }
            }
        }
    }
    
    private fun handleTouchUp(event: MotionEvent, pointerIndex: Int, pointerId: Int) {
        val screenX = event.getX(pointerIndex)
        val screenY = event.getY(pointerIndex)
        val (x, y) = transformScreenToDrawing(screenX, screenY)
        
        val toolToUse = getToolForPointer(pointerId)
        
        when (toolToUse) {
            DrawingTool.TEXT -> handleTextTouchUp(x, y)
            DrawingTool.SELECTION -> handleSelectionTouchUp(x, y)
            DrawingTool.SELECTIVE_ERASER -> handleSelectiveEraserTouchUp(x, y)
            DrawingTool.LINE, DrawingTool.CIRCLE, DrawingTool.TRIANGLE, DrawingTool.RECTANGLE,
            DrawingTool.CUBE, DrawingTool.CYLINDER, DrawingTool.PYRAMID, DrawingTool.SPHERE, DrawingTool.CONE,
            DrawingTool.RIGHT_TRIANGLE, DrawingTool.ISOSCELES_TRIANGLE, DrawingTool.PARALLELOGRAM, DrawingTool.TRAPEZOID,
            DrawingTool.PENTAGON, DrawingTool.HEXAGON, DrawingTool.STAR, DrawingTool.DIAMOND, DrawingTool.ELLIPSE, DrawingTool.ARROW -> {
                handleShapeTouchUp(x, y, toolToUse, pointerId)
            }
            else -> {
                finishDrawingPath(pointerId)
            }
        }
    }
    
    private fun finishDrawingPath(pointerId: Int) {
        val path = activePaths[pointerId]
        val lastX = activePathsLastX[pointerId]
        val lastY = activePathsLastY[pointerId]
        
        if (path != null && lastX != null && lastY != null) {
            path.lineTo(lastX, lastY)
            val pathCopy = Path(path)
            
            // In split mode, add to the section
            if (sectionManager.isSplitModeEnabled()) {
                val pointerSection = pointerSectionMap[pointerId]
                if (pointerSection != null) {
                    val sectionPaint = paintHelper.getPaintForSection(pointerSection.currentTool, pointerSection.currentColor, pointerSection.currentStrokeWidth)
                    val drawingPath = DrawingPath(pathCopy, sectionPaint, pointerSection.currentTool)
                    pointerSection.paths.add(drawingPath)
                }
                pointerSectionMap.remove(pointerId)
            } else {
                val paintCopy = paintHelper.getCurrentPaint(stateManager.currentTool, stateManager.currentColor, stateManager.currentStrokeWidth)
                val drawingPath = DrawingPath(pathCopy, paintCopy, stateManager.currentTool)
                stateManager.addPath(drawingPath)
                
                // Draw to bitmap
                getDrawingCanvas?.invoke()?.drawPath(pathCopy, paintCopy)
            }
            
            // Remove from active paths
            activePaths.remove(pointerId)
            activePathsLastX.remove(pointerId)
            activePathsLastY.remove(pointerId)
            
            // Clear undo stack and notify when all fingers are up
            if (activePaths.isEmpty()) {
                if (!sectionManager.isSplitModeEnabled()) {
                    stateManager.clearUndoStack()
                }
                onDrawingChanged?.invoke()
            }
        }
    }
    
    // Helper methods for coordinate transformation
    private fun transformScreenToDrawing(screenX: Float, screenY: Float): Pair<Float, Float> {
        return if (sectionManager.isSplitModeEnabled()) {
            Pair(screenX, screenY)
        } else {
            val x = (screenX + zoomPanController.focusX) / zoomPanController.scaleFactor
            val y = (screenY + zoomPanController.focusY) / zoomPanController.scaleFactor
            clampToBitmapBounds(x, y)
        }
    }
    
    private fun clampToBitmapBounds(x: Float, y: Float): Pair<Float, Float> {
        if (sectionManager.isSplitModeEnabled()) {
            return Pair(x, y)
        }
        
        val bounds = getBitmapBounds?.invoke() ?: return Pair(x, y)
        val clampedX = x.coerceIn(0f, bounds.first)
        val clampedY = y.coerceIn(0f, bounds.second)
        return Pair(clampedX, clampedY)
    }
    
    private fun getToolForPointer(pointerId: Int): DrawingTool {
        return if (sectionManager.isSplitModeEnabled() && pointerSectionMap[pointerId] != null) {
            pointerSectionMap[pointerId]!!.currentTool
        } else {
            stateManager.currentTool
        }
    }
    
    // Text tool handlers (delegated to state manager)
    private fun handleTextTouchDown(x: Float, y: Float) {
        stateManager.handleTextTouchDown(x, y) { tx, ty ->
            onTextInputRequested?.invoke(tx, ty)
        }
        onInvalidate?.invoke()
    }
    
    private fun handleTextTouchMove(x: Float, y: Float) {
        stateManager.handleTextTouchMove(x, y)
        onInvalidate?.invoke()
    }
    
    private fun handleTextTouchUp(x: Float, y: Float) {
        stateManager.handleTextTouchUp(x, y)
        onDrawingChanged?.invoke()
        onInvalidate?.invoke()
    }
    
    // Selection tool handlers
    private fun handleSelectionTouchDown(x: Float, y: Float) {
        stateManager.handleSelectionTouchDown(x, y)
        onInvalidate?.invoke()
    }
    
    private fun handleSelectionTouchMove(x: Float, y: Float) {
        stateManager.handleSelectionTouchMove(x, y)
        onInvalidate?.invoke()
    }
    
    private fun handleSelectionTouchUp(x: Float, y: Float) {
        stateManager.handleSelectionTouchUp(x, y)
        onDrawingChanged?.invoke()
        onInvalidate?.invoke()
    }
    
    // Selective eraser handlers
    private fun handleSelectiveEraserTouchDown(x: Float, y: Float) {
        selectionPath = Path().apply { moveTo(x, y) }
        isDrawingSelection = true
        onInvalidate?.invoke()
    }
    
    private fun handleSelectiveEraserTouchMove(x: Float, y: Float) {
        if (isDrawingSelection) {
            selectionPath?.lineTo(x, y)
            onInvalidate?.invoke()
        }
    }
    
    private fun handleSelectiveEraserTouchUp(x: Float, y: Float) {
        if (isDrawingSelection) {
            selectionPath?.close()
            stateManager.eraseElementsInSelection(selectionPath)
            selectionPath = null
            isDrawingSelection = false
            onDrawingChanged?.invoke()
            
            // Properly redraw canvas with background pattern and image support
            onRedrawCanvas?.invoke()
            
            onInvalidate?.invoke()
        }
    }
    
    // Shape tool handlers
    private fun handleShapeTouchDown(x: Float, y: Float, tool: DrawingTool, pointerId: Int) {
        // Check if touching existing shape object first
        if (!stateManager.handleShapeObjectInteraction(x, y, MotionEvent.ACTION_DOWN)) {
            // Start creating new shape
            isDrawingShape = true
            shapeStartX = x
            shapeStartY = y
            shapeEndX = x
            shapeEndY = y
            previewPath = createShapePath(tool, shapeStartX, shapeStartY, shapeEndX, shapeEndY)
        }
        onInvalidate?.invoke()
    }
    
    private fun handleShapeTouchMove(x: Float, y: Float, tool: DrawingTool, pointerId: Int) {
        if (!stateManager.handleShapeObjectInteraction(x, y, MotionEvent.ACTION_MOVE)) {
            if (isDrawingShape) {
                shapeEndX = x
                shapeEndY = y
                previewPath = createShapePath(tool, shapeStartX, shapeStartY, shapeEndX, shapeEndY)
            }
        }
        onInvalidate?.invoke()
    }
    
    private fun handleShapeTouchUp(x: Float, y: Float, tool: DrawingTool, pointerId: Int) {
        if (!stateManager.handleShapeObjectInteraction(x, y, MotionEvent.ACTION_UP)) {
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
                    color = stateManager.currentColor,
                    strokeWidth = stateManager.currentStrokeWidth
                )
                
                stateManager.addShapeObject(shapeObject)
                isDrawingShape = false
                previewPath = null
                onDrawingChanged?.invoke()
            }
        }
        onInvalidate?.invoke()
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
                val radius = kotlin.math.sqrt((endX - startX) * (endX - startX) + (endY - startY) * (endY - startY)) / 2
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
                
                // Top face
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
                
                // Bottom ellipse
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
                
                // Right face
                path.moveTo(topX, topY)
                path.lineTo(centerX + size / 2, bottomY)
                path.lineTo(centerX + size, bottomY - size / 3)
                path.close()
                
                // Base line
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
                
                // Horizontal ellipse
                val equatorRadiusX = radius * 0.8f
                val equatorRadiusY = radius * 0.25f
                path.addOval(
                    RectF(
                        centerX - equatorRadiusX, centerY - equatorRadiusY,
                        centerX + equatorRadiusX, centerY + equatorRadiusY
                    ),
                    Path.Direction.CW
                )
                
                // Vertical ellipse
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
                
                // Left side
                path.moveTo(topX, topY)
                path.lineTo(centerX - radiusX, bottomY)
                
                // Right side
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
                
                path.moveTo(centerX - topWidth / 2, centerY - height / 2)
                path.lineTo(centerX + topWidth / 2, centerY - height / 2)
                path.lineTo(centerX + width / 2, centerY + height / 2)
                path.lineTo(centerX - width / 2, centerY + height / 2)
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
                    val x = centerX + radius * kotlin.math.cos(angle).toFloat()
                    val y = centerY + radius * kotlin.math.sin(angle).toFloat()
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
                    val x = centerX + radius * kotlin.math.cos(angle).toFloat()
                    val y = centerY + radius * kotlin.math.sin(angle).toFloat()
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
                    val x = centerX + radius * kotlin.math.cos(angle).toFloat()
                    val y = centerY + radius * kotlin.math.sin(angle).toFloat()
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
                
                path.moveTo(left, centerY - shaftWidth / 2)
                path.lineTo(right - headLength, centerY - shaftWidth / 2)
                path.lineTo(right - headLength, centerY - headWidth / 2)
                path.lineTo(right, centerY)
                path.lineTo(right - headLength, centerY + headWidth / 2)
                path.lineTo(right - headLength, centerY + shaftWidth / 2)
                path.lineTo(left, centerY + shaftWidth / 2)
                path.close()
            }
            else -> {
                path.moveTo(startX, startY)
                path.lineTo(endX, endY)
            }
        }
        
        return path
    }
    
    fun handlePanningTouch(event: MotionEvent) {
        zoomPanController.handlePanningTouch(event) {
            onInvalidate?.invoke()
        }
    }
    
    // Handle clicks on section control buttons
    private fun handleSectionControlClick(section: SectionManager.Section, x: Float, y: Float) {
        val controlArea = section.controlBounds
        val sectionWidth = section.bounds.width()
        
        val buttonSize = 50f
        val buttonSpacing = 6f
        val buttonCount = 7
        
        val totalWidth = buttonCount * buttonSize + (buttonCount - 1) * buttonSpacing
        val startX = controlArea.left + (sectionWidth - totalWidth) / 2
        val centerY = controlArea.centerY()
        val buttonTop = centerY - buttonSize / 2
        
        for (i in 0 until buttonCount) {
            val buttonLeft = startX + i * (buttonSize + buttonSpacing)
            val buttonRight = buttonLeft + buttonSize
            val buttonBottom = buttonTop + buttonSize
            val rect = RectF(buttonLeft, buttonTop, buttonRight, buttonBottom)
            
            if (rect.contains(x, y)) {
                // Button index: 0=pen, 1=eraser, 2=clear, 3=black, 4=blue, 5=green, 6=red
                onSectionControlButtonClick?.invoke(section, i)
                onInvalidate?.invoke()
                return
            }
        }
    }
    
    fun getActivePaths(): Map<Int, Path> = activePaths
    
    fun getPointerSectionMap(): Map<Int, SectionManager.Section> = pointerSectionMap
}

