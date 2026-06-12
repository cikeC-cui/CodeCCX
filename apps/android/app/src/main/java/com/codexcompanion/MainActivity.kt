package com.codexcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codexcompanion.model.AppServerDiagnosticEvent
import com.codexcompanion.model.BridgeStatus
import com.codexcompanion.model.ConversationEvent
import com.codexcompanion.model.Screen
import com.codexcompanion.model.ThreadSummary
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.util.Locale
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

private val AppBackground = Color(0xFFFAFAFF)
private val CardSurface = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF111827)
private val TextMuted = Color(0xFF747A90)
private val BorderSoft = Color(0xFFE5E7F1)
private val BrandPurple = Color(0xFF6246EA)
private val BrandPurpleSoft = Color(0xFFEDE9FE)
private val TrafficIdle = Color(0xFF10B981)
private val TrafficActive = Color(0xFFF59E0B)
private val TrafficFocus = Color(0xFFE11D48)
private val TrafficComplete = Color(0xFF2563EB)
private val SuccessSoft = Color(0xFFE7F8EF)
private val WarningSoft = Color(0xFFFFF7E6)
private val ErrorSoft = Color(0xFFFFE8ED)
private val BlueSoft = Color(0xFFEAF3FF)
private val CompleteSoft = Color(0xFFEFF6FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CodexCompanionApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodexCompanionApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = BrandPurple,
            secondary = TrafficIdle,
            surface = CardSurface,
            background = AppBackground
        )
    ) {
        Scaffold(
            containerColor = AppBackground,
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AppBackground),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (state.screen != Screen.ThreadDetail) BrandMark()
                            Text(
                                "Code 伴侣",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        if (state.screen == Screen.ThreadDetail || state.screen == Screen.NetworkSettings) {
                            IconButton(onClick = viewModel::backToThreads) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            }
                        }
                    },
                    actions = {
                        if (state.savedBridge != null) {
                            IconButton(onClick = {
                                viewModel.refreshStatus()
                                viewModel.refreshThreads()
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "刷新")
                            }
                            IconButton(onClick = viewModel::disconnect) {
                                Icon(Icons.Default.Logout, contentDescription = "断开连接")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(AppBackground)
            ) {
                when (state.screen) {
                    Screen.Connect -> ConnectScreen(state, viewModel)
                    Screen.Threads -> ThreadsScreen(state, viewModel)
                    Screen.NetworkSettings -> NetworkSettingsScreen(state, viewModel)
                    Screen.ThreadDetail -> ThreadDetailScreen(state, viewModel)
                }
                if (state.networkAddressDialogVisible) {
                    NetworkAddressDialog(state, viewModel)
                }
                if (state.busy) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun ConnectScreen(state: UiState, viewModel: MainViewModel) {
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let(viewModel::applyQrPayload)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardSurface,
                shadowElevation = 5.dp,
                border = BorderStroke(1.dp, BorderSoft)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BrandMark()
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("连接电脑端 Bridge", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(state.bridgeStatus, color = TextMuted)
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = viewModel::updateBaseUrl,
                label = { Text("电脑地址") },
                placeholder = { Text("http://192.168.1.10:4518") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = formFieldColors()
            )
        }
        item {
            OutlinedTextField(
                value = state.pairToken,
                onValueChange = viewModel::updatePairToken,
                label = { Text("配对码") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = formFieldColors()
            )
        }
        item {
            OutlinedTextField(
                value = state.deviceName,
                onValueChange = viewModel::updateDeviceName,
                label = { Text("设备名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = formFieldColors()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        val options = ScanOptions()
                            .setDesiredBarcodeFormats(listOf(ScanOptions.QR_CODE))
                            .setPrompt("扫描 Bridge 二维码")
                            .setBeepEnabled(false)
                        scanner.launch(options)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderSoft)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("扫码")
                }
                OutlinedButton(
                    onClick = viewModel::testConnection,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderSoft)
                ) {
                    Icon(Icons.Default.Link, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("测试")
                }
            }
        }
        item {
            Button(
                onClick = viewModel::pair,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
            ) {
                Text("配对并进入")
            }
        }
        state.bridgeHealth?.let { item { BridgeDiagnostics(it) } }
        state.error?.let { error -> item { ErrorText(error) } }
    }
}

@Composable
fun ThreadsScreen(state: UiState, viewModel: MainViewModel) {
    val query = state.searchQuery.trim().lowercase()
    var collapsedProjects by remember { mutableStateOf(setOf<String>()) }
    val filtered = state.threads
        .filter { state.threadStatusFilter.matches(it) }
        .filter {
            query.isBlank() || "${it.title} ${it.preview} ${it.cwd.orEmpty()} ${it.model.orEmpty()}"
                .lowercase()
                .contains(query)
        }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        item {
            BridgeHeader(
                status = state.bridgeHealth,
                currentUrl = state.savedBridge?.baseUrl ?: state.baseUrl,
                networkMode = state.networkMode,
                onSettings = viewModel::openNetworkSettings
            )
        }
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("搜索会话") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPurple,
                    unfocusedBorderColor = BorderSoft,
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface
                )
            )
        }
        item {
            ThreadStatusFilterBar(
                selected = state.threadStatusFilter,
                threads = state.threads,
                onSelect = viewModel::setThreadStatusFilter
            )
        }
        if (filtered.isEmpty()) {
            item {
                EmptyText("没有找到会话。")
            }
        } else {
            filtered.groupBy(::projectKey).forEach { (project, threads) ->
                val expanded = project !in collapsedProjects
                item(key = "project-$project") {
                    ProjectHeader(
                        project = project,
                        count = threads.size,
                        expanded = expanded,
                        onToggle = {
                            collapsedProjects = if (expanded) {
                                collapsedProjects + project
                            } else {
                                collapsedProjects - project
                            }
                        }
                    )
                }
                if (expanded) {
                    items(threads, key = { it.id }) { thread ->
                        ThreadRow(thread, viewModel::openThread)
                    }
                }
            }
        }
        state.error?.let { error -> item { ErrorText(error) } }
    }
}

