package com.example.moodle.ui.whiteboard.managers

import com.example.moodle.ui.whiteboard.commands.*
import com.example.moodle.ui.whiteboard.models.*

/**
 * Helper class to integrate command-based undo/redo with WhiteboardView
 * 
 * This class bridges between the drawing operations and the CommandManager,
 * converting high-level actions into commands that can be undone/redone.
 * 
 * Usage:
 * - Call execute methods to perform actions with automatic command creation
 * - Commands are automatically added to the CommandManager's undo stack
 * - Provides callback for view invalidation after commands execute
 */
class CommandBasedUndoRedoHelper(
    private val commandManager: CommandManager,
    private val stateManager: DrawingStateManager,
    private val bitmapManager: BitmapManager
) {
    
    // Callback to invalidate view after command execution
    var onInvalidateView: (() -> Unit)? = null
    
    // Callback to redraw canvas after undo/redo
    var onRedrawCanvas: (() -> Unit)? = null
    
    /**
     * Execute a path draw action
     */
    fun addPath(path: DrawingPath) {
        val command = AddPathCommand(stateManager, bitmapManager, path)
        commandManager.execute(command)
        onInvalidateView?.invoke()
    }
    
    /**
     * Execute multiple paths (batch operation)
     */
    fun addPaths(paths: List<DrawingPath>) {
        val command = AddPathsBatchCommand(stateManager, bitmapManager, paths)
        commandManager.execute(command)
        onInvalidateView?.invoke()
    }
    
    /**
     * Remove a path
     */
    fun removePath(path: DrawingPath) {
        val command = RemovePathCommand(stateManager, path)
        commandManager.execute(command)
        onRedrawCanvas?.invoke()
        onInvalidateView?.invoke()
    }
    
    /**
     * Add a text object
     */
    fun addText(textObject: TextObject) {
        val command = AddTextCommand(stateManager, textObject)
        commandManager.execute(command)
        onInvalidateView?.invoke()
    }
    
    /**
     * Remove a text object
     */
    fun removeText(textObject: TextObject) {
        val command = RemoveTextCommand(stateManager, textObject)
        commandManager.execute(command)
        onInvalidateView?.invoke()
    }
    
    /**
     * Modify a text object
     */
    fun modifyText(
        textObject: TextObject,
        newText: String? = null,
        newX: Float? = null,
        newY: Float? = null,
        newSize: Float? = null,
        newColor: Int? = null,
        newRotation: Float? = null
    ) {
        val command = ModifyTextCommand(
            textObject, newText, newX, newY, newSize, newColor, newRotation
        )
        commandManager.execute(command)
        onInvalidateView?.invoke()
    }
    
    /**
     * Add a shape object
     */
    fun addShape(shapeObject: ShapeObject) {
        val command = AddShapeCommand(stateManager, shapeObject)
        commandManager.execute(command)
        onInvalidateView?.invoke()
    }
    
    /**
     * Remove a shape object
     */
    fun removeShape(shapeObject: ShapeObject) {
        val command = RemoveShapeCommand(stateManager, shapeObject)
        commandManager.execute(command)
        onInvalidateView?.invoke()
    }
    
    /**
     * Modify a shape object
     */
    fun modifyShape(
        shapeObject: ShapeObject,
        newStartX: Float? = null,
        newStartY: Float? = null,
        newEndX: Float? = null,
        newEndY: Float? = null,
        newColor: Int? = null,
        newRotation: Float? = null
    ) {
        val command = ModifyShapeCommand(
            shapeObject, newStartX, newStartY, newEndX, newEndY, newColor, newRotation
        )
        commandManager.execute(command)
        onInvalidateView?.invoke()
    }
    
    /**
     * Add an image object
     */
    fun addImage(imageObject: ImageObject) {
        val command = AddImageCommand(stateManager, imageObject)
        commandManager.execute(command)
        onInvalidateView?.invoke()
    }

    /**
     * Remove an image object
     */
    fun removeImage(imageObject: ImageObject) {
        val command = RemoveImageCommand(stateManager, imageObject)
        commandManager.execute(command)
        onInvalidateView?.invoke()
    }

    /**
     * Modify an image object
     */
    fun modifyImage(
        imageObject: ImageObject,
        newX: Float? = null,
        newY: Float? = null,
        newWidth: Float? = null,
        newHeight: Float? = null,
        newRotation: Float? = null
    ) {
        val command = ModifyImageCommand(
            imageObject, newX, newY, newWidth, newHeight, newRotation
        )
        commandManager.execute(command)
        onInvalidateView?.invoke()
    }

    /**
     * Clear the entire canvas
     */
    fun clearCanvas() {
        val command = ClearCanvasCommand(stateManager)
        commandManager.execute(command)
        onRedrawCanvas?.invoke()
        onInvalidateView?.invoke()
    }

    /**
     * Cancel the current batch
     */
    fun cancelBatch() {
        commandManager.cancelBatch()
    }

    /**
     * Perform undo
     */
    fun undo(): Boolean {
        val result = commandManager.undo()
        if (result) {
            onRedrawCanvas?.invoke()
            onInvalidateView?.invoke()
        }
        return result
    }
    
    /**
     * Perform redo
     */
    fun redo(): Boolean {
        val result = commandManager.redo()
        if (result) {
            onRedrawCanvas?.invoke()
            onInvalidateView?.invoke()
        }
        return result
    }
    
    /**
     * Check if undo is available
     */
    fun canUndo(): Boolean = commandManager.canUndo()
    
    /**
     * Check if redo is available
     */
    fun canRedo(): Boolean = commandManager.canRedo()
}

