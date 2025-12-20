package com.example.moodle.ui.whiteboard.managers

import android.util.Log
import com.example.moodle.ui.whiteboard.commands.DrawingCommand
import java.util.*

/**
 * Manages command-based undo/redo functionality
 */
class CommandManager {
    
    companion object {
        private const val TAG = "CommandManager"
    }
    
    // Command stacks
    private val undoStack = Stack<DrawingCommand>()
    private val redoStack = Stack<DrawingCommand>()
    
    // Configuration
    private var maxStackSize = 100
    private var isEnabled = true
    
    // Batching support
    private var isBatching = false
    private val batchedCommands = mutableListOf<DrawingCommand>()

    // Callbacks
    var onStackChanged: ((canUndo: Boolean, canRedo: Boolean) -> Unit)? = null
    var onCommandExecuted: ((command: DrawingCommand) -> Unit)? = null
    
    /**
     * Execute a command and add it to the undo stack
     * If batching is active, adds to batch instead
     */
    fun execute(command: DrawingCommand) {
        if (!isEnabled) {
            command.execute()
            return
        }
        
        try {
            // Execute the command
            command.execute()
            
            if (isBatching) {
                // Add to batch
                batchedCommands.add(command)
                Log.d(TAG, "Batched: ${command.getDescription()} (${batchedCommands.size} in batch)")
            } else {
                // Add to undo stack
                undoStack.push(command)
                
                // Clear redo stack when new command is executed
                redoStack.clear()
                
                // Enforce max stack size
                enforceStackSize()
                
                // Notify listeners AFTER stack is updated
                notifyStackChanged()
                onCommandExecuted?.invoke(command)
                
                Log.d(TAG, "Executed: ${command.getDescription()} (Undo: ${undoStack.size}, Redo: ${redoStack.size})")
            }
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory when executing command", e)
            clearOldCommands(10)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command: ${command.getDescription()}", e)
        }
    }
    
    /**
     * Undo the last command
     * @return true if undo was successful, false if nothing to undo
     */
    fun undo(): Boolean {
        if (!canUndo()) return false
        
        try {
            val command = undoStack.pop()
            command.undo()
            redoStack.push(command)
            
            notifyStackChanged()
            
            Log.d(TAG, "Undone: ${command.getDescription()} (Stack: ${undoStack.size})")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error during undo", e)
            return false
        }
    }
    
    /**
     * Redo the last undone command
     * @return true if redo was successful, false if nothing to redo
     */
    fun redo(): Boolean {
        if (!canRedo()) return false
        
        try {
            val command = redoStack.pop()
            command.execute()
            undoStack.push(command)
            
            notifyStackChanged()
            
            Log.d(TAG, "Redone: ${command.getDescription()} (Stack: ${undoStack.size})")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error during redo", e)
            return false
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
        undoStack.clear()
        redoStack.clear()
        notifyStackChanged()
        Log.d(TAG, "Command history cleared")
    }


    /**
     * Cancel current batch without creating a compound command
     */
    fun cancelBatch() {
        isBatching = false
        batchedCommands.clear()
        Log.d(TAG, "Batch cancelled")
    }

    /**
     * Get total memory usage of all commands
     */
    fun getTotalMemoryUsage(): Long {
        val undoMemory = undoStack.sumOf { it.getMemoryUsage() }
        val redoMemory = redoStack.sumOf { it.getMemoryUsage() }
        return undoMemory + redoMemory
    }

    /**
     * Enforce maximum stack size
     */
    private fun enforceStackSize() {
        while (undoStack.size > maxStackSize) {
            undoStack.removeAt(0)
        }
    }
    
    /**
     * Clear oldest commands to free memory
     */
    private fun clearOldCommands(count: Int) {
        var cleared = 0
        while (cleared < count && undoStack.isNotEmpty()) {
            undoStack.removeAt(0)
            cleared++
        }
        notifyStackChanged()
        Log.d(TAG, "Cleared $cleared old commands to free memory")
    }
    
    /**
     * Notify listeners about stack changes
     */
    private fun notifyStackChanged() {
        onStackChanged?.invoke(canUndo(), canRedo())
    }
    
    /**
     * Manually trigger stack changed notification
     */
    fun updateButtonStates() {
        notifyStackChanged()
    }
}

