package com.example.moodle.ui.whiteboard.managers

import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import android.widget.Toast
import com.example.moodle.ui.whiteboard.models.DrawingPath
import com.example.moodle.ui.whiteboard.models.DrawingTool
import com.example.moodle.ui.whiteboard.models.TextObject
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink

/**
 * Manager responsible for Persian/Farsi handwriting recognition from drawn strokes
 * Uses ML Kit Digital Ink Recognition API which supports Persian
 */
class TextRecognitionManager(private val context: Context) {
    
    private var recognizer: DigitalInkRecognizer? = null
    private var isModelDownloaded = false
    
    /**
     * Callback interface for text recognition results
     */
    interface TextRecognitionCallback {
        fun onRecognitionSuccess(textObjects: List<TextObject>, recognizedCount: Int)
        fun onRecognitionFailure(error: String)
        fun onRecognitionProgress(message: String)
    }
    
    /**
     * Initialize the recognizer with Persian language model
     */
    private fun initializeRecognizer(callback: TextRecognitionCallback) {
        if (recognizer != null && isModelDownloaded) return
        
        callback.onRecognitionProgress("در حال آماده‌سازی شناساگر فارسی...")
        
        val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("fa")
        
        if (modelIdentifier == null) {
            callback.onRecognitionFailure("مدل شناسایی فارسی در دسترس نیست")
            return
        }
        
        val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
        
        // Check if model is already downloaded
        val modelManager = com.google.mlkit.common.model.RemoteModelManager.getInstance()
        
        modelManager.isModelDownloaded(model)
            .addOnSuccessListener { isDownloaded ->
                if (isDownloaded) {
                    Log.d(TAG, "Persian model is already downloaded")
                    createRecognizer(modelIdentifier)
                    isModelDownloaded = true
                } else {
                    Log.d(TAG, "Downloading Persian model...")
                    callback.onRecognitionProgress("در حال دانلود مدل فارسی...")
                    downloadModel(model, modelIdentifier, callback)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to check model download status", e)
                callback.onRecognitionFailure("خطا در بررسی مدل: ${e.message}")
            }
    }
    
    /**
     * Download the Persian recognition model
     */
    private fun downloadModel(
        model: DigitalInkRecognitionModel,
        modelIdentifier: DigitalInkRecognitionModelIdentifier,
        callback: TextRecognitionCallback
    ) {
        val modelManager = com.google.mlkit.common.model.RemoteModelManager.getInstance()
        
        modelManager.download(model, com.google.mlkit.common.model.DownloadConditions.Builder().build())
            .addOnSuccessListener {
                Log.d(TAG, "Persian model downloaded successfully")
                Toast.makeText(context, "مدل فارسی دانلود شد", Toast.LENGTH_SHORT).show()
                createRecognizer(modelIdentifier)
                isModelDownloaded = true
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to download Persian model", e)
                callback.onRecognitionFailure("دانلود مدل فارسی ناموفق بود: ${e.message}")
            }
    }
    
    /**
     * Create the recognizer with Persian model
     */
    private fun createRecognizer(modelIdentifier: DigitalInkRecognitionModelIdentifier) {
        val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
        val options = DigitalInkRecognizerOptions.builder(model).build()
        recognizer = DigitalInkRecognition.getClient(options)
        Log.d(TAG, "Persian recognizer created")
    }
    
    /**
     * Recognize Persian handwriting from drawn paths
     * 
     * @param paths List of drawing paths to analyze
     * @param callback Callback to receive recognition results
     */
    fun recognizeTextFromPaths(
        paths: List<DrawingPath>,
        callback: TextRecognitionCallback
    ) {
        if (paths.isEmpty()) {
            callback.onRecognitionFailure("چیزی برای تحلیل وجود ندارد")
            return
        }
        
        // Filter only pen strokes (not shapes or erasers)
        val drawingPaths = paths.filter { 
            it.tool == DrawingTool.PEN || it.tool == DrawingTool.SELECTION
        }
        
        if (drawingPaths.isEmpty()) {
            callback.onRecognitionFailure("چیزی برای تحلیل وجود ندارد")
            return
        }
        
        // Initialize recognizer if needed
        if (recognizer == null || !isModelDownloaded) {
            initializeRecognizer(object : TextRecognitionCallback {
                override fun onRecognitionSuccess(textObjects: List<TextObject>, recognizedCount: Int) {
                    // Not used in initialization
                }
                
                override fun onRecognitionFailure(error: String) {
                    callback.onRecognitionFailure(error)
                }
                
                override fun onRecognitionProgress(message: String) {
                    callback.onRecognitionProgress(message)
                }
            })
            
            // Wait for initialization and try again after a delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (recognizer != null && isModelDownloaded) {
                    performRecognition(drawingPaths, callback)
                } else {
                    callback.onRecognitionFailure("لطفاً چند ثانیه صبر کنید و دوباره امتحان کنید")
                }
            }, 2000)
            return
        }
        
        performRecognition(drawingPaths, callback)
    }
    
    /**
     * Perform the actual recognition
     */
    private fun performRecognition(
        drawingPaths: List<DrawingPath>,
        callback: TextRecognitionCallback
    ) {
        callback.onRecognitionProgress("در حال تحلیل...")
        
        try {
            // Convert DrawingPath objects to Ink
            val ink = convertPathsToInk(drawingPaths)
            
            if (ink.strokes.isEmpty()) {
                callback.onRecognitionFailure("خطوط قابل تحلیل یافت نشد")
                return
            }
            
            Log.d(TAG, "Created ink with ${ink.strokes.size} strokes")
            
            // Perform recognition
            recognizer?.recognize(ink)
                ?.addOnSuccessListener { result ->
                    val candidates = result.candidates
                    Log.d(TAG, "Recognition returned ${candidates.size} candidates")
                    
                    if (candidates.isEmpty()) {
                        callback.onRecognitionFailure("متنی شناسایی نشد. لطفاً با خط واضح‌ تر بنویسید")
                        return@addOnSuccessListener
                    }
                    
                    // Get the best candidate
                    val recognizedText = candidates[0].text
                    Log.d(TAG, "Best candidate: '$recognizedText'")
                    
                    // Calculate position for the text (center of drawn strokes)
                    val bounds = calculatePathsBounds(drawingPaths)
                    val textX = bounds.left
                    val textY = bounds.centerY()
                    
                    // Create text object
                    val textObject = TextObject(
                        text = recognizedText,
                        x = textX,
                        y = textY,
                        textSize = (bounds.height() * 0.8f).coerceIn(32f, 120f),
                        color = drawingPaths.firstOrNull()?.paint?.color ?: android.graphics.Color.BLACK
                    )
                    
                    callback.onRecognitionSuccess(listOf(textObject), 1)
                }
                ?.addOnFailureListener { e ->
                    Log.e(TAG, "Recognition failed", e)
                    callback.onRecognitionFailure("شناسایی ناموفق بود: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error during recognition", e)
            callback.onRecognitionFailure("خطا در شناسایی: ${e.message}")
        }
    }
    
    /**
     * Convert DrawingPath objects to ML Kit Ink format
     */
    private fun convertPathsToInk(paths: List<DrawingPath>): Ink {
        val inkBuilder = Ink.builder()
        
        for (drawingPath in paths) {
            val strokeBuilder = Ink.Stroke.builder()
            
            // Extract points from the Path
            val points = extractPointsFromPath(drawingPath)
            
            if (points.isEmpty()) continue
            
            // Add points to stroke with timestamps
            var timestamp = 0L
            for (point in points) {
                strokeBuilder.addPoint(Ink.Point.create(point.x, point.y, timestamp))
                timestamp += 10 // 10ms between points
            }
            
            try {
                inkBuilder.addStroke(strokeBuilder.build())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to add stroke: ${e.message}")
            }
        }
        
        return inkBuilder.build()
    }
    
    /**
     * Extract points from a Path object
     * This is a simplified version that samples the path
     */
    private fun extractPointsFromPath(drawingPath: DrawingPath): List<PointF> {
        val points = mutableListOf<PointF>()
        val path = drawingPath.path
        
        val pathMeasure = android.graphics.PathMeasure(path, false)
        val length = pathMeasure.length
        
        if (length == 0f) return points
        
        // Sample points along the path
        val sampleInterval = 5f // Sample every 5 pixels
        var distance = 0f
        val coords = FloatArray(2)
        
        while (distance <= length) {
            pathMeasure.getPosTan(distance, coords, null)
            points.add(PointF(coords[0], coords[1]))
            distance += sampleInterval
        }
        
        // Always include the last point
        if (distance - sampleInterval < length) {
            pathMeasure.getPosTan(length, coords, null)
            points.add(PointF(coords[0], coords[1]))
        }
        
        return points
    }
    
    /**
     * Calculate the bounding box of all paths
     */
    private fun calculatePathsBounds(paths: List<DrawingPath>): RectF {
        val bounds = RectF()
        var isFirst = true
        
        for (drawingPath in paths) {
            val pathBounds = RectF()
            drawingPath.path.computeBounds(pathBounds, true)
            
            if (isFirst) {
                bounds.set(pathBounds)
                isFirst = false
            } else bounds.union(pathBounds)
        }
        
        if (bounds.isEmpty) bounds.set(0f, 0f, 100f, 100f)
        
        return bounds
    }
    
    /**
     * Release resources
     */
    fun cleanup() {
        recognizer?.close()
        recognizer = null
        isModelDownloaded = false
    }
    
    companion object {
        private const val TAG = "TextRecognitionManager"
    }
}
