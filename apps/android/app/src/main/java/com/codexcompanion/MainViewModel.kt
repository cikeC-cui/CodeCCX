package com.codexcompanion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codexcompanion.data.BridgeClient
import com.codexcompanion.data.BridgeStorage
import com.codexcompanion.model.BridgeStatus
import com.codexcompanion.model.ConversationEvent
import com.codexcompanion.model.PairQrPayload
import com.codexcompanion.model.SavedBridge
import com.codexcompanion.model.Screen
import com.codexcompanion.model.ThreadSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class UiState(
    val screen: Screen = Screen.Connect,
    val baseUrl: String = "",
    val remoteBaseUrl: String = "",
    val networkMode: NetworkMode = NetworkMode.Lan,
    val pairToken: String = "",
    val deviceName: String = "安卓手机",
    val savedBridge: SavedBridge? = null,
    val bridgeStatus: String = "未连接",
    val bridgeHealth: BridgeStatus? = null,
    val threads: List<ThreadSummary> = emptyList(),
    val searchQuery: String = "",
    val threadStatusFilter: ThreadStatusFilter = ThreadStatusFilter.All,
    val selectedThread: ThreadSummary? = null,
    val events: List<ConversationEvent> = emptyList(),
    val eventFilter: EventFilter = EventFilter.Messages,
    val inputText: String = "",
    val renameDialogVisible: Boolean = false,
    val renameTitle: String = "",
    val deleteDialogVisible: Boolean = false,
    val networkAddressDialogVisible: Boolean = false,
    val networkAddressDraft: String = "",
    val networkAddressSwitchAfterSave: Boolean = false,
    val operationResult: String? = null,
    val awaitingReply: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null
)

enum class NetworkMode(val id: String, val label: String) {
    Lan("lan", "局域网"),
    Remote("remote", "虚拟组网");

    companion object {
        fun fromId(value: String?): NetworkMode = entries.firstOrNull { it.id == value } ?: Lan
    }
}

enum class EventFilter(val label: String) {
    All("全部"),
    Messages("消息"),
    Reasoning("思考"),
    Tools("工具"),
    Status("状态"),
    Error("错误")
}

enum class ThreadStatusFilter(val label: String) {
    All("全部"),
    Active("进行中"),
    Idle("空闲"),
    Error("错误"),
    Archived("归档");

    fun matches(thread: ThreadSummary): Boolean {
        return when (this) {
            All -> true
            Active -> thread.status == "active"
            Idle -> thread.status == "idle" || thread.status == "unknown"
            Error -> thread.status == "systemError" || thread.status == "error"
            Archived -> thread.archived
        }
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = BridgeStorage(application)
    private val client = BridgeClient()
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private var socketJob: Job? = null
    private var refreshJob: Job? = null
    private var replyRefreshJob: Job? = null

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        storage.load()?.let { bridge ->
            _state.value = _state.value.copy(
                savedBridge = bridge,
                baseUrl = bridge.baseUrl,
                remoteBaseUrl = bridge.remoteUrl,
                networkMode = NetworkMode.fromId(bridge.networkMode),
                bridgeStatus = "已保存：${bridge.bridgeName}",
                screen = Screen.Threads
            )
            refreshStatus()
            refreshThreads()
            startAutoRefresh()
        }
    }

    fun updateBaseUrl(value: String) {
        _state.value = _state.value.copy(baseUrl = value.trim())
    }

    fun updateRemoteBaseUrl(value: String) {
        _state.value = _state.value.copy(remoteBaseUrl = value.trim())
    }

    fun updateNetworkAddressDraft(value: String) {
        _state.value = _state.value.copy(networkAddressDraft = value.trim())
    }

    fun updatePairToken(value: String) {
        _state.value = _state.value.copy(pairToken = value.trim())
    }

    fun updateDeviceName(value: String) {
        _state.value = _state.value.copy(deviceName = value)
    }

    fun updateInput(value: String) {
        _state.value = _state.value.copy(inputText = value)
    }

    fun updateRenameTitle(value: String) {
        _state.value = _state.value.copy(renameTitle = value)
    }

