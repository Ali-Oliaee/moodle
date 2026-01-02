package com.example.moodle.ui.whiteboard.controllers

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.max

/**
 * Controller responsible for managing zoom and pan operations on the canvas
 */
class ZoomPanController(context: Context) {
    
    // Zoom/Pan state
    var focusX = 0f
    var focusY = 0f
    var scaleFactor = 1.0f
    var maxFocusX = 0f
    var maxFocusY = 0f
    var minScaleFactor = 1.0f
    
    private var isPanning = false
    var isScaling = false
        private set
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    
    // Zoom limits
    private val minZoomLevel = 0.5f
    private val maxZoomLevel = 3.0f
    val maxScaleFactor = 5.0f
    
    // Callbacks
    var onInvalidate: (() -> Unit)? = null
    var getViewDimensions: (() -> Pair<Int, Int>)? = null
    var getBitmapDimensions: (() -> Pair<Float, Float>)? = null
    
    /**
     * Handle panning touch events
     */
    fun handlePanningTouch(event: MotionEvent, onInvalidate: () -> Unit) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                activePointerId = event.getPointerId(0)
                isPanning = true
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isPanning && !isScaling) {
                    val pointerIndex = event.findPointerIndex(activePointerId)
                    if (pointerIndex != -1) {
                        val x = event.getX(pointerIndex)
                        val y = event.getY(pointerIndex)
                        
                        val dx = x - lastTouchX
                        val dy = y - lastTouchY
                        
                        // Reverse directions: swipe up moves canvas down, swipe left moves canvas right
                        focusX -= dx
                        focusY -= dy
                        
                        // Constrain focus to bitmap boundaries
                        constrainFocusToBounds()
                        
                        lastTouchX = x
                        lastTouchY = y
                        
                        onInvalidate()
                    }
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPanning = false
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }
        }
    }
    
    /**
     * Constrains focus position to stay within bitmap boundaries
     */
    fun constrainFocusToBounds() {
        val (viewWidth, viewHeight) = getViewDimensions?.invoke() ?: return
        val (bitmapWidth, bitmapHeight) = getBitmapDimensions?.invoke() ?: return
        
        // Calculate the scaled bitmap size
        val scaledBitmapWidth = bitmapWidth * scaleFactor
        val scaledBitmapHeight = bitmapHeight * scaleFactor
        
        // If bitmap fits entirely in view, center it
        if (scaledBitmapWidth <= viewWidth) {
            focusX = -(viewWidth - scaledBitmapWidth) / 2f
        } else {
            // Calculate max focus values to keep bitmap edges within view
            val maxX = bitmapWidth - viewWidth / scaleFactor
            focusX = focusX.coerceIn(0f, maxX * scaleFactor)
        }
        
        if (scaledBitmapHeight <= viewHeight) {
            focusY = -(viewHeight - scaledBitmapHeight) / 2f
        } else {
            // Calculate max focus values to keep bitmap edges within view
            val maxY = bitmapHeight - viewHeight / scaleFactor
            focusY = focusY.coerceIn(0f, maxY * scaleFactor)
        }
    }
    
    /**
     * Set zoom level
     */
    fun setZoomLevel(zoomLevel: Float) {
        val newScaleFactor = zoomLevel.coerceIn(max(minScaleFactor, minZoomLevel), maxZoomLevel)
        
        if (newScaleFactor != scaleFactor) {
            scaleFactor = newScaleFactor
            constrainFocusToBounds()
            onInvalidate?.invoke()
        }
    }
    
    /**
     * Initialize focus and boundaries for new canvas size
     */
    fun initializeFocus(canvasWidth: Int, canvasHeight: Int) {
        val (viewWidth, viewHeight) = getViewDimensions?.invoke() ?: return
        
        // Center the initial view on the large canvas
        focusX = (canvasWidth - viewWidth) / 2f
        focusY = (canvasHeight - viewHeight) / 2f
        
        // Set maximum focus boundaries (bitmap size - view size)
        maxFocusX = (canvasWidth - viewWidth).toFloat()
        maxFocusY = (canvasHeight - viewHeight).toFloat()
        
        // Calculate minimum scale factor to prevent seeing empty space
        minScaleFactor = max(
            viewWidth.toFloat() / canvasWidth.toFloat(),
            viewHeight.toFloat() / canvasHeight.toFloat()
        )
    }
    
    /**
     * Scale gesture handling
     * Implements zoom-to-focal-point behavior following MDN guidelines
     * https://developer.mozilla.org/en-US/docs/Web/API/Pointer_events/Pinch_zoom_gestures
     */
    fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        isScaling = true
        return true
    }
    
    fun onScale(detector: ScaleGestureDetector): Boolean {
        // Store the old scale factor before applying the new one
        val oldScaleFactor = scaleFactor
        
        // Apply the scale factor increment from the gesture
        scaleFactor *= detector.scaleFactor
        // Clamp to min/max zoom levels
        scaleFactor = max(minScaleFactor, kotlin.math.min(scaleFactor, maxScaleFactor))
        
        // Zoom toward the focal point (center between two fingers)
        // Algorithm:
        // 1. Convert the focal point from screen coordinates to canvas coordinates
        // 2. After scaling, adjust the pan offset so the same canvas point appears under the focal point
        // This ensures the point between the user's fingers stays visually stationary during zoom
        
        // Step 1: Find which point on the canvas is currently under the focal point
        val focusXInCanvas = (detector.focusX + focusX) / oldScaleFactor
        val focusYInCanvas = (detector.focusY + focusY) / oldScaleFactor
        
        // Step 2: Calculate new pan offset to keep that canvas point under the focal point
        focusX = focusXInCanvas * scaleFactor - detector.focusX
        focusY = focusYInCanvas * scaleFactor - detector.focusY
        
        // Ensure we don't pan outside the canvas boundaries
        constrainFocusToBounds()
        onInvalidate?.invoke()
        return true
    }
    
    fun onScaleEnd(detector: ScaleGestureDetector) {
        isScaling = false
    }
}
