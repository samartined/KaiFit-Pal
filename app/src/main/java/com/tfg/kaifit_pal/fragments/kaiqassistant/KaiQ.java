package com.tfg.kaifit_pal.fragments.kaiqassistant;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import com.tfg.kaifit_pal.R;
import com.tfg.kaifit_pal.chatcontroller.MessageController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KaiQ extends DialogFragment {

    private RecyclerView recyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private List<MessageController> messageList;
    MessageAdapter messageAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kai_q, container, false);

        messageList = new ArrayList<>();

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
            addToChat(userQuery, MessageController.SENT_BY_USER);
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
            messageEditText.setText("");
        };
        requireActivity().runOnUiThread(runnable);
    }
}