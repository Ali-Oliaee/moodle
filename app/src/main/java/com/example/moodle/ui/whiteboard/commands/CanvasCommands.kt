package com.example.moodle.ui.whiteboard.commands

import com.example.moodle.ui.whiteboard.managers.DrawingStateManager
import com.example.moodle.ui.whiteboard.models.*

/**
 * Command to clear the entire canvas
 */
class ClearCanvasCommand(
    private val stateManager: DrawingStateManager
) : DrawingCommand {
    
    private val savedPaths = mutableListOf<DrawingPath>()
    private val savedTextObjects = mutableListOf<TextObject>()
    private val savedShapeObjects = mutableListOf<ShapeObject>()
    private val savedImageObjects = mutableListOf<ImageObject>()
    
    override fun execute() {
        // Save current state before clearing
        savedPaths.clear()
        savedPaths.addAll(stateManager.paths.map { it.copy() })
        
        savedTextObjects.clear()
        savedTextObjects.addAll(stateManager.textObjects.map { it.copy() })
        
        savedShapeObjects.clear()
        savedShapeObjects.addAll(stateManager.shapeObjects.map { it.copy() })
        
        savedImageObjects.clear()
        savedImageObjects.addAll(stateManager.imageObjects.map { it.copy() })
        
        // Clear all
        stateManager.clear()
    }
    
    override fun undo() {
        // Restore saved state
        stateManager.paths.clear()
        stateManager.paths.addAll(savedPaths)
        
        stateManager.textObjects.clear()
        stateManager.textObjects.addAll(savedTextObjects)
        
        stateManager.shapeObjects.clear()
        stateManager.shapeObjects.addAll(savedShapeObjects)
        
        stateManager.imageObjects.clear()
        stateManager.imageObjects.addAll(savedImageObjects)
    }
    
    override fun getMemoryUsage(): Long {
        val pathsSize = savedPaths.size * 800L
        val textSize = savedTextObjects.sumOf { 200L + (it.text.length * 2L) }
        val shapesSize = savedShapeObjects.size * 300L
        val imagesSize = savedImageObjects.sumOf { imageObj ->
            500L + (imageObj.bitmap?.byteCount?.toLong() ?: 0L)
        }
        return pathsSize + textSize + shapesSize + imagesSize
    }
    
    override fun getDescription(): String {
        return "Clear canvas"
    }
}

