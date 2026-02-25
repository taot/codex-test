package com.opencode.pycharm.acp

import com.intellij.openapi.diagnostic.Logger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

class AcpClient(
    private val endpoint: String,
    private val tokenProvider: () -> String
) {
    private val logger = Logger.getInstance(AcpClient::class.java)
    private val httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    @Volatile
    private var socket: WebSocket? = null

    fun connect(onMessage: (String) -> Unit, onClosed: () -> Unit) {
        if (socket != null) return
        val listener = object : WebSocket.Listener {
            private val buffer = StringBuilder()

            override fun onOpen(webSocket: WebSocket) {
                logger.info("ACP socket opened")
                socket = webSocket
                WebSocket.Listener.super.onOpen(webSocket)
            }

            override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*> {
                buffer.append(data)
                if (last) {
                    val payload = buffer.toString()
                    buffer.setLength(0)
                    onMessage(payload)
                }
                return CompletableFuture.completedFuture(null)
            }

            override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*> {
                logger.info("ACP socket closed: $statusCode $reason")
                socket = null
                onClosed()
                return CompletableFuture.completedFuture(null)
            }

            override fun onError(webSocket: WebSocket, error: Throwable) {
                logger.warn("ACP socket error", error)
            }
        }

        val token = tokenProvider()
        httpClient.newWebSocketBuilder()
            .header("Authorization", "Bearer $token")
            .buildAsync(URI.create(endpoint), listener)
            .exceptionally {
                logger.warn("Failed to connect ACP socket", it)
                null
            }
    }

    fun disconnect() {
        socket?.sendClose(WebSocket.NORMAL_CLOSURE, "Plugin shutdown")
        socket = null
    }

    fun sendRequest(method: String, params: Map<String, Any?>): String {
        val id = UUID.randomUUID().toString()
        val payload = """
            {"jsonrpc":"2.0","id":"$id","method":"$method","params":${encodeObject(params)}}
        """.trimIndent().replace("\n", "")
        socket?.sendText(payload, true)
        return id
    }

    fun sendNotification(method: String, params: Map<String, Any?>) {
        val payload = """
            {"jsonrpc":"2.0","method":"$method","params":${encodeObject(params)}}
        """.trimIndent().replace("\n", "")
        socket?.sendText(payload, true)
    }

    private fun encodeObject(map: Map<String, Any?>): String {
        return map.entries.joinToString(prefix = "{", postfix = "}") { (k, v) ->
            "\"${escape(k)}\":${encodeValue(v)}"
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun encodeValue(value: Any?): String = when (value) {
        null -> "null"
        is String -> "\"${escape(value)}\""
        is Number, is Boolean -> value.toString()
        is Map<*, *> -> encodeObject(value as Map<String, Any?>)
        is List<*> -> value.joinToString(prefix = "[", postfix = "]") { encodeValue(it) }
        else -> "\"${escape(value.toString())}\""
    }

    private fun escape(raw: String): String = raw
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}
