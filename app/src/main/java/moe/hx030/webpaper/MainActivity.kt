package moe.hx030.webpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var urlEditText: EditText
    private lateinit var errorText: TextView
    private lateinit var resumeTypeSpinner: Spinner
    private lateinit var delayTimeEditText: EditText
    private lateinit var gestureTypeSpinner: Spinner
    private lateinit var gestureDelayEditText: EditText
    private lateinit var customJsEditText: EditText
    private lateinit var addGestureButton: Button
    private lateinit var gestureListContainer: LinearLayout
    private lateinit var noGesturesText: TextView
    private lateinit var saveButton: Button
    private lateinit var setWallpaperButton: Button
    
    private val gestureConfigs = mutableListOf<GestureConfig>()
    
    companion object {
        const val RESUME_TYPE_REALTIME = 0
        const val RESUME_TYPE_TIME_DELAY = 1
        const val RESUME_TYPE_GESTURE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlEditText = findViewById(R.id.urlEditText)
        errorText = findViewById(R.id.errorText)
        resumeTypeSpinner = findViewById(R.id.resumeTypeSpinner)
        delayTimeEditText = findViewById(R.id.delayTimeEditText)
        gestureTypeSpinner = findViewById(R.id.gestureTypeSpinner)
        gestureDelayEditText = findViewById(R.id.gestureDelayEditText)
        customJsEditText = findViewById(R.id.customJsEditText)
        addGestureButton = findViewById(R.id.addGestureButton)
        gestureListContainer = findViewById(R.id.gestureListContainer)
        noGesturesText = findViewById(R.id.noGesturesText)
        saveButton = findViewById(R.id.saveButton)
        setWallpaperButton = findViewById(R.id.setWallpaperButton)

        // Setup resume type spinner
        val resumeTypes = arrayOf(
            getString(R.string.resume_realtime),
            getString(R.string.resume_time_delay),
            getString(R.string.resume_gesture)
        )
        val resumeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resumeTypes)
        resumeTypeSpinner.adapter = resumeAdapter

        // Setup gesture type spinner
        val gestureTypes = arrayOf(
            getString(R.string.gesture_long_click),
            getString(R.string.gesture_tap_area),
            getString(R.string.gesture_swipe_left),
            getString(R.string.gesture_swipe_right),
            getString(R.string.gesture_long_click_end)
        )
        val gestureAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, gestureTypes)
        gestureTypeSpinner.adapter = gestureAdapter

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val savedUrl = preferences.getString("wallpaper_url", null) ?: "https://example.com"
        val savedResumeType = preferences.getInt("resume_type", RESUME_TYPE_REALTIME)
        val savedDelayTime = preferences.getInt("delay_time_ms", 3000)
        val savedGesturesJson = preferences.getString("gesture_configs", "") ?: ""

        urlEditText.setText(savedUrl)
        resumeTypeSpinner.setSelection(savedResumeType)
        delayTimeEditText.setText(savedDelayTime.toString())
        
        // Load saved gesture configurations
        loadGestureConfigs(savedGesturesJson)

        // Show/hide delay time input based on resume type selection
        resumeTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                delayTimeEditText.visibility = if (position == RESUME_TYPE_TIME_DELAY) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Show/hide gesture delay input based on gesture type selection
        gestureTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                gestureDelayEditText.visibility = if (position == GestureConfigUtils.GESTURE_TYPE_LONG_CLICK) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Set initial visibility
        delayTimeEditText.visibility = if (savedResumeType == RESUME_TYPE_TIME_DELAY) View.VISIBLE else View.GONE
        gestureDelayEditText.visibility = View.GONE // Will be shown when long click is selected
        
        // Add gesture button click handler
        addGestureButton.setOnClickListener {
            addGestureConfig()
        }

        urlEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val url = s.toString()
                val isValid = url.isEmpty() || UrlUtil.isValidUrl(UrlUtil.formatUrl(url))
                errorText.visibility = if (isValid) View.GONE else View.VISIBLE
                saveButton.isEnabled = url.isNotEmpty()
            }
        })

        saveButton.setOnClickListener {
            val url = urlEditText.text.toString()
            val formattedUrl = UrlUtil.formatUrl(url)
            val resumeType = resumeTypeSpinner.selectedItemPosition
            val delayTime = delayTimeEditText.text.toString().toIntOrNull() ?: 3000
            
            preferences.edit {
                putString("wallpaper_url", formattedUrl)
                putInt("resume_type", resumeType)
                putInt("delay_time_ms", delayTime)
                putString("gesture_configs", GestureConfigUtils.saveGestureConfigs(gestureConfigs))
            }
            urlEditText.setText(formattedUrl)
        }

        setWallpaperButton.setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, WebPaperWallpaperService::class.java))
            startActivity(intent)
        }
    }
    
    private fun addGestureConfig() {
        val selectedType = gestureTypeSpinner.selectedItemPosition
        val gestureTypeName = gestureTypeSpinner.selectedItem.toString()
        val delay = if (selectedType == GestureConfigUtils.GESTURE_TYPE_LONG_CLICK) {
            gestureDelayEditText.text.toString().toIntOrNull() ?: 500
        } else 0
        val jsCode = customJsEditText.text.toString().trim()
        
        if (jsCode.isEmpty()) {
            Toast.makeText(this, "Please enter JavaScript code", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Check if this gesture type already exists
        if (gestureConfigs.any { it.type == selectedType }) {
            Toast.makeText(this, "This gesture type is already configured", Toast.LENGTH_SHORT).show()
            return
        }
        
        val gestureConfig = GestureConfig(selectedType, gestureTypeName, delay, jsCode)
        gestureConfigs.add(gestureConfig)
        
        // Clear input fields
        customJsEditText.setText("")
        gestureDelayEditText.setText("500")
        
        refreshGestureList()
    }
    
    private fun removeGestureConfig(index: Int) {
        if (index >= 0 && index < gestureConfigs.size) {
            gestureConfigs.removeAt(index)
            refreshGestureList()
        }
    }
    
    private fun refreshGestureList() {
        gestureListContainer.removeAllViews()
        
        if (gestureConfigs.isEmpty()) {
            gestureListContainer.addView(noGesturesText)
        } else {
            gestureConfigs.forEachIndexed { index, config ->
                val gestureView = createGestureItemView(config, index)
                gestureListContainer.addView(gestureView)
            }
        }
    }
    
    private fun createGestureItemView(config: GestureConfig, index: Int): View {
        val itemView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 4, 8, 4)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        val infoText = TextView(this).apply {
            text = "${config.typeName}: ${config.jsCode}"
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(0, 8, 8, 8)
        }
        
        val removeButton = Button(this).apply {
            text = getString(R.string.remove_gesture)
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { removeGestureConfig(index) }
        }
        
        itemView.addView(infoText)
        itemView.addView(removeButton)
        
        return itemView
    }
    
    private fun loadGestureConfigs(json: String) {
        gestureConfigs.clear()
        gestureConfigs.addAll(GestureConfigUtils.loadGestureConfigs(json))
        refreshGestureList()
    }
}

