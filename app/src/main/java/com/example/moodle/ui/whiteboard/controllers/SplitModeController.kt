package com.example.moodle.ui.whiteboard.controllers
import com.example.moodle.ui.whiteboard.models.*
import com.example.moodle.ui.whiteboard.views.*

import com.example.moodle.ui.whiteboard.views.WhiteboardView

import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import com.example.moodle.R

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
    
    // State
    private var isSplitMode = false
    
    // Callbacks
    var onSplitModeChanged: ((Boolean) -> Unit)? = null
    
    /**
     * Initialize the split mode controller with UI components
     */
    fun initialize(
        enableSplit: ImageButton,
        disableSplit: ImageButton,
        toolbar: LinearLayout
    ) {
        btnEnableSplit = enableSplit
        btnDisableSplit = disableSplit
        bottomToolbar = toolbar
        
        setupSplitModeControls()
    }
    
    /**
     * Setup split mode control listeners
     */
    private fun setupSplitModeControls() {
        btnEnableSplit.setOnClickListener {
            showSplitModeDialog()
        }
        
        btnDisableSplit.setOnClickListener {
            disableSplitMode()
        }
    }
    
    /**
     * Show split mode selection dialog
     */
    private fun showSplitModeDialog() {
        val sectionCounts = arrayOf(
            activity.getString(R.string.section_count_2),
            activity.getString(R.string.section_count_3),
            activity.getString(R.string.section_count_4),
            activity.getString(R.string.section_count_6)
        )
        
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.choose_section_count))
            .setItems(sectionCounts) { _, which ->
                val sectionCount = when (which) {
                    0 -> 2
                    1 -> 3
                    2 -> 4
                    3 -> 6
                    else -> 2
                }
                enableSplitMode(sectionCount)
            }
            .setNegativeButton(activity.getString(R.string.cancel), null)
            .show()
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
        
        onSplitModeChanged?.invoke(true)
        
        Toast.makeText(
            activity,
            activity.getString(R.string.split_mode_enabled),
            Toast.LENGTH_SHORT
        ).show()
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
        
        Toast.makeText(
            activity,
            activity.getString(R.string.split_mode_disabled),
            Toast.LENGTH_SHORT
        ).show()
    }
    
    /**
     * Check if split mode is active
     */
    fun isSplitMode(): Boolean = isSplitMode
}

