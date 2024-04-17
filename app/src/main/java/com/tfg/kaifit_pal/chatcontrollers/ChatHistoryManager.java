package com.tfg.kaifit_pal.chatcontrollers;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This class manages the chat history between the user and the assistant.
 * It stores the chat history as a list of JSONObjects, where each JSONObject represents a message.
 * Each message has a role (either "user" or "assistant") and content (the text of the message).
 */
public class ChatHistoryManager {
    // List to store the chat history
    private final List<JSONObject> chatHistory;

    /**
     * Constructor for the ChatHistoryManager class.
     * Initializes the chatHistory list.
     */
    public ChatHistoryManager() {
        this.chatHistory = new ArrayList<>();
    }

    /**
     * Getter for the chatHistory list.
     *
     * @return The chatHistory list.
     */
    public List<JSONObject> getChatHistory() {
        return chatHistory;
    }

    /**
     * Adds a user message to the chat history.
     *
     * @param message The text of the user message.
     */
    public void addUserMessageToHistory(String message) {
        chatHistory.add(createMessage("user", message));
    }

    /**
     * Adds an assistant message to the chat history.
     *
     * @param message The text of the assistant message.
     */
    public void addAssistantMessageToHistory(String message) {
        chatHistory.add(createMessage("assistant", message));
    }

    /**
     * Creates a JSONObject representing a message.
     *
     * @param role    The role of the message (either "user" or "assistant").
     * @param content The text of the message.
     * @return A JSONObject representing the message.
     */
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