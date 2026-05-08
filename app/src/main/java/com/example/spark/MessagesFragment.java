package com.example.spark;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MessagesFragment extends Fragment {

    private NewMatchAdapter matchAdapter;
    private List<ProfileModel> matchList;
    private ChatAdapter chatAdapter;
    private List<ChatModel> chatList;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    // Persistent listeners tracked for cleanup
    private ValueEventListener conversationsListener;
    private ValueEventListener matchedIdsListener;
    private DatabaseReference conversationsRef;
    private DatabaseReference matchedIdsRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        RecyclerView rvNewMatches = view.findViewById(R.id.rv_new_matches);
        rvNewMatches.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        matchList = new ArrayList<>();
        matchAdapter = new NewMatchAdapter(matchList, new NewMatchAdapter.OnMatchClickListener() {
            @Override
            public void onShowProfile(ProfileModel profile) {
                Toast.makeText(getContext(), "Showing profile: " + profile.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartMessaging(ProfileModel profile) {
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("OTHER_USER_ID", profile.getId());
                intent.putExtra("OTHER_USER_NAME", profile.getName());
                intent.putExtra("OTHER_USER_IMAGE", profile.getProfileImageUrl());
                startActivity(intent);
            }
        });
        rvNewMatches.setAdapter(matchAdapter);

        RecyclerView rvConversations = view.findViewById(R.id.rv_conversations);
        rvConversations.setLayoutManager(new LinearLayoutManager(getContext()));

        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatList);
        rvConversations.setAdapter(chatAdapter);

        fetchMatchedProfiles();
        fetchConversations();

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (conversationsRef != null && conversationsListener != null) {
            conversationsRef.removeEventListener(conversationsListener);
        }
        if (matchedIdsRef != null && matchedIdsListener != null) {
            matchedIdsRef.removeEventListener(matchedIdsListener);
        }
    }

    private void fetchConversations() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        conversationsRef = mDatabase.child("messages");
        conversationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                chatList.clear();
                for (DataSnapshot chatRoomSnapshot : snapshot.getChildren()) {
                    String chatRoomId = chatRoomSnapshot.getKey();
                    if (chatRoomId != null && chatRoomId.contains(myId)) {
                        String otherUserId = chatRoomId.replace(myId, "").replace("_", "");

                        DataSnapshot lastMsgSnapshot = null;
                        for (DataSnapshot msg : chatRoomSnapshot.getChildren()) {
                            lastMsgSnapshot = msg;
                        }

                        if (lastMsgSnapshot != null) {
                            MessageModel lastMsg = lastMsgSnapshot.getValue(MessageModel.class);
                            if (lastMsg != null) {
                                ChatModel chat = new ChatModel("Loading...", 0, lastMsg.getText(), "", false, otherUserId);
                                chatList.add(chat);
                                fetchProfileDetailsForChat(chat, otherUserId);
                            }
                        }
                    }
                }
                chatAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        conversationsRef.addValueEventListener(conversationsListener);
    }

    private void fetchProfileDetailsForChat(ChatModel chat, String otherUserId) {
        mDatabase.child("users").child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                ProfileModel profile = snapshot.getValue(ProfileModel.class);
                if (profile != null) {
                    chat.setName(profile.getName());
                    chat.setAge(profile.getAge());
                    chat.setProfileImageUrl(profile.getProfileImageUrl());
                    chatAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchMatchedProfiles() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        matchedIdsRef = mDatabase.child("users").child(myId).child("matchedUserIds");
        matchedIdsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                List<String> matchIds = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String matchId = ds.getValue(String.class);
                    if (matchId != null) matchIds.add(matchId);
                }

                if (matchIds.isEmpty()) {
                    matchList.clear();
                    matchAdapter.notifyDataSetChanged();
                    return;
                }

                mDatabase.child("messages").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot messagesSnapshot) {
                        if (!isAdded()) return;
                        matchList.clear();
                        for (String id : matchIds) {
                            List<String> pair = new ArrayList<>();
                            pair.add(myId);
                            pair.add(id);
                            java.util.Collections.sort(pair);
                            String convId = pair.get(0) + "_" + pair.get(1);

                            if (!messagesSnapshot.hasChild(convId)) {
                                fetchProfileForNewMatch(id);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        matchedIdsRef.addValueEventListener(matchedIdsListener);
    }

    private void fetchProfileForNewMatch(String userId) {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                if (!isAdded()) return;
                ProfileModel profile = userSnapshot.getValue(ProfileModel.class);
                if (profile != null) {
                    boolean exists = false;
                    for (ProfileModel p : matchList) {
                        if (p.getId().equals(profile.getId())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        matchList.add(profile);
                        matchAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}