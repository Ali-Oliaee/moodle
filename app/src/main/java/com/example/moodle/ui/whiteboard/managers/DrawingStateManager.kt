package com.example.moodle.ui.whiteboard.managers
import com.example.moodle.ui.whiteboard.models.*
import com.example.moodle.ui.whiteboard.listeners.*

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region

/**
 * Manager responsible for maintaining drawing state and object collections
 */
class DrawingStateManager {
    
    // Current tool settings
    var currentTool = DrawingTool.PEN
    var currentColor = Color.BLACK
    var currentStrokeWidth = 8f
    
    // Listeners
    var drawingChangeListener: OnDrawingChangeListener? = null
    var textInputListener: OnTextInputListener? = null
    
    // Path tracking
    val paths = mutableListOf<DrawingPath>()
    private val undoStack = mutableListOf<DrawingPath>()
    
    // Text objects tracking
    val textObjects = mutableListOf<TextObject>()
    var selectedTextObject: TextObject? = null
    var isDraggingText = false
    var isResizingText = false
    var isRotatingText = false
    var dragStartX = 0f
    var dragStartY = 0f
    var initialTextSize = 0f
    var initialRotation = 0f
    
    // Shape objects tracking
    val shapeObjects = mutableListOf<ShapeObject>()
    var selectedShapeObject: ShapeObject? = null
    var isDraggingShape = false
    var isResizingShape = false
    var isRotatingShape = false
    var currentResizeHandle: ShapeObject.ResizeHandle? = null
    var initialShapeStartX = 0f
    var initialShapeStartY = 0f
    var initialShapeEndX = 0f
    var initialShapeEndY = 0f
    var initialShapeRotation = 0f
    
    // Image objects tracking
    val imageObjects = mutableListOf<ImageObject>()
    var selectedImageObject: ImageObject? = null
    var isDraggingImage = false
    var isResizingImage = false
    var isRotatingImage = false
    var currentImageResizeHandle: ImageObject.ResizeHandle? = null
    var initialImageX = 0f
    var initialImageY = 0f
    var initialImageWidth = 0f
    var initialImageHeight = 0f
    var initialImageScaleX = 1f
    var initialImageScaleY = 1f
    var initialImageRotation = 0f
    
    // Active drawing paths (multi-touch support)
    val activePaths = mutableMapOf<Int, Path>()
    val activePathsLastX = mutableMapOf<Int, Float>()
    val activePathsLastY = mutableMapOf<Int, Float>()
    
    // Selection state
    var selectionStartX = 0f
    var selectionStartY = 0f
    var selectionEndX = 0f
    var selectionEndY = 0f
    var isSelecting = false
    
    /**
     * Add a completed drawing path
     */
    fun addPath(path: DrawingPath) {
        paths.add(path)
        undoStack.clear()
    }
    
    /**
     * Add a text object
     */
    fun addTextObject(textObject: TextObject) {
        textObjects.add(textObject)
        selectedTextObject?.isSelected = false
        selectedTextObject = textObject
        textObject.isSelected = true
    }
    
    /**
     * Add a shape object
     */
    fun addShapeObject(shapeObject: ShapeObject) {
        shapeObjects.add(shapeObject)
        selectedShapeObject?.isSelected = false
        selectedShapeObject = shapeObject
        shapeObject.isSelected = true
        undoStack.clear()
    }
    
    /**
     * Add an image object
     */
    fun addImageObject(imageObject: ImageObject) {
        imageObjects.add(imageObject)
        selectedImageObject?.isSelected = false
        selectedTextObject?.isSelected = false
        selectedShapeObject?.isSelected = false
        selectedImageObject = imageObject
        imageObject.isSelected = true
    }
    
    /**
     * Clear undo stack
     */
    fun clearUndoStack() {
        undoStack.clear()
    }
    
    /**
     * Remove selected text object
     */
    fun removeSelectedTextObject(): Boolean {
        selectedTextObject?.let { obj ->
            textObjects.remove(obj)
            selectedTextObject = null
            return true
        }
        return false
    }
    
