package com.example.shakelight

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    const val KEY_ENABLED = "enabled"
    const val KEY_THRESHOLD = "threshold"

    /** Shake strength in m/s² beyond gravity. Lower = more sensitive. */
    const val DEFAULT_THRESHOLD = 15
    const val MIN_THRESHOLD = 8
    const val MAX_THRESHOLD = 32

    fun get(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences("shake_light", Context.MODE_PRIVATE)
}
