# Explain simple export manager
_Exported on 12/11/2025 at 09:55:33 GMT+3:30 from Cursor (2.1.46)_

---

**User**

explain simple export manager, which libraries used for it

how it works

---

**Cursor**

I'll help you understand the export manager in your codebase. Let me search for export-related files and functionality.

Now let me check the build.gradle file to see if there are any special dependencies used for export functionality:

Let me also check how it's used in the ViewModel to give you a complete picture:

## Export Manager Explanation

The **ExportManager** is a class that handles exporting whiteboard content into various formats. It's located at `app/src/main/java/com/example/moodle/ui/whiteboard/managers/ExportManager.kt`.

## Libraries Used

The ExportManager uses **only built-in Android libraries** - no external third-party dependencies are required:

### Android Framework Libraries:
- **`android.graphics.*`** - For bitmap creation, canvas drawing, and image manipulation
  - `Bitmap`, `Canvas`, `Paint`, `Path`, `PathMeasure`
- **`android.provider.MediaStore`** - For saving files to external storage (Android 10+ scoped storage)
- **`android.os.Environment`** - For accessing external storage directories
- **`android.content.ContentValues`** - For MediaStore operations
- **`android.util.Base64`** - For encoding images to Base64 in custom format
- **`org.json.*`** - For JSON serialization (JSONObject, JSONArray)
- **`java.io.*`** - For file I/O operations
- **`java.text.SimpleDateFormat`** - For timestamp formatting in filenames

## How It Works

The ExportManager provides **three main export functions**:

### 1. **Export Single Page to PNG** (`exportToPNG`)

```31:78:app/src/main/java/com/example/moodle/ui/whiteboard/managers/ExportManager.kt
companion object {
    private const val EXPORT_FOLDER = "MoodleWhiteboard"
    // Note: For PNG, quality is ignored by Bitmap.compress().
    private const val PNG_QUALITY = 90
    private const val CUSTOM_FORMAT_VERSION = "1.0"
    
    // For display purposes - actual location depends on Android version
    private fun getExportLocationMessage(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Pictures/MoodleWhiteboard/" // Android 10+ uses Pictures for images
        } else {
            "Downloads/MoodleWhiteboard/" // Below Android 10 uses Downloads
        }
    }
}

/**
 * Export current whiteboard view as PNG image
 */
fun exportToPNG(
    whiteboardView: WhiteboardView,
    onSuccess: (File) -> Unit,
    onError: (String) -> Unit
) {
    try {
        // Create bitmap from whiteboard view
        val bitmap = createBitmapFromView(whiteboardView)
        
        // Generate filename with timestamp
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "whiteboard_$timestamp.png"
        
        // Save to external storage
        val file = saveBitmapToFile(bitmap, filename)
        
        if (file != null) {
            onSuccess(file)
            val location = getExportLocationMessage()
            Toast.makeText(
                context, 
                context.getString(R.string.export_png_success_location, location), 
                Toast.LENGTH_LONG
            ).show()
        } else onError(context.getString(R.string.export_png_failed))
    } catch (e: Exception) {
        onError(context.getString(R.string.export_error, e.message))
    }
}
```

**Flow:**
- Creates a bitmap from the whiteboard view
- Generates timestamped filename (e.g., `whiteboard_20251211_143022.png`)
- Saves to Pictures folder (Android 10+) or Downloads (older Android)
- Shows success toast notification

### 2. **Export All Pages to PNG** (`exportAllPagesToPNG`)

