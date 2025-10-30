package com.example.moodle.ui.whiteboard

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.example.moodle.R

/**
 * Individual controller widget for each section
 * Contains tools specific to one section/student
 */
class SectionControllerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface OnSectionToolChangeListener {
        fun onToolChanged(sectionId: Int, tool: DrawingTool)
        fun onColorChanged(sectionId: Int, color: Int)
        fun onStrokeWidthChanged(sectionId: Int, width: Float)
        fun onUndoClicked(sectionId: Int)
        fun onRedoClicked(sectionId: Int)
        fun onClearClicked(sectionId: Int)
        fun onSectionSelected(sectionId: Int)
    }

    private var sectionId: Int = 0
    private var studentName: String = ""
    private var listener: OnSectionToolChangeListener? = null
    
    // UI Components
    private lateinit var sectionLabel: TextView
    private lateinit var btnPen: ImageButton
    private lateinit var btnEraser: ImageButton
    private lateinit var btnText: ImageButton
    private lateinit var btnUndo: ImageButton
    private lateinit var btnRedo: ImageButton
    private lateinit var btnClear: ImageButton
    private lateinit var colorPicker: Button
    private lateinit var strokeWidthSeekBar: SeekBar
    private lateinit var strokeWidthLabel: TextView
    
    // State
    private var currentTool = DrawingTool.PEN
    private var currentColor = Color.BLACK
    private var currentStrokeWidth = 8f
    private var isActive = false
    
    init {
        orientation = VERTICAL
        setupView()
    }
    
    private fun setupView() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.section_controller, this, true)
        
        initializeViews(view)
        setupListeners()
        updateToolButtons()
    }
    
    private fun initializeViews(view: View) {
        sectionLabel = view.findViewById(R.id.sectionLabel)
        btnPen = view.findViewById(R.id.btnPen)
        btnEraser = view.findViewById(R.id.btnEraser)
        btnText = view.findViewById(R.id.btnText)
        btnUndo = view.findViewById(R.id.btnUndo)
        btnRedo = view.findViewById(R.id.btnRedo)
        btnClear = view.findViewById(R.id.btnClear)
        colorPicker = view.findViewById(R.id.colorPicker)
        strokeWidthSeekBar = view.findViewById(R.id.strokeWidthSeekBar)
        strokeWidthLabel = view.findViewById(R.id.strokeWidthLabel)
        
        // Set initial values
        colorPicker.backgroundTintList = android.content.res.ColorStateList.valueOf(currentColor)
        strokeWidthSeekBar.progress = (currentStrokeWidth - 1).toInt()
        strokeWidthLabel.text = currentStrokeWidth.toInt().toString()
        btnPen.isSelected = true
    }
    
    private fun setupListeners() {
        // Tool selection
        btnPen.setOnClickListener { selectTool(DrawingTool.PEN, btnPen) }
        btnEraser.setOnClickListener { selectTool(DrawingTool.ERASER, btnEraser) }
        btnText.setOnClickListener { selectTool(DrawingTool.TEXT, btnText) }
        
        // Actions
        btnUndo.setOnClickListener { 
            listener?.onUndoClicked(sectionId)
        }
        btnRedo.setOnClickListener { 
            listener?.onRedoClicked(sectionId)
        }
        btnClear.setOnClickListener { 
            listener?.onClearClicked(sectionId)
        }
        
        // Color picker
        colorPicker.setOnClickListener {
            showColorPickerDialog()
        }
        
        // Stroke width
        strokeWidthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentStrokeWidth = (progress + 1).toFloat()
                    strokeWidthLabel.text = currentStrokeWidth.toInt().toString()
                    listener?.onStrokeWidthChanged(sectionId, currentStrokeWidth)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Section selection
        sectionLabel.setOnClickListener {
            listener?.onSectionSelected(sectionId)
        }
    }
    
    private fun selectTool(tool: DrawingTool, button: ImageButton) {
        // Update UI
        btnPen.isSelected = false
        btnEraser.isSelected = false
        btnText.isSelected = false
        button.isSelected = true
        
        currentTool = tool
        listener?.onToolChanged(sectionId, tool)
    }
    
    private fun showColorPickerDialog() {
        val colors = arrayOf(
            Color.BLACK,
            Color.parseColor("#F44336"), // Red
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#FFEB3B"), // Yellow
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#FF9800"), // Orange
            Color.WHITE
        )
        
        val colorNames = arrayOf(
            context.getString(R.string.color_black),
            context.getString(R.string.color_red),
            context.getString(R.string.color_blue),
            context.getString(R.string.color_green),
            context.getString(R.string.color_yellow),
            context.getString(R.string.color_purple),
            context.getString(R.string.color_orange),
            context.getString(R.string.color_white)
        )
        
        val builder = android.support.v7.app.AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.colors))
        builder.setItems(colorNames) { _, which ->
            selectColor(colors[which])
        }
        builder.show()
    }
    
    private fun selectColor(color: Int) {
        currentColor = color
        colorPicker.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        listener?.onColorChanged(sectionId, color)
    }
    
    fun setSectionInfo(id: Int, name: String) {
        sectionId = id
        studentName = name
        sectionLabel.text = name
    }
    
    fun setListener(listener: OnSectionToolChangeListener?) {
        this.listener = listener
    }
    
    fun updateUndoRedoButtons(canUndo: Boolean, canRedo: Boolean) {
        btnUndo.isEnabled = canUndo
        btnRedo.isEnabled = canRedo
        btnUndo.alpha = if (canUndo) 1.0f else 0.5f
        btnRedo.alpha = if (canRedo) 1.0f else 0.5f
    }
    
    fun setActive(active: Boolean) {
        isActive = active
        // All sections are always active in concurrent mode
        // Use a consistent, ready-to-use appearance
        val backgroundColor = Color.parseColor("#F8F9FA") // Light, clean background
        setBackgroundColor(backgroundColor)
        
        // Use consistent text color
        sectionLabel.setTextColor(Color.parseColor("#495057")) // Dark gray for readability
    }
    
    fun getCurrentTool(): DrawingTool = currentTool
    fun getCurrentColor(): Int = currentColor
    fun getCurrentStrokeWidth(): Float = currentStrokeWidth
    
    fun getSectionInfo(): Pair<Int, String> = Pair(sectionId, studentName)
    
    fun updateFromSection(section: SectionManager.Section) {
        // Update tool selection
        when (section.currentTool) {
            DrawingTool.PEN -> selectTool(DrawingTool.PEN, btnPen)
            DrawingTool.ERASER -> selectTool(DrawingTool.ERASER, btnEraser)
            DrawingTool.TEXT -> selectTool(DrawingTool.TEXT, btnText)
            else -> selectTool(DrawingTool.PEN, btnPen)
        }
        
        // Update color
        selectColor(section.currentColor)
        
        // Update stroke width
        currentStrokeWidth = section.currentStrokeWidth
        strokeWidthSeekBar.progress = (currentStrokeWidth - 1).toInt()
        strokeWidthLabel.text = currentStrokeWidth.toInt().toString()
        
        // Update undo/redo buttons
        updateUndoRedoButtons(section.canUndo(), section.canRedo())
    }
    
    private fun updateToolButtons() {
        // Initial state - pen selected
        btnPen.isSelected = true
        btnEraser.isSelected = false
        btnText.isSelected = false
    }
}
