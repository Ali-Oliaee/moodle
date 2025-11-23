package com.example.moodle.ui.whiteboard.managers
import com.example.moodle.ui.whiteboard.models.*
import com.example.moodle.ui.whiteboard.views.*

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import com.example.moodle.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages exporting whiteboard content to various formats
 * Supports PNG export and custom .moodle format for re-importing
 */
class ExportManager(private val context: Context) {
    
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
    
    /**
     * Export all pages as separate PNG images
     * Loads each page into the view temporarily to capture it properly
     */
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
    
    /**
     * Helper method to restore a page to the whiteboard view
     */
    private fun restorePage(whiteboardView: WhiteboardView, pageManager: PageManager, pageId: Int) {
        pageManager.goToPage(pageId)
        val page = pageManager.getCurrentPage()
        whiteboardView.loadPageData(page.paths, page.textObjects, page.shapeObjects, page.imageObjects)
        whiteboardView.setBackgroundPattern(page.backgroundPattern)
        whiteboardView.invalidate()
    }
    
    /**
     * Export whiteboard data in custom format that can be re-imported
     */
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
    
    /**
     * Create bitmap from whiteboard view
     */
    private fun createBitmapFromView(whiteboardView: WhiteboardView): Bitmap {
        val bitmap = Bitmap.createBitmap(
            whiteboardView.width,
            whiteboardView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        
        // Draw white background
        canvas.drawColor(Color.WHITE)
        
        // Draw the whiteboard content
        whiteboardView.draw(canvas)
        
        return bitmap
    }
    
    /**
     * Save bitmap to PNG file
     * Uses MediaStore API for Android 10+ (scoped storage)
     */
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
    
    /**
     * Save bitmap using legacy file API (Below Android 10)
     */
    private fun saveBitmapLegacy(bitmap: Bitmap, filename: String): File? {
        val exportDir = getExportDirectory()
        if (!exportDir.exists()) exportDir.mkdirs()
        
        val file = File(exportDir, filename)
        val outputStream = FileOutputStream(file)
        
        bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, outputStream)
        outputStream.flush()
        outputStream.close()
        
        return file
    }
    
