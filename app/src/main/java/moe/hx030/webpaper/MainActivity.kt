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

class MainActivity : AppCompatActivity() {
    private lateinit var urlEditText: EditText
    private lateinit var errorText: TextView
    private lateinit var resumeTypeSpinner: Spinner
    private lateinit var delayTimeEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var setWallpaperButton: Button
    
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
        saveButton = findViewById(R.id.saveButton)
        setWallpaperButton = findViewById(R.id.setWallpaperButton)

        // Setup spinner
        val resumeTypes = arrayOf(
            getString(R.string.resume_realtime),
            getString(R.string.resume_time_delay),
            getString(R.string.resume_gesture)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resumeTypes)
        resumeTypeSpinner.adapter = adapter

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val savedUrl = preferences.getString("wallpaper_url", null) ?: "https://example.com"
        val savedResumeType = preferences.getInt("resume_type", RESUME_TYPE_REALTIME)
        val savedDelayTime = preferences.getInt("delay_time_ms", 3000)

        urlEditText.setText(savedUrl)
        resumeTypeSpinner.setSelection(savedResumeType)
        delayTimeEditText.setText(savedDelayTime.toString())

        // Show/hide delay time input based on selection
        resumeTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                delayTimeEditText.visibility = if (position == RESUME_TYPE_TIME_DELAY) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Set initial visibility
        delayTimeEditText.visibility = if (savedResumeType == RESUME_TYPE_TIME_DELAY) View.VISIBLE else View.GONE

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
                // Keep old preference for backward compatibility
                putBoolean("delay_resume", resumeType != RESUME_TYPE_REALTIME)
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
}

