package com.skant.otpgateway.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.skant.otpgateway.MainActivity
import com.skant.otpgateway.OtpGatewayApplication.Companion.CHANNEL_GATEWAY
import com.skant.otpgateway.R
import com.skant.otpgateway.server.OtpHttpServer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GatewayForegroundService : Service() {

    @Inject lateinit var server: OtpHttpServer

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification("starting…"))
        server.start(
            onStarted = { port -> updateNotification("listening on :$port") },
            onError = { e -> updateNotification("error: ${e.message}") }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        server.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun updateNotification(text: String) {
        val nm = getSystemService(android.app.NotificationManager::class.java)
        nm?.notify(NOTIF_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val tap = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_GATEWAY)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setContentIntent(tap)
            .build()
    }

    companion object {
        private const val NOTIF_ID = 1001
        fun start(ctx: Context) {
            ctx.startForegroundService(Intent(ctx, GatewayForegroundService::class.java))
        }
        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, GatewayForegroundService::class.java))
        }
    }
}
