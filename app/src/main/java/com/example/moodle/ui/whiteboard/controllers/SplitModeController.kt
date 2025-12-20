package com.example.moodle.ui.whiteboard.controllers

import com.example.moodle.ui.whiteboard.views.WhiteboardView

import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.example.moodle.R
import com.example.moodle.ui.whiteboard.components.AnimatedSectionDropdown

/**
 * Controller responsible for managing split mode functionality
 */
class SplitModeController(
    private val activity: AppCompatActivity,
    private val whiteboardView: WhiteboardView
) {
    
    // Split mode components
    private lateinit var btnEnableSplit: ImageButton
    private lateinit var btnDisableSplit: ImageButton
    private lateinit var bottomToolbar: LinearLayout
    private lateinit var dropdownComposeView: ComposeView
    
    // State
    private var isSplitMode = false
    private var isDropdownExpanded by mutableStateOf(false)
    
    // Callbacks
    var onSplitModeChanged: ((Boolean) -> Unit)? = null
    
    /**
     * Initialize the split mode controller with UI components
     */
    fun initialize(
        enableSplit: ImageButton,
        disableSplit: ImageButton,
        toolbar: LinearLayout,
        dropdownCompose: ComposeView
    ) {
        btnEnableSplit = enableSplit
        btnDisableSplit = disableSplit
        bottomToolbar = toolbar
        dropdownComposeView = dropdownCompose
        
        setupSplitModeControls()
        setupDropdownCompose()
    }
    
    /**
     * Setup split mode control listeners
     */
    private fun setupSplitModeControls() {
        btnEnableSplit.setOnClickListener {
            toggleDropdown()
        }
        
        btnDisableSplit.setOnClickListener {
            disableSplitMode()
        }
    }
    
    /**
     * Setup Compose dropdown with animated section selector
     */
    private fun setupDropdownCompose() {
        dropdownComposeView.setContent {
            MaterialTheme {
                AnimatedSectionDropdown(
                    isExpanded = isDropdownExpanded,
                    onItemSelected = { sectionCount ->
                        enableSplitMode(sectionCount)
                        hideDropdown()
                    },
                    onDismiss = {
                        hideDropdown()
                    }
                )
            }
        }
    }
    
    /**
     * Toggle dropdown visibility
     */
    private fun toggleDropdown() {
        if (isDropdownExpanded) {
            hideDropdown()
        } else {
            showDropdown()
        }
    }
    
    /**
     * Show the animated dropdown
     */
    private fun showDropdown() {
        isDropdownExpanded = true
        dropdownComposeView.visibility = View.VISIBLE
    }
    
    /**
     * Hide the animated dropdown
     */
    private fun hideDropdown() {
        isDropdownExpanded = false
        // Delay hiding to allow animation to complete
        dropdownComposeView.postDelayed({
            if (!isDropdownExpanded) {
                dropdownComposeView.visibility = View.GONE
            }
        }, 300)
    }
    
    /**
     * Enable split mode with specified section count
     */
    private fun enableSplitMode(sectionCount: Int) {
        // Enable split mode in whiteboard
        whiteboardView.enableSplitMode(sectionCount)
        isSplitMode = true
        
        // Update UI
        btnEnableSplit.visibility = View.GONE
        btnDisableSplit.visibility = View.VISIBLE
        bottomToolbar.visibility = View.GONE
        hideDropdown()
        
        onSplitModeChanged?.invoke(true)
    }
    
    /**
     * Disable split mode
     */
    private fun disableSplitMode() {
        // Disable split mode in whiteboard
        whiteboardView.disableSplitMode()
        isSplitMode = false
        
        // Update UI
        btnEnableSplit.visibility = View.VISIBLE
        btnDisableSplit.visibility = View.GONE
        bottomToolbar.visibility = View.VISIBLE
        
        onSplitModeChanged?.invoke(false)
    }
}