    fun showRenameDialog() {
        val thread = _state.value.selectedThread ?: return
        _state.value = _state.value.copy(renameDialogVisible = true, renameTitle = thread.title, error = null)
    }

    fun hideRenameDialog() {
        _state.value = _state.value.copy(renameDialogVisible = false, renameTitle = "")
    }

    fun showDeleteDialog() {
        if (_state.value.selectedThread == null) return
        _state.value = _state.value.copy(deleteDialogVisible = true, error = null)
    }

    fun hideDeleteDialog() {
        _state.value = _state.value.copy(deleteDialogVisible = false)
    }

    fun showNetworkAddressDialog(switchAfterSave: Boolean = false) {
        val current = _state.value.remoteBaseUrl.ifBlank { _state.value.savedBridge?.remoteUrl.orEmpty() }
        _state.value = _state.value.copy(
            networkAddressDialogVisible = true,
            networkAddressDraft = current,
            networkAddressSwitchAfterSave = switchAfterSave,
            error = null
        )
    }

    fun hideNetworkAddressDialog() {
        _state.value = _state.value.copy(
            networkAddressDialogVisible = false,
            networkAddressDraft = "",
            networkAddressSwitchAfterSave = false
        )
    }

    fun saveNetworkAddress() {
        val url = normalizedUrlOrNull(_state.value.networkAddressDraft)
        if (url == null) {
            clearNetworkAddress()
            return
        }
        val bridge = _state.value.savedBridge
        if (bridge != null) {
            val updatedBridge = bridge.copy(remoteUrl = url)
            storage.save(updatedBridge)
            _state.value = _state.value.copy(savedBridge = updatedBridge)
        }
        val shouldSwitch = _state.value.networkAddressSwitchAfterSave || _state.value.networkMode == NetworkMode.Remote
        _state.value = _state.value.copy(
            remoteBaseUrl = url,
            networkAddressDialogVisible = false,
            networkAddressDraft = "",
            networkAddressSwitchAfterSave = false,
            operationResult = "虚拟组网地址已保存"
        )
        if (shouldSwitch && bridge != null) {
            switchBridgeUrl(url, NetworkMode.Remote)
        }
    }

    fun clearNetworkAddress() {
        val bridge = _state.value.savedBridge
        val currentMode = _state.value.networkMode
        if (bridge != null) {
            val updatedBridge = bridge.copy(remoteUrl = "")
            storage.save(updatedBridge)
            _state.value = _state.value.copy(savedBridge = updatedBridge)
        }
        _state.value = _state.value.copy(
            remoteBaseUrl = "",
            networkAddressDialogVisible = false,
            networkAddressDraft = "",
            networkAddressSwitchAfterSave = false,
            operationResult = "虚拟组网地址已清空"
        )
        if (bridge != null && currentMode == NetworkMode.Remote) {
            val lanUrl = bestLanUrl(_state.value.bridgeHealth, bridge)
            if (lanUrl != null) {
                switchBridgeUrl(lanUrl, NetworkMode.Lan)
            }
        }
    }

    fun updateSearchQuery(value: String) {
        _state.value = _state.value.copy(searchQuery = value)
    }

    fun setEventFilter(filter: EventFilter) {
        _state.value = _state.value.copy(eventFilter = filter)
    }

    fun setThreadStatusFilter(filter: ThreadStatusFilter) {
        _state.value = _state.value.copy(threadStatusFilter = filter)
    }

    fun useLanNetwork() {
        val bridge = _state.value.savedBridge ?: return
        val lanUrl = bestLanUrl(_state.value.bridgeHealth, bridge) ?: run {
            _state.value = _state.value.copy(error = "没有可用的局域网地址。请确认手机和电脑在同一个 Wi-Fi，或改用虚拟组网地址。")
            return
        }
        switchBridgeUrl(lanUrl, NetworkMode.Lan)
    }

