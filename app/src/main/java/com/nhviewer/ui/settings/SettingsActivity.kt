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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nhviewer.R
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nhviewer.ui.user.UserProfileActivity
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsActivity : AppCompatActivity() {
    private val viewModel: SettingsViewModel by viewModel()
    private var bindingFromState: Boolean = false

    private val qualityOptions = listOf("high", "medium", "low")
    private val themeOptions = listOf("system", "light", "dark")
    private val languageOptions = listOf("zh-CN", "en-US")
    private val homeLanguageOptions = listOf("all", "japanese", "chinese")
    private val homeSortOptions = listOf("popular", "recent", "random")
    private val favoritesSourceOptions = listOf("local", "online")
    private val readerPagingModeOptions = listOf("single", "continuous")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val qualitySpinner: Spinner = findViewById(R.id.qualitySpinner)
        val themeSpinner: Spinner = findViewById(R.id.themeSpinner)
        val languageSpinner: Spinner = findViewById(R.id.languageSpinner)
        val homeLanguageFilterSpinner: Spinner = findViewById(R.id.homeLanguageFilterSpinner)
        val homeSortSpinner: Spinner = findViewById(R.id.homeSortSpinner)
        val favoritesSourceSpinner: Spinner = findViewById(R.id.favoritesSourceSpinner)
        val splashAnimationSwitch: SwitchMaterial = findViewById(R.id.splashAnimationSwitch)
        val concurrencySeekbar: SeekBar = findViewById(R.id.concurrencySeekbar)
        val concurrencyValue: TextView = findViewById(R.id.concurrencyValue)
        val preferJapaneseTitleSwitch: SwitchMaterial = findViewById(R.id.preferJapaneseTitleSwitch)
        val showChineseTagsSwitch: SwitchMaterial = findViewById(R.id.showChineseTagsSwitch)
        val hideBlacklistedSwitch: SwitchMaterial = findViewById(R.id.hideBlacklistedSwitch)
        val readerTapPagingSwitch: SwitchMaterial = findViewById(R.id.readerTapPagingSwitch)
        val readerSwipePagingSwitch: SwitchMaterial = findViewById(R.id.readerSwipePagingSwitch)
        val readerTapToggleChromeSwitch: SwitchMaterial = findViewById(R.id.readerTapToggleChromeSwitch)
        val readerReverseTapSwitch: SwitchMaterial = findViewById(R.id.readerReverseTapSwitch)
        val readerLeftHandedSwitch: SwitchMaterial = findViewById(R.id.readerLeftHandedSwitch)
        val readerGestureSwitch: SwitchMaterial = findViewById(R.id.readerGestureSwitch)
        val readerPagingModeSpinner: Spinner = findViewById(R.id.readerPagingModeSpinner)
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
        readerPagingModeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                getString(R.string.setting_reader_mode_single),
                getString(R.string.setting_reader_mode_continuous)
            )
        )

        qualitySpinner.setOnItemSelectedListener(SimpleItemSelectedListener { position ->
            if (bindingFromState) return@SimpleItemSelectedListener
            viewModel.setImageQuality(qualityOptions[position])
        })
        themeSpinner.setOnItemSelectedListener(SimpleItemSelectedListener { position ->
            if (bindingFromState) return@SimpleItemSelectedListener
            viewModel.setThemeMode(themeOptions[position])
        })
        languageSpinner.setOnItemSelectedListener(SimpleItemSelectedListener { position ->
            if (bindingFromState) return@SimpleItemSelectedListener
            viewModel.setLanguage(languageOptions[position])
        })
        homeLanguageFilterSpinner.setOnItemSelectedListener(SimpleItemSelectedListener { position ->
            if (bindingFromState) return@SimpleItemSelectedListener
            viewModel.setHomeLanguageFilter(homeLanguageOptions[position])
        })
        homeSortSpinner.setOnItemSelectedListener(SimpleItemSelectedListener { position ->
            if (bindingFromState) return@SimpleItemSelectedListener
            viewModel.setHomeSortOption(homeSortOptions[position])
        })
        favoritesSourceSpinner.setOnItemSelectedListener(SimpleItemSelectedListener { position ->
            if (bindingFromState) return@SimpleItemSelectedListener
            viewModel.setFavoritesSource(favoritesSourceOptions[position])
        })
        readerPagingModeSpinner.setOnItemSelectedListener(SimpleItemSelectedListener { position ->
            if (bindingFromState) return@SimpleItemSelectedListener
            viewModel.setReaderPagingMode(readerPagingModeOptions[position])
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
            if (bindingFromState) return@setOnCheckedChangeListener
            viewModel.setPreferJapaneseTitle(isChecked)
        }
        showChineseTagsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (bindingFromState) return@setOnCheckedChangeListener
            viewModel.setShowChineseTags(isChecked)
        }
        hideBlacklistedSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (bindingFromState) return@setOnCheckedChangeListener
            viewModel.setHideBlacklisted(isChecked)
        }
        splashAnimationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (bindingFromState) return@setOnCheckedChangeListener
            viewModel.setSplashAnimationEnabled(isChecked)
        }
        readerTapPagingSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (bindingFromState) return@setOnCheckedChangeListener
            viewModel.setReaderTapPagingEnabled(isChecked)
        }
        readerSwipePagingSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (bindingFromState) return@setOnCheckedChangeListener
            viewModel.setReaderSwipePagingEnabled(isChecked)
        }
        readerTapToggleChromeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (bindingFromState) return@setOnCheckedChangeListener
            viewModel.setReaderTapToToggleChromeEnabled(isChecked)
        }
        readerReverseTapSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (bindingFromState) return@setOnCheckedChangeListener
            viewModel.setReaderReverseTapZones(isChecked)
        }
        readerLeftHandedSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (bindingFromState) return@setOnCheckedChangeListener
            viewModel.setReaderLeftHandedMode(isChecked)
        }
        readerGestureSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (bindingFromState) return@setOnCheckedChangeListener
            viewModel.setReaderGestureEnabled(isChecked)
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
                    bindingFromState = true
                    setSpinnerSelectionIfChanged(
                        qualitySpinner,
                        qualityOptions.indexOf(s.imageQuality).coerceAtLeast(0)
                    )
                    setSpinnerSelectionIfChanged(
                        themeSpinner,
                        themeOptions.indexOf(s.themeMode).coerceAtLeast(0)
                    )
                    setSpinnerSelectionIfChanged(
                        languageSpinner,
                        languageOptions.indexOf(s.language).coerceAtLeast(0)
                    )
                    setSpinnerSelectionIfChanged(
                        homeLanguageFilterSpinner,
                        homeLanguageOptions.indexOf(s.homeLanguageFilter).coerceAtLeast(0)
                    )
                    setSpinnerSelectionIfChanged(
                        homeSortSpinner,
                        homeSortOptions.indexOf(s.homeSortOption).coerceAtLeast(0)
                    )
                    setSpinnerSelectionIfChanged(
                        favoritesSourceSpinner,
                        favoritesSourceOptions.indexOf(s.favoritesSource).coerceAtLeast(0)
                    )
                    concurrencySeekbar.progress = (s.maxConcurrency - 1).coerceIn(0, 7)
                    concurrencyValue.text = getString(R.string.setting_concurrency_value, s.maxConcurrency)
                    if (preferJapaneseTitleSwitch.isChecked != s.preferJapaneseTitle) {
                        preferJapaneseTitleSwitch.isChecked = s.preferJapaneseTitle
                    }
                    if (showChineseTagsSwitch.isChecked != s.showChineseTags) {
                        showChineseTagsSwitch.isChecked = s.showChineseTags
                    }
                    if (hideBlacklistedSwitch.isChecked != s.hideBlacklisted) {
                        hideBlacklistedSwitch.isChecked = s.hideBlacklisted
                    }
                    if (splashAnimationSwitch.isChecked != s.splashAnimationEnabled) {
                        splashAnimationSwitch.isChecked = s.splashAnimationEnabled
                    }
                    if (readerTapPagingSwitch.isChecked != s.readerTapPagingEnabled) {
                        readerTapPagingSwitch.isChecked = s.readerTapPagingEnabled
                    }
                    if (readerSwipePagingSwitch.isChecked != s.readerSwipePagingEnabled) {
                        readerSwipePagingSwitch.isChecked = s.readerSwipePagingEnabled
                    }
                    if (readerTapToggleChromeSwitch.isChecked != s.readerTapToToggleChromeEnabled) {
                        readerTapToggleChromeSwitch.isChecked = s.readerTapToToggleChromeEnabled
                    }
                    if (readerReverseTapSwitch.isChecked != s.readerReverseTapZones) {
                        readerReverseTapSwitch.isChecked = s.readerReverseTapZones
                    }
                    if (readerLeftHandedSwitch.isChecked != s.readerLeftHandedMode) {
                        readerLeftHandedSwitch.isChecked = s.readerLeftHandedMode
                    }
                    if (readerGestureSwitch.isChecked != s.readerGestureEnabled) {
                        readerGestureSwitch.isChecked = s.readerGestureEnabled
                    }
                    setSpinnerSelectionIfChanged(
                        readerPagingModeSpinner,
                        readerPagingModeOptions.indexOf(s.readerPagingMode).coerceAtLeast(0)
                    )
                    if (apiKeyInput.text?.toString().orEmpty() != s.apiKey) {
                        apiKeyInput.setText(s.apiKey)
                        apiKeyInput.setSelection(s.apiKey.length)
                    }
                    bindingFromState = false
                }
            }
        }
    }

    private fun setSpinnerSelectionIfChanged(spinner: Spinner, selection: Int) {
        if (spinner.selectedItemPosition != selection) {
            spinner.setSelection(selection, false)
        }
    }
}
