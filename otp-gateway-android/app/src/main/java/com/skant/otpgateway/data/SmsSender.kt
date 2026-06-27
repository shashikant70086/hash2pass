package com.skant.otpgateway.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SmsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class SmsSender @Inject constructor(@ApplicationContext private val ctx: Context) {

    suspend fun send(phone: String, body: String, requestId: String): Result = suspendCancellableCoroutine { cont ->
        val action = "com.skant.otpgateway.SMS_SENT.$requestId"
        val sent = PendingIntent.getBroadcast(
            ctx, requestId.hashCode(), Intent(action).setPackage(ctx.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val ok = resultCode == android.app.Activity.RESULT_OK
                ctx.unregisterReceiver(this)
                if (cont.isActive) cont.resume(if (ok) Result.Sent else Result.Failed(resultCode))
            }
        }
        val filter = IntentFilter(action)
        if (Build.VERSION.SDK_INT >= 33) ctx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else ctx.registerReceiver(receiver, filter)

        try {
            val sm: SmsManager = if (Build.VERSION.SDK_INT >= 31)
                ctx.getSystemService(SmsManager::class.java)
            else @Suppress("DEPRECATION") SmsManager.getDefault()
            val parts = sm.divideMessage(body)
            if (parts.size == 1) sm.sendTextMessage(phone, null, body, sent, null)
            else sm.sendMultipartTextMessage(phone, null, parts, arrayListOf(sent), null)
        } catch (t: Throwable) {
            runCatching { ctx.unregisterReceiver(receiver) }
            if (cont.isActive) cont.resume(Result.Failed(-1))
        }
    }

    sealed interface Result {
        data object Sent : Result
        data class Failed(val code: Int) : Result
    }
}
