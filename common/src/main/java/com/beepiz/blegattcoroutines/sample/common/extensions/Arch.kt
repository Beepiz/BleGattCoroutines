package com.beepiz.blegattcoroutines.sample.common.extensions

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import splitties.checkedlazy.uiLazy

inline fun <T> LifecycleOwner.observe(liveData: LiveData<T>, crossinline observer: (t: T?) -> Unit) {
    liveData.observe(this, Observer { observer(it) })
}

inline fun <T> LifecycleOwner.observeNotNull(liveData: LiveData<T>, crossinline observer: (t: T) -> Unit) {
    liveData.observe(this, Observer { it?.let { t -> observer(t) } })
}

inline fun <reified VM: ViewModel> FragmentActivity.activityScope() = uiLazy {
    ViewModelProviders.of(this).get(VM::class.java)
}

inline fun <reified VM: ViewModel> Fragment.activityScope() = uiLazy {
    ViewModelProviders.of(this).get(VM::class.java)
}
