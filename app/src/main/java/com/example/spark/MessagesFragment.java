package com.example.spark;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MessagesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        // New Matches RecyclerView
        RecyclerView rvNewMatches = view.findViewById(R.id.rv_new_matches);
        rvNewMatches.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        List<ProfileModel> matchList = new ArrayList<>();
        matchList.add(new ProfileModel("Leo", 25, "", R.drawable.heart));
        matchList.add(new ProfileModel("Mia", 22, "", R.drawable.heart));
        matchList.add(new ProfileModel("James", 27, "", R.drawable.heart));
        matchList.add(new ProfileModel("Chloe", 24, "", R.drawable.heart));

        NewMatchAdapter matchAdapter = new NewMatchAdapter(matchList);
        rvNewMatches.setAdapter(matchAdapter);

        // Conversations RecyclerView
        RecyclerView rvConversations = view.findViewById(R.id.rv_conversations);
        rvConversations.setLayoutManager(new LinearLayoutManager(getContext()));

        List<ChatModel> chatList = new ArrayList<>();
        chatList.add(new ChatModel("Sophie", 24, "That sounds like a perfect...", R.drawable.heart, true));
        chatList.add(new ChatModel("Marcus", 27, "I'll let you know when I finish...", R.drawable.heart, false));
        chatList.add(new ChatModel("Emma", 23, "Haha exactly! Have a great day...", R.drawable.heart, false));

        ChatAdapter chatAdapter = new ChatAdapter(chatList);
        rvConversations.setAdapter(chatAdapter);

        return view;
    }
}