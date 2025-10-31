package com.example.moodle.ui.whiteboard

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.LiveData
import android.graphics.Color
import android.net.Uri
import com.example.moodle.data.datastore.UserPreferences
import com.example.moodle.ui.whiteboard.managers.ExportManager
import com.example.moodle.ui.whiteboard.managers.ImportManager
import com.example.moodle.ui.whiteboard.managers.PageManager
import com.example.moodle.ui.whiteboard.models.BackgroundColor
import com.example.moodle.ui.whiteboard.models.BackgroundPattern
import com.example.moodle.ui.whiteboard.models.DrawingTool
import com.example.moodle.ui.whiteboard.models.PageData
import com.example.moodle.ui.whiteboard.views.WhiteboardView

/**
 * ViewModel for WhiteboardActivity
 * Manages UI state and business logic following MVVM architecture
 * Consolidates functionality from ToolsController, PaginationController, and ImportExportController
 */
class WhiteboardViewModel(application: Application) : AndroidViewModel(application) {
    
    // Managers (business logic)
    private val userPreferences = UserPreferences(application)
    val pageManager = PageManager(userPreferences)
    val exportManager = ExportManager(application)
    val importManager = ImportManager(application)
    
    // ========== UI State - Observable LiveData ==========
    
    // Tool State
    private val _currentColor = MutableLiveData<Int>()
    val currentColor: LiveData<Int> = _currentColor
    
    private val _currentStrokeWidth = MutableLiveData<Float>()
    val currentStrokeWidth: LiveData<Float> = _currentStrokeWidth
    
    private val _currentTool = MutableLiveData<DrawingTool>()
    val currentTool: LiveData<DrawingTool> = _currentTool
    
    private val _isDrawingMode = MutableLiveData<Boolean>()
    val isDrawingMode: LiveData<Boolean> = _isDrawingMode
    
    private val _isPenInSelectionMode = MutableLiveData<Boolean>()
    val isPenInSelectionMode: LiveData<Boolean> = _isPenInSelectionMode
    
    private val _isSelectiveEraserMode = MutableLiveData<Boolean>()
    val isSelectiveEraserMode: LiveData<Boolean> = _isSelectiveEraserMode
    
    // Undo/Redo State
    private val _canUndo = MutableLiveData<Boolean>()
    val canUndo: LiveData<Boolean> = _canUndo
    
    private val _canRedo = MutableLiveData<Boolean>()
    val canRedo: LiveData<Boolean> = _canRedo
    
    // Pagination State
    private val _currentPageNumber = MutableLiveData<Int>()
    val currentPageNumber: LiveData<Int> = _currentPageNumber
    
    private val _totalPages = MutableLiveData<Int>()
    val totalPages: LiveData<Int> = _totalPages
    
    private val _canGoToPreviousPage = MutableLiveData<Boolean>()
    val canGoToPreviousPage: LiveData<Boolean> = _canGoToPreviousPage
    
    private val _canGoToNextPage = MutableLiveData<Boolean>()
    val canGoToNextPage: LiveData<Boolean> = _canGoToNextPage
    
    private val _canCreateNewPage = MutableLiveData<Boolean>()
    val canCreateNewPage: LiveData<Boolean> = _canCreateNewPage
    
    private val _canDeletePage = MutableLiveData<Boolean>()
    val canDeletePage: LiveData<Boolean> = _canDeletePage
    
    // Background State
    private val _backgroundPattern = MutableLiveData<BackgroundPattern>()
    val backgroundPattern: LiveData<BackgroundPattern> = _backgroundPattern
    
    private val _backgroundColor = MutableLiveData<BackgroundColor>()
    val backgroundColor: LiveData<BackgroundColor> = _backgroundColor
    
    // ========== Events (one-time actions) ==========
    
    private val _showTextInputDialog = MutableLiveData<Pair<Float, Float>?>()
    val showTextInputDialog: LiveData<Pair<Float, Float>?> = _showTextInputDialog
    
    private val _showToastMessage = MutableLiveData<String?>()
    val showToastMessage: LiveData<String?> = _showToastMessage
    
    private val _exportSuccess = MutableLiveData<String?>()
    val exportSuccess: LiveData<String?> = _exportSuccess
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _pageChangeEvent = MutableLiveData<Boolean>()
    val pageChangeEvent: LiveData<Boolean> = _pageChangeEvent
    
    init {
        initializeDefaultValues()
        setupPageManagerListener()
    }
    
    /**
     * Initialize default values
     */
    private fun initializeDefaultValues() {
        // Load saved shared settings
        _currentColor.value = pageManager.sharedColor
        _currentStrokeWidth.value = pageManager.sharedStrokeWidth
        
        // Tool defaults
        _currentTool.value = DrawingTool.PEN
        _isDrawingMode.value = true
        _isPenInSelectionMode.value = false
        _isSelectiveEraserMode.value = false
        
        // Undo/Redo defaults
        _canUndo.value = false
        _canRedo.value = false
        
        // Load current page's background pattern and color
        val currentPage = pageManager.getCurrentPage()
        _backgroundPattern.value = currentPage.backgroundPattern
        _backgroundColor.value = currentPage.backgroundColor
        
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
                _backgroundPattern.value = currentPage.backgroundPattern
                _backgroundColor.value = currentPage.backgroundColor
                _pageChangeEvent.value = true
            }
            
