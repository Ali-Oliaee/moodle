package com.example.moodle.ui.whiteboard

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
)

