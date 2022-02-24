package com.beepiz.blegattcoroutines.sample

import android.app.AlertDialog
import android.content.DialogInterface
import kotlinx.coroutines.*
import splitties.resources.*


class DialogButton<T>(val text: CharSequence, val value: T) {
    @Suppress("NOTHING_TO_INLINE")
    companion object {
        fun <T> ok(value: T) = DialogButton(appTxt(android.R.string.ok), value)
        fun <T> cancel(value: T) = DialogButton(appTxt(android.R.string.cancel), value)
    }
}

suspend inline fun AlertDialog.showAndAwaitOkOrDismiss() {
    showAndAwait(
        positiveButton = DialogButton.ok(Unit),
        dismissValue = Unit
    )
}

suspend inline fun <R> AlertDialog.showAndAwait(
    okValue: R,
    cancelValue: R,
    dismissValue: R
): R = showAndAwait(
    positiveButton = DialogButton.ok(okValue),
    negativeButton = DialogButton.cancel(cancelValue),
    dismissValue = dismissValue
)

@JvmName("showAndAwaitWithOptionalCancel")
suspend inline fun <R : Any> AlertDialog.showAndAwait(
    okValue: R,
    cancelValue: R? = null,
    dismissValue: R
): R = showAndAwait(
    positiveButton = DialogButton.ok(okValue),
    negativeButton = cancelValue?.let { DialogButton.cancel(it) },
    dismissValue = dismissValue
)

suspend inline fun <R> AlertDialog.showAndAwait(
    okValue: R,
    negativeButton: DialogButton<R>,
    dismissValue: R
): R = showAndAwait(
    positiveButton = DialogButton.ok(okValue),
    negativeButton = negativeButton,
    dismissValue = dismissValue
)

suspend fun <R> AlertDialog.showAndAwait(
    positiveButton: DialogButton<R>? = null,
    negativeButton: DialogButton<R>? = null,
    neutralButton: DialogButton<R>? = null,
    dismissValue: R
): R = try {
    val valueAsync = CompletableDeferred<R>()
    val clickListener = DialogInterface.OnClickListener { _, which ->
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> positiveButton
            DialogInterface.BUTTON_NEUTRAL -> neutralButton
            DialogInterface.BUTTON_NEGATIVE -> negativeButton
            else -> null
        }?.apply { valueAsync.complete(value) }
    }
    positiveButton?.let { setButton(DialogInterface.BUTTON_POSITIVE, it.text, clickListener) }
    neutralButton?.let { setButton(DialogInterface.BUTTON_NEUTRAL, it.text, clickListener) }
    negativeButton?.let { setButton(DialogInterface.BUTTON_NEGATIVE, it.text, clickListener) }
    setOnDismissListener {
        valueAsync.complete(dismissValue)
    }
    show()
    valueAsync.await()
} finally {
    dismiss() // Dismiss call is ignored if already dismissed (including by button press).
}
