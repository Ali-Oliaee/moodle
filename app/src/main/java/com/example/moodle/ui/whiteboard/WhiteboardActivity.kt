package com.example.moodle.ui.whiteboard

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.PopupWindow
import com.example.moodle.R
import com.example.moodle.ui.attendance.AttendanceActivity
import com.example.moodle.data.datastore.UserPreferences
import com.example.moodle.ui.login.LoginActivity
import com.example.moodle.ui.whiteboard.ScreenRecordService
import com.example.moodle.utils.FontHelper

class WhiteboardActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_SCREEN_CAPTURE = 1000
        private const val REQUEST_CODE_PERMISSIONS = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )
    }

    // UI Components
    private lateinit var toggleRecordBtn: Button
    private lateinit var whiteboardView: WhiteboardView
    private lateinit var recordContainer: LinearLayout
    private lateinit var btnAttendance: ImageButton
    private lateinit var btnLogout: ImageButton
    
    // Split mode components
    private lateinit var btnEnableSplit: Button
    private lateinit var btnDisableSplit: Button
    private lateinit var bottomToolbar: LinearLayout
    
    private var isSplitMode = false
    
    // Tool buttons
    private lateinit var btnPen: ImageButton
    private lateinit var btnEraser: ImageButton
    private lateinit var btnHighlighter: ImageButton
    private lateinit var btnText: ImageButton
    private lateinit var btnFormula: ImageButton
    private lateinit var btnShapes: ImageButton
    private lateinit var btnUndo: ImageButton
    private lateinit var btnRedo: ImageButton
    private lateinit var btnClear: ImageButton
    private lateinit var btnModeToggle: ImageButton
    private lateinit var btnPinchMode: ImageButton
    private lateinit var btnColorPicker: Button
    private lateinit var btnStrokeWidth: ImageButton
    private lateinit var btnBackground: ImageButton
    
    // Pagination components
    private lateinit var btnPreviousPage: ImageButton
    private lateinit var btnNextPage: ImageButton
    private lateinit var btnDeletePage: ImageButton
    private lateinit var tvPageIndicator: TextView
    private lateinit var pageManager: PageManager
    
    // Popup views
    private var colorPopupWindow: PopupWindow? = null
    private var strokePopupWindow: PopupWindow? = null
    private var eraserModePopupWindow: PopupWindow? = null
    private var formulaPopupWindow: PopupWindow? = null
    private var backgroundPopupWindow: PopupWindow? = null
    private var currentStrokeWidth: Float = 8f
    
    // State
    private var isRecording = false
    private var isDrawingMode = true
    private var currentSelectedTool: ImageButton? = null
    private var currentSelectedColor: Int = Color.BLACK
    private var isSelectiveEraserMode = false
    private lateinit var userPreferences: UserPreferences
    
    private val mediaProjectionManager by lazy {
        getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private val recordingStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "RECORDING_STARTED" -> {
                    isRecording = true
                    updateButtonAppearance()
                }
                "RECORDING_STOPPED" -> {
                    isRecording = false
                    updateButtonAppearance()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize UserPreferences
        userPreferences = UserPreferences(this)
        
        // Initialize PageManager
        pageManager = PageManager(userPreferences)
        
        // Set the split-enabled layout
        setContentView(R.layout.activity_whiteboard_split)
        
        initializeViews()
        applyFonts()
        setupToolListeners()
        setupPopupListeners()
        setupWhiteboardListener()
        registerRecordingStateReceiver()
        updateButtonAppearance()
        updateToolButtons()
        loadSavedBackgroundPattern()
    }

    private fun initializeViews() {
        // Main UI components
        whiteboardView = findViewById(R.id.whiteboardView)
        toggleRecordBtn = findViewById(R.id.toggleRecordBtn)
        recordContainer = findViewById(R.id.recordContainer)
        btnAttendance = findViewById(R.id.btnAttendance)
        btnLogout = findViewById(R.id.btnLogout)
        
        // Tool buttons
        btnPen = findViewById(R.id.btnPen)
        btnEraser = findViewById(R.id.btnEraser)
        btnHighlighter = findViewById(R.id.btnHighlighter)
        btnText = findViewById(R.id.btnText)
        btnFormula = findViewById(R.id.btnFormula)
        btnShapes = findViewById(R.id.btnShapes)
        btnUndo = findViewById(R.id.btnUndo)
        btnRedo = findViewById(R.id.btnRedo)
        btnClear = findViewById(R.id.btnClear)
        btnModeToggle = findViewById(R.id.btnModeToggle)
        btnPinchMode = findViewById(R.id.btnPinchMode)
        btnColorPicker = findViewById(R.id.btnColorPicker)
        btnStrokeWidth = findViewById(R.id.btnStrokeWidth)
        btnBackground = findViewById(R.id.btnBackground)
        
        // Pagination components
        btnPreviousPage = findViewById(R.id.btnPreviousPage)
        btnNextPage = findViewById(R.id.btnNextPage)
        btnDeletePage = findViewById(R.id.btnDeletePage)
        tvPageIndicator = findViewById(R.id.tvPageIndicator)
        
        // Split mode components
        btnEnableSplit = findViewById(R.id.btnEnableSplit)
        btnDisableSplit = findViewById(R.id.btnDisableSplit)
        bottomToolbar = findViewById(R.id.bottomToolbar)
        
        setupSplitModeControls()
        setupPaginationControls()
        
        // Set welcome message with username
        val username = userPreferences.getUsername()
        val welcomeMessage = if (!username.isNullOrBlank()) {
            getString(R.string.welcome_user_message, username)
        } else {
            getString(R.string.welcome_default_message)
        }
        
        // Set initial selections
        currentSelectedColor = Color.BLACK
        btnColorPicker.backgroundTintList = android.content.res.ColorStateList.valueOf(currentSelectedColor)
        currentSelectedTool = btnPen
        btnPen.isSelected = true
        
        // Set initial stroke width
        whiteboardView.setStrokeWidth(currentStrokeWidth)
        
        setupTopBarListeners()
    }
    
    private fun setupSplitModeControls() {
        // Setup split mode listeners
        btnEnableSplit.setOnClickListener {
            showSplitModeDialog()
        }
        
        btnDisableSplit.setOnClickListener {
            disableSplitMode()
        }
    }
    
    private fun setupPaginationControls() {
        // Setup page manager listener
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
        
        // Setup pagination button listeners
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
                Toast.makeText(this, getString(R.string.max_pages_reached, PageManager.MAX_PAGES), Toast.LENGTH_SHORT).show()
            }
        }
        
        btnDeletePage.setOnClickListener {
            showDeletePageConfirmDialog()
        }
        
        // Initialize pagination UI
        updatePaginationUI()
        loadCurrentPageToWhiteboard()
    }
    
    private fun updatePaginationUI() {
        val currentPageId = pageManager.getCurrentPageId()
        val totalPages = pageManager.getTotalPages()
        val pageIds = pageManager.getPageIds()
        val currentIndex = pageIds.indexOf(currentPageId) + 1
        
        // Update page indicator
        tvPageIndicator.text = getString(R.string.page_indicator_format, currentIndex, totalPages)
        
        // Update previous button state
        btnPreviousPage.isEnabled = pageManager.canGoToPreviousPage()
        btnPreviousPage.alpha = if (pageManager.canGoToPreviousPage()) 1.0f else 0.5f
        
        // Update next button icon and state
        if (pageManager.canGoToNextPage()) {
            btnNextPage.setImageResource(R.drawable.ic_arrow_forward)
            btnNextPage.contentDescription = getString(R.string.desc_next_page)
        } else if (pageManager.canCreateNewPage()) {
            btnNextPage.setImageResource(R.drawable.ic_add)
            btnNextPage.contentDescription = getString(R.string.desc_add_page)
        } else {
            btnNextPage.setImageResource(R.drawable.ic_add)
            btnNextPage.contentDescription = getString(R.string.desc_add_page)
            btnNextPage.alpha = 0.5f
        }
        
        if (pageManager.canGoToNextPage() || pageManager.canCreateNewPage()) {
            btnNextPage.alpha = 1.0f
        }
        
        // Update delete button state (disable if only one page)
        btnDeletePage.isEnabled = pageManager.getTotalPages() > 1
        btnDeletePage.alpha = if (pageManager.getTotalPages() > 1) 1.0f else 0.5f
    }
    
    private fun saveCurrentPageFromWhiteboard() {
        val currentPage = pageManager.getCurrentPage()
        
        // Save current whiteboard state to the page
        currentPage.paths.clear()
        currentPage.paths.addAll(whiteboardView.getAllPaths())
        
        currentPage.textObjects.clear()
        currentPage.textObjects.addAll(whiteboardView.getAllTextObjects())
        
        currentPage.shapeObjects.clear()
        currentPage.shapeObjects.addAll(whiteboardView.getAllShapeObjects())
        
        currentPage.backgroundPattern = whiteboardView.getBackgroundPattern()
    }
    
    private fun loadCurrentPageToWhiteboard() {
        val currentPage = pageManager.getCurrentPage()
        
        // Load page state to whiteboard
        whiteboardView.loadPageData(currentPage.paths, currentPage.textObjects, currentPage.shapeObjects)
        whiteboardView.setBackgroundPattern(currentPage.backgroundPattern)
        
        // Apply shared settings
        whiteboardView.setColor(pageManager.sharedColor)
        whiteboardView.setStrokeWidth(pageManager.sharedStrokeWidth)
        
        // Update UI to reflect shared settings
        currentSelectedColor = pageManager.sharedColor
        btnColorPicker.backgroundTintList = android.content.res.ColorStateList.valueOf(currentSelectedColor)
        currentStrokeWidth = pageManager.sharedStrokeWidth
    }
    
    private fun showSplitModeDialog() {
        val sectionCounts = arrayOf(
            getString(R.string.section_count_2),
            getString(R.string.section_count_3),
            getString(R.string.section_count_4)
        )
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.choose_section_count))
            .setItems(sectionCounts) { _, which ->
                val sectionCount = when (which) {
                    0 -> 2
                    1 -> 3
                    2 -> 4
                    else -> 2
                }
                enableSplitMode(sectionCount)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun setupTopBarListeners() {
        // Record button listener
        recordContainer.setOnClickListener {
            if (isRecording) stopRecording()
            else startRecording()
        }

        // Attendance button listener
        btnAttendance.setOnClickListener {
            openAttendanceScreen()
        }

        // Logout button listener
        btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun applyFonts() {
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        FontHelper.applyFontToViewGroup(this, rootView)
    }
    
    private fun setupToolListeners() {
        // Tool selection listeners
        btnPen.setOnClickListener { selectTool(WhiteboardView.DrawingTool.PEN, btnPen) }
        btnEraser.setOnClickListener { showEraserModePopup() }
        btnHighlighter.setOnClickListener { selectTool(WhiteboardView.DrawingTool.HIGHLIGHTER, btnHighlighter) }
        btnText.setOnClickListener { selectTool(WhiteboardView.DrawingTool.TEXT, btnText) }
        btnFormula.setOnClickListener { showFormulaPopup() }
        btnShapes.setOnClickListener { showShapesPopup() }
        
        // Action listeners
        btnUndo.setOnClickListener { 
            if (pageManager.undoCurrentPage()) {
                loadCurrentPageToWhiteboard()
            }
        }
        btnRedo.setOnClickListener { 
            if (pageManager.redoCurrentPage()) {
                loadCurrentPageToWhiteboard()
            }
        }
        btnClear.setOnClickListener { showClearConfirmDialog() }
        btnModeToggle.setOnClickListener { toggleMode() }
        btnPinchMode.setOnClickListener { togglePinchMode() }
    }
    
    private fun setupPopupListeners() {
        // Color picker popup
        btnColorPicker.setOnClickListener {
            showColorPickerPopup()
        }
        
        // Stroke width popup
        btnStrokeWidth.setOnClickListener {
            showStrokeWidthPopup()
        }
        
        // Background pattern popup
        btnBackground.setOnClickListener {
            showBackgroundPatternPopup()
        }
    }
    
    private fun showColorPickerPopup() {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_color_picker, null)
        
        colorPopupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        
        // Set up color buttons in popup
        val colors = mapOf(
            R.id.colorBlack to Color.BLACK,
            R.id.colorRed to Color.parseColor("#F44336"),
            R.id.colorBlue to Color.parseColor("#2196F3"),
            R.id.colorGreen to Color.parseColor("#4CAF50"),
            R.id.colorYellow to Color.parseColor("#FFEB3B"),
            R.id.colorPurple to Color.parseColor("#9C27B0"),
            R.id.colorOrange to Color.parseColor("#FF9800"),
            R.id.colorWhite to Color.WHITE
        )
        
        colors.forEach { (buttonId, color) ->
            popupView.findViewById<Button>(buttonId)?.setOnClickListener {
                selectColor(color)
                colorPopupWindow?.dismiss()
            }
        }
        
        // Show popup above the color picker button
        colorPopupWindow?.showAsDropDown(btnColorPicker, 0, -btnColorPicker.height - 200, Gravity.CENTER)
    }
    
    private fun showStrokeWidthPopup() {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_stroke_width, null)
        
        strokePopupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        
        val seekBar = popupView.findViewById<SeekBar>(R.id.strokeWidthSeekBar)
        
        // Set current values
        seekBar.progress = (currentStrokeWidth - 1).toInt()
        
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val strokeWidth = (progress + 1).toFloat() // Min 1, Max 21
                
                if (fromUser) {
                    currentStrokeWidth = strokeWidth
                    pageManager.updateSharedStrokeWidth(strokeWidth)
                    whiteboardView.setStrokeWidth(strokeWidth)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Show popup above the stroke width button
        strokePopupWindow?.showAsDropDown(btnStrokeWidth, 0, -btnStrokeWidth.height - 150, Gravity.CENTER)
    }
    
    private fun showBackgroundPatternPopup() {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_background_patterns, null)
        
        backgroundPopupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        
        // Set up preview views
        val previewGridSmall = popupView.findViewById<BackgroundPreviewView>(R.id.previewGridSmall)
        val previewGridLarge = popupView.findViewById<BackgroundPreviewView>(R.id.previewGridLarge)
        val previewLinesHorizontal = popupView.findViewById<BackgroundPreviewView>(R.id.previewLinesHorizontal)
        val previewLinesVertical = popupView.findViewById<BackgroundPreviewView>(R.id.previewLinesVertical)
        val previewLinesBoth = popupView.findViewById<BackgroundPreviewView>(R.id.previewLinesBoth)
        val previewDotGrid = popupView.findViewById<BackgroundPreviewView>(R.id.previewDotGrid)
        val previewGraphPaper = popupView.findViewById<BackgroundPreviewView>(R.id.previewGraphPaper)
        
        // Set patterns for preview views
        previewGridSmall.setBackgroundPattern(BackgroundPattern.GRID_SMALL)
        previewGridLarge.setBackgroundPattern(BackgroundPattern.GRID_LARGE)
        previewLinesHorizontal.setBackgroundPattern(BackgroundPattern.LINES_HORIZONTAL)
        previewLinesVertical.setBackgroundPattern(BackgroundPattern.LINES_VERTICAL)
        previewLinesBoth.setBackgroundPattern(BackgroundPattern.LINES_BOTH)
        previewDotGrid.setBackgroundPattern(BackgroundPattern.DOT_GRID)
        previewGraphPaper.setBackgroundPattern(BackgroundPattern.GRAPH_PAPER)
        
        // Set up click listeners
        popupView.findViewById<FrameLayout>(R.id.backgroundPlain).setOnClickListener {
            selectBackgroundPattern(BackgroundPattern.PLAIN)
            backgroundPopupWindow?.dismiss()
        }
        
        popupView.findViewById<FrameLayout>(R.id.backgroundGridSmall).setOnClickListener {
            selectBackgroundPattern(BackgroundPattern.GRID_SMALL)
            backgroundPopupWindow?.dismiss()
        }
        
        popupView.findViewById<FrameLayout>(R.id.backgroundGridLarge).setOnClickListener {
            selectBackgroundPattern(BackgroundPattern.GRID_LARGE)
            backgroundPopupWindow?.dismiss()
        }
        
        popupView.findViewById<FrameLayout>(R.id.backgroundLinesHorizontal).setOnClickListener {
            selectBackgroundPattern(BackgroundPattern.LINES_HORIZONTAL)
            backgroundPopupWindow?.dismiss()
        }
        
        popupView.findViewById<FrameLayout>(R.id.backgroundLinesVertical).setOnClickListener {
            selectBackgroundPattern(BackgroundPattern.LINES_VERTICAL)
            backgroundPopupWindow?.dismiss()
        }
        
        popupView.findViewById<FrameLayout>(R.id.backgroundLinesBoth).setOnClickListener {
            selectBackgroundPattern(BackgroundPattern.LINES_BOTH)
            backgroundPopupWindow?.dismiss()
        }
        
        popupView.findViewById<FrameLayout>(R.id.backgroundDotGrid).setOnClickListener {
            selectBackgroundPattern(BackgroundPattern.DOT_GRID)
            backgroundPopupWindow?.dismiss()
        }
        
        popupView.findViewById<FrameLayout>(R.id.backgroundGraphPaper).setOnClickListener {
            selectBackgroundPattern(BackgroundPattern.GRAPH_PAPER)
            backgroundPopupWindow?.dismiss()
        }
        
        backgroundPopupWindow?.showAtLocation(whiteboardView, Gravity.CENTER, 0, 0)
    }
    
    private fun selectBackgroundPattern(pattern: BackgroundPattern) {
        pageManager.updateSharedBackgroundPattern(pattern)
        whiteboardView.setBackgroundPattern(pattern)
        userPreferences.saveBackgroundPattern(pattern.id)
    }
    
    private fun loadSavedBackgroundPattern() {
        val savedPatternId = userPreferences.getBackgroundPattern()
        val pattern = BackgroundPattern.fromId(savedPatternId)
        whiteboardView.setBackgroundPattern(pattern)
    }
    
    private fun setupWhiteboardListener() {
        whiteboardView.setOnDrawingChangeListener(object : WhiteboardView.OnDrawingChangeListener {
            override fun onDrawingChanged() {
                // Save current drawing state to the current page
                saveCurrentPageFromWhiteboard()
                updateToolButtons()
            }
        })
        
        whiteboardView.setOnTextInputListener(object : WhiteboardView.OnTextInputListener {
            override fun onTextInputRequested(x: Float, y: Float) {
                showTextInputDialog(x, y)
            }
        })
    }
    
    private fun showTextInputDialog(x: Float, y: Float) {
        val editText = EditText(this)
        editText.hint = "متن خود را وارد کنید"
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("افزودن متن")
            .setView(editText)
            .setPositiveButton("تایید") { _, _ ->
                val text = editText.text.toString().trim()
                if (text.isNotEmpty()) {
                    whiteboardView.addText(text, x, y)
                }
            }
            .setNegativeButton("لغو", null)
            .create()
            
        dialog.show()
        
        // Show keyboard automatically
        editText.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
    
    private fun showEraserModePopup() {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_eraser_mode, null)
        
        eraserModePopupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        
        // Normal eraser option
        val normalEraserOption = popupView.findViewById<ImageButton>(R.id.normalEraserOption)
        normalEraserOption.setOnClickListener {
            selectTool(WhiteboardView.DrawingTool.ERASER, btnEraser)
            isSelectiveEraserMode = false
            eraserModePopupWindow?.dismiss()
            Toast.makeText(this, "پاک کن معمولی انتخاب شد", Toast.LENGTH_SHORT).show()
        }
        
        // Selective eraser option
        val selectiveEraserOption = popupView.findViewById<ImageButton>(R.id.selectiveEraserOption)
        selectiveEraserOption.setOnClickListener {
            selectTool(WhiteboardView.DrawingTool.SELECTIVE_ERASER, btnEraser)
            isSelectiveEraserMode = true
            eraserModePopupWindow?.dismiss()
            Toast.makeText(this, "پاک کن انتخابی انتخاب شد", Toast.LENGTH_SHORT).show()
        }
        
        // Show popup above the eraser button
        eraserModePopupWindow?.showAsDropDown(btnEraser, 0, -btnEraser.height - 200, Gravity.CENTER)
    }

    // Data class for formula templates
    data class FormulaTemplate(
        val name: String,
        val template: String,
        val variables: List<String>,
        val descriptions: List<String>
    )

    private fun getFormulaTemplates(): List<FormulaTemplate> {
        return listOf(
            FormulaTemplate(
                "Quadratic Formula",
                "x = (-{b} ± √({b}²-4{a}{c})) / 2{a}",
                listOf("a", "b", "c"),
                listOf("ضریب x²", "ضریب x", "عدد ثابت")
            ),
            FormulaTemplate(
                "Pythagorean Theorem",
                "{a}² + {b}² = {c}²",
                listOf("a", "b", "c"),
                listOf("ضلع اول", "ضلع دوم", "وتر")
            ),
            FormulaTemplate(
                "Circle Area",
                "A = π{r}²",
                listOf("r"),
                listOf("شعاع")
            ),
            FormulaTemplate(
                "Distance Formula",
                "d = √(({x2}-{x1})² + ({y2}-{y1})²)",
                listOf("x1", "y1", "x2", "y2"),
                listOf("x نقطه اول", "y نقطه اول", "x نقطه دوم", "y نقطه دوم")
            ),
            FormulaTemplate(
                "Slope Formula",
                "m = ({y2}-{y1}) / ({x2}-{x1})",
                listOf("x1", "y1", "x2", "y2"),
                listOf("x نقطه اول", "y نقطه اول", "x نقطه دوم", "y نقطه دوم")
            ),
            FormulaTemplate(
                "Sphere Volume",
                "V = (4/3)π{r}³",
                listOf("r"),
                listOf("شعاع")
            ),
            FormulaTemplate(
                "Compound Interest",
                "A = {P}(1 + {r}/{n})^({n}{t})",
                listOf("P", "r", "n", "t"),
                listOf("سرمایه اولیه", "نرخ بهره", "تعداد دفعات", "زمان")
            ),
            FormulaTemplate(
                "Law of Cosines",
                "{c}² = {a}² + {b}² - 2{a}{b} cos({C})",
                listOf("a", "b", "c", "C"),
                listOf("ضلع اول", "ضلع دوم", "ضلع سوم", "زاویه")
            )
        )
    }

    private fun showFormulaPopup() {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_math_formulas, null)
        
        formulaPopupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        
        val formulas = getFormulaTemplates()
        
        // Set up formula option listeners
        popupView.findViewById<LinearLayout>(R.id.formulaQuadratic).setOnClickListener {
            formulaPopupWindow?.dismiss()
            showFormulaInputDialog(formulas[0])
        }
        
        popupView.findViewById<LinearLayout>(R.id.formulaPythagorean).setOnClickListener {
            formulaPopupWindow?.dismiss()
            showFormulaInputDialog(formulas[1])
        }
        
        popupView.findViewById<LinearLayout>(R.id.formulaCircleArea).setOnClickListener {
            formulaPopupWindow?.dismiss()
            showFormulaInputDialog(formulas[2])
        }
        
        popupView.findViewById<LinearLayout>(R.id.formulaDistance).setOnClickListener {
            formulaPopupWindow?.dismiss()
            showFormulaInputDialog(formulas[3])
        }
        
        popupView.findViewById<LinearLayout>(R.id.formulaSlope).setOnClickListener {
            formulaPopupWindow?.dismiss()
            showFormulaInputDialog(formulas[4])
        }
        
        popupView.findViewById<LinearLayout>(R.id.formulaSphereVolume).setOnClickListener {
            formulaPopupWindow?.dismiss()
            showFormulaInputDialog(formulas[5])
        }
        
        popupView.findViewById<LinearLayout>(R.id.formulaCompoundInterest).setOnClickListener {
            formulaPopupWindow?.dismiss()
            showFormulaInputDialog(formulas[6])
        }
        
        popupView.findViewById<LinearLayout>(R.id.formulaCosines).setOnClickListener {
            formulaPopupWindow?.dismiss()
            showFormulaInputDialog(formulas[7])
        }
        
        // Show popup above the formula button
        formulaPopupWindow?.showAsDropDown(btnFormula, 0, -btnFormula.height - 450, Gravity.CENTER)
    }

    private fun showFormulaInputDialog(formula: FormulaTemplate) {
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_formula_input, null)
        
        val formulaDisplay = dialogView.findViewById<TextView>(R.id.formulaDisplay)
        val inputContainer = dialogView.findViewById<LinearLayout>(R.id.inputFieldsContainer)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnApply = dialogView.findViewById<Button>(R.id.btnApply)
        
        // Display the formula template
        formulaDisplay.text = formula.template.replace(Regex("\\{[^}]+\\}"), "_")
        
        // Create input fields for each variable
        val inputFields = mutableMapOf<String, EditText>()
        
        for (i in formula.variables.indices) {
            val variable = formula.variables[i]
            val description = formula.descriptions[i]
            
            // Create label
            val label = TextView(this)
            label.text = "$description ($variable):"
            label.textSize = 14f
            label.setTextColor(android.graphics.Color.parseColor("#333333"))
            label.setPadding(0, 16, 0, 8)
            
            // Create input field
            val editText = EditText(this)
            editText.hint = "مقدار $variable را وارد کنید"
            editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            editText.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            inputContainer.addView(label)
            inputContainer.addView(editText)
            inputFields[variable] = editText
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnApply.setOnClickListener {
            // Get values from input fields
            val values = mutableMapOf<String, String>()
            var allFieldsFilled = true
            
            for ((variable, editText) in inputFields) {
                val value = editText.text.toString().trim()
                if (value.isEmpty()) {
                    allFieldsFilled = false
                    editText.error = "این فیلد الزامی است"
                } else {
                    values[variable] = value
                }
            }
            
            if (allFieldsFilled) {
                // Replace placeholders with actual values
                var finalFormula = formula.template
                for ((variable, value) in values) {
                    finalFormula = finalFormula.replace("{$variable}", value)
                }
                
                // Add the formula as text to the whiteboard
                // Position it at the center of the screen
                val centerX = whiteboardView.width / 2f
                val centerY = whiteboardView.height / 2f
                whiteboardView.addText(finalFormula, centerX, centerY)
                
                dialog.dismiss()
                Toast.makeText(this, "فرمول به تخته اضافه شد", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.show()
    }
    
    private fun showShapesPopup() {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_shapes, null)
        
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        
        // Shape buttons in popup
        val btnLinePopup = popupView.findViewById<ImageButton>(R.id.btnLinePopup)
        val btnCirclePopup = popupView.findViewById<ImageButton>(R.id.btnCirclePopup)
        val btnTrianglePopup = popupView.findViewById<ImageButton>(R.id.btnTrianglePopup)
        val btnRectanglePopup = popupView.findViewById<ImageButton>(R.id.btnRectanglePopup)
        
        // Set click listeners
        btnLinePopup.setOnClickListener {
            selectTool(WhiteboardView.DrawingTool.LINE, btnShapes)
            popupWindow.dismiss()
        }
        
        btnCirclePopup.setOnClickListener {
            selectTool(WhiteboardView.DrawingTool.CIRCLE, btnShapes)
            popupWindow.dismiss()
        }
        
        btnTrianglePopup.setOnClickListener {
            selectTool(WhiteboardView.DrawingTool.TRIANGLE, btnShapes)
            popupWindow.dismiss()
        }
        
        btnRectanglePopup.setOnClickListener {
            selectTool(WhiteboardView.DrawingTool.RECTANGLE, btnShapes)
            popupWindow.dismiss()
        }
        
        // Show popup at center of screen
        popupWindow.showAtLocation(btnShapes, Gravity.CENTER, 0, 0)
    }
    
    private fun selectTool(tool: WhiteboardView.DrawingTool, button: ImageButton) {
        // Update tool selection
        btnPen.isSelected = false
        btnEraser.isSelected = false
        btnHighlighter.isSelected = false
        btnText.isSelected = false
        btnShapes.isSelected = false
        button.isSelected = true
        currentSelectedTool = button
        
        // Update whiteboard view
        whiteboardView.setDrawingTool(tool)
        
        // Ensure drawing mode is enabled when selecting a tool
        if (!isDrawingMode) {
            toggleMode()
        }
        
        // Update button states
        updateToolButtons()
    }
    
    private fun selectColor(color: Int) {
        // Update color selection
        currentSelectedColor = color
        btnColorPicker.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        
        // Update shared settings in page manager
        pageManager.updateSharedColor(color)
        
        // Update whiteboard view
        whiteboardView.setColor(color)
    }
    
    private fun toggleMode() {
        isDrawingMode = !isDrawingMode
        whiteboardView.setDrawingMode(isDrawingMode)
        
        // Update button icon
        if (isDrawingMode) {
            btnModeToggle.setImageResource(R.drawable.ic_pen)
        } else {
            btnModeToggle.setImageResource(R.drawable.ic_pan)
        }
        
        // Show toast to indicate mode
        val modeText = if (isDrawingMode) {
            getString(R.string.drawing_mode)
        } else {
            getString(R.string.pan_mode)
        }
        Toast.makeText(this, modeText, Toast.LENGTH_SHORT).show()
    }
    
    private fun togglePinchMode() {
        val isPinchEnabled = whiteboardView.isPinchModeEnabled()
        whiteboardView.setPinchModeEnabled(!isPinchEnabled)
        
        // Update button state
        btnPinchMode.isSelected = !isPinchEnabled
        
        // Show toast to indicate mode
        val modeText = if (!isPinchEnabled) {
            getString(R.string.pinch_mode_enabled)
        } else {
            getString(R.string.pinch_mode_disabled)
        }
        Toast.makeText(this, modeText, Toast.LENGTH_SHORT).show()
    }
    
    private fun updateToolButtons() {
        // Update undo/redo button states using page manager
        val currentPage = pageManager.getCurrentPage()
        btnUndo.isEnabled = currentPage.canUndo()
        btnRedo.isEnabled = currentPage.canRedo()
        btnUndo.alpha = if (currentPage.canUndo()) 1.0f else 0.5f
        btnRedo.alpha = if (currentPage.canRedo()) 1.0f else 0.5f
    }
    
    private fun showClearConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_all))
            .setMessage(getString(R.string.clear_confirm))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                pageManager.clearCurrentPage()
                loadCurrentPageToWhiteboard()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }
    
    private fun showDeletePageConfirmDialog() {
        if (pageManager.getTotalPages() <= 1) {
            Toast.makeText(this, getString(R.string.cannot_delete_last_page), Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_page_title))
            .setMessage(getString(R.string.delete_page_confirm))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                val currentPageId = pageManager.getCurrentPageId()
                if (pageManager.deletePage(currentPageId)) {
                    updatePaginationUI()
                    loadCurrentPageToWhiteboard()
                    Toast.makeText(this, getString(R.string.page_deleted), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun openAttendanceScreen() {
        val intent = Intent(this, AttendanceActivity::class.java)
        startActivity(intent)
    }

    private fun startRecording() {
        if (checkPermissions()) requestScreenCapture()
        else requestPermissions()
    }

    private fun stopRecording() {
        val intent = Intent(this, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_STOP
        }
        startService(intent)
        Toast.makeText(this, getString(R.string.recording_stopped), Toast.LENGTH_SHORT).show()
    }

    private fun requestScreenCapture() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE)
    }

    private fun checkPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, getString(R.string.permissions_granted), Toast.LENGTH_SHORT).show()
                startRecording()
            } else {
                Toast.makeText(this, getString(R.string.permissions_denied), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                startRecordingService(resultCode, data)
                Toast.makeText(this, getString(R.string.recording_started), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.screen_capture_denied), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startRecordingService(resultCode: Int, data: Intent) {
        val intent = Intent(this, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_START
            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordService.EXTRA_DATA_INTENT, data)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
            startForegroundService(intent)
        else startService(intent)
    }

    private fun updateButtonAppearance() {
        if (isRecording) toggleRecordBtn.setBackgroundResource(R.drawable.square_button)
        else toggleRecordBtn.setBackgroundResource(R.drawable.circle_button)
    }

    private fun registerRecordingStateReceiver() {
        val filter = IntentFilter().apply {
            addAction("RECORDING_STARTED")
            addAction("RECORDING_STOPPED")
        }
        registerReceiver(recordingStateReceiver, filter)
    }

    private fun logout() {
        userPreferences.clearAllData()
        Toast.makeText(this, getString(R.string.logged_out_successfully), Toast.LENGTH_SHORT).show()
        
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun enableSplitMode(sectionCount: Int) {
        // Enable split mode in whiteboard
        whiteboardView.enableSplitMode(sectionCount)
        isSplitMode = true
        
        // Update UI
        btnEnableSplit.visibility = View.GONE
        btnDisableSplit.visibility = View.VISIBLE
        bottomToolbar.visibility = View.GONE
        
        Toast.makeText(this, getString(R.string.split_mode_enabled), Toast.LENGTH_SHORT).show()
    }
    
    private fun disableSplitMode() {
        // Disable split mode in whiteboard
        whiteboardView.disableSplitMode()
        isSplitMode = false
        
        // Update UI
        btnEnableSplit.visibility = View.VISIBLE
        btnDisableSplit.visibility = View.GONE
        bottomToolbar.visibility = View.VISIBLE
        
        Toast.makeText(this, getString(R.string.split_mode_disabled), Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(recordingStateReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        
        // Clean up popup windows
        colorPopupWindow?.dismiss()
        strokePopupWindow?.dismiss()
        backgroundPopupWindow?.dismiss()
        
        // Clean up whiteboard resources
        whiteboardView.cleanup()
    }
}
