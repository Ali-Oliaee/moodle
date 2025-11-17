# Whiteboard Feature - Developer Guide

## 📋 Overview

The Whiteboard feature is a multi-page drawing application built with **MVVM architecture** and follows **clean separation of concerns**. It supports drawing, text, shapes, images, zoom/pan, split-screen mode, and import/export functionality.

---

## 🏗️ Architecture Layers

### **1. Presentation Layer** (UI)

- **WhiteboardActivity** - Main UI controller, handles user interactions and Android lifecycle
- **WhiteboardView** - Custom View for rendering and touch events

### **2. ViewModel Layer** (MVVM)

- **WhiteboardViewModel** - Single source of truth for UI state, exposes LiveData

### **3. Business Logic Layer** (Managers)

- **PageManager** - Multi-page navigation and state management
- **DrawingStateManager** - Drawing data (paths, text, shapes, images)
- **BitmapManager** - Bitmap/Canvas management and memory
- **CanvasRenderManager** - Rendering all drawing elements
- **PaintHelper** - Paint creation and styling
- **SectionManager** - Split-screen mode management
- **ExportManager** - Export to PNG and custom format
- **ImportManager** - Import images and custom files

### **4. Controller Layer** (UI-specific)

- **ZoomPanController** - Zoom and pan gestures
- **PopupController** - Popup windows (color picker, tools, etc.)
- **RecordingController** - Screen recording functionality
- **SplitModeController** - Split screen UI coordination

### **5. Utility Layer**

- **TouchGestureHandler** - Touch event processing and routing

### **6. Model Layer** (Data Classes)

- **PageData** - Represents a single whiteboard page
- **DrawingPath** - A drawn path with paint and tool info
- **DrawingTool** - Enum of available tools (PEN, ERASER, TEXT, etc.)
- **TextObject** - Text element on canvas
- **ShapeObject** - Shape element on canvas
- **ImageObject** - Image element on canvas
- **BackgroundPattern** - Background patterns (PLAIN, GRID, LINES, DOTS)
- **BackgroundColor** - Background colors
- **WhiteboardViewState** - UI state snapshot

### **7. Listener Layer** (Callbacks)

- **OnDrawingChangeListener** - Notifies drawing changes
- **OnTextInputListener** - Requests text input dialog

---

## 📊 Data Flow

### Drawing Flow (User draws on screen)

```
User Touch Event
  → WhiteboardView.onTouchEvent()
  → TouchGestureHandler.handleDrawingTouch()
  → DrawingStateManager (updates paths)
  → BitmapManager (draws to bitmap)
  → WhiteboardView.invalidate()
  → WhiteboardView.onDraw()
  → CanvasRenderManager.drawCanvas()
  → Screen Updated
```

### Tool Selection Flow (User selects a tool)

```
User clicks tool button
  → WhiteboardActivity button click
  → WhiteboardViewModel.selectTool()
  → Updates uiState LiveData
  → WhiteboardActivity observes change
  → WhiteboardView.setDrawingTool()
  → DrawingStateManager.currentTool updated
```

### Page Navigation Flow (User changes page)

```
User clicks next/previous page
  → WhiteboardActivity button click
  → Save current page data to ViewModel
  → WhiteboardViewModel.nextPage/previousPage()
  → PageManager navigates to page
  → PageManager.OnPageChangeListener triggered
  → WhiteboardViewModel updates state
  → WhiteboardActivity.loadCurrentPageFromViewModel()
  → WhiteboardView.loadPageData()
  → DrawingStateManager cleared and reloaded
  → Screen redraws
```

### Export Flow (User exports to PNG)

```
User clicks export button
  → WhiteboardActivity.showExportOptionsDialog()
  → User selects PNG export
  → WhiteboardViewModel.exportCurrentPageToPNG()
  → ExportManager.exportToPNG()
  → WhiteboardView creates bitmap
  → ExportManager saves to file
  → Success/Error updates state
  → Toast message displayed
```

---

## 🔄 Function Call Order

### **App Startup Flow**

```
1. WhiteboardActivity.onCreate()
2. Initialize ViewModel (with DI)
3. WhiteboardActivity.initializeViews()
4. WhiteboardActivity.initializeUIControllers()
5. WhiteboardActivity.setupListeners()
6. WhiteboardActivity.observeViewModel()
7. WhiteboardActivity.loadCurrentPageFromViewModel()
8. WhiteboardView created
9. WhiteboardView.init() - creates all managers
10. WhiteboardView.onSizeChanged() - initializes bitmaps
```

### **Drawing a Line (Touch Events)**

```
1. User touches screen
2. WhiteboardView.onTouchEvent(ACTION_DOWN)
3. TouchGestureHandler.handleDrawingTouch()
4. DrawingStateManager creates new Path
5. BitmapManager.drawingCanvas used
6. User moves finger
7. WhiteboardView.onTouchEvent(ACTION_MOVE)
8. TouchGestureHandler updates Path
9. WhiteboardView.invalidate()
10. WhiteboardView.onDraw()
11. CanvasRenderManager.drawCanvas()
12. User lifts finger
13. WhiteboardView.onTouchEvent(ACTION_UP)
14. DrawingStateManager.addPath()
15. OnDrawingChangeListener.onDrawingChanged()
16. WhiteboardActivity saves to ViewModel
```

