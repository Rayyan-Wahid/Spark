package com.example.spark;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private String otherUserId, otherUserName, otherUserImage;
    private String currentUserId, chatRoomId;
    private DatabaseReference mDatabase;
    
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<MessageModel> messageList;
    private EditText etMessage;
    private ImageView btnSend, btnBack, ivProfile;
    private TextView tvName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        otherUserId = getIntent().getStringExtra("OTHER_USER_ID");
        otherUserName = getIntent().getStringExtra("OTHER_USER_NAME");
        otherUserImage = getIntent().getStringExtra("OTHER_USER_IMAGE");
        
        if (FirebaseAuth.getInstance().getCurrentUser() == null || otherUserId == null) {
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Create Chat Room ID by concatenating IDs in alphabetical order
        List<String> ids = new ArrayList<>();
        ids.add(currentUserId);
        ids.add(otherUserId);
        Collections.sort(ids);
        chatRoomId = ids.get(0) + "_" + ids.get(1);

        initUI();
        loadMessages();
    }

    private void initUI() {
        rvMessages = findViewById(R.id.rv_chat_messages);
        etMessage = findViewById(R.id.et_chat_message);
        btnSend = findViewById(R.id.btn_send_message);
        btnBack = findViewById(R.id.btn_chat_back);
        ivProfile = findViewById(R.id.iv_chat_profile);
        tvName = findViewById(R.id.tv_chat_name);

        tvName.setText(otherUserName);
        if (otherUserImage != null && !otherUserImage.isEmpty()) {
            Glide.with(this).load(otherUserImage).circleCrop().into(ivProfile);
        }

        messageList = new ArrayList<>();
        adapter = new MessageAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Always show newest messages
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void loadMessages() {
        mDatabase.child("messages").child(chatRoomId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    MessageModel msg = ds.getValue(MessageModel.class);
                    if (msg != null) messageList.add(msg);
                }
                adapter.notifyDataSetChanged();
                if (messageList.size() > 0) {
                    rvMessages.smoothScrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendMessage() {
        String msgText = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(msgText)) return;

        // Use the structure: sender, text, timestamp
        MessageModel message = new MessageModel(
                currentUserId,
                msgText,
                System.currentTimeMillis()
        );

        // Save to messages/conversationId using push()
        mDatabase.child("messages").child(chatRoomId).push().setValue(message)
                .addOnSuccessListener(aVoid -> {
                    etMessage.setText("");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ChatActivity", "Send failed", e);
                    Toast.makeText(ChatActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                 });
     }
 }
