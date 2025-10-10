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
    
    // Tool buttons
    private lateinit var btnPen: ImageButton
    private lateinit var btnEraser: ImageButton
    private lateinit var btnHighlighter: ImageButton
    private lateinit var btnText: ImageButton
    private lateinit var btnUndo: ImageButton
    private lateinit var btnRedo: ImageButton
    private lateinit var btnClear: ImageButton
    private lateinit var btnModeToggle: ImageButton
    private lateinit var btnPinchMode: ImageButton
    private lateinit var btnColorPicker: Button
    private lateinit var btnStrokeWidth: ImageButton
    
    // Popup views
    private var colorPopupWindow: PopupWindow? = null
    private var strokePopupWindow: PopupWindow? = null
    private var currentStrokeWidth: Float = 8f
    
    // State
    private var isRecording = false
    private var isDrawingMode = true
    private var currentSelectedTool: ImageButton? = null
    private var currentSelectedColor: Int = Color.BLACK
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
        
        // Set the new simplified layout
        setContentView(R.layout.activity_whiteboard)
        
        initializeViews()
        applyFonts()
        setupToolListeners()
        setupPopupListeners()
        setupWhiteboardListener()
        registerRecordingStateReceiver()
        updateButtonAppearance()
        updateToolButtons()
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
        btnUndo = findViewById(R.id.btnUndo)
        btnRedo = findViewById(R.id.btnRedo)
        btnClear = findViewById(R.id.btnClear)
        btnModeToggle = findViewById(R.id.btnModeToggle)
        btnPinchMode = findViewById(R.id.btnPinchMode)
        btnColorPicker = findViewById(R.id.btnColorPicker)
        btnStrokeWidth = findViewById(R.id.btnStrokeWidth)
        
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
        btnEraser.setOnClickListener { selectTool(WhiteboardView.DrawingTool.ERASER, btnEraser) }
        btnHighlighter.setOnClickListener { selectTool(WhiteboardView.DrawingTool.HIGHLIGHTER, btnHighlighter) }
        btnText.setOnClickListener { selectTool(WhiteboardView.DrawingTool.TEXT, btnText) }
        
        // Action listeners
        btnUndo.setOnClickListener { whiteboardView.undo() }
        btnRedo.setOnClickListener { whiteboardView.redo() }
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
                    whiteboardView.setStrokeWidth(strokeWidth)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Show popup above the stroke width button
        strokePopupWindow?.showAsDropDown(btnStrokeWidth, 0, -btnStrokeWidth.height - 150, Gravity.CENTER)
    }
    
    private fun setupWhiteboardListener() {
        whiteboardView.setOnDrawingChangeListener(object : WhiteboardView.OnDrawingChangeListener {
            override fun onDrawingChanged() {
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
    
    private fun selectTool(tool: WhiteboardView.DrawingTool, button: ImageButton) {
        // Update tool selection
        btnPen.isSelected = false
        btnEraser.isSelected = false
        btnHighlighter.isSelected = false
        btnText.isSelected = false
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
        // Update undo/redo button states
        btnUndo.isEnabled = whiteboardView.canUndo()
        btnRedo.isEnabled = whiteboardView.canRedo()
        btnUndo.alpha = if (whiteboardView.canUndo()) 1.0f else 0.5f
        btnRedo.alpha = if (whiteboardView.canRedo()) 1.0f else 0.5f
    }
    
    private fun showClearConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_all))
            .setMessage(getString(R.string.clear_confirm))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                whiteboardView.clear()
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
        
        // Clean up whiteboard resources
        whiteboardView.cleanup()
    }
}