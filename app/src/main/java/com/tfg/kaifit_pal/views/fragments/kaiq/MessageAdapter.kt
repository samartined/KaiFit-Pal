package com.tfg.kaifit_pal.views.fragments.kaiq

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tfg.kaifit_pal.R
import com.tfg.kaifit_pal.views.fragments.kaiq.MessageAdapter.MyViewHolder

class MessageAdapter(var messageList: List<MessageController>?) :
    RecyclerView.Adapter<MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val KaiQChatView = View.inflate(parent.context, R.layout.chat_item, null)

        return MyViewHolder(KaiQChatView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val message = messageList!![position]
        if (message.getSentBy() == MessageController.Companion.SENT_BY_USER) {
            holder.kaiQChatView.visibility = View.GONE
            holder.userChatView.visibility = View.VISIBLE
            holder.userChatText.text = message.getMessage()
            holder.userChatText.setTextIsSelectable(true)
        } else {
            holder.userChatView.visibility = View.GONE
            holder.kaiQChatView.visibility = View.VISIBLE
            holder.kaiQChatText.text = message.getMessage()
            holder.kaiQChatText.setTextIsSelectable(true)
        }
    }

    override fun getItemCount(): Int {
        return messageList!!.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var kaiQChatView: LinearLayout = itemView.findViewById(R.id.left_chat_view)
        var userChatView: LinearLayout = itemView.findViewById(R.id.right_chat_view)
        var kaiQChatText: TextView = itemView.findViewById(R.id.left_chat_textview)
        var userChatText: TextView = itemView.findViewById(R.id.right_chat_textview)
    }
}