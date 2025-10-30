package com.example.moodle.ui.whiteboard

import android.content.Context
import android.graphics.Color
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.example.moodle.R
import com.example.moodle.data.datastore.UserPreferences

/**
 * Controller responsible for managing drawing tools and their states
 */
class ToolsController(
    private val activity: AppCompatActivity,
    private val whiteboardView: WhiteboardView,
    private val userPreferences: UserPreferences
) {
    
    // Tool buttons
    private lateinit var btnPen: ImageButton
    private lateinit var btnEraser: ImageButton
    private lateinit var btnText: ImageButton
    private lateinit var btnFormula: ImageButton
    private lateinit var btnShapes: ImageButton
    private lateinit var btnUndo: ImageButton
    private lateinit var btnRedo: ImageButton
    private lateinit var btnClear: ImageButton
    private lateinit var btnModeToggle: ImageButton
    private lateinit var btnColorPicker: Button
    private lateinit var btnStrokeWidth: ImageButton
    private lateinit var btnBackground: ImageButton
    
    // State
    private var isDrawingMode = true
    private var currentSelectedTool: ImageButton? = null
    private var currentSelectedColor: Int = Color.BLACK
    private var currentStrokeWidth: Float = 8f
    private var isSelectiveEraserMode = false
    private var isPenInSelectionMode = false
    
    // Callbacks
    var onClearRequested: (() -> Unit)? = null
    var onUndoRequested: (() -> Unit)? = null
    var onRedoRequested: (() -> Unit)? = null
    var onColorChanged: ((Int) -> Unit)? = null
    var onStrokeWidthChanged: ((Float) -> Unit)? = null
    var onBackgroundPatternRequested: (() -> Unit)? = null
    
    /**
     * Initialize the tools controller with UI components
     */
    fun initialize(
        pen: ImageButton,
        eraser: ImageButton,
        text: ImageButton,
        formula: ImageButton,
        shapes: ImageButton,
        undo: ImageButton,
        redo: ImageButton,
        clear: ImageButton,
        modeToggle: ImageButton,
        colorPicker: Button,
        strokeWidth: ImageButton,
        background: ImageButton
    ) {
        btnPen = pen
        btnEraser = eraser
        btnText = text
        btnFormula = formula
        btnShapes = shapes
        btnUndo = undo
        btnRedo = redo
        btnClear = clear
        btnModeToggle = modeToggle
        btnColorPicker = colorPicker
        btnStrokeWidth = strokeWidth
        btnBackground = background
        
        // Set initial selections
        currentSelectedColor = Color.BLACK
        btnColorPicker.backgroundTintList = android.content.res.ColorStateList.valueOf(currentSelectedColor)
        currentSelectedTool = btnPen
        btnPen.isSelected = true
        
        // Set initial stroke width
        whiteboardView.setStrokeWidth(currentStrokeWidth)
    }
    
    /**
     * Select a drawing tool
     */
    fun selectTool(tool: DrawingTool, button: ImageButton) {
        // Update tool selection
        btnPen.isSelected = false
        btnEraser.isSelected = false
        btnText.isSelected = false
        btnShapes.isSelected = false
        button.isSelected = true
        currentSelectedTool = button
        
        // Reset pen selection mode when selecting other tools
        if (button != btnPen && isPenInSelectionMode) {
            isPenInSelectionMode = false
            btnPen.setImageResource(R.drawable.ic_pen)
        }
        
        // Update whiteboard view
        whiteboardView.setDrawingTool(tool)
        
        // Ensure drawing mode is enabled when selecting a tool
        if (!isDrawingMode) {
            toggleMode()
        }
    }
    
    /**
     * Toggle pen between drawing and selection mode
     */
    fun togglePenSelectionMode() {
        isPenInSelectionMode = !isPenInSelectionMode
        
        if (isPenInSelectionMode) {
            // Switch to selection mode
            btnPen.setImageResource(R.drawable.ic_hand)
            selectTool(DrawingTool.SELECTION, btnPen)
            Toast.makeText(activity, activity.getString(R.string.selection_mode), Toast.LENGTH_SHORT).show()
        } else {
            // Switch back to pen mode
            btnPen.setImageResource(R.drawable.ic_pen)
            selectTool(DrawingTool.PEN, btnPen)
            Toast.makeText(activity, activity.getString(R.string.pen_mode), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Select a color
     */
    fun selectColor(color: Int) {
        currentSelectedColor = color
        btnColorPicker.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        whiteboardView.setColor(color)
        onColorChanged?.invoke(color)
    }
    
    /**
     * Set stroke width
     */
    fun setStrokeWidth(width: Float) {
        currentStrokeWidth = width
        whiteboardView.setStrokeWidth(width)
        onStrokeWidthChanged?.invoke(width)
    }
    
    /**
     * Toggle between drawing and pan mode
     */
    fun toggleMode() {
        isDrawingMode = !isDrawingMode
        whiteboardView.setDrawingMode(isDrawingMode)
        
        // Update button selected state (selected = pan mode, not selected = drawing mode)
        btnModeToggle.isSelected = !isDrawingMode
        
        // Show toast to indicate mode
        val modeText = if (isDrawingMode) {
            activity.getString(R.string.drawing_mode)
        } else {
            activity.getString(R.string.pan_mode)
        }
        Toast.makeText(activity, modeText, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Show text input dialog
     */
    fun showTextInputDialog(x: Float, y: Float) {
        val editText = EditText(activity)
        editText.hint = "متن خود را وارد کنید"
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        
        val dialog = AlertDialog.Builder(activity)
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
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
    
    /**
     * Show clear confirmation dialog
     */
    fun showClearConfirmDialog() {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.clear_all))
            .setMessage(activity.getString(R.string.clear_confirm))
            .setPositiveButton(activity.getString(R.string.yes)) { _, _ ->
                onClearRequested?.invoke()
            }
            .setNegativeButton(activity.getString(R.string.no), null)
            .show()
    }
    
    /**
     * Update undo/redo button states
     */
    fun updateUndoRedoButtons(canUndo: Boolean, canRedo: Boolean) {
        btnUndo.isEnabled = canUndo
        btnRedo.isEnabled = canRedo
        btnUndo.alpha = if (canUndo) 1.0f else 0.5f
        btnRedo.alpha = if (canRedo) 1.0f else 0.5f
    }
    
    /**
     * Set selective eraser mode
     */
    fun setSelectiveEraserMode(selective: Boolean) {
        isSelectiveEraserMode = selective
    }
    
    /**
     * Select background pattern
     */
    fun selectBackgroundPattern(pattern: BackgroundPattern) {
        whiteboardView.setBackgroundPattern(pattern)
        userPreferences.saveBackgroundPattern(pattern.id)
    }
    
    /**
     * Load saved background pattern
     */
    fun loadSavedBackgroundPattern() {
        val savedPatternId = userPreferences.getBackgroundPattern()
        val pattern = BackgroundPattern.fromId(savedPatternId)
        whiteboardView.setBackgroundPattern(pattern)
    }
    
    /**
     * Get current color
     */
    fun getCurrentColor(): Int = currentSelectedColor
    
    /**
     * Get current stroke width
     */
    fun getCurrentStrokeWidth(): Float = currentStrokeWidth
    
    /**
     * Get pen button
     */
    fun getPenButton(): ImageButton = btnPen
    
    /**
     * Get eraser button
     */
    fun getEraserButton(): ImageButton = btnEraser
    
    /**
     * Get text button
     */
    fun getTextButton(): ImageButton = btnText
    
    /**
     * Get shapes button
     */
    fun getShapesButton(): ImageButton = btnShapes
    
    /**
     * Get formula button
     */
    fun getFormulaButton(): ImageButton = btnFormula
    
    /**
     * Get color picker button
     */
    fun getColorPickerButton(): Button = btnColorPicker
    
    /**
     * Get stroke width button
     */
    fun getStrokeWidthButton(): ImageButton = btnStrokeWidth
    
    /**
     * Get background button
     */
    fun getBackgroundButton(): ImageButton = btnBackground
    
    /**
     * Check if in drawing mode
     */
    fun isDrawingMode(): Boolean = isDrawingMode
}