    /**
     * Save JSON string to file
     * Uses MediaStore API for Android 10+ (scoped storage)
     */
    private fun saveJsonToFile(jsonString: String, filename: String): File? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - Use MediaStore API
                saveJsonUsingMediaStore(jsonString, filename)
            } else {
                // Below Android 10 - Use legacy file API
                saveJsonLegacy(jsonString, filename)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Save JSON using MediaStore API (Android 10+)
     */
    private fun saveJsonUsingMediaStore(jsonString: String, filename: String): File? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$EXPORT_FOLDER")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: return null
        
        return try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            }
            
            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            
            // Return a File representation for compatibility
            File("${Environment.DIRECTORY_DOWNLOADS}/$EXPORT_FOLDER", filename)
        } catch (e: IOException) {
            resolver.delete(uri, null, null)
            null
        }
    }
    
    /**
     * Save JSON using legacy file API (Below Android 10)
     */
    private fun saveJsonLegacy(jsonString: String, filename: String): File? {
        val exportDir = getExportDirectory()
        if (!exportDir.exists()) exportDir.mkdirs()
        
        val file = File(exportDir, filename)
        file.writeText(jsonString)
        
        return file
    }
    
    /**
     * Get export directory - using Downloads folder like screen recordings
     */
    private fun getExportDirectory(): File {
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), EXPORT_FOLDER)
    }
    
    /**
     * Serialize DrawingPath to JSON
     */
    private fun serializeDrawingPath(drawingPath: DrawingPath): JSONObject {
        val pathObj = JSONObject()
        
        // Serialize tool type
        pathObj.put("tool", drawingPath.tool.name)
        
        // Serialize paint properties
        val paintObj = JSONObject()
        paintObj.put("color", drawingPath.paint.color)
        paintObj.put("strokeWidth", drawingPath.paint.strokeWidth.toDouble())
        paintObj.put("alpha", drawingPath.paint.alpha)
        pathObj.put("paint", paintObj)
        
        // Serialize path data (simplified - store as SVG-like commands)
        pathObj.put("pathData", serializePath(drawingPath.path))
        
        // Serialize text data if present
        if (drawingPath.text != null) {
            pathObj.put("text", drawingPath.text)
            pathObj.put("textX", drawingPath.textX.toDouble())
            pathObj.put("textY", drawingPath.textY.toDouble())
        }
        
        // Serialize shape data if present
        if (drawingPath.tool in listOf(
                DrawingTool.LINE,
                DrawingTool.CIRCLE,
                DrawingTool.TRIANGLE,
                DrawingTool.RECTANGLE
            )) {
            pathObj.put("shapeStartX", drawingPath.shapeStartX.toDouble())
            pathObj.put("shapeStartY", drawingPath.shapeStartY.toDouble())
            pathObj.put("shapeEndX", drawingPath.shapeEndX.toDouble())
            pathObj.put("shapeEndY", drawingPath.shapeEndY.toDouble())
        }
        
        return pathObj
    }
    
    /**
     * Serialize TextObject to JSON
     */
    private fun serializeTextObject(textObject: TextObject): JSONObject {
        val textObj = JSONObject()
        textObj.put("text", textObject.text)
        textObj.put("x", textObject.x.toDouble())
        textObj.put("y", textObject.y.toDouble())
        textObj.put("textSize", textObject.textSize.toDouble())
        textObj.put("color", textObject.color)
        textObj.put("rotation", textObject.rotation.toDouble())
        return textObj
    }
    
    /**
     * Serialize ShapeObject to JSON
     */
    private fun serializeShapeObject(shapeObject: ShapeObject): JSONObject {
        val shapeObj = JSONObject()
        shapeObj.put("shapeType", shapeObject.shapeType.name)
        shapeObj.put("startX", shapeObject.startX.toDouble())
        shapeObj.put("startY", shapeObject.startY.toDouble())
        shapeObj.put("endX", shapeObject.endX.toDouble())
        shapeObj.put("endY", shapeObject.endY.toDouble())
        shapeObj.put("color", shapeObject.color)
        shapeObj.put("strokeWidth", shapeObject.strokeWidth.toDouble())
        shapeObj.put("rotation", shapeObject.rotation.toDouble())
        shapeObj.put("scaleX", shapeObject.scaleX.toDouble())
        shapeObj.put("scaleY", shapeObject.scaleY.toDouble())
        return shapeObj
    }
    
    /**
     * Serialize ImageObject to JSON
     * Bitmap is encoded as Base64 PNG
     */
    private fun serializeImageObject(imageObject: ImageObject): JSONObject {
        val imageObj = JSONObject()
        
        // Encode bitmap to Base64
        val outputStream = ByteArrayOutputStream()
        imageObject.bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val bitmapBytes = outputStream.toByteArray()
        val bitmapBase64 = Base64.encodeToString(bitmapBytes, Base64.DEFAULT)
        
        imageObj.put("bitmapData", bitmapBase64)
        imageObj.put("x", imageObject.x.toDouble())
        imageObj.put("y", imageObject.y.toDouble())
        imageObj.put("width", imageObject.width.toDouble())
        imageObj.put("height", imageObject.height.toDouble())
        imageObj.put("rotation", imageObject.rotation.toDouble())
        imageObj.put("scaleX", imageObject.scaleX.toDouble())
        imageObj.put("scaleY", imageObject.scaleY.toDouble())
        
        return imageObj
    }
    
    /**
     * Serialize Section to JSON
     */
    private fun serializeSection(section: SectionManager.Section): JSONObject {
        val sectionObj = JSONObject()
        sectionObj.put("id", section.id)
        sectionObj.put("studentName", section.studentName)
        sectionObj.put("currentTool", section.currentTool.name)
        sectionObj.put("currentColor", section.currentColor)
        sectionObj.put("currentStrokeWidth", section.currentStrokeWidth.toDouble())
        
        // Serialize bounds
        val boundsObj = JSONObject()
        boundsObj.put("left", section.bounds.left.toDouble())
        boundsObj.put("top", section.bounds.top.toDouble())
        boundsObj.put("right", section.bounds.right.toDouble())
        boundsObj.put("bottom", section.bounds.bottom.toDouble())
        sectionObj.put("bounds", boundsObj)
        
        // Serialize paths
        val pathsArray = JSONArray()
        for (path in section.paths) {
            pathsArray.put(serializeDrawingPath(path))
        }
        sectionObj.put("paths", pathsArray)
        
        // Serialize text objects
        val textArray = JSONArray()
        for (textObj in section.textObjects) {
            textArray.put(serializeTextObject(textObj))
        }
        sectionObj.put("textObjects", textArray)
        
        return sectionObj
    }
    
    /**
     * Serialize Page to JSON
     */
    private fun serializePage(page: PageData): JSONObject {
        val pageObj = JSONObject()
        pageObj.put("pageId", page.pageId)
        pageObj.put("backgroundPattern", page.backgroundPattern.id)
        
        // Serialize paths
        val pathsArray = JSONArray()
        for (path in page.paths) {
            pathsArray.put(serializeDrawingPath(path))
        }
        pageObj.put("paths", pathsArray)
        
        // Serialize text objects
        val textArray = JSONArray()
        for (textObj in page.textObjects) 
            textArray.put(serializeTextObject(textObj))
        pageObj.put("textObjects", textArray)
        
        // Serialize shape objects
        val shapesArray = JSONArray()
        for (shapeObj in page.shapeObjects) 
            shapesArray.put(serializeShapeObject(shapeObj))
        pageObj.put("shapeObjects", shapesArray)
        
        // Serialize image objects
        val imagesArray = JSONArray()
        for (imageObj in page.imageObjects) 
            imagesArray.put(serializeImageObject(imageObj))
        pageObj.put("imageObjects", imagesArray)
        
        return pageObj
    }
    
    /**
     * Serialize Path to string using PathMeasure to extract points
     */
    private fun serializePath(path: Path): String {
        val pathMeasure = android.graphics.PathMeasure(path, false)
        val length = pathMeasure.length
        
        if (length == 0f) return ""
        
        val points = mutableListOf<String>()
        val numPoints = (length / 5f).toInt().coerceAtLeast(2).coerceAtMost(500) // Sample points every 5 pixels, max 500 points
        val step = length / numPoints
        
        for (i in 0..numPoints) {
            val distance = (i * step).coerceAtMost(length)
            val coords = FloatArray(2)
            pathMeasure.getPosTan(distance, coords, null)
            points.add("${coords[0]},${coords[1]}")
        }
        
        return points.joinToString(";")
    }
}