@Composable
fun NetworkSettingsScreen(state: UiState, viewModel: MainViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CardSurface,
                shadowElevation = 5.dp,
                border = BorderStroke(1.dp, BorderSoft)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("网络设置", color = TextPrimary, fontWeight = FontWeight.Bold)
                        StatusPill(state.networkMode.label, if (state.networkMode == NetworkMode.Lan) "idle" else "active")
                    }
                    Text(
                        state.savedBridge?.baseUrl ?: state.baseUrl,
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        item {
            NetworkSwitchCard(state, viewModel)
        }
        state.operationResult?.let { message -> item { OperationText(message) } }
        state.error?.let { error -> item { ErrorText(error) } }
    }
}

@Composable
fun ProjectHeader(project: String, count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(999.dp),
        color = BrandPurpleSoft,
        border = BorderStroke(1.dp, Color(0xFFD8CCFF))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = BrandPurple,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer(rotationZ = if (expanded) 90f else 0f)
            )
            Text(
                project,
                modifier = Modifier.weight(1f),
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "$count 个会话",
                color = BrandPurple,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun BridgeHeader(
    status: BridgeStatus?,
    currentUrl: String = "",
    networkMode: NetworkMode,
    onSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardSurface,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TrafficLightDot(if (status?.codexAppServer?.available == true) "idle" else "focus")
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        currentUrl.ifBlank { bestAddress(status) },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    StatusPill(networkMode.label, if (networkMode == NetworkMode.Lan) "idle" else "active")
                    IconButton(onClick = onSettings, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "网络设置", tint = BrandPurple)
                    }
                }
            }
            StatusPill(if (status?.codexAppServer?.available == true) "可用" else "不可用", if (status?.codexAppServer?.available == true) "idle" else "focus")
        }
    }
}

