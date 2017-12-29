@file:Suppress("NOTHING_TO_INLINE")

package com.beepiz.blegattcoroutines.sample.viewdsl

import android.content.Context
import android.view.View

inline fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()
inline fun Context.dp(value: Int): Float = (value * resources.displayMetrics.density)

inline fun View.dip(value: Int) = context.dip(value)
inline fun View.dp(value: Int) = context.dp(value)
