package com.example.moodle.ui.whiteboard.models

import com.example.moodle.ui.whiteboard.managers.UndoRedoManager

/**
 * Data class representing a single page in the whiteboard
 * Contains all drawing elements and state for that page
 * Each page has its own independent undo/redo history
 */
data class PageData(
    val pageId: Int,
    val paths: MutableList<DrawingPath> = mutableListOf(),
    val textObjects: MutableList<TextObject> = mutableListOf(),
    val shapeObjects: MutableList<ShapeObject> = mutableListOf(),
    val imageObjects: MutableList<ImageObject> = mutableListOf(),
    var backgroundPattern: BackgroundPattern = BackgroundPattern.PLAIN,
    var backgroundColor: BackgroundColor = BackgroundColor.WHITE,
    val undoRedoManager: UndoRedoManager = UndoRedoManager()
) {
    fun clear() {
        paths.clear()
        textObjects.clear()
        shapeObjects.clear()
        imageObjects.clear()
        // Don't clear undo/redo - clearing is an undoable action!
    }
}
