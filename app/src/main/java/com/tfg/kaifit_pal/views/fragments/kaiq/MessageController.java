package com.tfg.kaifit_pal.views.fragments.kaiq;

/**
 * This class represents a message in the chat.
 * It contains the message content and the sender of the message.
 */
public class MessageController {

    // Constants to represent the sender of the message
    public static final String SENT_BY_USER = "me";
    public static final String SENT_BY_BOT = "bot";

    String message;
    String sentBy;

    public MessageController(String message, String sentBy) {
        this.message = message;
        this.sentBy = sentBy;
    }

    public String getMessage() {
        return message;
    }

    public String getSentBy() {
        return sentBy;
    }
}