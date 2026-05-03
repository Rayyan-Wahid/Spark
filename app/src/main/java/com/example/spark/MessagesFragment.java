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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // New Matches RecyclerView
        RecyclerView rvNewMatches = view.findViewById(R.id.rv_new_matches);
        rvNewMatches.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        matchList = new ArrayList<>();
        matchAdapter = new NewMatchAdapter(matchList, new NewMatchAdapter.OnMatchClickListener() {
            @Override
            public void onShowProfile(ProfileModel profile) {
                // To be implemented: View Profile
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

        // Conversations RecyclerView
        RecyclerView rvConversations = view.findViewById(R.id.rv_conversations);
        rvConversations.setLayoutManager(new LinearLayoutManager(getContext()));

        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatList);
        rvConversations.setAdapter(chatAdapter);

        fetchMatchedProfiles();
        fetchConversations();

        return view;
    }

    private void fetchConversations() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        // Directly listen to the 'messages' node to extract conversations
        mDatabase.child("messages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                chatList.clear();
                for (DataSnapshot chatRoomSnapshot : snapshot.getChildren()) {
                    String chatRoomId = chatRoomSnapshot.getKey();
                    if (chatRoomId != null && chatRoomId.contains(myId)) {
                        // This room belongs to me. Find the other user's ID
                        String otherUserId = chatRoomId.replace(myId, "").replace("_", "");
                        
                        // Get the last message in this room
                        DataSnapshot lastMsgSnapshot = null;
                        for (DataSnapshot msg : chatRoomSnapshot.getChildren()) {
                            lastMsgSnapshot = msg;
                        }

                        if (lastMsgSnapshot != null) {
                            MessageModel lastMsg = lastMsgSnapshot.getValue(MessageModel.class);
                            if (lastMsg != null) {
                                // Create a placeholder chat model
                                ChatModel chat = new ChatModel("Loading...", 0, lastMsg.getText(), "", false, otherUserId);
                                chatList.add(chat);
                                
                                // Fetch the actual profile details for this user
                                fetchProfileDetailsForChat(chat, otherUserId);
                            }
                        }
                    }
                }
                chatAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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

        // ONLY show users who are mutual matches (both liked each other)
        mDatabase.child("users").child(myId).child("matchedUserIds").addValueEventListener(new ValueEventListener() {
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

                // Now filter out users with whom we already have a conversation
                mDatabase.child("messages").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot messagesSnapshot) {
                        if (!isAdded()) return;
                        matchList.clear();
                        for (String id : matchIds) {
                            // Deterministic conversation ID logic
                            List<String> pair = new ArrayList<>();
                            pair.add(myId);
                            pair.add(id);
                            java.util.Collections.sort(pair);
                            String convId = pair.get(0) + "_" + pair.get(1);

                            // If conversation DOES NOT exist, add to New Matches
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
        });
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