    /**
     * Remove selected shape object
     */
    fun removeSelectedShapeObject(): Boolean {
        selectedShapeObject?.let { obj ->
            shapeObjects.remove(obj)
            selectedShapeObject = null
            return true
        }
        return false
    }
    
    /**
     * Remove selected image object
     */
    fun removeSelectedImageObject(): Boolean {
        selectedImageObject?.let { obj ->
            imageObjects.remove(obj)
            selectedImageObject = null
            return true
        }
        return false
    }
    
    /**
     * Deselect all objects
     */
    fun deselectAll() {
        selectedTextObject?.isSelected = false
        selectedTextObject = null
        
        selectedShapeObject?.isSelected = false
        selectedShapeObject = null
        
        selectedImageObject?.isSelected = false
        selectedImageObject = null
    }
    
    /**
     * Perform undo operation
     */
    fun undo(): Boolean {
        if (paths.isNotEmpty()) {
            val lastPath = paths.removeAt(paths.size - 1)
            undoStack.add(lastPath)
            return true
        }
        return false
    }
    
    /**
     * Perform redo operation
     */
    fun redo(): Boolean {
        if (undoStack.isNotEmpty()) {
            val path = undoStack.removeAt(undoStack.size - 1)
            paths.add(path)
            return true
        }
        return false
    }
    
    /**
     * Check if undo is available
     */
    fun canUndo(): Boolean = paths.isNotEmpty()
    
    /**
     * Check if redo is available
     */
    fun canRedo(): Boolean = undoStack.isNotEmpty()
    
    /**
     * Clear all drawing data
     */
    fun clear() {
        paths.clear()
        undoStack.clear()
        textObjects.clear()
        shapeObjects.clear()
        imageObjects.clear()
        deselectAll()
    }
    
    /**
     * Draw all objects to canvas
     */
    fun drawObjectsToCanvas(canvas: Canvas) {
        // Draw text objects
        for (textObject in textObjects) {
            textObject.draw(canvas)
        }
        
        // Draw shape objects
        for (shapeObject in shapeObjects) {
            shapeObject.draw(canvas)
        }
        
        // Draw image objects
        for (imageObject in imageObjects) {
            imageObject.draw(canvas)
        }
    }
    
    /**
     * Redraw all paths to a bitmap canvas
     */
    fun redrawPathsToBitmapCanvas(canvas: Canvas?) {
        canvas?.let { c ->
            for (drawingPath in paths) {
                if (drawingPath.tool == DrawingTool.TEXT && drawingPath.text != null) {
                    c.drawText(drawingPath.text, drawingPath.textX, drawingPath.textY, drawingPath.paint)
                } else {
                    c.drawPath(drawingPath.path, drawingPath.paint)
                }
            }
        }
    }
    
    /**
     * Get all paths
     */
    fun getAllPaths(): List<DrawingPath> = paths.toList()
    
    /**
     * Get all text objects
     */
    fun getAllTextObjects(): List<TextObject> = textObjects.toList()
    
    /**
     * Get all shape objects
     */
    fun getAllShapeObjects(): List<ShapeObject> = shapeObjects.toList()
    
    /**
     * Get all image objects
     */
    fun getAllImageObjects(): List<ImageObject> = imageObjects.toList()
    
    /**
     * Load page data
     */
    fun loadPageData(
        pagePaths: List<DrawingPath>,
        pageTextObjects: List<TextObject>,
        pageShapeObjects: List<ShapeObject>,
        pageImageObjects: List<ImageObject>
    ) {
        clear()
        paths.addAll(pagePaths)
        textObjects.addAll(pageTextObjects)
        shapeObjects.addAll(pageShapeObjects)
        imageObjects.addAll(pageImageObjects)
    }
    
