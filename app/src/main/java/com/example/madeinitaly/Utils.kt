package com.example.madeinitaly

import android.content.Context
import android.widget.Toast
import android.util.Log

object Utils {
    private const val TAG = "MadeInItaly"

    fun logError(message: String, e: Exception? = null) {
        e?.let {
            Log.e(TAG, message, it)
        } ?: Log.e(TAG, message)
    }

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}