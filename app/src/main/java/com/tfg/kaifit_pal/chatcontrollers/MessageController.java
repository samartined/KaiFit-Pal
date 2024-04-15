package com.tfg.kaifit_pal.chatcontrollers;

public class MessageController {

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
