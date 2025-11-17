package com.example.moodle.ui.whiteboard.models

/**
 * Data class representing a single page in the whiteboard
 * Contains all drawing elements and state for that page
 */
data class PageData(
    val pageId: Int,
    val paths: MutableList<DrawingPath> = mutableListOf(),
    val undoStack: MutableList<DrawingPath> = mutableListOf(),
    val textObjects: MutableList<TextObject> = mutableListOf(),
    val shapeObjects: MutableList<ShapeObject> = mutableListOf(),
    val imageObjects: MutableList<ImageObject> = mutableListOf(),
    var backgroundPattern: BackgroundPattern = BackgroundPattern.PLAIN,
    var backgroundColor: BackgroundColor = BackgroundColor.WHITE
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
    
    fun undo(): DrawingPath? {
        return if (canUndo()) {
            val lastPath = paths.removeAt(paths.size - 1)
            undoStack.add(lastPath)
            lastPath
        } else null
    }
    
    fun redo(): DrawingPath? {
        return if (canRedo()) {
            val pathToRedo = undoStack.removeAt(undoStack.size - 1)
            paths.add(pathToRedo)
            pathToRedo
        } else null
    }
}
