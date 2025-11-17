package com.example.moodle.utils

import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.TextView

/**
 * Helper class for managing fonts in the application
 * Provides easy access to Shabnam font family variants
 */
object FontHelper {
    
    private var shabnamRegular: Typeface? = null
    private var shabnamBold: Typeface? = null
    private var shabnamLight: Typeface? = null
    private var shabnamMedium: Typeface? = null
    private var shabnamThin: Typeface? = null
    
    /**
     * Get the default Shabnam font (regular weight)
     */
    fun getShabnamFont(context: Context): Typeface? {
        if (shabnamRegular == null) {
            try {
                shabnamRegular = Typeface.createFromAsset(context.assets, "fonts/shabnam_fd.ttf")
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
        return shabnamRegular
    }
    
    /**
     * Get Shabnam font with specific weight
     */
    fun getShabnamFont(context: Context, weight: FontWeight): Typeface? {
        return when (weight) {
            FontWeight.THIN -> {
                if (shabnamThin == null) {
                    try {
                        shabnamThin = Typeface.createFromAsset(context.assets, "fonts/shabnam_thin_fd.ttf")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return getShabnamFont(context) // fallback to regular
                    }
                }
                shabnamThin
            }
            FontWeight.LIGHT -> {
                if (shabnamLight == null) {
                    try {
                        shabnamLight = Typeface.createFromAsset(context.assets, "fonts/shabnam_light_fd.ttf")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return getShabnamFont(context) // fallback to regular
                    }
                }
                shabnamLight
            }
            FontWeight.REGULAR -> getShabnamFont(context)
            FontWeight.MEDIUM -> {
                if (shabnamMedium == null) {
                    try {
                        shabnamMedium = Typeface.createFromAsset(context.assets, "fonts/shabnam_medium_fd.ttf")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return getShabnamFont(context) // fallback to regular
                    }
                }
                shabnamMedium
            }
            FontWeight.BOLD -> {
                if (shabnamBold == null) {
                    try {
                        shabnamBold = Typeface.createFromAsset(context.assets, "fonts/shabnam_bold_fd.ttf")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return getShabnamFont(context) // fallback to regular
                    }
                }
                shabnamBold
            }
        }
    }
    
    /**
     * Apply Shabnam font to all TextViews in a view hierarchy
     */
    fun applyFontToViewGroup(context: Context, viewGroup: ViewGroup, weight: FontWeight = FontWeight.REGULAR) {
        val font = getShabnamFont(context, weight) ?: return
        
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            when (child) {
                is TextView -> child.typeface = font
                is ViewGroup -> applyFontToViewGroup(context, child, weight)
            }
        }
    }
    
    /**
     * Apply Shabnam font to a single TextView
     */
    fun applyFontToTextView(context: Context, textView: TextView, weight: FontWeight = FontWeight.REGULAR) {
        val font = getShabnamFont(context, weight)
        if (font != null) {
            textView.typeface = font
        }
    }
    
    enum class FontWeight {
        THIN,
        LIGHT,
        REGULAR,
        MEDIUM,
        BOLD
    }
}
