package com.tfg.kaifit_pal.fragments.kaiqassistant;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import com.tfg.kaifit_pal.R;
import com.tfg.kaifit_pal.chatcontrollers.GPTApiCaller;
import com.tfg.kaifit_pal.chatcontrollers.MessageController;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the KaiQ fragment.
 * It handles the user interaction with the chat interface.
 */
public class KaiQ extends Fragment {

    private RecyclerView recyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private List<MessageController> messageList;
    private MessageAdapter messageAdapter;

    // An instance of the GPTApiCaller class to make API requests
    private final GPTApiCaller gptApiCaller = new GPTApiCaller(this);

    /**
     * This method is called to do initial creation of the fragment.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kai_q, container, false);

        messageList = new ArrayList<>();

        setUpComponents(view);

        setUpAdapter();

        return view;
    }

    /**
     * This method sets up the components of the fragment.
     *
     * @param view The View returned by onCreateView(LayoutInflater, ViewGroup, Bundle).
     */
    public void setUpComponents(@NonNull View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        messageEditText = view.findViewById(R.id.message_edit_text);
        sendButton = view.findViewById(R.id.send_btn);
        setUpListeners();
    }

    /**
     * This method sets up the listeners for the components of the fragment.
     */
    public void setUpListeners() {
        sendButton.setOnClickListener(v -> {
            String userQuery = messageEditText.getText().toString().trim();
            if (!userQuery.isEmpty()) {
                addToChat(userQuery, MessageController.SENT_BY_USER);
                messageEditText.setText("");
                gptApiCaller.gptApiRequest(userQuery);
            }
        });
    }

    /**
     * This method sets up the adapter for the RecyclerView.
     */
    public void setUpAdapter() {
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
    }

    /**
     * This method adds a message to the chat.
     *
     * @param message The message to be added.
     * @param sentBy  The sender of the message.
     */
    public void addToChat(final String message, final String sentBy) {
        Runnable runnable = () -> {
            messageList.add(new MessageController(message, sentBy));
            messageAdapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
        };
        requireActivity().runOnUiThread(runnable);
    }

    /**
     * This method adds a response to the chat.
     *
     * @param response The response to be added.
     */
    public void addResponseToChat(String response) {
        addToChat(response, MessageController.SENT_BY_BOT);
    }
}