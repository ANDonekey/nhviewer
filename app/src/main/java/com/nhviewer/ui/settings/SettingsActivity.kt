package com.nhviewer.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nhviewer.R
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nhviewer.ui.common.NhViewModelFactory
import com.nhviewer.ui.user.UserProfileActivity
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private val viewModel: SettingsViewModel by viewModels { NhViewModelFactory() }

    private val qualityOptions = listOf("high", "medium", "low")
    private val themeOptions = listOf("system", "light", "dark")
    private val languageOptions = listOf("zh-CN", "en-US")
    private val homeLanguageOptions = listOf("all", "japanese", "chinese")
    private val homeSortOptions = listOf("popular", "recent", "random")
    private val favoritesSourceOptions = listOf("local", "online")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val qualitySpinner: Spinner = findViewById(R.id.qualitySpinner)
        val themeSpinner: Spinner = findViewById(R.id.themeSpinner)
        val languageSpinner: Spinner = findViewById(R.id.languageSpinner)
        val homeLanguageFilterSpinner: Spinner = findViewById(R.id.homeLanguageFilterSpinner)
        val homeSortSpinner: Spinner = findViewById(R.id.homeSortSpinner)
        val favoritesSourceSpinner: Spinner = findViewById(R.id.favoritesSourceSpinner)
        val concurrencySeekbar: SeekBar = findViewById(R.id.concurrencySeekbar)
        val concurrencyValue: TextView = findViewById(R.id.concurrencyValue)
        val preferJapaneseTitleSwitch: SwitchMaterial = findViewById(R.id.preferJapaneseTitleSwitch)
        val hideBlacklistedSwitch: SwitchMaterial = findViewById(R.id.hideBlacklistedSwitch)
        val apiKeyInput: EditText = findViewById(R.id.apiKeyInput)
        val saveApiKeyButton: Button = findViewById(R.id.saveApiKeyButton)
        val userProfileButton: Button = findViewById(R.id.userProfileButton)

        qualitySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, qualityOptions)
        themeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themeOptions)
        languageSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languageOptions)
        homeLanguageFilterSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                getString(R.string.home_language_all),
                getString(R.string.home_language_japanese),
                getString(R.string.home_language_chinese)
            )
        )
        homeSortSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                getString(R.string.home_sort_popular),
                getString(R.string.home_sort_recent),
                getString(R.string.home_sort_random)
            )
        )
        favoritesSourceSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                getString(R.string.setting_favorites_source_local),
                getString(R.string.setting_favorites_source_online)
            )
        )

        qualitySpinner.setOnItemSelectedListener(SimpleItemSelectedListener { position ->
            viewModel.setImageQuality(qualityOptions[position])
        })
        themeSpinner.setOnItemSelectedListener(SimpleItemSelectedListener { position ->
            viewModel.setThemeMode(themeOptions[position])
        })
        languageSpinner.setOnItemSelectedListener(SimpleItemSelectedListener { position ->
            viewModel.setLanguage(languageOptions[position])
        })
        homeLanguageFilterSpinner.setOnItemSelectedListener(SimpleItemSelectedListener { position ->
            viewModel.setHomeLanguageFilter(homeLanguageOptions[position])
        })
        homeSortSpinner.setOnItemSelectedListener(SimpleItemSelectedListener { position ->
            viewModel.setHomeSortOption(homeSortOptions[position])
        })
        favoritesSourceSpinner.setOnItemSelectedListener(SimpleItemSelectedListener { position ->
            viewModel.setFavoritesSource(favoritesSourceOptions[position])
        })

        concurrencySeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress + 1
                concurrencyValue.text = getString(R.string.setting_concurrency_value, value)
                if (fromUser) viewModel.setMaxConcurrency(value)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        preferJapaneseTitleSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setPreferJapaneseTitle(isChecked)
        }
        hideBlacklistedSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setHideBlacklisted(isChecked)
        }

        saveApiKeyButton.setOnClickListener {
            val value = apiKeyInput.text?.toString().orEmpty().trim()
            viewModel.setApiKey(value)
            Toast.makeText(this, R.string.setting_api_key_saved, Toast.LENGTH_SHORT).show()
        }

        userProfileButton.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings.collect { s ->
                    qualitySpinner.setSelection(qualityOptions.indexOf(s.imageQuality).coerceAtLeast(0))
                    themeSpinner.setSelection(themeOptions.indexOf(s.themeMode).coerceAtLeast(0))
                    languageSpinner.setSelection(languageOptions.indexOf(s.language).coerceAtLeast(0))
                    homeLanguageFilterSpinner.setSelection(
                        homeLanguageOptions.indexOf(s.homeLanguageFilter).coerceAtLeast(0)
                    )
                    homeSortSpinner.setSelection(
                        homeSortOptions.indexOf(s.homeSortOption).coerceAtLeast(0)
                    )
                    favoritesSourceSpinner.setSelection(
                        favoritesSourceOptions.indexOf(s.favoritesSource).coerceAtLeast(0)
                    )
                    concurrencySeekbar.progress = (s.maxConcurrency - 1).coerceIn(0, 7)
                    concurrencyValue.text = getString(R.string.setting_concurrency_value, s.maxConcurrency)
                    if (preferJapaneseTitleSwitch.isChecked != s.preferJapaneseTitle) {
                        preferJapaneseTitleSwitch.isChecked = s.preferJapaneseTitle
                    }
                    if (hideBlacklistedSwitch.isChecked != s.hideBlacklisted) {
                        hideBlacklistedSwitch.isChecked = s.hideBlacklisted
                    }
                    if (apiKeyInput.text?.toString().orEmpty() != s.apiKey) {
                        apiKeyInput.setText(s.apiKey)
                        apiKeyInput.setSelection(s.apiKey.length)
                    }
                }
            }
        }
    }
}