@Composable
fun NetworkSwitchCard(state: UiState, viewModel: MainViewModel) {
    val currentUrl = state.savedBridge?.baseUrl ?: state.baseUrl
    val lanUrl = state.savedBridge?.lanUrl.orEmpty()
    val remoteUrl = state.remoteBaseUrl.ifBlank { state.savedBridge?.remoteUrl.orEmpty() }
    val remoteReady = remoteUrl.isNotBlank()
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardSurface,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text("连接入口", modifier = Modifier.weight(1f), color = TextPrimary, fontWeight = FontWeight.Bold)
                StatusPill(state.networkMode.label, if (state.networkMode == NetworkMode.Lan) "idle" else "active")
            }
            Text(
                currentUrl,
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            NetworkOptionRow(
                title = "局域网",
                description = lanUrl.ifBlank { "同一 Wi-Fi 下自动使用电脑局域网地址" },
                selected = state.networkMode == NetworkMode.Lan,
                available = lanUrl.isNotBlank(),
                unavailableText = "未检测到可用局域网地址",
                icon = Icons.Default.Link,
                onClick = viewModel::useLanNetwork
            )
            NetworkOptionRow(
                title = "虚拟组网",
                description = remoteUrl.ifBlank { "填写 Tailscale 或 ZeroTier 地址后可用" },
                selected = state.networkMode == NetworkMode.Remote,
                available = remoteReady,
                unavailableText = "不可用，请先填写地址",
                icon = Icons.Default.Storage,
                onClick = viewModel::useRemoteNetwork
            )
            RemoteAddressRow(remoteUrl) { viewModel.showNetworkAddressDialog() }
            Text(
                "设置页切换入口后会停留在当前页面，方便继续检查连接状态。",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun NetworkOptionRow(
    title: String,
    description: String,
    selected: Boolean,
    available: Boolean,
    unavailableText: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) BrandPurpleSoft else Color(0xFFF8FAFF),
        border = BorderStroke(1.dp, if (selected) BrandPurple else BorderSoft)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) BrandPurple else TextMuted, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, color = TextPrimary, fontWeight = FontWeight.Bold)
                    StatusPill(
                        if (available) "可用" else "不可用",
                        if (available) "idle" else "focus"
                    )
                }
                Text(
                    if (available) description else unavailableText,
                    color = if (available) TextMuted else TrafficFocus,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "当前入口", tint = BrandPurple, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun RemoteAddressRow(remoteUrl: String, onEdit: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFF),
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("虚拟组网地址", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(
                    remoteUrl.ifBlank { "未设置，点击填写" },
                    color = if (remoteUrl.isBlank()) TextMuted else TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.Edit, contentDescription = "编辑地址", tint = BrandPurple)
        }
    }
}

@Composable
fun NetworkAddressDialog(state: UiState, viewModel: MainViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::hideNetworkAddressDialog,
        title = { Text("虚拟组网地址", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.networkAddressDraft,
                    onValueChange = viewModel::updateNetworkAddressDraft,
                    label = { Text("地址") },
                    placeholder = { Text("http://100.84.41.86:4518") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = formFieldColors()
                )
                Text(
                    "这里适合填写 Tailscale 或 ZeroTier 地址。保存后会写入本地缓存。",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = viewModel::saveNetworkAddress, colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)) {
                Text("保存")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = viewModel::clearNetworkAddress) {
                    Text("清空", color = TrafficFocus)
                }
                TextButton(onClick = viewModel::hideNetworkAddressDialog) {
                    Text("取消", color = TextMuted)
                }
            }
        },
        containerColor = CardSurface,
        shape = RoundedCornerShape(18.dp)
    )
}

fun projectKey(thread: ThreadSummary): String {
    val cwd = thread.cwd?.trim().orEmpty()
    if (cwd.isBlank()) return "未记录项目"
    return cwd.trimEnd('\\', '/').substringAfterLast('\\').substringAfterLast('/').ifBlank { cwd }
}

