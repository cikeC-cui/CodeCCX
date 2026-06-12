package com.codexcompanion.data

import com.codexcompanion.model.BridgeStatus
import com.codexcompanion.model.ApiErrorBody
import com.codexcompanion.model.PairRequest
import com.codexcompanion.model.PairResponse
import com.codexcompanion.model.DeleteThreadResponse
import com.codexcompanion.model.RenameThreadRequest
import com.codexcompanion.model.RenameThreadResponse
import com.codexcompanion.model.SendMessageRequest
import com.codexcompanion.model.SendMessageResponse
import com.codexcompanion.model.SocketSnapshot
import com.codexcompanion.model.ThreadEventsResponse
import com.codexcompanion.model.ThreadSummary
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BridgeClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val mediaType = "application/json".toMediaType()

    suspend fun health(baseUrl: String): BridgeStatus {
        return get("$baseUrl/health")
    }

    suspend fun pair(baseUrl: String, pairToken: String, deviceName: String): PairResponse {
        val body = json.encodeToString(PairRequest(pairToken, deviceName)).toRequestBody(mediaType)
        return post("$baseUrl/pair", body, null)
    }

    suspend fun threads(baseUrl: String, token: String): List<ThreadSummary> {
        return get("$baseUrl/threads", token)
    }

    suspend fun events(baseUrl: String, token: String, threadId: String): ThreadEventsResponse {
        return get("$baseUrl/threads/$threadId/events", token)
    }

    suspend fun send(baseUrl: String, token: String, threadId: String, text: String): SendMessageResponse {
        val body = json.encodeToString(SendMessageRequest(text)).toRequestBody(mediaType)
        return post("$baseUrl/threads/$threadId/send", body, token)
    }

    suspend fun renameThread(baseUrl: String, token: String, threadId: String, title: String): RenameThreadResponse {
        val body = json.encodeToString(RenameThreadRequest(title)).toRequestBody(mediaType)
        return post("$baseUrl/threads/$threadId/rename", body, token)
    }

    suspend fun deleteThread(baseUrl: String, token: String, threadId: String): DeleteThreadResponse {
        val request = Request.Builder().url("$baseUrl/threads/$threadId").delete().apply {
            header("Authorization", "Bearer $token")
        }.build()
        return execute(request)
    }

    suspend fun interrupt(baseUrl: String, token: String, threadId: String): SendMessageResponse {
        return post("$baseUrl/threads/$threadId/interrupt", "{}".toRequestBody(mediaType), token)
    }

    fun watchThread(baseUrl: String, token: String, threadId: String): Flow<SocketSnapshot> = callbackFlow {
        val wsUrl = baseUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://") + "/threads/$threadId/events?token=$token"
        val request = Request.Builder().url(wsUrl).build()
        val socket = http.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching { json.decodeFromString<SocketSnapshot>(text) }
                    .onSuccess { trySend(it) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(t)
            }
        })
        awaitClose { socket.close(1000, "Screen closed") }
    }

    private suspend inline fun <reified T> get(url: String, token: String? = null): T {
        val request = Request.Builder().url(url).apply {
            token?.let { header("Authorization", "Bearer $it") }
        }.build()
        return execute(request)
    }

    private suspend inline fun <reified T> post(url: String, body: okhttp3.RequestBody, token: String?): T {
        val request = Request.Builder().url(url).post(body).apply {
            token?.let { header("Authorization", "Bearer $it") }
        }.build()
        return execute(request)
    }

    private suspend inline fun <reified T> execute(request: Request): T {
        val raw = suspendCancellableCoroutine<String> { continuation ->
            val call = http.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: okhttp3.Call, response: Response) {
                    response.use {
                        if (!continuation.isActive) return
                        val body = it.body?.string().orEmpty()
                        if (!it.isSuccessful) {
                            val message = runCatching {
                                json.decodeFromString<ApiErrorBody>(body).error.message
                            }.getOrNull()
                            continuation.resumeWithException(IOException(message ?: body.ifBlank { "HTTP ${it.code}" }))
                        } else {
                            continuation.resume(body)
                        }
                    }
                }
            })
        }
        return json.decodeFromString(raw)
    }
}
