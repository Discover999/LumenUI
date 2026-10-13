package com.lumenui.app.util

import android.util.Log
import com.lumenui.app.BuildConfig

/**
 * Thin logging facade: one tag for the whole app, and output is dropped entirely in release
 * builds. That keeps diagnostics (which can include host addresses and egress IPs) out of a
 * shipped device's logcat, while debug builds stay fully observable.
 */
object AppLog {
    private const val TAG = "LumenUI"

    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    fun w(message: String, error: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.w(TAG, message, error)
    }

    fun e(message: String, error: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.e(TAG, message, error)
    }
}
