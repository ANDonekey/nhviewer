package com.nhviewer.ui.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nhviewer.R
import com.nhviewer.domain.repository.SettingsRepository
import com.nhviewer.ui.main.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class SplashActivity : AppCompatActivity() {
    private val settingsRepository: SettingsRepository by inject()
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logoView: ImageView = findViewById(R.id.logoView)
        lifecycleScope.launch {
            val enabled = settingsRepository.observeSettings().first().splashAnimationEnabled
            if (!enabled) {
                openMain()
                return@launch
            }
            startLaunchAnimation(logoView)
        }
    }

    private fun startLaunchAnimation(logoView: ImageView) {
        logoView.alpha = 0f
        logoView.scaleX = 0.85f
        logoView.scaleY = 0.85f
        logoView.translationY = 26f

        val fadeIn = ObjectAnimator.ofFloat(logoView, View.ALPHA, 0f, 1f).setDuration(420)
        val scaleX = ObjectAnimator.ofFloat(logoView, View.SCALE_X, 0.85f, 1.05f, 1f).setDuration(1100)
        val scaleY = ObjectAnimator.ofFloat(logoView, View.SCALE_Y, 0.85f, 1.05f, 1f).setDuration(1100)
        val moveY = ObjectAnimator.ofFloat(logoView, View.TRANSLATION_Y, 26f, 0f).setDuration(900)
        val tilt = ObjectAnimator.ofFloat(logoView, View.ROTATION, -3f, 0f).setDuration(900)

        AnimatorSet().apply {
            playTogether(fadeIn, scaleX, scaleY, moveY, tilt)
            interpolator = DecelerateInterpolator()
            doOnEndCompat { openMain() }
            start()
        }
    }

    private fun openMain() {
        if (hasNavigated) return
        hasNavigated = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun AnimatorSet.doOnEndCompat(block: () -> Unit) {
        addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) = block()
            override fun onAnimationCancel(animation: android.animation.Animator) = block()
        })
    }
}
