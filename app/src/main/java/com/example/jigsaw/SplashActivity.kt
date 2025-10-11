package com.example.jigsaw

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class SplashActivity : AppCompatActivity() {

    private lateinit var logoImageView: ImageView
    private lateinit var appNameTextView: TextView
    private lateinit var sloganTextView: TextView
    private lateinit var blackOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 全屏
        setupFullScreen()

        setContentView(R.layout.activity_splash)

        initViews()
        startAnimation()
    }

    private fun setupFullScreen() {
        // 隐藏状态栏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.let {
            it.hide(WindowInsetsCompat.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun initViews() {
        logoImageView = findViewById(R.id.logoImageView)
        appNameTextView = findViewById(R.id.appNameTextView)
        sloganTextView = findViewById(R.id.sloganTextView)
        blackOverlay = findViewById(R.id.blackOverlay)

        // 初始状态：全部不可见
        logoImageView.alpha = 0f
        appNameTextView.alpha = 0f
        sloganTextView.alpha = 0f
        blackOverlay.alpha = 1f // 黑色遮罩初始为不透明
    }

    private fun startAnimation() {
        // 动画序列
        val animatorSet = AnimatorSet()

        // 黑屏渐隐
        val fadeOutBlack1 = ObjectAnimator.ofFloat(blackOverlay, "alpha", 1f, 0f).apply {
            duration = 800
            startDelay = 300
        }

        // Logo淡入并放大
        val logoFadeIn = ObjectAnimator.ofFloat(logoImageView, "alpha", 0f, 1f).apply {
            duration = 1000
        }
        val logoScaleX = ObjectAnimator.ofFloat(logoImageView, "scaleX", 0.8f, 1f).apply {
            duration = 1000
        }
        val logoScaleY = ObjectAnimator.ofFloat(logoImageView, "scaleY", 0.8f, 1f).apply {
            duration = 1000
        }

        //  应用名称淡入 上移
        val appNameFadeIn = ObjectAnimator.ofFloat(appNameTextView, "alpha", 0f, 1f).apply {
            duration = 800
            startDelay = 200
        }
        val appNameTranslate = ObjectAnimator.ofFloat(appNameTextView, "translationY", 50f, 0f).apply {
            duration = 800
            startDelay = 200
        }

        // 标语淡入
        val sloganFadeIn = ObjectAnimator.ofFloat(sloganTextView, "alpha", 0f, 1f).apply {
            duration = 600
            startDelay = 400
        }

        // 全部淡出
        val allFadeOut = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logoImageView, "alpha", 1f, 0f),
                ObjectAnimator.ofFloat(appNameTextView, "alpha", 1f, 0f),
                ObjectAnimator.ofFloat(sloganTextView, "alpha", 1f, 0f)
            )
            duration = 600
            startDelay = 2000 // 显示2秒后开始淡出
        }

        // 黑屏渐显
        val fadeInBlack = ObjectAnimator.ofFloat(blackOverlay, "alpha", 0f, 1f).apply {
            duration = 600
            startDelay = 2000
        }

        animatorSet.interpolator = AccelerateDecelerateInterpolator()

        // 组合动画
        animatorSet.play(fadeOutBlack1).before(logoFadeIn)
        animatorSet.play(logoFadeIn).with(logoScaleX).with(logoScaleY)
        animatorSet.play(appNameFadeIn).with(appNameTranslate).after(logoFadeIn)
        animatorSet.play(sloganFadeIn).after(appNameFadeIn)
        animatorSet.play(allFadeOut).after(sloganFadeIn)
        animatorSet.play(fadeInBlack).after(sloganFadeIn)

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                Handler(Looper.getMainLooper()).postDelayed({
                    navigateToMain()
                }, 300)
            }
        })

        // 开始动画
        animatorSet.start()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        // 转场
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}