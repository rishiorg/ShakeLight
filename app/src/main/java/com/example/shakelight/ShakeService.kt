package com.example.shakelight

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator

class ShakeService : Service() {

    private lateinit var sensorManager: SensorManager
    private lateinit var cameraManager: CameraManager
    private lateinit var detector: ShakeDetector
    private var wakeLock: PowerManager.WakeLock? = null

    private var cameraId: String? = null
    @Volatile private var torchOn = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id == cameraId) torchOn = enabled
        }
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == Prefs.KEY_THRESHOLD) {
            detector.threshold = prefs.getInt(Prefs.KEY_THRESHOLD, Prefs.DEFAULT_THRESHOLD).toFloat()
        }
    }

    override fun onCreate() {
        super.onCreate()
        startAsForeground()

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = findFlashCamera()
        cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))

        val prefs = Prefs.get(this)
        detector = ShakeDetector(::toggleTorch).apply {
            threshold = prefs.getInt(Prefs.KEY_THRESHOLD, Prefs.DEFAULT_THRESHOLD).toFloat()
        }
        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // Prefer the "wake-up" accelerometer: it keeps delivering while the screen is off
        // without holding the CPU awake. Fall back to a partial wake lock if unavailable.
        var sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true)
        if (sensor == null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ShakeLight:listener")
                .apply { acquire() }
        }
        if (sensor != null) {
            sensorManager.registerListener(detector, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        sensorManager.unregisterListener(detector)
        cameraManager.unregisterTorchCallback(torchCallback)
        Prefs.get(this).unregisterOnSharedPreferenceChangeListener(prefListener)
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---- flashlight ----

    private fun findFlashCamera(): String? = try {
        cameraManager.cameraIdList.firstOrNull { id ->
            val c = cameraManager.getCameraCharacteristics(id)
            c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    } catch (e: CameraAccessException) {
        null
    }

    private fun toggleTorch() {
        val id = cameraId ?: return
        try {
            cameraManager.setTorchMode(id, !torchOn)
            buzz()
        } catch (e: CameraAccessException) {
            // Camera in use by another app; ignore.
        } catch (e: IllegalArgumentException) {
            // Camera id no longer valid; ignore.
        }
    }

    private fun buzz() {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        v.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    // ---- foreground notification ----

    private fun startAsForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Shake for flashlight", NotificationManager.IMPORTANCE_MIN)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_flash)
            .setContentTitle("Shake for flashlight is on")
            .setContentText("Shake your phone twice to toggle the torch")
            .setContentIntent(open)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "shake_light"
        private const val NOTIF_ID = 1

        fun start(ctx: Context) {
            ctx.startForegroundService(Intent(ctx, ShakeService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, ShakeService::class.java))
        }
    }
}
