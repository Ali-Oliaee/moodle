package com.example.moodle.ui.whiteboard.models

import android.graphics.Paint
import android.graphics.Path

/**
 * Data class representing a drawing path on the whiteboard
 */
data class DrawingPath(
    val path: Path,
    val paint: Paint,
    val tool: DrawingTool,
    val text: String? = null,
    val textX: Float = 0f,
    val textY: Float = 0f,
    val shapeStartX: Float = 0f,
    val shapeStartY: Float = 0f,
    val shapeEndX: Float = 0f,
    val shapeEndY: Float = 0f
) {
    /**
     * Create a deep copy of this DrawingPath
     */
    fun copy(): DrawingPath {
        val pathCopy = Path(path)
        val paintCopy = Paint(paint)
        return DrawingPath(
            path = pathCopy,
            paint = paintCopy,
            tool = tool,
            text = text,
            textX = textX,
            textY = textY,
            shapeStartX = shapeStartX,
            shapeStartY = shapeStartY,
            shapeEndX = shapeEndX,
            shapeEndY = shapeEndY
        )
    }
}

