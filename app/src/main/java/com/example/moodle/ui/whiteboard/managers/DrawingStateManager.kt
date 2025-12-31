package com.example.moodle.ui.whiteboard.managers
import com.example.moodle.ui.whiteboard.models.*
import com.example.moodle.ui.whiteboard.listeners.*

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
    
    /**
     * Add a completed drawing path
     */
    fun addPath(path: DrawingPath) {
        paths.add(path)
    }
    
    /**
     * Add a text object
     */
    fun addTextObject(textObject: TextObject) {
        textObjects.add(textObject)
        deselectAll()
        // Select the new text object
        selectedTextObject = textObject
        textObject.isSelected = true
    }
    
    /**
     * Add a shape object
     */
    fun addShapeObject(shapeObject: ShapeObject) {
        shapeObjects.add(shapeObject)
        deselectAll()
        // Select the new shape object
        selectedShapeObject = shapeObject
        shapeObject.isSelected = true
    }
    
    /**
     * Add an image object
     */
    fun addImageObject(imageObject: ImageObject) {
        imageObjects.add(imageObject)
        deselectAll()
        // Select the new image object
        selectedImageObject = imageObject
        imageObject.isSelected = true
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
        
        for (textObj in textObjects) textObj.isSelected = false
        for (shapeObj in shapeObjects) shapeObj.isSelected = false
        for (imageObj in imageObjects) imageObj.isSelected = false
    }
    
    /**
     * Clear all drawing data
     */
    fun clear() {
        paths.clear()
        textObjects.clear()
        shapeObjects.clear()
        imageObjects.clear()
        deselectAll()
    }
    
    // Touch handling methods (to be called from TouchGestureHandler)
    fun handleTextTouchDown(x: Float, y: Float, onTextInputRequested: (Float, Float) -> Unit) {
        // First check if touching selected text's special handles
        selectedTextObject?.let { textObj ->
            if (textObj.isDeleteButtonTouched(x, y)) {
                removeSelectedTextObject()
                return
            }
            if (textObj.isResizeHandleTouched(x, y)) {
                isResizingText = true
                dragStartX = x
                dragStartY = y
                initialTextSize = textObj.textSize
                return
            }
            if (textObj.isRotationHandleTouched(x, y)) {
                isRotatingText = true
                dragStartX = x
                dragStartY = y
                initialRotation = textObj.rotation
                return
            }
            if (textObj.contains(x, y)) {
                isDraggingText = true
                dragStartX = x
                dragStartY = y
                return
            }
        }
        
        // Check if touching existing text
        var touchedText: TextObject? = null
        for (textObj in textObjects.reversed()) {
            if (textObj.contains(x, y)) {
                touchedText = textObj
                break
            }
        }
        
        if (touchedText != null) {
            deselectAll()
            selectedTextObject = touchedText
            touchedText.isSelected = true
            isDraggingText = true
            dragStartX = x
            dragStartY = y
        } else {
            deselectAll()
            onTextInputRequested(x, y)
        }
    }
    
    fun handleTextTouchMove(x: Float, y: Float) {
        if (isDraggingText) {
            selectedTextObject?.let {
                val dx = x - dragStartX
                val dy = y - dragStartY
                it.x += dx
                it.y += dy
                dragStartX = x
                dragStartY = y
            }
        } else if (isResizingText) {
            selectedTextObject?.let { textObj ->
                val dx = x - dragStartX
                val dy = y - dragStartY
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                val direction = if (dy > 0) 1 else -1
                textObj.textSize = (initialTextSize + distance * direction * 0.5f).coerceIn(12f, 200f)
            }
        } else if (isRotatingText) {
            selectedTextObject?.let { textObj ->
                val bounds = android.graphics.Rect()
                textObj.paint.getTextBounds(textObj.text, 0, textObj.text.length, bounds)
                val centerX = textObj.x + bounds.width() / 2f
                val centerY = textObj.y + bounds.height() / 2f
                
                val angle1 = kotlin.math.atan2(dragStartY - centerY, dragStartX - centerX)
                val angle2 = kotlin.math.atan2(y - centerY, x - centerX)
                val angleDiff = Math.toDegrees((angle2 - angle1).toDouble()).toFloat()
                textObj.rotation = (initialRotation + angleDiff) % 360f
            }
        }
    }
    
    fun handleTextTouchUp(x: Float, y: Float) {
        isDraggingText = false
        isResizingText = false
        isRotatingText = false
    }
    
    fun handleSelectionTouchDown(x: Float, y: Float) {
        // First check if touching selected object's special handles
        selectedTextObject?.let { textObj ->
            if (textObj.isDeleteButtonTouched(x, y)) {
                removeSelectedTextObject()
                return
            }
            if (textObj.isResizeHandleTouched(x, y)) {
                isResizingText = true
                dragStartX = x
                dragStartY = y
                initialTextSize = textObj.textSize
                return
            }
            if (textObj.isRotationHandleTouched(x, y)) {
                isRotatingText = true
                dragStartX = x
                dragStartY = y
                initialRotation = textObj.rotation
                return
            }
            if (textObj.contains(x, y)) {
                isDraggingText = true
                dragStartX = x
                dragStartY = y
                return
            }
        }
        
        selectedShapeObject?.let { shapeObj ->
            if (shapeObj.isDeleteButtonTouched(x, y)) {
                removeSelectedShapeObject()
                return
            }
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
                return
            }
            if (shapeObj.isRotationHandleTouched(x, y)) {
                isRotatingShape = true
                dragStartX = x
                dragStartY = y
                initialShapeRotation = shapeObj.rotation
                return
            }
            if (shapeObj.contains(x, y)) {
                isDraggingShape = true
                dragStartX = x
                dragStartY = y
                return
            }
        }
        
        selectedImageObject?.let { imageObj ->
            if (imageObj.isDeleteButtonTouched(x, y)) {
                removeSelectedImageObject()
                return
            }
            val resizeHandle = imageObj.isResizeHandleTouched(x, y)
            if (resizeHandle != null) {
                isResizingImage = true
                currentImageResizeHandle = resizeHandle
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
            if (imageObj.isRotationHandleTouched(x, y)) {
                isRotatingImage = true
                dragStartX = x
                dragStartY = y
                initialImageRotation = imageObj.rotation
                return
            }
            if (imageObj.contains(x, y)) {
                isDraggingImage = true
                dragStartX = x
                dragStartY = y
                return
            }
        }
        
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
            deselectAll()
            selectedTextObject = touchedText
            touchedText.isSelected = true
            isDraggingText = true
            dragStartX = x
            dragStartY = y
        } else if (touchedShape != null) {
            deselectAll()
            selectedShapeObject = touchedShape
            touchedShape.isSelected = true
            isDraggingShape = true
            dragStartX = x
            dragStartY = y
        } else if (touchedImage != null) {
            deselectAll()
            selectedImageObject = touchedImage
            touchedImage.isSelected = true
            isDraggingImage = true
            dragStartX = x
            dragStartY = y
        } else {
            // Deselect all
            deselectAll()
        }
    }
    
    fun handleSelectionTouchMove(x: Float, y: Float) {
        // Handle text operations
        if (isDraggingText) {
            selectedTextObject?.let {
                val dx = x - dragStartX
                val dy = y - dragStartY
                it.x += dx
                it.y += dy
                dragStartX = x
                dragStartY = y
            }
        } else if (isResizingText) {
            selectedTextObject?.let { textObj ->
                val dx = x - dragStartX
                val dy = y - dragStartY
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                val direction = if (dy > 0) 1 else -1
                textObj.textSize = (initialTextSize + distance * direction * 0.5f).coerceIn(12f, 200f)
            }
        } else if (isRotatingText) {
            selectedTextObject?.let { textObj ->
                val bounds = android.graphics.Rect()
                textObj.paint.getTextBounds(textObj.text, 0, textObj.text.length, bounds)
                val centerX = textObj.x + bounds.width() / 2f
                val centerY = textObj.y + bounds.height() / 2f
                
                val angle1 = kotlin.math.atan2(dragStartY - centerY, dragStartX - centerX)
                val angle2 = kotlin.math.atan2(y - centerY, x - centerX)
                val angleDiff = Math.toDegrees((angle2 - angle1).toDouble()).toFloat()
                textObj.rotation = (initialRotation + angleDiff) % 360f
            }
        }
        
        // Handle shape operations
        else if (isDraggingShape) {
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
        } else if (isResizingShape) {
            selectedShapeObject?.let { shapeObj ->
                when (currentResizeHandle) {
                    ShapeObject.ResizeHandle.TOP_LEFT -> {
                        shapeObj.startX = initialShapeStartX + (x - dragStartX)
                        shapeObj.startY = initialShapeStartY + (y - dragStartY)
                    }
                    ShapeObject.ResizeHandle.TOP_RIGHT -> {
                        shapeObj.endX = initialShapeEndX + (x - dragStartX)
                        shapeObj.startY = initialShapeStartY + (y - dragStartY)
                    }
                    ShapeObject.ResizeHandle.BOTTOM_LEFT -> {
                        shapeObj.startX = initialShapeStartX + (x - dragStartX)
                        shapeObj.endY = initialShapeEndY + (y - dragStartY)
                    }
                    ShapeObject.ResizeHandle.BOTTOM_RIGHT -> {
                        shapeObj.endX = initialShapeEndX + (x - dragStartX)
                        shapeObj.endY = initialShapeEndY + (y - dragStartY)
                    }
                    ShapeObject.ResizeHandle.LINE_START -> {
                        shapeObj.startX = x
                        shapeObj.startY = y
                    }
                    ShapeObject.ResizeHandle.LINE_END -> {
                        shapeObj.endX = x
                        shapeObj.endY = y
                    }
                    null -> {}
                }
            }
        } else if (isRotatingShape) {
            selectedShapeObject?.let { shapeObj ->
                val centerX = shapeObj.getCenterX()
                val centerY = shapeObj.getCenterY()
                
                val angle1 = kotlin.math.atan2(dragStartY - centerY, dragStartX - centerX)
                val angle2 = kotlin.math.atan2(y - centerY, x - centerX)
                val angleDiff = Math.toDegrees((angle2 - angle1).toDouble()).toFloat()
                shapeObj.rotation = (initialShapeRotation + angleDiff) % 360f
            }
        }
        
        // Handle image operations
        else if (isDraggingImage) {
            selectedImageObject?.let {
                val dx = x - dragStartX
                val dy = y - dragStartY
                it.x += dx
                it.y += dy
                dragStartX = x
                dragStartY = y
            }
        } else if (isResizingImage) {
            selectedImageObject?.let { imageObj ->
                val dx = x - dragStartX
                val dy = y - dragStartY
                
                when (currentImageResizeHandle) {
                    ImageObject.ResizeHandle.TOP_LEFT -> {
                        val newX = initialImageX + dx
                        val newY = initialImageY + dy
                        val newWidth = initialImageWidth - dx
                        val newHeight = initialImageHeight - dy
                        
                        if (newWidth > 20f && newHeight > 20f) {
                            imageObj.x = newX
                            imageObj.y = newY
                            imageObj.width = newWidth
                            imageObj.height = newHeight
                        }
                    }
                    ImageObject.ResizeHandle.TOP_RIGHT -> {
                        val newY = initialImageY + dy
                        val newWidth = initialImageWidth + dx
                        val newHeight = initialImageHeight - dy
                        
                        if (newWidth > 20f && newHeight > 20f) {
                            imageObj.y = newY
                            imageObj.width = newWidth
                            imageObj.height = newHeight
                        }
                    }
                    ImageObject.ResizeHandle.BOTTOM_LEFT -> {
                        val newX = initialImageX + dx
                        val newWidth = initialImageWidth - dx
                        val newHeight = initialImageHeight + dy
                        
                        if (newWidth > 20f && newHeight > 20f) {
                            imageObj.x = newX
                            imageObj.width = newWidth
                            imageObj.height = newHeight
                        }
                    }
                    ImageObject.ResizeHandle.BOTTOM_RIGHT -> {
                        val newWidth = initialImageWidth + dx
                        val newHeight = initialImageHeight + dy
                        
                        if (newWidth > 20f && newHeight > 20f) {
                            imageObj.width = newWidth
                            imageObj.height = newHeight
                        }
                    }
                    null -> {}
                }
            }
        } else if (isRotatingImage) {
            selectedImageObject?.let { imageObj ->
                val centerX = imageObj.getCenterX()
                val centerY = imageObj.getCenterY()
                
                val angle1 = kotlin.math.atan2(dragStartY - centerY, dragStartX - centerX)
                val angle2 = kotlin.math.atan2(y - centerY, x - centerX)
                val angleDiff = Math.toDegrees((angle2 - angle1).toDouble()).toFloat()
                imageObj.rotation = (initialImageRotation + angleDiff) % 360f
            }
        }
    }
    
    fun handleSelectionTouchUp(x: Float, y: Float) {
        // Reset all operation flags
        isDraggingText = false
        isDraggingShape = false
        isDraggingImage = false
        isResizingText = false
        isResizingShape = false
        isResizingImage = false
        isRotatingText = false
        isRotatingShape = false
        isRotatingImage = false
        currentResizeHandle = null
        currentImageResizeHandle = null
    }
    
    fun handleShapeObjectInteraction(x: Float, y: Float, action: Int): Boolean {
        when (action) {
            android.view.MotionEvent.ACTION_DOWN -> {
                // First check if touching selected shape's special handles
                selectedShapeObject?.let { shapeObj ->
                    if (shapeObj.isDeleteButtonTouched(x, y)) {
                        removeSelectedShapeObject()
                        return true
                    }
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
                    if (shapeObj.isRotationHandleTouched(x, y)) {
                        isRotatingShape = true
                        dragStartX = x
                        dragStartY = y
                        initialShapeRotation = shapeObj.rotation
                        return true
                    }
                    if (shapeObj.contains(x, y)) {
                        isDraggingShape = true
                        dragStartX = x
                        dragStartY = y
                        return true
                    }
                }
                
                // Check if touching any shape object
                for (shapeObj in shapeObjects.reversed()) {
                    if (shapeObj.contains(x, y)) {
                        deselectAll()
                        selectedShapeObject = shapeObj
                        shapeObj.isSelected = true
                        isDraggingShape = true
                        dragStartX = x
                        dragStartY = y
                        return true
                    }
                }
            }
            
            android.view.MotionEvent.ACTION_MOVE -> {
                if (isDraggingShape || isResizingShape || isRotatingShape) {
                    handleSelectionTouchMove(x, y)
                    return true
                }
            }
            
            android.view.MotionEvent.ACTION_UP -> {
                if (isDraggingShape || isResizingShape || isRotatingShape) {
                    handleSelectionTouchUp(x, y)
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
    }
}

