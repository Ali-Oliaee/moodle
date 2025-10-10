package com.example.moodle.ui.whiteboard

import android.graphics.*
import android.util.Log

/**
 * Manages board sections for split whiteboard functionality
 * Each section has its own drawing area and tools state
 */
class SectionManager {
    
    data class Section(
        val id: Int,
        val bounds: RectF,
        val studentName: String = "Student $id",
        var currentTool: WhiteboardView.DrawingTool = WhiteboardView.DrawingTool.PEN,
        var currentColor: Int = Color.BLACK,
        var currentStrokeWidth: Float = 8f,
        val paths: MutableList<WhiteboardView.DrawingPath> = mutableListOf(),
        val undoStack: MutableList<WhiteboardView.DrawingPath> = mutableListOf(),
        val textObjects: MutableList<WhiteboardView.TextObject> = mutableListOf(),
        var selectedTextObject: WhiteboardView.TextObject? = null
    ) {
        fun canUndo(): Boolean = paths.isNotEmpty()
        fun canRedo(): Boolean = undoStack.isNotEmpty()
        
        fun undo() {
            if (canUndo()) {
                val lastPath = paths.removeAt(paths.size - 1)
                undoStack.add(lastPath)
            }
        }
        
        fun redo() {
            if (canRedo()) {
                val pathToRedo = undoStack.removeAt(undoStack.size - 1)
                paths.add(pathToRedo)
            }
        }
        
        fun clear() {
            paths.clear()
            undoStack.clear()
            textObjects.clear()
            selectedTextObject = null
        }
        
        fun contains(x: Float, y: Float): Boolean {
            return bounds.contains(x, y)
        }
    }
    
    private var sections = mutableListOf<Section>()
    private var isSplitMode = false
    private var boardWidth = 0f
    private var boardHeight = 0f
    private var currentActiveSection: Section? = null
    
