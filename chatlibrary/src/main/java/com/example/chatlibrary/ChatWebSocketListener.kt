package com.example.chatlibrary

import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

internal class ChatWebSocketListener(private val messageCallback: (String, Boolean) -> Unit) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("ChatWebSocket", "Connection Opened")

        messageCallback("Connected to server.", false)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d("ChatWebSocket", "Receiving text: $text")
        messageCallback(text, false)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        Log.d("ChatWebSocket", "Receiving bytes: ${bytes.hex()}")
        messageCallback("Received binary data: ${bytes.hex()}", false)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("ChatWebSocket", "Closing: $code / $reason")
        messageCallback("Connection closing...", false)

    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("ChatWebSocket", "Closed: $code / $reason")
        messageCallback("Connection closed.", false)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("ChatWebSocket", "Error: " + t.message, t)
        messageCallback("Connection error: ${t.message}", false)
    }
}