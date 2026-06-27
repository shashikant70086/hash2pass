package com.skant.otpgateway.server

import android.util.Log
import com.skant.otpgateway.BuildConfig
import com.skant.otpgateway.data.Config
import com.skant.otpgateway.data.ConfigRepository
import com.skant.otpgateway.data.OtpService
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Serializable data class RequestBody(val phone: String)
@Serializable data class RequestResp(val requestId: String, val expiresInSeconds: Int)
@Serializable data class VerifyBody(val requestId: String, val code: String)
@Serializable data class VerifyResp(val ok: Boolean, val reason: String? = null)
@Serializable data class ErrorResp(val error: String)
@Serializable data class InfoResp(
    val version: String,
    val otpDigits: Int,
    val otpTtlSeconds: Int,
    val rateLimitPerPhonePerHour: Int,
    val rateLimitPerKeyPerMinute: Int
)
@Serializable data class HealthResp(val ok: Boolean = true)

@Singleton
class OtpHttpServer @Inject constructor(
    private val configRepo: ConfigRepository,
    private val otp: OtpService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var engine: ApplicationEngine? = null
    @Volatile private var runningPort: Int = 0

    private val keyCounters = ConcurrentHashMap<Long, AtomicLong>()

    fun isRunning(): Boolean = engine != null
    fun port(): Int = runningPort

    fun start(onStarted: (Int) -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        if (engine != null) { onStarted(runningPort); return }
        scope.launch {
            val cfg = configRepo.get()
            try {
                val srv = embeddedServer(CIO, host = "0.0.0.0", port = cfg.port) {
                    configure(cfg)
                }.also { it.start(wait = false) }
                engine = srv
                runningPort = cfg.port
                onStarted(cfg.port)
                Log.i(TAG, "embedded server up on :${cfg.port}")
            } catch (t: Throwable) {
                Log.e(TAG, "server start failed", t)
                onError(t)
            }
        }
    }

    fun stop() {
        scope.launch {
            engine?.stop(500, 1000)
            engine = null
            runningPort = 0
            keyCounters.clear()
            Log.i(TAG, "embedded server stopped")
        }
    }

    private fun Application.configure(cfg: Config) {
        install(DefaultHeaders) { header("X-Powered-By", "otp-gateway/${BuildConfig.VERSION_NAME}") }
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; prettyPrint = false }) }
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
            allowHeader("x-api-key")
            allowMethod(HttpMethod.Post); allowMethod(HttpMethod.Get); allowMethod(HttpMethod.Options)
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                Log.e(TAG, "unhandled", cause)
                call.respond(HttpStatusCode.InternalServerError, ErrorResp("internal error"))
            }
        }

        routing {
            get("/healthz") { call.respond(HealthResp()) }

            get("/v1/info") {
                if (!authorize(call, cfg)) return@get
                call.respond(
                    InfoResp(
                        version = BuildConfig.VERSION_NAME,
                        otpDigits = cfg.otpDigits,
                        otpTtlSeconds = cfg.otpTtlSeconds,
                        rateLimitPerPhonePerHour = cfg.rateLimitPerPhonePerHour,
                        rateLimitPerKeyPerMinute = cfg.rateLimitPerKeyPerMinute
                    )
                )
            }

            post("/v1/otp/request") {
                if (!authorize(call, cfg)) return@post
                val body = runCatching { call.receive<RequestBody>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, ErrorResp("invalid body")); return@post
                }
                val phone = body.phone.trim()
                if (!phone.matches(E164)) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResp("phone must be E.164, e.g. +919876543210"))
                    return@post
                }
                when (val r = otp.request(phone)) {
                    is OtpService.RequestResult.Ok ->
                        call.respond(RequestResp(r.requestId, r.expiresInSeconds))
                    OtpService.RequestResult.RateLimited ->
                        call.respond(HttpStatusCode.TooManyRequests, ErrorResp("rate limit exceeded for this phone"))
                    is OtpService.RequestResult.Failed ->
                        call.respond(HttpStatusCode.BadGateway, ErrorResp(r.reason))
                }
            }

            post("/v1/otp/verify") {
                if (!authorize(call, cfg)) return@post
                val body = runCatching { call.receive<VerifyBody>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, ErrorResp("invalid body")); return@post
                }
                when (otp.verify(body.requestId, body.code)) {
                    OtpService.VerifyResult.Ok -> call.respond(VerifyResp(true))
                    OtpService.VerifyResult.Wrong -> call.respond(HttpStatusCode.Unauthorized, VerifyResp(false, "wrong code"))
                    OtpService.VerifyResult.Expired -> call.respond(HttpStatusCode.Gone, VerifyResp(false, "expired"))
                    OtpService.VerifyResult.NotFound -> call.respond(HttpStatusCode.NotFound, VerifyResp(false, "unknown request"))
                    OtpService.VerifyResult.TooManyAttempts -> call.respond(HttpStatusCode.TooManyRequests, VerifyResp(false, "too many attempts"))
                }
            }
        }
    }

    private suspend fun authorize(call: ApplicationCall, cfg: Config): Boolean {
        val provided = call.request.headers["x-api-key"]
            ?: call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")?.trim()
        if (provided.isNullOrBlank() || provided != cfg.apiKey) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResp("unauthorized"))
            return false
        }
        val nowMin = System.currentTimeMillis() / 60_000L
        val counter = keyCounters.computeIfAbsent(nowMin) { AtomicLong(0) }
        keyCounters.keys.removeIf { it < nowMin }
        if (counter.incrementAndGet() > cfg.rateLimitPerKeyPerMinute) {
            call.respond(HttpStatusCode.TooManyRequests, ErrorResp("global rate limit exceeded"))
            return false
        }
        return true
    }

    private companion object {
        const val TAG = "OtpHttpServer"
        val E164 = Regex("^\\+[1-9]\\d{6,14}$")
    }
}
