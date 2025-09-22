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
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.ViewGroup
import android.widget.*
import com.example.moodle.R
import com.example.moodle.ui.whiteboard.ScreenRecordService
import com.example.moodle.data.datastore.UserPreferences
import com.example.moodle.ui.login.LoginActivity
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
    private var drawerLayout: DrawerLayout? = null
    private var btnMenu: ImageButton? = null
    private lateinit var toggleRecordBtn: Button
    private lateinit var tvWelcome: TextView
    private lateinit var tvDrawerWelcome: TextView
    private lateinit var whiteboardView: WhiteboardView
    
    // Drawer navigation items (for sliding drawer)
    private var drawerPen: LinearLayout? = null
    private var drawerEraser: LinearLayout? = null
    private var drawerHighlighter: LinearLayout? = null
    private var drawerUndo: LinearLayout? = null
    private var drawerRedo: LinearLayout? = null
    private var drawerClear: LinearLayout? = null
    private var drawerModeToggle: LinearLayout? = null
    private var drawerPinchMode: LinearLayout? = null
    private var drawerRecord: LinearLayout? = null
    private var drawerLogout: LinearLayout? = null
    
    // Tool buttons (for fixed drawer)
    private var btnPen: ImageButton? = null
    private var btnEraser: ImageButton? = null
    private var btnHighlighter: ImageButton? = null
    private var btnUndo: ImageButton? = null
    private var btnRedo: ImageButton? = null
    private var btnClear: ImageButton? = null
    private var btnModeToggle: ImageButton? = null
    private var btnPinchMode: ImageButton? = null
    
    // Drawer UI elements (for sliding drawer)
    private var imgModeToggle: ImageView? = null
    private var tvModeToggle: TextView? = null
    private var tvPinchMode: TextView? = null
    private var tvRecordStatus: TextView? = null
    
    // Color buttons
    private lateinit var colorButtons: List<Button>
    private lateinit var strokeWidthSeekBar: SeekBar
    private lateinit var strokeWidthValue: TextView
    
    // State
    private var isRecording = false
    private var isDrawingMode = true
    private var isFixedDrawer = false
    private var currentSelectedTool: ImageButton? = null
    private var currentSelectedColor: Button? = null
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
        
        // Initialize UserPreferences first
        userPreferences = UserPreferences(this)
        
        // Check for saved drawer preference
        isFixedDrawer = userPreferences.isFixedDrawerEnabled()
        
        // Set appropriate layout based on drawer mode
        if (isFixedDrawer) {
            setContentView(R.layout.activity_whiteboard_fixed_drawer)
        } else {
            setContentView(R.layout.activity_whiteboard)
        }
        initializeViews()
        applyFonts()
        setupToolListeners()
        setupColorListeners()
        setupStrokeWidthListener()
        setupWhiteboardListener()
        registerRecordingStateReceiver()
        updateButtonAppearance()
        updateToolButtons()
    }

    private fun initializeViews() {
        // Main UI components
        tvWelcome = findViewById(R.id.tvWelcome)
        tvDrawerWelcome = findViewById(R.id.tvDrawerWelcome)
        whiteboardView = findViewById(R.id.whiteboardView)
        toggleRecordBtn = findViewById(R.id.toggleRecordBtn)
        
        if (isFixedDrawer) {
            // Fixed drawer layout - using ImageButtons directly
            // drawerLayout is a LinearLayout in this case, not DrawerLayout
            initializeFixedDrawerViews()
        } else {
            // Sliding drawer layout - using LinearLayouts
            drawerLayout = findViewById(R.id.drawerLayout)
            btnMenu = findViewById(R.id.btnMenu)
            initializeSlidingDrawerViews()
        }
        
        // Color buttons
        colorButtons = listOf(
            findViewById(R.id.colorBlack),
            findViewById(R.id.colorRed),
            findViewById(R.id.colorBlue),
            findViewById(R.id.colorGreen),
            findViewById(R.id.colorYellow),
            findViewById(R.id.colorPurple),
            findViewById(R.id.colorOrange)
        )
        
        // Stroke width controls
        strokeWidthSeekBar = findViewById(R.id.strokeWidthSeekBar)
        strokeWidthValue = findViewById(R.id.strokeWidthValue)
        
        // Set welcome message with username
        val username = userPreferences.getUsername()
        val welcomeMessage = if (!username.isNullOrBlank()) {
            getString(R.string.welcome_user_message, username)
        } else {
            getString(R.string.welcome_default_message)
        }
        tvWelcome.text = welcomeMessage
        tvDrawerWelcome.text = welcomeMessage
        
        // Set initial selections
        currentSelectedColor = colorButtons[0] // Black
        colorButtons[0].isSelected = true
        
        if (isFixedDrawer) {
            btnPen?.isSelected = true
        } else {
            drawerPen?.isSelected = true
        }
        
        // Set up menu button (only for sliding drawer)
        if (!isFixedDrawer) {
            btnMenu?.setOnClickListener {
                drawerLayout?.openDrawer(findViewById(R.id.navigationDrawer))
            }
        }
        
        setupDrawerListeners()
    }
    
    private fun initializeFixedDrawerViews() {
        // For fixed drawer, we use ImageButtons directly
        try {
            btnPen = findViewById(R.id.btnPen)
            btnEraser = findViewById(R.id.btnEraser)
            btnHighlighter = findViewById(R.id.btnHighlighter)
            btnUndo = findViewById(R.id.btnUndo)
            btnRedo = findViewById(R.id.btnRedo)
            btnClear = findViewById(R.id.btnClear)
            btnModeToggle = findViewById(R.id.btnModeToggle)
            btnPinchMode = findViewById(R.id.btnPinchMode)
            
            // Record and logout
            drawerRecord = findViewById(R.id.drawerRecord)
            drawerLogout = findViewById(R.id.drawerLogout)
            tvRecordStatus = findViewById(R.id.tvRecordStatus)
            
            // Drawer toggle button
            val drawerToggle = findViewById<LinearLayout>(R.id.drawerToggle)
            drawerToggle.setOnClickListener {
                toggleDrawerMode()
            }
        } catch (e: Exception) {
            // Handle missing IDs gracefully
            e.printStackTrace()
        }
    }
    
    private fun initializeSlidingDrawerViews() {
        // For sliding drawer, we use LinearLayouts
        try {
            drawerPen = findViewById(R.id.drawerPen)
            drawerEraser = findViewById(R.id.drawerEraser)
            drawerHighlighter = findViewById(R.id.drawerHighlighter)
            drawerUndo = findViewById(R.id.drawerUndo)
            drawerRedo = findViewById(R.id.drawerRedo)
            drawerClear = findViewById(R.id.drawerClear)
            drawerModeToggle = findViewById(R.id.drawerModeToggle)
            drawerPinchMode = findViewById(R.id.drawerPinchMode)
            drawerRecord = findViewById(R.id.drawerRecord)
            drawerLogout = findViewById(R.id.drawerLogout)
            
            // Drawer UI elements
            imgModeToggle = findViewById(R.id.imgModeToggle)
            tvModeToggle = findViewById(R.id.tvModeToggle)
            tvPinchMode = findViewById(R.id.tvPinchMode)
            tvRecordStatus = findViewById(R.id.tvRecordStatus)
        } catch (e: Exception) {
            // Handle missing IDs gracefully
            e.printStackTrace()
        }
    }

    private fun applyFonts() {
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        FontHelper.applyFontToViewGroup(this, rootView)
    }

    private fun setupDrawerListeners() {
        if (isFixedDrawer) {
            setupFixedDrawerListeners()
        } else {
            setupSlidingDrawerListeners()
        }
    }
    
    private fun setupFixedDrawerListeners() {
        // Tool selection listeners for ImageButtons
        btnPen?.setOnClickListener { btnPen?.let { selectToolFixed(WhiteboardView.DrawingTool.PEN, it) } }
        btnEraser?.setOnClickListener { btnEraser?.let { selectToolFixed(WhiteboardView.DrawingTool.ERASER, it) } }
        btnHighlighter?.setOnClickListener { btnHighlighter?.let { selectToolFixed(WhiteboardView.DrawingTool.HIGHLIGHTER, it) } }
        
        // Action listeners for ImageButtons
        btnUndo?.setOnClickListener { whiteboardView.undo() }
        btnRedo?.setOnClickListener { whiteboardView.redo() }
        btnClear?.setOnClickListener { showClearConfirmDialog() }
        btnModeToggle?.setOnClickListener { toggleMode() }
        btnPinchMode?.setOnClickListener { togglePinchMode() }
        
        // Record and logout listeners
        drawerRecord?.setOnClickListener {
            if (isRecording) stopRecording()
            else startRecording()
        }
        
        drawerLogout?.setOnClickListener { logout() }
    }
    
    private fun setupSlidingDrawerListeners() {
        // Tool selection listeners for LinearLayouts
        drawerPen?.setOnClickListener { drawerPen?.let { selectTool(WhiteboardView.DrawingTool.PEN, it) } }
        drawerEraser?.setOnClickListener { drawerEraser?.let { selectTool(WhiteboardView.DrawingTool.ERASER, it) } }
        drawerHighlighter?.setOnClickListener { drawerHighlighter?.let { selectTool(WhiteboardView.DrawingTool.HIGHLIGHTER, it) } }
        
        // Action listeners
        drawerUndo?.setOnClickListener { 
            whiteboardView.undo()
            closeDrawer()
        }
        
        drawerRedo?.setOnClickListener { 
            whiteboardView.redo()
            closeDrawer()
        }
        
        drawerClear?.setOnClickListener { 
            showClearConfirmDialog()
            closeDrawer()
        }
        
        drawerModeToggle?.setOnClickListener { 
            toggleMode()
            closeDrawer()
        }
        
        drawerPinchMode?.setOnClickListener { 
            togglePinchMode()
            closeDrawer()
        }
        
        // Record button listener
        drawerRecord?.setOnClickListener {
            if (isRecording) stopRecording()
            else startRecording()
            closeDrawer()
        }
        
        // Logout listener
        drawerLogout?.setOnClickListener {
            logout()
        }
    }
    
    private fun closeDrawer() {
        if (!isFixedDrawer && drawerLayout != null) {
            drawerLayout?.closeDrawer(findViewById(R.id.navigationDrawer))
        }
    }
    
    private fun toggleDrawerMode() {
        isFixedDrawer = !isFixedDrawer
        userPreferences.setFixedDrawerEnabled(isFixedDrawer)
        
        Toast.makeText(this, getString(R.string.drawer_mode_changed), Toast.LENGTH_SHORT).show()
        
        // Recreate activity to apply new layout
        recreate()
    }
    
    private fun selectToolFixed(tool: WhiteboardView.DrawingTool, button: ImageButton) {
        // Update tool selection for ImageButtons
        btnPen?.isSelected = false
        btnEraser?.isSelected = false
        btnHighlighter?.isSelected = false
        button.isSelected = true
        
        // Update whiteboard view
        whiteboardView.setDrawingTool(tool)
        
        // Ensure drawing mode is enabled when selecting a tool
        if (!isDrawingMode) {
            toggleMode()
        }
        
        // Update button states
        updateToolButtons()
    }
    
    private fun setupToolListeners() {
        // This method is now replaced by setupDrawerListeners
    }
    
    private fun setupColorListeners() {
        val colors = listOf(
            Color.BLACK, Color.parseColor("#F44336"), Color.parseColor("#2196F3"),
            Color.parseColor("#4CAF50"), Color.parseColor("#FFEB3B"),
            Color.parseColor("#9C27B0"), Color.parseColor("#FF9800")
        )
        
        colorButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                selectColor(colors[index], button)
            }
        }
    }
    
    private fun setupStrokeWidthListener() {
        strokeWidthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val strokeWidth = (progress + 1).toFloat() // Min 1, Max 21
                strokeWidthValue.text = strokeWidth.toInt().toString()
                whiteboardView.setStrokeWidth(strokeWidth)
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun setupWhiteboardListener() {
        whiteboardView.setOnDrawingChangeListener(object : WhiteboardView.OnDrawingChangeListener {
            override fun onDrawingChanged() {
                updateToolButtons()
            }
        })
    }
    
    private fun selectTool(tool: WhiteboardView.DrawingTool, drawerItem: LinearLayout) {
        // Update tool selection in drawer
        drawerPen?.isSelected = false
        drawerEraser?.isSelected = false
        drawerHighlighter?.isSelected = false
        drawerItem.isSelected = true
        
        // Update whiteboard view
        whiteboardView.setDrawingTool(tool)
        
        // Ensure drawing mode is enabled when selecting a tool
        if (!isDrawingMode) {
            toggleMode()
        }
        
        // Update button states
        updateToolButtons()
        closeDrawer()
    }
    
    private fun selectColor(color: Int, button: Button) {
        // Update color selection
        currentSelectedColor?.isSelected = false
        currentSelectedColor = button
        button.isSelected = true
        
        // Update whiteboard view
        whiteboardView.setColor(color)
    }
    
    private fun toggleMode() {
        isDrawingMode = !isDrawingMode
        whiteboardView.setDrawingMode(isDrawingMode)
        
        // Update UI based on drawer type
        if (isFixedDrawer) {
            // Update ImageButton for fixed drawer
            if (isDrawingMode) {
                btnModeToggle?.setImageResource(R.drawable.ic_pen)
            } else {
                btnModeToggle?.setImageResource(R.drawable.ic_pan)
            }
        } else {
            // Update LinearLayout for sliding drawer
            if (isDrawingMode) {
                imgModeToggle?.setImageResource(R.drawable.ic_pen)
                tvModeToggle?.text = getString(R.string.drawing_mode)
            } else {
                imgModeToggle?.setImageResource(R.drawable.ic_pan)
                tvModeToggle?.text = getString(R.string.pan_mode)
            }
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
        
        // Update UI based on drawer type
        if (isFixedDrawer) {
            // Update ImageButton for fixed drawer
            btnPinchMode?.isSelected = !isPinchEnabled
        } else {
            // Update LinearLayout for sliding drawer
            drawerPinchMode?.isSelected = !isPinchEnabled
            tvPinchMode?.text = if (!isPinchEnabled) {
                getString(R.string.pinch_mode_enabled)
            } else {
                getString(R.string.pinch_mode_disabled)
            }
        }
        
        // Show toast to indicate mode
        val modeText = if (!isPinchEnabled) {
            getString(R.string.pinch_mode_enabled)
        } else {
            getString(R.string.pinch_mode_disabled)
        }
        Toast.makeText(this, modeText, Toast.LENGTH_SHORT).show()
    }
    
    private fun updateToolButtons() {
        if (isFixedDrawer) {
            // Update ImageButtons for fixed drawer
            btnUndo?.isEnabled = whiteboardView.canUndo()
            btnRedo?.isEnabled = whiteboardView.canRedo()
            btnUndo?.alpha = if (whiteboardView.canUndo()) 1.0f else 0.5f
            btnRedo?.alpha = if (whiteboardView.canRedo()) 1.0f else 0.5f
        } else {
            // Update LinearLayouts for sliding drawer
            drawerUndo?.isEnabled = whiteboardView.canUndo()
            drawerRedo?.isEnabled = whiteboardView.canRedo()
            drawerUndo?.alpha = if (whiteboardView.canUndo()) 1.0f else 0.5f
            drawerRedo?.alpha = if (whiteboardView.canRedo()) 1.0f else 0.5f
        }
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
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun updateButtonAppearance() {
        if (isRecording) {
            toggleRecordBtn.setBackgroundResource(R.drawable.square_button)
            tvRecordStatus?.text = getString(R.string.stop_recording)
        } else {
            toggleRecordBtn.setBackgroundResource(R.drawable.circle_button)
            tvRecordStatus?.text = getString(R.string.start_recording)
        }
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
        
        // Clean up whiteboard resources
        whiteboardView.cleanup()
    }
}
