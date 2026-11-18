package com.example.shakelight

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs.get(this)
        val dp = resources.displayMetrics.density

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (24 * dp).toInt()
            setPadding(pad, (56 * dp).toInt(), pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "Shake Light"
            textSize = 28f
        })
        root.addView(TextView(this).apply {
            text = "Shake the phone twice, firmly, to turn the flashlight on or off. " +
                "Works with the screen off while this is enabled."
            setPadding(0, (8 * dp).toInt(), 0, (24 * dp).toInt())
        })

        val toggle = Switch(this).apply {
            text = "Enable shake for flashlight"
            textSize = 18f
            isChecked = prefs.getBoolean(Prefs.KEY_ENABLED, false)
            setOnCheckedChangeListener { _, on ->
                prefs.edit().putBoolean(Prefs.KEY_ENABLED, on).apply()
                if (on) ShakeService.start(this@MainActivity) else ShakeService.stop(this@MainActivity)
            }
        }
        root.addView(toggle)

        // Slider: right = more sensitive (lower threshold).
        val range = Prefs.MAX_THRESHOLD - Prefs.MIN_THRESHOLD
        val label = TextView(this).apply { setPadding(0, (32 * dp).toInt(), 0, 0) }
        val bar = SeekBar(this).apply {
            max = range
            progress = Prefs.MAX_THRESHOLD - prefs.getInt(Prefs.KEY_THRESHOLD, Prefs.DEFAULT_THRESHOLD)
        }
        fun updateLabel() {
            label.text = "Sensitivity: ${bar.progress * 100 / range}%  (higher = easier to trigger)"
        }
        updateLabel()
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, value: Int, fromUser: Boolean) {
                prefs.edit().putInt(Prefs.KEY_THRESHOLD, Prefs.MAX_THRESHOLD - value).apply()
                updateLabel()
            }
            override fun onStartTrackingTouch(sb: SeekBar) = Unit
            override fun onStopTrackingTouch(sb: SeekBar) = Unit
        })
        root.addView(label)
        root.addView(bar)

        root.addView(TextView(this).apply {
            text = "\nTo keep it working in the background, set this app's battery usage " +
                "to Unrestricted / Allow background activity, and allow auto-start."
        })
        root.addView(Button(this).apply {
            text = "Open battery settings"
            gravity = Gravity.CENTER
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        })

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        // Re-start if the user had it enabled but the system killed the service.
        if (Prefs.get(this).getBoolean(Prefs.KEY_ENABLED, false)) ShakeService.start(this)
    }
}
