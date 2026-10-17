package com.freefit.app.run

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.freefit.app.MainActivity
import com.freefit.app.R
import com.freefit.app.t
import kotlin.math.roundToInt

/** 前台服务：持续接收 GPS 定位并写入 RunTracker。 */
class RunService : Service() {

    companion object {
        const val CHANNEL_ID = "run_tracking"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "start"
        const val ACTION_PAUSE = "pause"
        const val ACTION_RESUME = "resume"
        const val ACTION_STOP = "stop"

        fun start(ctx: Context) = send(ctx, ACTION_START)
        fun pause(ctx: Context) = send(ctx, ACTION_PAUSE)
        fun resume(ctx: Context) = send(ctx, ACTION_RESUME)
        fun stop(ctx: Context) = send(ctx, ACTION_STOP)

        private fun send(ctx: Context, action: String) {
            val intent = Intent(ctx, RunService::class.java).setAction(action)
            if (action == ACTION_START) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }
    }

    private var locationManager: LocationManager? = null
    private val listener = LocationListener { location: Location ->
        RunTracker.onLocation(location.latitude, location.longitude, location.accuracy)
        updateNotification()
    }
    private var listening = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        when (intent?.action) {
            ACTION_START -> {
                RunTracker.start()
                startAsForeground()
                startListening()
            }
            ACTION_PAUSE -> {
                RunTracker.pause()
                stopListening()
            }
            ACTION_RESUME -> {
                RunTracker.resume()
                startListening()
            }
            ACTION_STOP -> {
                stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startListening() {
        if (listening) return
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager = lm
        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return
        }
        try {
            lm.requestLocationUpdates(provider, 2000L, 1f, listener)
            listening = true
        } catch (_: SecurityException) {
            // 无定位权限时保持空转，界面层已做权限引导
        }
    }

    private fun stopListening() {
        if (!listening) return
        locationManager?.removeUpdates(listener)
        listening = false
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val s = RunTracker.state.value
        val km = s.distanceM / 1000f
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_run)
            .setContentTitle(if (s.paused) t("跑步已暂停", "Run paused") else t("FreeFit 正在记录跑步", "FreeFit is tracking your run"))
            .setContentText(String.format(t("已跑 %.2f 公里", "%.2f km so far"), km))
            .setContentIntent(openApp)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID, t("跑步记录", "Run Tracking"), NotificationManager.IMPORTANCE_LOW,
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }
}