@Composable
fun ThreadRow(thread: ThreadSummary, onOpen: (ThreadSummary) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(thread) },
        shape = RoundedCornerShape(14.dp),
        color = CardSurface,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrafficLightDot(thread.status)
                Spacer(Modifier.width(10.dp))
                Text(thread.title, modifier = Modifier.weight(1f), color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                StatusPill(statusLabel(thread), thread.status)
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
            }
            Text(thread.preview, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${thread.model ?: "未知模型"} · ${formatShortTime(thread.updatedAt)}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}

@Composable
fun ThreadDetailScreen(state: UiState, viewModel: MainViewModel) {
    val visibleEvents = state.events.filterFor(state.eventFilter)
    val listState = rememberLazyListState()
    LaunchedEffect(state.selectedThread?.id, visibleEvents.size, state.eventFilter) {
        if (visibleEvents.isNotEmpty()) {
            listState.scrollToItem(visibleEvents.lastIndex)
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        state.selectedThread?.let { thread ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurface)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.showRenameDialog() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrafficLightDot(thread.status, focused = true)
                    Text(thread.title, modifier = Modifier.weight(1f), color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = viewModel::showRenameDialog, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "重命名会话", tint = TextMuted)
                    }
                    IconButton(onClick = viewModel::showDeleteDialog, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "删除会话", tint = TrafficFocus)
                    }
                    Text(formatShortTime(thread.updatedAt), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                FilterBar(state.eventFilter, state.events, viewModel::setEventFilter)
                QuotaSummaryCard(state.events)
            }
        }
        if (state.renameDialogVisible) {
            RenameThreadDialog(
                title = state.renameTitle,
                onTitleChange = viewModel::updateRenameTitle,
                onDismiss = viewModel::hideRenameDialog,
                onConfirm = viewModel::renameSelectedThread
            )
        }
        if (state.deleteDialogVisible) {
            DeleteThreadDialog(
                title = state.selectedThread?.title.orEmpty(),
                onDismiss = viewModel::hideDeleteDialog,
                onConfirm = viewModel::deleteSelectedThread
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (visibleEvents.isEmpty()) item { EmptyText("当前筛选条件下没有事件。") }
            items(visibleEvents, key = { it.id }) { event -> EventBubble(event) }
            state.error?.let { error -> item { ErrorText(error) } }
        }
        if (state.awaitingReply) {
            WaitingReplyIndicator(state.operationResult ?: "正在等待 Code 回复...")
        } else {
            state.operationResult?.let { OperationText(it) }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface)
                .border(BorderStroke(1.dp, BorderSoft))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = viewModel::interruptThread, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Stop, contentDescription = "中断", tint = TrafficFocus)
            }
            OutlinedTextField(
                value = state.inputText,
                onValueChange = viewModel::updateInput,
                placeholder = { Text("发送给 Code") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                minLines = 1,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPurple,
                    unfocusedBorderColor = BorderSoft,
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface
                )
            )
            FilledIconButton(onClick = viewModel::sendMessage, modifier = Modifier.size(50.dp)) {
                Icon(Icons.Default.Send, contentDescription = "发送")
            }
        }
    }
}

@Composable
fun ThreadStatusFilterBar(selected: ThreadStatusFilter, threads: List<ThreadSummary>, onSelect: (ThreadStatusFilter) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        items(ThreadStatusFilter.entries) { filter ->
            val count = threads.count { filter.matches(it) }
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text("${filter.label} $count") },
                shape = RoundedCornerShape(14.dp),
                colors = chipColors(selected == filter)
            )
        }
    }
}

@Composable
fun RenameThreadDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名会话", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("会话名称") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = formFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextMuted)
            }
        },
        containerColor = CardSurface,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
fun DeleteThreadDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除会话", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("确定删除这个会话吗？删除后将从手机端列表和电脑本地历史中移除。", color = TextPrimary)
                Text(title, color = TextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TrafficFocus)
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextMuted)
            }
        },
        containerColor = CardSurface,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
fun FilterBar(selected: EventFilter, events: List<ConversationEvent>, onSelect: (EventFilter) -> Unit) {
    val hasReasoning = events.any { it.kind == "reasoning_summary" && !it.text.isNullOrBlank() }
    val filters = EventFilter.entries.filter { it != EventFilter.Reasoning || hasReasoning }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filters) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(filter.label) },
                shape = RoundedCornerShape(14.dp),
                colors = chipColors(selected == filter)
            )
        }
    }
}

