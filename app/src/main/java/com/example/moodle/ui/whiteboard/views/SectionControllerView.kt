package com.example.moodle.ui.whiteboard.views
import com.example.moodle.ui.whiteboard.models.*
import com.example.moodle.ui.whiteboard.managers.*

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
    private lateinit var btnClear: ImageButton
    private lateinit var btnColorBlack: Button
    private lateinit var btnColorBlue: Button
    private lateinit var btnColorGreen: Button
    private lateinit var btnColorRed: Button
    
    // State
    private var currentTool = DrawingTool.PEN
    private var currentColor = Color.BLACK
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
        btnClear = view.findViewById(R.id.btnClear)
        btnColorBlack = view.findViewById(R.id.btnColorBlack)
        btnColorBlue = view.findViewById(R.id.btnColorBlue)
        btnColorGreen = view.findViewById(R.id.btnColorGreen)
        btnColorRed = view.findViewById(R.id.btnColorRed)
        
        // Set initial values
        btnPen.isSelected = true
        updateColorButtonSelection()
    }
    
    private fun setupListeners() {
        // Tool selection
        btnPen.setOnClickListener { selectTool(DrawingTool.PEN, btnPen) }
        btnEraser.setOnClickListener { selectTool(DrawingTool.ERASER, btnEraser) }
        btnText.setOnClickListener { selectTool(DrawingTool.TEXT, btnText) }
        
        // Clear action
        btnClear.setOnClickListener { 
            listener?.onClearClicked(sectionId)
        }
        
        // Color selection
        btnColorBlack.setOnClickListener { 
            selectColor(Color.BLACK)
        }
        btnColorBlue.setOnClickListener { 
            selectColor(Color.parseColor("#2196F3"))
        }
        btnColorGreen.setOnClickListener { 
            selectColor(Color.parseColor("#4CAF50"))
        }
        btnColorRed.setOnClickListener { 
            selectColor(Color.parseColor("#F44336"))
        }
        
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
    
    private fun selectColor(color: Int) {
        currentColor = color
        updateColorButtonSelection()
        listener?.onColorChanged(sectionId, color)
    }
    
    private fun updateColorButtonSelection() {
        // Reset all color button elevations/alphas
        btnColorBlack.alpha = if (currentColor == Color.BLACK) 1.0f else 0.5f
        btnColorBlue.alpha = if (currentColor == Color.parseColor("#2196F3")) 1.0f else 0.5f
        btnColorGreen.alpha = if (currentColor == Color.parseColor("#4CAF50")) 1.0f else 0.5f
        btnColorRed.alpha = if (currentColor == Color.parseColor("#F44336")) 1.0f else 0.5f
    }

    private fun updateToolButtons() {
        // Initial state - pen selected
        btnPen.isSelected = true
        btnEraser.isSelected = false
        btnText.isSelected = false
    }
}
