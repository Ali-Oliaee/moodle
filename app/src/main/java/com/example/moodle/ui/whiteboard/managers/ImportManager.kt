package com.example.moodle.ui.whiteboard.managers
import com.example.moodle.ui.whiteboard.models.*
import com.example.moodle.ui.whiteboard.views.*

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import com.example.moodle.R
import org.json.JSONObject
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
    
    /**
     * Import page data into whiteboard view
     */
    private fun importPageData(pageData: JSONObject, whiteboardView: WhiteboardView) {
        // Import background pattern
        val backgroundPatternId = pageData.optInt("backgroundPattern", BackgroundPattern.PLAIN.id)
        whiteboardView.setBackgroundPattern(BackgroundPattern.fromId(backgroundPatternId))
        
        // Collect all imported data
        val paths = mutableListOf<DrawingPath>()
        val textObjects = mutableListOf<TextObject>()
        val shapeObjects = mutableListOf<ShapeObject>()
        val imageObjects = mutableListOf<ImageObject>()
        
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
        
        // Import image objects
        if (pageData.has("imageObjects")) {
            val imagesArray = pageData.getJSONArray("imageObjects")
            
            for (i in 0 until imagesArray.length()) {
                val imageObj = imagesArray.getJSONObject(i)
                val imageObject = deserializeImageObject(imageObj)
                if (imageObject != null) {
                    imageObjects.add(imageObject)
                }
            }
        }
        
        // Load all data into whiteboard view
        whiteboardView.loadPageData(paths, textObjects, shapeObjects, imageObjects)
        
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
            DrawingTool.valueOf(toolName)
        } catch (e: IllegalArgumentException) {
            DrawingTool.PEN
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
    
    /**
     * Deserialize TextObject from JSON
     */
    private fun deserializeTextObject(textObj: JSONObject): TextObject? {
        return try {
            TextObject(
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
    private fun deserializeShapeObject(shapeObj: JSONObject): ShapeObject? {
        return try {
            val toolName = shapeObj.getString("shapeType")
            val tool = DrawingTool.valueOf(toolName)
            
            ShapeObject(
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
     * Deserialize ImageObject from JSON
     * Bitmap data is decoded from Base64 PNG
     */
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
        
        // Deserialize image objects
        if (pageObj.has("imageObjects")) {
            val imagesArray = pageObj.getJSONArray("imageObjects")
            for (i in 0 until imagesArray.length()) {
                val imageData = imagesArray.getJSONObject(i)
                val imageObject = deserializeImageObject(imageData)
                if (imageObject != null) {
                    page.imageObjects.add(imageObject)
                }
            }
        }
        
        return page
    }
    
    /**
     * Deserialize path from string of points
     * Format: "x1,y1;x2,y2;x3,y3;..."
     */
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
}