    // Touch handling methods (to be called from TouchGestureHandler)
    fun handleTextTouchDown(x: Float, y: Float, onTextInputRequested: (Float, Float) -> Unit) {
        // Check if touching existing text
        var touchedText: TextObject? = null
        for (textObj in textObjects.reversed()) {
            if (textObj.contains(x, y)) {
                touchedText = textObj
                break
            }
        }
        
        if (touchedText != null) {
            selectedTextObject?.isSelected = false
            selectedTextObject = touchedText
            touchedText.isSelected = true
            isDraggingText = true
            dragStartX = x
            dragStartY = y
        } else {
            selectedTextObject?.isSelected = false
            selectedTextObject = null
            onTextInputRequested(x, y)
        }
    }
    
    fun handleTextTouchMove(x: Float, y: Float) {
        selectedTextObject?.let { textObj ->
            if (isDraggingText) {
                val deltaX = x - dragStartX
                val deltaY = y - dragStartY
                textObj.x += deltaX
                textObj.y += deltaY
                dragStartX = x
                dragStartY = y
            }
        }
    }
    
    fun handleTextTouchUp(x: Float, y: Float) {
        isDraggingText = false
    }
    
    fun handleSelectionTouchDown(x: Float, y: Float) {
        // Find touched object
        var touchedText: TextObject? = null
        for (textObj in textObjects.reversed()) {
            if (textObj.contains(x, y)) {
                touchedText = textObj
                break
            }
        }
        
        var touchedShape: ShapeObject? = null
        for (shapeObj in shapeObjects.reversed()) {
            if (shapeObj.contains(x, y)) {
                touchedShape = shapeObj
                break
            }
        }
        
        var touchedImage: ImageObject? = null
        for (imageObj in imageObjects.reversed()) {
            if (imageObj.contains(x, y)) {
                touchedImage = imageObj
                break
            }
        }
        
        // Select first found object
        if (touchedText != null) {
            selectedTextObject?.isSelected = false
            selectedShapeObject?.isSelected = false
            selectedImageObject?.isSelected = false
            selectedTextObject = touchedText
            touchedText.isSelected = true
            isDraggingText = true
            dragStartX = x
            dragStartY = y
        } else if (touchedShape != null) {
            selectedTextObject?.isSelected = false
            selectedShapeObject?.isSelected = false
            selectedImageObject?.isSelected = false
            selectedShapeObject = touchedShape
            touchedShape.isSelected = true
            isDraggingShape = true
            dragStartX = x
            dragStartY = y
        } else if (touchedImage != null) {
            selectedTextObject?.isSelected = false
            selectedShapeObject?.isSelected = false
            selectedImageObject?.isSelected = false
            selectedImageObject = touchedImage
            touchedImage.isSelected = true
            isDraggingImage = true
            dragStartX = x
            dragStartY = y
        } else {
            // Deselect all
            selectedTextObject?.isSelected = false
            selectedShapeObject?.isSelected = false
            selectedImageObject?.isSelected = false
            selectedTextObject = null
            selectedShapeObject = null
            selectedImageObject = null
        }
    }
    
    fun handleSelectionTouchMove(x: Float, y: Float) {
        // Handle dragging
        if (isDraggingText) {
            selectedTextObject?.let {
                val dx = x - dragStartX
                val dy = y - dragStartY
                it.x += dx
                it.y += dy
                dragStartX = x
                dragStartY = y
            }
        } else if (isDraggingShape) {
            selectedShapeObject?.let {
                val dx = x - dragStartX
                val dy = y - dragStartY
                it.startX += dx
                it.startY += dy
                it.endX += dx
                it.endY += dy
                dragStartX = x
                dragStartY = y
            }
        } else if (isDraggingImage) {
            selectedImageObject?.let {
                val dx = x - dragStartX
                val dy = y - dragStartY
                it.x += dx
                it.y += dy
                dragStartX = x
                dragStartY = y
            }
        }
    }
    
    fun handleSelectionTouchUp(x: Float, y: Float) {
        isDraggingText = false
        isDraggingShape = false
        isDraggingImage = false
    }
    
