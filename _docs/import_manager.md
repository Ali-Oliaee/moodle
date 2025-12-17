# Explain simple import manager
_Exported on 12/11/2025 at 09:58:03 GMT+3:30 from Cursor (2.1.46)_

---

**User**

explain simple import manager, which libraries used for it

how it works

---

**Cursor**

I'll help you understand the import manager in your codebase. Let me search for import manager related code.

Now let me check how it's used in the codebase to give you a complete picture:

# ImportManager Explanation

## Overview
The **ImportManager** is a utility class in your Moodle whiteboard application that handles importing content into the whiteboard. It supports two types of imports:
1. **Image files** (PNG, JPEG, etc.)
2. **Custom `.moodle` format files** (JSON-based whiteboard data)

## Libraries Used

The ImportManager uses **only Android standard libraries**:

```18:18:app/src/main/java/com/example/moodle/ui/whiteboard/managers/ImportManager.kt
class ImportManager(private val context: Context) {
```

**Core Android libraries:**
- `android.content.Context` - For accessing app resources and content resolver
- `android.graphics.*` - For bitmap operations (Bitmap, Paint, Path, BitmapFactory)
- `android.net.Uri` - For handling file URIs
- `android.util.Base64` - For decoding Base64-encoded image data
- `android.widget.Toast` - For showing user notifications
- `org.json.JSONObject` - For parsing JSON data

**No external third-party libraries** - it's a lightweight, self-contained solution using Android SDK only.

---

## How It Works

### 1. **Dependency Injection (Hilt)**

The ImportManager is provided as a **Singleton** through Dagger Hilt:

```52:58:app/src/main/java/com/example/moodle/di/WhiteboardModule.kt
    @Provides
    @Singleton
    fun provideImportManager(
        @ApplicationContext context: Context
    ): ImportManager {
        return ImportManager(context)
    }
```

This ensures only one instance exists throughout the app lifecycle.

---

### 2. **Image Import Flow**

```28:59:app/src/main/java/com/example/moodle/ui/whiteboard/managers/ImportManager.kt
    fun importImage(
        uri: Uri,
        whiteboardView: WhiteboardView,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                onError(context.getString(R.string.import_image_failed))
                return
            }
            
            // Decode bitmap with size limits
            val bitmap = decodeBitmapFromStream(inputStream)
            if (bitmap == null) {
                onError(context.getString(R.string.import_image_invalid))
                return
            }
            
            // Add the image to the whiteboard at center of current view
            val centerX = whiteboardView.getVisibleCanvasCenterX()
            val centerY = whiteboardView.getVisibleCanvasCenterY()
            whiteboardView.addImage(bitmap, centerX, centerY)
            
            onSuccess()
            Toast.makeText(context, context.getString(R.string.import_image_success), Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            onError(context.getString(R.string.import_error, e.message))
        }
    }
```

**Steps:**
1. Opens the file URI using `ContentResolver`
2. Decodes the bitmap with **size constraints** (max 2048x2048) to prevent memory issues
3. Calculates the center of the visible canvas
4. Adds the image to the whiteboard at that position
5. Shows success/error feedback

**Memory Optimization:**

```215:237:app/src/main/java/com/example/moodle/ui/whiteboard/managers/ImportManager.kt
    private fun decodeBitmapFromStream(inputStream: InputStream): Bitmap? {
        return try {
            // First, decode bounds to check size
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            
            val bufferedStream = inputStream.buffered()
            bufferedStream.mark(bufferedStream.available())
            BitmapFactory.decodeStream(bufferedStream, null, options)
            bufferedStream.reset()
            
            // Calculate sample size to fit within max dimensions
            val sampleSize = calculateSampleSize(options.outWidth, options.outHeight)
            
            // Decode actual bitmap
            options.inJustDecodeBounds = false
            options.inSampleSize = sampleSize
            
            BitmapFactory.decodeStream(bufferedStream, null, options)
        } catch (e: Exception) {
            null
        }
    }
```

Uses **two-pass decoding**:
- First pass: Gets image dimensions without loading into memory
- Calculates downsampling factor if image is too large
- Second pass: Loads the downsampled image

---

### 3. **Custom Format Import Flow**

