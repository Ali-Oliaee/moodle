package com.example.moodle.ui.whiteboard.managers

import android.graphics.Bitmap
import android.util.Log
import com.example.moodle.ui.whiteboard.models.*
import java.util.*

/**
 * Manages undo/redo functionality by storing bitmap diffs and state snapshots
 * Uses incremental diffs to store only changed regions for efficient memory usage
 */
class UndoRedoManager {
    
    // Undo/redo stacks
    private val undoStack = Stack<DrawingSnapshot>()
    private val redoStack = Stack<DrawingSnapshot>()
    
    // Bitmap diff manager
    private val diffManager = BitmapDiffManager()
    
    // Base bitmap for diff calculations
    private var baseBitmap: Bitmap? = null
    
    // Previous bitmap for diff calculation
    private var previousBitmap: Bitmap? = null
    
    // Configuration
    private var maxStackSize = 50
    private var isEnabled = true
    private var useIncrementalDiffs = true
    
    // Callbacks
    var onStackChanged: ((canUndo: Boolean, canRedo: Boolean) -> Unit)? = null
    
    /**
     * Represents a snapshot of the drawing state with incremental bitmap diff
     */
    data class DrawingSnapshot(
        val bitmapDiff: BitmapDiffManager.BitmapDiff?,
        val paths: List<DrawingPath>,
        val textObjects: List<TextObject>,
        val shapeObjects: List<ShapeObject>,
        val imageObjects: List<ImageObject>,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        /**
         * Get estimated memory usage
         */
        fun getMemoryUsage(): Long {
            var total = 0L
            total += bitmapDiff?.getMemoryUsage() ?: 0L
            // Add rough estimate for objects (each object ~200 bytes average)
            total += (paths.size + textObjects.size + shapeObjects.size + imageObjects.size) * 200L
            return total
        }
    }
    
    /**
     * Save current state to undo stack with incremental diff
     */
    fun saveState(
        bitmap: Bitmap?,
        paths: List<DrawingPath>,
        textObjects: List<TextObject>,
        shapeObjects: List<ShapeObject>,
        imageObjects: List<ImageObject>,
    ) {
        if (!isEnabled) return
        
        try {
            // Calculate bitmap diff
            val bitmapDiff = if (useIncrementalDiffs && bitmap != null) {
                val diff = diffManager.calculateDiff(previousBitmap, bitmap)
                
                // Update previous bitmap for next diff
                previousBitmap?.recycle()
                previousBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                
                // Store base bitmap if this is the first state
                if (baseBitmap == null && undoStack.isEmpty()) {
                    baseBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                }
                
                diff
            } else {
                null
            }
            
            // Create deep copies of objects
            val pathsCopy = paths.map { it.copy() }
            val textObjectsCopy = textObjects.map { it.copy() }
            val shapeObjectsCopy = shapeObjects.map { it.copy() }
            val imageObjectsCopy = imageObjects.map { it.copy() }
            
            val snapshot = DrawingSnapshot(
                bitmapDiff = bitmapDiff,
                paths = pathsCopy,
                textObjects = textObjectsCopy,
                shapeObjects = shapeObjectsCopy,
                imageObjects = imageObjectsCopy,
            )
            
            // Add to undo stack
            undoStack.push(snapshot)
            
            // Clear redo stack when new action is performed
            clearRedoStack()
            
            // Enforce max stack size
            if (undoStack.size > maxStackSize) {
                val removed = undoStack.removeAt(0)
                // No need to recycle bitmaps as we're using diffs
            }
            
            notifyStackChanged()
        } catch (e: OutOfMemoryError) {
            Log.e("UndoRedoManager", "Out of memory when saving state", e)
            // Clear some old states to free memory
            clearOldStates(5)
        } catch (e: Exception) {
            Log.e("UndoRedoManager", "Error saving state", e)
        }
    }
    
