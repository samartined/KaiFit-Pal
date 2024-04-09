package com.tfg.kaifit_pal.fragments.kaiqassistant;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tfg.kaifit_pal.R;
import com.tfg.kaifit_pal.chatcontrollers.MessageController;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder> {
    List<MessageController> messageList;

    public MessageAdapter(List<MessageController> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View KaiQChatView = View.inflate(parent.getContext(), R.layout.chat_item, null);

        return new MyViewHolder(KaiQChatView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        MessageController message = messageList.get(position);
        if (message.getSentBy().equals(MessageController.SENT_BY_USER)) {
            holder.kaiQChatView.setVisibility(View.GONE);
            holder.userChatView.setVisibility(View.VISIBLE);
            holder.userChatText.setText(message.getMessage());
        } else {
            holder.userChatView.setVisibility(View.GONE);
            holder.kaiQChatView.setVisibility(View.VISIBLE);
            holder.kaiQChatText.setText(message.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        LinearLayout kaiQChatView, userChatView;
        TextView kaiQChatText, userChatText;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            kaiQChatView = itemView.findViewById(R.id.left_chat_view);
            userChatView = itemView.findViewById(R.id.right_chat_view);
            kaiQChatText = itemView.findViewById(R.id.left_chat_textview);
            userChatText = itemView.findViewById(R.id.right_chat_textview);
        }
    }
}
