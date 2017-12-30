package com.beepiz.blegattcoroutines.sample.common.viewdsl

const val NO_GETTER: String = "Property does not have a getter"

/**
 * Usage example:
 * `@Deprecated(NO_GETTER, level = DeprecationLevel.HIDDEN) get() = noGetter()`
 */
fun noGetter(): Nothing = throw UnsupportedOperationException(NO_GETTER)
