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
import android.widget.*
import com.example.moodle.R
import com.example.moodle.ui.whiteboard.ScreenRecordService
import com.example.moodle.data.datastore.UserPreferences
import com.example.moodle.ui.login.LoginActivity

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
    private lateinit var btnLogout: Button
    private lateinit var tvWelcome: TextView
    private lateinit var whiteboardView: WhiteboardView
    
    // Tool buttons
    private lateinit var btnPen: ImageButton
    private lateinit var btnEraser: ImageButton
    private lateinit var btnHighlighter: ImageButton
    private lateinit var btnUndo: ImageButton
    private lateinit var btnRedo: ImageButton
    private lateinit var btnClear: ImageButton
    private lateinit var btnModeToggle: ImageButton
    
    // Color buttons
    private lateinit var colorButtons: List<Button>
    private lateinit var strokeWidthSeekBar: SeekBar
    private lateinit var strokeWidthValue: TextView
    
    // State
    private var isRecording = false
    private var isDrawingMode = true
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
        setContentView(R.layout.activity_whiteboard)

        userPreferences = UserPreferences(this)
        initializeViews()
        setupToolListeners()
        setupColorListeners()
        setupStrokeWidthListener()
        registerRecordingStateReceiver()
        updateButtonAppearance()
        updateToolButtons()
    }

    private fun initializeViews() {
        // Main UI components
        toggleRecordBtn = findViewById(R.id.toggleRecordBtn)
        btnLogout = findViewById(R.id.btnLogout)
        tvWelcome = findViewById(R.id.tvWelcome)
        whiteboardView = findViewById(R.id.whiteboardView)
        
        // Tool buttons
        btnPen = findViewById(R.id.btnPen)
        btnEraser = findViewById(R.id.btnEraser)
        btnHighlighter = findViewById(R.id.btnHighlighter)
        btnUndo = findViewById(R.id.btnUndo)
        btnRedo = findViewById(R.id.btnRedo)
        btnClear = findViewById(R.id.btnClear)
        btnModeToggle = findViewById(R.id.btnModeToggle)
        
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
        tvWelcome.text = if (!username.isNullOrBlank()) {
            getString(R.string.welcome_user_message, username)
        } else {
            getString(R.string.welcome_default_message)
        }
        
        // Set initial selections
        currentSelectedTool = btnPen
        currentSelectedColor = colorButtons[0] // Black
        
        // Set initial selected states programmatically
        btnPen.isSelected = true
        colorButtons[0].isSelected = true
        
        toggleRecordBtn.setOnClickListener {
            if (isRecording) stopRecording()
            else startRecording()
        }
        
        btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun setupToolListeners() {
        btnPen.setOnClickListener { selectTool(WhiteboardView.DrawingTool.PEN, btnPen) }
        btnEraser.setOnClickListener { selectTool(WhiteboardView.DrawingTool.ERASER, btnEraser) }
        btnHighlighter.setOnClickListener { selectTool(WhiteboardView.DrawingTool.HIGHLIGHTER, btnHighlighter) }
        
        btnUndo.setOnClickListener { 
            whiteboardView.undo()
            updateToolButtons()
        }
        
        btnRedo.setOnClickListener { 
            whiteboardView.redo()
            updateToolButtons()
        }
        
        btnClear.setOnClickListener { showClearConfirmDialog() }
        
        btnModeToggle.setOnClickListener { toggleMode() }
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
    
    private fun selectTool(tool: WhiteboardView.DrawingTool, button: ImageButton) {
        // Update tool selection
        currentSelectedTool?.isSelected = false
        currentSelectedTool = button
        button.isSelected = true
        
        // Update whiteboard view
        whiteboardView.setDrawingTool(tool)
        
        // Ensure drawing mode is enabled when selecting a tool
        if (!isDrawingMode) {
            toggleMode()
        }
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
        
        // Update button appearance
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
    
    private fun updateToolButtons() {
        btnUndo.isEnabled = whiteboardView.canUndo()
        btnRedo.isEnabled = whiteboardView.canRedo()
    }
    
    private fun showClearConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_all))
            .setMessage(getString(R.string.clear_confirm))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                whiteboardView.clear()
                updateToolButtons()
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
        } else {
            toggleRecordBtn.setBackgroundResource(R.drawable.circle_button)
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
    }
}
