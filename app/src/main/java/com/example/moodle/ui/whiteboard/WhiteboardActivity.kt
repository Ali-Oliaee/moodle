package com.example.moodle.ui.whiteboard

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.ViewGroup
import android.widget.*
import com.example.moodle.R
import com.example.moodle.ui.attendance.AttendanceActivity
import com.example.moodle.data.datastore.UserPreferences
import com.example.moodle.ui.login.LoginActivity
import com.example.moodle.utils.FontHelper

/**
 * Main activity for the whiteboard functionality
 * Delegates responsibilities to specialized controllers
 */
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
    
    // Controllers
    private lateinit var recordingController: RecordingController
    private lateinit var toolsController: ToolsController
    private lateinit var popupController: PopupController
    private lateinit var paginationController: PaginationController
    private lateinit var importExportController: ImportExportController
    private lateinit var splitModeController: SplitModeController
    
    // Managers
    private lateinit var pageManager: PageManager
    private lateinit var exportManager: ExportManager
    private lateinit var importManager: ImportManager
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize preferences and managers
        userPreferences = UserPreferences(this)
        pageManager = PageManager(userPreferences)
        exportManager = ExportManager(this)
        importManager = ImportManager(this)
        
        // Set layout
        setContentView(R.layout.activity_whiteboard_split)
        
        // Initialize views
        initializeViews()
        
        // Initialize controllers
        initializeControllers()
        
        // Setup listeners
        setupListeners()
        
        // Apply fonts
        applyFonts()
        
        // Load saved settings
        toolsController.loadSavedBackgroundPattern()
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
    }
    
    /**
     * Initialize all controllers
     */
    private fun initializeControllers() {
        // Initialize Recording Controller
        recordingController = RecordingController(this)
        recordingController.initialize(toggleRecordBtn)
        
        // Initialize Tools Controller
        toolsController = ToolsController(this, whiteboardView, userPreferences)
        toolsController.initialize(
            pen = findViewById(R.id.btnPen),
            eraser = findViewById(R.id.btnEraser),
            text = findViewById(R.id.btnText),
            formula = findViewById(R.id.btnFormula),
            shapes = findViewById(R.id.btnShapes),
            undo = findViewById(R.id.btnUndo),
            redo = findViewById(R.id.btnRedo),
            clear = findViewById(R.id.btnClear),
            modeToggle = findViewById(R.id.btnModeToggle),
            colorPicker = findViewById(R.id.btnColorPicker),
            strokeWidth = findViewById(R.id.btnStrokeWidth),
            background = findViewById(R.id.btnBackground)
        )
        
        // Initialize Popup Controller
        popupController = PopupController(this, whiteboardView)
        
        // Initialize Pagination Controller
        paginationController = PaginationController(this, whiteboardView, pageManager)
        paginationController.initialize(
            previousPage = findViewById(R.id.btnPreviousPage),
            nextPage = findViewById(R.id.btnNextPage),
            deletePage = findViewById(R.id.btnDeletePage),
            pageIndicator = findViewById(R.id.tvPageIndicator)
        )
        
        // Initialize Import/Export Controller
        importExportController = ImportExportController(this, whiteboardView, exportManager, importManager)
        
        // Initialize Split Mode Controller
        splitModeController = SplitModeController(this, whiteboardView)
        splitModeController.initialize(
            enableSplit = findViewById(R.id.btnEnableSplit),
            disableSplit = findViewById(R.id.btnDisableSplit),
            toolbar = findViewById(R.id.bottomToolbar)
        )
    }
    
    /**
     * Setup all listeners and callbacks
     */
    private fun setupListeners() {
        // Top bar listeners
        recordContainer.setOnClickListener { recordingController.toggleRecording() }
        btnAttendance.setOnClickListener { openAttendanceScreen() }
        btnLogout.setOnClickListener { logout() }
        btnExport.setOnClickListener { importExportController.showExportOptionsDialog() }
        btnImport.setOnClickListener { importExportController.showImportOptionsDialog() }
        
        // Tool listeners
        setupToolListeners()
        
        // Popup controller callbacks
        setupPopupControllerCallbacks()
        
        // Tools controller callbacks
        setupToolsControllerCallbacks()
        
        // Pagination controller callbacks
        setupPaginationControllerCallbacks()
        
        // Import/Export controller callbacks
        setupImportExportControllerCallbacks()
        
        // Split mode controller callbacks
        setupSplitModeControllerCallbacks()
        
        // Whiteboard listeners
        setupWhiteboardListeners()
        
        // Zoom button
        btnPinchMode.setOnClickListener { 
            popupController.showZoomPopup(btnPinchMode)
        }
    }
    
    /**
     * Setup tool button listeners
     */
    private fun setupToolListeners() {
        toolsController.getPenButton().setOnClickListener {
            toolsController.togglePenSelectionMode()
        }
        
        toolsController.getEraserButton().setOnClickListener {
            popupController.showEraserModePopup(toolsController.getEraserButton())
        }
        
        toolsController.getTextButton().setOnClickListener {
            toolsController.selectTool(DrawingTool.TEXT, toolsController.getTextButton())
        }
        
        toolsController.getFormulaButton().setOnClickListener {
            popupController.showFormulaPopup()
        }
        
        toolsController.getShapesButton().setOnClickListener {
            popupController.showShapesPopup(toolsController.getShapesButton())
        }
        
        findViewById<ImageButton>(R.id.btnUndo).setOnClickListener {
            paginationController.undoCurrentPage()
            updateToolButtons()
        }
        
        findViewById<ImageButton>(R.id.btnRedo).setOnClickListener {
            paginationController.redoCurrentPage()
                updateToolButtons()
            }
        
        findViewById<ImageButton>(R.id.btnClear).setOnClickListener {
            toolsController.showClearConfirmDialog()
        }
        
        findViewById<ImageButton>(R.id.btnModeToggle).setOnClickListener {
            toolsController.toggleMode()
        }
        
        toolsController.getColorPickerButton().setOnClickListener {
            popupController.showColorPickerPopup(
                toolsController.getColorPickerButton(),
                toolsController.getCurrentColor()
            )
        }
        
        toolsController.getStrokeWidthButton().setOnClickListener {
            popupController.showStrokeWidthPopup(
                toolsController.getStrokeWidthButton(),
                toolsController.getCurrentStrokeWidth()
            )
        }
        
        toolsController.getBackgroundButton().setOnClickListener {
            popupController.showBackgroundPatternPopup()
        }
    }
    
    /**
     * Setup popup controller callbacks
     */
    private fun setupPopupControllerCallbacks() {
        popupController.onColorSelected = { color ->
            toolsController.selectColor(color)
            pageManager.updateSharedColor(color)
        }
        
        popupController.onStrokeWidthChanged = { width ->
            toolsController.setStrokeWidth(width)
            pageManager.updateSharedStrokeWidth(width)
        }
        
        popupController.onBackgroundPatternSelected = { pattern ->
            toolsController.selectBackgroundPattern(pattern)
            pageManager.updateSharedBackgroundPattern(pattern)
        }
        
        popupController.onToolSelected = { tool ->
            val button = when (tool) {
                DrawingTool.ERASER -> toolsController.getEraserButton()
                DrawingTool.SELECTIVE_ERASER -> toolsController.getEraserButton()
                DrawingTool.LINE,
                DrawingTool.CIRCLE,
                DrawingTool.TRIANGLE,
                DrawingTool.RECTANGLE -> toolsController.getShapesButton()
                else -> toolsController.getPenButton()
            }
            toolsController.selectTool(tool, button)
        }
        
        popupController.onEraserModeSelected = { isSelective ->
            toolsController.setSelectiveEraserMode(isSelective)
        }
    }
    
    /**
     * Setup tools controller callbacks
     */
    private fun setupToolsControllerCallbacks() {
        toolsController.onClearRequested = {
            paginationController.clearCurrentPage()
            updateToolButtons()
        }
        
        toolsController.onColorChanged = { color ->
            pageManager.updateSharedColor(color)
        }
        
        toolsController.onStrokeWidthChanged = { width ->
            pageManager.updateSharedStrokeWidth(width)
        }
    }
    
    /**
     * Setup pagination controller callbacks
     */
    private fun setupPaginationControllerCallbacks() {
        paginationController.onPageChanged = {
            // Update shared settings when page changes
            whiteboardView.setColor(pageManager.sharedColor)
            whiteboardView.setStrokeWidth(pageManager.sharedStrokeWidth)
            toolsController.selectColor(pageManager.sharedColor)
            toolsController.setStrokeWidth(pageManager.sharedStrokeWidth)
            updateToolButtons()
        }
    }
    
    /**
     * Setup import/export controller callbacks
     */
    private fun setupImportExportControllerCallbacks() {
        importExportController.onPageSaveRequired = {
            paginationController.saveCurrentPageFromWhiteboard()
        }
        
        importExportController.getPageManager = {
            pageManager
        }
        
        importExportController.onImportSuccess = {
            paginationController.updatePaginationUI()
            paginationController.loadCurrentPageToWhiteboard()
            updateToolButtons()
        }
    }
    
    /**
     * Setup split mode controller callbacks
     */
    private fun setupSplitModeControllerCallbacks() {
        splitModeController.onSplitModeChanged = { enabled ->
            if (enabled) {
                popupController.dismissZoomPopup()
            }
        }
    }
    
    /**
     * Setup whiteboard listeners
     */
    private fun setupWhiteboardListeners() {
        whiteboardView.setOnDrawingChangeListener(object : OnDrawingChangeListener {
            override fun onDrawingChanged() {
                paginationController.saveCurrentPageFromWhiteboard()
        updateToolButtons()
    }
        })
        
        whiteboardView.setOnTextInputListener(object : OnTextInputListener {
            override fun onTextInputRequested(x: Float, y: Float) {
                toolsController.showTextInputDialog(x, y)
            }
        })
    }
    
    /**
     * Update tool button states
     */
    private fun updateToolButtons() {
        val (canUndo, canRedo) = paginationController.getCurrentPageUndoRedoState()
        toolsController.updateUndoRedoButtons(canUndo, canRedo)
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
        val intent = Intent(this, AttendanceActivity::class.java)
        startActivity(intent)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        // Delegate to controllers
        if (!recordingController.handlePermissionsResult(requestCode, grantResults)) {
            importExportController.handlePermissionsResult(requestCode, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // Delegate to controllers
        if (!recordingController.handleActivityResult(requestCode, resultCode, data)) {
            importExportController.handleActivityResult(requestCode, resultCode, data)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up all controllers
        recordingController.cleanup()
        popupController.cleanup()
        whiteboardView.cleanup()
    }
}
