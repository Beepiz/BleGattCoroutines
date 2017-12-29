package com.beepiz.blegattcoroutines.sample.extensions

import android.arch.lifecycle.*
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import splitties.concurrency.uiLazy

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
