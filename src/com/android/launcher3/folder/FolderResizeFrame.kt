/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.android.launcher3.CellLayout
import com.android.launcher3.Launcher
import com.android.launcher3.Utilities
import com.android.launcher3.celllayout.CellLayoutLayoutParams
import com.android.launcher3.views.BaseDragLayer

interface ResizableGridHost {
    val cellLayout: CellLayout
    val layoutParamsForResize: CellLayoutLayoutParams
    val minHSpan: Int
    val minVSpan: Int
    val maxHSpan: Int
    val maxVSpan: Int

    fun onLiveSpanChanged(spanX: Int, spanY: Int)
    fun onSpanCommitted(spanX: Int, spanY: Int)
}

class FolderResizeFrame
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    View(context, attrs, defStyleAttr) {

    private lateinit var host: ResizableGridHost
    private val launcher: Launcher = Launcher.getLauncher(context)

    private var xDown = 0
    private var yDown = 0
    private var runningHInc = 0
    private var runningVInc = 0

    private var deltaXAddOn = 0
    private var deltaYAddOn = 0

    fun attachTo(gridHost: ResizableGridHost) {
        host = gridHost
        runningHInc = 0
        runningVInc = 0
        deltaXAddOn = 0
        deltaYAddOn = 0
        updatePosition()
    }

    private fun updatePosition() {
        val lp = host.layoutParamsForResize
        val cellRect = Rect()
        host.cellLayout.cellToRect(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, cellRect)

        val dragLayerRect = Rect()
        launcher.dragLayer.getDescendantRectRelativeToSelf(host.cellLayout, dragLayerRect)

        val cornerX = dragLayerRect.left + cellRect.right
        val cornerY = dragLayerRect.top + cellRect.bottom

        val frameLp = layoutParams as BaseDragLayer.LayoutParams
        frameLp.customPosition = true
        frameLp.x = cornerX - frameLp.width / 2
        frameLp.y = cornerY - frameLp.height / 2
        layoutParams = frameLp
        requestLayout()
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val x = ev.rawX.toInt()
        val y = ev.rawY.toInt()
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                xDown = x
                yDown = y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                stepResize(deltaX = x - xDown, deltaY = y - yDown, commit = false)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stepResize(deltaX = x - xDown, deltaY = y - yDown, commit = true)
                val (xThreshold, yThreshold) = cellThresholds()
                deltaXAddOn = runningHInc * xThreshold
                deltaYAddOn = runningVInc * yThreshold
                runningHInc = 0
                runningVInc = 0
                return true
            }
        }
        return false
    }

    private fun cellThresholds(): Pair<Int, Int> {
        val dp = launcher.deviceProfile
        val xThreshold =
            host.cellLayout.cellWidth + dp.workspaceIconProfile.cellLayoutBorderSpacePx.x
        val yThreshold =
            host.cellLayout.cellHeight + dp.workspaceIconProfile.cellLayoutBorderSpacePx.y
        return xThreshold to yThreshold
    }

    private fun stepResize(deltaX: Int, deltaY: Int, commit: Boolean) {
        val (xThreshold, yThreshold) = cellThresholds()
        val hSpanInc = getSpanIncrement((deltaX + deltaXAddOn).toFloat() / xThreshold - runningHInc)
        val vSpanInc = getSpanIncrement((deltaY + deltaYAddOn).toFloat() / yThreshold - runningVInc)
        if (!commit && hSpanInc == 0 && vSpanInc == 0) return

        val lp = host.layoutParamsForResize
        val newSpanX = Utilities.boundToRange(lp.cellHSpan + hSpanInc, host.minHSpan, host.maxHSpan)
        val newSpanY = Utilities.boundToRange(lp.cellVSpan + vSpanInc, host.minVSpan, host.maxVSpan)
        if (newSpanX == lp.cellHSpan && newSpanY == lp.cellVSpan && !commit) return

        val fits = host.cellLayout.createAreaForResizeFromOutsidePackage(
            lp.cellX, lp.cellY, newSpanX, newSpanY,
            /* dragView = */ null, /* direction = */ intArrayOf(0, 0), /* commit = */ commit,
        )
        if (!fits && !commit) return

        runningHInc += hSpanInc
        runningVInc += vSpanInc
        lp.cellHSpan = newSpanX
        lp.cellVSpan = newSpanY

        if (commit) {
            host.onSpanCommitted(newSpanX, newSpanY)
        } else {
            host.onLiveSpanChanged(newSpanX, newSpanY)
        }
        host.cellLayout.requestLayout()
        updatePosition()
    }

    private fun getSpanIncrement(deltaFrac: Float): Int = when {
        deltaFrac >= 1f -> Math.floor(deltaFrac.toDouble()).toInt()
        deltaFrac <= -1f -> Math.ceil(deltaFrac.toDouble()).toInt()
        else -> 0
    }
}
