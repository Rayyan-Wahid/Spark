package com.example.spark;

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

public class LikesFragment extends Fragment {

    private RecyclerView rvLikes;
    private LikeRequestAdapter adapter;
    private List<ProfileModel> profileList;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_likes, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        rvLikes = view.findViewById(R.id.rv_likes);
        rvLikes.setLayoutManager(new LinearLayoutManager(getContext()));

        profileList = new ArrayList<>();
        adapter = new LikeRequestAdapter(profileList, new LikeRequestAdapter.OnLikeActionListener() {
            @Override
            public void onAccept(ProfileModel profile) {
                handleMatch(profile);
            }

            @Override
            public void onDecline(ProfileModel profile) {
                handleDecline(profile);
            }
        });
        rvLikes.setAdapter(adapter);

        fetchLikedByProfiles();

        return view;
    }

    private void fetchLikedByProfiles() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        mDatabase.child("users").child(myId).child("likedByUids").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                profileList.clear();
                List<String> userIds = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String userId = ds.getValue(String.class);
                    if (userId != null) userIds.add(userId);
                }

                if (userIds.isEmpty()) {
                    adapter.notifyDataSetChanged();
                    return;
                }

                for (String id : userIds) {
                    mDatabase.child("users").child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            ProfileModel profile = userSnapshot.getValue(ProfileModel.class);
                            if (profile != null) {
                                boolean exists = false;
                                for (ProfileModel p : profileList) {
                                    if (p.getId().equals(profile.getId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    profileList.add(profile);
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void handleMatch(ProfileModel otherUser) {
        String myId = mAuth.getCurrentUser().getUid();
        String otherId = otherUser.getId();

        // Add to both matchedUserIds
        addMatch(myId, otherId);
        addMatch(otherId, myId);

        // Remove from my likedByUids
        removeFromLikes(myId, otherId);
        
        Toast.makeText(getContext(), "It's a Match!", Toast.LENGTH_SHORT).show();
    }

    private void handleDecline(ProfileModel otherUser) {
        String myId = mAuth.getCurrentUser().getUid();
        removeFromLikes(myId, otherUser.getId());
        Toast.makeText(getContext(), "Declined", Toast.LENGTH_SHORT).show();
    }

    private void addMatch(String userId, String matchId) {
        mDatabase.child("users").child(userId).child("matchedUserIds").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> matches = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    matches.add(ds.getValue(String.class));
                }
                if (!matches.contains(matchId)) {
                    matches.add(matchId);
                    mDatabase.child("users").child(userId).child("matchedUserIds").setValue(matches);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void removeFromLikes(String myId, String otherId) {
        mDatabase.child("users").child(myId).child("likedByUids").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> likes = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String id = ds.getValue(String.class);
                    if (id != null && !id.equals(otherId)) {
                        likes.add(id);
                    }
                }
                mDatabase.child("users").child(myId).child("likedByUids").setValue(likes);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
