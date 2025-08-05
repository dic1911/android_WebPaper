package moe.hx030.webpaper

import android.app.Presentation
import android.content.Context
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.preference.PreferenceManager
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import android.view.ViewGroup

class WebPaperWallpaperService : WallpaperService() {

    companion object {
        private const val TAG = "WebPaperWallpaperService"
        private const val ENGINE_TAG = "WebPaperEngine"
    }

    override fun onCreateEngine(): Engine {
        Log.v(TAG, "onCreateEngine() called - Creating new WebPaper engine")
        return WebPaperEngine()
    }

    override fun onCreate() {
        super.onCreate()
        Log.v(TAG, "onCreate() - WebPaper wallpaper service created")
    }

    override fun onDestroy() {
        Log.v(TAG, "onDestroy() - WebPaper wallpaper service destroyed")
        super.onDestroy()
    }

    inner class WebPaperEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private var webView: RendererWebView? = null
        private var presentation: Presentation? = null
        private var virtualDisplay: VirtualDisplay? = null
        private var isVisible = false
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var surfaceFormat = 0
        private var preferences: SharedPreferences? = null
        private var currentUrl: String? = null

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            Log.v(ENGINE_TAG, "onCreate() - Engine created with surfaceHolder: ${surfaceHolder != null}")

            // Set up preferences listener
            preferences = PreferenceManager.getDefaultSharedPreferences(this@WebPaperWallpaperService)
            preferences?.registerOnSharedPreferenceChangeListener(this)
            Log.v(ENGINE_TAG, "Registered SharedPreferences listener")
        }

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            Log.v(ENGINE_TAG, "onSurfaceCreated() - Surface created, holder: ${holder != null}")
            Log.v(ENGINE_TAG, "Surface details - isValid: ${holder?.surface?.isValid}, " +
                    "lockCanvas available: ${holder?.surface != null}")
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            Log.v(ENGINE_TAG, "onSurfaceChanged() - Format: $format, Size: ${width}x${height}")
            Log.v(ENGINE_TAG, "Previous surface - Format: $surfaceFormat, Size: ${surfaceWidth}x${surfaceHeight}")
            Log.v(ENGINE_TAG, "Surface holder valid: ${holder?.surface?.isValid}")

            // Store current surface properties
            surfaceWidth = width
            surfaceHeight = height
            surfaceFormat = format

            if (webView != null) {
                Log.v(ENGINE_TAG, "WebView already exists, skipping recreation")
                return
            }

            Log.v(ENGINE_TAG, "Creating virtual display and presentation...")

            // Create virtual display
            val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            virtualDisplay = displayManager.createVirtualDisplay(
                "WebPaper",
                width,
                height,
                160,
                holder?.surface,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
            )

            Log.v(ENGINE_TAG, "Virtual display created - ID: ${virtualDisplay?.display?.displayId}, " +
                    "Size: ${virtualDisplay?.display?.width}x${virtualDisplay?.display?.height}")

            // Create presentation
            presentation = Presentation(this@WebPaperWallpaperService, virtualDisplay!!.display)
            presentation!!.show()
            Log.v(ENGINE_TAG, "Presentation created and shown")

            // Create RendererWebView in presentation context
            webView = RendererWebView(presentation!!.context, width, height)
            webView!!.setAlwaysDrawnWithCacheEnabled(true)
            Log.v(ENGINE_TAG, "RendererWebView created with size: ${width}x${height}")

            // Set WebView as presentation content
            val layoutParams = ViewGroup.LayoutParams(width, height)
            presentation!!.setContentView(webView!!, layoutParams)
            Log.v(ENGINE_TAG, "WebView set as presentation content")

            // Load URL
            loadUrl()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            Log.v(ENGINE_TAG, "onSurfaceDestroyed() - Surface being destroyed")
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            val previousVisibility = isVisible
            isVisible = visible

            // Get current activity/app context for debugging
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningTasks = try {
                activityManager.getRunningTasks(1)
            } catch (e: Exception) {
                null
            }
            val topActivity = runningTasks?.firstOrNull()?.topActivity?.className

            Log.v(ENGINE_TAG, "onVisibilityChanged() - Visible: $visible (was: $previousVisibility)")
            Log.v(ENGINE_TAG, "Current context - Top activity: $topActivity")

            when {
                !visible && previousVisibility -> {
                    Log.v(ENGINE_TAG, " Wallpaper became INVISIBLE => Pausing WebView")
                    webView?.onPause()
                }
                visible && !previousVisibility -> {
                    Log.v(ENGINE_TAG, " Wallpaper became VISIBLE => Resuming WebView")
                    webView?.onResume()
                }
            }
        }

        override fun onOffsetsChanged(
            xOffset: Float, yOffset: Float,
            xOffsetStep: Float, yOffsetStep: Float,
            xPixelOffset: Int, yPixelOffset: Int
        ) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
            Log.v(ENGINE_TAG, "onOffsetsChanged() - xOffset: $xOffset, yOffset: $yOffset, " +
                    "xPixelOffset: $xPixelOffset, yPixelOffset: $yPixelOffset")
        }

        private fun loadUrl() {
            val url = preferences?.getString("wallpaper_url", "https://example.com") ?: "https://example.com"
            val delay = preferences?.getBoolean("delay_resume", false) ?: false

            // Set delay resume on the specific WebView instance
            webView?.delayResume = delay
            Log.v(ENGINE_TAG, "loadUrl() - Delay resume set to: $delay")

            // Only reload if URL has changed
            if (url != currentUrl) {
                Log.v(ENGINE_TAG, "loadUrl() - URL changed from '$currentUrl' to '$url'")
                currentUrl = url
                Log.d("030-ww", url) // Keep original log for compatibility
                webView?.loadUrl(url)
            } else {
                Log.v(ENGINE_TAG, "loadUrl() - URL unchanged: $url")
            }
        }

        // Called when SharedPreferences change
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            Log.v(ENGINE_TAG, "onSharedPreferenceChanged() - Key: $key")

            if (key == "wallpaper_url") {
                val newUrl = sharedPreferences?.getString("wallpaper_url", "https://example.com") ?: "https://example.com"
                Log.v(ENGINE_TAG, "URL preference changed to: $newUrl")

                // Force reload the new URL
                currentUrl = null // Reset to force reload
                loadUrl()
            } else if (key == "delay_resume") {
                val delay = sharedPreferences?.getBoolean("delay_resume", false) ?: false
                webView?.delayResume = delay
                Log.v(ENGINE_TAG, "Delay resume preference changed to: $delay")
            }
        }

        override fun onDestroy() {
            Log.v(ENGINE_TAG, "onDestroy() - Cleaning up engine resources")
            cleanup()
            super.onDestroy()
        }

        // Public cleanup method that can be called from companion object
        fun cleanup() {
            Log.v(ENGINE_TAG, "cleanup() - Current state - WebView: ${webView != null}, " +
                    "Presentation: ${presentation != null}, VirtualDisplay: ${virtualDisplay != null}")

            // Unregister preferences listener
            preferences?.unregisterOnSharedPreferenceChangeListener(this)
            Log.v(ENGINE_TAG, "Unregistered SharedPreferences listener")

            webView?.let {
                Log.v(ENGINE_TAG, "Destroying WebView")
                try {
                    it.destroy()
                } catch (e: Exception) {
                    Log.w(ENGINE_TAG, "Error destroying WebView: ${e.message}")
                }
            }
            webView = null

            presentation?.let {
                Log.v(ENGINE_TAG, "Dismissing presentation")
                try {
                    it.dismiss()
                } catch (e: Exception) {
                    Log.w(ENGINE_TAG, "Error dismissing presentation: ${e.message}")
                }
            }
            presentation = null

            virtualDisplay?.let {
                Log.v(ENGINE_TAG, "Releasing virtual display")
                try {
                    it.release()
                } catch (e: Exception) {
                    Log.w(ENGINE_TAG, "Error releasing virtual display: ${e.message}")
                }
            }
            virtualDisplay = null

            Log.v(ENGINE_TAG, "Engine cleanup completed")
        }
    }
}