    fun useRemoteNetwork() {
        val bridge = _state.value.savedBridge ?: return
        if (normalizedUrlOrNull(_state.value.remoteBaseUrl) == null && normalizedUrlOrNull(bridge.remoteUrl) == null) {
            _state.value = _state.value.copy(error = "虚拟组网不可用：请先填写 Tailscale 或 ZeroTier 地址。")
            showNetworkAddressDialog(switchAfterSave = true)
            return
        }
        val remoteUrl = normalizedUrlOrNull(_state.value.remoteBaseUrl)
            ?: normalizedUrlOrNull(bridge.remoteUrl)
            ?: run {
                _state.value = _state.value.copy(error = "虚拟组网不可用：请先填写 Tailscale 或 ZeroTier 地址。")
                return
            }
        switchBridgeUrl(remoteUrl, NetworkMode.Remote)
    }

    fun applyQrPayload(raw: String) {
        runCatching {
            val payload = json.decodeFromString<PairQrPayload>(raw)
            val firstAddress = preferredLanAddress(payload.addresses)
            val url = firstAddress?.let { "http://$it:${payload.port}" }.orEmpty()
            _state.value = _state.value.copy(
                baseUrl = url,
                remoteBaseUrl = payload.publicUrl.orEmpty(),
                pairToken = payload.pairToken,
                bridgeStatus = payload.bridgeName,
                bridgeHealth = null,
                error = null
            )
        }.onFailure {
            _state.value = _state.value.copy(error = "这个二维码不是 Code 伴侣的配对二维码。")
        }
    }

    fun testConnection() {
        val baseUrl = normalizedBaseUrlOrNull()
        if (baseUrl == null) {
            _state.value = _state.value.copy(error = "请先填写电脑地址。模拟器请填：http://10.0.2.2:4518")
            return
        }
        launchBusy {
            val status = client.health(baseUrl)
            _state.value = _state.value.copy(
                baseUrl = baseUrl,
                bridgeStatus = "${status.bridgeName} - ${status.addresses.joinToString()}",
                bridgeHealth = status,
                error = null
            )
        }
    }

    fun refreshStatus(showError: Boolean = true) {
        val baseUrl = _state.value.savedBridge?.baseUrl ?: normalizedBaseUrlOrNull() ?: return
        viewModelScope.launch {
            runCatching { client.health(baseUrl) }
                .onSuccess { status ->
                    _state.value = _state.value.copy(
                        baseUrl = baseUrl,
                        bridgeStatus = status.bridgeName,
                        bridgeHealth = status,
                        remoteBaseUrl = _state.value.remoteBaseUrl.ifBlank { status.publicUrl.orEmpty() },
                        error = null
                    )
                }
                .onFailure {
                    if (showError) _state.value = _state.value.copy(error = it.message)
                }
        }
    }

    fun pair() {
        val baseUrl = normalizedBaseUrlOrNull()
        if (baseUrl == null) {
            _state.value = _state.value.copy(error = "请先填写电脑地址。模拟器请填：http://10.0.2.2:4518")
            return
        }
        val token = _state.value.pairToken
        if (token.isBlank()) {
            _state.value = _state.value.copy(error = "请先填写配对码。配对码在电脑端 Bridge 页面或启动窗口里显示。")
            return
        }
        val deviceName = _state.value.deviceName
        launchBusy {
            val response = client.pair(baseUrl, token, deviceName)
            val mode = modeForUrl(baseUrl)
            val remoteUrl = normalizedUrlOrNull(_state.value.remoteBaseUrl).orEmpty()
            val bridge = SavedBridge(
                baseUrl = baseUrl,
                bridgeName = response.bridgeName,
                deviceId = response.deviceId,
                authToken = response.authToken,
                networkMode = mode.id,
                lanUrl = if (mode == NetworkMode.Lan) baseUrl else "",
                remoteUrl = if (mode == NetworkMode.Remote) baseUrl else remoteUrl
            )
            storage.save(bridge)
            _state.value = _state.value.copy(
                savedBridge = bridge,
                networkMode = mode,
                remoteBaseUrl = bridge.remoteUrl,
                bridgeStatus = "已连接：${response.bridgeName}",
                screen = Screen.Threads,
                error = null
            )
            startAutoRefresh()
            refreshStatus()
            refreshThreads()
        }
    }

