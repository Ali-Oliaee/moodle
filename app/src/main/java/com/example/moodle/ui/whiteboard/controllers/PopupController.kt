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
import com.example.moodle.ui.whiteboard.components.MathFormulasPopup
import com.example.moodle.ui.whiteboard.components.FormulaItem

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
    
    // Compose-based popups
    private var penOptionsComposeView: ComposeView? = null
    private var isPenOptionsVisible by mutableStateOf(false)
    
    private var backgroundPatternComposeView: ComposeView? = null
    private var isBackgroundPatternVisible by mutableStateOf(false)
    
    private var shapesComposeView: ComposeView? = null
    private var isShapesVisible by mutableStateOf(false)
    
    private var mathFormulasComposeView: ComposeView? = null
    private var isMathFormulasVisible by mutableStateOf(false)
    
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
     * Show formula selection popup using Jetpack Compose
     */
    fun showFormulaPopup() {
        // Close any existing popup
        dismissFormulaPopup()
        
        // Create or reuse ComposeView
        if (mathFormulasComposeView == null) {
            mathFormulasComposeView = ComposeView(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
        
        // Add ComposeView to activity's root layout
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        if (mathFormulasComposeView?.parent == null) {
            rootView.addView(mathFormulasComposeView)
        }
        
        // Get formula items
        val formulaItems = getFormulaItems()
        
        // Set Compose content
        mathFormulasComposeView?.setContent {
            MathFormulasPopup(
                isVisible = isMathFormulasVisible,
                formulas = formulaItems,
                onFormulaSelected = { formula ->
                    addFormulaToBoard(formula)
                },
                onDismiss = {
                    dismissFormulaPopup()
                }
            )
        }
        
        // Show the popup
        isMathFormulasVisible = true
    }
    
    /**
     * Dismiss the math formulas popup
     */
    fun dismissFormulaPopup() {
        isMathFormulasVisible = false
        mathFormulasComposeView?.let { view ->
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            rootView.removeView(view)
        }
        mathFormulasComposeView = null
    }
    
    /**
     * Add formula to the whiteboard
     */
    private fun addFormulaToBoard(formula: FormulaItem) {
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
     * Get list of formula items for Compose popup
     */
    private fun getFormulaItems(): List<FormulaItem> {
        return listOf(
            FormulaItem(
                name = "Quadratic Formula",
                template = "x = (-{b} ± √({b}²-4{a}{c})) / 2{a}",
                displayText = "x = (-b ± √(b²-4ac)) / 2a",
                variables = listOf("a", "b", "c"),
                descriptions = listOf("ضریب x²", "ضریب x", "عدد ثابت")
            ),
            FormulaItem(
                name = "Pythagorean Theorem",
                template = "{a}² + {b}² = {c}²",
                displayText = "a² + b² = c²",
                variables = listOf("a", "b", "c"),
                descriptions = listOf("ضلع اول", "ضلع دوم", "وتر")
            ),
            FormulaItem(
                name = "Circle Area",
                template = "A = π{r}²",
                displayText = "A = πr²",
                variables = listOf("r"),
                descriptions = listOf("شعاع")
            ),
            FormulaItem(
                name = "Distance Formula",
                template = "d = √(({x2}-{x1})² + ({y2}-{y1})²)",
                displayText = "d = √((x₂-x₁)² + (y₂-y₁)²)",
                variables = listOf("x1", "y1", "x2", "y2"),
                descriptions = listOf("x نقطه اول", "y نقطه اول", "x نقطه دوم", "y نقطه دوم")
            ),
            FormulaItem(
                name = "Slope Formula",
                template = "m = ({y2}-{y1}) / ({x2}-{x1})",
                displayText = "m = (y₂-y₁) / (x₂-x₁)",
                variables = listOf("x1", "y1", "x2", "y2"),
                descriptions = listOf("x نقطه اول", "y نقطه اول", "x نقطه دوم", "y نقطه دوم")
            ),
            FormulaItem(
                name = "Sphere Volume",
                template = "V = (4/3)π{r}³",
                displayText = "V = (4/3)πr³",
                variables = listOf("r"),
                descriptions = listOf("شعاع")
            ),
            FormulaItem(
                name = "Compound Interest",
                template = "A = {P}(1 + {r}/{n})^({n}{t})",
                displayText = "A = P(1 + r/n)^(nt)",
                variables = listOf("P", "r", "n", "t"),
                descriptions = listOf("سرمایه اولیه", "نرخ بهره", "تعداد دفعات", "زمان")
            ),
            FormulaItem(
                name = "Law of Cosines",
                template = "{c}² = {a}² + {b}² - 2{a}{b} cos({C})",
                displayText = "c² = a² + b² - 2ab cos(C)",
                variables = listOf("a", "b", "c", "C"),
                descriptions = listOf("ضلع اول", "ضلع دوم", "ضلع سوم", "زاویه")
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
        dismissFormulaPopup()
        eraserModePopupWindow?.dismiss()
    }
}