```64:140:app/src/main/java/com/example/moodle/ui/whiteboard/managers/ImportManager.kt
    fun importCustomFormat(
        uri: Uri,
        whiteboardView: WhiteboardView,
        pageManager: PageManager,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                onError(context.getString(R.string.import_custom_failed))
                return
            }
            
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val importData = JSONObject(jsonString)
            
            // Validate version compatibility
            val version = importData.optString("version", "")
            if (version != SUPPORTED_CUSTOM_VERSION) {
                onError(context.getString(R.string.import_version_mismatch, version, SUPPORTED_CUSTOM_VERSION))
                return
            }
            
            // Clear current whiteboard state
            whiteboardView.clear()
            
            // Import shared settings
            if (importData.has("sharedSettings")) {
                val sharedSettings = importData.getJSONObject("sharedSettings")
                val color = sharedSettings.optInt("color", Color.BLACK)
                val strokeWidth = sharedSettings.optDouble("strokeWidth", 8.0).toFloat()
                val backgroundPatternId = sharedSettings.optInt("backgroundPattern", BackgroundPattern.PLAIN.id)
                
                pageManager.updateSharedColor(color)
                pageManager.updateSharedStrokeWidth(strokeWidth)
                pageManager.updateSharedBackgroundPattern(BackgroundPattern.fromId(backgroundPatternId))
                
                whiteboardView.setColor(color)
                whiteboardView.setStrokeWidth(strokeWidth)
                whiteboardView.setBackgroundPattern(BackgroundPattern.fromId(backgroundPatternId))
            }
            
            // Import all pages if available
            if (importData.has("allPages")) {
                val allPagesArray = importData.getJSONArray("allPages")
                // Clear existing pages first
                pageManager.clearAllPages()
                
                for (i in 0 until allPagesArray.length()) {
                    val pageData = allPagesArray.getJSONObject(i)
                    val page = deserializePage(pageData)
                    pageManager.addPage(page)
                }
                
                // Set current page if specified
                val currentPageId = importData.optInt("currentPageId", 1)
                val pageLoaded = pageManager.goToPage(currentPageId)
                
                // If the specified page doesn't exist, go to the first available page
                if (!pageLoaded && pageManager.getTotalPages() > 0) {
                    val firstPageId = pageManager.getPageIds().firstOrNull() ?: 1
                    pageManager.goToPage(firstPageId)
                }
            } else if (importData.has("currentPage")) {
                // Import current page data only if we don't have allPages
                val currentPageData = importData.getJSONObject("currentPage")
                importPageData(currentPageData, whiteboardView)
            }
            
            onSuccess()
            Toast.makeText(context, context.getString(R.string.import_custom_success), Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            onError(context.getString(R.string.import_error, e.message))
        }
    }
```

**Steps:**
1. Reads the `.moodle` file as JSON
2. **Validates version** (currently supports version "1.0")
3. Clears the current whiteboard
4. Imports **shared settings** (color, stroke width, background pattern)
5. Imports **all pages** or just the current page
6. Deserializes each page's content:
   - Drawing paths (freehand strokes)
   - Text objects
   - Shape objects (rectangles, circles, lines, triangles)
   - Image objects (Base64-encoded)

---

### 4. **Deserialization Process**

The manager deserializes different object types:

**a) Drawing Paths:**

