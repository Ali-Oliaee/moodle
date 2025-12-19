package com.example.moodle.ui.whiteboard

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.example.moodle.R
import com.example.moodle.data.datastore.UserPreferences
import com.example.moodle.ui.login.LoginActivity
import com.example.moodle.ui.whiteboard.controllers.PopupController
import com.example.moodle.ui.whiteboard.controllers.RecordingController
import com.example.moodle.ui.whiteboard.controllers.SplitModeController
import com.example.moodle.ui.whiteboard.listeners.OnDrawingChangeListener
import com.example.moodle.ui.whiteboard.listeners.OnTextInputListener
import com.example.moodle.ui.whiteboard.managers.PageManager
import com.example.moodle.ui.whiteboard.managers.TextRecognitionManager
import com.example.moodle.ui.whiteboard.models.DrawingTool
import com.example.moodle.ui.whiteboard.models.WhiteboardViewState
import com.example.moodle.ui.whiteboard.views.WhiteboardView
import com.example.moodle.utils.FontHelper
import com.example.moodle.utils.PermissionManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main activity for the whiteboard functionality
 * Uses MVVM architecture with WhiteboardViewModel
 * Keeps UI-specific controllers for Android components
 */
@AndroidEntryPoint
class WhiteboardActivity : AppCompatActivity() {

    // UI Components
    private lateinit var whiteboardView: WhiteboardView
    private lateinit var recordContainer: LinearLayout
    private lateinit var toggleRecordBtn: Button
    private lateinit var btnAttendance: ImageButton
    private lateinit var btnLogout: ImageButton
    private lateinit var btnExport: ImageButton
    private lateinit var btnImport: ImageButton
    private lateinit var btnPinchMode: ImageButton
    private lateinit var btnToggleToolbars: ImageButton
    private lateinit var topToolbar: LinearLayout
    private lateinit var bottomToolbar: LinearLayout
    
    // Tool Buttons
    private lateinit var btnPen: ImageButton
    private lateinit var btnEraser: ImageButton
    private lateinit var btnText: ImageButton
    private lateinit var btnTextRecognition: ImageButton
    private lateinit var btnFormula: ImageButton
    private lateinit var btnShapes: ImageButton
    private lateinit var btnClear: ImageButton
    private lateinit var btnColorPicker: Button
    private lateinit var btnStrokeWidth: ImageButton
    private lateinit var btnBackground: ImageButton
    private lateinit var btnUndo: ImageButton
    private lateinit var btnRedo: ImageButton
    
    // Pagination Buttons
    private lateinit var btnPreviousPage: ImageButton
    private lateinit var btnNextPage: ImageButton
    private lateinit var btnDeletePage: ImageButton
    private lateinit var tvPageIndicator: TextView
    
    // ViewModel - Single source of truth for state
    private lateinit var viewModel: WhiteboardViewModel
    
    // UI-specific controllers (Android components)
    private lateinit var recordingController: RecordingController
    private lateinit var popupController: PopupController
    private lateinit var splitModeController: SplitModeController
    
    // Text recognition manager
    private lateinit var textRecognitionManager: TextRecognitionManager
    
    // Permission manager
    private lateinit var permissionManager: PermissionManager
    
    // Injected dependencies
    @Inject
    lateinit var userPreferences: UserPreferences
    
    // Toolbar visibility state
    private var areToolbarsVisible = true
    
    // Pinch mode state
    private var isPinchModeActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ViewModel using Hilt
        viewModel = ViewModelProvider(this).get(WhiteboardViewModel::class.java)
        
        setContentView(R.layout.activity_whiteboard)
        
        // Hide status bar for immersive whiteboard experience (must be after setContentView)
        hideSystemBars()
        
