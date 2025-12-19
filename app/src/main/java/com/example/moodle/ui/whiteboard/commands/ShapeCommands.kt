package com.example.moodle.ui.whiteboard.commands

import com.example.moodle.ui.whiteboard.managers.DrawingStateManager
import com.example.moodle.ui.whiteboard.models.ShapeObject

/**
 * Command to add a shape object to the canvas
 */
class AddShapeCommand(
    private val stateManager: DrawingStateManager,
    private val shapeObject: ShapeObject
) : DrawingCommand {
    
    override fun execute() {
        stateManager.shapeObjects.add(shapeObject)
    }
    
    override fun undo() {
        stateManager.shapeObjects.remove(shapeObject)
    }
    
    override fun getMemoryUsage(): Long {
        // ShapeObject estimate
        return 300L
    }
    
    override fun getDescription(): String {
        return "Add ${shapeObject.shapeType.name.lowercase()}"
    }
}

/**
 * Command to remove a shape object from the canvas
 */
class RemoveShapeCommand(
    private val stateManager: DrawingStateManager,
    private val shapeObject: ShapeObject
) : DrawingCommand {
    
    private var removedIndex: Int = -1
    
    override fun execute() {
        removedIndex = stateManager.shapeObjects.indexOf(shapeObject)
        if (removedIndex >= 0) {
            stateManager.shapeObjects.removeAt(removedIndex)
        }
    }
    
    override fun undo() {
        if (removedIndex >= 0) {
            stateManager.shapeObjects.add(removedIndex, shapeObject)
        }
    }
    
    override fun getMemoryUsage(): Long {
        return 300L + 8L
    }
    
    override fun getDescription(): String {
        return "Remove ${shapeObject.shapeType.name.lowercase()}"
    }
}

/**
 * Command to modify a shape object's properties
 */
class ModifyShapeCommand(
    private val shapeObject: ShapeObject,
    private val newStartX: Float? = null,
    private val newStartY: Float? = null,
    private val newEndX: Float? = null,
    private val newEndY: Float? = null,
    private val newColor: Int? = null,
    private val newRotation: Float? = null
) : DrawingCommand {
    
    private val oldStartX = shapeObject.startX
    private val oldStartY = shapeObject.startY
    private val oldEndX = shapeObject.endX
    private val oldEndY = shapeObject.endY
    private val oldColor = shapeObject.color
    private val oldRotation = shapeObject.rotation
    
    override fun execute() {
        newStartX?.let { shapeObject.startX = it }
        newStartY?.let { shapeObject.startY = it }
        newEndX?.let { shapeObject.endX = it }
        newEndY?.let { shapeObject.endY = it }
        newColor?.let { shapeObject.color = it }
        newRotation?.let { shapeObject.rotation = it }
    }
    
    override fun undo() {
        shapeObject.startX = oldStartX
        shapeObject.startY = oldStartY
        shapeObject.endX = oldEndX
        shapeObject.endY = oldEndY
        shapeObject.color = oldColor
        shapeObject.rotation = oldRotation
    }
    
    override fun getMemoryUsage(): Long {
        return 200L
    }
    
    override fun getDescription(): String {
        return when {
            newStartX != null || newStartY != null || newEndX != null || newEndY != null -> "Move/Resize shape"
            newColor != null -> "Change shape color"
            newRotation != null -> "Rotate shape"
            else -> "Modify shape"
        }
    }
}

