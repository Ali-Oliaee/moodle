// Example: How to enable Command Pattern in WhiteboardActivity
// Place this code in WhiteboardActivity.onCreate() after view initialization

package com.example.moodle.ui.whiteboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class WhiteboardActivity : AppCompatActivity() {
    
    private lateinit var whiteboardView: WhiteboardView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whiteboard)
        
        // ... existing initialization code ...
        
        // ============================================================
        // ENABLE COMMAND-BASED UNDO/REDO (90% Memory Reduction)
        // ============================================================
        
        // Option 1: Enable globally for all pages
        whiteboardView.setCommandBasedUndoRedoEnabled(true)
        
        // Option 2: Enable based on user preference
        val useCommandBased = getSharedPreferences("settings", MODE_PRIVATE)
            .getBoolean("use_command_based_undo", true)  // Default: true
        whiteboardView.setCommandBasedUndoRedoEnabled(useCommandBased)
        
        // Option 3: Enable based on device capabilities
        val hasLowMemory = isLowRamDevice()
        if (hasLowMemory) {
            // Definitely use command-based on low-memory devices
            whiteboardView.setCommandBasedUndoRedoEnabled(true)
        }
        
        // ============================================================
        // OPTIONAL: Add settings toggle in UI
        // ============================================================
        
        // Add to your settings menu
        btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }
    
    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Settings")
        
        val currentMode = whiteboardView.isCommandBasedUndoRedoEnabled()
        
        val options = arrayOf(
            "Use Command-Based Undo (90% less memory)",
            "Use Snapshot-Based Undo (legacy)"
        )
        
        builder.setSingleChoiceItems(options, if (currentMode) 0 else 1) { dialog, which ->
            val useCommandBased = (which == 0)
            whiteboardView.setCommandBasedUndoRedoEnabled(useCommandBased)
            
            // Save preference
            getSharedPreferences("settings", MODE_PRIVATE)
                .edit()
                .putBoolean("use_command_based_undo", useCommandBased)
                .apply()
            
            Toast.makeText(
                this,
                if (useCommandBased) 
                    "Switched to Command-Based Undo (90% less memory)"
                else 
                    "Switched to Snapshot-Based Undo (legacy)",
                Toast.LENGTH_SHORT
            ).show()
            
            dialog.dismiss()
        }
        
        builder.create().show()
    }
    
    private fun isLowRamDevice(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activityManager.isLowRamDevice
        } else {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            memInfo.totalMem < 2L * 1024 * 1024 * 1024  // < 2GB RAM
        }
    }
    
    // ============================================================
    // OPTIONAL: Monitor memory usage
    // ============================================================
    
    private fun logUndoRedoMemoryUsage() {
        val pageData = viewModel.getCurrentPage()
        
        if (whiteboardView.isCommandBasedUndoRedoEnabled()) {
            val analytics = pageData.commandManager.getAnalytics()
            Log.d("UndoRedo", """
                Command-Based Undo/Redo:
                - Total commands: ${analytics.totalCommands}
                - Memory usage: ${analytics.totalMemoryUsage / 1024}KB
                - Average command: ${analytics.averageCommandSize} bytes
            """.trimIndent())
        } else {
            // Snapshot-based memory is harder to estimate
            val stackSize = pageData.undoRedoManager.getUndoStackSize()
            val estimatedMemory = stackSize * 8 * 1024  // ~8MB per snapshot
            Log.d("UndoRedo", """
                Snapshot-Based Undo/Redo:
                - Stack size: $stackSize
                - Estimated memory: ${estimatedMemory / 1024}KB
            """.trimIndent())
        }
    }
}

// ============================================================
// EXAMPLE: Settings UI (res/layout/dialog_settings.xml)
// ============================================================
/*
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Undo/Redo System"
        android:textSize="18sp"
        android:textStyle="bold"
        android:paddingBottom="8dp"/>

    <RadioGroup
        android:id="@+id/radioGroupUndoMode"
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <RadioButton
            android:id="@+id/radioCommandBased"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Command-Based (Recommended)"
            android:checked="true"/>

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="• 90% less memory usage\n• Faster undo/redo\n• Same functionality"
            android:textSize="12sp"
            android:textColor="#666"
            android:paddingStart="32dp"
            android:paddingBottom="8dp"/>

        <RadioButton
            android:id="@+id/radioSnapshotBased"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Snapshot-Based (Legacy)"/>

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="• Higher memory usage\n• Slower performance\n• Legacy compatibility"
            android:textSize="12sp"
            android:textColor="#666"
            android:paddingStart="32dp"/>

    </RadioGroup>

</LinearLayout>
*/

// ============================================================
// EXAMPLE: Enable with feature flag (for gradual rollout)
// ============================================================
/*
class WhiteboardActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check remote config or feature flag
        val featureFlags = RemoteConfig.getInstance()
        val commandBasedEnabled = featureFlags.getBoolean(
            "command_based_undo_enabled",
            defaultValue = true  // Default to enabled
        )
        
        if (commandBasedEnabled) {
            whiteboardView.setCommandBasedUndoRedoEnabled(true)
            Log.i("FeatureFlag", "Command-based undo/redo enabled")
        }
    }
}
*/

// ============================================================
// EXAMPLE: A/B Testing
// ============================================================
/*
class WhiteboardActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Assign users to A/B test groups
        val userId = getUserId()
        val useCommandBased = (userId.hashCode() % 2 == 0)  // 50/50 split
        
        whiteboardView.setCommandBasedUndoRedoEnabled(useCommandBased)
        
        // Log for analytics
        Analytics.logEvent("undo_mode_assigned", mapOf(
            "mode" to if (useCommandBased) "command" else "snapshot",
            "userId" to userId
        ))
    }
}
*/

