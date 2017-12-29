@file:Suppress("NOTHING_TO_INLINE")

package com.beepiz.blegattcoroutines.sample.viewdsl

import android.annotation.SuppressLint
import android.support.constraint.ConstraintLayout
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout

inline val ViewGroup.matchParent get() = ViewGroup.LayoutParams.MATCH_PARENT
inline val ViewGroup.wrapContent get() = ViewGroup.LayoutParams.WRAP_CONTENT
const val MATCH_CONSTRAINT = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT

inline val ViewGroup.justMatchParent get() = ViewGroup.LayoutParams(matchParent, matchParent)

inline fun ConstraintLayout.lParams(width: Int = MATCH_CONSTRAINT, height: Int = MATCH_CONSTRAINT,
                                    initParams: ConstraintLayout.LayoutParams.() -> Unit = {}): ConstraintLayout.LayoutParams {
    val matchParentWidth = width == matchParent
    val matchParentHeight = height == matchParent
    val realWidth = if (matchParentWidth) MATCH_CONSTRAINT else width
    val realHeight = if (matchParentHeight) MATCH_CONSTRAINT else height
    return ConstraintLayout.LayoutParams(realWidth, realHeight).apply {
        if (matchParentWidth) {
            startToStart = parentId
            endToEnd = parentId
        }
        if (matchParentHeight) {
            topToTop = parentId
            bottomToBottom = parentId
        }
        initParams()
    }
}

inline fun CoordinatorLayout.lParams(width: Int = wrapContent, height: Int = wrapContent,
                                     gravity: Int = Gravity.NO_GRAVITY,
                                     initParams: CoordinatorLayout.LayoutParams.() -> Unit) =
        CoordinatorLayout.LayoutParams(width, height).also { it.gravity = gravity }.apply(initParams)

inline fun CoordinatorLayout.lParams(width: Int = wrapContent, height: Int = wrapContent,
                                     gravity: Int = Gravity.NO_GRAVITY) =
        CoordinatorLayout.LayoutParams(width, height).also { it.gravity = gravity }

inline fun CoordinatorLayout.lParams(width: Int = wrapContent, height: Int = wrapContent) =
        CoordinatorLayout.LayoutParams(width, height)

inline fun AppBarLayout.appBarLParams(width: Int = wrapContent, height: Int = wrapContent,
                                      initParams: AppBarLayout.LayoutParams.() -> Unit) =
        AppBarLayout.LayoutParams(width, height).apply(initParams)

inline fun AppBarLayout.appBarLParams(width: Int = wrapContent, height: Int = wrapContent) =
        AppBarLayout.LayoutParams(width, height)

/**
 * Default gravity is treated by FrameLayout as: `Gravity.TOP or Gravity.START`.
 */
@SuppressLint("InlinedApi")
inline fun FrameLayout.lParams(width: Int = wrapContent, height: Int = wrapContent,
                               gravity: Int = FrameLayout.LayoutParams.UNSPECIFIED_GRAVITY,
                               initParams: FrameLayout.LayoutParams.() -> Unit = {}) =
        FrameLayout.LayoutParams(width, height).also { it.gravity = gravity }.apply(initParams)

inline fun LinearLayout.lParams(width: Int = wrapContent, height: Int = wrapContent,
                                gravity: Int = -1,
                                initParams: LinearLayout.LayoutParams.() -> Unit) =
        LinearLayout.LayoutParams(width, height).also {
            it.gravity = gravity
            it.initParams()
        }

inline fun LinearLayout.lParams(width: Int = wrapContent, height: Int = wrapContent,
                                initParams: LinearLayout.LayoutParams.() -> Unit) =
        LinearLayout.LayoutParams(width, height).apply(initParams)

inline fun LinearLayout.lParams(width: Int = wrapContent, height: Int = wrapContent) =
        LinearLayout.LayoutParams(width, height)


inline fun LinearLayout.lParams(width: Int = wrapContent, height: Int = wrapContent,
                                gravity: Int = -1) =
        LinearLayout.LayoutParams(width, height).also { it.gravity = gravity }
