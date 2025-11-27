package com.example.moodle.ui.whiteboard.managers
import com.example.moodle.ui.whiteboard.models.*

import android.graphics.Color

/**
 * Manages multiple pages in the whiteboard application
 * Handles page creation, navigation, and state management
 */
class PageManager(private val userPreferences: com.example.moodle.data.datastore.UserPreferences? = null) {
    
    companion object {
        const val MAX_PAGES = 10
        const val DEFAULT_PAGE_ID = 1
    }
    
    private val pages = mutableMapOf<Int, PageData>()
    private var currentPageId = DEFAULT_PAGE_ID
    private var nextPageId = DEFAULT_PAGE_ID + 1
    
    // Shared settings across all pages
    var sharedColor: Int = Color.BLACK
        private set
    var sharedStrokeWidth: Float = 8f
        private set
    var sharedBackgroundPattern: BackgroundPattern = BackgroundPattern.PLAIN
        private set
    var sharedBackgroundColor: BackgroundColor = BackgroundColor.WHITE
        private set
    
    // Listener for page changes
    interface OnPageChangeListener {
        fun onPageChanged(pageId: Int, totalPages: Int)
        fun onPageCreated(pageId: Int, totalPages: Int)
    }
    
    private var pageChangeListener: OnPageChangeListener? = null
    
    init {
        // Load saved state or create the first page
        loadSavedState()
    }
    
    fun setOnPageChangeListener(listener: OnPageChangeListener?) {
        pageChangeListener = listener
    }
    
    fun getCurrentPage(): PageData {
        return pages[currentPageId] ?: run {
            // If current page doesn't exist, create it with default background
            val newPage = PageData(currentPageId)
            pages[currentPageId] = newPage
            newPage
        }
    }
    
    fun getCurrentPageId(): Int = currentPageId
    
    fun getTotalPages(): Int = pages.size
    
    fun getPageIds(): List<Int> = pages.keys.sorted()
    
    fun canGoToPreviousPage(): Boolean {
        val sortedPageIds = getPageIds()
        val currentIndex = sortedPageIds.indexOf(currentPageId)
        return currentIndex > 0
    }
    
    fun canGoToNextPage(): Boolean {
        val sortedPageIds = getPageIds()
        val currentIndex = sortedPageIds.indexOf(currentPageId)
        return currentIndex < sortedPageIds.size - 1
    }
    
    fun canCreateNewPage(): Boolean = pages.size < MAX_PAGES
    
    fun goToPreviousPage(): Boolean {
        if (!canGoToPreviousPage()) return false
        
        val sortedPageIds = getPageIds()
        val currentIndex = sortedPageIds.indexOf(currentPageId)
        currentPageId = sortedPageIds[currentIndex - 1]
        
        saveState()
        pageChangeListener?.onPageChanged(currentPageId, getTotalPages())
        return true
    }
    
    fun goToNextPage(): Boolean {
        if (!canGoToNextPage()) return false
        
        val sortedPageIds = getPageIds()
        val currentIndex = sortedPageIds.indexOf(currentPageId)
        currentPageId = sortedPageIds[currentIndex + 1]
        
        saveState()
        pageChangeListener?.onPageChanged(currentPageId, getTotalPages())
        return true
    }
    
    fun createNewPage(): Boolean {
        if (!canCreateNewPage()) return false
        
        val newPageId = nextPageId++
        val newPage = PageData(newPageId)
        pages[newPageId] = newPage
        currentPageId = newPageId
        
        saveState()
        pageChangeListener?.onPageCreated(currentPageId, getTotalPages())
        return true
    }
    
    fun goToPage(pageId: Int): Boolean {
        if (!pages.containsKey(pageId)) return false
        
        currentPageId = pageId
        saveState()
        pageChangeListener?.onPageChanged(currentPageId, getTotalPages())
        return true
    }
    
    fun updateSharedColor(color: Int) {
        sharedColor = color
        userPreferences?.saveSharedColor(color)
    }
    
    fun updateSharedStrokeWidth(strokeWidth: Float) {
        sharedStrokeWidth = strokeWidth
        userPreferences?.saveSharedStrokeWidth(strokeWidth)
    }
    
    fun updateSharedBackgroundPattern(pattern: BackgroundPattern) {
        sharedBackgroundPattern = pattern
        userPreferences?.saveSharedBackgroundPattern(pattern.id)
        // Update background pattern for all existing pages
        pages.values.forEach { page ->
            page.backgroundPattern = pattern
        }
    }
    
    fun deletePage(pageId: Int): Boolean {
        if (pages.size <= 1 || !pages.containsKey(pageId)) return false
        
        val replacePageIndex = getPageIds().indexOf(currentPageId) - 1
        pages.remove(pageId)

        // If we deleted the current page, navigate to the first available page
        if (currentPageId == pageId) {
            currentPageId = getPageIds()[replacePageIndex]
            saveState()
            pageChangeListener?.onPageChanged(currentPageId, getTotalPages())
        } else saveState()
        
        return true
    }
    
    fun clearCurrentPage() {
        getCurrentPage().clear()
    }
    
    fun clearAllPages() {
        pages.clear()
        currentPageId = DEFAULT_PAGE_ID
        nextPageId = DEFAULT_PAGE_ID + 1
        // Create default first page with default background
        pages[DEFAULT_PAGE_ID] = PageData(DEFAULT_PAGE_ID)
        saveState()
        pageChangeListener?.onPageChanged(currentPageId, getTotalPages())
    }
    
    fun getPage(pageId: Int): PageData? {
        return pages[pageId]
    }
    
    fun addPage(page: PageData) {
        pages[page.pageId] = page
        if (page.pageId >= nextPageId) {
            nextPageId = page.pageId + 1
        }
        saveState()
    }
    
    fun undoCurrentPage(): Boolean {
        val currentPage = getCurrentPage()
        return currentPage.undo() != null
    }
    
    fun redoCurrentPage(): Boolean {
        val currentPage = getCurrentPage()
        return currentPage.redo() != null
    }
    
    private fun loadSavedState() {
        if (userPreferences != null) {
            // Load shared settings
            sharedColor = userPreferences.getSharedColor()
            sharedStrokeWidth = userPreferences.getSharedStrokeWidth()
            val savedPatternId = userPreferences.getSharedBackgroundPattern()
            sharedBackgroundPattern = BackgroundPattern.fromId(savedPatternId)
            val savedColorId = userPreferences.getSharedBackgroundColor()
            sharedBackgroundColor = BackgroundColor.fromId(savedColorId)
            
            // Load page state
            currentPageId = userPreferences.getCurrentPageId()
            nextPageId = userPreferences.getNextPageId()
            
            val savedPageIds = userPreferences.getPageIds().map { it.toInt() }.toSet()
            savedPageIds.forEach { pageId ->
                // Create pages with default background (per-page settings)
                pages[pageId] = PageData(pageId)
            }
        } else {
            // Create default first page
            pages[DEFAULT_PAGE_ID] = PageData(DEFAULT_PAGE_ID)
        }
    }
    
    private fun saveState() {
        userPreferences?.let { prefs ->
            prefs.saveCurrentPageId(currentPageId)
            prefs.saveNextPageId(nextPageId)
            prefs.savePageIds(pages.keys)
            prefs.saveSharedColor(sharedColor)
            prefs.saveSharedStrokeWidth(sharedStrokeWidth)
            prefs.saveSharedBackgroundPattern(sharedBackgroundPattern.id)
            prefs.saveSharedBackgroundColor(sharedBackgroundColor.id)
        }
    }
}
