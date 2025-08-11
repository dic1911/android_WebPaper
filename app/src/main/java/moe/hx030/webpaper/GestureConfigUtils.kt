package moe.hx030.webpaper

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

data class GestureConfig(
    val type: Int,
    val typeName: String,
    val delay: Int,
    val jsCode: String
)

object GestureConfigUtils {
    private const val TAG = "GestureConfigUtils"
    
    fun loadGestureConfigs(json: String): List<GestureConfig> {
        val configs = mutableListOf<GestureConfig>()
        if (json.isNotEmpty()) {
            try {
                val jsonArray = JSONArray(json)
                for (i in 0 until jsonArray.length()) {
                    val jsonObj = jsonArray.getJSONObject(i)
                    val config = GestureConfig(
                        type = jsonObj.getInt("type"),
                        typeName = jsonObj.optString("typeName", "Unknown"),
                        delay = jsonObj.getInt("delay"),
                        jsCode = jsonObj.getString("jsCode")
                    )
                    configs.add(config)
                }
                Log.v(TAG, "loadGestureConfigs() - Loaded ${configs.size} gesture configurations")
            } catch (e: Exception) {
                Log.w(TAG, "loadGestureConfigs() - Error parsing gesture configs: ${e.message}")
            }
        }
        return configs
    }
    
    fun saveGestureConfigs(configs: List<GestureConfig>): String {
        val jsonArray = JSONArray()
        configs.forEach { config ->
            val jsonObj = JSONObject().apply {
                put("type", config.type)
                put("typeName", config.typeName)
                put("delay", config.delay)
                put("jsCode", config.jsCode)
            }
            jsonArray.put(jsonObj)
        }
        Log.v(TAG, "saveGestureConfigs() - Saved ${configs.size} gesture configurations")
        return jsonArray.toString()
    }
    
    // Gesture type constants
    const val GESTURE_TYPE_LONG_CLICK = 0
    const val GESTURE_TYPE_TAP_AREA = 1
    const val GESTURE_TYPE_SWIPE_LEFT = 2
    const val GESTURE_TYPE_SWIPE_RIGHT = 3
    const val GESTURE_TYPE_LONG_CLICK_END = 4
}