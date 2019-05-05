@file:UseExperimental(ExperimentalSplittiesApi::class)

package com.beepiz.blegattcoroutines.sample

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.coroutines.DialogButton
import splitties.alertdialog.appcompat.coroutines.showAndAwait
import splitties.experimental.ExperimentalSplittiesApi
import splitties.permissions.ensurePermission
import splitties.resources.txt

suspend inline fun FragmentActivity.ensurePermission(
    permission: String,
    askDialogTitle: CharSequence,
    askDialogMessage: CharSequence,
    showRationaleBeforeFirstAsk: Boolean = true,
    returnButtonText: CharSequence = txt(R.string.quit),
    returnOrThrowBlock: () -> Nothing
): Unit = ensurePermission(
    activity = this,
    fragmentManager = supportFragmentManager,
    lifecycle = lifecycle,
    permission = permission,
    askDialogTitle = askDialogTitle,
    askDialogMessage = askDialogMessage,
    showRationaleBeforeFirstAsk = showRationaleBeforeFirstAsk,
    returnButtonText = returnButtonText,
    returnOrThrowBlock = returnOrThrowBlock
)

suspend inline fun Fragment.ensurePermission(
    permission: String,
    askDialogTitle: CharSequence,
    askDialogMessage: CharSequence,
    showRationaleBeforeFirstAsk: Boolean = true,
    returnButtonText: CharSequence = txt(R.string.quit),
    returnOrThrowBlock: () -> Nothing
): Unit = ensurePermission(
    activity = requireActivity(),
    fragmentManager = requireFragmentManager(),
    lifecycle = lifecycle,
    permission = permission,
    askDialogTitle = askDialogTitle,
    askDialogMessage = askDialogMessage,
    showRationaleBeforeFirstAsk = showRationaleBeforeFirstAsk,
    returnButtonText = returnButtonText,
    returnOrThrowBlock = returnOrThrowBlock
)

suspend inline fun ensurePermission(
    activity: Activity,
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    permission: String,
    askDialogTitle: CharSequence,
    askDialogMessage: CharSequence,
    showRationaleBeforeFirstAsk: Boolean = true,
    returnButtonText: CharSequence = activity.txt(R.string.quit),
    returnOrThrowBlock: () -> Nothing
): Unit = ensurePermission(
    activity = activity,
    fragmentManager = fragmentManager,
    lifecycle = lifecycle,
    permission = permission,
    showRationaleAndContinueOrReturn = {
        activity.alertDialog(
            title = askDialogTitle,
            message = askDialogMessage
        ).showAndAwait(
            okValue = true,
            negativeButton = DialogButton(returnButtonText, false),
            dismissValue = true
        )
    },
    showRationaleBeforeFirstAsk = showRationaleBeforeFirstAsk,
    askOpenSettingsOrReturn = {
        activity.alertDialog(
            message = activity.txt(R.string.permission_denied_permanently_go_to_settings)
        ).showAndAwait(
            okValue = true,
            negativeButton = DialogButton(returnButtonText, false),
            dismissValue = true
        )
    },
    returnOrThrowBlock = returnOrThrowBlock
)
