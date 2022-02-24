@file:OptIn(ExperimentalSplittiesApi::class)

package com.beepiz.blegattcoroutines.sample

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import splitties.alertdialog.alertDialog
import splitties.experimental.ExperimentalSplittiesApi
import splitties.permissions.ensureAllPermissions
import splitties.resources.txt
import com.beepiz.blegattcoroutines.sample.common.R as CommonR

suspend inline fun FragmentActivity.ensureAllPermissions(
    permissionNames: List<String>,
    askDialogTitle: CharSequence?,
    askDialogMessage: CharSequence?,
    showRationaleBeforeFirstAsk: Boolean = true,
    returnButtonText: CharSequence = txt(CommonR.string.quit),
    returnOrThrowBlock: () -> Nothing
): Unit = ensureAllPermissions(
    activity = this,
    fragmentManager = supportFragmentManager,
    lifecycle = lifecycle,
    permissionNames = permissionNames,
    askDialogTitle = askDialogTitle,
    askDialogMessage = askDialogMessage,
    showRationaleBeforeFirstAsk = showRationaleBeforeFirstAsk,
    returnButtonText = returnButtonText,
    returnOrThrowBlock = returnOrThrowBlock
)

suspend inline fun Fragment.ensureAllPermissions(
    permissionNames: List<String>,
    askDialogTitle: CharSequence?,
    askDialogMessage: CharSequence?,
    showRationaleBeforeFirstAsk: Boolean = true,
    returnButtonText: CharSequence = txt(CommonR.string.quit),
    returnOrThrowBlock: () -> Nothing
): Unit = ensureAllPermissions(
    activity = requireActivity(),
    fragmentManager = parentFragmentManager,
    lifecycle = lifecycle,
    permissionNames = permissionNames,
    askDialogTitle = askDialogTitle,
    askDialogMessage = askDialogMessage,
    showRationaleBeforeFirstAsk = showRationaleBeforeFirstAsk,
    returnButtonText = returnButtonText,
    returnOrThrowBlock = returnOrThrowBlock
)

suspend inline fun ensureAllPermissions(
    activity: Activity,
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    permissionNames: List<String>,
    askDialogTitle: CharSequence?,
    askDialogMessage: CharSequence?,
    showRationaleBeforeFirstAsk: Boolean = true,
    returnButtonText: CharSequence = activity.txt(CommonR.string.quit),
    returnOrThrowBlock: () -> Nothing
): Unit = ensureAllPermissions(
    activity = activity,
    fragmentManager = fragmentManager,
    lifecycle = lifecycle,
    permissionNames = permissionNames,
    showRationaleAndContinueOrReturn = {
        activity.alertDialog(
            title = askDialogTitle,
            message = askDialogMessage,
            icon = null
        ).showAndAwait(
            okValue = true,
            negativeButton = DialogButton(returnButtonText, false),
            dismissValue = true
        )
    },
    showRationaleBeforeFirstAsk = showRationaleBeforeFirstAsk,
    askOpenSettingsOrReturn = {
        activity.alertDialog(
            message = activity.txt(CommonR.string.permission_denied_permanently_go_to_settings),
            icon = null
        ).showAndAwait(
            okValue = true,
            negativeButton = DialogButton(returnButtonText, false),
            dismissValue = true
        )
    },
    returnOrThrowBlock = returnOrThrowBlock
)
