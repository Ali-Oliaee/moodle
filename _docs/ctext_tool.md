# Explain text tool functionality and libraries
_Exported on 12/11/2025 at 10:12:21 GMT+3:30 from Cursor (2.1.46)_

---

**User**

explain simple the text tool, which libraries used for it

how it works

---

**Cursor**

I'll help you understand the text tool in this whiteboard application. Let me search for the text tool implementation.

# Text Tool - Simple Explanation

## 📚 Libraries Used

The text tool uses **native Android libraries only** - no external dependencies needed:

1. **`android.graphics.Paint`** - For drawing text and visual elements
2. **`android.graphics.Canvas`** - For rendering text on the whiteboard
3. **`android.widget.EditText`** - For the text input dialog
4. **`android.app.AlertDialog`** - For showing the input popup

**Bonus Feature:** There's also a **handwriting recognition** feature using:
- **Google ML Kit Digital Ink Recognition** - Converts handwritten Persian/Farsi text to digital text

---

## 🎯 How It Works (Simple Flow)

### **1. User Taps on Whiteboard**
When the TEXT tool is selected and user taps the whiteboard:

```75:78:app/src/main/java/com/example/moodle/ui/whiteboard/models/TextObject.kt
    fun draw(canvas: Canvas) {
        updatePaint()
        
        canvas.save()
```

### **2. Dialog Appears**
A popup dialog with an EditText field appears asking for text input:

```557:578:app/src/main/java/com/example/moodle/ui/whiteboard/WhiteboardActivity.kt
    private fun showTextInputDialog(x: Float, y: Float) {
        val editText = EditText(this)
        editText.hint = getString(R.string.enter_text)
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_text))
            .setView(editText)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                val text = editText.text.toString().trim()
                if (text.isNotEmpty()) {
                    whiteboardView.addText(text, x, y)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
        
        // Show keyboard automatically
        editText.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
```

### **3. TextObject is Created**
When user confirms, a `TextObject` is created with properties:

```9:17:app/src/main/java/com/example/moodle/ui/whiteboard/models/TextObject.kt
data class TextObject(
    var text: String,
    var x: Float,
    var y: Float,
    var textSize: Float,
    var color: Int,
    var rotation: Float = 0f,
    var isSelected: Boolean = false
) {
```

### **4. Text is Drawn**
The text is rendered on the canvas with Paint:

```58:78:app/src/main/java/com/example/moodle/ui/whiteboard/models/TextObject.kt
    fun draw(canvas: Canvas) {
        updatePaint()
        
        canvas.save()
        
        // Apply rotation around text center
        if (rotation != 0f) {
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            val centerX = x + bounds.width() / 2f
            val centerY = y + bounds.height() / 2f
            canvas.rotate(rotation, centerX, centerY)
        }
        
        canvas.drawText(text, x, y, paint)
        canvas.restore()
        
        if (isSelected) {
            drawSelectionBounds(canvas)
        }
    }
```

### **5. Interactive Features**
When a text object is selected, users can:
- **Move it** - Drag to reposition
- **Resize it** - Use blue handle at bottom-right
- **Rotate it** - Use green handle at top-center
- **Delete it** - Use red X button at top-right

```80:132:app/src/main/java/com/example/moodle/ui/whiteboard/models/TextObject.kt
    private fun drawSelectionBounds(canvas: Canvas) {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        
        val left = x - 10f
        val top = y + bounds.top - 10f
        val right = x + bounds.width() + 10f
        val bottom = y + bounds.bottom + 10f
        
        canvas.save()
        
        // Apply rotation for selection bounds
        if (rotation != 0f) {
            val centerX = x + bounds.width() / 2f
            val centerY = y + bounds.height() / 2f
            canvas.rotate(rotation, centerX, centerY)
        }
        
        // Draw selection rectangle
        canvas.drawRect(left, top, right, bottom, selectionPaint)
        
        // Draw resize handle (blue square at bottom-right)
        val handleSize = 20f
        canvas.drawRect(right - handleSize/2, bottom - handleSize/2, 
                      right + handleSize/2, bottom + handleSize/2, handlePaint)
        
        // Draw delete button (red circle with X at top-right corner)
        val deleteButtonX = right + 20f
        val deleteButtonY = top - 20f
        val deleteButtonRadius = 15f
        
        canvas.drawCircle(deleteButtonX, deleteButtonY, deleteButtonRadius, deleteButtonPaint)
        
        // Draw white X
        val xSize = 8f
        canvas.drawLine(
            deleteButtonX - xSize, deleteButtonY - xSize,
            deleteButtonX + xSize, deleteButtonY + xSize,
            deleteIconPaint
        )
        canvas.drawLine(
            deleteButtonX + xSize, deleteButtonY - xSize,
            deleteButtonX - xSize, deleteButtonY + xSize,
            deleteIconPaint
        )
        
        canvas.restore()
        
        // Draw rotation handle (green circle at top-center, outside rotation)
        val rotationHandleX = x + bounds.width() / 2f
        val rotationHandleY = top - 30f
        canvas.drawCircle(rotationHandleX, rotationHandleY, 10f, rotationHandlePaint)
    }
```