```84:165:app/src/main/java/com/example/moodle/ui/whiteboard/managers/ExportManager.kt
fun exportAllPagesToPNG(
    whiteboardView: WhiteboardView,
    pageManager: PageManager,
    onSuccess: (List<File>) -> Unit,
    onError: (String) -> Unit
) {
    val exportedFiles = mutableListOf<File>()
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val currentPageId = pageManager.getCurrentPageId()
    val pageIds = pageManager.getPageIds()
    val handler = Handler(Looper.getMainLooper())
    
    // Function to export a single page
    fun exportPageAtIndex(index: Int) {
        if (index >= pageIds.size) {
            // All pages exported, restore original page
            restorePage(whiteboardView, pageManager, currentPageId)
            
            if (exportedFiles.isNotEmpty()) {
                onSuccess(exportedFiles)
                val location = getExportLocationMessage()
                Toast.makeText(
                    context, 
                    context.getString(R.string.export_multiple_pages_success_location, exportedFiles.size, location), 
                    Toast.LENGTH_LONG
                ).show()
            } else {
                onError(context.getString(R.string.export_png_failed))
            }
            return
        }
        
        try {
            val pageId = pageIds[index]
            val page = pageManager.getPage(pageId)
            
            if (page != null) {
                // Load this page's data into the whiteboard view
                whiteboardView.loadPageData(page.paths, page.textObjects, page.shapeObjects, page.imageObjects)
                whiteboardView.setBackgroundPattern(page.backgroundPattern)
                whiteboardView.invalidate()
                
                // Wait for the view to render, then capture it
                handler.postDelayed({
                    try {
                        // Create bitmap from the whiteboard view
                        val bitmap = createBitmapFromView(whiteboardView)
                        
                        // Generate filename for this page
                        val pageNumber = index + 1
                        val filename = "whiteboard_${timestamp}_page${pageNumber}.png"
                        
                        // Save to external storage
                        val file = saveBitmapToFile(bitmap, filename)
                        
                        if (file != null) {
                            exportedFiles.add(file)
                            // Export next page
                            exportPageAtIndex(index + 1)
                        } else {
                            // Restore original page before returning error
                            restorePage(whiteboardView, pageManager, currentPageId)
                            onError(context.getString(R.string.export_png_failed))
                        }
                    } catch (e: Exception) {
                        restorePage(whiteboardView, pageManager, currentPageId)
                        onError(context.getString(R.string.export_error, e.message))
                    }
                }, 200) // 200ms delay to ensure rendering is complete
            } else {
                // Skip this page and continue
                exportPageAtIndex(index + 1)
            }
        } catch (e: Exception) {
            restorePage(whiteboardView, pageManager, currentPageId)
            onError(context.getString(R.string.export_error, e.message))
        }
    }
    
    // Start exporting from the first page
    exportPageAtIndex(0)
}
```

**Flow:**
- Iterates through all pages recursively
- Temporarily loads each page into the whiteboard view
- Waits 200ms for rendering to complete
- Captures bitmap of each page
- Saves with page number (e.g., `whiteboard_20251211_143022_page1.png`)
- Restores original page when done

### 3. **Export to Custom Moodle Format** (`exportToCustomFormat`)

```181:271:app/src/main/java/com/example/moodle/ui/whiteboard/managers/ExportManager.kt
fun exportToCustomFormat(
    whiteboardView: WhiteboardView,
    pageManager: PageManager,
    onSuccess: (File) -> Unit,
    onError: (String) -> Unit
) {
    try {
        // Create JSON structure for export
        val exportData = JSONObject()
        exportData.put("version", CUSTOM_FORMAT_VERSION)
        exportData.put("exportDate", System.currentTimeMillis())
        exportData.put("appVersion", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
        
        // Export current page data
        val currentPageData = JSONObject()
        currentPageData.put("backgroundPattern", whiteboardView.getBackgroundPattern().id)
        
        // Export paths
        val pathsArray = JSONArray()
        for (path in whiteboardView.getAllPaths()) {
            val pathObj = serializeDrawingPath(path)
            pathsArray.put(pathObj)
        }
        currentPageData.put("paths", pathsArray)
        
        // Export text objects
        val textArray = JSONArray()
        for (textObj in whiteboardView.getAllTextObjects()) {
            val textObjData = serializeTextObject(textObj)
            textArray.put(textObjData)
        }
        currentPageData.put("textObjects", textArray)
        
        // Export shape objects
        val shapesArray = JSONArray()
        for (shapeObj in whiteboardView.getAllShapeObjects()) {
            val shapeObjData = serializeShapeObject(shapeObj)
            shapesArray.put(shapeObjData)
        }
        currentPageData.put("shapeObjects", shapesArray)
        
        // Export image objects
        val imagesArray = JSONArray()
        for (imageObj in whiteboardView.getAllImageObjects()) {
            val imageObjData = serializeImageObject(imageObj)
            imagesArray.put(imageObjData)
        }
        currentPageData.put("imageObjects", imagesArray)
        
        currentPageData.put("splitModeEnabled", false)
        
        exportData.put("currentPage", currentPageData)
        
        // Export all pages from page manager
        val allPagesArray = JSONArray()
        for (pageId in pageManager.getPageIds()) {
            val page = pageManager.getPage(pageId)
            if (page != null) {
                val pageData = serializePage(page)
                allPagesArray.put(pageData)
            }
        }
        exportData.put("allPages", allPagesArray)
        exportData.put("currentPageId", pageManager.getCurrentPageId())
        
        // Export shared settings
        val sharedSettings = JSONObject()
        sharedSettings.put("color", pageManager.sharedColor)
        sharedSettings.put("strokeWidth", pageManager.sharedStrokeWidth.toDouble())
        sharedSettings.put("backgroundPattern", pageManager.sharedBackgroundPattern.id)
        exportData.put("sharedSettings", sharedSettings)
        
        // Generate filename with timestamp
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "whiteboard_$timestamp.moodle"
        
        // Save to file
        val file = saveJsonToFile(exportData.toString(2), filename)
        
        if (file != null) {
            onSuccess(file)
            Toast.makeText(
                context, 
                context.getString(R.string.export_custom_success_location, "Downloads/MoodleWhiteboard/"), 
                Toast.LENGTH_LONG
            ).show()
        } else onError(context.getString(R.string.export_custom_failed))
    } catch (e: Exception) {
        onError(context.getString(R.string.export_error, e.message))
    }
}
```

