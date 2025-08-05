package moe.hx030.webpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {
    private lateinit var urlEditText: EditText
    private lateinit var errorText: TextView
    private lateinit var delayResumeSwitch: SwitchCompat
    private lateinit var saveButton: Button
    private lateinit var setWallpaperButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlEditText = findViewById(R.id.urlEditText)
        errorText = findViewById(R.id.errorText)
        delayResumeSwitch = findViewById(R.id.delayResumeToggle)
        saveButton = findViewById(R.id.saveButton)
        setWallpaperButton = findViewById(R.id.setWallpaperButton)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val savedUrl = preferences.getString("wallpaper_url", null) ?: "https://example.com"
        val savedDelayResume = preferences.getBoolean("delay_resume", false)

        urlEditText.setText(savedUrl)
        delayResumeSwitch.isChecked = savedDelayResume

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
            preferences.edit {
                putString("wallpaper_url", formattedUrl)
                putBoolean("delay_resume", delayResumeSwitch.isChecked)
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

