package com.example.moodle.ui.whiteboard

import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.example.moodle.R

/**
 * Controller responsible for managing pagination functionality
 */
class PaginationController(
    private val activity: AppCompatActivity,
    private val whiteboardView: WhiteboardView,
    private val pageManager: PageManager
) {
    
    // Pagination components
    private lateinit var btnPreviousPage: ImageButton
    private lateinit var btnNextPage: ImageButton
    private lateinit var btnDeletePage: ImageButton
    private lateinit var tvPageIndicator: TextView
    
    // Callbacks
    var onPageChanged: (() -> Unit)? = null
    
    /**
     * Initialize the pagination controller with UI components
     */
    fun initialize(
        previousPage: ImageButton,
        nextPage: ImageButton,
        deletePage: ImageButton,
        pageIndicator: TextView
    ) {
        btnPreviousPage = previousPage
        btnNextPage = nextPage
        btnDeletePage = deletePage
        tvPageIndicator = pageIndicator
        
        setupPageManagerListener()
        setupPaginationButtons()
        updatePaginationUI()
        loadCurrentPageToWhiteboard()
    }
    
    /**
     * Setup page manager listener
     */
    private fun setupPageManagerListener() {
        pageManager.setOnPageChangeListener(object : PageManager.OnPageChangeListener {
            override fun onPageChanged(pageId: Int, totalPages: Int) {
                updatePaginationUI()
                loadCurrentPageToWhiteboard()
            }
            
            override fun onPageCreated(pageId: Int, totalPages: Int) {
                updatePaginationUI()
                loadCurrentPageToWhiteboard()
            }
        })
    }
    
    /**
     * Setup pagination button listeners
     */
    private fun setupPaginationButtons() {
        btnPreviousPage.setOnClickListener {
            saveCurrentPageFromWhiteboard()
            pageManager.goToPreviousPage()
        }
        
        btnNextPage.setOnClickListener {
            saveCurrentPageFromWhiteboard()
            if (pageManager.canGoToNextPage()) {
                pageManager.goToNextPage()
            } else if (pageManager.canCreateNewPage()) {
                pageManager.createNewPage()
            } else {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.max_pages_reached, PageManager.MAX_PAGES),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        btnDeletePage.setOnClickListener {
            showDeletePageConfirmDialog()
        }
    }
    
    /**
     * Update pagination UI based on current state
     */
    fun updatePaginationUI() {
        val currentPageId = pageManager.getCurrentPageId()
        val totalPages = pageManager.getTotalPages()
        val pageIds = pageManager.getPageIds()
        val currentIndex = pageIds.indexOf(currentPageId) + 1
        
        // Update page indicator
        tvPageIndicator.text = activity.getString(R.string.page_indicator_format, currentIndex, totalPages)
        
        // Update previous button state
        btnPreviousPage.isEnabled = pageManager.canGoToPreviousPage()
        btnPreviousPage.alpha = if (pageManager.canGoToPreviousPage()) 1.0f else 0.5f
        
        // Update next button icon and state
        if (pageManager.canGoToNextPage()) {
            btnNextPage.setImageResource(R.drawable.ic_arrow_forward)
            btnNextPage.contentDescription = activity.getString(R.string.desc_next_page)
        } else if (pageManager.canCreateNewPage()) {
            btnNextPage.setImageResource(R.drawable.ic_add)
            btnNextPage.contentDescription = activity.getString(R.string.desc_add_page)
        } else {
            btnNextPage.setImageResource(R.drawable.ic_add)
            btnNextPage.contentDescription = activity.getString(R.string.desc_add_page)
            btnNextPage.alpha = 0.5f
        }
        
        if (pageManager.canGoToNextPage() || pageManager.canCreateNewPage()) {
            btnNextPage.alpha = 1.0f
        }
        
        // Update delete button state (disable if only one page)
        btnDeletePage.isEnabled = pageManager.getTotalPages() > 1
        btnDeletePage.alpha = if (pageManager.getTotalPages() > 1) 1.0f else 0.5f
    }
    
    /**
     * Save current page data from whiteboard
     */
    fun saveCurrentPageFromWhiteboard() {
        val currentPage = pageManager.getCurrentPage()
        
        // Save current whiteboard state to the page
        currentPage.paths.clear()
        currentPage.paths.addAll(whiteboardView.getAllPaths())
        
        currentPage.textObjects.clear()
        currentPage.textObjects.addAll(whiteboardView.getAllTextObjects())
        
        currentPage.shapeObjects.clear()
        currentPage.shapeObjects.addAll(whiteboardView.getAllShapeObjects())
        
        currentPage.imageObjects.clear()
        currentPage.imageObjects.addAll(whiteboardView.getAllImageObjects())
        
        currentPage.backgroundPattern = whiteboardView.getBackgroundPattern()
    }
    
    /**
     * Load current page data to whiteboard
     */
    fun loadCurrentPageToWhiteboard() {
        val currentPage = pageManager.getCurrentPage()
        
        // Load page state to whiteboard
        whiteboardView.loadPageData(
            currentPage.paths,
            currentPage.textObjects,
            currentPage.shapeObjects,
            currentPage.imageObjects
        )
        whiteboardView.setBackgroundPattern(currentPage.backgroundPattern)
        
        onPageChanged?.invoke()
    }
    
    /**
     * Clear current page
     */
    fun clearCurrentPage() {
        pageManager.clearCurrentPage()
        loadCurrentPageToWhiteboard()
    }
    
    /**
     * Perform undo on current page
     */
    fun undoCurrentPage(): Boolean {
        val result = pageManager.undoCurrentPage()
        if (result) {
            loadCurrentPageToWhiteboard()
        }
        return result
    }
    
    /**
     * Perform redo on current page
     */
    fun redoCurrentPage(): Boolean {
        val result = pageManager.redoCurrentPage()
        if (result) {
            loadCurrentPageToWhiteboard()
        }
        return result
    }
    
    /**
     * Get current page undo/redo state
     */
    fun getCurrentPageUndoRedoState(): Pair<Boolean, Boolean> {
        val currentPage = pageManager.getCurrentPage()
        return Pair(currentPage.canUndo(), currentPage.canRedo())
    }
    
    /**
     * Show delete page confirmation dialog
     */
    private fun showDeletePageConfirmDialog() {
        if (pageManager.getTotalPages() <= 1) {
            Toast.makeText(
                activity,
                activity.getString(R.string.cannot_delete_last_page),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.delete_page_title))
            .setMessage(activity.getString(R.string.delete_page_confirm))
            .setPositiveButton(activity.getString(R.string.yes)) { _, _ ->
                val currentPageId = pageManager.getCurrentPageId()
                if (pageManager.deletePage(currentPageId)) {
                    updatePaginationUI()
                    loadCurrentPageToWhiteboard()
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.page_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(activity.getString(R.string.no), null)
            .show()
    }
    
    /**
     * Get page manager
     */
    fun getPageManager(): PageManager = pageManager
}