---

## 🎨 Key Components

| Component | Purpose |
|-----------|---------|
| **TextObject** | Data model that stores text properties (position, size, color, rotation) |
| **OnTextInputListener** | Interface that triggers the input dialog |
| **Paint** | Configures how text looks (color, size, style) |
| **Canvas** | The drawing surface where text is rendered |

---

## ✨ Summary

**In simple terms:** 
1. Tap whiteboard → Dialog opens
2. Type text → Press OK
3. Text appears at tapped position
4. Can move, resize, rotate, or delete it later

**Pure Android** - no fancy libraries needed, just standard Android graphics and UI components! 🎯

```plaintext
    fun draw(canvas: Canvas) {
        updatePaint()
        
        canvas.save()
```

```plaintext
    private fun showTextInputDialog(x: Float, y: Float) {
        val editText = EditText(this)
        editText.hint = getString(R.string.enter_text)
        editText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.add_text))
            .setView(editText)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                val text = editText.text.toString().trim()
                if (text.isNotEmpty()) {
                    whiteboardView.addText(text, x, y)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
        
        // Show keyboard automatically
        editText.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(editText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
```

```plaintext
data class TextObject(
    var text: String,
    var x: Float,
    var y: Float,
    var textSize: Float,
    var color: Int,
    var rotation: Float = 0f,
    var isSelected: Boolean = false
) {
```

```plaintext
    fun draw(canvas: Canvas) {
        updatePaint()
        
        canvas.save()
        
        // Apply rotation around text center
        if (rotation != 0f) {
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            val centerX = x + bounds.width() / 2f
            val centerY = y + bounds.height() / 2f
            canvas.rotate(rotation, centerX, centerY)
        }
        
        canvas.drawText(text, x, y, paint)
        canvas.restore()
        
        if (isSelected) {
            drawSelectionBounds(canvas)
        }
    }
```

```plaintext
    private fun drawSelectionBounds(canvas: Canvas) {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        
        val left = x - 10f
        val top = y + bounds.top - 10f
        val right = x + bounds.width() + 10f
        val bottom = y + bounds.bottom + 10f
        
        canvas.save()
        
        // Apply rotation for selection bounds
        if (rotation != 0f) {
            val centerX = x + bounds.width() / 2f
            val centerY = y + bounds.height() / 2f
            canvas.rotate(rotation, centerX, centerY)
        }
        
        // Draw selection rectangle
        canvas.drawRect(left, top, right, bottom, selectionPaint)
        
        // Draw resize handle (blue square at bottom-right)
        val handleSize = 20f
        canvas.drawRect(right - handleSize/2, bottom - handleSize/2, 
                      right + handleSize/2, bottom + handleSize/2, handlePaint)
        
        // Draw delete button (red circle with X at top-right corner)
        val deleteButtonX = right + 20f
        val deleteButtonY = top - 20f
        val deleteButtonRadius = 15f
        
        canvas.drawCircle(deleteButtonX, deleteButtonY, deleteButtonRadius, deleteButtonPaint)
        
        // Draw white X
        val xSize = 8f
        canvas.drawLine(
            deleteButtonX - xSize, deleteButtonY - xSize,
            deleteButtonX + xSize, deleteButtonY + xSize,
            deleteIconPaint
        )
        canvas.drawLine(
            deleteButtonX + xSize, deleteButtonY - xSize,
            deleteButtonX - xSize, deleteButtonY + xSize,
            deleteIconPaint
        )
        
        canvas.restore()
        
        // Draw rotation handle (green circle at top-center, outside rotation)
        val rotationHandleX = x + bounds.width() / 2f
        val rotationHandleY = top - 30f
        canvas.drawCircle(rotationHandleX, rotationHandleY, 10f, rotationHandlePaint)
    }
```

