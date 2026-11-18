package com.example.shakelight

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Counts sharp jolts. A back-and-forth shake produces roughly one jolt per
 * direction change, so [requiredJolts] = 4 means "shake twice, firmly".
 */
class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {

    /** Minimum deviation from gravity (m/s²) that counts as a jolt. */
    @Volatile
    var threshold: Float = Prefs.DEFAULT_THRESHOLD.toFloat()

    private val requiredJolts = 4
    private val minGapMs = 100L       // ignore samples belonging to the same jolt
    private val windowMs = 1500L      // all jolts must happen within this time
    private val cooldownMs = 1500L    // ignore input right after a trigger

    private var joltCount = 0
    private var firstJoltAt = 0L
    private var lastJoltAt = 0L
    private var lastTriggerAt = 0L

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val deviation = abs(sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH)
        if (deviation < threshold) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastTriggerAt < cooldownMs) return
        if (now - lastJoltAt < minGapMs) return

        if (joltCount == 0 || now - firstJoltAt > windowMs) {
            joltCount = 0
            firstJoltAt = now
        }
        lastJoltAt = now
        joltCount++

        if (joltCount >= requiredJolts) {
            joltCount = 0
            lastTriggerAt = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