### **Adding Text**

```
1. User selects TEXT tool
2. User taps on canvas
3. TouchGestureHandler detects TEXT tool
4. OnTextInputListener.onTextInputRequested()
5. WhiteboardViewModel.requestTextInput()
6. WhiteboardActivity observes event
7. WhiteboardActivity.showTextInputDialog()
8. User enters text and clicks OK
9. WhiteboardView.addText()
10. DrawingStateManager.addTextObject()
11. WhiteboardView.invalidate()
12. CanvasRenderManager renders text
```

### **Changing Pages**

```
1. User clicks next page button
2. WhiteboardActivity.saveCurrentPageToViewModel()
3. WhiteboardViewModel.nextPageOrCreate()
4. PageManager.goToNextPage() or createNewPage()
5. PageManager.OnPageChangeListener.onPageChanged()
6. WhiteboardViewModel updates pagination state
7. WhiteboardActivity.handleStateUpdate() (pageChangeEvent)
8. WhiteboardActivity.loadCurrentPageFromViewModel()
9. WhiteboardView.loadPageData()
10. DrawingStateManager.clear()
11. DrawingStateManager loads new page data
12. CanvasRenderManager.redrawCanvas()
13. WhiteboardView.invalidate()
```

---

## 💾 State Management

### **UI State (Observable)**

- Managed by **WhiteboardViewModel**
- Exposed via **LiveData**
- WhiteboardActivity observes and reacts to changes

### **Drawing State (Transient)**

- Managed by **DrawingStateManager**
- Current tool, color, stroke width
- Active paths, text objects, shape objects, image objects

### **Page State (Persistent)**

- Managed by **PageManager**
- Multiple pages with unique IDs
- Each page contains its own drawing data
- Shared settings (color, stroke width) across pages

### **Preferences (Persistent)**

- Managed by **UserPreferences** (DataStore)
- Current page ID, page list, shared settings
- Survives app restarts

---

## 🎨 Key Classes Responsibilities

| Class                   | Single Responsibility                       |
| ----------------------- | ------------------------------------------- |
| **WhiteboardActivity**  | UI coordination and Android lifecycle       |
| **WhiteboardViewModel** | Business logic and UI state management      |
| **WhiteboardView**      | Custom view rendering and touch handling    |
| **DrawingStateManager** | Maintains drawing elements in memory        |
| **PageManager**         | Multi-page navigation and storage           |
| **CanvasRenderManager** | Renders all drawing to canvas               |
| **TouchGestureHandler** | Processes touch events into drawing actions |
| **BitmapManager**       | Bitmap creation and memory management       |
| **ZoomPanController**   | Zoom and pan transformations                |
| **ExportManager**       | File export operations                      |
| **ImportManager**       | File import operations                      |

---

## 📝 Examples

### **Example 1: Adding a New Tool**

**Step 1:** Add tool to enum

```kotlin
// models/DrawingTool.kt
enum class DrawingTool {
    PEN, ERASER, TEXT, HIGHLIGHTER  // Add HIGHLIGHTER
}
```

**Step 2:** Update TouchGestureHandler to handle new tool

```kotlin
// utils/TouchGestureHandler.kt
when (stateManager.currentTool) {
    DrawingTool.HIGHLIGHTER -> {
        // Handle highlighter drawing logic
        paint.alpha = 128  // Semi-transparent
    }
}
```

**Step 3:** Add UI button in WhiteboardActivity

```kotlin
btnHighlighter.setOnClickListener {
    viewModel.selectTool(DrawingTool.HIGHLIGHTER)
}
```

---

### **Example 2: Implementing Custom Export Format**

```kotlin
// In ExportManager.kt
fun exportToCustomFormat(
    whiteboardView: WhiteboardView,
    pageManager: PageManager,
    onSuccess: (File) -> Unit,
    onError: (String) -> Unit
) {
    try {
        // 1. Get all pages
        val allPages = pageManager.getPageIds()

        // 2. Create JSON structure
        val jsonData = JSONObject()
        jsonData.put("version", "1.0")
        jsonData.put("totalPages", allPages.size)

        // 3. Export each page
        val pagesArray = JSONArray()
        for (pageId in allPages) {
            val pageData = pageManager.getPage(pageId)
            val pageJson = serializePageData(pageData)
            pagesArray.put(pageJson)
        }
        jsonData.put("pages", pagesArray)

        // 4. Save to file
        val file = File(context.getExternalFilesDir(null), "whiteboard.moodle")
        file.writeText(jsonData.toString())

        onSuccess(file)
    } catch (e: Exception) {
        onError(e.message ?: "Export failed")
    }
}
```

---

### **Example 3: Observing State Changes**