    // Paint for drawing section borders - solid divider lines
    private val sectionBorderPaint = Paint().apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        // Remove dashed effect for clear solid lines
    }
    
    // Paint for section labels
    private val sectionLabelPaint = Paint().apply {
        color = Color.BLUE
        textSize = 24f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    
    // Paint for section background (light tint to distinguish sections)
    private val sectionBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        alpha = 30 // Very light background
    }
    
    // Different background colors for sections
    private val sectionColors = arrayOf(
        Color.parseColor("#E3F2FD"), // Light Blue
        Color.parseColor("#E8F5E8"), // Light Green  
        Color.parseColor("#FFF3E0"), // Light Orange
        Color.parseColor("#F3E5F5"), // Light Purple
        Color.parseColor("#FFEBEE"), // Light Red
        Color.parseColor("#E0F2F1")  // Light Teal
    )
    
    fun setBoardDimensions(width: Float, height: Float) {
        boardWidth = width
        boardHeight = height
        if (isSplitMode) {
            recalculateSections()
        }
    }
    
    fun enableSplitMode(numberOfSections: Int) {
        if (numberOfSections < 2 || numberOfSections > 6) {
            Log.w("SectionManager", "Invalid number of sections: $numberOfSections. Must be between 2 and 6.")
            return
        }
        
        isSplitMode = true
        createSections(numberOfSections)
    }
    
    fun disableSplitMode() {
        isSplitMode = false
        sections.clear()
        currentActiveSection = null
    }
    
    private fun createSections(count: Int) {
        sections.clear()
        
        when (count) {
            2 -> create2Sections()
            3 -> create3Sections()
            4 -> create4Sections()
            6 -> create6Sections()
            else -> create2Sections() // Default fallback
        }
        
        Log.d("SectionManager", "Created $count sections")
    }
    
    private fun create2Sections() {
        val halfWidth = boardWidth / 2f
        
        sections.add(Section(
            id = 1,
            bounds = RectF(0f, 0f, halfWidth, boardHeight),
            studentName = "Student 1"
        ))
        
        sections.add(Section(
            id = 2,
            bounds = RectF(halfWidth, 0f, boardWidth, boardHeight),
            studentName = "Student 2"
        ))
    }
    
    private fun create3Sections() {
        val thirdWidth = boardWidth / 3f
        
        sections.add(Section(
            id = 1,
            bounds = RectF(0f, 0f, thirdWidth, boardHeight),
            studentName = "Student 1"
        ))
        
        sections.add(Section(
            id = 2,
            bounds = RectF(thirdWidth, 0f, thirdWidth * 2f, boardHeight),
            studentName = "Student 2"
        ))
        
        sections.add(Section(
            id = 3,
            bounds = RectF(thirdWidth * 2f, 0f, boardWidth, boardHeight),
            studentName = "Student 3"
        ))
    }
    
    private fun create4Sections() {
        val halfWidth = boardWidth / 2f
        val halfHeight = boardHeight / 2f
        
        sections.add(Section(
            id = 1,
            bounds = RectF(0f, 0f, halfWidth, halfHeight),
            studentName = "Student 1"
        ))
        
        sections.add(Section(
            id = 2,
            bounds = RectF(halfWidth, 0f, boardWidth, halfHeight),
            studentName = "Student 2"
        ))
        
        sections.add(Section(
            id = 3,
            bounds = RectF(0f, halfHeight, halfWidth, boardHeight),
            studentName = "Student 3"
        ))
        
        sections.add(Section(
            id = 4,
            bounds = RectF(halfWidth, halfHeight, boardWidth, boardHeight),
            studentName = "Student 4"
        ))
    }
    
    private fun create6Sections() {
        val thirdWidth = boardWidth / 3f
        val halfHeight = boardHeight / 2f
        
        sections.add(Section(
            id = 1,
            bounds = RectF(0f, 0f, thirdWidth, halfHeight),
            studentName = "Student 1"
        ))
        
        sections.add(Section(
            id = 2,
            bounds = RectF(thirdWidth, 0f, thirdWidth * 2f, halfHeight),
            studentName = "Student 2"
        ))
        
        sections.add(Section(
            id = 3,
            bounds = RectF(thirdWidth * 2f, 0f, boardWidth, halfHeight),
            studentName = "Student 3"
        ))
        
        sections.add(Section(
            id = 4,
            bounds = RectF(0f, halfHeight, thirdWidth, boardHeight),
            studentName = "Student 4"
        ))
        
        sections.add(Section(
            id = 5,
            bounds = RectF(thirdWidth, halfHeight, thirdWidth * 2f, boardHeight),
            studentName = "Student 5"
        ))
        
        sections.add(Section(
            id = 6,
            bounds = RectF(thirdWidth * 2f, halfHeight, boardWidth, boardHeight),
            studentName = "Student 6"
        ))
    }
    
    private fun recalculateSections() {
        if (sections.isNotEmpty()) {
            val count = sections.size
            createSections(count)
        }
    }
    
    fun getSectionAt(x: Float, y: Float): Section? {
        if (!isSplitMode) return null
        
        return sections.find { it.contains(x, y) }
    }
    
    fun setActiveSection(section: Section?) {
        currentActiveSection = section
    }
    
    fun getActiveSection(): Section? = currentActiveSection
    
    // Get section by touch coordinates - for concurrent drawing
    fun getSectionForDrawing(x: Float, y: Float): Section? {
        return getSectionAt(x, y)
    }
    
    fun getAllSections(): List<Section> = sections.toList()
    
    fun isSplitModeEnabled(): Boolean = isSplitMode
    
    fun drawSections(canvas: Canvas) {
        if (!isSplitMode) return
        
        for ((index, section) in sections.withIndex()) {
            // Draw light background for each section
            sectionBackgroundPaint.color = sectionColors[index % sectionColors.size]
            canvas.drawRect(section.bounds, sectionBackgroundPaint)
            
            // Draw section border (divider lines)
            canvas.drawRect(section.bounds, sectionBorderPaint)
            
            // Draw section label
            val centerX = section.bounds.centerX()
            val labelY = section.bounds.top + 35f
            canvas.drawText(section.studentName, centerX, labelY, sectionLabelPaint)
        }
    }
    
    fun clearAllSections() {
        sections.forEach { it.clear() }
    }
    
    fun clearSection(sectionId: Int) {
        sections.find { it.id == sectionId }?.clear()
    }
    
    // Convert global coordinates to section-local coordinates
    fun globalToLocal(section: Section, globalX: Float, globalY: Float): Pair<Float, Float> {
        return Pair(
            globalX - section.bounds.left,
            globalY - section.bounds.top
        )
    }
    
    // Convert section-local coordinates to global coordinates
    fun localToGlobal(section: Section, localX: Float, localY: Float): Pair<Float, Float> {
        return Pair(
            localX + section.bounds.left,
            localY + section.bounds.top
        )
    }
    
    // Check if a path is within section bounds
    fun isPathInSection(section: Section, path: Path): Boolean {
        val pathBounds = RectF()
        path.computeBounds(pathBounds, true)
        return section.bounds.contains(pathBounds)
    }
    
    // Clip a path to section bounds
    fun clipPathToSection(section: Section, path: Path): Path {
        val clippedPath = Path()
        val sectionPath = Path().apply {
            addRect(section.bounds, Path.Direction.CW)
        }
        
        clippedPath.op(path, sectionPath, Path.Op.INTERSECT)
        return clippedPath
    }
}
