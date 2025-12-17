# Explain math formula tool and libraries
_Exported on 12/11/2025 at 10:17:26 GMT+3:30 from Cursor (2.1.46)_

---

**User**

explain simple the math fomrula tool, which libraries used for it

how it works

---

**Cursor**

I'll help you understand the math formula tool in your project. Let me search for the relevant code.

## Math Formula Tool - Simple Explanation

Based on your codebase, here's how the math formula tool works:

### **📚 What It Does**
The math formula tool is a **simple template-based system** that helps users quickly insert common mathematical formulas onto the whiteboard as text.

### **🛠️ Libraries Used**
**None!** 

The tool doesn't use any specialized math rendering libraries like:
- ❌ LaTeX
- ❌ MathJax  
- ❌ KaTeX

It uses only **standard Android components**:
- `PopupWindow` - for displaying the formula picker
- `TextView` - for showing formulas
- `LinearLayout` - for organizing the UI

### **⚙️ How It Works**

**1. Pre-defined Formula Templates:**

```234:263:app/src/main/java/com/example/moodle/ui/whiteboard/controllers/PopupController.kt
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
```

The tool has **8 pre-defined formulas**:
- Quadratic Formula
- Pythagorean Theorem  
- Circle Area
- Distance Formula
- Slope Formula
- Sphere Volume
- Compound Interest
- Law of Cosines

**2. Template Structure:**

```352:403:app/src/main/java/com/example/moodle/ui/whiteboard/controllers/PopupController.kt
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
```

Each formula has:
- **Name** - Formula identifier
- **Template** - Formula with placeholders like `{a}`, `{b}`
- **Variables** - List of variable names
- **Descriptions** - Persian descriptions for each variable

**3. Adding Formula to Board:**

```279:295:app/src/main/java/com/example/moodle/ui/whiteboard/controllers/PopupController.kt
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
```

When user selects a formula:
1. Replace `{variables}` with empty box symbols ☐
2. Add the formula as **plain text** to the whiteboard center
3. User can then manually edit/fill in values

**4. Display on Canvas:**

The formula is rendered using `TextObject` class, which uses Android's standard `Paint` and `Canvas` APIs to draw text.

### **✅ Summary**
- **Simple approach**: No complex math rendering
- **Template-based**: Pre-defined formulas with placeholders
- **Plain text**: Formulas are just regular text on the canvas
- **User fills values**: Placeholders become ☐ symbols that users can edit
- **Pure Android**: Only standard Android UI components

This approach is lightweight and works well for basic educational needs!

```plaintext
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
```

```plaintext
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
```

```plaintext
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
```