@Composable
fun TaskStats(events: List<ConversationEvent>) {
    val messages = events.count { it.kind == "user_message" || it.kind == "assistant_message" }
    val tools = events.count { it.kind == "tool_call" || it.kind == "tool_result" }
    val errors = events.count { it.kind == "error" }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatChip("$messages 条消息")
        StatChip("$tools 个工具")
        StatChip("$errors 个错误")
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun QuotaSummaryCard(events: List<ConversationEvent>) {
    val summary = quotaSummary(events) ?: return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFF),
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            summary.primary?.let { QuotaChip("5小时", it) }
            summary.secondary?.let { QuotaChip("7天", it) }
            summary.tokens?.let { QuotaChip("Token", it) }
        }
    }
}

@Composable
fun QuotaChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(96.dp)
            .background(CardSurface, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, BorderSoft), RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = TextMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun StatChip(text: String) {
    AssistChip(onClick = {}, label = { Text(text) })
}

@Composable
fun BrandMark() {
    Image(
        painter = painterResource(id = R.drawable.app_logo),
        contentDescription = "项目 logo",
        modifier = Modifier
            .size(38.dp)
            .padding(1.dp)
    )
}

@Composable
fun TrafficLightDot(status: String, focused: Boolean = false) {
    val isActive = !focused && status == "active"
    val transition = rememberInfiniteTransition(label = "traffic-light")
    val activeAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(700), repeatMode = RepeatMode.Reverse),
        label = "active-alpha"
    )
    val color = when {
        focused || status == "focus" || status == "systemError" || status == "error" -> TrafficFocus
        status == "active" -> TrafficActive
        else -> TrafficIdle
    }
    Box(
        modifier = Modifier
            .size(13.dp)
            .graphicsLayer(alpha = if (isActive) activeAlpha else 1f)
            .background(color, CircleShape)
            .border(BorderStroke(2.dp, Color.White), CircleShape)
    )
}

