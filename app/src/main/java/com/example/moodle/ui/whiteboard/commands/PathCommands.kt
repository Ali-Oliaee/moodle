package com.example.moodle.ui.whiteboard.commands

import android.graphics.Canvas
import com.example.moodle.ui.whiteboard.managers.DrawingStateManager
import com.example.moodle.ui.whiteboard.managers.BitmapManager
import com.example.moodle.ui.whiteboard.models.DrawingPath

/**
 * Command to add a drawing path to the canvas
 */
class AddPathCommand(
    private val stateManager: DrawingStateManager,
    private val bitmapManager: BitmapManager,
    private val path: DrawingPath
) : DrawingCommand {
    
    override fun execute() {
        stateManager.paths.add(path)
        
        // Draw path to bitmap
        bitmapManager.drawingBitmap?.let { bitmap ->
            if (!bitmap.isRecycled) {
                val canvas = Canvas(bitmap)
                canvas.drawPath(path.path, path.paint)
            }
        }
    }
    
    override fun undo() {
        stateManager.paths.remove(path)
        
        // Note: For bitmap, we need to redraw all paths
        // This is handled by the RenderManager in the view
    }
    
    override fun getMemoryUsage(): Long {
        // Rough estimate: Path object ~500 bytes, Paint ~300 bytes
        return 800L
    }
    
    override fun getDescription(): String {
        return "Draw ${path.tool.name.lowercase()}"
    }
}

/**
 * Command to remove a specific path from the canvas
 */
class RemovePathCommand(
    private val stateManager: DrawingStateManager,
    private val path: DrawingPath
) : DrawingCommand {
    
    private var removedIndex: Int = -1
    
    override fun execute() {
        removedIndex = stateManager.paths.indexOf(path)
        if (removedIndex >= 0) {
            stateManager.paths.removeAt(removedIndex)
        }
    }
    
    override fun undo() {
        if (removedIndex >= 0) {
            stateManager.paths.add(removedIndex, path)
        }
    }
    
    override fun getMemoryUsage(): Long {
        return 800L + 8L // Path + index storage
    }
    
    override fun getDescription(): String {
        return "Remove path"
    }
}

/**
 * Command to add multiple paths at once (optimization for batch operations)
 */
class AddPathsBatchCommand(
    private val stateManager: DrawingStateManager,
    private val bitmapManager: BitmapManager,
    private val paths: List<DrawingPath>
) : DrawingCommand {
    
    override fun execute() {
        stateManager.paths.addAll(paths)
        
        // Draw all paths to bitmap
        bitmapManager.drawingBitmap?.let { bitmap ->
            if (!bitmap.isRecycled) {
                val canvas = Canvas(bitmap)
                paths.forEach { path ->
                    canvas.drawPath(path.path, path.paint)
                }
            }
        }
    }
    
    override fun undo() {
        paths.forEach { path ->
            stateManager.paths.remove(path)
        }
    }
    
    override fun getMemoryUsage(): Long {
        return paths.size * 800L
    }
    
    override fun getDescription(): String {
        return "Draw ${paths.size} strokes"
    }
}