    fun handleShapeObjectInteraction(x: Float, y: Float, action: Int): Boolean {
        // Check if touching existing shape
        if (action == android.view.MotionEvent.ACTION_DOWN) {
            for (shapeObj in shapeObjects.reversed()) {
                if (shapeObj.contains(x, y)) {
                    selectedShapeObject?.isSelected = false
                    selectedShapeObject = shapeObj
                    shapeObj.isSelected = true
                    isDraggingShape = true
                    dragStartX = x
                    dragStartY = y
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * Erases elements that intersect with the selection area
     * Removes entire paths/strokes, text objects, shape objects, and image objects
     * that intersect with the selection
     */
    fun eraseElementsInSelection(selectionPath: Path?) {
        if (selectionPath == null) return
        
        // Create a region from the selection path for intersection testing
        val selectionBounds = RectF()
        selectionPath.computeBounds(selectionBounds, true)
        val selectionRegion = Region().apply {
            setPath(selectionPath, Region(
                selectionBounds.left.toInt(),
                selectionBounds.top.toInt(),
                selectionBounds.right.toInt(),
                selectionBounds.bottom.toInt()
            ))
        }
        
        // Handle drawing paths - remove paths that intersect with selection
        val pathsToRemove = mutableListOf<DrawingPath>()
        
        for (drawingPath in paths) {
            // Check if path intersects with selection region
            val pathBounds = RectF()
            drawingPath.path.computeBounds(pathBounds, true)
            
            // Quick bounds check first
            if (!RectF.intersects(pathBounds, selectionBounds)) {
                continue
            }
            
            // More precise region-based intersection check
            val pathRegion = Region().apply {
                setPath(drawingPath.path, Region(
                    pathBounds.left.toInt(),
                    pathBounds.top.toInt(),
                    pathBounds.right.toInt(),
                    pathBounds.bottom.toInt()
                ))
            }
            
            // Create a copy of selection region for testing (since op modifies it)
            val testRegion = Region(selectionRegion)
            if (testRegion.op(pathRegion, Region.Op.INTERSECT) && !testRegion.isEmpty) {
                // Path intersects with selection, mark for removal
                pathsToRemove.add(drawingPath)
            }
        }
        
        // Apply path changes
        paths.removeAll(pathsToRemove)
        
        // Handle text objects - remove if they intersect with selection
        val textObjectsToRemove = mutableListOf<TextObject>()
        for (textObj in textObjects) {
            // Get text bounds
            val textPaint = Paint().apply {
                textSize = textObj.textSize
            }
            val textBounds = Rect()
            textPaint.getTextBounds(textObj.text, 0, textObj.text.length, textBounds)
            
            val textRectF = RectF(
                textObj.x - 10f,
                textObj.y + textBounds.top - 10f,
                textObj.x + textBounds.width() + 10f,
                textObj.y + textBounds.bottom + 10f
            )
            
            // Check if text intersects with selection
            if (RectF.intersects(textRectF, selectionBounds)) {
                textObjectsToRemove.add(textObj)
            }
        }
        textObjects.removeAll(textObjectsToRemove)
        if (selectedTextObject in textObjectsToRemove) {
            selectedTextObject = null
        }
        
        // Handle shape objects - remove if they intersect with selection
        val shapeObjectsToRemove = mutableListOf<ShapeObject>()
        for (shapeObj in shapeObjects) {
            val shapeBounds = shapeObj.getBounds()
            
            // Check if shape intersects with selection
            if (RectF.intersects(shapeBounds, selectionBounds)) {
                shapeObjectsToRemove.add(shapeObj)
            }
        }
        shapeObjects.removeAll(shapeObjectsToRemove)
        if (selectedShapeObject in shapeObjectsToRemove) {
            selectedShapeObject = null
        }
        
        // Handle image objects - remove if they intersect with selection
        val imageObjectsToRemove = mutableListOf<ImageObject>()
        for (imageObj in imageObjects) {
            val imageBounds = imageObj.getBounds()
            
            // Check if image intersects with selection
            if (RectF.intersects(imageBounds, selectionBounds)) {
                imageObjectsToRemove.add(imageObj)
            }
        }
        imageObjects.removeAll(imageObjectsToRemove)
        if (selectedImageObject in imageObjectsToRemove) {
            selectedImageObject = null
        }
        
        // Clear undo stack since we've modified the drawing
        undoStack.clear()
    }
}