```259:371:app/src/main/java/com/example/moodle/ui/whiteboard/managers/ImportManager.kt
    private fun deserializeDrawingPath(pathObj: JSONObject): DrawingPath? {
        return try {
            // Deserialize tool
            val toolName = pathObj.optString("tool", "PEN")
            val tool = DrawingTool.valueOf(toolName)
            
            // Deserialize paint
            val paintObj = pathObj.optJSONObject("paint")
            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                
                if (paintObj != null) {
                    color = paintObj.optInt("color", Color.BLACK)
                    strokeWidth = paintObj.optDouble("strokeWidth", 8.0).toFloat()
                    alpha = paintObj.optInt("alpha", 255)
                }
            }
            
            // Get text data if present
            val text = if (pathObj.has("text")) pathObj.getString("text") else null
            val textX = pathObj.optDouble("textX", 0.0).toFloat()
            val textY = pathObj.optDouble("textY", 0.0).toFloat()
            
            // Get shape data if present
            val shapeStartX = pathObj.optDouble("shapeStartX", 0.0).toFloat()
            val shapeStartY = pathObj.optDouble("shapeStartY", 0.0).toFloat()
            val shapeEndX = pathObj.optDouble("shapeEndX", 0.0).toFloat()
            val shapeEndY = pathObj.optDouble("shapeEndY", 0.0).toFloat()
            
            // Create path (simplified - for now create a basic path)
            val path = Path()
            
            // If it's a shape, recreate the shape path
            if (tool in listOf(
                    DrawingTool.LINE,
                    DrawingTool.CIRCLE,
                    DrawingTool.TRIANGLE,
                    DrawingTool.RECTANGLE
                )) {
                // Recreate shape path from stored coordinates
                when (tool) {
                    DrawingTool.LINE -> {
                        path.moveTo(shapeStartX, shapeStartY)
                        path.lineTo(shapeEndX, shapeEndY)
                    }
                    DrawingTool.RECTANGLE -> {
                        val left = kotlin.math.min(shapeStartX, shapeEndX)
                        val top = kotlin.math.min(shapeStartY, shapeEndY)
                        val right = kotlin.math.max(shapeStartX, shapeEndX)
                        val bottom = kotlin.math.max(shapeStartY, shapeEndY)
                        path.addRect(left, top, right, bottom, Path.Direction.CW)
                    }
                    DrawingTool.CIRCLE -> {
                        val centerX = (shapeStartX + shapeEndX) / 2
                        val centerY = (shapeStartY + shapeEndY) / 2
                        val radius = kotlin.math.sqrt(
                            (shapeEndX - shapeStartX) * (shapeEndX - shapeStartX) + 
                            (shapeEndY - shapeStartY) * (shapeEndY - shapeStartY)
                        ).toFloat() / 2
                        path.addCircle(centerX, centerY, radius, Path.Direction.CW)
                    }
                    DrawingTool.TRIANGLE -> {
                        val centerX = (shapeStartX + shapeEndX) / 2
                        val centerY = (shapeStartY + shapeEndY) / 2
                        val width = kotlin.math.abs(shapeEndX - shapeStartX)
                        val height = kotlin.math.abs(shapeEndY - shapeStartY)
                        val size = kotlin.math.max(width, height)
                        
                        val topX = centerX
                        val topY = centerY - size / 2
                        val leftX = centerX - size / 2
                        val leftY = centerY + size / 2
                        val rightX = centerX + size / 2
                        val rightY = centerY + size / 2
                        
                        path.moveTo(topX, topY)
                        path.lineTo(leftX, leftY)
                        path.lineTo(rightX, rightY)
                        path.close()
                    }
                    else -> {
                        // Default line
                        path.moveTo(shapeStartX, shapeStartY)
                        path.lineTo(shapeEndX, shapeEndY)
                    }
                }
            } else {
                // For freehand drawing, deserialize path from pathData
                val pathData = pathObj.optString("pathData", "")
                if (pathData.isNotEmpty()) {
                    deserializePath(pathData, path)
                }
            }
            
            DrawingPath(
                path = path,
                paint = paint,
                tool = tool,
                text = text,
                textX = textX,
                textY = textY,
                shapeStartX = shapeStartX,
                shapeStartY = shapeStartY,
                shapeEndX = shapeEndX,
                shapeEndY = shapeEndY
            )
        } catch (e: Exception) {
            null
        }
    }
```

**b) Images (Base64 decoded):**

```420:444:app/src/main/java/com/example/moodle/ui/whiteboard/managers/ImportManager.kt
    private fun deserializeImageObject(imageObj: JSONObject): ImageObject? {
        return try {
            // Decode bitmap from Base64
            val bitmapBase64 = imageObj.getString("bitmapData")
            val bitmapBytes = Base64.decode(bitmapBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.size)
            
            if (bitmap == null) {
                return null
            }
            
            ImageObject(
                bitmap = bitmap,
                x = imageObj.optDouble("x", 0.0).toFloat(),
                y = imageObj.optDouble("y", 0.0).toFloat(),
                width = imageObj.optDouble("width", bitmap.width.toDouble()).toFloat(),
                height = imageObj.optDouble("height", bitmap.height.toDouble()).toFloat(),
                rotation = imageObj.optDouble("rotation", 0.0).toFloat(),
                scaleX = imageObj.optDouble("scaleX", 1.0).toFloat(),
                scaleY = imageObj.optDouble("scaleY", 1.0).toFloat()
            )
        } catch (e: Exception) {
            null
        }
    }
```

