/*
 * Copyright (C) 2024-2026 The Lunaris AOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.folder

import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.android.launcher3.Reorderable
import com.android.launcher3.util.MultiTranslateDelegate.INDEX_FOLDER_RESIZE_REFLOW

object FolderReflowSpring {

    private const val STIFFNESS = SpringForce.STIFFNESS_MEDIUM
    private const val DAMPING_RATIO = 0.55f
    private const val STAGGER_MS_PER_INDEX = 18L

    private val activeSprings = HashMap<View, Pair<SpringAnimation, SpringAnimation>>()

    fun <T> reflowTo(
        child: T,
        deltaX: Float,
        deltaY: Float,
        index: Int,
    ) where T : View, T : Reorderable {
        activeSprings.remove(child)?.let { (sx, sy) ->
            sx.cancel()
            sy.cancel()
        }

        val translateDelegate = child.getTranslateDelegate()
        val startX = translateDelegate.getTranslationX(INDEX_FOLDER_RESIZE_REFLOW).value
        val startY = translateDelegate.getTranslationY(INDEX_FOLDER_RESIZE_REFLOW).value

        val springX = SpringAnimation(FloatValueHolder(startX)).apply {
            spring = SpringForce(deltaX).apply {
                stiffness = STIFFNESS
                dampingRatio = DAMPING_RATIO
            }
            addUpdateListener { _, value, _ ->
                translateDelegate.setTranslation(INDEX_FOLDER_RESIZE_REFLOW, value,
                    translateDelegate.getTranslationY(INDEX_FOLDER_RESIZE_REFLOW).value)
            }
        }
        val springY = SpringAnimation(FloatValueHolder(startY)).apply {
            spring = SpringForce(deltaY).apply {
                stiffness = STIFFNESS
                dampingRatio = DAMPING_RATIO
            }
            addUpdateListener { _, value, _ ->
                translateDelegate.setTranslation(INDEX_FOLDER_RESIZE_REFLOW,
                    translateDelegate.getTranslationX(INDEX_FOLDER_RESIZE_REFLOW).value, value)
            }
        }

        val endListener = object : DynamicAnimation.OnAnimationEndListener {
            override fun onAnimationEnd(
                animation: DynamicAnimation<*>, canceled: Boolean, value: Float, velocity: Float,
            ) {
                if (!canceled) activeSprings.remove(child)
            }
        }
        springX.addEndListener(endListener)
        springY.addEndListener(endListener)

        activeSprings[child] = springX to springY

        val startDelay = index * STAGGER_MS_PER_INDEX
        if (startDelay == 0L) {
            springX.start()
            springY.start()
        } else {
            child.postDelayed({
                if (activeSprings[child]?.first === springX) {
                    springX.start()
                    springY.start()
                }
            }, startDelay)
        }
    }

    fun <T> cancelAndReset(child: T) where T : View, T : Reorderable {
        activeSprings.remove(child)?.let { (sx, sy) ->
            sx.cancel()
            sy.cancel()
        }
        child.getTranslateDelegate().setTranslation(INDEX_FOLDER_RESIZE_REFLOW, 0f, 0f)
    }
}

private class FloatValueHolder(startValue: Float) :
    androidx.dynamicanimation.animation.FloatValueHolder(startValue)
