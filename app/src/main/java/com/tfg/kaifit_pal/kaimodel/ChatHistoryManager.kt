package com.tfg.kaifit_pal.kaimodel;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatHistoryManager {
    private final List<JSONObject> chatHistory;

    public ChatHistoryManager() {
        this.chatHistory = new ArrayList<>();
    }

    public List<JSONObject> getChatHistory() {
        return chatHistory;
    }

    public void addUserMessageToHistory(String message) {
        chatHistory.add(createMessage("user", message));
    }

    public void addAssistantMessageToHistory(String message) {
        chatHistory.add(createMessage("assistant", message));
    }

    @NonNull
    private JSONObject createMessage(String role, String content) {
        JSONObject message = new JSONObject();
        try {
            message.put("role", role);
            message.put("content", content);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return message;
    }
}