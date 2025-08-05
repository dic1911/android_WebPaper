package moe.hx030.webpaper

import android.util.Patterns
import java.net.URL

object UrlValidator {

    fun isValidUrl(url: String): Boolean {
        return try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return false
            }
            URL(url)
            Patterns.WEB_URL.matcher(url).matches()
        } catch (e: Exception) {
            false
        }
    }

    fun formatUrl(input: String): String {
        val trimmed = input.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.isNotEmpty() -> "https://$trimmed"
            else -> "https://example.com"
        }
    }
}