**Flow:**
- Creates a comprehensive JSON structure containing:
  - Version info and metadata
  - All pages with their content (paths, text, shapes, images)
  - Current page ID
  - Shared settings (colors, stroke width, background pattern)
- Images are encoded as Base64 strings
- Paths are serialized by sampling points along the path
- Saves as `.moodle` file in Downloads folder

## Storage Strategy

The ExportManager handles Android's **scoped storage** correctly:

```297:344:app/src/main/java/com/example/moodle/ui/whiteboard/managers/ExportManager.kt
private fun saveBitmapToFile(bitmap: Bitmap, filename: String): File? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - Use MediaStore API
            saveBitmapUsingMediaStore(bitmap, filename)
        } else {
            // Below Android 10 - Use legacy file API
            saveBitmapLegacy(bitmap, filename)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Save bitmap using MediaStore API (Android 10+)
 * Images must be saved to Pictures directory, not Downloads
 */
private fun saveBitmapUsingMediaStore(bitmap: Bitmap, filename: String): File? {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        // Images can only be saved to Pictures or DCIM, not Downloads
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$EXPORT_FOLDER")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?: return null
    
    return try {
        resolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, outputStream)
        }
        
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
        
        // Return a File representation for compatibility
        File("${Environment.DIRECTORY_PICTURES}/$EXPORT_FOLDER", filename)
    } catch (e: IOException) {
        resolver.delete(uri, null, null)
        null
    }
}
```

- **Android 10+**: Uses MediaStore API with `IS_PENDING` flag
- **Below Android 10**: Uses legacy file system access
- **PNG images**: Saved to `Pictures/MoodleWhiteboard/`
- **Custom format**: Saved to `Downloads/MoodleWhiteboard/`

## Usage Example

From the `WhiteboardViewModel`:

```371:385:app/src/main/java/com/example/moodle/ui/whiteboard/WhiteboardViewModel.kt
fun exportCurrentPageToPNG(whiteboardView: WhiteboardView) {
    exportManager.exportToPNG(
        whiteboardView,
        onSuccess = { file ->
            updateState { state ->
                state.copy(exportSuccess = file.absolutePath)
            }
        },
        onError = { error ->
            updateState { state ->
                state.copy(errorMessage = error)
            }
        }
    )
}
```

The ExportManager is injected via **Hilt dependency injection** and provides callback-based error handling with user-friendly Toast notifications.

