package com.codexcompanion.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class BridgeStatus(
    val bridgeName: String,
    val version: String,
    val host: String,
    val port: Int,
    val addresses: List<String> = emptyList(),
    val publicUrl: String? = null,
    val transports: List<String> = emptyList(),
    val codexAppServer: CodexAppServerStatus
)

@Serializable
data class CodexAppServerStatus(
    val enabled: Boolean,
    val available: Boolean,
    val lastError: String? = null,
    val pendingRequests: Int? = null,
    val initialized: Boolean? = null,
    val recentEvents: List<AppServerDiagnosticEvent> = emptyList()
)

@Serializable
data class AppServerDiagnosticEvent(
    val at: String,
    val direction: String,
    val method: String? = null,
    val id: JsonElement? = null,
    val summary: String
)

@Serializable
data class PairRequest(
    val pairToken: String,
    val deviceName: String
)

@Serializable
data class PairResponse(
    val deviceId: String,
    val authToken: String,
    val bridgeName: String
)

@Serializable
data class PairQrPayload(
    val type: String,
    val bridgeName: String,
    val addresses: List<String> = emptyList(),
    val port: Int,
    val publicUrl: String? = null,
    val pairToken: String,
    val expiresAt: String,
    val transports: List<String> = emptyList()
)

@Serializable
data class ThreadSummary(
    val id: String,
    val title: String = "未命名会话",
    val preview: String = "",
    val updatedAt: String,
    val createdAt: String? = null,
    val cwd: String? = null,
    val model: String? = null,
    val archived: Boolean = false,
    val status: String = "unknown"
)

@Serializable
data class ConversationEvent(
    val id: String,
    val threadId: String,
    val timestamp: String,
    val kind: String,
    val title: String? = null,
    val text: String? = null,
    val metadata: JsonElement? = null
)

@Serializable
data class ThreadEventsResponse(
    val thread: ThreadSummary,
    val events: List<ConversationEvent>
)

@Serializable
data class SendMessageRequest(
    val text: String
)

@Serializable
data class RenameThreadRequest(
    val title: String
)

@Serializable
data class RenameThreadResponse(
    val thread: ThreadSummary
)

@Serializable
data class DeleteThreadResponse(
    val deleted: Boolean,
    val threadId: String
)

@Serializable
data class SendMessageResponse(
    val accepted: Boolean,
    val turnId: String? = null,
    val message: String
)

@Serializable
data class SocketSnapshot(
    val type: String,
    val thread: ThreadSummary? = null,
    val events: List<ConversationEvent> = emptyList(),
    val message: String? = null
)

@Serializable
data class ApiErrorBody(
    val error: ApiError
)

@Serializable
data class ApiError(
    val code: String,
    val message: String
)

data class SavedBridge(
    val baseUrl: String,
    val bridgeName: String,
    val deviceId: String,
    val authToken: String,
    val networkMode: String = "lan",
    val lanUrl: String = baseUrl,
    val remoteUrl: String = ""
)

enum class Screen {
    Connect,
    Threads,
    NetworkSettings,
    ThreadDetail
}
