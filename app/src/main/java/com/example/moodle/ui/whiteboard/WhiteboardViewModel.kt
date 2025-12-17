package com.example.moodle.ui.whiteboard

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.moodle.data.datastore.UserPreferences
import com.example.moodle.ui.whiteboard.managers.ExportManager
import com.example.moodle.ui.whiteboard.managers.ImportManager
import com.example.moodle.ui.whiteboard.managers.PageManager
import com.example.moodle.ui.whiteboard.managers.TextRecognitionManager
import com.example.moodle.ui.whiteboard.models.BackgroundColor
import com.example.moodle.ui.whiteboard.models.BackgroundPattern
import com.example.moodle.ui.whiteboard.models.DrawingTool
import com.example.moodle.ui.whiteboard.models.PageData
import com.example.moodle.ui.whiteboard.models.WhiteboardViewState
import com.example.moodle.ui.whiteboard.views.WhiteboardView
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for WhiteboardActivity
 * Manages UI state and business logic following MVVM architecture
 * Consolidates functionality from ToolsController, PaginationController, and ImportExportController
 * Dependencies are injected via Hilt
 * 
 * @param application Application context
 * @param userPreferences UserPreferences for storing settings
 * @param pageManager PageManager for page navigation and state
 * @param exportManager ExportManager for exporting whiteboard content
 * @param importManager ImportManager for importing content
 * @param textRecognitionManager TextRecognitionManager for text recognition functionality
 */
