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
import androidx.core.graphics.toColorInt

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
    
    // Current page data (contains undo/redo manager)
    private var currentPageData: PageData? = null
    
    // Default undo/redo manager (used when no page is loaded)
    private val defaultUndoRedoManager = UndoRedoManager()
    
    // Current page's undo/redo manager (from page data, or default)
    private val currentPageUndoRedoManager: UndoRedoManager
        get() = currentPageData?.undoRedoManager ?: defaultUndoRedoManager
    
    // View state
    private var isDrawingMode = true
    private var isPinchModeEnabled = false

    // Gesture detection
    private val gestureDetector = GestureDetector(context, GestureListener())
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    
    // Debouncing for state saves
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var pendingStateSave: Runnable? = null
    private var stateSaveDebounceMs = 300L // Wait 300ms after last change before saving
    
    // Undo/redo callback (stored at view level and applied to each page's manager)
    private var undoRedoStackChangedCallback: ((Boolean, Boolean) -> Unit)? = null

    init {
        // Setup callbacks for managers
        setupManagerCallbacks()
    }
    
    private fun setupManagerCallbacks() {
        // Touch handler callbacks
        touchHandler.onInvalidate = { invalidate() }
        touchHandler.onDrawingChanged = { 
            saveStateToUndoStackDebounced()
            stateManager.drawingChangeListener?.onDrawingChanged()
        }
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
        touchHandler.onRedrawCanvas = { renderManager.redrawCanvas() }
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
    
    /**
     * Save current state to undo stack (immediate)
     */
    private fun saveStateToUndoStack() {
        currentPageUndoRedoManager.saveState(
            bitmap = bitmapManager.drawingBitmap,
            paths = stateManager.paths,
            textObjects = stateManager.textObjects,
            shapeObjects = stateManager.shapeObjects,
            imageObjects = stateManager.imageObjects,
        )
    }
    
    /**
     * Save current state to undo stack (debounced to avoid excessive saves)
     */
    private fun saveStateToUndoStackDebounced() {
        // Cancel any pending save
        pendingStateSave?.let { handler.removeCallbacks(it) }
        
        // Schedule new save
        pendingStateSave = Runnable {
            saveStateToUndoStack()
            pendingStateSave = null
        }
        
        handler.postDelayed(pendingStateSave!!, stateSaveDebounceMs)
    }
    
    /**
     * Restore state from snapshot
     */
    private fun restoreState(snapshot: UndoRedoManager.DrawingSnapshot?) {
        if (snapshot == null) return
        
        // Clear current state
        stateManager.clear()
        
        // Restore bitmap from diff
        if (snapshot.bitmapDiff != null) {
            val restoredBitmap = currentPageUndoRedoManager.reconstructBitmap(snapshot)
            if (restoredBitmap != null) {
                bitmapManager.drawingBitmap?.let { currentBitmap ->
                    if (!currentBitmap.isRecycled) {
                        val canvas = Canvas(currentBitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        canvas.drawBitmap(restoredBitmap, 0f, 0f, null)
                    }
                }
                
                // Recycle the temporary restored bitmap
                if (restoredBitmap != bitmapManager.drawingBitmap) {
                    restoredBitmap.recycle()
                }
            }
        } else {
            // No bitmap diff, just clear the canvas
            bitmapManager.drawingBitmap?.let { currentBitmap ->
                if (!currentBitmap.isRecycled) {
                    val canvas = Canvas(currentBitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                }
            }
        }
        
        // Restore state objects
        stateManager.paths.addAll(snapshot.paths)
        stateManager.textObjects.addAll(snapshot.textObjects)
        stateManager.shapeObjects.addAll(snapshot.shapeObjects)
        stateManager.imageObjects.addAll(snapshot.imageObjects)

        // Redraw
        renderManager.redrawCanvas()
        invalidate()
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
            4 -> {
                section.currentColor = "#2196F3".toColorInt()
                section.currentTool = DrawingTool.PEN
            }  // Blue
            5 -> {
                section.currentColor = "#4CAF50".toColorInt()
                section.currentTool = DrawingTool.PEN
            }  // Green
            6 -> {
                section.currentColor = "#F44336".toColorInt()
                section.currentTool = DrawingTool.PEN
            }  // Red
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bitmapManager.onSizeChanged(w, h, oldw, oldh)
        sectionManager.setBoardDimensions(w.toFloat(), h.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
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
            if (!zoomPanController.isScaling) touchHandler.handlePanningTouch(event)

            return true
        }
        
        // Normal mode: Handle gesture detection for double tap (zoom reset)
        gestureDetector.onTouchEvent(event)
        
        // Handle drawing or panning based on mode
        if (isDrawingMode) touchHandler.handleDrawingTouch(event, isDrawingMode)
        else touchHandler.handlePanningTouch(event)

        
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
                textObject.isSelected = true
            }
        } else stateManager.addTextObject(textObject)

        saveStateToUndoStackDebounced()
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
        saveStateToUndoStackDebounced()
        stateManager.drawingChangeListener?.onDrawingChanged()
        invalidate()
    }

    // Pinch mode controls
    fun setPinchModeEnabled(enabled: Boolean) {
        isPinchModeEnabled = enabled
    }

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
    
    // Background image
    fun setBackgroundImage(bitmap: Bitmap?) {
        renderManager.setBackgroundImage(bitmap)
        renderManager.redrawCanvas()
        invalidate()
    }
    
    // Coordinate conversion
    fun getVisibleCanvasCenterX(): Float {
        return (width / 2f + zoomPanController.focusX) / zoomPanController.scaleFactor
    }
    
    fun getVisibleCanvasCenterY(): Float {
        return (height / 2f + zoomPanController.focusY) / zoomPanController.scaleFactor
    }

    // Clear operations
    fun clear() {
        if (sectionManager.isSplitModeEnabled() && touchHandler.currentActiveSection != null) {
            touchHandler.currentActiveSection!!.clear()
        } else {
            stateManager.clear()
            renderManager.redrawCanvas()
        }
        saveStateToUndoStackDebounced()
        stateManager.drawingChangeListener?.onDrawingChanged()
        invalidate()
    }
    
    // Undo/Redo operations
    
    /**
     * Undo the last drawing action
     * @return true if undo was successful, false if there's nothing to undo
     */
    fun undo(): Boolean {
        val manager = currentPageUndoRedoManager ?: return false
        val snapshot = manager.undo()
        if (snapshot != null) {
            restoreState(snapshot)
            return true
        }
        return false
    }
    
    /**
     * Redo the last undone action
     * @return true if redo was successful, false if there's nothing to redo
     */
    fun redo(): Boolean {
        val manager = currentPageUndoRedoManager ?: return false
        val snapshot = manager.redo()
        if (snapshot != null) {
            restoreState(snapshot)
            return true
        }
        return false
    }
    
    /**
     * Check if undo is available
     * @return true if there are actions to undo
     */
    fun canUndo(): Boolean = currentPageUndoRedoManager?.canUndo() ?: false
    
    /**
     * Check if redo is available
     * @return true if there are actions to redo
     */
    fun canRedo(): Boolean = currentPageUndoRedoManager?.canRedo() ?: false
    
    /**
     * Clear all undo/redo history
     */
    fun clearUndoRedoHistory() {
        currentPageUndoRedoManager.clear()
    }
    
    /**
     * Set undo/redo stack size limit
     * @param size Maximum number of states to keep (10-100)
     */
    fun setUndoRedoStackSize(size: Int) {
        currentPageUndoRedoManager.setMaxStackSize(size)
    }
    
    /**
     * Enable or disable undo/redo functionality
     * @param enabled true to enable, false to disable
     */
    fun setUndoRedoEnabled(enabled: Boolean) {
        currentPageUndoRedoManager.setEnabled(enabled)
    }
    
    /**
     * Set a callback to be notified when undo/redo availability changes
     * @param callback Lambda that receives (canUndo, canRedo) booleans
     */
    fun setOnUndoRedoStackChangedListener(callback: ((Boolean, Boolean) -> Unit)?) {
        // Store callback at view level
        undoRedoStackChangedCallback = callback
        
        // Set callback on current page's manager
        currentPageUndoRedoManager.onStackChanged = callback
        
        // Immediately invoke callback with current state to initialize button states
        callback?.invoke(canUndo(), canRedo())
    }
    
    /**
     * Enable or disable incremental diffs for undo/redo
     * When enabled, only changed bitmap regions are stored (more memory efficient)
     * When disabled, full bitmaps are stored (simpler but uses more memory)
     * @param enabled true to use incremental diffs, false for full bitmaps
     */
    fun setIncrementalDiffsEnabled(enabled: Boolean) {
        currentPageUndoRedoManager.setIncrementalDiffsEnabled(enabled)
    }
    
    /**
     * Check if incremental diffs are enabled
     * @return true if using incremental diffs
     */
    fun isIncrementalDiffsEnabled(): Boolean = currentPageUndoRedoManager.isIncrementalDiffsEnabled()
    
    /**
     * Set debounce delay for state saves (in milliseconds)
     * Higher values = less frequent saves = better performance but less granular undo
     * Lower values = more frequent saves = worse performance but more granular undo
     * @param delayMs Delay in milliseconds (default: 300ms, recommended: 100-500ms)
     */
    fun setStateSaveDebounce(delayMs: Long) {
        stateSaveDebounceMs = delayMs.coerceIn(0L, 2000L)
    }
    
    /**
     * Set performance mode for diff calculation
     * FAST: Larger tiles, more sampling - fastest but less accurate
     * BALANCED: Default settings - good balance
     * QUALITY: Smaller tiles, less sampling - slower but more accurate
     */
    fun setDiffPerformanceMode(mode: BitmapDiffManager.PerformanceMode) {
        currentPageUndoRedoManager.setDiffPerformanceMode(mode)
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
    
    // Page management
    fun getAllPaths(): List<DrawingPath> = stateManager.paths.toList()
    fun getAllTextObjects(): List<TextObject> = stateManager.textObjects.toList()
    fun getAllShapeObjects(): List<ShapeObject> = stateManager.shapeObjects.toList()
    fun getAllImageObjects(): List<ImageObject> = stateManager.imageObjects.toList()
    
    /**
     * Load page data and set up undo/redo for this page
     * @param pageData Complete page data including undo/redo manager
     */
    fun loadPageData(pageData: PageData) {
        // Store reference to current page data
        currentPageData = pageData
        
        // Set callback on the new page's undo/redo manager
        currentPageUndoRedoManager.onStackChanged = undoRedoStackChangedCallback
        
        // Load page content
        stateManager.clear()
        stateManager.paths.addAll(pageData.paths)
        stateManager.textObjects.addAll(pageData.textObjects)
        stateManager.shapeObjects.addAll(pageData.shapeObjects)
        stateManager.imageObjects.addAll(pageData.imageObjects)
        
        // Load background
        renderManager.setBackgroundPattern(pageData.backgroundPattern)
        renderManager.setBackgroundColor(pageData.backgroundColor)
        
        renderManager.redrawCanvas()
        invalidate()
        
        // Update button states for the new page's undo/redo manager
        currentPageUndoRedoManager.updateButtonStates()
        
        // Save initial state if this is a new page with no history
        if (!canUndo() && !canRedo()) {
            post {
                saveStateToUndoStack()
            }
        }
    }
    
    /**
     * Legacy method for backward compatibility
     * @deprecated Use loadPageData(PageData) instead
     * Note: This method does NOT save initial state - used for temporary rendering only
     */
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
        
        // Note: No initial state save here - this is for temporary rendering
        // The main loadPageData(PageData) handles initial state saves
    }

    // Cleanup
    fun cleanup() {
        // Cancel any pending state saves
        pendingStateSave?.let { handler.removeCallbacks(it) }
        pendingStateSave = null
        
        bitmapManager.cleanup()
        // Don't cleanup undo/redo managers - they're owned by PageData
    }
    
    // Gesture listeners
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            return false
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