```kotlin
// In WhiteboardActivity.kt
private fun observeViewModel() {
    viewModel.uiState.observe(this, Observer { state ->
        state?.let {
            // Update UI based on state
            whiteboardView.setDrawingTool(state.currentTool)
            whiteboardView.setColor(state.currentColor)
            whiteboardView.setStrokeWidth(state.currentStrokeWidth)

            // Update button states
            btnUndo.isEnabled = state.canUndo
            btnRedo.isEnabled = state.canRedo

            // Update pagination
            tvPageIndicator.text = "${state.currentPageNumber}/${state.totalPages}"

            // Handle one-time events
            state.showTextInputDialog?.let { coords ->
                showTextInputDialog(coords.first, coords.second)
                viewModel.clearTextInputDialogEvent()
            }
        }
    })
}
```

---

### **Example 4: Adding a Custom Background Pattern**

**Step 1:** Add pattern to enum

```kotlin
// models/BackgroundPattern.kt
enum class BackgroundPattern(val id: Int) {
    PLAIN(0),
    GRID(1),
    LINED(2),
    DOTS(3),
    CHECKERED(4)  // Add new pattern
}
```

**Step 2:** Implement rendering

```kotlin
// managers/CanvasRenderManager.kt
private fun drawBackgroundPattern(canvas: Canvas) {
    when (currentBackgroundPattern) {
        BackgroundPattern.CHECKERED -> {
            val squareSize = 50f
            val paint = Paint().apply {
                color = Color.LTGRAY
            }

            for (x in 0 until canvas.width step (squareSize * 2).toInt()) {
                for (y in 0 until canvas.height step (squareSize * 2).toInt()) {
                    canvas.drawRect(x.toFloat(), y.toFloat(),
                                   x + squareSize, y + squareSize, paint)
                    canvas.drawRect(x + squareSize, y + squareSize,
                                   x + squareSize * 2, y + squareSize * 2, paint)
                }
            }
        }
    }
}
```

---

### **Example 5: Implementing Undo/Redo**

The undo/redo system works with stacks:

```kotlin
// In DrawingStateManager.kt
private val paths = mutableListOf<DrawingPath>()  // Current paths
private val undoStack = mutableListOf<DrawingPath>()  // Undo history

fun undo(): Boolean {
    if (paths.isNotEmpty()) {
        val lastPath = paths.removeAt(paths.size - 1)
        undoStack.add(lastPath)  // Move to undo stack
        return true
    }
    return false
}

fun redo(): Boolean {
    if (undoStack.isNotEmpty()) {
        val path = undoStack.removeAt(undoStack.size - 1)
        paths.add(path)  // Restore from undo stack
        return true
    }
    return false
}

fun addPath(path: DrawingPath) {
    paths.add(path)
    undoStack.clear()  // Clear undo stack when new action performed
}
```

**Usage in Activity:**

```kotlin
btnUndo.setOnClickListener {
    whiteboardView.undo()
    saveCurrentPageToViewModel()
    updateUndoRedoState()
}
```

---

## 🔑 Key Design Patterns

1. **MVVM** - Separation of UI, business logic, and data
2. **Observer Pattern** - LiveData for reactive UI updates
3. **Delegation** - WhiteboardView delegates to specialized managers
4. **Dependency Injection** - ViewModelFactory provides dependencies
5. **Single Responsibility** - Each class has one clear purpose
6. **Command Pattern** - Undo/Redo implementation

---

## 🚀 Quick Start for New Developers

1. **Start with WhiteboardActivity** - Understand UI flow
2. **Explore WhiteboardViewModel** - See how state is managed
3. **Check WhiteboardView** - Learn how rendering works
4. **Review DrawingStateManager** - Understand data structures
5. **Look at TouchGestureHandler** - See touch event processing
6. **Study PageManager** - Understand multi-page logic

---

## 📚 Common Tasks

### Adding a new feature button

1. Add button to XML layout
2. Initialize in `WhiteboardActivity.initializeViews()`
3. Setup listener in `WhiteboardActivity.setupListeners()`
4. Add logic to `WhiteboardViewModel`
5. Update UI state in `WhiteboardViewState`

### Modifying drawing behavior

1. Update `DrawingTool` enum if needed
2. Modify `TouchGestureHandler.handleDrawingTouch()`
3. Update rendering in `CanvasRenderManager`

### Adding persistence

1. Add fields to `PageData` model
2. Update save/load in `WhiteboardActivity`
3. Update `PageManager` if needed
4. Add to `UserPreferences` for app-wide settings

---

## 🎯 Architecture Benefits

✅ **Testable** - Business logic separated from Android framework  
✅ **Maintainable** - Clear separation of concerns  
✅ **Scalable** - Easy to add new tools and features  
✅ **Reusable** - Managers can be reused in other contexts  
✅ **Understandable** - Each class has a single, clear purpose

---

## 📞 Need Help?

- Check `DI_QUICK_REFERENCE.md` for dependency injection details
- Review `WHITEBOARD_DI_SETUP.md` for setup instructions
- Explore the inline documentation in each file

Happy coding! 🎨
