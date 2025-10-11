package com.example.jigsaw.utils

import android.content.Context
import android.util.Log

object DebugUtils {

    fun printAllLevelProgress(context: Context) {
        val prefs = context.getSharedPreferences("level_progress", Context.MODE_PRIVATE)
        val allEntries = prefs.all

        Log.d("DebugUtils", "=== All Level Progress ===")
        for ((key, value) in allEntries) {
            Log.d("DebugUtils", "$key: $value")
        }
        Log.d("DebugUtils", "========================")
    }

    fun clearAllProgress(context: Context) {
        val prefs = context.getSharedPreferences("level_progress", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Log.d("DebugUtils", "All progress cleared")
    }
}