```plaintext
companion object {
    private const val EXPORT_FOLDER = "MoodleWhiteboard"
    // Note: For PNG, quality is ignored by Bitmap.compress().
    private const val PNG_QUALITY = 90
    private const val CUSTOM_FORMAT_VERSION = "1.0"
    
    // For display purposes - actual location depends on Android version
    private fun getExportLocationMessage(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Pictures/MoodleWhiteboard/" // Android 10+ uses Pictures for images
        } else {
            "Downloads/MoodleWhiteboard/" // Below Android 10 uses Downloads
        }
    }
}

/**
 * Export current whiteboard view as PNG image
 */
fun exportToPNG(
    whiteboardView: WhiteboardView,
    onSuccess: (File) -> Unit,
    onError: (String) -> Unit
) {
    try {
        // Create bitmap from whiteboard view
        val bitmap = createBitmapFromView(whiteboardView)
        
        // Generate filename with timestamp
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "whiteboard_$timestamp.png"
        
        // Save to external storage
        val file = saveBitmapToFile(bitmap, filename)
        
        if (file != null) {
            onSuccess(file)
            val location = getExportLocationMessage()
            Toast.makeText(
                context, 
                context.getString(R.string.export_png_success_location, location), 
                Toast.LENGTH_LONG
            ).show()
        } else onError(context.getString(R.string.export_png_failed))
    } catch (e: Exception) {
        onError(context.getString(R.string.export_error, e.message))
    }
}
```

```plaintext
fun exportAllPagesToPNG(
    whiteboardView: WhiteboardView,
    pageManager: PageManager,
    onSuccess: (List<File>) -> Unit,
    onError: (String) -> Unit
) {
    val exportedFiles = mutableListOf<File>()
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val currentPageId = pageManager.getCurrentPageId()
    val pageIds = pageManager.getPageIds()
    val handler = Handler(Looper.getMainLooper())
    
    // Function to export a single page
    fun exportPageAtIndex(index: Int) {
        if (index >= pageIds.size) {
            // All pages exported, restore original page
            restorePage(whiteboardView, pageManager, currentPageId)
            
            if (exportedFiles.isNotEmpty()) {
                onSuccess(exportedFiles)
                val location = getExportLocationMessage()
                Toast.makeText(
                    context, 
                    context.getString(R.string.export_multiple_pages_success_location, exportedFiles.size, location), 
                    Toast.LENGTH_LONG
                ).show()
            } else {
                onError(context.getString(R.string.export_png_failed))
            }
            return
        }
        
        try {
            val pageId = pageIds[index]
            val page = pageManager.getPage(pageId)
            
            if (page != null) {
                // Load this page's data into the whiteboard view
                whiteboardView.loadPageData(page.paths, page.textObjects, page.shapeObjects, page.imageObjects)
                whiteboardView.setBackgroundPattern(page.backgroundPattern)
                whiteboardView.invalidate()
                
                // Wait for the view to render, then capture it
                handler.postDelayed({
                    try {
                        // Create bitmap from the whiteboard view
                        val bitmap = createBitmapFromView(whiteboardView)
                        
                        // Generate filename for this page
                        val pageNumber = index + 1
                        val filename = "whiteboard_${timestamp}_page${pageNumber}.png"
                        
                        // Save to external storage
                        val file = saveBitmapToFile(bitmap, filename)
                        
                        if (file != null) {
                            exportedFiles.add(file)
                            // Export next page
                            exportPageAtIndex(index + 1)
                        } else {
                            // Restore original page before returning error
                            restorePage(whiteboardView, pageManager, currentPageId)
                            onError(context.getString(R.string.export_png_failed))
                        }
                    } catch (e: Exception) {
                        restorePage(whiteboardView, pageManager, currentPageId)
                        onError(context.getString(R.string.export_error, e.message))
                    }
                }, 200) // 200ms delay to ensure rendering is complete
            } else {
                // Skip this page and continue
                exportPageAtIndex(index + 1)
            }
        } catch (e: Exception) {
            restorePage(whiteboardView, pageManager, currentPageId)
            onError(context.getString(R.string.export_error, e.message))
        }
    }
    
    // Start exporting from the first page
    exportPageAtIndex(0)
}
```

