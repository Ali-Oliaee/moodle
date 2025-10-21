package com.example.moodle.ui.whiteboard

import android.graphics.Color

/**
 * Data class representing a single page in the whiteboard
 * Contains all drawing elements and state for that page
 */
data class PageData(
    val pageId: Int,
    val paths: MutableList<WhiteboardView.DrawingPath> = mutableListOf(),
    val undoStack: MutableList<WhiteboardView.DrawingPath> = mutableListOf(),
    val textObjects: MutableList<WhiteboardView.TextObject> = mutableListOf(),
    val shapeObjects: MutableList<WhiteboardView.ShapeObject> = mutableListOf(),
    val imageObjects: MutableList<WhiteboardView.ImageObject> = mutableListOf(),
    var backgroundPattern: BackgroundPattern = BackgroundPattern.PLAIN
) {
    fun canUndo(): Boolean = paths.isNotEmpty()
    fun canRedo(): Boolean = undoStack.isNotEmpty()
    
    fun clear() {
        paths.clear()
        undoStack.clear()
        textObjects.clear()
        shapeObjects.clear()
        imageObjects.clear()
    }
    
    fun undo(): WhiteboardView.DrawingPath? {
        return if (canUndo()) {
            val lastPath = paths.removeAt(paths.size - 1)
            undoStack.add(lastPath)
            lastPath
        } else null
    }
    
    fun redo(): WhiteboardView.DrawingPath? {
        return if (canRedo()) {
            val pathToRedo = undoStack.removeAt(undoStack.size - 1)
            paths.add(pathToRedo)
            pathToRedo
        } else null
    }
}
