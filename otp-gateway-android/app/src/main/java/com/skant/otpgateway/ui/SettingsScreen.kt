package com.skant.otpgateway.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skant.otpgateway.data.Config
import com.skant.otpgateway.data.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: ConfigRepository
) : ViewModel() {

    private val _draft = MutableStateFlow<Config?>(null)
    val draft: StateFlow<Config?> = _draft

    private val _saved = MutableStateFlow<String?>(null)
    val saved: StateFlow<String?> = _saved

    init {
        viewModelScope.launch { _draft.value = repo.get() }
    }

    fun update(transform: (Config) -> Config) {
        _draft.value = _draft.value?.let(transform)
    }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        _draft.value?.let { repo.save(it); _saved.value = "Saved. Restart the server to apply port/CORS changes." }
        onDone()
    }

    fun rotate() = viewModelScope.launch {
        val newKey = repo.rotateApiKey()
        _draft.value = _draft.value?.copy(apiKey = newKey)
        _saved.value = "API key rotated."
    }

    fun resetDefaults() = viewModelScope.launch {
        val current = _draft.value ?: repo.get()
        _draft.value = Config.DEFAULT.copy(apiKey = current.apiKey)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val draft by vm.draft.collectAsState()
    val saved by vm.saved.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = { TextButton(onClick = { vm.save(onBack) }) { Text("Save") } }
            )
        }
    ) { pad ->
        val c = draft ?: return@Scaffold
        Column(
            Modifier.padding(pad).padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionHeader("Server")
            NumberField("Port", c.port, 1..65535) { v -> vm.update { it.copy(port = v) } }
            TextField(
                value = c.publicBaseUrl,
                onValueChange = { v -> vm.update { it.copy(publicBaseUrl = v) } },
                label = { Text("Public base URL (e.g. https://otp.your-tunnel.app)") },
                placeholder = { Text("optional — leave blank for LAN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            SectionHeader("Auth")
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                TextField(
                    value = c.apiKey,
                    onValueChange = { v -> vm.update { it.copy(apiKey = v) } },
                    label = { Text("API key (sk_…)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { vm.rotate() }) { Icon(Icons.Filled.Refresh, "Rotate") }
            }

            SectionHeader("OTP")
            NumberField("Digits", c.otpDigits, 4..8) { v -> vm.update { it.copy(otpDigits = v) } }
            NumberField("TTL (seconds)", c.otpTtlSeconds, 30..1800) { v -> vm.update { it.copy(otpTtlSeconds = v) } }
            TextField(
                value = c.smsTemplate,
                onValueChange = { v -> vm.update { it.copy(smsTemplate = v) } },
                label = { Text("SMS template ({otp}, {ttl_min})") },
                modifier = Modifier.fillMaxWidth()
            )

            SectionHeader("Limits")
            NumberField("Max OTP requests per phone per hour", c.rateLimitPerPhonePerHour, 1..100) { v -> vm.update { it.copy(rateLimitPerPhonePerHour = v) } }
            NumberField("Max API calls per key per minute", c.rateLimitPerKeyPerMinute, 1..1000) { v -> vm.update { it.copy(rateLimitPerKeyPerMinute = v) } }
            NumberField("Max verify attempts per OTP", c.maxVerifyAttempts, 1..20) { v -> vm.update { it.copy(maxVerifyAttempts = v) } }

            SectionHeader("CORS")
            TextField(
                value = c.corsOrigins,
                onValueChange = { v -> vm.update { it.copy(corsOrigins = v) } },
                label = { Text("Allowed origins (comma-separated or *)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { vm.resetDefaults() }, modifier = Modifier.fillMaxWidth()) {
                Text("Reset defaults (keeps API key)")
            }
            saved?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun NumberField(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    var raw by remember(value) { mutableStateOf(value.toString()) }
    TextField(
        value = raw,
        onValueChange = { s ->
            raw = s.filter { it.isDigit() }.take(7)
            raw.toIntOrNull()?.let { if (it in range) onChange(it) }
        },
        label = { Text("$label  (${range.first}–${range.last})") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
