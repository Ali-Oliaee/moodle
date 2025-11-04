package com.example.moodle.ui.whiteboard.managers
import com.example.moodle.ui.whiteboard.models.*
import com.example.moodle.ui.whiteboard.views.*
import com.example.moodle.ui.whiteboard.listeners.*
import com.example.moodle.ui.whiteboard.controllers.*
import com.example.moodle.ui.whiteboard.utils.*

import android.graphics.*

/**
 * Manages all canvas rendering operations for the whiteboard
 * Handles drawing paths, text, shapes, images, and background patterns
 */
class CanvasRenderManager(
    private val stateManager: DrawingStateManager,
    private val zoomPanController: ZoomPanController,
    private val paintHelper: PaintHelper,
    private val bitmapManager: BitmapManager,
    private val sectionManager: SectionManager
) {
    // Background pattern and color
    private var currentBackgroundPattern = BackgroundPattern.PLAIN
    private var currentBackgroundColor = BackgroundColor.WHITE
    private var backgroundImage: Bitmap? = null
    
    // Selection paint for selective eraser
    private val selectionPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
        alpha = 150
    }
    
    fun drawCanvas(canvas: Canvas, activePaths: Map<Int, Path>, selectionPath: Path?, previewPath: Path?, pointerSectionMap: Map<Int, SectionManager.Section>, currentActiveSection: SectionManager.Section?) {
        if (sectionManager.isSplitModeEnabled()) {
            drawSplitMode(canvas, activePaths, previewPath, pointerSectionMap, currentActiveSection)
        } else {
            drawNormalMode(canvas, activePaths, selectionPath, previewPath)
        }
    }
    
    private fun drawSplitMode(canvas: Canvas, activePaths: Map<Int, Path>, previewPath: Path?, pointerSectionMap: Map<Int, SectionManager.Section>, currentActiveSection: SectionManager.Section?) {
        // Draw white background (no patterns in split mode)
        canvas.drawColor(Color.WHITE)
        
        // Draw section backgrounds/borders first
        sectionManager.drawSections(canvas)
        
        // Draw section content
        for (section in sectionManager.getAllSections()) {
            canvas.save()
            canvas.clipRect(section.drawingBounds)
            
            // Draw section's paths
            for (drawingPath in section.paths) {
                if (drawingPath.tool == DrawingTool.TEXT && drawingPath.text != null) {
                    canvas.drawText(drawingPath.text, drawingPath.textX, drawingPath.textY, drawingPath.paint)
                } else {
                    canvas.drawPath(drawingPath.path, drawingPath.paint)
                }
            }
            
            // Draw section's text objects
            for (textObject in section.textObjects) {
                textObject.draw(canvas)
            }
            
            canvas.restore()
        }
        
        // Draw current active paths being drawn
        for ((pointerId, path) in activePaths) {
            if (!path.isEmpty) {
                val section = pointerSectionMap[pointerId]
                if (section != null) {
                    canvas.save()
                    canvas.clipRect(section.drawingBounds)
                    canvas.drawPath(path, paintHelper.getPaintForSection(section.currentTool, section.currentColor, section.currentStrokeWidth))
                    canvas.restore()
                }
            }
        }
        
        // Draw shape preview in split mode
        previewPath?.let { path ->
            currentActiveSection?.let { section ->
                canvas.save()
                canvas.clipRect(section.drawingBounds)
                canvas.drawPath(path, paintHelper.getPaintForSection(section.currentTool, section.currentColor, section.currentStrokeWidth))
                canvas.restore()
            }
        }
        
        // Draw section control toolbars on top
        drawSectionControls(canvas)
    }
    
    private fun drawNormalMode(canvas: Canvas, activePaths: Map<Int, Path>, selectionPath: Path?, previewPath: Path?) {
        canvas.save()
        
        // Apply transformations
        canvas.translate(-zoomPanController.focusX, -zoomPanController.focusY)
        canvas.scale(zoomPanController.scaleFactor, zoomPanController.scaleFactor)
        
        // Draw background color
        drawBackgroundColor(canvas)
        
        // Draw background pattern
        drawBackgroundPattern(canvas)
        
        // Draw the infinite bitmap with all previous strokes
        try {
            bitmapManager.drawingBitmap?.let { bitmap ->
                if (!bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0) {
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CanvasRenderManager", "Error drawing bitmap", e)
        }
        
        // Draw current active paths being drawn
        for ((_, path) in activePaths) {
            if (!path.isEmpty) {
                val paint = paintHelper.getCurrentPaint(stateManager.currentTool, stateManager.currentColor, stateManager.currentStrokeWidth)
                canvas.drawPath(path, paint)
            }
        }
        
        // Draw text objects
        for (textObject in stateManager.textObjects) {
            textObject.draw(canvas)
        }
        
        // Draw shape objects
        for (shapeObject in stateManager.shapeObjects) {
            shapeObject.draw(canvas)
        }
        
        // Draw image objects
        for (imageObject in stateManager.imageObjects) {
            imageObject.draw(canvas)
        }
        
        // Draw selection area for selective eraser
        selectionPath?.let { path ->
            canvas.drawPath(path, selectionPaint)
        }
        
        // Draw shape preview
        previewPath?.let { path ->
            val paint = paintHelper.getCurrentPaint(stateManager.currentTool, stateManager.currentColor, stateManager.currentStrokeWidth)
            canvas.drawPath(path, paint)
        }
        
        canvas.restore()
    }
    
    private fun drawBackgroundColor(canvas: Canvas) {
        val bitmapWidth = bitmapManager.drawingBitmap?.width?.toFloat() ?: 0f
        val bitmapHeight = bitmapManager.drawingBitmap?.height?.toFloat() ?: 0f
        
        if (bitmapWidth == 0f || bitmapHeight == 0f) return
        
        // Draw background image if available, otherwise draw color
        backgroundImage?.let { image ->
            if (!image.isRecycled) {
                val destRect = RectF(0f, 0f, bitmapWidth, bitmapHeight)
                canvas.drawBitmap(image, null, destRect, null)
                return
            }
        }
        
        // Draw background color if no image
        val backgroundPaint = Paint().apply {
            color = currentBackgroundColor.colorValue
            style = Paint.Style.FILL
        }
        
        canvas.drawRect(0f, 0f, bitmapWidth, bitmapHeight, backgroundPaint)
    }
    
    fun drawBackgroundPattern(canvas: Canvas) {
        if (currentBackgroundPattern == BackgroundPattern.PLAIN) {
            return
        }
        
        val patternPaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f / zoomPanController.scaleFactor
            style = Paint.Style.STROKE
        }
        
        val bitmapWidth = bitmapManager.drawingBitmap?.width?.toFloat() ?: 0f
        val bitmapHeight = bitmapManager.drawingBitmap?.height?.toFloat() ?: 0f
        
        if (bitmapWidth == 0f || bitmapHeight == 0f) return
        
        when (currentBackgroundPattern) {
            BackgroundPattern.GRID_SMALL -> drawGrid(canvas, patternPaint, 30f, bitmapWidth, bitmapHeight)
            BackgroundPattern.GRID_LARGE -> drawGrid(canvas, patternPaint, 60f, bitmapWidth, bitmapHeight)
            BackgroundPattern.LINES_HORIZONTAL -> drawHorizontalLines(canvas, patternPaint, 40f, bitmapWidth, bitmapHeight)
            BackgroundPattern.LINES_VERTICAL -> drawVerticalLines(canvas, patternPaint, 40f, bitmapWidth, bitmapHeight)
            BackgroundPattern.LINES_BOTH -> {
                drawHorizontalLines(canvas, patternPaint, 40f, bitmapWidth, bitmapHeight)
                drawVerticalLines(canvas, patternPaint, 40f, bitmapWidth, bitmapHeight)
            }
            BackgroundPattern.DOT_GRID -> drawDotGrid(canvas, patternPaint, 30f, bitmapWidth, bitmapHeight)
            BackgroundPattern.RULED_PAPER -> drawRuledPaper(canvas, 40f, bitmapWidth, bitmapHeight)
            BackgroundPattern.GRAPH_PAPER -> drawGraphPaper(canvas, bitmapWidth, bitmapHeight)
            BackgroundPattern.PLAIN -> {}
        }
    }
    
    private fun drawGrid(canvas: Canvas, paint: Paint, spacing: Float, width: Float, height: Float) {
        val adjustedSpacing = spacing / zoomPanController.scaleFactor
        
        // Vertical lines
        var x = 0f
        while (x <= width) {
            canvas.drawLine(x, 0f, x, height, paint)
            x += adjustedSpacing
        }
        
        // Horizontal lines
        var y = 0f
        while (y <= height) {
            canvas.drawLine(0f, y, width, y, paint)
            y += adjustedSpacing
        }
    }
    
    private fun drawHorizontalLines(canvas: Canvas, paint: Paint, spacing: Float, width: Float, height: Float) {
        val adjustedSpacing = spacing / zoomPanController.scaleFactor
        var y = 0f
        while (y <= height) {
            canvas.drawLine(0f, y, width, y, paint)
            y += adjustedSpacing
        }
    }
    
    private fun drawVerticalLines(canvas: Canvas, paint: Paint, spacing: Float, width: Float, height: Float) {
        val adjustedSpacing = spacing / zoomPanController.scaleFactor
        var x = 0f
        while (x <= width) {
            canvas.drawLine(x, 0f, x, height, paint)
            x += adjustedSpacing
        }
    }
    
    private fun drawDotGrid(canvas: Canvas, paint: Paint, spacing: Float, width: Float, height: Float) {
        val adjustedSpacing = spacing / zoomPanController.scaleFactor
        val dotRadius = 2f / zoomPanController.scaleFactor
        paint.style = Paint.Style.FILL
        
        var y = 0f
        while (y <= height) {
            var x = 0f
            while (x <= width) {
                canvas.drawCircle(x, y, dotRadius, paint)
                x += adjustedSpacing
            }
            y += adjustedSpacing
        }
    }
    
    private fun drawRuledPaper(canvas: Canvas, spacing: Float, width: Float, height: Float) {
        val adjustedSpacing = spacing / zoomPanController.scaleFactor
        val paint = Paint().apply {
            color = Color.parseColor("#B0BEC5")
            strokeWidth = 1f / zoomPanController.scaleFactor
            style = Paint.Style.STROKE
        }
        
        var y = 0f
        while (y <= height) {
            canvas.drawLine(0f, y, width, y, paint)
            y += adjustedSpacing
        }
    }
    
    private fun drawGraphPaper(canvas: Canvas, width: Float, height: Float) {
        val smallSpacing = 20f / zoomPanController.scaleFactor
        val largeSpacing = 100f / zoomPanController.scaleFactor
        
        // Small grid
        val smallPaint = Paint().apply {
            color = Color.parseColor("#EEEEEE")
            strokeWidth = 0.5f / zoomPanController.scaleFactor
            style = Paint.Style.STROKE
        }
        
        var x = 0f
        while (x <= width) {
            canvas.drawLine(x, 0f, x, height, smallPaint)
            x += smallSpacing
        }
        var y = 0f
        while (y <= height) {
            canvas.drawLine(0f, y, width, y, smallPaint)
            y += smallSpacing
        }
        
        // Large grid
        val largePaint = Paint().apply {
            color = Color.parseColor("#BDBDBD")
            strokeWidth = 1.5f / zoomPanController.scaleFactor
            style = Paint.Style.STROKE
        }
        
        x = 0f
        while (x <= width) {
            canvas.drawLine(x, 0f, x, height, largePaint)
            x += largeSpacing
        }
        y = 0f
        while (y <= height) {
            canvas.drawLine(0f, y, width, y, largePaint)
            y += largeSpacing
        }
    }
    
    private fun drawSectionControls(canvas: Canvas) {
        val buttonBg = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.parseColor("#F5F5F5")
        }
        
        val selectedBg = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.parseColor("#E0E0E0")
        }
        
        val borderPaint = Paint().apply {
            color = Color.parseColor("#CCCCCC")
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        
        // Define the 4 colors
        val colorBlack = Color.BLACK
        val colorBlue = Color.parseColor("#2196F3")
        val colorGreen = Color.parseColor("#4CAF50")
        val colorRed = Color.parseColor("#F44336")
        
        for (section in sectionManager.getAllSections()) {
            val controlArea = section.controlBounds
            val sectionWidth = section.bounds.width()
            
            val buttonSize = 50f
            val buttonSpacing = 6f
            val buttonCount = 7  // pen, eraser, clear, black, blue, green, red
            
            val totalWidth = buttonCount * buttonSize + (buttonCount - 1) * buttonSpacing
            val startX = controlArea.left + (sectionWidth - totalWidth) / 2
            val centerY = controlArea.centerY()
            val buttonTop = centerY - buttonSize / 2
            
            for (i in 0 until buttonCount) {
                val buttonLeft = startX + i * (buttonSize + buttonSpacing)
                val buttonRight = buttonLeft + buttonSize
                val buttonBottom = buttonTop + buttonSize
                val rect = RectF(buttonLeft, buttonTop, buttonRight, buttonBottom)
                
                val isSelected = when (i) {
                    0 -> section.currentTool == DrawingTool.PEN
                    1 -> section.currentTool == DrawingTool.ERASER
                    3 -> section.currentColor == colorBlack
                    4 -> section.currentColor == colorBlue
                    5 -> section.currentColor == colorGreen
                    6 -> section.currentColor == colorRed
                    else -> false
                }
                
                // Draw button background
                if (isSelected && i >= 3) {
                    // For color buttons, use a thicker border when selected
                    canvas.drawRoundRect(rect, 4f, 4f, buttonBg)
                } else if (isSelected) {
                    canvas.drawRoundRect(rect, 4f, 4f, selectedBg)
                } else {
                    canvas.drawRoundRect(rect, 4f, 4f, buttonBg)
                }
                canvas.drawRoundRect(rect, 4f, 4f, borderPaint)
                
                // Draw button icon/content
                when (i) {
                    0 -> drawIcon(canvas, "✎", rect, isSelected)
                    1 -> drawIcon(canvas, "⌫", rect, isSelected)
                    2 -> drawIcon(canvas, "✕", rect, true)
                    3 -> drawColorButton(canvas, rect, colorBlack, isSelected)
                    4 -> drawColorButton(canvas, rect, colorBlue, isSelected)
                    5 -> drawColorButton(canvas, rect, colorGreen, isSelected)
                    6 -> drawColorButton(canvas, rect, colorRed, isSelected)
                }
            }
        }
    }
    
    private fun drawIcon(canvas: Canvas, icon: String, rect: RectF, enabled: Boolean) {
        val paint = Paint().apply {
            isAntiAlias = true
            color = if (enabled) Color.parseColor("#333333") else Color.parseColor("#AAAAAA")
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText(icon, rect.centerX(), rect.centerY() + 10f, paint)
    }
    
    private fun drawColorButton(canvas: Canvas, rect: RectF, color: Int, isSelected: Boolean = false) {
        val colorPaint = Paint().apply {
            style = Paint.Style.FILL
            this.color = color
            isAntiAlias = true
        }
        val innerRect = RectF(rect.left + 8f, rect.top + 8f, rect.right - 8f, rect.bottom - 8f)
        canvas.drawRoundRect(innerRect, 4f, 4f, colorPaint)
        
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            this.color = if (isSelected) Color.parseColor("#333333") else Color.parseColor("#888888")
            strokeWidth = if (isSelected) 3.5f else 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(innerRect, 4f, 4f, borderPaint)
    }
    
    // Method to detect which button was clicked in section controls
    fun detectSectionControlButton(section: SectionManager.Section, x: Float, y: Float): Int? {
        val controlArea = section.controlBounds
        val sectionWidth = section.bounds.width()
        
        val buttonSize = 50f
        val buttonSpacing = 6f
        val buttonCount = 7
        
        val totalWidth = buttonCount * buttonSize + (buttonCount - 1) * buttonSpacing
        val startX = controlArea.left + (sectionWidth - totalWidth) / 2
        val centerY = controlArea.centerY()
        val buttonTop = centerY - buttonSize / 2
        
        for (i in 0 until buttonCount) {
            val buttonLeft = startX + i * (buttonSize + buttonSpacing)
            val buttonRight = buttonLeft + buttonSize
            val buttonBottom = buttonTop + buttonSize
            val rect = RectF(buttonLeft, buttonTop, buttonRight, buttonBottom)
            
            if (rect.contains(x, y)) {
                return i  // Return button index: 0=pen, 1=eraser, 2=clear, 3=black, 4=blue, 5=green, 6=red
            }
        }
        
        return null
    }
    
    fun setBackgroundPattern(pattern: BackgroundPattern) {
        currentBackgroundPattern = pattern
    }
    
    fun getBackgroundPattern(): BackgroundPattern = currentBackgroundPattern
    
    fun setBackgroundColor(color: BackgroundColor) {
        currentBackgroundColor = color
    }
    
    fun getBackgroundColor(): BackgroundColor = currentBackgroundColor
    
    fun setBackgroundImage(bitmap: Bitmap?) {
        backgroundImage = bitmap
    }
    
    fun getBackgroundImage(): Bitmap? = backgroundImage
    
    fun redrawCanvas() {
        bitmapManager.drawingCanvas?.let { canvas ->
            canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC)
            drawBackgroundColor(canvas)
            drawBackgroundPattern(canvas)
            
            for (drawingPath in stateManager.paths) {
                if (drawingPath.tool == DrawingTool.TEXT && drawingPath.text != null) {
                    canvas.drawText(drawingPath.text, drawingPath.textX, drawingPath.textY, drawingPath.paint)
                } else {
                    canvas.drawPath(drawingPath.path, drawingPath.paint)
                }
            }
        }
    }
}

