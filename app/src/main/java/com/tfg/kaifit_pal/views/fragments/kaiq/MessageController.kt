package com.tfg.kaifit_pal.views.fragments.kaiq

class MessageController(var message: String?, var sentBy: String) {
    companion object {
        const val SENT_BY_USER: String = "me"
        const val SENT_BY_BOT: String = "bot"
    }
}