package com.example.moodle.ui.whiteboard.controllers
import com.example.moodle.ui.whiteboard.models.*
import com.example.moodle.ui.whiteboard.views.*

import com.example.moodle.ui.whiteboard.models.BackgroundPattern
import com.example.moodle.ui.whiteboard.models.DrawingTool
import com.example.moodle.ui.whiteboard.views.WhiteboardView

import android.content.Context
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.moodle.R

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
    private var zoomPopupWindow: PopupWindow? = null
    private var shapesPopupWindow: PopupWindow? = null
    
    // State
    private var isUpdatingZoomFromSlider = false
    
    // Callbacks
    var onColorSelected: ((Int) -> Unit)? = null
    var onStrokeWidthChanged: ((Float) -> Unit)? = null
    var onBackgroundPatternSelected: ((BackgroundPattern) -> Unit)? = null
    var onBackgroundColorSelected: ((BackgroundColor) -> Unit)? = null
    var onBackgroundImageUploadRequested: (() -> Unit)? = null
    var onToolSelected: ((DrawingTool) -> Unit)? = null
    var onEraserModeSelected: ((Boolean) -> Unit)? = null
    
    /**
     * Show color picker popup
     */
    fun showColorPickerPopup(anchorView: View, currentColor: Int) {
        val inflater = LayoutInflater.from(activity)
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
                onColorSelected?.invoke(color)
                colorPopupWindow?.dismiss()
            }
        }
        
        // Show popup above the anchor view
        colorPopupWindow?.showAsDropDown(anchorView, 0, -anchorView.height - 200, Gravity.CENTER)
    }
    
    /**
     * Show stroke width popup
     */
    fun showStrokeWidthPopup(anchorView: View, currentStrokeWidth: Float) {
        val inflater = LayoutInflater.from(activity)
        val popupView = inflater.inflate(R.layout.popup_stroke_width, null)
        
        strokePopupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        
        val seekBar = popupView.findViewById<SeekBar>(R.id.strokeWidthSeekBar)
        
        // Set current value
        seekBar.progress = (currentStrokeWidth - 1).toInt()
        
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val strokeWidth = (progress + 1).toFloat() // Min 1, Max 21
                
                if (fromUser) {
                    onStrokeWidthChanged?.invoke(strokeWidth)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Show popup above the anchor view
        strokePopupWindow?.showAsDropDown(anchorView, 0, -anchorView.height - 150, Gravity.CENTER)
    }
    
    /**
     * Show zoom slider popup
     */
    fun showZoomPopup(anchorView: View) {
        val inflater = LayoutInflater.from(activity)
        val popupView = inflater.inflate(R.layout.popup_zoom_slider, null)
        
        zoomPopupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        
        val zoomSlider = popupView.findViewById<SeekBar>(R.id.zoomSlider)
        val btnResetZoom = popupView.findViewById<Button>(R.id.btnResetZoom)
        
        // Get current zoom level and range
        val currentZoom = whiteboardView.getZoomLevel()
        val minZoom = whiteboardView.getMinZoomLevel()
        val maxZoom = whiteboardView.getMaxZoomLevel()
        
        // Convert zoom level to progress (0-100)
        val progress = ((currentZoom - minZoom) / (maxZoom - minZoom) * 100f).toInt()
        zoomSlider.progress = progress
        
        // Setup slider listener
        zoomSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isUpdatingZoomFromSlider) {
                    // Convert progress (0-100) to zoom level (minZoom-maxZoom)
                    val zoomLevel = minZoom + (progress / 100f) * (maxZoom - minZoom)
                    
                    isUpdatingZoomFromSlider = true
                    whiteboardView.setZoomLevel(zoomLevel)
                    isUpdatingZoomFromSlider = false
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Setup reset button
        btnResetZoom.setOnClickListener {
            whiteboardView.setZoomLevel(1.0f)
            
            // Update slider
            val resetProgress = ((1.0f - minZoom) / (maxZoom - minZoom) * 100f).toInt()
            zoomSlider.progress = resetProgress
        }
        
        // Show popup above the anchor view
        zoomPopupWindow?.showAsDropDown(anchorView, 0, -anchorView.height - 150, Gravity.CENTER)
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
            Toast.makeText(activity, "پاک کن معمولی انتخاب شد", Toast.LENGTH_SHORT).show()
        }
        
        // Selective eraser option
        val selectiveEraserOption = popupView.findViewById<ImageButton>(R.id.selectiveEraserOption)
        selectiveEraserOption.setOnClickListener {
            onToolSelected?.invoke(DrawingTool.SELECTIVE_ERASER)
            onEraserModeSelected?.invoke(true)
            eraserModePopupWindow?.dismiss()
            Toast.makeText(activity, "پاک کن انتخابی انتخاب شد", Toast.LENGTH_SHORT).show()
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
            
            Toast.makeText(activity, "فرمول به تخته اضافه شد - با قلم اعداد را پر کنید", Toast.LENGTH_SHORT).show()
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
        
        // Shape buttons in popup
        val btnLinePopup = popupView.findViewById<ImageButton>(R.id.btnLinePopup)
        val btnCirclePopup = popupView.findViewById<ImageButton>(R.id.btnCirclePopup)
        val btnTrianglePopup = popupView.findViewById<ImageButton>(R.id.btnTrianglePopup)
        val btnRectanglePopup = popupView.findViewById<ImageButton>(R.id.btnRectanglePopup)
        
        // Set click listeners
        btnLinePopup.setOnClickListener {
            onToolSelected?.invoke(DrawingTool.LINE)
            shapesPopupWindow?.dismiss()
        }
        
        btnCirclePopup.setOnClickListener {
            onToolSelected?.invoke(DrawingTool.CIRCLE)
            shapesPopupWindow?.dismiss()
        }
        
        btnTrianglePopup.setOnClickListener {
            onToolSelected?.invoke(DrawingTool.TRIANGLE)
            shapesPopupWindow?.dismiss()
        }
        
        btnRectanglePopup.setOnClickListener {
            onToolSelected?.invoke(DrawingTool.RECTANGLE)
            shapesPopupWindow?.dismiss()
        }
        
        // Show popup at center of screen
        shapesPopupWindow?.showAtLocation(anchorView, Gravity.CENTER, 0, 0)
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
     * Dismiss zoom popup if open
     */
    fun dismissZoomPopup() {
        zoomPopupWindow?.dismiss()
    }
    
    /**
     * Clean up all popup windows
     */
    fun cleanup() {
        colorPopupWindow?.dismiss()
        strokePopupWindow?.dismiss()
        eraserModePopupWindow?.dismiss()
        formulaPopupWindow?.dismiss()
        backgroundPopupWindow?.dismiss()
        zoomPopupWindow?.dismiss()
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

