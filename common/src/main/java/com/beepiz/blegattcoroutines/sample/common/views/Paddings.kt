package com.beepiz.blegattcoroutines.sample.common.views

import android.view.View
import com.beepiz.blegattcoroutines.sample.common.viewdsl.NO_GETTER
import com.beepiz.blegattcoroutines.sample.common.viewdsl.dip
import com.beepiz.blegattcoroutines.sample.common.viewdsl.noGetter

inline var View.horizontalPadding: Int
    @Deprecated(NO_GETTER, level = DeprecationLevel.HIDDEN) get() = noGetter()
    set(value) = setPadding(value, paddingTop, value, paddingBottom)

inline var View.verticalPadding: Int
    @Deprecated(NO_GETTER, level = DeprecationLevel.HIDDEN) get() = noGetter()
    set(value) = setPadding(paddingLeft, value, paddingRight, value)

inline var View.padding: Int
    @Deprecated(NO_GETTER, level = DeprecationLevel.HIDDEN) get() = noGetter()
    set(value) = setPadding(value, value, value, value)

fun View.setPaddingDp(start: Int = 0,
                      top: Int = 0,
                      end: Int = 0,
                      bottom: Int = 0) {
    val left = if (isLtr) start else end
    val right = if (isLtr) end else start
    setPadding(dip(left), dip(top), dip(right), dip(bottom))
}