**c) Path data deserialization:**

```511:538:app/src/main/java/com/example/moodle/ui/whiteboard/managers/ImportManager.kt
    private fun deserializePath(pathData: String, path: Path) {
        if (pathData.isEmpty()) return
        
        try {
            val pointPairs = pathData.split(";")
            if (pointPairs.isEmpty()) return
            
            // Move to first point
            val firstPoint = pointPairs[0].split(",")
            if (firstPoint.size == 2) {
                val x = firstPoint[0].toFloatOrNull() ?: return
                val y = firstPoint[1].toFloatOrNull() ?: return
                path.moveTo(x, y)
            }
            
            // Draw lines to subsequent points
            for (i in 1 until pointPairs.size) {
                val point = pointPairs[i].split(",")
                if (point.size == 2) {
                    val x = point[0].toFloatOrNull() ?: continue
                    val y = point[1].toFloatOrNull() ?: continue
                    path.lineTo(x, y)
                }
            }
        } catch (e: Exception) {
            // If deserialization fails, leave path empty
        }
    }
```

Format: `"x1,y1;x2,y2;x3,y3;..."` - semicolon-separated coordinate pairs

---

## Key Features

✅ **Memory-efficient** - Downsamples large images  
✅ **Version control** - Validates file format version  
✅ **Error handling** - Graceful failures with user feedback  
✅ **Multi-page support** - Can import entire whiteboard sessions  
✅ **Dependency injection** - Uses Hilt for clean architecture  
✅ **Type safety** - Null-safe Kotlin with proper error handling  
✅ **Localized strings** - All user messages use resource strings

The ImportManager is a well-designed, lightweight component that handles complex file imports without any third-party dependencies! 🎨

```plaintext
class ImportManager(private val context: Context) {
```

```plaintext
    @Provides
    @Singleton
    fun provideImportManager(
        @ApplicationContext context: Context
    ): ImportManager {
        return ImportManager(context)
    }
```

```plaintext
    fun importImage(
        uri: Uri,
        whiteboardView: WhiteboardView,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                onError(context.getString(R.string.import_image_failed))
                return
            }
            
            // Decode bitmap with size limits
            val bitmap = decodeBitmapFromStream(inputStream)
            if (bitmap == null) {
                onError(context.getString(R.string.import_image_invalid))
                return
            }
            
            // Add the image to the whiteboard at center of current view
            val centerX = whiteboardView.getVisibleCanvasCenterX()
            val centerY = whiteboardView.getVisibleCanvasCenterY()
            whiteboardView.addImage(bitmap, centerX, centerY)
            
            onSuccess()
            Toast.makeText(context, context.getString(R.string.import_image_success), Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            onError(context.getString(R.string.import_error, e.message))
        }
    }
```

```plaintext
    private fun decodeBitmapFromStream(inputStream: InputStream): Bitmap? {
        return try {
            // First, decode bounds to check size
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            
            val bufferedStream = inputStream.buffered()
            bufferedStream.mark(bufferedStream.available())
            BitmapFactory.decodeStream(bufferedStream, null, options)
            bufferedStream.reset()
            
            // Calculate sample size to fit within max dimensions
            val sampleSize = calculateSampleSize(options.outWidth, options.outHeight)
            
            // Decode actual bitmap
            options.inJustDecodeBounds = false
            options.inSampleSize = sampleSize
            
            BitmapFactory.decodeStream(bufferedStream, null, options)
        } catch (e: Exception) {
            null
        }
    }
```