            override fun onPageCreated(pageId: Int, totalPages: Int) {
                updatePaginationState()
                // Load the new page's background settings
                val currentPage = pageManager.getCurrentPage()
                _backgroundPattern.value = currentPage.backgroundPattern
                _backgroundColor.value = currentPage.backgroundColor
                _pageChangeEvent.value = true
            }
        })
    }
    
    // ========== Tool Management ==========
    
    /**
     * Select a drawing tool
     */
    fun selectTool(tool: DrawingTool) {
        _currentTool.value = tool
        
        // Reset pen selection mode when selecting other tools
        if (tool != DrawingTool.PEN && tool != DrawingTool.SELECTION) {
            _isPenInSelectionMode.value = false
        }
        
        // Ensure drawing mode is enabled when selecting a tool
        if (_isDrawingMode.value == false) {
            setDrawingMode(true)
        }
    }
    
    /**
     * Toggle pen between drawing and selection mode
     */
    fun togglePenSelectionMode() {
        val currentMode = _isPenInSelectionMode.value ?: false
        _isPenInSelectionMode.value = !currentMode
        
        if (!currentMode) {
            // Switch to selection mode
            _currentTool.value = DrawingTool.SELECTION
            _showToastMessage.value = "selection_mode"
        } else {
            // Switch back to pen mode
            _currentTool.value = DrawingTool.PEN
            _showToastMessage.value = "pen_mode"
        }
    }
    
    /**
     * Select a color
     */
    fun selectColor(color: Int) {
        _currentColor.value = color
        pageManager.updateSharedColor(color)
    }
    
    /**
     * Set stroke width
     */
    fun setStrokeWidth(width: Float) {
        _currentStrokeWidth.value = width
        pageManager.updateSharedStrokeWidth(width)
    }
    
    /**
     * Toggle between drawing and pan mode
     */
    fun toggleDrawingMode() {
        val currentMode = _isDrawingMode.value ?: true
        setDrawingMode(!currentMode)
    }
    
    /**
     * Set drawing mode
     */
    fun setDrawingMode(isDrawing: Boolean) {
        _isDrawingMode.value = isDrawing
        
        val modeKey = if (isDrawing) "drawing_mode" else "pan_mode"
        _showToastMessage.value = modeKey
    }
    
    /**
     * Set selective eraser mode
     */
    fun setSelectiveEraserMode(selective: Boolean) {
        _isSelectiveEraserMode.value = selective
    }
    
    /**
     * Select background pattern for current page
     */
    fun selectBackgroundPattern(pattern: BackgroundPattern) {
        _backgroundPattern.value = pattern
        val currentPage = pageManager.getCurrentPage()
        currentPage.backgroundPattern = pattern
    }
    
    /**
     * Select background color for current page
     */
    fun selectBackgroundColor(color: BackgroundColor) {
        _backgroundColor.value = color
        val currentPage = pageManager.getCurrentPage()
        currentPage.backgroundColor = color
    }
    
    /**
     * Request text input dialog
     */
    fun requestTextInput(x: Float, y: Float) {
        _showTextInputDialog.value = Pair(x, y)
    }
    
    /**
     * Clear text input dialog event
     */
    fun clearTextInputDialogEvent() {
        _showTextInputDialog.value = null
    }
    
    /**
     * Clear toast message event
     */
    fun clearToastMessageEvent() {
        _showToastMessage.value = null
    }
    
    // ========== Page Management ==========
    
    /**
     * Update pagination state
     */
    private fun updatePaginationState() {
        val pageIds = pageManager.getPageIds()
        val currentIndex = pageIds.indexOf(pageManager.getCurrentPageId())
        
        _currentPageNumber.value = currentIndex + 1
        _totalPages.value = pageManager.getTotalPages()
        _canGoToPreviousPage.value = pageManager.canGoToPreviousPage()
        _canGoToNextPage.value = pageManager.canGoToNextPage()
        _canCreateNewPage.value = pageManager.canCreateNewPage()
        _canDeletePage.value = pageManager.getTotalPages() > 1
    }
    
    /**
     * Load shared settings from page manager
     */
    private fun loadSharedSettings() {
        _currentColor.value = pageManager.sharedColor
        _currentStrokeWidth.value = pageManager.sharedStrokeWidth
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
            _showToastMessage.value = "max_pages_reached"
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
            _showToastMessage.value = "cannot_delete_last_page"
            return false
        }
        
        val currentPageId = pageManager.getCurrentPageId()
        val result = pageManager.deletePage(currentPageId)
        
        if (result) {
            updatePaginationState()
            _showToastMessage.value = "page_deleted"
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
     * Undo on current page
     */
    fun undoCurrentPage(): Boolean {
        return pageManager.undoCurrentPage()
    }
    
    /**
     * Redo on current page
     */
    fun redoCurrentPage(): Boolean {
        return pageManager.redoCurrentPage()
    }
    
    /**
     * Update undo/redo state
     */
    fun updateUndoRedoState(canUndo: Boolean, canRedo: Boolean) {
        _canUndo.value = canUndo
        _canRedo.value = canRedo
    }
    
    // ========== Import/Export ==========
    
    /**
     * Export current page to PNG
     */
    fun exportCurrentPageToPNG(whiteboardView: WhiteboardView) {
        exportManager.exportToPNG(
            whiteboardView,
            onSuccess = { file ->
                _exportSuccess.value = file.absolutePath
            },
            onError = { error ->
                _errorMessage.value = error
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
                _exportSuccess.value = "exported_${files.size}_pages"
            },
            onError = { error ->
                _errorMessage.value = error
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
                _exportSuccess.value = file.absolutePath
            },
            onError = { error ->
                _errorMessage.value = error
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
                _errorMessage.value = error
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
                _errorMessage.value = error
            }
        )
    }
    
    /**
     * Clear export success event
     */
    fun clearExportSuccessEvent() {
        _exportSuccess.value = null
    }
    
    /**
     * Clear error message event
     */
    fun clearErrorMessageEvent() {
        _errorMessage.value = null
    }
    
    /**
     * Clear page change event
     */
    fun clearPageChangeEvent() {
        _pageChangeEvent.value = false
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up if needed
    }
}

