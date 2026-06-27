package com.skant.otpgateway.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skant.otpgateway.data.Config
import com.skant.otpgateway.data.ConfigRepository
import com.skant.otpgateway.data.NetworkUtils
import com.skant.otpgateway.data.local.SendLogDao
import com.skant.otpgateway.server.OtpHttpServer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val configRepo: ConfigRepository,
    private val server: OtpHttpServer,
    sendLogDao: SendLogDao
) : ViewModel() {

    val config: StateFlow<Config> = configRepo.flow.stateIn(viewModelScope, SharingStarted.Eagerly, Config.DEFAULT)
    val sentToday: Flow<Int> = sendLogDao.countSentSince(startOfDayMs())

    private val _running = MutableStateFlow(server.isRunning())
    val running: StateFlow<Boolean> = _running

    private val _lanIp = MutableStateFlow<String?>(null)
    val lanIp: StateFlow<String?> = _lanIp

    init {
        viewModelScope.launch {
            _lanIp.value = NetworkUtils.lanIpv4()
            // poll the engine for status reflections
            while (true) {
                _running.value = server.isRunning()
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun startOfDayMs(): Long {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettings: () -> Unit,
    onLog: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onBatteryWhitelist: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val cfg by vm.config.collectAsState()
    val sentToday by vm.sentToday.collectAsState(initial = 0)
    val running by vm.running.collectAsState()
    val lanIp by vm.lanIp.collectAsState()
    val clipboard = LocalClipboardManager.current

    val lanUrl = lanIp?.let { "http://$it:${cfg.port}" } ?: "(no Wi-Fi)"
    val publicUrl = cfg.publicBaseUrl.ifBlank { lanUrl }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("hash2pass") },
            actions = {
                TextButton(onClick = onSettings) { Text("Settings") }
                TextButton(onClick = onLog) { Text("Log") }
            }
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(10.dp).clip(RoundedCornerShape(50))
                                .background(if (running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (running) "Running" else "Stopped", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("$sentToday OTPs sent today", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Text("Base URL", style = MaterialTheme.typography.labelMedium)
                    UrlRow(publicUrl, clipboard)
                    if (cfg.publicBaseUrl.isNotBlank() && lanIp != null) {
                        Spacer(Modifier.height(8.dp))
                        Text("Local URL", style = MaterialTheme.typography.labelMedium)
                        UrlRow(lanUrl, clipboard)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("API key", style = MaterialTheme.typography.labelMedium)
                    UrlRow(if (cfg.apiKey.length > 12) cfg.apiKey.take(8) + "…" + cfg.apiKey.takeLast(4) else cfg.apiKey, clipboard, copyValue = cfg.apiKey)
                }
            }

            if (running) {
                OutlinedButton(onClick = onStopService, modifier = Modifier.fillMaxWidth()) { Text("Stop server") }
            } else {
                Button(onClick = onStartService, modifier = Modifier.fillMaxWidth()) { Text("Start server") }
            }
            OutlinedButton(onClick = onBatteryWhitelist, modifier = Modifier.fillMaxWidth()) {
                Text("Disable battery optimization")
            }

            Spacer(Modifier.weight(1f))
            Text(
                "Stay charged + on Wi-Fi. Phone IP changes when you switch networks.",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun UrlRow(display: String, clipboard: androidx.compose.ui.platform.ClipboardManager, copyValue: String = display) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            display,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { clipboard.setText(AnnotatedString(copyValue)) }) {
            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
        }
    }
}
