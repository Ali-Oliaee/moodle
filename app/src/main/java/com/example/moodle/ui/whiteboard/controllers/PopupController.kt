package com.example.moodle.ui.whiteboard.controllers
import com.example.moodle.ui.whiteboard.models.*

import com.example.moodle.ui.whiteboard.models.BackgroundPattern
import com.example.moodle.ui.whiteboard.models.DrawingTool
import com.example.moodle.ui.whiteboard.views.WhiteboardView

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.example.moodle.R
import com.example.moodle.ui.whiteboard.components.PenOptionsPopup
import com.example.moodle.ui.whiteboard.components.BackgroundPatternPopup
import com.example.moodle.ui.whiteboard.components.ShapesPopup

/**
 * Controller responsible for managing all popup windows in the whiteboard
 */
class PopupController(
    private val activity: AppCompatActivity,
    private val whiteboardView: WhiteboardView
) {
    
    // Popup windows
    private var colorPopupWindow: PopupWindow? = null
    private var strokePopupWindow: PopupWindow? = null
    private var eraserModePopupWindow: PopupWindow? = null
    private var formulaPopupWindow: PopupWindow? = null
    
    // Compose-based popups
    private var penOptionsComposeView: ComposeView? = null
    private var isPenOptionsVisible by mutableStateOf(false)
    
    private var backgroundPatternComposeView: ComposeView? = null
    private var isBackgroundPatternVisible by mutableStateOf(false)
    
    private var shapesComposeView: ComposeView? = null
    private var isShapesVisible by mutableStateOf(false)
    
    // Callbacks
    var onColorSelected: ((Int) -> Unit)? = null
    var onStrokeWidthChanged: ((Float) -> Unit)? = null
    var onBackgroundPatternSelected: ((BackgroundPattern) -> Unit)? = null
    var onBackgroundColorSelected: ((BackgroundColor) -> Unit)? = null
    var onBackgroundImageUploadRequested: (() -> Unit)? = null
    var onToolSelected: ((DrawingTool) -> Unit)? = null
    var onEraserModeSelected: ((Boolean) -> Unit)? = null
    

    /**
     * Show combined pen options popup (color + stroke width) using Jetpack Compose
     */
    fun showPenOptionsPopup(anchorView: View, currentColor: Int, currentStrokeWidth: Float) {
        // Close any existing popup
        dismissPenOptionsPopup()
        
        // Get anchor view location on screen
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        
        val anchorX = location[0]
        val anchorY = location[1] - 600
        
        // Create or reuse ComposeView
        if (penOptionsComposeView == null) {
            penOptionsComposeView = ComposeView(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
        
        // Add ComposeView to activity's root layout
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        if (penOptionsComposeView?.parent == null) {
            rootView.addView(penOptionsComposeView)
        }
        
        // Convert Android Color to Compose Color
        val composeColor = androidx.compose.ui.graphics.Color(currentColor)
        
        // Set Compose content
        penOptionsComposeView?.setContent {
            PenOptionsPopup(
                isVisible = isPenOptionsVisible,
                currentColor = composeColor,
                currentStrokeWidth = currentStrokeWidth,
                anchorX = anchorX,
                anchorY = anchorY,
                onColorSelected = { color ->
                    // Convert Compose Color back to Android Color
                    val androidColor = android.graphics.Color.argb(
                        (color.alpha * 255).toInt(),
                        (color.red * 255).toInt(),
                        (color.green * 255).toInt(),
                        (color.blue * 255).toInt()
                    )
                    onColorSelected?.invoke(androidColor)
                },
                onStrokeWidthChanged = { width ->
                    onStrokeWidthChanged?.invoke(width)
                },
                onDismiss = {
                    dismissPenOptionsPopup()
                }
            )
        }
        
        // Show the popup
        isPenOptionsVisible = true
    }
    
    /**
     * Dismiss the pen options popup
     */
    fun dismissPenOptionsPopup() {
        isPenOptionsVisible = false
        penOptionsComposeView?.let { view ->
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            rootView.removeView(view)
        }
        penOptionsComposeView = null
    }
    
    /**
     * Show background pattern popup using Jetpack Compose
     */
    fun showBackgroundPatternPopup(currentPattern: BackgroundPattern, currentColor: BackgroundColor) {
        // Close any existing popup
        dismissBackgroundPatternPopup()
        
        // Create or reuse ComposeView
        if (backgroundPatternComposeView == null) {
            backgroundPatternComposeView = ComposeView(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
        
        // Add ComposeView to activity's root layout
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        if (backgroundPatternComposeView?.parent == null) {
            rootView.addView(backgroundPatternComposeView)
        }
        
        // Set Compose content
        backgroundPatternComposeView?.setContent {
            BackgroundPatternPopup(
                isVisible = isBackgroundPatternVisible,
                currentPattern = currentPattern,
                currentColor = currentColor,
                onPatternSelected = { pattern ->
                    onBackgroundPatternSelected?.invoke(pattern)
                },
                onColorSelected = { color ->
                    onBackgroundColorSelected?.invoke(color)
                },
                onUploadImageRequested = {
                    onBackgroundImageUploadRequested?.invoke()
                    dismissBackgroundPatternPopup()
                },
                onDismiss = {
                    dismissBackgroundPatternPopup()
                }
            )
        }
        
        // Show the popup
        isBackgroundPatternVisible = true
    }
    
    /**
     * Dismiss the background pattern popup
     */
    fun dismissBackgroundPatternPopup() {
        isBackgroundPatternVisible = false
        backgroundPatternComposeView?.let { view ->
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            rootView.removeView(view)
        }
        backgroundPatternComposeView = null
    }
    
    /**
     * Show eraser mode selection popup
     */
    fun showEraserModePopup(anchorView: View) {
        val inflater = LayoutInflater.from(activity)
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
            onToolSelected?.invoke(DrawingTool.ERASER)
            onEraserModeSelected?.invoke(false)
            eraserModePopupWindow?.dismiss()
        }
        
        // Selective eraser option
        val selectiveEraserOption = popupView.findViewById<ImageButton>(R.id.selectiveEraserOption)
        selectiveEraserOption.setOnClickListener {
            onToolSelected?.invoke(DrawingTool.SELECTIVE_ERASER)
            onEraserModeSelected?.invoke(true)
            eraserModePopupWindow?.dismiss()
        }
        
        // Show popup above the anchor view
        eraserModePopupWindow?.showAsDropDown(anchorView, 0, -anchorView.height - 200, Gravity.CENTER)
    }
    
    /**
     * Show formula selection popup
     */
    fun showFormulaPopup() {
        val inflater = LayoutInflater.from(activity)
        val popupView = inflater.inflate(R.layout.popup_math_formulas, null)
        
        formulaPopupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        
        val formulas = getFormulaTemplates()
        
        // Set up formula option listeners
        setupFormulaClickListener(popupView, R.id.formulaQuadratic, formulas[0])
        setupFormulaClickListener(popupView, R.id.formulaPythagorean, formulas[1])
        setupFormulaClickListener(popupView, R.id.formulaCircleArea, formulas[2])
        setupFormulaClickListener(popupView, R.id.formulaDistance, formulas[3])
        setupFormulaClickListener(popupView, R.id.formulaSlope, formulas[4])
        setupFormulaClickListener(popupView, R.id.formulaSphereVolume, formulas[5])
        setupFormulaClickListener(popupView, R.id.formulaCompoundInterest, formulas[6])
        setupFormulaClickListener(popupView, R.id.formulaCosines, formulas[7])
        
        // Show popup at center of screen
        formulaPopupWindow?.showAtLocation(
            activity.findViewById(android.R.id.content),
            Gravity.CENTER,
            0,
            0
        )
    }
    
    /**
     * Helper to setup formula click listener
     */
    private fun setupFormulaClickListener(popupView: View, viewId: Int, formula: FormulaTemplate) {
        popupView.findViewById<LinearLayout>(viewId)?.setOnClickListener {
            formulaPopupWindow?.dismiss()
            addFormulaToBoard(formula)
        }
    }
    
    /**
     * Add formula to the whiteboard
     */
    private fun addFormulaToBoard(formula: FormulaTemplate) {
        // Replace placeholders with empty spaces (boxes) for manual filling
        var formulaText = formula.template
        
        // Replace each variable placeholder with empty box symbol ☐
        for (variable in formula.variables) {
            formulaText = formulaText.replace("{$variable}", "☐")
        }
        
        // Post to ensure view is measured and positioned correctly
        whiteboardView.post {
            // Add the formula as text to the whiteboard at center of current view
            val centerX = whiteboardView.getVisibleCanvasCenterX()
            val centerY = whiteboardView.getVisibleCanvasCenterY()
            whiteboardView.addText(formulaText, centerX, centerY)
        }
    }
    
    /**
     * Show shapes selection popup using Jetpack Compose
     */
    fun showShapesPopup(currentTool: DrawingTool) {
        // Close any existing popup
        dismissShapesPopup()
        
        // Create or reuse ComposeView
        if (shapesComposeView == null) {
            shapesComposeView = ComposeView(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
        
        // Add ComposeView to activity's root layout
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        if (shapesComposeView?.parent == null) {
            rootView.addView(shapesComposeView)
        }
        
        // Set Compose content
        shapesComposeView?.setContent {
            ShapesPopup(
                isVisible = isShapesVisible,
                currentTool = currentTool,
                onShapeSelected = { tool ->
                    onToolSelected?.invoke(tool)
                },
                onDismiss = {
                    dismissShapesPopup()
                }
            )
        }
        
        // Show the popup
        isShapesVisible = true
    }
    
    /**
     * Dismiss the shapes popup
     */
    fun dismissShapesPopup() {
        isShapesVisible = false
        shapesComposeView?.let { view ->
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            rootView.removeView(view)
        }
        shapesComposeView = null
    }
    
    /**
     * Get list of formula templates
     */
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
    
    /**
     * Clean up all popup windows
     */
    fun cleanup() {
        colorPopupWindow?.dismiss()
        strokePopupWindow?.dismiss()
        dismissPenOptionsPopup()
        dismissBackgroundPatternPopup()
        dismissShapesPopup()
        eraserModePopupWindow?.dismiss()
        formulaPopupWindow?.dismiss()
    }
    
    /**
     * Data class for formula templates
     */
    data class FormulaTemplate(
        val name: String,
        val template: String,
        val variables: List<String>,
        val descriptions: List<String>
    )
}

