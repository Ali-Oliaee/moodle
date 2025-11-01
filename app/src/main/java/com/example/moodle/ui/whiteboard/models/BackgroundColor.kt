package com.example.moodle.ui.whiteboard.models

import android.graphics.Color

/**
 * Enum representing available background colors for the whiteboard
 * Each color has an ID for persistence and a color value
 */
enum class BackgroundColor(val id: Int, val colorValue: Int, val nameResId: Int) {
    WHITE(0, Color.WHITE, com.example.moodle.R.string.background_color_white),
    LIGHT_GRAY(1, Color.parseColor("#F5F5F5"), com.example.moodle.R.string.background_color_light_gray),
    LIGHT_BLUE(2, Color.parseColor("#E3F2FD"), com.example.moodle.R.string.background_color_light_blue),
    LIGHT_GREEN(3, Color.parseColor("#E8F5E9"), com.example.moodle.R.string.background_color_light_green),
    LIGHT_YELLOW(4, Color.parseColor("#FFFDE7"), com.example.moodle.R.string.background_color_light_yellow),
    LIGHT_PINK(5, Color.parseColor("#FCE4EC"), com.example.moodle.R.string.background_color_light_pink);

    companion object {
        fun fromId(id: Int): BackgroundColor {
            return values().find { it.id == id } ?: WHITE
        }
    }
}

