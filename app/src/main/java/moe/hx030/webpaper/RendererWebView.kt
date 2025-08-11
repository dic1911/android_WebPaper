package moe.hx030.webpaper

import android.content.Context
import android.util.Log
import android.webkit.*
import android.widget.Toast

class RendererWebView(context: Context, private val width: Int, private val height: Int) : WebView(context) {

    companion object {
        private const val TAG = "RendererWebView"
    }

    var firstLoadComplete = false
        private set

    var resumeType = 0 // 0=realtime, 1=time-delay, 2=gesture
    var delayTimeMs = 3000
    var running = true

    // Store the delayed resume runnable so we can cancel it
    private var delayedResumeRunnable: Runnable? = null
    
    // Gesture detection for resume
    private var isWaitingForGesture = false
    private var gestureResumeType = 0 // Store which gesture should trigger resume

    init {
        Log.v(TAG, "Initializing RendererWebView with size: ${width}x${height}")

        // Enable WebView debugging
        WebView.setWebContentsDebuggingEnabled(true)
        Log.v(TAG, "WebView debugging enabled")

        // Configure settings
        settings.apply {
            userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
            useWideViewPort = true
            javaScriptEnabled = true
            blockNetworkImage = false
            blockNetworkLoads = false
            allowFileAccess = true
            allowContentAccess = true
            domStorageEnabled = true
        }
        Log.v(TAG, "WebView settings configured - JS enabled: ${settings.javaScriptEnabled}, " +
                "DOM storage: ${settings.domStorageEnabled}")

        // Set WebViewClient
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                Log.v(TAG, "shouldOverrideUrlLoading() - URL: ${request?.url}")
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.v(TAG, "onPageStarted() - URL: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.v(TAG, "onPageFinished() - URL: $url, First load: ${!firstLoadComplete}")
                // Replace with basic WebViewClient after first load
                webViewClient = WebViewClient()
                firstLoadComplete = true
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.w(TAG, "onReceivedError() - URL: ${request?.url}, Error: ${error?.description}")
            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)
                Log.w(TAG, "onReceivedHttpError() - URL: ${request?.url}, Status: ${errorResponse?.statusCode}")
            }
        }

        // Set WebChromeClient for console logging and JS alerts
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Log.d("RendererWebView", "${it.message()} :: From line ${it.lineNumber()} of ${it.sourceId()}")
                }
                return super.onConsoleMessage(consoleMessage)
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                Log.d("RendererWebView-alert", message ?: "")
                return super.onJsAlert(view, url, message, result)
            }
        }

        Log.v(TAG, "RendererWebView initialization completed")
    }

    override fun loadUrl(url: String) {
        Log.v(TAG, "loadUrl() called with: $url")
        super.loadUrl(url)
    }

    override fun onPause() {
        Log.v(TAG, "onPause() - WebView paused")
        super.onPause()
        running = false

        // Cancel any pending delayed resume
        delayedResumeRunnable?.let {
            removeCallbacks(it)
            Log.v(TAG, "onPause() - Cancelled pending delayed resume")
        }
        delayedResumeRunnable = null
    }

    fun forceResume() {
        super.onResume()
        isWaitingForGesture = false
        running = true
    }

    override fun onResume() {
        when (resumeType) {
            MainActivity.RESUME_TYPE_REALTIME -> { // Realtime
                Log.v(TAG, "onResume() - WebView resumed immediately (realtime)")
                super.onResume()
                running = true
            }
            MainActivity.RESUME_TYPE_TIME_DELAY -> { // Time-based delay
                Log.v(TAG, "onResume() - WebView resuming in ${delayTimeMs}ms (time-delay)")
                delayedResumeRunnable = Runnable {
                    Log.v(TAG, "onResume() - Delayed resume executing now")
                    super.onResume()
                    running = true
                    delayedResumeRunnable = null
                }
                postDelayed(delayedResumeRunnable!!, delayTimeMs.toLong())
            }
            MainActivity.RESUME_TYPE_GESTURE -> { // Gesture-based resume
                Log.v(TAG, "onResume() - WebView waiting for gesture to resume")
                isWaitingForGesture = true
                // Don't call super.onResume() yet, wait for gesture
            }
        }
    }
    
    fun handleTapForGesture() {
        // This method is called when a gesture is detected for potential resume
        // The actual gesture checking is now done in WebPaperWallpaperService
        // This method just marks that a gesture occurred for potential resume triggering
    }
    
    fun handleGestureResume(gestureType: Int, currentGestureType: Int) {
        if (!isWaitingForGesture) return
        
        // For the enhanced gesture system, any configured gesture can trigger resume
        // The gestureType parameter indicates which gesture was performed
        Log.v(TAG, "handleGestureResume() - Gesture detected (type: $gestureType), resuming WebView")
        isWaitingForGesture = false
        running = true
        super.onResume()
    }

    override fun destroy() {
        Log.v(TAG, "destroy() - WebView being destroyed")
        super.destroy()
        running = false
    }

    override fun onVisibilityChanged(changedView: android.view.View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        val visibilityString = when (visibility) {
            VISIBLE -> "VISIBLE"
            INVISIBLE -> "INVISIBLE"
            GONE -> "GONE"
            else -> "UNKNOWN($visibility)"
        }
        Log.v(TAG, "onVisibilityChanged() - Visibility: $visibilityString")
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        val visibilityString = when (visibility) {
            VISIBLE -> "VISIBLE"
            INVISIBLE -> "INVISIBLE"
            GONE -> "GONE"
            else -> "UNKNOWN($visibility)"
        }
        Log.v(TAG, "onWindowVisibilityChanged() - Window visibility: $visibilityString")
    }

    override fun evaluateJavascript(script: String, resultCallback: ValueCallback<String>?) {
        if (!running) return
        super.evaluateJavascript(script, resultCallback)
    }
}