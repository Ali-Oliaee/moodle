package com.example.moodle.ui.whiteboard.models

/**
 * Enum representing the available drawing tools
 */
enum class DrawingTool {
    PEN, ERASER, TEXT, SELECTIVE_ERASER, LINE, CIRCLE, TRIANGLE, RECTANGLE, SELECTION,
    // 3D Shapes
    CUBE, CYLINDER, PYRAMID, SPHERE, CONE,
    // Additional 2D Shapes
    RIGHT_TRIANGLE, ISOSCELES_TRIANGLE, PARALLELOGRAM, TRAPEZOID, 
    PENTAGON, HEXAGON, STAR, DIAMOND, ELLIPSE, ARROW,
    // Measurement and Special Tools
    RULER, BAG, CONVEYOR
}

