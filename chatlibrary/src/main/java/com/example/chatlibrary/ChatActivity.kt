package com.example.chatlibrary

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import java.util.concurrent.TimeUnit

internal class ChatActivity : AppCompatActivity() {

    private lateinit var client: OkHttpClient
    private var webSocket: WebSocket? = null
    private lateinit var chatAdapter: ChatAdapter
    private val messagesList = mutableListOf<ChatMessage>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button

    private val webSocketUrl = "wss://echo.websocket.org/"
    private val NORMAL_CLOSURE_STATUS = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        recyclerView = findViewById(R.id.recyclerViewChat)
        messageInput = findViewById(R.id.editTextMessage)
        sendButton = findViewById(R.id.buttonSend)

        setupRecyclerView()
        setupWebSocket()
        setupSendButton()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messagesList)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupWebSocket() {
        client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(webSocketUrl).build()
        val listener = ChatWebSocketListener { message, messageIsSentByUser ->
            runOnUiThread {
                addMessageToList(message, messageIsSentByUser)
            }
        }

        webSocket = client.newWebSocket(request, listener)

    }


    private val hexByteWithPrefixRegex = "^0x([0-9A-F]{2})$".toRegex(RegexOption.IGNORE_CASE)

    private fun setupSendButton() {
        sendButton.setOnClickListener {
            val inputText = messageInput.text.toString().trim()
            if (inputText.isNotEmpty()) {

                val matchResult = hexByteWithPrefixRegex.matchEntire(inputText)

                if (matchResult != null) {
                    val hexValue = matchResult.groupValues[1]
                    if (hexValue.equals("CB", ignoreCase = true) || hexValue.equals("BF", ignoreCase = true)) {
                        try {
                            val byteToSend = hexValue.toInt(16).toByte()
                            val byteStringToSend = okio.ByteString.of(byteToSend)
                            webSocket?.send(byteStringToSend)
                            Log.d("ChatActivity", "Matched '${inputText}', sent byte: 0x${hexValue.uppercase()}")
                            addMessageToList("Sent byte: 0x${hexValue.uppercase()}", true)
                            messageInput.setText("")
                        } catch (e: NumberFormatException) {
                            Log.e("ChatActivity", "Error converting hex '$hexValue' from matched regex. Sending as text.", e)
                            sendAsText(inputText)
                        }
                    } else {
                        Log.d("ChatActivity", "Input '${inputText}' matches 0xHH pattern but not 0xCB/0xBF. Sending as text.")
                        sendAsText(inputText)
                    }
                } else {
                    Log.d("ChatActivity", "Input '${inputText}' doesn't match 0xHH pattern. Sending as text.")
                    sendAsText(inputText)
                }
            }
        }
    }

    private fun sendAsText(text: String) {
        webSocket?.send(text)
        addMessageToList(text, true)
        messageInput.setText("")
    }

    private fun addMessageToList(text: String, isSent: Boolean) {
        val message = ChatMessage(text, isSent)
        messagesList.add(message)
        chatAdapter.notifyItemInserted(messagesList.size - 1)
        recyclerView.scrollToPosition(messagesList.size - 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ChatActivity", "onDestroy: Closing WebSocket")
        webSocket?.close(NORMAL_CLOSURE_STATUS, "Activity Destroyed")
        client.dispatcher.executorService.shutdown()
    }
}