```plaintext
    fun importCustomFormat(
        uri: Uri,
        whiteboardView: WhiteboardView,
        pageManager: PageManager,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                onError(context.getString(R.string.import_custom_failed))
                return
            }
            
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val importData = JSONObject(jsonString)
            
            // Validate version compatibility
            val version = importData.optString("version", "")
            if (version != SUPPORTED_CUSTOM_VERSION) {
                onError(context.getString(R.string.import_version_mismatch, version, SUPPORTED_CUSTOM_VERSION))
                return
            }
            
            // Clear current whiteboard state
            whiteboardView.clear()
            
            // Import shared settings
            if (importData.has("sharedSettings")) {
                val sharedSettings = importData.getJSONObject("sharedSettings")
                val color = sharedSettings.optInt("color", Color.BLACK)
                val strokeWidth = sharedSettings.optDouble("strokeWidth", 8.0).toFloat()
                val backgroundPatternId = sharedSettings.optInt("backgroundPattern", BackgroundPattern.PLAIN.id)
                
                pageManager.updateSharedColor(color)
                pageManager.updateSharedStrokeWidth(strokeWidth)
                pageManager.updateSharedBackgroundPattern(BackgroundPattern.fromId(backgroundPatternId))
                
                whiteboardView.setColor(color)
                whiteboardView.setStrokeWidth(strokeWidth)
                whiteboardView.setBackgroundPattern(BackgroundPattern.fromId(backgroundPatternId))
            }
            
            // Import all pages if available
            if (importData.has("allPages")) {
                val allPagesArray = importData.getJSONArray("allPages")
                // Clear existing pages first
                pageManager.clearAllPages()
                
                for (i in 0 until allPagesArray.length()) {
                    val pageData = allPagesArray.getJSONObject(i)
                    val page = deserializePage(pageData)
                    pageManager.addPage(page)
                }
                
                // Set current page if specified
                val currentPageId = importData.optInt("currentPageId", 1)
                val pageLoaded = pageManager.goToPage(currentPageId)
                
                // If the specified page doesn't exist, go to the first available page
                if (!pageLoaded && pageManager.getTotalPages() > 0) {
                    val firstPageId = pageManager.getPageIds().firstOrNull() ?: 1
                    pageManager.goToPage(firstPageId)
                }
            } else if (importData.has("currentPage")) {
                // Import current page data only if we don't have allPages
                val currentPageData = importData.getJSONObject("currentPage")
                importPageData(currentPageData, whiteboardView)
            }
            
            onSuccess()
            Toast.makeText(context, context.getString(R.string.import_custom_success), Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            onError(context.getString(R.string.import_error, e.message))
        }
    }
```

