package com.example.moodle.ui.whiteboard.models

enum class BackgroundPattern(val id: Int, val nameResId: Int) {
    PLAIN(0, com.example.moodle.R.string.background_plain),
    GRID_SMALL(1, com.example.moodle.R.string.background_grid_small),
    GRID_LARGE(2, com.example.moodle.R.string.background_grid_large),
    LINES_HORIZONTAL(3, com.example.moodle.R.string.background_lines_horizontal),
    LINES_VERTICAL(4, com.example.moodle.R.string.background_lines_vertical),
    LINES_BOTH(5, com.example.moodle.R.string.background_lines_both),
    DOT_GRID(6, com.example.moodle.R.string.background_dot_grid),
    RULED_PAPER(7, com.example.moodle.R.string.background_ruled_paper),
    GRAPH_PAPER(8, com.example.moodle.R.string.background_graph_paper);

    companion object {
        fun fromId(id: Int): BackgroundPattern {
            return values().find { it.id == id } ?: PLAIN
        }
    }
}

