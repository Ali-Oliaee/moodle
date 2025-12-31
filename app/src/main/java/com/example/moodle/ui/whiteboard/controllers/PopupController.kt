package com.example.moodle.ui.whiteboard.controllers
import com.example.moodle.ui.whiteboard.models.*
import com.example.moodle.ui.whiteboard.views.*

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
    private var backgroundPopupWindow: PopupWindow? = null
    private var shapesPopupWindow: PopupWindow? = null
    
    // Compose-based popup
    private var penOptionsComposeView: ComposeView? = null
    private var isPenOptionsVisible by mutableStateOf(false)
    
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
        val anchorY = location[1] - 550
        
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
     * Show background pattern popup
     */
    fun showBackgroundPatternPopup() {
        val inflater = LayoutInflater.from(activity)
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
        
        // Set up click listeners for background colors
        setupBackgroundColorClickListener(popupView, R.id.backgroundColorWhite, BackgroundColor.WHITE)
        setupBackgroundColorClickListener(popupView, R.id.backgroundColorLightGray, BackgroundColor.LIGHT_GRAY)
        setupBackgroundColorClickListener(popupView, R.id.backgroundColorLightBlue, BackgroundColor.LIGHT_BLUE)
        setupBackgroundColorClickListener(popupView, R.id.backgroundColorLightGreen, BackgroundColor.LIGHT_GREEN)
        setupBackgroundColorClickListener(popupView, R.id.backgroundColorLightYellow, BackgroundColor.LIGHT_YELLOW)
        setupBackgroundColorClickListener(popupView, R.id.backgroundColorLightPink, BackgroundColor.LIGHT_PINK)
        setupBackgroundColorClickListener(popupView, R.id.backgroundColorDarkBlue, BackgroundColor.DARK_BLUE)
        setupBackgroundColorClickListener(popupView, R.id.backgroundColorDarkGreen, BackgroundColor.DARK_GREEN)
        setupBackgroundColorClickListener(popupView, R.id.backgroundColorBlack, BackgroundColor.BLACK)
        
        // Set up click listeners for all patterns
        setupBackgroundPatternClickListener(popupView, R.id.backgroundPlain, BackgroundPattern.PLAIN)
        setupBackgroundPatternClickListener(popupView, R.id.backgroundGridSmall, BackgroundPattern.GRID_SMALL)
        setupBackgroundPatternClickListener(popupView, R.id.backgroundGridLarge, BackgroundPattern.GRID_LARGE)
        setupBackgroundPatternClickListener(popupView, R.id.backgroundLinesHorizontal, BackgroundPattern.LINES_HORIZONTAL)
        setupBackgroundPatternClickListener(popupView, R.id.backgroundLinesVertical, BackgroundPattern.LINES_VERTICAL)
        setupBackgroundPatternClickListener(popupView, R.id.backgroundLinesBoth, BackgroundPattern.LINES_BOTH)
        setupBackgroundPatternClickListener(popupView, R.id.backgroundDotGrid, BackgroundPattern.DOT_GRID)
        setupBackgroundPatternClickListener(popupView, R.id.backgroundGraphPaper, BackgroundPattern.GRAPH_PAPER)
        
        // Setup upload background image button
        val btnUploadBackgroundImage = popupView.findViewById<Button>(R.id.btnUploadBackgroundImage)
        btnUploadBackgroundImage?.setOnClickListener {
            onBackgroundImageUploadRequested?.invoke()
            backgroundPopupWindow?.dismiss()
        }
        
        backgroundPopupWindow?.showAtLocation(whiteboardView, Gravity.CENTER, 0, 0)
    }
    
    /**
     * Helper to setup background color click listener
     */
    private fun setupBackgroundColorClickListener(popupView: View, viewId: Int, color: BackgroundColor) {
        popupView.findViewById<View>(viewId)?.setOnClickListener {
            onBackgroundColorSelected?.invoke(color)
        }
    }
    
    /**
     * Helper to setup background pattern click listener
     */
    private fun setupBackgroundPatternClickListener(popupView: View, viewId: Int, pattern: BackgroundPattern) {
        popupView.findViewById<FrameLayout>(viewId)?.setOnClickListener {
            onBackgroundPatternSelected?.invoke(pattern)
        }
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
     * Show shapes selection popup
     */
    fun showShapesPopup(anchorView: View) {
        val inflater = LayoutInflater.from(activity)
        val popupView = inflater.inflate(R.layout.popup_shapes, null)
        
        shapesPopupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        
        // Find all shape buttons
        setupShapeButton(popupView, R.id.btnLinePopup, DrawingTool.LINE)
        setupShapeButton(popupView, R.id.btnCirclePopup, DrawingTool.CIRCLE)
        setupShapeButton(popupView, R.id.btnEllipsePopup, DrawingTool.ELLIPSE)
        setupShapeButton(popupView, R.id.btnRectanglePopup, DrawingTool.RECTANGLE)
        setupShapeButton(popupView, R.id.btnDiamondPopup, DrawingTool.DIAMOND)
        
        setupShapeButton(popupView, R.id.btnTrianglePopup, DrawingTool.TRIANGLE)
        setupShapeButton(popupView, R.id.btnRightTrianglePopup, DrawingTool.RIGHT_TRIANGLE)
        setupShapeButton(popupView, R.id.btnIsoscelesTrianglePopup, DrawingTool.ISOSCELES_TRIANGLE)
        setupShapeButton(popupView, R.id.btnStarPopup, DrawingTool.STAR)
        setupShapeButton(popupView, R.id.btnArrowPopup, DrawingTool.ARROW)
        
        setupShapeButton(popupView, R.id.btnPentagonPopup, DrawingTool.PENTAGON)
        setupShapeButton(popupView, R.id.btnHexagonPopup, DrawingTool.HEXAGON)
        setupShapeButton(popupView, R.id.btnParallelogramPopup, DrawingTool.PARALLELOGRAM)
        setupShapeButton(popupView, R.id.btnTrapezoidPopup, DrawingTool.TRAPEZOID)
        
        setupShapeButton(popupView, R.id.btnCubePopup, DrawingTool.CUBE)
        setupShapeButton(popupView, R.id.btnCylinderPopup, DrawingTool.CYLINDER)
        setupShapeButton(popupView, R.id.btnPyramidPopup, DrawingTool.PYRAMID)
        setupShapeButton(popupView, R.id.btnSpherePopup, DrawingTool.SPHERE)
        setupShapeButton(popupView, R.id.btnConePopup, DrawingTool.CONE)
        
        // Show popup at center of screen
        shapesPopupWindow?.showAtLocation(anchorView, Gravity.CENTER, 0, 0)
    }
    
    /**
     * Helper to setup shape button click listener
     */
    private fun setupShapeButton(popupView: View, buttonId: Int, tool: DrawingTool) {
        popupView.findViewById<ImageButton>(buttonId)?.setOnClickListener {
            onToolSelected?.invoke(tool)
            shapesPopupWindow?.dismiss()
        }
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
        eraserModePopupWindow?.dismiss()
        formulaPopupWindow?.dismiss()
        backgroundPopupWindow?.dismiss()
        shapesPopupWindow?.dismiss()
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

