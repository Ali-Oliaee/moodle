package com.example.moodle.ui.whiteboard.commands

import com.example.moodle.ui.whiteboard.managers.DrawingStateManager
import com.example.moodle.ui.whiteboard.models.TextObject

/**
 * Command to add a text object to the canvas
 */
class AddTextCommand(
    private val stateManager: DrawingStateManager,
    private val textObject: TextObject
) : DrawingCommand {
    
    override fun execute() {
        stateManager.textObjects.add(textObject)
    }
    
    override fun undo() {
        stateManager.textObjects.remove(textObject)
    }
    
    override fun getMemoryUsage(): Long {
        // TextObject + text content
        return 200L + (textObject.text.length * 2L)
    }
    
    override fun getDescription(): String {
        val preview = if (textObject.text.length > 20) {
            textObject.text.substring(0, 17) + "..."
        } else {
            textObject.text
        }
        return "Add text \"$preview\""
    }
}

/**
 * Command to remove a text object from the canvas
 */
class RemoveTextCommand(
    private val stateManager: DrawingStateManager,
    private val textObject: TextObject
) : DrawingCommand {
    
    private var removedIndex: Int = -1
    
    override fun execute() {
        removedIndex = stateManager.textObjects.indexOf(textObject)
        if (removedIndex >= 0) {
            stateManager.textObjects.removeAt(removedIndex)
        }
    }
    
    override fun undo() {
        if (removedIndex >= 0) {
            stateManager.textObjects.add(removedIndex, textObject)
        }
    }
    
    override fun getMemoryUsage(): Long {
        return 200L + (textObject.text.length * 2L) + 8L
    }
    
    override fun getDescription(): String {
        return "Remove text"
    }
}

/**
 * Command to modify a text object's properties
 */
class ModifyTextCommand(
    private val textObject: TextObject,
    private val newText: String? = null,
    private val newX: Float? = null,
    private val newY: Float? = null,
    private val newSize: Float? = null,
    private val newColor: Int? = null,
    private val newRotation: Float? = null
) : DrawingCommand {
    
    private val oldText = textObject.text
    private val oldX = textObject.x
    private val oldY = textObject.y
    private val oldSize = textObject.textSize
    private val oldColor = textObject.color
    private val oldRotation = textObject.rotation
    
    override fun execute() {
        newText?.let { textObject.text = it }
        newX?.let { textObject.x = it }
        newY?.let { textObject.y = it }
        newSize?.let { textObject.textSize = it }
        newColor?.let { textObject.color = it }
        newRotation?.let { textObject.rotation = it }
    }
    
    override fun undo() {
        textObject.text = oldText
        textObject.x = oldX
        textObject.y = oldY
        textObject.textSize = oldSize
        textObject.color = oldColor
        textObject.rotation = oldRotation
    }
    
    override fun getMemoryUsage(): Long {
        // Store old values
        return 200L + (oldText.length * 2L)
    }
    
    override fun getDescription(): String {
        return when {
            newText != null -> "Edit text"
            newX != null || newY != null -> "Move text"
            newSize != null -> "Resize text"
            newColor != null -> "Change text color"
            newRotation != null -> "Rotate text"
            else -> "Modify text"
        }
    }
}

