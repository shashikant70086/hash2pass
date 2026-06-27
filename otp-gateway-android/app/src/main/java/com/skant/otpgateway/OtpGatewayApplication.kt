package com.skant.otpgateway

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OtpGatewayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GATEWAY,
                getString(R.string.channel_gateway),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }
    companion object { const val CHANNEL_GATEWAY = "gateway" }
}
