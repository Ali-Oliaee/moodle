package com.example.moodle.ui.whiteboard.managers
import com.example.moodle.ui.whiteboard.models.*

import android.graphics.*

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

        // Tool state for this section
        var currentTool: DrawingTool = DrawingTool.PEN,
        var currentColor: Int = Color.BLACK,
        var currentStrokeWidth: Float = 8f,
        
        // Drawing data for this section
        val paths: MutableList<DrawingPath> = mutableListOf(),
        val textObjects: MutableList<TextObject> = mutableListOf(),
        var selectedTextObject: TextObject? = null
    ) {

        fun clear() {
            paths.clear()
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

    
    // Paint for section background (light tint to distinguish sections)
    private val sectionBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        alpha = 30 // Very light background
    }
    
    fun setBoardDimensions(width: Float, height: Float) {
        boardWidth = width
        boardHeight = height
        if (isSplitMode) recalculateSections()
    }
    
    fun enableSplitMode(numberOfSections: Int) {
        if (numberOfSections < 2 || numberOfSections > 6) return

        if (boardWidth == 0f || boardHeight == 0f) return

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
        
        if (count < 4 ) {
            val width = boardWidth / count.toFloat()

            for (i in 1..count) {
                sections.add(Section(
                    id = i,
                    bounds = RectF((i - 1) * width, 0f, i * width, boardHeight),
                    drawingBounds = RectF((i - 1) * width, 0f, i * width, boardHeight - CONTROL_TOOLBAR_HEIGHT),
                    controlBounds = RectF((i - 1) * width, boardHeight - CONTROL_TOOLBAR_HEIGHT, i * width, boardHeight),
                ))
            }
        }
        else if (count == 4) create4Sections()
        else create6Sections()
    }
    
    private fun create4Sections() {
        val halfWidth = boardWidth / 2f
        val halfHeight = boardHeight / 2f
        
        sections.add(Section(
            id = 1,
            bounds = RectF(0f, 0f, halfWidth, halfHeight),
            drawingBounds = RectF(0f, 0f, halfWidth, halfHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(0f, halfHeight - CONTROL_TOOLBAR_HEIGHT, halfWidth, halfHeight),
        ))
        
        sections.add(Section(
            id = 2,
            bounds = RectF(halfWidth, 0f, boardWidth, halfHeight),
            drawingBounds = RectF(halfWidth, 0f, boardWidth, halfHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(halfWidth, halfHeight - CONTROL_TOOLBAR_HEIGHT, boardWidth, halfHeight),
        ))
        
        sections.add(Section(
            id = 3,
            bounds = RectF(0f, halfHeight, halfWidth, boardHeight),
            drawingBounds = RectF(0f, halfHeight, halfWidth, boardHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(0f, boardHeight - CONTROL_TOOLBAR_HEIGHT, halfWidth, boardHeight),
        ))
        
        sections.add(Section(
            id = 4,
            bounds = RectF(halfWidth, halfHeight, boardWidth, boardHeight),
            drawingBounds = RectF(halfWidth, halfHeight, boardWidth, boardHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(halfWidth, boardHeight - CONTROL_TOOLBAR_HEIGHT, boardWidth, boardHeight),
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
        ))
        
        sections.add(Section(
            id = 2,
            bounds = RectF(thirdWidth, 0f, thirdWidth * 2f, halfHeight),
            drawingBounds = RectF(thirdWidth, 0f, thirdWidth * 2f, halfHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(thirdWidth, halfHeight - CONTROL_TOOLBAR_HEIGHT, thirdWidth * 2f, halfHeight),
        ))
        
        sections.add(Section(
            id = 3,
            bounds = RectF(thirdWidth * 2f, 0f, boardWidth, halfHeight),
            drawingBounds = RectF(thirdWidth * 2f, 0f, boardWidth, halfHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(thirdWidth * 2f, halfHeight - CONTROL_TOOLBAR_HEIGHT, boardWidth, halfHeight),
        ))
        
        sections.add(Section(
            id = 4,
            bounds = RectF(0f, halfHeight, thirdWidth, boardHeight),
            drawingBounds = RectF(0f, halfHeight, thirdWidth, boardHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(0f, boardHeight - CONTROL_TOOLBAR_HEIGHT, thirdWidth, boardHeight),
        ))
        
        sections.add(Section(
            id = 5,
            bounds = RectF(thirdWidth, halfHeight, thirdWidth * 2f, boardHeight),
            drawingBounds = RectF(thirdWidth, halfHeight, thirdWidth * 2f, boardHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(thirdWidth, boardHeight - CONTROL_TOOLBAR_HEIGHT, thirdWidth * 2f, boardHeight),
        ))
        
        sections.add(Section(
            id = 6,
            bounds = RectF(thirdWidth * 2f, halfHeight, boardWidth, boardHeight),
            drawingBounds = RectF(thirdWidth * 2f, halfHeight, boardWidth, boardHeight - CONTROL_TOOLBAR_HEIGHT),
            controlBounds = RectF(thirdWidth * 2f, boardHeight - CONTROL_TOOLBAR_HEIGHT, boardWidth, boardHeight),
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
}