@HiltViewModel
class WhiteboardViewModel @Inject constructor(
    application: Application,
    private val userPreferences: UserPreferences,
    val pageManager: PageManager,
    val exportManager: ExportManager,
    val importManager: ImportManager,
    val textRecognitionManager: TextRecognitionManager
) : AndroidViewModel(application) {
    
    // Managers (business logic) - injected via constructor
    
    // ========== UI State - Observable LiveData ==========
    
    private val _uiState = MutableLiveData<WhiteboardViewState>()
    val uiState: LiveData<WhiteboardViewState> = _uiState

    init {
        initializeDefaultValues()
        setupPageManagerListener()
    }
    
    /**
     * Initialize default values
     */
    private fun initializeDefaultValues() {
        // Load saved shared settings
        val currentPage = pageManager.getCurrentPage()
        
        _uiState.value = WhiteboardViewState(
            backgroundPattern = currentPage.backgroundPattern,
            backgroundColor = currentPage.backgroundColor,
            isDrawingMode = true,
            isZooming = false,
            canUndo = false,
            canRedo = false,
            currentColor = pageManager.sharedColor,
            currentStrokeWidth = pageManager.sharedStrokeWidth,
            currentTool = DrawingTool.PEN,
            isSelectiveEraserMode = false,
            currentPageNumber = 1,
            totalPages = pageManager.getTotalPages(),
            canGoToPreviousPage = false,
            canGoToNextPage = pageManager.canGoToNextPage(),
            canCreateNewPage = pageManager.canCreateNewPage(),
            canDeletePage = pageManager.getTotalPages() > 1,
            showTextInputDialog = null,
            showToastMessage = null,
            exportSuccess = null,
            errorMessage = null,
            pageChangeEvent = false
        )
        
        // Initialize pagination state
        updatePaginationState()
    }
    
    /**
     * Setup page manager listener for page changes
     */
    private fun setupPageManagerListener() {
        pageManager.setOnPageChangeListener(object : PageManager.OnPageChangeListener {
            override fun onPageChanged(pageId: Int, totalPages: Int) {
                updatePaginationState()
                loadSharedSettings()
                // Load the current page's background settings
                val currentPage = pageManager.getCurrentPage()
                updateState { state ->
                    state.copy(
                        backgroundPattern = currentPage.backgroundPattern,
                        backgroundColor = currentPage.backgroundColor,
                        pageChangeEvent = true
                    )
                }
            }
            
            override fun onPageCreated(pageId: Int, totalPages: Int) {
                updatePaginationState()
                // Load the new page's background settings
                val currentPage = pageManager.getCurrentPage()
                updateState { state ->
                    state.copy(
                        backgroundPattern = currentPage.backgroundPattern,
                        backgroundColor = currentPage.backgroundColor,
                        pageChangeEvent = true
                    )
                }
            }
        })
    }
    
    // ========== State Update Helper ==========
    
    /**
     * Helper function to update state immutably
     */
    private fun updateState(update: (WhiteboardViewState) -> WhiteboardViewState) {
        _uiState.value?.let { currentState ->
            _uiState.value = update(currentState)
        }
    }
    
    // ========== Tool Management ==========
    
    /**
     * Select a drawing tool
     */
    fun selectTool(tool: DrawingTool) {
        updateState { state ->
            val shouldEnableDrawingMode = !state.isDrawingMode
            state.copy(
                currentTool = tool,
                isDrawingMode = if (shouldEnableDrawingMode) true else state.isDrawingMode
            )
        }
    }
    
    /**
     * Toggle pen between drawing and selection mode
     */
    fun togglePenSelectionMode() {
        updateState { state ->
            val newTool = if (state.currentTool == DrawingTool.PEN) {
                DrawingTool.SELECTION
            } else {
                DrawingTool.PEN
            }
            state.copy(currentTool = newTool)
        }
    }
    
    /**
     * Select a color
     */
    fun selectColor(color: Int) {
        pageManager.updateSharedColor(color)
        updateState { state ->
            state.copy(currentColor = color)
        }
    }
    
    /**
     * Set stroke width
     */
    fun setStrokeWidth(width: Float) {
        pageManager.updateSharedStrokeWidth(width)
        updateState { state ->
            state.copy(currentStrokeWidth = width)
        }
    }
    
    /**
     * Toggle between drawing and pan mode
     */
    fun toggleDrawingMode() {
        updateState { state ->
            state.copy(isDrawingMode = !state.isDrawingMode)
        }
    }
    
    /**
     * Set drawing mode
     */
    fun setDrawingMode(isDrawing: Boolean) {
        updateState { state ->
            state.copy(isDrawingMode = isDrawing)
        }
    }
    
    /**
     * Set selective eraser mode
     */
    fun setSelectiveEraserMode(selective: Boolean) {
        updateState { state ->
            state.copy(isSelectiveEraserMode = selective)
        }
    }
    
    /**
     * Select background pattern for current page
     */
    fun selectBackgroundPattern(pattern: BackgroundPattern) {
        val currentPage = pageManager.getCurrentPage()
        currentPage.backgroundPattern = pattern
        updateState { state ->
            state.copy(backgroundPattern = pattern)
        }
    }
    
    /**
     * Select background color for current page
     */
    fun selectBackgroundColor(color: BackgroundColor) {
        val currentPage = pageManager.getCurrentPage()
        currentPage.backgroundColor = color
        updateState { state ->
            state.copy(backgroundColor = color)
        }
    }
    
    /**
     * Request text input dialog
     */
    fun requestTextInput(x: Float, y: Float) {
        updateState { state ->
            state.copy(showTextInputDialog = Pair(x, y))
        }
    }
    
    /**
     * Clear text input dialog event
     */
    fun clearTextInputDialogEvent() {
        updateState { state ->
            state.copy(showTextInputDialog = null)
        }
    }
    
    /**
     * Clear toast message event
     */
    fun clearToastMessageEvent() {
        updateState { state ->
            state.copy(showToastMessage = null)
        }
    }
    
    // ========== Page Management ==========
    
    /**
     * Update pagination state
     */
    private fun updatePaginationState() {
        val pageIds = pageManager.getPageIds()
        val currentIndex = pageIds.indexOf(pageManager.getCurrentPageId())
        
        updateState { state ->
            state.copy(
                currentPageNumber = currentIndex + 1,
                totalPages = pageManager.getTotalPages(),
                canGoToPreviousPage = pageManager.canGoToPreviousPage(),
                canGoToNextPage = pageManager.canGoToNextPage(),
                canCreateNewPage = pageManager.canCreateNewPage(),
                canDeletePage = pageManager.getTotalPages() > 1
            )
        }
    }
    
    /**
     * Load shared settings from page manager
     */
    private fun loadSharedSettings() {
        updateState { state ->
            state.copy(
                currentColor = pageManager.sharedColor,
                currentStrokeWidth = pageManager.sharedStrokeWidth
            )
        }
    }
    
    /**
     * Navigate to next page or create new page
     */
    fun nextPageOrCreate() {
        if (pageManager.canGoToNextPage()) {
            pageManager.goToNextPage()
        } else if (pageManager.canCreateNewPage()) {
            pageManager.createNewPage()
        } else {
            updateState { state ->
                state.copy(showToastMessage = "max_pages_reached")
            }
        }
    }
    
    /**
     * Navigate to previous page
     */
    fun previousPage() {
        if (pageManager.canGoToPreviousPage()) {
            pageManager.goToPreviousPage()
        }
    }
    
    /**
     * Delete current page
     */
    fun deleteCurrentPage(): Boolean {
        if (pageManager.getTotalPages() <= 1) {
            updateState { state ->
                state.copy(showToastMessage = "cannot_delete_last_page")
            }
            return false
        }
        
        val currentPageId = pageManager.getCurrentPageId()
        val result = pageManager.deletePage(currentPageId)
        
        if (result) {
            updatePaginationState()
            updateState { state ->
                state.copy(showToastMessage = "page_deleted")
            }
        }
        
        return result
    }
    
    /**
     * Get current page data
     */
    fun getCurrentPage(): PageData {
        return pageManager.getCurrentPage()
    }
    
    /**
     * Clear current page
     */
    fun clearCurrentPage() {
        pageManager.clearCurrentPage()
    }
    
    /**
     * Update undo/redo state
     */
    fun updateUndoRedoState(canUndo: Boolean, canRedo: Boolean) {
        updateState { state ->
            state.copy(canUndo = canUndo, canRedo = canRedo)
        }
    }
    
    // ========== Import/Export ==========
    
    /**
     * Export current page to PNG
     */
    fun exportCurrentPageToPNG(whiteboardView: WhiteboardView) {
        exportManager.exportToPNG(
            whiteboardView,
            onSuccess = { file ->
                updateState { state ->
                    state.copy(exportSuccess = file.absolutePath)
                }
            },
            onError = { error ->
                updateState { state ->
                    state.copy(errorMessage = error)
                }
            }
        )
    }
    
    /**
     * Export all pages to PNG
     */
    fun exportAllPagesToPNG(whiteboardView: WhiteboardView) {
        exportManager.exportAllPagesToPNG(
            whiteboardView,
            pageManager,
            onSuccess = { files ->
                updateState { state ->
                    state.copy(exportSuccess = "exported_${files.size}_pages")
                }
            },
            onError = { error ->
                updateState { state ->
                    state.copy(errorMessage = error)
                }
            }
        )
    }
    
    /**
     * Export to Moodle custom format
     */
    fun exportToMoodleFormat(whiteboardView: WhiteboardView) {
        exportManager.exportToCustomFormat(
            whiteboardView,
            pageManager,
            onSuccess = { file ->
                updateState { state ->
                    state.copy(exportSuccess = file.absolutePath)
                }
            },
            onError = { error ->
                updateState { state ->
                    state.copy(errorMessage = error)
                }
            }
        )
    }
    
    /**
     * Import image from URI
     */
    fun importImage(uri: Uri, whiteboardView: WhiteboardView, onComplete: () -> Unit) {
        importManager.importImage(
            uri,
            whiteboardView,
            onSuccess = {
                onComplete()
            },
            onError = { error ->
                updateState { state ->
                    state.copy(errorMessage = error)
                }
            }
        )
    }
    
    /**
     * Import Moodle file from URI
     */
    fun importMoodleFile(uri: Uri, whiteboardView: WhiteboardView, onComplete: () -> Unit) {
        importManager.importCustomFormat(
            uri,
            whiteboardView,
            pageManager,
            onSuccess = {
                updatePaginationState()
                onComplete()
            },
            onError = { error ->
                updateState { state ->
                    state.copy(errorMessage = error)
                }
            }
        )
    }
    
    /**
     * Clear export success event
     */
    fun clearExportSuccessEvent() {
        updateState { state ->
            state.copy(exportSuccess = null)
        }
    }
    
    /**
     * Clear error message event
     */
    fun clearErrorMessageEvent() {
        updateState { state ->
            state.copy(errorMessage = null)
        }
    }
    
    /**
     * Clear page change event
     */
    fun clearPageChangeEvent() {
        updateState { state ->
            state.copy(pageChangeEvent = false)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up if needed
    }
}

