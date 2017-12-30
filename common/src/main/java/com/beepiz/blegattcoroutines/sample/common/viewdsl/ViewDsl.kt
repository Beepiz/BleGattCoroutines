@file:Suppress("NOTHING_TO_INLINE")

package com.beepiz.blegattcoroutines.sample.common.viewdsl

import android.annotation.SuppressLint
import android.content.Context
import android.support.annotation.IdRes
import android.support.annotation.StyleRes
import android.support.v7.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup

typealias NewViewRef<V> = (Context) -> V

const val NO_THEME = 0

@SuppressLint("RestrictedApi")
fun Context.wrapCtxIfNeeded(theme: Int): Context {
    return if (theme == NO_THEME) this else ContextThemeWrapper(this, theme)
}

inline fun <V : View> Context.v(createView: NewViewRef<V>,
                                initView: V.() -> Unit = {}): V {
    return createView(this).apply(initView)
}

inline fun <V : View> Context.v(createView: NewViewRef<V>,
                                @IdRes id: Int = View.NO_ID,
                                theme: Int = NO_THEME,
                                initView: V.() -> Unit = {}): V {
    return createView(wrapCtxIfNeeded(theme)).also {
        it.id = id
        it.initView()
    }
}

inline fun <V : View> View.v(createView: NewViewRef<V>,
                             @IdRes id: Int = View.NO_ID,
                             theme: Int = NO_THEME,
                             initView: V.() -> Unit = {}) = context.v(createView, id, theme, initView)

inline fun <V : View> Ui.v(createView: NewViewRef<V>,
                           @IdRes id: Int = View.NO_ID,
                           theme: Int = NO_THEME,
                           initView: V.() -> Unit = {}) = ctx.v(createView, id, theme, initView)

inline fun <V : View> ViewGroup.add(createView: NewViewRef<V>,
                                    @IdRes id: Int = View.NO_ID,
                                    @StyleRes theme: Int = NO_THEME,
                                    lp: ViewGroup.LayoutParams,
                                    initView: V.() -> Unit = {}) {
    val view = createView(context.wrapCtxIfNeeded(theme)).also {
        it.id = id
        it.initView()
    }
    addView(view, lp)
}

inline fun <V : View> ViewGroup.add(createView: NewViewRef<V>,
                                    @IdRes id: Int,
                                    lp: ViewGroup.LayoutParams,
                                    initView: V.() -> Unit = {}) {
    val view = createView(context).also {
        it.id = id
        it.initView()
    }
    addView(view, lp)
}

inline fun <V : View> ViewGroup.add(createView: NewViewRef<V>,
                                    lp: ViewGroup.LayoutParams,
                                    initView: V.() -> Unit = {}) {
    val view = createView(context).apply(initView)
    addView(view, lp)
}

inline fun ViewGroup.add(view: View, lp: ViewGroup.LayoutParams) = addView(view, lp)
