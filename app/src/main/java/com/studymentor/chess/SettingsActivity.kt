package com.studymentor.chess

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.SharedPreferences

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        prefs = getSharedPreferences("app_settings", MODE_PRIVATE)

        setupSettingsUI()
    }

    private fun setupSettingsUI() {
        val container = findViewById<LinearLayout>(R.id.settingsContainer)
        container.removeAllViews()

        // --- ВНЕШНИЙ ВИД (НАМУДИ ЗОҲИРӢ) ---
        addSectionHeader(container, getString(R.string.appearance))

        // Тема доски (Professional Themes)
        addThemeSelector(container)

        // Координаты
        addSwitchSetting(container, getString(R.string.coordinates), "show_coordinates", true)
        
        // Подсветка хода
        addSwitchSetting(container, getString(R.string.last_move_highlight), "highlight_last_move", true)

        // --- ИГРОВОЙ ПРОЦЕСС (ТАНЗИМОТИ БОЗӢ) ---
        addSectionHeader(container, getString(R.string.game_options))
        
        addSwitchSetting(container, getString(R.string.show_hints), "show_hints", true)
        addSwitchSetting(container, getString(R.string.vibration), "vibration", true)
        addSwitchSetting(container, getString(R.string.sound_effects), "sound_enabled", true)

        // --- КНОПКА СБРОСА ---
        addResetButton(container)
    }

    private fun addSectionHeader(container: LinearLayout, title: String) {
        val tv = TextView(this).apply {
            text = title.uppercase()
            textSize = 13f
            setPadding(48, 48, 16, 16)
            setTextColor(resources.getColor(R.color.accent, theme))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        container.addView(tv)
    }

    private fun addThemeSelector(container: LinearLayout) {
        val card = createCard()
        val layout = createVerticalLayout()
        
        val tv = TextView(this).apply {
            text = getString(R.string.board_theme)
            textSize = 16f
            setTextColor(resources.getColor(R.color.text_primary, theme))
            setPadding(0, 0, 0, 16)
        }
        
        val spinner = Spinner(this).apply {
            val themes = arrayOf(
                getString(R.string.theme_classic),
                getString(R.string.theme_wood),
                getString(R.string.theme_marble),
                getString(R.string.theme_dark)
            )
            adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, themes)
            
            val current = prefs.getString("board_style", "classic")
            val pos = when(current) {
                "wood" -> 1
                "marble" -> 2
                "dark" -> 3
                else -> 0
            }
            setSelection(pos)
            
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: android.view.View?, pos: Int, p3: Long) {
                    val style = when(pos) {
                        1 -> "wood"
                        2 -> "marble"
                        3 -> "dark"
                        else -> "classic"
                    }
                    prefs.edit().putString("board_style", style).apply()
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
        }
        
        layout.addView(tv)
        layout.addView(spinner)
        card.addView(layout)
        container.addView(card)
    }

    private fun addSwitchSetting(container: LinearLayout, title: String, key: String, defaultValue: Boolean) {
        val card = createCard()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(48, 32, 48, 32)
        }

        val tv = TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(resources.getColor(R.color.text_primary, theme))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val sw = Switch(this).apply {
            isChecked = prefs.getBoolean(key, defaultValue)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean(key, isChecked).apply()
            }
        }

        layout.addView(tv)
        layout.addView(sw)
        card.addView(layout)
        container.addView(card)
    }

    private fun addResetButton(container: LinearLayout) {
        val btn = Button(this).apply {
            text = getString(R.string.reset_settings)
            setTextColor(android.graphics.Color.parseColor("#FF5252"))
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setPadding(0, 64, 0, 64)
            setOnClickListener {
                MaterialAlertDialogBuilder(this@SettingsActivity)
                    .setTitle(getString(R.string.reset_settings))
                    .setMessage(getString(R.string.reset_confirm))
                    .setPositiveButton(getString(R.string.accept)) { _, _ ->
                        prefs.edit().clear().apply()
                        recreate()
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
        }
        container.addView(btn)
    }

    private fun createCard(): MaterialCardView {
        return MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(16, 8, 16, 8) }
            radius = 24f
            cardElevation = 2f
            setCardBackgroundColor(resources.getColor(R.color.primary_light, theme))
            strokeWidth = 1
            strokeColor = resources.getColor(R.color.primary, theme)
        }
    }

    private fun createVerticalLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }
    }
}