    /**
     * Undo the last action
     * Returns the previous state or null if no undo available
     */
    fun undo(): DrawingSnapshot? {
        if (!canUndo()) return null
        
        try {
            val currentState = undoStack.pop()
            redoStack.push(currentState)
            
            notifyStackChanged()
            
            // Return the new current state (top of undo stack) or null if stack is empty
            val resultSnapshot = if (undoStack.isNotEmpty()) {
                undoStack.peek()
            } else {
                // Return empty state
                createEmptySnapshot()
            }
            
            Log.d("UndoRedoManager", "Undo: Stack size ${undoStack.size}, Redo size ${redoStack.size}")
            return resultSnapshot
        } catch (e: Exception) {
            Log.e("UndoRedoManager", "Error during undo", e)
            return null
        }
    }
    
    /**
     * Redo the last undone action
     * Returns the restored state or null if no redo available
     */
    fun redo(): DrawingSnapshot? {
        if (!canRedo()) return null
        
        try {
            val stateToRestore = redoStack.pop()
            undoStack.push(stateToRestore)
            
            notifyStackChanged()
            
            Log.d("UndoRedoManager", "Redo: Stack size ${undoStack.size}, Redo size ${redoStack.size}")
            return stateToRestore
        } catch (e: Exception) {
            Log.e("UndoRedoManager", "Error during redo", e)
            return null
        }
    }
    
    /**
     * Check if undo is available
     */
    fun canUndo(): Boolean = undoStack.isNotEmpty()
    
    /**
     * Check if redo is available
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    
    /**
     * Clear all undo/redo history
     */
    fun clear() {
        clearUndoStack()
        clearRedoStack()
        baseBitmap?.recycle()
        baseBitmap = null
        previousBitmap?.recycle()
        previousBitmap = null
        notifyStackChanged()
    }
    
    /**
     * Clear undo stack
     */
    private fun clearUndoStack() {
        undoStack.clear()
    }
    
    /**
     * Clear redo stack
     */
    private fun clearRedoStack() {
        redoStack.clear()
    }
    
    /**
     * Clear oldest states to free memory
     */
    private fun clearOldStates(count: Int) {
        var cleared = 0
        while (cleared < count && undoStack.isNotEmpty()) {
            undoStack.removeAt(0)
            cleared++
        }
        notifyStackChanged()
    }
    
    /**
     * Create an empty snapshot for initial state
     */
    private fun createEmptySnapshot(): DrawingSnapshot {
        return DrawingSnapshot(
            bitmapDiff = null,
            paths = emptyList(),
            textObjects = emptyList(),
            shapeObjects = emptyList(),
            imageObjects = emptyList(),
        )
    }
    
    /**
     * Reconstruct bitmap from base and applying diffs up to a specific snapshot
     */
    fun reconstructBitmap(snapshot: DrawingSnapshot): Bitmap? {
        if (!useIncrementalDiffs) return null
        
        val diff = snapshot.bitmapDiff ?: return null
        
        // Apply diff to base bitmap
        return diffManager.applyDiff(baseBitmap, diff)
    }
    
    /**
     * Set maximum stack size
     */
    fun setMaxStackSize(size: Int) {
        maxStackSize = size.coerceIn(10, 100)
    }
    
    /**
     * Enable or disable undo/redo functionality
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    /**
     * Enable or disable incremental diffs (vs full bitmap storage)
     */
    fun setIncrementalDiffsEnabled(enabled: Boolean) {
        useIncrementalDiffs = enabled
        if (!enabled) {
            // Clear base and previous bitmaps
            baseBitmap?.recycle()
            baseBitmap = null
            previousBitmap?.recycle()
            previousBitmap = null
        }
    }
    
    /**
     * Check if incremental diffs are enabled
     */
    fun isIncrementalDiffsEnabled(): Boolean = useIncrementalDiffs
    
    /**
     * Set performance mode for diff calculation
     */
    fun setDiffPerformanceMode(mode: BitmapDiffManager.PerformanceMode) {
        diffManager.setPerformanceMode(mode)
    }
    
    /**
     * Notify listeners about stack changes
     */
    private fun notifyStackChanged() {
        onStackChanged?.invoke(canUndo(), canRedo())
    }
    
    /**
     * Manually trigger stack changed notification
     * Useful when switching to this manager to update UI button states
     */
    fun updateButtonStates() {
        notifyStackChanged()
    }
}