        initializeViews()
        initializeUIControllers()
        setupListeners()
        observeViewModel()
        applyFonts()
        // Load initial page
        loadCurrentPageFromViewModel()
    }
    
    /**
     * Initialize UI components
     */
    private fun initializeViews() {
        whiteboardView = findViewById(R.id.whiteboardView)
        recordContainer = findViewById(R.id.recordContainer)
        toggleRecordBtn = findViewById(R.id.toggleRecordBtn)
        btnAttendance = findViewById(R.id.btnAttendance)
        btnLogout = findViewById(R.id.btnLogout)
        btnExport = findViewById(R.id.btnExport)
        btnImport = findViewById(R.id.btnImport)
        btnPinchMode = findViewById(R.id.btnPinchMode)
        btnToggleToolbars = findViewById(R.id.btnToggleToolbars)
        topToolbar = findViewById(R.id.topToolbar)
        bottomToolbar = findViewById(R.id.bottomToolbar)
        
        // Tool buttons
        btnPen = findViewById(R.id.btnPen)
        btnEraser = findViewById(R.id.btnEraser)
        btnText = findViewById(R.id.btnText)
        btnTextRecognition = findViewById(R.id.btnTextRecognition)
        btnFormula = findViewById(R.id.btnFormula)
        btnShapes = findViewById(R.id.btnShapes)
        btnClear = findViewById(R.id.btnClear)
        btnColorPicker = findViewById(R.id.btnColorPicker)
        btnStrokeWidth = findViewById(R.id.btnStrokeWidth)
        btnBackground = findViewById(R.id.btnBackground)
        btnUndo = findViewById(R.id.btnUndo)
        btnRedo = findViewById(R.id.btnRedo)
        
        // Pagination buttons
        btnPreviousPage = findViewById(R.id.btnPreviousPage)
        btnNextPage = findViewById(R.id.btnNextPage)
        btnDeletePage = findViewById(R.id.btnDeletePage)
        tvPageIndicator = findViewById(R.id.tvPageIndicator)
        
        // Set initial tool selection
        btnPen.isSelected = true
    }
    
    /**
     * Initialize UI-specific controllers
     */
    private fun initializeUIControllers() {
        // Recording Controller - handles permissions and MediaProjection
        recordingController = RecordingController(this)
        recordingController.initialize(toggleRecordBtn)
        
        // Popup Controller - manages PopupWindows
        popupController = PopupController(this, whiteboardView)
        setupPopupCallbacks()
        
        // Split Mode Controller - manages split screen UI
        splitModeController = SplitModeController(this, whiteboardView)
        splitModeController.initialize(
            enableSplit = findViewById(R.id.btnEnableSplit),
            disableSplit = findViewById(R.id.btnDisableSplit),
            toolbar = findViewById(R.id.bottomToolbar)
        )
        
        splitModeController.onSplitModeChanged = { enabled ->
            if (enabled) {
                // Disable pinch mode in split mode
                if (isPinchModeActive) {
                    isPinchModeActive = false
                    whiteboardView.setPinchModeEnabled(false)
                    btnPinchMode.isSelected = false
                }
                // Hide toggle button in split mode
                btnToggleToolbars.visibility = android.view.View.GONE
                // If toolbars were hidden, show them back in split mode
                if (!areToolbarsVisible) {
                    areToolbarsVisible = true
                    showToolbar(topToolbar)
                    btnToggleToolbars.setImageResource(R.drawable.ic_chevron_up)
                }
            } else {
                // Show toggle button in normal mode
                btnToggleToolbars.visibility = android.view.View.VISIBLE
            }
            // Disable background button in split mode, enable in normal mode
            btnBackground.isEnabled = !enabled
            btnBackground.alpha = if (enabled) 0.5f else 1.0f
            // Disable pinch mode button in split mode
            btnPinchMode.isEnabled = !enabled
            btnPinchMode.alpha = if (enabled) 0.5f else 1.0f
        }
        
        // Text Recognition Manager - get from ViewModel (injected by Hilt)
        textRecognitionManager = viewModel.textRecognitionManager
        
        // Permission Manager - handles runtime permissions
        permissionManager = PermissionManager(this)
    }
    
    /**
     * Setup all listeners and callbacks
     */
    private fun setupListeners() {
        // Top bar listeners
        recordContainer.setOnClickListener { recordingController.toggleRecording() }
        btnAttendance.setOnClickListener { openAttendanceScreen() }
        btnLogout.setOnClickListener { logout() }
        btnExport.setOnClickListener { showExportOptionsDialog() }
        btnImport.setOnClickListener { showImportOptionsDialog() }
        btnPinchMode.setOnClickListener { togglePinchMode() }
        
        // Toggle toolbars button listener
        btnToggleToolbars.setOnClickListener { toggleToolbarsVisibility() }
        
        // Tool button listeners
        setupToolListeners()
        
        // Pagination button listeners
        setupPaginationListeners()
        
        // Whiteboard listeners
        setupWhiteboardListeners()
    }
    
    /**
     * Setup tool button listeners
     */
    private fun setupToolListeners() {
        btnPen.setOnClickListener {
            viewModel.togglePenSelectionMode()
        }
        
        btnEraser.setOnClickListener {
            popupController.showEraserModePopup(btnEraser)
        }
        
        btnText.setOnClickListener {
            viewModel.selectTool(DrawingTool.TEXT)
        }
        
        btnTextRecognition.setOnClickListener {
            performTextRecognition()
        }
        
        btnFormula.setOnClickListener {
            popupController.showFormulaPopup()
        }
        
        btnShapes.setOnClickListener {
            popupController.showShapesPopup(btnShapes)
        }
        
        btnClear.setOnClickListener {
            showClearConfirmDialog()
        }
        
        btnColorPicker.setOnClickListener {
            popupController.showColorPickerPopup(
                btnColorPicker,
                viewModel.uiState.value?.currentColor ?: Color.BLACK
            )
        }
        
        btnStrokeWidth.setOnClickListener {
            popupController.showStrokeWidthPopup(
                btnStrokeWidth,
                viewModel.uiState.value?.currentStrokeWidth ?: 8f
            )
        }
        
        btnBackground.setOnClickListener {
            popupController.showBackgroundPatternPopup()
        }
        
        btnUndo.setOnClickListener {
            whiteboardView.undo()
        }
        
        btnRedo.setOnClickListener {
            whiteboardView.redo()
        }
    }
    
    /**
     * Setup pagination button listeners
     */
    private fun setupPaginationListeners() {
        btnPreviousPage.setOnClickListener {
            saveCurrentPageToViewModel()
            viewModel.previousPage()
        }
        
        btnNextPage.setOnClickListener {
            saveCurrentPageToViewModel()
            viewModel.nextPageOrCreate()
        }
        
        btnDeletePage.setOnClickListener {
            showDeletePageConfirmDialog()
        }
    }
    
    /**
     * Setup popup controller callbacks
     */
    private fun setupPopupCallbacks() {
        popupController.onColorSelected = { color ->
            viewModel.selectColor(color)
        }
        
        popupController.onStrokeWidthChanged = { width ->
            viewModel.setStrokeWidth(width)
        }
        
        popupController.onBackgroundPatternSelected = { pattern ->
            viewModel.selectBackgroundPattern(pattern)
        }
        
        popupController.onBackgroundColorSelected = { color ->
            viewModel.selectBackgroundColor(color)
            whiteboardView.setBackgroundImage(null) // Clear background image when color is selected
        }
        
        popupController.onToolSelected = { tool ->
            viewModel.selectTool(tool)
        }
        
        popupController.onEraserModeSelected = { isSelective ->
            viewModel.setSelectiveEraserMode(isSelective)
        }
        
        popupController.onBackgroundImageUploadRequested = {
            openBackgroundImagePicker()
        }
        
    }
    
    /**
     * Setup whiteboard listeners
     */
    private fun setupWhiteboardListeners() {
        whiteboardView.setOnDrawingChangeListener(object : OnDrawingChangeListener {
            override fun onDrawingChanged() {
                saveCurrentPageToViewModel()
            }
        })
        
        whiteboardView.setOnTextInputListener(object : OnTextInputListener {
            override fun onTextInputRequested(x: Float, y: Float) {
                viewModel.requestTextInput(x, y)
            }
        })
        
        // Setup undo/redo listener
        whiteboardView.setOnUndoRedoStackChangedListener { canUndo, canRedo ->
            btnUndo.isEnabled = canUndo
            btnUndo.alpha = if (canUndo) 1.0f else 0.5f
            btnRedo.isEnabled = canRedo
            btnRedo.alpha = if (canRedo) 1.0f else 0.5f
        }
    }
    
    /**
     * Observe ViewModel LiveData
     * This is the core of MVVM - UI reacts to state changes
     */
    private fun observeViewModel() {
        viewModel.uiState.observe(this, Observer { state ->
            state?.let { handleStateUpdate(it) }
        })
    }
    
    /**
     * Handle UI state updates
     */
    private fun handleStateUpdate(state: WhiteboardViewState) {
        // Update tool
        whiteboardView.setDrawingTool(state.currentTool)
        updateToolButtonSelection(state.currentTool)
        
        // Update pen button icon based on tool
        val penIconRes = if (state.currentTool == DrawingTool.SELECTION) {
            R.drawable.ic_hand
        } else {
            R.drawable.ic_pen
        }
        btnPen.setImageResource(penIconRes)
        
        // Update color
        whiteboardView.setColor(state.currentColor)
        btnColorPicker.backgroundTintList = android.content.res.ColorStateList.valueOf(state.currentColor)
        
        // Update stroke width
        whiteboardView.setStrokeWidth(state.currentStrokeWidth)
        
        // Update drawing mode
        whiteboardView.setDrawingMode(state.isDrawingMode)
        
        // Update pagination
        updatePageIndicatorFromState(state)
        
        btnPreviousPage.isEnabled = state.canGoToPreviousPage
        btnPreviousPage.alpha = if (state.canGoToPreviousPage) 1.0f else 0.5f
        
        updateNextPageButtonFromState(state)
        
        btnDeletePage.isEnabled = state.canDeletePage
        btnDeletePage.alpha = if (state.canDeletePage) 1.0f else 0.5f
        
        // Update background
        whiteboardView.setBackgroundPattern(state.backgroundPattern)
        whiteboardView.setBackgroundColor(state.backgroundColor)
        
        // Handle page change event
        if (state.pageChangeEvent) {
            loadCurrentPageFromViewModel()
            viewModel.clearPageChangeEvent()
        }
        
        // Handle one-time events
        state.showTextInputDialog?.let { coords ->
            showTextInputDialog(coords.first, coords.second)
            viewModel.clearTextInputDialogEvent()
        }
        
        state.showToastMessage?.let { messageKey ->
            showToastByKey(messageKey)
            viewModel.clearToastMessageEvent()
        }
        
        state.errorMessage?.let { error ->
            Toast.makeText(this, error as CharSequence, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessageEvent()
        }
        
        state.exportSuccess?.let { message ->
            showExportSuccessMessage(message)
            viewModel.clearExportSuccessEvent()
        }
    }
    
    /**
     * Update tool button selection state
     */
    private fun updateToolButtonSelection(tool: DrawingTool) {
        btnPen.isSelected = false
        btnEraser.isSelected = false
        btnText.isSelected = false
        btnShapes.isSelected = false
        
        when (tool) {
            DrawingTool.PEN, DrawingTool.SELECTION -> btnPen.isSelected = true
            DrawingTool.ERASER, DrawingTool.SELECTIVE_ERASER -> btnEraser.isSelected = true
            DrawingTool.TEXT -> btnText.isSelected = true
            DrawingTool.LINE, DrawingTool.CIRCLE, DrawingTool.TRIANGLE, DrawingTool.RECTANGLE,
            DrawingTool.ELLIPSE, DrawingTool.DIAMOND, DrawingTool.STAR, DrawingTool.ARROW,
            DrawingTool.RIGHT_TRIANGLE, DrawingTool.ISOSCELES_TRIANGLE, DrawingTool.PARALLELOGRAM,
            DrawingTool.TRAPEZOID, DrawingTool.PENTAGON, DrawingTool.HEXAGON,
            DrawingTool.CUBE, DrawingTool.CYLINDER, DrawingTool.PYRAMID, DrawingTool.SPHERE, DrawingTool.CONE -> btnShapes.isSelected = true
        }
    }
    
    /**
     * Update page indicator text from state
     */
    private fun updatePageIndicatorFromState(state: WhiteboardViewState) {
        tvPageIndicator.text = getString(R.string.page_indicator_format, state.currentPageNumber, state.totalPages)
    }
    
    /**
     * Update next page button icon and state from state
     */
    private fun updateNextPageButtonFromState(state: WhiteboardViewState) {
        if (state.canGoToNextPage) {
            btnNextPage.setImageResource(R.drawable.ic_arrow_forward)
            btnNextPage.contentDescription = getString(R.string.desc_next_page)
            btnNextPage.isEnabled = true
            btnNextPage.alpha = 1.0f
        } else if (state.canCreateNewPage) {
            btnNextPage.setImageResource(R.drawable.ic_add)
            btnNextPage.contentDescription = getString(R.string.desc_add_page)
            btnNextPage.isEnabled = true
            btnNextPage.alpha = 1.0f
        } else {
            btnNextPage.setImageResource(R.drawable.ic_add)
            btnNextPage.contentDescription = getString(R.string.desc_add_page)
            btnNextPage.isEnabled = false
            btnNextPage.alpha = 0.5f
        }
    }
    
    /**
     * Save current page data to ViewModel
     */
    private fun saveCurrentPageToViewModel() {
        val currentPage = viewModel.getCurrentPage()
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
     * Load current page data from ViewModel
     */
    private fun loadCurrentPageFromViewModel() {
        val currentPage = viewModel.getCurrentPage()
        // Use the new method that accepts PageData directly
        // This will automatically set up the page's undo/redo manager
        whiteboardView.loadPageData(currentPage)
    }
    
    /**
     * Show text input dialog
     */
    private fun showTextInputDialog(x: Float, y: Float) {
        val editText = EditText(this)
        editText.hint = getString(R.string.enter_text)
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_text))
            .setView(editText)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                val text = editText.text.toString().trim()
                if (text.isNotEmpty()) {
                    whiteboardView.addText(text, x, y)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
        
        // Show keyboard automatically
        editText.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
    
    /**
     * Show clear confirmation dialog
     */
    private fun showClearConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_all))
            .setMessage(getString(R.string.clear_confirm))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                viewModel.clearCurrentPage()
                loadCurrentPageFromViewModel()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }
    
    /**
     * Show delete page confirmation dialog
     */
    private fun showDeletePageConfirmDialog() {
        if (viewModel.uiState.value?.totalPages ?: 1 <= 1) {
            Toast.makeText(this, getString(R.string.cannot_delete_last_page), Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_page_title))
            .setMessage(getString(R.string.delete_page_confirm))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                viewModel.deleteCurrentPage()
                loadCurrentPageFromViewModel()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }
    
    /**
     * Show export options dialog
     */
    private fun showExportOptionsDialog() {
        saveCurrentPageToViewModel()
        
        val options = arrayOf(
            getString(R.string.export_png),
            getString(R.string.export_moodle_format)
        )
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.export_title))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showPNGExportOptions()
                    1 -> requestStoragePermissionsForExport {
                        viewModel.exportToMoodleFormat(whiteboardView)
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    /**
     * Show PNG export options (current page or all pages)
     */
    private fun showPNGExportOptions() {
        // Request storage permissions before exporting
        requestStoragePermissionsForExport {
            if (viewModel.uiState.value?.totalPages ?: 1 > 1) {
                val options = arrayOf(
                    getString(R.string.export_current_page),
                    getString(R.string.export_all_pages)
                )
                
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.export_png))
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> viewModel.exportCurrentPageToPNG(whiteboardView)
                            1 -> viewModel.exportAllPagesToPNG(whiteboardView)
                        }
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            } else {
                viewModel.exportCurrentPageToPNG(whiteboardView)
            }
        }
    }
    
    /**
     * Show import options dialog
     */
    private fun showImportOptionsDialog() {
        val options = arrayOf(
            getString(R.string.import_image),
            getString(R.string.import_moodle_file)
        )
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.import_title))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openImagePicker()
                    1 -> showImportMoodleConfirmDialog()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    /**
     * Open image picker
     */
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        
        try {
            startActivityForResult(
                Intent.createChooser(intent, getString(R.string.select_image_file)),
                REQUEST_CODE_IMPORT_IMAGE
            )
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.no_file_manager), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Open background image picker
     */
    private fun openBackgroundImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        
        try {
            startActivityForResult(
                Intent.createChooser(intent, getString(R.string.upload_background_image)),
                REQUEST_CODE_BACKGROUND_IMAGE
            )
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.no_file_manager), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Handle background image selection
     */
    private fun handleBackgroundImageSelection(uri: android.net.Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap != null) {
                whiteboardView.setBackgroundImage(bitmap)
                Toast.makeText(this, getString(R.string.background_image_set), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.import_image_invalid), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.import_image_failed), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Show import Moodle file confirmation dialog
     */
    private fun showImportMoodleConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.import_moodle_file))
            .setMessage(getString(R.string.import_will_clear_board))
            .setPositiveButton(getString(R.string.continue_import)) { _, _ ->
                openMoodleFilePicker()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    /**
     * Open Moodle file picker
     */
    private fun openMoodleFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream", "text/plain"))
        }
        
        try {
            startActivityForResult(
                Intent.createChooser(intent, getString(R.string.select_moodle_file)),
                REQUEST_CODE_IMPORT_MOODLE
            )
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.no_file_manager), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Show toast message by resource key
     */
    private fun showToastByKey(key: String) {
        val stringRes = when (key) {
            "max_pages_reached" -> {
                Toast.makeText(this, getString(R.string.max_pages_reached, PageManager.MAX_PAGES), Toast.LENGTH_SHORT).show()
                return
            }
            "cannot_delete_last_page" -> R.string.cannot_delete_last_page
            "page_deleted" -> R.string.page_deleted
            else -> return
        }
        Toast.makeText(this, getString(stringRes), Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Show export success message
     */
    private fun showExportSuccessMessage(message: String) {
        if (message.startsWith("exported_") && message.endsWith("_pages")) {
            val count = message.substringAfter("exported_").substringBefore("_pages")
            Toast.makeText(this, getString(R.string.exported_pages, count), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Request storage permissions for export operations
     */
    private fun requestStoragePermissionsForExport(onGranted: () -> Unit) {
        // For Android 10+ (API 29+), MediaStore API doesn't require runtime permissions
        // But we still request them for older versions
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Android 10+ - No permission needed for MediaStore writes to Downloads
            onGranted()
        } else {
            // Below Android 10 - Need WRITE_EXTERNAL_STORAGE permission
            permissionManager.requestStoragePermissions(
                onGranted = onGranted,
                onDenied = { deniedPermissions ->
                    Toast.makeText(
                        this,
                        getString(R.string.storage_permission_required_for_export),
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
    
    /**
     * Apply custom fonts to all views
     */
    private fun applyFonts() {
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        FontHelper.applyFontToViewGroup(this, rootView)
    }
    
    /**
     * Open attendance screen
     */
    private fun openAttendanceScreen() {
        // startActivity(Intent(this, AttendanceActivity::class.java))
    }

    /**
     * Logout user
     */
    private fun logout() {
        userPreferences.clearAllData()
        Toast.makeText(this, getString(R.string.logged_out_successfully), Toast.LENGTH_SHORT).show()
        
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    /**
     * Toggle pinch and zoom mode
     * When enabled, users can only zoom/pan, drawing is disabled
     */
    private fun togglePinchMode() {
        isPinchModeActive = !isPinchModeActive
        whiteboardView.setPinchModeEnabled(isPinchModeActive)
        
        // Update button visual state
        btnPinchMode.isSelected = isPinchModeActive
    }
    
    /**
     * Toggle visibility of top and bottom toolbars with animation
     */
    private fun toggleToolbarsVisibility() {
        areToolbarsVisible = !areToolbarsVisible
        
        if (areToolbarsVisible) {
            showToolbar(topToolbar)
            showToolbar(bottomToolbar)
            btnToggleToolbars.setImageResource(R.drawable.ic_chevron_up)
        } else {
            hideToolbar(topToolbar)
            hideToolbar(bottomToolbar)
            btnToggleToolbars.setImageResource(R.drawable.ic_chevron_down)
        }
    }
    
    /**
     * Hide toolbar with fade and scale animation for modern glassy effect
     */
    private fun hideToolbar(toolbar: android.view.View) {
        toolbar.animate()
            .alpha(0f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .translationY(if (toolbar.id == R.id.topToolbar) -20f else 20f)
            .setDuration(250)
            .withStartAction {
                // Disable clicks on hidden toolbar
                toolbar.isClickable = false
                disableViewGroupClicks(toolbar)
            }
            .withEndAction {
                toolbar.visibility = android.view.View.GONE
            }
            .start()
    }
    
    /**
     * Show toolbar with fade and scale animation for modern glassy effect
     */
    private fun showToolbar(toolbar: android.view.View) {
        // Re-enable clicks
        toolbar.isClickable = true
        enableViewGroupClicks(toolbar)
        
        toolbar.visibility = android.view.View.VISIBLE
        toolbar.scaleX = 0.95f
        toolbar.scaleY = 0.95f
        toolbar.alpha = 0f
        
        toolbar.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(250)
            .start()
    }
    
    /**
     * Disable all clicks within a ViewGroup
     */
    private fun disableViewGroupClicks(view: android.view.View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                child.isClickable = false
                child.isFocusable = false
                if (child is ViewGroup) {
                    disableViewGroupClicks(child)
                }
            }
        }
    }
    
    /**
     * Enable all clicks within a ViewGroup
     */
    private fun enableViewGroupClicks(view: android.view.View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                child.isClickable = true
                child.isFocusable = true
                if (child is ViewGroup) {
                    enableViewGroupClicks(child)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_CODE_IMPORT_IMAGE -> {
                if (resultCode == RESULT_OK && data != null) {
                    data.data?.let { uri ->
                        viewModel.importImage(uri, whiteboardView) {
                            loadCurrentPageFromViewModel()
                        }
                    }
                }
            }
            REQUEST_CODE_IMPORT_MOODLE -> {
                if (resultCode == RESULT_OK && data != null) {
                    data.data?.let { uri ->
                        viewModel.importMoodleFile(uri, whiteboardView) {
                            loadCurrentPageFromViewModel()
                        }
                    }
                }
            }
            REQUEST_CODE_BACKGROUND_IMAGE -> {
                if (resultCode == RESULT_OK && data != null) {
                    data.data?.let { uri ->
                        handleBackgroundImageSelection(uri)
                    }
                }
            }
        }
    }
    
    /**
     * Perform text recognition on drawn strokes
     */
    private fun performTextRecognition() {
        // Save current page first
        saveCurrentPageToViewModel()
        
        val paths = whiteboardView.getAllPaths()
        
        if (paths.isEmpty()) {
            Toast.makeText(this, getString(R.string.text_recognition_no_strokes), Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show progress
        Toast.makeText(this, getString(R.string.text_recognition_processing), Toast.LENGTH_SHORT).show()
        
        textRecognitionManager.recognizeTextFromPaths(
            paths,
            object : com.example.moodle.ui.whiteboard.managers.TextRecognitionManager.TextRecognitionCallback {
                override fun onRecognitionSuccess(
                    textObjects: List<com.example.moodle.ui.whiteboard.models.TextObject>,
                    recognizedCount: Int
                ) {
                    runOnUiThread {
                        replaceDrawingWithRecognizedText(textObjects)
                    }
                }
                
                override fun onRecognitionFailure(error: String) {
                    runOnUiThread {
                        Toast.makeText(
                            this@WhiteboardActivity,
                            getString(R.string.text_recognition_failed) + ": $error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                
                override fun onRecognitionProgress(message: String) {
                    // Progress already shown
                }
            }
        )
    }
    
    /**
     * Replace drawn strokes with recognized text
     */
    private fun replaceDrawingWithRecognizedText(
        textObjects: List<com.example.moodle.ui.whiteboard.models.TextObject>
    ) {
        // Clear the drawing paths
        val currentPage = viewModel.getCurrentPage()
        currentPage.paths.clear()
        
        // Add recognized text objects
        for (textObject in textObjects) {
            currentPage.textObjects.add(textObject)
        }
        
        // Reload the page
        loadCurrentPageFromViewModel()
        
        Toast.makeText(
            this,
            getString(R.string.text_recognition_success, textObjects.size),
            Toast.LENGTH_SHORT
        ).show()
    }
    
    /**
     * Hide system bars (status bar and navigation bar) for immersive experience
     */
    private fun hideSystemBars() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11 (API 30) and above
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars())
                    controller.systemBarsBehavior = 
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                // Android 10 and below
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
            }
            
            // Set status bar color to transparent for smoother appearance
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        } catch (e: Exception) {
            // Log error but don't crash the app
            e.printStackTrace()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        recordingController.cleanup()
        popupController.cleanup()
        whiteboardView.cleanup()
        textRecognitionManager.cleanup()
    }
    
    companion object {
        private const val REQUEST_CODE_IMPORT_IMAGE = 2001
        private const val REQUEST_CODE_IMPORT_MOODLE = 2002
        private const val REQUEST_CODE_BACKGROUND_IMAGE = 2003
    }
}
