package com.example.moodle.ui.whiteboard.managers
import com.example.moodle.ui.whiteboard.models.*

import android.graphics.*
import android.util.Log

/**
 * Manages board sections for split whiteboard functionality
 * Each section has its own drawing area and independent tools
 * 
 * Layout: Each section has controls at the bottom with:
 * - Pen, Eraser, Clear, and 4 color options (Black, Blue, Green, Red)
 */
class SectionManager {
    
    companion object {
        // Height reserved for control toolbar at bottom of each section
        // Matches the main whiteboard toolbar height
        const val CONTROL_TOOLBAR_HEIGHT = 60f
    }
    
    data class Section(
        val id: Int,
        val bounds: RectF,               // Total section bounds
        val drawingBounds: RectF,        // Area for drawing (top part)
        val controlBounds: RectF,        // Control toolbar at bottom
        val studentName: String = "Section $id",
        
        // Tool state for this section
        var currentTool: DrawingTool = DrawingTool.PEN,
        var currentColor: Int = Color.BLACK,
        var currentStrokeWidth: Float = 8f,
        
        // Drawing data for this section
        val paths: MutableList<DrawingPath> = mutableListOf(),
        val undoStack: MutableList<DrawingPath> = mutableListOf(),
        val textObjects: MutableList<TextObject> = mutableListOf(),
        var selectedTextObject: TextObject? = null
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
        
        fun containsInDrawingArea(x: Float, y: Float): Boolean {
            return drawingBounds.contains(x, y)
        }
        
        fun containsInControlArea(x: Float, y: Float): Boolean {
            return controlBounds.contains(x, y)
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
    
    fun setBoardDimensions(width: Float, height: Float) {
        android.util.Log.d("SectionManager", "setBoardDimensions: $width x $height, isSplitMode=$isSplitMode")
        boardWidth = width
        boardHeight = height
        if (isSplitMode) {
            android.util.Log.d("SectionManager", "Recalculating sections...")
            recalculateSections()
        }
    }
    
    fun enableSplitMode(numberOfSections: Int) {
        android.util.Log.d("SectionManager", "enableSplitMode called with $numberOfSections sections, board: $boardWidth x $boardHeight")
        
        if (numberOfSections < 2 || numberOfSections > 6) {
            Log.w("SectionManager", "Invalid number of sections: $numberOfSections. Must be between 2 and 6.")
            return
        }
        
        if (boardWidth == 0f || boardHeight == 0f) {
            Log.e("SectionManager", "ERROR: Cannot enable split mode - board dimensions not set!")
            return
        }
        
        isSplitMode = true
        createSections(numberOfSections)
        
        // Log section bounds for debugging
        sections.forEach { section ->
            Log.d("SectionManager", "Section ${section.id}: bounds=${section.bounds}, drawing=${section.drawingBounds}, control=${section.controlBounds}")
        }
    }
    
    fun disableSplitMode() {
        isSplitMode = false
        sections.clear()
        currentActiveSection = null
    }
    
    private fun createSections(count: Int) {
        sections.clear()
        
        if (count < 4 ) {
            val width = boardWidth / count.toFloat()

            for (i in 1..count) {
                sections.add(Section(
                    id = i,
                    bounds = RectF((i - 1) * width, 0f, i * width, boardHeight),
                    drawingBounds = RectF((i - 1) * width, 0f, i * width, boardHeight - CONTROL_TOOLBAR_HEIGHT),
                    controlBounds = RectF((i - 1) * width, boardHeight - CONTROL_TOOLBAR_HEIGHT, i * width, boardHeight),
                    studentName = "Section $i"
                ))
            }
        }
        else if (count == 4) create4Sections()
        else create6Sections()
        
        Log.d("SectionManager", "Created $count sections")
    }
    
    private fun create4Sections() {
        val halfWidth = boardWidth / 2f
        val halfHeight = boardHeight / 2f
        
        sections.add(Section(
            id = 1,
            bounds = RectF(0f, 0f, halfWidth, halfHeight),
            drawingBounds = RectF(0f, 0f, halfWidth, halfHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(0f, halfHeight - CONTROL_TOOLBAR_HEIGHT, halfWidth, halfHeight),
            studentName = "Section 1"
        ))
        
        sections.add(Section(
            id = 2,
            bounds = RectF(halfWidth, 0f, boardWidth, halfHeight),
            drawingBounds = RectF(halfWidth, 0f, boardWidth, halfHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(halfWidth, halfHeight - CONTROL_TOOLBAR_HEIGHT, boardWidth, halfHeight),
            studentName = "Section 2"
        ))
        
        sections.add(Section(
            id = 3,
            bounds = RectF(0f, halfHeight, halfWidth, boardHeight),
            drawingBounds = RectF(0f, halfHeight, halfWidth, boardHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(0f, boardHeight - CONTROL_TOOLBAR_HEIGHT, halfWidth, boardHeight),
            studentName = "Section 3"
        ))
        
        sections.add(Section(
            id = 4,
            bounds = RectF(halfWidth, halfHeight, boardWidth, boardHeight),
            drawingBounds = RectF(halfWidth, halfHeight, boardWidth, boardHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(halfWidth, boardHeight - CONTROL_TOOLBAR_HEIGHT, boardWidth, boardHeight),
            studentName = "Section 4"
        ))
    }
    
    private fun create6Sections() {
        val thirdWidth = boardWidth / 3f
        val halfHeight = boardHeight / 2f
        
        sections.add(Section(
            id = 1,
            bounds = RectF(0f, 0f, thirdWidth, halfHeight),
            drawingBounds = RectF(0f, 0f, thirdWidth, halfHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(0f, halfHeight - CONTROL_TOOLBAR_HEIGHT, thirdWidth, halfHeight),
            studentName = "Section 1"
        ))
        
        sections.add(Section(
            id = 2,
            bounds = RectF(thirdWidth, 0f, thirdWidth * 2f, halfHeight),
            drawingBounds = RectF(thirdWidth, 0f, thirdWidth * 2f, halfHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(thirdWidth, halfHeight - CONTROL_TOOLBAR_HEIGHT, thirdWidth * 2f, halfHeight),
            studentName = "Section 2"
        ))
        
        sections.add(Section(
            id = 3,
            bounds = RectF(thirdWidth * 2f, 0f, boardWidth, halfHeight),
            drawingBounds = RectF(thirdWidth * 2f, 0f, boardWidth, halfHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(thirdWidth * 2f, halfHeight - CONTROL_TOOLBAR_HEIGHT, boardWidth, halfHeight),
            studentName = "Section 3"
        ))
        
        sections.add(Section(
            id = 4,
            bounds = RectF(0f, halfHeight, thirdWidth, boardHeight),
            drawingBounds = RectF(0f, halfHeight, thirdWidth, boardHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(0f, boardHeight - CONTROL_TOOLBAR_HEIGHT, thirdWidth, boardHeight),
            studentName = "Section 4"
        ))
        
        sections.add(Section(
            id = 5,
            bounds = RectF(thirdWidth, halfHeight, thirdWidth * 2f, boardHeight),
            drawingBounds = RectF(thirdWidth, halfHeight, thirdWidth * 2f, boardHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(thirdWidth, boardHeight - CONTROL_TOOLBAR_HEIGHT, thirdWidth * 2f, boardHeight),
            studentName = "Section 5"
        ))
        
        sections.add(Section(
            id = 6,
            bounds = RectF(thirdWidth * 2f, halfHeight, boardWidth, boardHeight),
            drawingBounds = RectF(thirdWidth * 2f, halfHeight, boardWidth, boardHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(thirdWidth * 2f, boardHeight - CONTROL_TOOLBAR_HEIGHT, boardWidth, boardHeight),
            studentName = "Section 6"
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
            // Draw white background for drawing area
            sectionBackgroundPaint.color = Color.WHITE
            canvas.drawRect(section.drawingBounds, sectionBackgroundPaint)

            // Draw light gray background for control toolbar
            sectionBackgroundPaint.color = Color.parseColor("#F5F5F5")
            canvas.drawRect(section.controlBounds, sectionBackgroundPaint)
            
            // Draw thin divider line between drawing area and control toolbar
            val dividerPaint = Paint().apply {
                color = Color.parseColor("#DDDDDD")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawLine(
                section.bounds.left,
                section.controlBounds.top,
                section.bounds.right,
                section.controlBounds.top,
                dividerPaint
            )
            
            // Draw vertical section borders (bold lines between sections)
            if (index > 0) {
                val borderPaint = Paint().apply {
                    color = Color.parseColor("#333333")
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                canvas.drawLine(
                    section.bounds.left,
                    0f,
                    section.bounds.left,
                    boardHeight,
                    borderPaint
                )
            }
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