@Composable
fun StatusPill(text: String, status: String) {
    val normalized = when {
        status == "active" -> "active"
        status == "systemError" || status == "error" || status == "focus" -> "focus"
        else -> "idle"
    }
    val background = when (normalized) {
        "active" -> WarningSoft
        "focus" -> ErrorSoft
        else -> SuccessSoft
    }
    val foreground = when (normalized) {
        "active" -> TrafficActive
        "focus" -> TrafficFocus
        else -> TrafficIdle
    }
    Text(
        text = text,
        color = foreground,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(background, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
fun chipColors(selected: Boolean) = FilterChipDefaults.filterChipColors(
    containerColor = CardSurface,
    labelColor = TextPrimary,
    selectedContainerColor = BrandPurple,
    selectedLabelColor = Color.White,
    iconColor = TextMuted,
    selectedLeadingIconColor = Color.White
)

@Composable
fun formFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandPurple,
    unfocusedBorderColor = BorderSoft,
    focusedContainerColor = CardSurface,
    unfocusedContainerColor = CardSurface,
    focusedLabelColor = BrandPurple,
    cursorColor = BrandPurple
)

fun eventStyle(event: ConversationEvent): EventStyle {
    if (isCompletedEvent(event)) {
        return EventStyle(CompleteSoft, TrafficComplete, TrafficComplete, Icons.Default.CheckCircle)
    }
    return eventStyle(event.kind)
}

fun eventStyle(kind: String): EventStyle {
    return when (kind) {
        "assistant_message" -> EventStyle(BlueSoft, Color(0xFF7CB8FF), BrandPurple, Icons.Default.SmartToy)
        "user_message" -> EventStyle(Color(0xFFF4F7FB), Color(0xFFC8CEDB), TextPrimary, Icons.Default.Inbox)
        "tool_call", "tool_result" -> EventStyle(SuccessSoft, Color(0xFF6ED19D), Color(0xFF087443), Icons.Default.Build)
        "reasoning_summary" -> EventStyle(BrandPurpleSoft, BrandPurple, BrandPurple, Icons.Default.Storage)
        "error" -> EventStyle(ErrorSoft, Color(0xFFFF8A9A), TrafficFocus, Icons.Default.ErrorOutline)
        else -> EventStyle(Color(0xFFF7F8FC), Color(0xFFD8DCE8), TextMuted, Icons.Default.CheckCircle)
    }
}

data class EventStyle(
    val background: Color,
    val accent: Color,
    val iconColor: Color,
    val icon: ImageVector
)

fun statusLabel(thread: ThreadSummary): String {
    return when {
        thread.archived -> "归档"
        thread.status == "active" -> "进行中"
        thread.status == "systemError" || thread.status == "error" -> "错误"
        thread.status == "idle" -> "空闲"
        else -> "未知"
    }
}

fun statusColor(status: String): Color {
    return when (status) {
        "active" -> Color(0xFF245C4F)
        "systemError", "error" -> Color(0xFF9B2D20)
        "idle" -> Color(0xFF52615C)
        else -> Color(0xFF7C8782)
    }
}

@Composable
fun EventBubble(event: ConversationEvent) {
    val isUser = event.kind == "user_message"
    val isTool = event.kind == "tool_call" || event.kind == "tool_result"
    val text = event.text.orEmpty()
    val isLongMessage = !isTool && (text.length > 360 || text.count { it == '\n' } > 8)
    val canExpand = (isTool && text.isNotBlank()) || isLongMessage
    var expanded by remember(event.id) { mutableStateOf(false) }
    val style = eventStyle(event)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Surface(
            color = CardSurface,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, BorderSoft),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.9f else 0.98f)
        ) {
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .background(style.background)
                    .then(if (isTool && !expanded) Modifier.heightIn(min = 72.dp) else Modifier),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(style.accent)
                )
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(style.icon, contentDescription = null, tint = style.iconColor, modifier = Modifier.size(21.dp))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 10.dp, end = 12.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            displayEventTitle(event),
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isCompletedEvent(event)) {
                            CompletionBadge()
                        }
                    }
                    text.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                            maxLines = when {
                                expanded -> Int.MAX_VALUE
                                isTool -> 2
                                isLongMessage -> 9
                                else -> Int.MAX_VALUE
                            },
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (canExpand) {
                        TextButton(
                            onClick = { expanded = !expanded },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(if (expanded) "收起详情" else "查看完整详情", color = BrandPurple, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = TextMuted, modifier = Modifier.size(15.dp))
                        Text(formatShortTime(event.timestamp), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun CompletionBadge() {
    Text(
        "已完成",
        color = TrafficComplete,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(Color(0xFFDBEAFE), RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}

@Composable
fun BridgeDiagnostics(status: BridgeStatus) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("连接诊断", color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("电脑地址：${bestAddress(status)}", color = TextMuted)
            Text("Codex 服务：${if (status.codexAppServer.available) "可用" else "不可用"}", color = TextMuted)
            if (!status.codexAppServer.available) {
                status.codexAppServer.lastError
                    ?.let(::friendlyDiagnosticError)
                    ?.let { Text("原因：$it", color = TrafficFocus) }
            }
        }
    }
}

@Composable
fun DiagnosticList(events: List<AppServerDiagnosticEvent>) {
    if (events.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        events.forEach { event ->
            Text(
                "${event.directionLabel()} ${event.method ?: ""} - ${friendlyEventSummary(event)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF52615C)
            )
        }
    }
}

@Composable
fun ErrorText(error: String) {
    Text(
        text = friendlyDiagnosticError(error),
        color = TrafficFocus,
        modifier = Modifier
            .fillMaxWidth()
            .background(ErrorSoft, RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, Color(0xFFFFCAD4)), RoundedCornerShape(14.dp))
            .padding(12.dp)
    )
}

@Composable
fun OperationText(message: String) {
    Text(
        text = message,
        color = BrandPurple,
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandPurpleSoft, RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, Color(0xFFD8CCFF)), RoundedCornerShape(14.dp))
            .padding(12.dp)
    )
}

