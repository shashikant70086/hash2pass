package com.skant.otpgateway.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

private val Context.cfgStore by preferencesDataStore("otp_gateway_config")

/**
 * Single source of truth. Mirrors a 12-factor-ish `.env` file but lives in DataStore
 * so the user can edit values from the Settings screen. Restart the server to pick
 * up changes that affect the listener (PORT, CORS).
 */
data class Config(
    val port: Int,
    val apiKey: String,
    val otpDigits: Int,
    val otpTtlSeconds: Int,
    val rateLimitPerPhonePerHour: Int,
    val rateLimitPerKeyPerMinute: Int,
    val maxVerifyAttempts: Int,
    val smsTemplate: String,
    val corsOrigins: String,
    val publicBaseUrl: String,
    val autoStartOnBoot: Boolean
) {
    fun renderSms(otp: String): String =
        smsTemplate.replace("{otp}", otp).replace("{ttl_min}", (otpTtlSeconds / 60).coerceAtLeast(1).toString())

    companion object {
        val DEFAULT = Config(
            port = 8787,
            apiKey = "",                                          // generated on first launch
            otpDigits = 6,
            otpTtlSeconds = 300,
            rateLimitPerPhonePerHour = 5,
            rateLimitPerKeyPerMinute = 30,
            maxVerifyAttempts = 5,
            smsTemplate = "Yo! Your survival key is {otp} 🔐. Don’t sleep on it! use it before {ttl_min} min otherwise it will sleep ",
            corsOrigins = "*",
            publicBaseUrl = "",
            autoStartOnBoot = false
        )
    }
}

@Singleton
class ConfigRepository @Inject constructor(@ApplicationContext private val ctx: Context) {

    private object K {
        val PORT = intPreferencesKey("port")
        val API_KEY = stringPreferencesKey("api_key")
        val OTP_DIGITS = intPreferencesKey("otp_digits")
        val OTP_TTL_SECONDS = intPreferencesKey("otp_ttl_seconds")
        val RL_PHONE_HOUR = intPreferencesKey("rl_phone_hour")
        val RL_KEY_MIN = intPreferencesKey("rl_key_min")
        val MAX_VERIFY = intPreferencesKey("max_verify")
        val SMS_TEMPLATE = stringPreferencesKey("sms_template")
        val CORS = stringPreferencesKey("cors")
        val PUBLIC_BASE_URL = stringPreferencesKey("public_base_url")
        val AUTOSTART = booleanPreferencesKey("autostart")
    }

    val flow: Flow<Config> = ctx.cfgStore.data.map { p ->
        Config(
            port = p[K.PORT] ?: Config.DEFAULT.port,
            apiKey = p[K.API_KEY] ?: "",
            otpDigits = p[K.OTP_DIGITS] ?: Config.DEFAULT.otpDigits,
            otpTtlSeconds = p[K.OTP_TTL_SECONDS] ?: Config.DEFAULT.otpTtlSeconds,
            rateLimitPerPhonePerHour = p[K.RL_PHONE_HOUR] ?: Config.DEFAULT.rateLimitPerPhonePerHour,
            rateLimitPerKeyPerMinute = p[K.RL_KEY_MIN] ?: Config.DEFAULT.rateLimitPerKeyPerMinute,
            maxVerifyAttempts = p[K.MAX_VERIFY] ?: Config.DEFAULT.maxVerifyAttempts,
            smsTemplate = p[K.SMS_TEMPLATE] ?: Config.DEFAULT.smsTemplate,
            corsOrigins = p[K.CORS] ?: Config.DEFAULT.corsOrigins,
            publicBaseUrl = p[K.PUBLIC_BASE_URL] ?: Config.DEFAULT.publicBaseUrl,
            autoStartOnBoot = p[K.AUTOSTART] ?: Config.DEFAULT.autoStartOnBoot
        )
    }

    suspend fun get(): Config = flow.first().let { c ->
        if (c.apiKey.isBlank()) {
            val key = generateApiKey()
            ctx.cfgStore.edit { it[K.API_KEY] = key }
            c.copy(apiKey = key)
        } else c
    }

    suspend fun save(c: Config) {
        ctx.cfgStore.edit { p ->
            p[K.PORT] = c.port
            p[K.API_KEY] = c.apiKey
            p[K.OTP_DIGITS] = c.otpDigits
            p[K.OTP_TTL_SECONDS] = c.otpTtlSeconds
            p[K.RL_PHONE_HOUR] = c.rateLimitPerPhonePerHour
            p[K.RL_KEY_MIN] = c.rateLimitPerKeyPerMinute
            p[K.MAX_VERIFY] = c.maxVerifyAttempts
            p[K.SMS_TEMPLATE] = c.smsTemplate
            p[K.CORS] = c.corsOrigins
            p[K.PUBLIC_BASE_URL] = c.publicBaseUrl
            p[K.AUTOSTART] = c.autoStartOnBoot
        }
    }

    suspend fun rotateApiKey(): String {
        val key = generateApiKey()
        ctx.cfgStore.edit { it[K.API_KEY] = key }
        return key
    }

    private fun generateApiKey(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return "sk_" + android.util.Base64.encodeToString(
            bytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
        )
    }
}