```plaintext
    private fun deserializeDrawingPath(pathObj: JSONObject): DrawingPath? {
        return try {
            // Deserialize tool
            val toolName = pathObj.optString("tool", "PEN")
            val tool = DrawingTool.valueOf(toolName)
            
            // Deserialize paint
            val paintObj = pathObj.optJSONObject("paint")
            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                
                if (paintObj != null) {
                    color = paintObj.optInt("color", Color.BLACK)
                    strokeWidth = paintObj.optDouble("strokeWidth", 8.0).toFloat()
                    alpha = paintObj.optInt("alpha", 255)
                }
            }
            
            // Get text data if present
            val text = if (pathObj.has("text")) pathObj.getString("text") else null
            val textX = pathObj.optDouble("textX", 0.0).toFloat()
            val textY = pathObj.optDouble("textY", 0.0).toFloat()
            
            // Get shape data if present
            val shapeStartX = pathObj.optDouble("shapeStartX", 0.0).toFloat()
            val shapeStartY = pathObj.optDouble("shapeStartY", 0.0).toFloat()
            val shapeEndX = pathObj.optDouble("shapeEndX", 0.0).toFloat()
            val shapeEndY = pathObj.optDouble("shapeEndY", 0.0).toFloat()
            
            // Create path (simplified - for now create a basic path)
            val path = Path()
            
            // If it's a shape, recreate the shape path
            if (tool in listOf(
                    DrawingTool.LINE,
                    DrawingTool.CIRCLE,
                    DrawingTool.TRIANGLE,
                    DrawingTool.RECTANGLE
                )) {
                // Recreate shape path from stored coordinates
                when (tool) {
                    DrawingTool.LINE -> {
                        path.moveTo(shapeStartX, shapeStartY)
                        path.lineTo(shapeEndX, shapeEndY)
                    }
                    DrawingTool.RECTANGLE -> {
                        val left = kotlin.math.min(shapeStartX, shapeEndX)
                        val top = kotlin.math.min(shapeStartY, shapeEndY)
                        val right = kotlin.math.max(shapeStartX, shapeEndX)
                        val bottom = kotlin.math.max(shapeStartY, shapeEndY)
                        path.addRect(left, top, right, bottom, Path.Direction.CW)
                    }
                    DrawingTool.CIRCLE -> {
                        val centerX = (shapeStartX + shapeEndX) / 2
                        val centerY = (shapeStartY + shapeEndY) / 2
                        val radius = kotlin.math.sqrt(
                            (shapeEndX - shapeStartX) * (shapeEndX - shapeStartX) + 
                            (shapeEndY - shapeStartY) * (shapeEndY - shapeStartY)
                        ).toFloat() / 2
                        path.addCircle(centerX, centerY, radius, Path.Direction.CW)
                    }
                    DrawingTool.TRIANGLE -> {
                        val centerX = (shapeStartX + shapeEndX) / 2
                        val centerY = (shapeStartY + shapeEndY) / 2
                        val width = kotlin.math.abs(shapeEndX - shapeStartX)
                        val height = kotlin.math.abs(shapeEndY - shapeStartY)
                        val size = kotlin.math.max(width, height)
                        
                        val topX = centerX
                        val topY = centerY - size / 2
                        val leftX = centerX - size / 2
                        val leftY = centerY + size / 2
                        val rightX = centerX + size / 2
                        val rightY = centerY + size / 2
                        
                        path.moveTo(topX, topY)
                        path.lineTo(leftX, leftY)
                        path.lineTo(rightX, rightY)
                        path.close()
                    }
                    else -> {
                        // Default line
                        path.moveTo(shapeStartX, shapeStartY)
                        path.lineTo(shapeEndX, shapeEndY)
                    }
                }
            } else {
                // For freehand drawing, deserialize path from pathData
                val pathData = pathObj.optString("pathData", "")
                if (pathData.isNotEmpty()) {
                    deserializePath(pathData, path)
                }
            }
            
            DrawingPath(
                path = path,
                paint = paint,
                tool = tool,
                text = text,
                textX = textX,
                textY = textY,
                shapeStartX = shapeStartX,
                shapeStartY = shapeStartY,
                shapeEndX = shapeEndX,
                shapeEndY = shapeEndY
            )
        } catch (e: Exception) {
            null
        }
    }
```

```plaintext
    private fun deserializeImageObject(imageObj: JSONObject): ImageObject? {
        return try {
            // Decode bitmap from Base64
            val bitmapBase64 = imageObj.getString("bitmapData")
            val bitmapBytes = Base64.decode(bitmapBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.size)
            
            if (bitmap == null) {
                return null
            }
            
            ImageObject(
                bitmap = bitmap,
                x = imageObj.optDouble("x", 0.0).toFloat(),
                y = imageObj.optDouble("y", 0.0).toFloat(),
                width = imageObj.optDouble("width", bitmap.width.toDouble()).toFloat(),
                height = imageObj.optDouble("height", bitmap.height.toDouble()).toFloat(),
                rotation = imageObj.optDouble("rotation", 0.0).toFloat(),
                scaleX = imageObj.optDouble("scaleX", 1.0).toFloat(),
                scaleY = imageObj.optDouble("scaleY", 1.0).toFloat()
            )
        } catch (e: Exception) {
            null
        }
    }
```

```plaintext
    private fun deserializePath(pathData: String, path: Path) {
        if (pathData.isEmpty()) return
        
        try {
            val pointPairs = pathData.split(";")
            if (pointPairs.isEmpty()) return
            
            // Move to first point
            val firstPoint = pointPairs[0].split(",")
            if (firstPoint.size == 2) {
                val x = firstPoint[0].toFloatOrNull() ?: return
                val y = firstPoint[1].toFloatOrNull() ?: return
                path.moveTo(x, y)
            }
            
            // Draw lines to subsequent points
            for (i in 1 until pointPairs.size) {
                val point = pointPairs[i].split(",")
                if (point.size == 2) {
                    val x = point[0].toFloatOrNull() ?: continue
                    val y = point[1].toFloatOrNull() ?: continue
                    path.lineTo(x, y)
                }
            }
        } catch (e: Exception) {
            // If deserialization fails, leave path empty
        }
    }
```