@Composable
fun WaitingReplyIndicator(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandPurpleSoft, RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, Color(0xFFD8CCFF)), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = BrandPurple
        )
        Text(message, color = BrandPurple, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyText(message: String) {
    Text(
        text = message,
        color = TextMuted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
    )
}

fun List<ConversationEvent>.filterFor(filter: EventFilter): List<ConversationEvent> {
    return when (filter) {
        EventFilter.All -> this
        EventFilter.Messages -> filter { it.kind == "user_message" || it.kind == "assistant_message" || isCompletedEvent(it) }
        EventFilter.Reasoning -> filter { it.kind == "reasoning_summary" }
        EventFilter.Tools -> filter { it.kind == "tool_call" || it.kind == "tool_result" }
        EventFilter.Status -> filter { it.kind == "status" }
        EventFilter.Error -> filter { it.kind == "error" }
    }
}

fun labelForKind(kind: String): String {
    return when (kind) {
        "user_message" -> "你"
        "assistant_message" -> "Code"
        "reasoning_summary" -> "思考"
        "tool_call" -> "工具调用"
        "tool_result" -> "工具结果"
        "status" -> "状态"
        "error" -> "错误"
        else -> kind
    }
}

fun displayEventTitle(event: ConversationEvent): String {
    val title = event.title ?: labelForKind(event.kind)
    return if (title.equals("Codex", ignoreCase = true)) "Code" else title
}

data class QuotaSummary(
    val primary: String?,
    val secondary: String?,
    val tokens: String?
)

fun quotaSummary(events: List<ConversationEvent>): QuotaSummary? {
    val root = findQuotaMetadata(events) ?: return null
    val rateLimits = root.objectAt("rate_limits") ?: root.objectAt("rateLimits")
    val tokenUsage = root.objectAt("info")?.objectAt("total_token_usage")
        ?: root.objectAt("info")?.objectAt("totalTokenUsage")
        ?: root.objectAt("total_token_usage")
        ?: root.objectAt("totalTokenUsage")
        ?: root.objectAt("usage")
        ?: root.objectAt("last_token_usage")
    val primary = rateLimits?.objectAt("primary")?.numberAt("used_percent")?.let(::formatPercent)
    val secondary = rateLimits?.objectAt("secondary")?.numberAt("used_percent")?.let(::formatPercent)
    val tokens = tokenUsage?.longAt("total_tokens")
        ?: tokenUsage?.longAt("totalTokens")
        ?: root.longAt("total_tokens")
        ?: root.longAt("totalTokens")
    if (primary == null && secondary == null && tokens == null) return null
    return QuotaSummary(
        primary = primary?.let { "$it%" },
        secondary = secondary?.let { "$it%" },
        tokens = tokens?.let(::compactNumber)
    )
}

fun findQuotaMetadata(events: List<ConversationEvent>): JsonObject? {
    return events.asReversed()
        .mapNotNull { it.metadata as? JsonObject }
        .firstOrNull {
            it.objectAt("rate_limits") != null ||
                it.objectAt("rateLimits") != null ||
                it.objectAt("info") != null ||
                it.objectAt("usage") != null ||
                it.objectAt("total_token_usage") != null ||
                it.objectAt("totalTokenUsage") != null ||
                it.longAt("total_tokens") != null ||
                it.longAt("totalTokens") != null
        }
}

fun JsonObject.objectAt(key: String): JsonObject? = this[key] as? JsonObject

fun JsonObject.numberAt(key: String): Double? = primitiveAt(key)?.doubleOrNull

fun JsonObject.longAt(key: String): Long? = primitiveAt(key)?.longOrNull

fun JsonObject.primitiveAt(key: String): JsonPrimitive? = this[key]?.primitiveOrNull()

fun JsonElement.primitiveOrNull(): JsonPrimitive? = runCatching { jsonPrimitive }.getOrNull()

fun formatPercent(value: Double): String {
    return if (value % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", value)
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}

fun compactNumber(value: Long): String {
    return when {
        value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format(Locale.US, "%.1fk", value / 1_000.0)
        else -> value.toString()
    }
}

fun isCompletedEvent(event: ConversationEvent): Boolean {
    val title = event.title.orEmpty()
    val text = event.text.orEmpty()
    return event.kind == "status" && (
        title.contains("任务完成") ||
            title.contains("Task complete", ignoreCase = true) ||
            title.contains("已完成") ||
            text.contains("已完成") ||
            text.contains("completed", ignoreCase = true)
        )
}

fun bestAddress(status: BridgeStatus?): String {
    if (status == null) return "-"
    val address = status.addresses.firstOrNull { it.startsWith("192.168.") } ?: status.addresses.firstOrNull() ?: status.host
    return "http://$address:${status.port}"
}

fun AppServerDiagnosticEvent.directionLabel(): String {
    return when (direction) {
        "stderr" -> "日志"
        "notification" -> "通知"
        "request" -> "请求"
        "response" -> "响应"
        "error" -> "错误"
        else -> "事件"
    }
}

fun friendlyDiagnosticError(error: String): String {
    return when {
        error.contains("Unable to resolve host \"pair\"", ignoreCase = true) ->
            "电脑地址为空或格式不正确。模拟器请填：http://10.0.2.2:4518"
        error.contains("Unable to resolve host", ignoreCase = true) ->
            "无法解析这个电脑地址，请检查地址是否填写正确。模拟器请填：http://10.0.2.2:4518"
        error.contains("Failed to connect", ignoreCase = true) ||
            error.contains("Connection refused", ignoreCase = true) ->
            "连接不上电脑端 Bridge。请确认电脑端页面已启动，地址和端口填写正确。"
        error.contains("timeout", ignoreCase = true) ||
            error.contains("timed out", ignoreCase = true) ->
            "连接超时。请确认手机和电脑在同一网络，或改用 Tailscale / ZeroTier 地址。"
        error.contains("Pairing token is invalid or expired", ignoreCase = true) ->
            "配对码不正确或已经过期，请在电脑端页面刷新后重新获取配对码。"
        error.contains("A valid paired device token is required", ignoreCase = true) ->
            "设备登录状态已失效，请断开后重新配对。"
        error.contains("Message text is required", ignoreCase = true) ->
            "请输入要发送的内容。"
        (error.contains("Invalid request", ignoreCase = true) &&
            (error.contains("turnId", ignoreCase = true) || error.contains("turnld", ignoreCase = true))) ||
            error.contains("missing field", ignoreCase = true) ->
            "当前会话没有正在运行的 Codex 回复，无法中断。"
        error.contains("Java heap space", ignoreCase = true) ->
            "电脑端之前编译时内存不足。当前服务如果显示可用，这只是历史错误。"
        error.contains("Gradle requires JVM 17", ignoreCase = true) ->
            "电脑端之前使用了过旧的 Java 版本。请使用 Android Studio 自带的 JDK。"
        else -> error
    }
}

fun friendlyEventSummary(event: AppServerDiagnosticEvent): String {
    val summary = event.summary
    return when {
        summary.contains("assistant delta", ignoreCase = true) -> "收到 Codex 回复片段"
        summary.contains("item/completed", ignoreCase = true) -> "一条事件已完成"
        summary.contains("turn completed", ignoreCase = true) -> "本轮已完成"
        summary.contains("thread/tokenUsage/updated", ignoreCase = true) -> "用量已更新"
        summary.contains("account/rateLimits/updated", ignoreCase = true) -> "账户额度已更新"
        summary.contains("turn/diff/updated", ignoreCase = true) -> "变更预览已更新"
        summary.contains("thread/status/changed", ignoreCase = true) -> "会话状态已更新"
        else -> summary
    }
}

fun formatShortTime(value: String): String {
    return value.replace('T', ' ').substringBeforeLast('.').substringBefore('+').substringBefore('Z')
}
