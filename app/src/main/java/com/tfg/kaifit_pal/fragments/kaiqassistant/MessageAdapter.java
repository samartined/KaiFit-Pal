package com.tfg.kaifit_pal.fragments.kaiqassistant;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tfg.kaifit_pal.R;
import com.tfg.kaifit_pal.chatcontroller.MessageController;

import java.util.List;

/**
 * This class represents the adapter for the RecyclerView in the KaiQ chat.
 * It handles the creation and binding of view holders for the chat messages.
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder> {
    // List of messages in the chat
    List<MessageController> messageList;

    /**
     * Constructor for the MessageAdapter class.
     *
     * @param messageList The list of messages in the chat.
     */
    public MessageAdapter(List<MessageController> messageList) {
        this.messageList = messageList;
    }

    /**
     * This method is called when the RecyclerView needs a new ViewHolder of the given type to represent an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View KaiQChatView = View.inflate(parent.getContext(), R.layout.chat_item, null);

        return new MyViewHolder(KaiQChatView);
    }

    /**
     * This method is called by RecyclerView to display the data at the specified position.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        MessageController message = messageList.get(position);
        if (message.getSentBy().equals(MessageController.SENT_BY_USER)) {
            holder.kaiQChatView.setVisibility(View.GONE);
            holder.userChatView.setVisibility(View.VISIBLE);
            holder.userChatText.setText(message.getMessage());
            holder.userChatText.setTextIsSelectable(true);
        } else {
            holder.userChatView.setVisibility(View.GONE);
            holder.kaiQChatView.setVisibility(View.VISIBLE);
            holder.kaiQChatText.setText(message.getMessage());
            holder.kaiQChatText.setTextIsSelectable(true);
        }
    }

    /**
     * This method returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return messageList.size();
    }

    /**
     * This class represents a ViewHolder in the RecyclerView, which extends the ViewHolder class.
     * It contains the chat views and text views for the KaiQ and user messages.
     */
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        LinearLayout kaiQChatView, userChatView;
        TextView kaiQChatText, userChatText;

        /**
         * Constructor for the MyViewHolder class.
         *
         * @param itemView The view that is managed by this holder.
         */
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            kaiQChatView = itemView.findViewById(R.id.left_chat_view);
            userChatView = itemView.findViewById(R.id.right_chat_view);
            kaiQChatText = itemView.findViewById(R.id.left_chat_textview);
            userChatText = itemView.findViewById(R.id.right_chat_textview);
        }
    }
}