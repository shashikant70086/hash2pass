package com.skant.otpgateway.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.skant.otpgateway.data.local.SendLogDao
import com.skant.otpgateway.data.local.SendLogEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(dao: SendLogDao) : ViewModel() {
    val entries: Flow<List<SendLogEntry>> = dao.recent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(onBack: () -> Unit, vm: LogViewModel = hiltViewModel()) {
    val list by vm.entries.collectAsState(initial = emptyList())
    val fmt = remember { SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault()) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Delivery log") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }
        )
    }) { pad ->
        if (list.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No deliveries yet", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
                items(list, key = { it.requestId }) { e ->
                    ListItem(
                        headlineContent = { Text(e.phone) },
                        supportingContent = {
                            Text("${fmt.format(Date(e.createdAt))} · ${e.status}${e.errorCode?.let { " (code $it)" } ?: ""}")
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
