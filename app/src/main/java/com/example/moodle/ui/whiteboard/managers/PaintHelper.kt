package com.example.moodle.ui.whiteboard.managers
import com.example.moodle.ui.whiteboard.models.*
import com.example.moodle.ui.whiteboard.views.*
import com.example.moodle.ui.whiteboard.listeners.*
import com.example.moodle.ui.whiteboard.controllers.*
import com.example.moodle.ui.whiteboard.utils.*

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
            // All shape tools
            DrawingTool.LINE, DrawingTool.CIRCLE, DrawingTool.TRIANGLE, DrawingTool.RECTANGLE,
            DrawingTool.CUBE, DrawingTool.CYLINDER, DrawingTool.PYRAMID, DrawingTool.SPHERE, DrawingTool.CONE,
            DrawingTool.RIGHT_TRIANGLE, DrawingTool.ISOSCELES_TRIANGLE, DrawingTool.PARALLELOGRAM, 
            DrawingTool.TRAPEZOID, DrawingTool.PENTAGON, DrawingTool.HEXAGON, DrawingTool.STAR, 
            DrawingTool.DIAMOND, DrawingTool.ELLIPSE, DrawingTool.ARROW,
            DrawingTool.RULER, DrawingTool.BAG, DrawingTool.CONVEYOR -> drawPaint.apply {
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
            else -> Paint(getCurrentPaint(tool, color, strokeWidth))
        }
    }
}