    fun refreshThreads(showBusy: Boolean = false) {
        val bridge = _state.value.savedBridge ?: return
        launchRequest(showBusy) {
            val threads = client.threads(bridge.baseUrl, bridge.authToken)
                .sortedByDescending { it.updatedAt }
            val selectedId = _state.value.selectedThread?.id
            val updatedSelected = selectedId?.let { id -> threads.firstOrNull { it.id == id } } ?: _state.value.selectedThread
            _state.value = _state.value.copy(
                threads = threads,
                selectedThread = updatedSelected,
                screen = if (_state.value.selectedThread == null) Screen.Threads else _state.value.screen,
                error = null
            )
        }
    }

    fun openThread(thread: ThreadSummary) {
        val bridge = _state.value.savedBridge ?: return
        socketJob?.cancel()
        _state.value = _state.value.copy(
            selectedThread = thread,
            events = emptyList(),
            eventFilter = EventFilter.Messages,
            renameDialogVisible = false,
            deleteDialogVisible = false,
            operationResult = null,
            screen = Screen.ThreadDetail
        )
        socketJob = viewModelScope.launch {
            try {
                val snapshot = client.events(bridge.baseUrl, bridge.authToken, thread.id)
                _state.value = _state.value.copy(selectedThread = snapshot.thread, events = snapshot.events.sortedBy { it.timestamp }, error = null)
                client.watchThread(bridge.baseUrl, bridge.authToken, thread.id).collect { envelope ->
                    envelope.thread?.let { updatedThread ->
                        _state.value = _state.value.copy(selectedThread = updatedThread, events = envelope.events.sortedBy { it.timestamp })
                    }
                    envelope.message?.let { message ->
                        _state.value = _state.value.copy(error = message)
                    }
                }
            } catch (_: CancellationException) {
            } catch (error: Throwable) {
                if (!isExpectedCancellation(error)) {
                    _state.value = _state.value.copy(error = error.message)
                }
            }
        }
    }

    fun sendMessage() {
        val bridge = _state.value.savedBridge ?: return
        val thread = _state.value.selectedThread ?: return
        val text = _state.value.inputText.trim()
        if (text.isEmpty()) return
        val assistantCountBeforeSend = _state.value.events.count { it.kind == "assistant_message" }
        val localEvent = ConversationEvent(
            id = "local-${System.currentTimeMillis()}",
            threadId = thread.id,
            timestamp = currentTimestamp(),
            kind = "user_message",
            title = "你",
            text = text
        )
        _state.value = _state.value.copy(
            inputText = "",
            events = (_state.value.events + localEvent).sortedBy { it.timestamp },
            operationResult = "已发送，正在等待 Code 回复...",
            awaitingReply = true,
            error = null
        )
        launchRequest(showBusy = false) {
            val response = client.send(bridge.baseUrl, bridge.authToken, thread.id, text)
            if (response.accepted) {
                waitForReply(thread.id, assistantCountBeforeSend)
            } else {
                _state.value = _state.value.copy(
                    events = _state.value.events.filterNot { it.id == localEvent.id },
                    operationResult = response.message,
                    awaitingReply = false,
                    error = response.message
                )
            }
        }
    }

    fun interruptThread() {
        val bridge = _state.value.savedBridge ?: return
        val thread = _state.value.selectedThread ?: return
        launchBusy {
            val response = client.interrupt(bridge.baseUrl, bridge.authToken, thread.id)
            _state.value = _state.value.copy(operationResult = response.message)
        }
    }

    fun renameSelectedThread() {
        val bridge = _state.value.savedBridge ?: return
        val thread = _state.value.selectedThread ?: return
        val title = _state.value.renameTitle.trim()
        if (title.isEmpty()) {
            _state.value = _state.value.copy(error = "请输入新的会话名称。")
            return
        }
        launchBusy {
            val response = client.renameThread(bridge.baseUrl, bridge.authToken, thread.id, title)
            val updatedThread = response.thread
            _state.value = _state.value.copy(
                renameDialogVisible = false,
                renameTitle = "",
                selectedThread = updatedThread,
                threads = _state.value.threads.map { item -> if (item.id == updatedThread.id) updatedThread else item },
                operationResult = "会话名称已更新。",
                error = null
            )
            refreshThreads(showBusy = false)
        }
    }

