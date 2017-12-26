@file:Suppress("NOTHING_TO_INLINE")

package com.beepiz.blegattcoroutines.sample.extensions

import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

inline fun Int.hasFlag(flag: Int) = flag and this == flag
inline fun Int.withFlag(flag: Int) = this or flag
inline fun Int.minusFlag(flag: Int) = this and flag.inv()

inline fun Byte.hasFlag(flag: Byte) = flag and this == flag
inline fun Byte.withFlag(flag: Byte) = this or flag
inline fun Byte.minusFlag(flag: Byte) = this and flag.inv()