```plaintext
fun exportToCustomFormat(
    whiteboardView: WhiteboardView,
    pageManager: PageManager,
    onSuccess: (File) -> Unit,
    onError: (String) -> Unit
) {
    try {
        // Create JSON structure for export
        val exportData = JSONObject()
        exportData.put("version", CUSTOM_FORMAT_VERSION)
        exportData.put("exportDate", System.currentTimeMillis())
        exportData.put("appVersion", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
        
        // Export current page data
        val currentPageData = JSONObject()
        currentPageData.put("backgroundPattern", whiteboardView.getBackgroundPattern().id)
        
        // Export paths
        val pathsArray = JSONArray()
        for (path in whiteboardView.getAllPaths()) {
            val pathObj = serializeDrawingPath(path)
            pathsArray.put(pathObj)
        }
        currentPageData.put("paths", pathsArray)
        
        // Export text objects
        val textArray = JSONArray()
        for (textObj in whiteboardView.getAllTextObjects()) {
            val textObjData = serializeTextObject(textObj)
            textArray.put(textObjData)
        }
        currentPageData.put("textObjects", textArray)
        
        // Export shape objects
        val shapesArray = JSONArray()
        for (shapeObj in whiteboardView.getAllShapeObjects()) {
            val shapeObjData = serializeShapeObject(shapeObj)
            shapesArray.put(shapeObjData)
        }
        currentPageData.put("shapeObjects", shapesArray)
        
        // Export image objects
        val imagesArray = JSONArray()
        for (imageObj in whiteboardView.getAllImageObjects()) {
            val imageObjData = serializeImageObject(imageObj)
            imagesArray.put(imageObjData)
        }
        currentPageData.put("imageObjects", imagesArray)
        
        currentPageData.put("splitModeEnabled", false)
        
        exportData.put("currentPage", currentPageData)
        
        // Export all pages from page manager
        val allPagesArray = JSONArray()
        for (pageId in pageManager.getPageIds()) {
            val page = pageManager.getPage(pageId)
            if (page != null) {
                val pageData = serializePage(page)
                allPagesArray.put(pageData)
            }
        }
        exportData.put("allPages", allPagesArray)
        exportData.put("currentPageId", pageManager.getCurrentPageId())
        
        // Export shared settings
        val sharedSettings = JSONObject()
        sharedSettings.put("color", pageManager.sharedColor)
        sharedSettings.put("strokeWidth", pageManager.sharedStrokeWidth.toDouble())
        sharedSettings.put("backgroundPattern", pageManager.sharedBackgroundPattern.id)
        exportData.put("sharedSettings", sharedSettings)
        
        // Generate filename with timestamp
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "whiteboard_$timestamp.moodle"
        
        // Save to file
        val file = saveJsonToFile(exportData.toString(2), filename)
        
        if (file != null) {
            onSuccess(file)
            Toast.makeText(
                context, 
                context.getString(R.string.export_custom_success_location, "Downloads/MoodleWhiteboard/"), 
                Toast.LENGTH_LONG
            ).show()
        } else onError(context.getString(R.string.export_custom_failed))
    } catch (e: Exception) {
        onError(context.getString(R.string.export_error, e.message))
    }
}
```

```plaintext
private fun saveBitmapToFile(bitmap: Bitmap, filename: String): File? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - Use MediaStore API
            saveBitmapUsingMediaStore(bitmap, filename)
        } else {
            // Below Android 10 - Use legacy file API
            saveBitmapLegacy(bitmap, filename)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Save bitmap using MediaStore API (Android 10+)
 * Images must be saved to Pictures directory, not Downloads
 */
private fun saveBitmapUsingMediaStore(bitmap: Bitmap, filename: String): File? {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        // Images can only be saved to Pictures or DCIM, not Downloads
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$EXPORT_FOLDER")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?: return null
    
    return try {
        resolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, outputStream)
        }
        
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
        
        // Return a File representation for compatibility
        File("${Environment.DIRECTORY_PICTURES}/$EXPORT_FOLDER", filename)
    } catch (e: IOException) {
        resolver.delete(uri, null, null)
        null
    }
}
```

```plaintext
fun exportCurrentPageToPNG(whiteboardView: WhiteboardView) {
    exportManager.exportToPNG(
        whiteboardView,
        onSuccess = { file ->
            updateState { state ->
                state.copy(exportSuccess = file.absolutePath)
            }
        },
        onError = { error ->
            updateState { state ->
                state.copy(errorMessage = error)
            }
        }
    )
}
```