    fun deleteSelectedThread() {
        val bridge = _state.value.savedBridge ?: return
        val thread = _state.value.selectedThread ?: return
        launchBusy {
            val response = client.deleteThread(bridge.baseUrl, bridge.authToken, thread.id)
            if (response.deleted) {
                socketJob?.cancel()
                _state.value = _state.value.copy(
                    deleteDialogVisible = false,
                    renameDialogVisible = false,
                    renameTitle = "",
                    selectedThread = null,
                    events = emptyList(),
                    threads = _state.value.threads.filterNot { it.id == response.threadId },
                    screen = Screen.Threads,
                    operationResult = null,
                    error = null
                )
                refreshThreads(showBusy = false)
            }
        }
    }

    fun disconnect() {
        socketJob?.cancel()
        refreshJob?.cancel()
        replyRefreshJob?.cancel()
        storage.clear()
        _state.value = UiState()
    }

    fun openNetworkSettings() {
        if (_state.value.savedBridge == null) return
        _state.value = _state.value.copy(
            screen = Screen.NetworkSettings,
            operationResult = null,
            error = null
        )
    }

    fun backToThreads() {
        socketJob?.cancel()
        _state.value = _state.value.copy(
            screen = Screen.Threads,
            selectedThread = null,
            events = emptyList(),
            renameDialogVisible = false,
            deleteDialogVisible = false
        )
    }

    private fun normalizedBaseUrl(): String {
        val value = _state.value.baseUrl.trim().trimEnd('/')
        return if (value.startsWith("http://") || value.startsWith("https://")) value else "http://$value"
    }

    private fun normalizedBaseUrlOrNull(): String? {
        val value = _state.value.baseUrl.trim()
        if (value.isBlank()) return null
        return normalizedBaseUrl()
    }

    private fun normalizedUrlOrNull(value: String): String? {
        val trimmed = value.trim().trimEnd('/')
        if (trimmed.isBlank()) return null
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
    }

    private fun switchBridgeUrl(baseUrl: String, mode: NetworkMode) {
        val bridge = _state.value.savedBridge ?: return
        socketJob?.cancel()
        val lanUrl = if (mode == NetworkMode.Lan) baseUrl else bridge.lanUrl
        val remoteUrl = if (mode == NetworkMode.Remote) baseUrl else normalizedUrlOrNull(_state.value.remoteBaseUrl).orEmpty()
        val updatedBridge = bridge.copy(
            baseUrl = baseUrl,
            networkMode = mode.id,
            lanUrl = lanUrl,
            remoteUrl = remoteUrl
        )
        storage.save(updatedBridge)
        _state.value = _state.value.copy(
            savedBridge = updatedBridge,
            baseUrl = baseUrl,
            remoteBaseUrl = remoteUrl,
            networkMode = mode,
            selectedThread = null,
            events = emptyList(),
            error = null,
            operationResult = "已切换到${mode.label}"
        )
        refreshStatus()
        refreshThreads(showBusy = true)
    }

    private fun bestLanUrl(status: BridgeStatus?, bridge: SavedBridge): String? {
        val address = preferredLanAddress(status?.addresses.orEmpty())
        return address?.let { "http://$it:${status?.port ?: 4518}" }
            ?: normalizedUrlOrNull(bridge.lanUrl)
            ?: if (modeForUrl(bridge.baseUrl) == NetworkMode.Lan) bridge.baseUrl else null
    }

    private fun preferredLanAddress(addresses: List<String>): String? {
        return addresses.firstOrNull { it.startsWith("192.168.") }
            ?: addresses.firstOrNull { it.startsWith("10.") }
            ?: addresses.firstOrNull { address ->
                val second = address.substringAfter("172.", "").substringBefore(".").toIntOrNull()
                address.startsWith("172.") && second != null && second in 16..31
            }
            ?: addresses.firstOrNull()
    }

