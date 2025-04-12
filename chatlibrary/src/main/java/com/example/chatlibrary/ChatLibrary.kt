package com.example.chatlibrary

import android.content.Context
import android.content.Intent

object ChatLibrary {

    fun start(context: Context) {
        val intent = Intent(context, ChatActivity::class.java)
        context.startActivity(intent)
    }
}