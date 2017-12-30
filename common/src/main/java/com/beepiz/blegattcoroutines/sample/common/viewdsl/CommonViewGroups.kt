@file:Suppress("NOTHING_TO_INLINE")

package com.beepiz.blegattcoroutines.sample.common.viewdsl

import android.content.Context
import android.widget.LinearLayout

inline fun verticalLayout(ctx: Context) = LinearLayout(ctx).apply {
    orientation = LinearLayout.VERTICAL
}

inline fun horizontalLayout(ctx: Context) = LinearLayout(ctx)
