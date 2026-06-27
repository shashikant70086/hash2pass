package com.skant.otpgateway.data

import android.util.Log
import at.favre.lib.crypto.bcrypt.BCrypt
import com.skant.otpgateway.data.local.OtpPending
import com.skant.otpgateway.data.local.OtpPendingDao
import com.skant.otpgateway.data.local.SendLogDao
import com.skant.otpgateway.data.local.SendLogEntry
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OtpService @Inject constructor(
    private val configRepo: ConfigRepository,
    private val sms: SmsSender,
    private val pendingDao: OtpPendingDao,
    private val logDao: SendLogDao
) {
    private val rng = SecureRandom()

    sealed interface RequestResult {
        data class Ok(val requestId: String, val expiresInSeconds: Int) : RequestResult
        data object RateLimited : RequestResult
        data class Failed(val reason: String) : RequestResult
    }

    sealed interface VerifyResult {
        data object Ok : VerifyResult
        data object Wrong : VerifyResult
        data object Expired : VerifyResult
        data object NotFound : VerifyResult
        data object TooManyAttempts : VerifyResult
    }

    suspend fun request(phone: String): RequestResult {
        val cfg = configRepo.get()
        val now = System.currentTimeMillis()

        pendingDao.purgeExpired(now)
        val recent = pendingDao.countByPhoneSince(phone, now - 60 * 60 * 1000)
        if (recent >= cfg.rateLimitPerPhonePerHour) return RequestResult.RateLimited

        val otp = (1..cfg.otpDigits).joinToString("") { rng.nextInt(10).toString() }
        val requestId = "req_" + randomId(16)
        val hash = BCrypt.withDefaults().hashToString(10, otp.toCharArray())

        pendingDao.upsert(
            OtpPending(
                requestId = requestId,
                phone = phone,
                otpHashBcrypt = hash,
                createdAt = now,
                expiresAt = now + cfg.otpTtlSeconds * 1000L,
                attempts = 0
            )
        )
        logDao.insert(SendLogEntry(requestId, phone, "pending", null, now))

        return when (val r = sms.send(phone, cfg.renderSms(otp), requestId)) {
            is SmsSender.Result.Sent -> {
                logDao.updateStatus(requestId, "sent")
                RequestResult.Ok(requestId, cfg.otpTtlSeconds)
            }
            is SmsSender.Result.Failed -> {
                logDao.updateStatus(requestId, "failed")
                pendingDao.delete(requestId)
                Log.w(TAG, "SMS send failed code=${r.code}")
                RequestResult.Failed("sms send failed (code ${r.code})")
            }
        }
    }

    suspend fun verify(requestId: String, code: String): VerifyResult {
        val cfg = configRepo.get()
        val row = pendingDao.get(requestId) ?: return VerifyResult.NotFound
        val now = System.currentTimeMillis()
        if (now > row.expiresAt) {
            pendingDao.delete(requestId)
            logDao.updateStatus(requestId, "expired")
            return VerifyResult.Expired
        }
        if (row.attempts >= cfg.maxVerifyAttempts) {
            pendingDao.delete(requestId)
            return VerifyResult.TooManyAttempts
        }
        pendingDao.bumpAttempts(requestId)
        val ok = BCrypt.verifyer().verify(code.toCharArray(), row.otpHashBcrypt).verified
        return if (ok) {
            pendingDao.delete(requestId)
            logDao.updateStatus(requestId, "verified")
            VerifyResult.Ok
        } else VerifyResult.Wrong
    }

    private fun randomId(len: Int): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val sb = StringBuilder(len)
        repeat(len) { sb.append(alphabet[rng.nextInt(alphabet.length)]) }
        return sb.toString()
    }

    private companion object { const val TAG = "OtpService" }
}
