package com.tfg.kaifit_pal.kaimodel

import org.json.JSONException
import org.json.JSONObject

class ChatHistoryManager {
    public val chatHistory: MutableList<JSONObject> = ArrayList()

    fun getChatHistory(): List<JSONObject> {
        return chatHistory
    }

    fun addUserMessageToHistory(message: String) {
        chatHistory.add(createMessage("user", message))
    }

    fun addAssistantMessageToHistory(message: String) {
        chatHistory.add(createMessage("assistant", message))
    }

    private fun createMessage(role: String, content: String): JSONObject {
        val message = JSONObject()
        try {
            message.put("role", role)
            message.put("content", content)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
        return message
    }
}