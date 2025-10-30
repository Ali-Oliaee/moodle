package com.example.moodle.ui.whiteboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.moodle.R

/**
 * Controller responsible for managing import/export functionality
 */
class ImportExportController(
    private val activity: AppCompatActivity,
    private val whiteboardView: WhiteboardView,
    private val exportManager: ExportManager,
    private val importManager: ImportManager
) {
    
    companion object {
        const val REQUEST_CODE_STORAGE_PERMISSIONS = 1002
        const val REQUEST_CODE_IMPORT_IMAGE = 2001
        const val REQUEST_CODE_IMPORT_MOODLE = 2002
        
        private val STORAGE_PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    
    // Callbacks
    var onImportSuccess: (() -> Unit)? = null
    var onExportSuccess: (() -> Unit)? = null
    var onPageSaveRequired: (() -> Unit)? = null
    var getPageManager: (() -> PageManager)? = null
    
    /**
     * Show export options dialog
     */
    fun showExportOptionsDialog() {
        if (!checkStoragePermissions()) {
            requestStoragePermissions()
            return
        }
        
        val inflater = LayoutInflater.from(activity)
        val dialogView = inflater.inflate(R.layout.dialog_export_options, null)
        
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()
        
        // Export PNG option
        dialogView.findViewById<LinearLayout>(R.id.exportPngOption).setOnClickListener {
            dialog.dismiss()
            exportToPNG()
        }
        
        // Export Moodle format option
        dialogView.findViewById<LinearLayout>(R.id.exportMoodleOption).setOnClickListener {
            dialog.dismiss()
            exportToMoodleFormat()
        }
        
        // Cancel button
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    /**
     * Show import options dialog
     */
    fun showImportOptionsDialog() {
        if (!checkStoragePermissions()) {
            requestStoragePermissions()
            return
        }
        
        val inflater = LayoutInflater.from(activity)
        val dialogView = inflater.inflate(R.layout.dialog_import_options, null)
        
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()
        
        // Import image option
        dialogView.findViewById<LinearLayout>(R.id.importImageOption).setOnClickListener {
            dialog.dismiss()
            openImagePicker()
        }
        
        // Import Moodle file option
        dialogView.findViewById<LinearLayout>(R.id.importMoodleOption).setOnClickListener {
            dialog.dismiss()
            showImportMoodleConfirmDialog()
        }
        
        // Cancel button
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    /**
     * Export to PNG - show page selection if multiple pages
     */
    private fun exportToPNG() {
        onPageSaveRequired?.invoke()
        
        val pageManager = getPageManager?.invoke()
        if (pageManager != null && pageManager.getTotalPages() > 1) {
            showPageSelectionDialog()
        } else {
            exportCurrentPageToPNG()
        }
    }
    
    /**
     * Show page selection dialog for export
     */
    private fun showPageSelectionDialog() {
        val inflater = LayoutInflater.from(activity)
        val dialogView = inflater.inflate(R.layout.dialog_page_selection, null)
        
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()
        
        val pageManager = getPageManager?.invoke()
        val totalPages = pageManager?.getTotalPages() ?: 1
        
        // Update the description for all pages option with the actual count
        val tvAllPagesDescription = dialogView.findViewById<TextView>(R.id.tvAllPagesDescription)
        tvAllPagesDescription.text = activity.getString(R.string.export_all_pages_description, totalPages)
        
        // Export current page option
        dialogView.findViewById<LinearLayout>(R.id.exportCurrentPageOption).setOnClickListener {
            dialog.dismiss()
            exportCurrentPageToPNG()
        }
        
        // Export all pages option
        dialogView.findViewById<LinearLayout>(R.id.exportAllPagesOption).setOnClickListener {
            dialog.dismiss()
            exportAllPagesToPNG()
        }
        
        // Cancel button
        dialogView.findViewById<Button>(R.id.btnCancelPageSelection).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    /**
     * Export current page to PNG
     */
    private fun exportCurrentPageToPNG() {
        exportManager.exportToPNG(
            whiteboardView,
            onSuccess = { file ->
                onExportSuccess?.invoke()
            },
            onError = { error ->
                Toast.makeText(activity, error, Toast.LENGTH_LONG).show()
            }
        )
    }
    
    /**
     * Export all pages to PNG
     */
    private fun exportAllPagesToPNG() {
        val pageManager = getPageManager?.invoke() ?: return
        
        exportManager.exportAllPagesToPNG(
            whiteboardView,
            pageManager,
            onSuccess = { files ->
                onExportSuccess?.invoke()
            },
            onError = { error ->
                Toast.makeText(activity, error, Toast.LENGTH_LONG).show()
            }
        )
    }
    
    /**
     * Export to Moodle custom format
     */
    private fun exportToMoodleFormat() {
        onPageSaveRequired?.invoke()
        
        val pageManager = getPageManager?.invoke() ?: return
        
        exportManager.exportToCustomFormat(
            whiteboardView,
            pageManager,
            onSuccess = { file ->
                onExportSuccess?.invoke()
            },
            onError = { error ->
                Toast.makeText(activity, error, Toast.LENGTH_LONG).show()
            }
        )
    }
    
    /**
     * Open image picker for import
     */
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        
        try {
            activity.startActivityForResult(
                Intent.createChooser(intent, activity.getString(R.string.select_image_file)),
                REQUEST_CODE_IMPORT_IMAGE
            )
        } catch (e: Exception) {
            Toast.makeText(
                activity,
                activity.getString(R.string.no_file_manager),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * Show import Moodle file confirmation dialog
     */
    private fun showImportMoodleConfirmDialog() {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.import_moodle_file))
            .setMessage(activity.getString(R.string.import_will_clear_board))
            .setPositiveButton(activity.getString(R.string.continue_import)) { _, _ ->
                openMoodleFilePicker()
            }
            .setNegativeButton(activity.getString(R.string.cancel), null)
            .show()
    }
    
    /**
     * Open Moodle file picker for import
     */
    private fun openMoodleFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream", "text/plain"))
        }
        
        try {
            activity.startActivityForResult(
                Intent.createChooser(intent, activity.getString(R.string.select_moodle_file)),
                REQUEST_CODE_IMPORT_MOODLE
            )
        } catch (e: Exception) {
            Toast.makeText(
                activity,
                activity.getString(R.string.no_file_manager),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * Import image from URI
     */
    fun importImage(uri: Uri) {
        importManager.importImage(
            uri,
            whiteboardView,
            onSuccess = {
                onImportSuccess?.invoke()
            },
            onError = { error ->
                Toast.makeText(activity, error, Toast.LENGTH_LONG).show()
            }
        )
    }
    
    /**
     * Import Moodle file from URI
     */
    fun importMoodleFile(uri: Uri) {
        val pageManager = getPageManager?.invoke() ?: return
        
        importManager.importCustomFormat(
            uri,
            whiteboardView,
            pageManager,
            onSuccess = {
                onImportSuccess?.invoke()
            },
            onError = { error ->
                Toast.makeText(activity, error, Toast.LENGTH_LONG).show()
            }
        )
    }
    
    /**
     * Handle activity result
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        when (requestCode) {
            REQUEST_CODE_IMPORT_IMAGE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    data.data?.let { uri ->
                        importImage(uri)
                    }
                }
                return true
            }
            REQUEST_CODE_IMPORT_MOODLE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    data.data?.let { uri ->
                        importMoodleFile(uri)
                    }
                }
                return true
            }
        }
        return false
    }
    
    /**
     * Handle permission results
     */
    fun handlePermissionsResult(requestCode: Int, grantResults: IntArray): Boolean {
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.permissions_granted),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.storage_permission_denied),
                    Toast.LENGTH_LONG
                ).show()
            }
            return true
        }
        return false
    }
    
    /**
     * Check if storage permissions are granted
     */
    private fun checkStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            STORAGE_PERMISSIONS.all { permission ->
                ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            true
        }
    }
    
    /**
     * Request storage permissions
     */
    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(STORAGE_PERMISSIONS, REQUEST_CODE_STORAGE_PERMISSIONS)
        }
    }
}

