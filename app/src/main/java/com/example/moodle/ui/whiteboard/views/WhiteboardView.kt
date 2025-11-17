package com.example.moodle.ui.whiteboard.views
import com.example.moodle.ui.whiteboard.models.*
import com.example.moodle.ui.whiteboard.managers.*
import com.example.moodle.ui.whiteboard.controllers.*
import com.example.moodle.ui.whiteboard.listeners.*
import com.example.moodle.ui.whiteboard.utils.*

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

/**
 * Custom view for whiteboard drawing functionality
 * Delegates to specialized managers for state, rendering, touch handling, zoom/pan, etc.
 */
class WhiteboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Core managers
    private val sectionManager = SectionManager()
    private val stateManager = DrawingStateManager()
    private val bitmapManager = BitmapManager()
    private val paintHelper = PaintHelper()
    private val zoomPanController = ZoomPanController(context)
    private val renderManager = CanvasRenderManager(stateManager, zoomPanController, paintHelper, bitmapManager, sectionManager)
    private val touchHandler = TouchGestureHandler(stateManager, zoomPanController, paintHelper, sectionManager)
    
    // View state
    private var isDrawingMode = true
    private var isPinchModeEnabled = false

    // Gesture detection
    private val gestureDetector = GestureDetector(context, GestureListener())
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())

    init {
        // Setup callbacks for managers
        setupManagerCallbacks()
    }
    
    private fun setupManagerCallbacks() {
        // Touch handler callbacks
        touchHandler.onInvalidate = { invalidate() }
        touchHandler.onDrawingChanged = { stateManager.drawingChangeListener?.onDrawingChanged() }
        touchHandler.onTextInputRequested = { x, y -> stateManager.textInputListener?.onTextInputRequested(x, y) }
        touchHandler.getViewWidth = { width }
        touchHandler.getViewHeight = { height }
        touchHandler.getBitmapBounds = { 
            val w = bitmapManager.drawingBitmap?.width?.toFloat() ?: width.toFloat()
            val h = bitmapManager.drawingBitmap?.height?.toFloat() ?: height.toFloat()
            Pair(w, h)
        }
        touchHandler.getDrawingCanvas = { bitmapManager.drawingCanvas }
        touchHandler.getBackgroundColor = { renderManager.getBackgroundColor() }
        touchHandler.onSectionControlButtonClick = { section, buttonIndex ->
            handleSectionControlButtonClick(section, buttonIndex)
        }
        
        // Zoom pan controller callbacks
        zoomPanController.onInvalidate = { invalidate() }
        zoomPanController.getViewDimensions = { Pair(width, height) }
        zoomPanController.getBitmapDimensions = {
            val w = bitmapManager.drawingBitmap?.width?.toFloat() ?: width.toFloat()
            val h = bitmapManager.drawingBitmap?.height?.toFloat() ?: height.toFloat()
            Pair(w, h)
        }
        
        // Bitmap manager callback
        bitmapManager.onBitmapInitialized = { canvasWidth, canvasHeight ->
            zoomPanController.initializeFocus(canvasWidth, canvasHeight)
        }
    }
    
    private fun handleSectionControlButtonClick(section: SectionManager.Section, buttonIndex: Int) {
        // Button index: 0=pen, 1=eraser, 2=clear, 3=black, 4=blue, 5=green, 6=red
        when (buttonIndex) {
            0 -> section.currentTool = DrawingTool.PEN
            1 -> section.currentTool = DrawingTool.ERASER
            2 -> {
                section.clear()
                stateManager.drawingChangeListener?.onDrawingChanged()
            }
            3 -> section.currentColor = Color.BLACK
            4 -> section.currentColor = Color.parseColor("#2196F3")  // Blue
            5 -> section.currentColor = Color.parseColor("#4CAF50")  // Green
            6 -> section.currentColor = Color.parseColor("#F44336")  // Red
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bitmapManager.onSizeChanged(w, h, oldw, oldh)
        sectionManager.setBoardDimensions(w.toFloat(), h.toFloat())
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let { c ->
            renderManager.drawCanvas(
                c,
                touchHandler.getActivePaths(),
                touchHandler.selectionPath,
                touchHandler.previewPath,
                touchHandler.getPointerSectionMap(),
                touchHandler.currentActiveSection
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // In split mode, disable pinch/pan features
        if (sectionManager.isSplitModeEnabled()) {
            touchHandler.handleDrawingTouch(event, isDrawingMode)
            return true
        }
        
        // Pinch mode: Only allow zoom/pan gestures, no drawing
        if (isPinchModeEnabled) {
            scaleGestureDetector.onTouchEvent(event)
            
            // Handle gesture detection for double tap (zoom reset)
            gestureDetector.onTouchEvent(event)
            
            // Allow panning when not scaling
            if (!zoomPanController.isScaling) {
                touchHandler.handlePanningTouch(event)
            }
            
            return true
        }
        
        // Normal mode: Handle gesture detection for double tap (zoom reset)
        gestureDetector.onTouchEvent(event)
        
        // Handle drawing or panning based on mode
        if (isDrawingMode) {
            touchHandler.handleDrawingTouch(event, isDrawingMode)
        } else {
            touchHandler.handlePanningTouch(event)
        }
        
        return true
    }

    // Public API for drawing tools
    fun setDrawingTool(tool: DrawingTool) {
        stateManager.currentTool = tool
    }

    fun setColor(color: Int) {
        stateManager.currentColor = color
    }

    fun setStrokeWidth(width: Float) {
        stateManager.currentStrokeWidth = width
    }

    fun setDrawingMode(enabled: Boolean) {
        isDrawingMode = enabled
    }

    fun setOnDrawingChangeListener(listener: OnDrawingChangeListener?) {
        stateManager.drawingChangeListener = listener
    }

    fun setOnTextInputListener(listener: OnTextInputListener?) {
        stateManager.textInputListener = listener
    }

    fun addText(text: String, x: Float, y: Float) {
        if (text.isEmpty()) return
        
        val textObject = TextObject(
            text = text,
            x = x,
            y = y,
            textSize = stateManager.currentStrokeWidth * 6,
            color = stateManager.currentColor
        )
        
        if (sectionManager.isSplitModeEnabled()) {
            val targetSection = sectionManager.getSectionAt(x, y)
            targetSection?.let {
                it.textObjects.add(textObject)
                it.selectedTextObject?.isSelected = false
                it.selectedTextObject = textObject
                textObject.isSelected = true
            }
        } else {
            stateManager.addTextObject(textObject)
        }
        
        stateManager.drawingChangeListener?.onDrawingChanged()
        invalidate()
    }

    fun addImage(bitmap: Bitmap, x: Float, y: Float) {
        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()
        val imageX = x - imageWidth / 2f
        val imageY = y - imageHeight / 2f
        
        val imageObject = ImageObject(
            bitmap = bitmap,
            x = imageX,
            y = imageY,
            width = imageWidth,
            height = imageHeight
        )
        
        stateManager.addImageObject(imageObject)
        stateManager.drawingChangeListener?.onDrawingChanged()
        invalidate()
    }

    // Pinch mode controls
    fun setPinchModeEnabled(enabled: Boolean) {
        isPinchModeEnabled = enabled
    }

    fun isPinchModeEnabled(): Boolean = isPinchModeEnabled
    
    // Background pattern
    fun setBackgroundPattern(pattern: BackgroundPattern) {
        renderManager.setBackgroundPattern(pattern)
        renderManager.redrawCanvas()
        invalidate()
    }
    
    fun getBackgroundPattern(): BackgroundPattern = renderManager.getBackgroundPattern()
    
    // Background color
    fun setBackgroundColor(color: BackgroundColor) {
        renderManager.setBackgroundColor(color)
        renderManager.redrawCanvas()
        invalidate()
    }
    
    fun getBackgroundColor(): BackgroundColor = renderManager.getBackgroundColor()
    
    // Background image
    fun setBackgroundImage(bitmap: Bitmap?) {
        renderManager.setBackgroundImage(bitmap)
        renderManager.redrawCanvas()
        invalidate()
    }
    
    fun getBackgroundImage(): Bitmap? = renderManager.getBackgroundImage()
    
    // Zoom controls
    fun setZoomLevel(zoomLevel: Float) {
        zoomPanController.setZoomLevel(zoomLevel)
        invalidate()
    }
    
    fun getZoomLevel(): Float = zoomPanController.scaleFactor
    fun getMinZoomLevel(): Float = zoomPanController.minScaleFactor
    fun getMaxZoomLevel(): Float = 5.0f
    
    // Coordinate conversion
    fun getVisibleCanvasCenterX(): Float {
        return (width / 2f + zoomPanController.focusX) / zoomPanController.scaleFactor
    }
    
    fun getVisibleCanvasCenterY(): Float {
        return (height / 2f + zoomPanController.focusY) / zoomPanController.scaleFactor
    }

    // Undo/Redo/Clear operations
    fun undo() {
        if (sectionManager.isSplitModeEnabled() && touchHandler.currentActiveSection != null) {
            touchHandler.currentActiveSection!!.undo()
        } else {
            stateManager.undo()
            renderManager.redrawCanvas()
        }
        stateManager.drawingChangeListener?.onDrawingChanged()
            invalidate()
    }

    fun redo() {
        if (sectionManager.isSplitModeEnabled() && touchHandler.currentActiveSection != null) {
            touchHandler.currentActiveSection!!.redo()
        } else {
            stateManager.redo()
            renderManager.redrawCanvas()
        }
        stateManager.drawingChangeListener?.onDrawingChanged()
            invalidate()
    }

    fun clear() {
        if (sectionManager.isSplitModeEnabled() && touchHandler.currentActiveSection != null) {
            touchHandler.currentActiveSection!!.clear()
        } else {
            stateManager.clear()
            renderManager.redrawCanvas()
        }
        stateManager.drawingChangeListener?.onDrawingChanged()
        invalidate()
    }

    fun canUndo(): Boolean {
        return if (sectionManager.isSplitModeEnabled() && touchHandler.currentActiveSection != null) {
            touchHandler.currentActiveSection!!.canUndo()
            } else {
            stateManager.paths.isNotEmpty()
        }
    }

    fun canRedo(): Boolean {
        return if (sectionManager.isSplitModeEnabled() && touchHandler.currentActiveSection != null) {
            touchHandler.currentActiveSection!!.canRedo()
                } else {
            stateManager.canRedo()
        }
    }

    // Split mode management
    fun enableSplitMode(numberOfSections: Int) {
        sectionManager.enableSplitMode(numberOfSections)
        invalidate()
    }
    
    fun disableSplitMode() {
        sectionManager.disableSplitMode()
        touchHandler.currentActiveSection = null
        invalidate()
    }
    
    fun isSplitModeEnabled(): Boolean = sectionManager.isSplitModeEnabled()
    
    fun getAllSections(): List<SectionManager.Section> = sectionManager.getAllSections()
    
    fun setActiveSection(sectionId: Int) {
        val section = sectionManager.getAllSections().find { it.id == sectionId }
        touchHandler.currentActiveSection = section
        sectionManager.setActiveSection(section)
        
        section?.let {
            stateManager.currentTool = it.currentTool
            stateManager.currentColor = it.currentColor
            stateManager.currentStrokeWidth = it.currentStrokeWidth
        }
        invalidate()
    }
    
    fun getActiveSection(): SectionManager.Section? = touchHandler.currentActiveSection
    
    fun updateSectionTool(sectionId: Int, tool: DrawingTool) {
        val section = sectionManager.getAllSections().find { it.id == sectionId }
        section?.currentTool = tool
        if (section == touchHandler.currentActiveSection) {
            stateManager.currentTool = tool
        }
    }
    
    fun updateSectionColor(sectionId: Int, color: Int) {
        val section = sectionManager.getAllSections().find { it.id == sectionId }
        section?.currentColor = color
        if (section == touchHandler.currentActiveSection) {
            stateManager.currentColor = color
        }
    }
    
    fun updateSectionStrokeWidth(sectionId: Int, width: Float) {
        val section = sectionManager.getAllSections().find { it.id == sectionId }
        section?.currentStrokeWidth = width
        if (section == touchHandler.currentActiveSection) {
            stateManager.currentStrokeWidth = width
        }
    }
    
    fun undoSection(sectionId: Int) {
        val section = sectionManager.getAllSections().find { it.id == sectionId }
        section?.undo()
        stateManager.drawingChangeListener?.onDrawingChanged()
        invalidate()
    }
    
    fun redoSection(sectionId: Int) {
        val section = sectionManager.getAllSections().find { it.id == sectionId }
        section?.redo()
        stateManager.drawingChangeListener?.onDrawingChanged()
        invalidate()
    }
    
    fun clearSection(sectionId: Int) {
        val section = sectionManager.getAllSections().find { it.id == sectionId }
        section?.clear()
        stateManager.drawingChangeListener?.onDrawingChanged()
        invalidate()
    }
    
    fun clearAllSections() {
        sectionManager.clearAllSections()
        stateManager.drawingChangeListener?.onDrawingChanged()
        invalidate()
    }

    // Getters for current state
    fun getCurrentTool(): DrawingTool = stateManager.currentTool
    fun getCurrentColor(): Int = stateManager.currentColor
    fun getCurrentStrokeWidth(): Float = stateManager.currentStrokeWidth
    
    // Page management
    fun getAllPaths(): List<DrawingPath> = stateManager.paths.toList()
    fun getAllTextObjects(): List<TextObject> = stateManager.textObjects.toList()
    fun getAllShapeObjects(): List<ShapeObject> = stateManager.shapeObjects.toList()
    fun getAllImageObjects(): List<ImageObject> = stateManager.imageObjects.toList()
    
    fun loadPageData(
        pagePaths: List<DrawingPath>,
        pageTextObjects: List<TextObject>,
        pageShapeObjects: List<ShapeObject>,
        pageImageObjects: List<ImageObject> = emptyList()
    ) {
        stateManager.clear()
        stateManager.paths.addAll(pagePaths)
        stateManager.textObjects.addAll(pageTextObjects)
        stateManager.shapeObjects.addAll(pageShapeObjects)
        stateManager.imageObjects.addAll(pageImageObjects)
        renderManager.redrawCanvas()
        invalidate()
    }

    // Cleanup
    fun cleanup() {
        bitmapManager.cleanup()
    }
    
    // Gesture listeners
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            zoomPanController.resetZoomAndPan(width, height)
        invalidate()
            return true
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            zoomPanController.onScaleBegin(detector)
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            zoomPanController.onScale(detector)
            invalidate()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            zoomPanController.onScaleEnd(detector)
        }
    }
}
