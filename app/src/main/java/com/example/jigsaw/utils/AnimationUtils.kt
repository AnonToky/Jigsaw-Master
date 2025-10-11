package com.example.jigsaw.utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.OvershootInterpolator

object AnimationUtils {

    fun animateSwap(view1: View, view2: View, onComplete: () -> Unit) {
        val x1 = view1.x
        val y1 = view1.y
        val x2 = view2.x
        val y2 = view2.y

        val animatorSet = AnimatorSet()

        val anim1X = ObjectAnimator.ofFloat(view1, "x", x1, x2)
        val anim1Y = ObjectAnimator.ofFloat(view1, "y", y1, y2)
        val anim2X = ObjectAnimator.ofFloat(view2, "x", x2, x1)
        val anim2Y = ObjectAnimator.ofFloat(view2, "y", y2, y1)

        animatorSet.playTogether(anim1X, anim1Y, anim2X, anim2Y)
        animatorSet.duration = 300
        animatorSet.interpolator = OvershootInterpolator()

        animatorSet.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                onComplete()
            }
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })

        animatorSet.start()
    }

    fun animateCompletion(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f)
        val rotation = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, rotation)
        animatorSet.duration = 1000
        animatorSet.start()
    }
}