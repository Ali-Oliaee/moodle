package com.example.moodle.ui.whiteboard.models

data class WhiteboardViewState(
    val backgroundPattern: BackgroundPattern,
    val backgroundColor: BackgroundColor,
    val isDrawingMode: Boolean,
    val isZooming: Boolean,
    val currentColor: Int,
    val currentStrokeWidth: Float,
    val currentTool: DrawingTool,
    val isSelectiveEraserMode: Boolean,
    // Pagination
    val currentPageNumber: Int,
    val totalPages: Int,
    val canGoToPreviousPage: Boolean,
    val canGoToNextPage: Boolean,
    val canCreateNewPage: Boolean,
    val canDeletePage: Boolean,

    val showTextInputDialog: Pair<Float, Float>?,
    val showToastMessage: String?,
    val exportSuccess: String?,
    val errorMessage: String?,
    val pageChangeEvent: Boolean
)

