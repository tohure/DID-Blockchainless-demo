package dev.tohure.didblockchainlessdemo.utils

import android.util.Log
import dev.tohure.didblockchainlessdemo.BuildConfig

object AppLogger {

    private const val TAG_PREFIX = "tohure-"

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG_PREFIX + tag, message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG_PREFIX + tag, message, throwable)
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG_PREFIX + tag, message, throwable)
        }
    }
}