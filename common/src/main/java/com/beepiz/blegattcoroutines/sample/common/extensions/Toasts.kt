package com.beepiz.blegattcoroutines.sample.common.extensions

import android.support.annotation.StringRes
import android.widget.Toast
import splitties.init.appCtx

fun toast(@StringRes msgResId: Int) = Toast.makeText(appCtx, msgResId, Toast.LENGTH_SHORT).show()
fun toast(msg: String) = Toast.makeText(appCtx, msg, Toast.LENGTH_SHORT).show()
fun longToast(@StringRes msgResId: Int) = Toast.makeText(appCtx, msgResId, Toast.LENGTH_LONG).show()
fun longToast(msg: String) = Toast.makeText(appCtx, msg, Toast.LENGTH_LONG).show()
