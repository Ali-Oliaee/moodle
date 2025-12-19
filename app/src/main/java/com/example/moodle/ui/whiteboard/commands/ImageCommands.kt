package com.example.moodle.ui.whiteboard.commands

import com.example.moodle.ui.whiteboard.managers.DrawingStateManager
import com.example.moodle.ui.whiteboard.models.ImageObject

/**
 * Command to add an image object to the canvas
 */
class AddImageCommand(
    private val stateManager: DrawingStateManager,
    private val imageObject: ImageObject
) : DrawingCommand {
    
    override fun execute() {
        stateManager.imageObjects.add(imageObject)
    }
    
    override fun undo() {
        stateManager.imageObjects.remove(imageObject)
    }

    override fun getMemoryUsage(): Long {
        // ImageObject + bitmap estimate
        val bitmapSize = if (imageObject.bitmap != null) {
            imageObject.bitmap.byteCount.toLong()
        } else {
            0L
        }
        return 500L + bitmapSize
    }

    override fun getDescription(): String {
        return "Add image"
    }
}

/**
 * Command to remove an image object from the canvas
 */
class RemoveImageCommand(
    private val stateManager: DrawingStateManager,
    private val imageObject: ImageObject
) : DrawingCommand {
    
    private var removedIndex: Int = -1
    
    override fun execute() {
        removedIndex = stateManager.imageObjects.indexOf(imageObject)
        if (removedIndex >= 0) {
            stateManager.imageObjects.removeAt(removedIndex)
        }
    }
    
    override fun undo() {
        if (removedIndex >= 0) {
            stateManager.imageObjects.add(removedIndex, imageObject)
        }
    }
    
    override fun getMemoryUsage(): Long {
        val bitmapSize = if (imageObject.bitmap != null) {
            imageObject.bitmap.byteCount.toLong()
        } else {
            0L
        }
        return 500L + bitmapSize + 8L
    }
    
    override fun getDescription(): String {
        return "Remove image"
    }
}

/**
 * Command to modify an image object's properties
 */
class ModifyImageCommand(
    private val imageObject: ImageObject,
    private val newX: Float? = null,
    private val newY: Float? = null,
    private val newWidth: Float? = null,
    private val newHeight: Float? = null,
    private val newRotation: Float? = null
) : DrawingCommand {
    
    private val oldX = imageObject.x
    private val oldY = imageObject.y
    private val oldWidth = imageObject.width
    private val oldHeight = imageObject.height
    private val oldRotation = imageObject.rotation
    
    override fun execute() {
        newX?.let { imageObject.x = it }
        newY?.let { imageObject.y = it }
        newWidth?.let { imageObject.width = it }
        newHeight?.let { imageObject.height = it }
        newRotation?.let { imageObject.rotation = it }
    }
    
    override fun undo() {
        imageObject.x = oldX
        imageObject.y = oldY
        imageObject.width = oldWidth
        imageObject.height = oldHeight
        imageObject.rotation = oldRotation
    }

    override fun getMemoryUsage(): Long {
        return 200L
    }
    
    override fun getDescription(): String {
        return when {
            newX != null || newY != null -> "Move image"
            newWidth != null || newHeight != null -> "Resize image"
            newRotation != null -> "Rotate image"
            else -> "Modify image"
        }
    }
}

