package com.example.moodle.ui.whiteboard

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.widget.Toast
import com.example.moodle.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Manages importing content into the whiteboard
 * Supports importing images and custom .moodle format files
 */
class ImportManager(private val context: Context) {
    
    companion object {
        private const val SUPPORTED_CUSTOM_VERSION = "1.0"
        private const val MAX_IMAGE_SIZE = 2048 // Max width/height for imported images
    }
    
    /**
     * Import image from URI and add it to the whiteboard
     */
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
            
            // For now, we'll add the image as a text object with image info and instructions
            val centerX = whiteboardView.width / 2f
            val centerY = whiteboardView.height / 2f
            
            // Create a more informative text about the imported image
            val imageText = context.getString(R.string.imported_image_placeholder, bitmap.width, bitmap.height)
            whiteboardView.addText(imageText, centerX, centerY)
            
            // Add instruction text below
            val instructionText = context.getString(R.string.image_import_instruction)
            whiteboardView.addText(instructionText, centerX, centerY + 60f)
            
            onSuccess()
            Toast.makeText(context, context.getString(R.string.import_image_success), Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            onError(context.getString(R.string.import_error, e.message))
        }
    }
    
    /**
     * Import whiteboard data from custom .moodle format file
     */
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
            
            // Import current page data
            if (importData.has("currentPage")) {
                val currentPageData = importData.getJSONObject("currentPage")
                importPageData(currentPageData, whiteboardView)
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
                pageManager.goToPage(currentPageId)
            }
            
            onSuccess()
            Toast.makeText(context, context.getString(R.string.import_custom_success), Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            onError(context.getString(R.string.import_error, e.message))
        }
    }
    
    /**
     * Import page data into whiteboard view
     */
    private fun importPageData(pageData: JSONObject, whiteboardView: WhiteboardView) {
        // Import background pattern
        val backgroundPatternId = pageData.optInt("backgroundPattern", BackgroundPattern.PLAIN.id)
        whiteboardView.setBackgroundPattern(BackgroundPattern.fromId(backgroundPatternId))
        
        // Collect all imported data
        val paths = mutableListOf<WhiteboardView.DrawingPath>()
        val textObjects = mutableListOf<WhiteboardView.TextObject>()
        val shapeObjects = mutableListOf<WhiteboardView.ShapeObject>()
        
        // Import paths
        if (pageData.has("paths")) {
            val pathsArray = pageData.getJSONArray("paths")
            
            for (i in 0 until pathsArray.length()) {
                val pathObj = pathsArray.getJSONObject(i)
                val drawingPath = deserializeDrawingPath(pathObj)
                if (drawingPath != null) {
                    paths.add(drawingPath)
                }
            }
        }
        
        // Import text objects
        if (pageData.has("textObjects")) {
            val textArray = pageData.getJSONArray("textObjects")
            
            for (i in 0 until textArray.length()) {
                val textObj = textArray.getJSONObject(i)
                val textObject = deserializeTextObject(textObj)
                if (textObject != null) {
                    textObjects.add(textObject)
                }
            }
        }
        
        // Import shape objects
        if (pageData.has("shapeObjects")) {
            val shapesArray = pageData.getJSONArray("shapeObjects")
            
            for (i in 0 until shapesArray.length()) {
                val shapeObj = shapesArray.getJSONObject(i)
                val shapeObject = deserializeShapeObject(shapeObj)
                if (shapeObject != null) {
                    shapeObjects.add(shapeObject)
                }
            }
        }
        
        // Load all data into whiteboard view
        whiteboardView.loadPageData(paths, textObjects, shapeObjects)
        
        // Import split mode data if present
        if (pageData.optBoolean("splitModeEnabled", false) && pageData.has("sections")) {
            val sectionsArray = pageData.getJSONArray("sections")
            // Enable split mode with appropriate section count
            whiteboardView.enableSplitMode(sectionsArray.length())
            
            // Import section data
            for (i in 0 until sectionsArray.length()) {
                val sectionData = sectionsArray.getJSONObject(i)
                importSectionData(sectionData, whiteboardView)
            }
        }
    }
    
    /**
     * Import section data for split mode
     */
    private fun importSectionData(sectionData: JSONObject, whiteboardView: WhiteboardView) {
        val sectionId = sectionData.optInt("id", 1)
        
        // Set section tool and properties
        val toolName = sectionData.optString("currentTool", "PEN")
        val tool = try {
            WhiteboardView.DrawingTool.valueOf(toolName)
        } catch (e: IllegalArgumentException) {
            WhiteboardView.DrawingTool.PEN
        }
        
        val color = sectionData.optInt("currentColor", Color.BLACK)
        val strokeWidth = sectionData.optDouble("currentStrokeWidth", 8.0).toFloat()
        
        whiteboardView.updateSectionTool(sectionId, tool)
        whiteboardView.updateSectionColor(sectionId, color)
        whiteboardView.updateSectionStrokeWidth(sectionId, strokeWidth)
        
        // Import section paths and text objects
        // Note: This would require extending the WhiteboardView API to support
        // importing data directly into sections
    }
    
    /**
     * Decode bitmap from input stream with size constraints
     */
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
    
    /**
     * Calculate sample size for bitmap decoding
     */
    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        
        if (width > MAX_IMAGE_SIZE || height > MAX_IMAGE_SIZE) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            
            while ((halfWidth / sampleSize) >= MAX_IMAGE_SIZE && (halfHeight / sampleSize) >= MAX_IMAGE_SIZE) {
                sampleSize *= 2
            }
        }
        
        return sampleSize
    }
    
    /**
     * Deserialize DrawingPath from JSON
     */
    private fun deserializeDrawingPath(pathObj: JSONObject): WhiteboardView.DrawingPath? {
        return try {
            // Deserialize tool
            val toolName = pathObj.optString("tool", "PEN")
            val tool = WhiteboardView.DrawingTool.valueOf(toolName)
            
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
                    WhiteboardView.DrawingTool.LINE,
                    WhiteboardView.DrawingTool.CIRCLE,
                    WhiteboardView.DrawingTool.TRIANGLE,
                    WhiteboardView.DrawingTool.RECTANGLE
                )) {
                // Recreate shape path from stored coordinates
                when (tool) {
                    WhiteboardView.DrawingTool.LINE -> {
                        path.moveTo(shapeStartX, shapeStartY)
                        path.lineTo(shapeEndX, shapeEndY)
                    }
                    WhiteboardView.DrawingTool.RECTANGLE -> {
                        val left = kotlin.math.min(shapeStartX, shapeEndX)
                        val top = kotlin.math.min(shapeStartY, shapeEndY)
                        val right = kotlin.math.max(shapeStartX, shapeEndX)
                        val bottom = kotlin.math.max(shapeStartY, shapeEndY)
                        path.addRect(left, top, right, bottom, Path.Direction.CW)
                    }
                    WhiteboardView.DrawingTool.CIRCLE -> {
                        val centerX = (shapeStartX + shapeEndX) / 2
                        val centerY = (shapeStartY + shapeEndY) / 2
                        val radius = kotlin.math.sqrt(
                            (shapeEndX - shapeStartX) * (shapeEndX - shapeStartX) + 
                            (shapeEndY - shapeStartY) * (shapeEndY - shapeStartY)
                        ).toFloat() / 2
                        path.addCircle(centerX, centerY, radius, Path.Direction.CW)
                    }
                    WhiteboardView.DrawingTool.TRIANGLE -> {
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
                // For freehand drawing, create a simple placeholder path
                // In a full implementation, you would store and restore the actual path data
                path.moveTo(0f, 0f)
                path.lineTo(10f, 10f)
            }
            
            WhiteboardView.DrawingPath(
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
    
    /**
     * Deserialize TextObject from JSON
     */
    private fun deserializeTextObject(textObj: JSONObject): WhiteboardView.TextObject? {
        return try {
            WhiteboardView.TextObject(
                text = textObj.getString("text"),
                x = textObj.optDouble("x", 0.0).toFloat(),
                y = textObj.optDouble("y", 0.0).toFloat(),
                textSize = textObj.optDouble("textSize", 48.0).toFloat(),
                color = textObj.optInt("color", Color.BLACK),
                rotation = textObj.optDouble("rotation", 0.0).toFloat()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Deserialize ShapeObject from JSON
     */
    private fun deserializeShapeObject(shapeObj: JSONObject): WhiteboardView.ShapeObject? {
        return try {
            val toolName = shapeObj.getString("shapeType")
            val tool = WhiteboardView.DrawingTool.valueOf(toolName)
            
            WhiteboardView.ShapeObject(
                shapeType = tool,
                startX = shapeObj.optDouble("startX", 0.0).toFloat(),
                startY = shapeObj.optDouble("startY", 0.0).toFloat(),
                endX = shapeObj.optDouble("endX", 0.0).toFloat(),
                endY = shapeObj.optDouble("endY", 0.0).toFloat(),
                color = shapeObj.optInt("color", Color.BLACK),
                strokeWidth = shapeObj.optDouble("strokeWidth", 8.0).toFloat(),
                rotation = shapeObj.optDouble("rotation", 0.0).toFloat(),
                scaleX = shapeObj.optDouble("scaleX", 1.0).toFloat(),
                scaleY = shapeObj.optDouble("scaleY", 1.0).toFloat()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Deserialize Page from JSON
     */
    private fun deserializePage(pageObj: JSONObject): PageData {
        val pageId = pageObj.optInt("pageId", 1)
        val backgroundPatternId = pageObj.optInt("backgroundPattern", BackgroundPattern.PLAIN.id)
        val backgroundPattern = BackgroundPattern.fromId(backgroundPatternId)
        
        val page = PageData(pageId, backgroundPattern = backgroundPattern)
        
        // Deserialize paths
        if (pageObj.has("paths")) {
            val pathsArray = pageObj.getJSONArray("paths")
            for (i in 0 until pathsArray.length()) {
                val pathData = pathsArray.getJSONObject(i)
                val drawingPath = deserializeDrawingPath(pathData)
                if (drawingPath != null) {
                    page.paths.add(drawingPath)
                }
            }
        }
        
        // Deserialize text objects
        if (pageObj.has("textObjects")) {
            val textArray = pageObj.getJSONArray("textObjects")
            for (i in 0 until textArray.length()) {
                val textData = textArray.getJSONObject(i)
                val textObject = deserializeTextObject(textData)
                if (textObject != null) {
                    page.textObjects.add(textObject)
                }
            }
        }
        
        // Deserialize shape objects
        if (pageObj.has("shapeObjects")) {
            val shapesArray = pageObj.getJSONArray("shapeObjects")
            for (i in 0 until shapesArray.length()) {
                val shapeData = shapesArray.getJSONObject(i)
                val shapeObject = deserializeShapeObject(shapeData)
                if (shapeObject != null) {
                    page.shapeObjects.add(shapeObject)
                }
            }
        }
        
        return page
    }
}
