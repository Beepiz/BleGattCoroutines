@file:Suppress("NOTHING_TO_INLINE")

package com.beepiz.blegattcoroutines.sample.common.viewdsl

import android.app.Activity
import android.view.View
import splitties.concurrency.uiLazy


inline fun <A : Activity, U : Ui> A.lazy(crossinline f: (A) -> U) = uiLazy { f(this) }

inline var Activity.contentView: View
    @Deprecated(NO_GETTER, level = DeprecationLevel.HIDDEN) get() = noGetter()
    set(value) = setContentView(value)

inline fun Activity.setContentView(ui: Ui) = setContentView(ui.root)
