package com.beepiz.blegattcoroutines.sample.viewdsl

import android.support.constraint.ConstraintLayout

inline val ConstraintLayout.LayoutParams.parentId get() = ConstraintLayout.LayoutParams.PARENT_ID
inline val ConstraintLayout.LayoutParams.packed get() = ConstraintLayout.LayoutParams.CHAIN_PACKED
inline val ConstraintLayout.LayoutParams.spread get() = ConstraintLayout.LayoutParams.CHAIN_SPREAD
inline val ConstraintLayout.LayoutParams.spreadInside get() = ConstraintLayout.LayoutParams.CHAIN_SPREAD_INSIDE

@Suppress("NOTHING_TO_INLINE")
inline fun ConstraintLayout.LayoutParams.centerHorizontally() {
    startToStart = parentId
    endToEnd = parentId
}

@Suppress("NOTHING_TO_INLINE")
inline fun ConstraintLayout.LayoutParams.centerVertically() {
    topToTop = parentId
    bottomToBottom = parentId
}
