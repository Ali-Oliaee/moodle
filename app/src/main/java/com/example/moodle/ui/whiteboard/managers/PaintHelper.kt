package com.example.moodle.ui.whiteboard.managers
import com.example.moodle.ui.whiteboard.models.*

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
     * Returns a new Paint instance to avoid shared references
     */
    fun getCurrentPaint(
        tool: DrawingTool,
        color: Int,
        strokeWidth: Float
    ): Paint {
        return when (tool) {
            DrawingTool.PEN -> Paint(drawPaint).apply {
                this.color = color
                this.strokeWidth = strokeWidth
                xfermode = null
            }
            DrawingTool.ERASER -> Paint(eraserPaint).apply {
                this.strokeWidth = strokeWidth * 2
            }
            DrawingTool.TEXT -> Paint(textPaint).apply {
                this.color = color
                textSize = strokeWidth * 6
            }
            DrawingTool.SELECTION -> Paint(selectionPaint)
            DrawingTool.SELECTIVE_ERASER -> Paint(selectionPaint)
            // All shape tools
            DrawingTool.LINE, DrawingTool.CIRCLE, DrawingTool.TRIANGLE, DrawingTool.RECTANGLE,
            DrawingTool.CUBE, DrawingTool.CYLINDER, DrawingTool.PYRAMID, DrawingTool.SPHERE, DrawingTool.CONE,
            DrawingTool.RIGHT_TRIANGLE, DrawingTool.ISOSCELES_TRIANGLE, DrawingTool.PARALLELOGRAM, 
            DrawingTool.TRAPEZOID, DrawingTool.PENTAGON, DrawingTool.HEXAGON, DrawingTool.STAR, 
            DrawingTool.DIAMOND, DrawingTool.ELLIPSE, DrawingTool.ARROW -> Paint(drawPaint).apply {
                this.color = color
                this.strokeWidth = strokeWidth
                xfermode = null
                style = Paint.Style.STROKE
            }
        }
    }
    
    /**
     * Get paint for a specific section in split mode
     * Uses white color for eraser since CLEAR mode doesn't work without bitmap layer
     * Returns a new Paint instance to avoid shared references
     */
    fun getPaintForSection(
        tool: DrawingTool,
        color: Int,
        strokeWidth: Float
    ): Paint {
        return when (tool) {
            DrawingTool.ERASER -> Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                this.color = Color.WHITE
                this.strokeWidth = strokeWidth * 2
            }
            else -> getCurrentPaint(tool, color, strokeWidth)
        }
    }
}

