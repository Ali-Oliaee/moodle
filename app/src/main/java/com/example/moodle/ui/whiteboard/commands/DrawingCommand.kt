package com.example.moodle.ui.whiteboard.commands

/**
 * Base interface for all drawing commands in the whiteboard
 * Implements the Command Pattern for efficient undo/redo operations
 * 
 * Each command encapsulates a single action and knows how to:
 * - Execute itself (apply the action)
 * - Undo itself (reverse the action)
 * - Report its memory usage
 */
interface DrawingCommand {
    /**
     * Execute this command (apply the action)
     */
    fun execute()
    
    /**
     * Undo this command (reverse the action)
     */
    fun undo()
    
    /**
     * Get estimated memory usage of this command in bytes
     * Used for memory management and optimization
     */
    fun getMemoryUsage(): Long
    
    /**
     * Get a human-readable description of this command
     * Used for debugging and undo history visualization
     */
    fun getDescription(): String
    
    /**
     * Get the timestamp when this command was created
     */
    fun getTimestamp(): Long = System.currentTimeMillis()
}

/**
 * Compound command that contains multiple sub-commands
 * Useful for grouping related actions (e.g., typing multiple characters)
 */
class CompoundCommand(
    private val commands: List<DrawingCommand>,
    private val description: String = "Compound Action"
) : DrawingCommand {
    
    override fun execute() {
        commands.forEach { it.execute() }
    }
    
    override fun undo() {
        // Undo in reverse order
        commands.reversed().forEach { it.undo() }
    }
    
    override fun getMemoryUsage(): Long {
        return commands.sumOf { it.getMemoryUsage() }
    }
    
    override fun getDescription(): String = description
    
    fun getCommandCount(): Int = commands.size
}

