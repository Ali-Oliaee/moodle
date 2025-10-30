package com.example.moodle.ui.whiteboard

import android.graphics.*

/**
 * Helper class for managing paint objects used in drawing
 */
class PaintHelper {
    
    val drawPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    
    val eraserPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    
    val textPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        textAlign = Paint.Align.LEFT
        textSize = 48f
    }
    
    val selectionPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
        alpha = 150
    }
    
    val backgroundPaint = Paint().apply {
        color = Color.WHITE
    }
    
    /**
     * Get paint configured for the current drawing tool
     */
    fun getCurrentPaint(
        tool: DrawingTool,
        color: Int,
        strokeWidth: Float
    ): Paint {
        return when (tool) {
            DrawingTool.PEN -> drawPaint.apply {
                this.color = color
                this.strokeWidth = strokeWidth
                xfermode = null
            }
            DrawingTool.ERASER -> eraserPaint.apply {
                this.strokeWidth = strokeWidth * 2
            }
            DrawingTool.TEXT -> textPaint.apply {
                this.color = color
                textSize = strokeWidth * 6
            }
            DrawingTool.SELECTION -> selectionPaint
            DrawingTool.SELECTIVE_ERASER -> selectionPaint
            DrawingTool.LINE, DrawingTool.CIRCLE, DrawingTool.TRIANGLE, DrawingTool.RECTANGLE -> drawPaint.apply {
                this.color = color
                this.strokeWidth = strokeWidth
                xfermode = null
                style = Paint.Style.STROKE
            }
        }
    }
    
    /**
     * Get paint for a specific section in split mode
     */
    fun getPaintForSection(
        tool: DrawingTool,
        color: Int,
        strokeWidth: Float
    ): Paint {
        return Paint(getCurrentPaint(tool, color, strokeWidth))
    }
}

