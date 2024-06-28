package com.tfg.kaifit_pal.views.fragments.kaiq;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import com.tfg.kaifit_pal.R;
import com.tfg.kaifit_pal.kaimodel.GPTApiCaller;

import java.util.ArrayList;
import java.util.List;

public class KaiQ extends Fragment {

    private RecyclerView recyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private List<MessageController> messageList;
    private MessageAdapter messageAdapter;

    private final GPTApiCaller gptApiCaller = new GPTApiCaller(this);

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kai_q, container, false);

        messageList = new ArrayList<>();

        setHasOptionsMenu(true);
        setUpComponents(view);
        setUpAdapter();

        return view;
    }

    public void setUpComponents(@NonNull View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        messageEditText = view.findViewById(R.id.message_edit_text);
        sendButton = view.findViewById(R.id.send_btn);
        setUpListeners();
    }

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

    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.kai_q_appbar, menu);
        MenuItem restoreItem = menu.findItem(R.id.action_restore);
        restoreItem.setOnMenuItemClickListener(item -> {
            clearChat();
            return true;
        });
    }

    public void setUpAdapter() {
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
    }

    public void addToChat(final String message, final String sentBy) {
        Runnable runnable = () -> {
            messageList.add(new MessageController(message, sentBy));
            messageAdapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
        };
        requireActivity().runOnUiThread(runnable);
    }

    public void addResponseToChat(String response) {
        addToChat(response, MessageController.SENT_BY_BOT);
    }

    public void clearChat() {
        messageList.clear();
        messageAdapter.notifyDataSetChanged();
    }
}