    private fun modeForUrl(url: String): NetworkMode {
        val host = url
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/")
            .substringBefore(":")
        val isLan = host == "localhost" ||
            host == "127.0.0.1" ||
            host == "10.0.2.2" ||
            host.startsWith("192.168.") ||
            host.startsWith("10.") ||
            host.let {
                val second = it.substringAfter("172.", "").substringBefore(".").toIntOrNull()
                it.startsWith("172.") && second != null && second in 16..31
            }
        return if (isLan) NetworkMode.Lan else NetworkMode.Remote
    }

    private fun launchBusy(block: suspend () -> Unit) {
        launchRequest(showBusy = true, block = block)
    }

    private fun launchRequest(showBusy: Boolean, block: suspend () -> Unit) {
        viewModelScope.launch {
            if (showBusy) _state.value = _state.value.copy(busy = true, error = null)
            try {
                block()
            } catch (_: CancellationException) {
            } catch (error: Throwable) {
                if (!isExpectedCancellation(error)) {
                    _state.value = _state.value.copy(error = error.message, awaitingReply = false)
                }
            } finally {
                if (showBusy) _state.value = _state.value.copy(busy = false)
            }
        }
    }

    private fun startAutoRefresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(4_000)
                val bridge = _state.value.savedBridge ?: continue
                runCatching { client.health(bridge.baseUrl) }
                    .onSuccess { status ->
                        _state.value = _state.value.copy(
                            bridgeHealth = status,
                            bridgeStatus = status.bridgeName,
                            remoteBaseUrl = _state.value.remoteBaseUrl.ifBlank { status.publicUrl.orEmpty() }
                        )
                    }
                runCatching { client.threads(bridge.baseUrl, bridge.authToken).sortedByDescending { it.updatedAt } }
                    .onSuccess { threads ->
                        val selectedId = _state.value.selectedThread?.id
                        val selectedFromList = selectedId?.let { id -> threads.firstOrNull { it.id == id } }
                        _state.value = _state.value.copy(
                            threads = threads,
                            selectedThread = selectedFromList ?: _state.value.selectedThread
                        )
                    }
                val thread = _state.value.selectedThread
                if (_state.value.screen == Screen.ThreadDetail && thread != null) {
                    runCatching { client.events(bridge.baseUrl, bridge.authToken, thread.id) }
                        .onSuccess { snapshot ->
                            _state.value = _state.value.copy(
                                selectedThread = snapshot.thread,
                                events = snapshot.events.sortedBy { it.timestamp }
                            )
                        }
                }
            }
        }
    }

    private suspend fun refreshSelectedThread(showBusy: Boolean) {
        val bridge = _state.value.savedBridge ?: return
        val thread = _state.value.selectedThread ?: return
        val block: suspend () -> Unit = {
            val snapshot = client.events(bridge.baseUrl, bridge.authToken, thread.id)
            _state.value = _state.value.copy(
                selectedThread = snapshot.thread,
                events = snapshot.events.sortedBy { it.timestamp },
                error = null
            )
        }
        if (showBusy) {
            block()
        } else {
            runCatching { block() }
        }
    }

    private fun waitForReply(threadId: String, assistantCountBeforeSend: Int) {
        replyRefreshJob?.cancel()
        replyRefreshJob = viewModelScope.launch {
            repeat(90) {
                delay(700)
                val selected = _state.value.selectedThread ?: return@launch
                if (selected.id != threadId) return@launch
                refreshSelectedThread(showBusy = false)
                val assistantCount = _state.value.events.count { it.kind == "assistant_message" }
                if (assistantCount > assistantCountBeforeSend) {
                    _state.value = _state.value.copy(operationResult = "已收到 Code 回复。", awaitingReply = false)
                    return@launch
                }
            }
            _state.value = _state.value.copy(operationResult = "还没等到完整回复，页面会继续自动刷新。", awaitingReply = false)
        }
    }

    private fun isExpectedCancellation(error: Throwable): Boolean {
        val message = error.message.orEmpty()
        return error is CancellationException ||
            message.contains("cancelled", ignoreCase = true) ||
            message.contains("canceled", ignoreCase = true) ||
            message.contains("socket closed", ignoreCase = true)
    }

    private fun currentTimestamp(): String {
        val now = java.util.Date()
        return java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.US).format(now)
    }
}
