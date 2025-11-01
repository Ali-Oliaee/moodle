package com.example.moodle.ui.whiteboard.views
import com.example.moodle.ui.whiteboard.models.*
import com.example.moodle.ui.whiteboard.managers.*
import com.example.moodle.ui.whiteboard.controllers.*
import com.example.moodle.ui.whiteboard.listeners.*
import com.example.moodle.ui.whiteboard.utils.*

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class BackgroundPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var backgroundPattern = BackgroundPattern.PLAIN
    
    // Paint objects for preview
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#E0E0E0")
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }
    
    private val linePaint = Paint().apply {
        color = Color.parseColor("#D0D0D0")
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }
    
    private val dotPaint = Paint().apply {
        color = Color.parseColor("#C0C0C0")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun setBackgroundPattern(pattern: BackgroundPattern) {
        backgroundPattern = pattern
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        
        // Draw white background
        canvas.drawColor(Color.WHITE)
        
        // Draw border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#CCCCCC")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(0f, 0f, width, height, borderPaint)
        
        // Draw pattern preview
        when (backgroundPattern) {
            BackgroundPattern.PLAIN -> {
                // Already drawn white background
            }
            BackgroundPattern.GRID_SMALL -> {
                drawGridPreview(canvas, width, height, 8f)
            }
            BackgroundPattern.GRID_LARGE -> {
                drawGridPreview(canvas, width, height, 12f)
            }
            BackgroundPattern.LINES_HORIZONTAL -> {
                drawHorizontalLinesPreview(canvas, width, height, 8f)
            }
            BackgroundPattern.LINES_VERTICAL -> {
                drawVerticalLinesPreview(canvas, width, height, 8f)
            }
            BackgroundPattern.LINES_BOTH -> {
                drawHorizontalLinesPreview(canvas, width, height, 8f)
                drawVerticalLinesPreview(canvas, width, height, 8f)
            }
            BackgroundPattern.DOT_GRID -> {
                drawDotGridPreview(canvas, width, height, 8f)
            }
            BackgroundPattern.RULED_PAPER -> {
                drawRuledPaperPreview(canvas, width, height)
            }
            BackgroundPattern.GRAPH_PAPER -> {
                drawGraphPaperPreview(canvas, width, height)
            }
        }
    }
    
    private fun drawGridPreview(canvas: Canvas, width: Float, height: Float, spacing: Float) {
        // Draw vertical lines
        var x = spacing
        while (x < width) {
            canvas.drawLine(x, 2f, x, height - 2f, gridPaint)
            x += spacing
        }
        
        // Draw horizontal lines
        var y = spacing
        while (y < height) {
            canvas.drawLine(2f, y, width - 2f, y, gridPaint)
            y += spacing
        }
    }
    
    private fun drawHorizontalLinesPreview(canvas: Canvas, width: Float, height: Float, spacing: Float) {
        var y = spacing
        while (y < height) {
            canvas.drawLine(2f, y, width - 2f, y, linePaint)
            y += spacing
        }
    }
    
    private fun drawVerticalLinesPreview(canvas: Canvas, width: Float, height: Float, spacing: Float) {
        var x = spacing
        while (x < width) {
            canvas.drawLine(x, 2f, x, height - 2f, linePaint)
            x += spacing
        }
    }
    
    private fun drawDotGridPreview(canvas: Canvas, width: Float, height: Float, spacing: Float) {
        var y = spacing
        while (y < height) {
            var x = spacing
            while (x < width) {
                canvas.drawCircle(x, y, 1f, dotPaint)
                x += spacing
            }
            y += spacing
        }
    }
    
    private fun drawRuledPaperPreview(canvas: Canvas, width: Float, height: Float) {
        // Draw margin line
        val marginLeft = width * 0.25f
        canvas.drawLine(marginLeft, 2f, marginLeft, height - 2f, Paint().apply {
            color = Color.parseColor("#FF6B6B")
            strokeWidth = 1f
        })
        
        // Draw horizontal lines
        val lineSpacing = 8f
        var y = lineSpacing
        while (y < height) {
            canvas.drawLine(2f, y, width - 2f, y, linePaint)
            y += lineSpacing
        }
    }
    
    private fun drawGraphPaperPreview(canvas: Canvas, width: Float, height: Float) {
        val smallSpacing = 4f
        val largeSpacing = 12f
        
        // Draw small grid
        val lightGridPaint = Paint().apply {
            color = Color.parseColor("#F0F0F0")
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        
        // Small grid
        var x = smallSpacing
        while (x < width) {
            canvas.drawLine(x, 2f, x, height - 2f, lightGridPaint)
            x += smallSpacing
        }
        
        var y = smallSpacing
        while (y < height) {
            canvas.drawLine(2f, y, width - 2f, y, lightGridPaint)
            y += smallSpacing
        }
        
        // Large grid
        val darkGridPaint = Paint().apply {
            color = Color.parseColor("#D0D0D0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        
        x = largeSpacing
        while (x < width) {
            canvas.drawLine(x, 2f, x, height - 2f, darkGridPaint)
            x += largeSpacing
        }
        
        y = largeSpacing
        while (y < height) {
            canvas.drawLine(2f, y, width - 2f, y, darkGridPaint)
            y += largeSpacing
        }
    }
}

