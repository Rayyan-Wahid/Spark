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
    private ValueEventListener likesListener;
    private DatabaseReference likesRef;

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

    @Override
    public void onStop() {
        super.onStop();
        if (likesRef != null && likesListener != null) {
            likesRef.removeEventListener(likesListener);
        }
    }

    private void fetchLikedByProfiles() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        likesRef = mDatabase.child("users").child(myId).child("likedByUids");
        likesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                profileList.clear();
                List<String> likedByIds = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String userId = ds.getValue(String.class);
                    if (userId != null) likedByIds.add(userId);
                }

                if (likedByIds.isEmpty()) {
                    adapter.notifyDataSetChanged();
                    return;
                }

                // Fetch current matchedUserIds to exclude already-matched
                mDatabase.child("users").child(myId).child("matchedUserIds")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot matchSnap) {
                        if (!isAdded()) return;
                        java.util.Set<String> matchedSet = new java.util.HashSet<>();
                        for (DataSnapshot ds : matchSnap.getChildren()) {
                            String uid = ds.getValue(String.class);
                            if (uid != null) matchedSet.add(uid);
                        }

                        for (String id : likedByIds) {
                            if (matchedSet.contains(id)) continue; // skip already matched
                            mDatabase.child("users").child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                    if (!isAdded()) return;
                                    ProfileModel profile = userSnapshot.getValue(ProfileModel.class);
                                    if (profile != null) {
                                        boolean exists = false;
                                        for (ProfileModel p : profileList) {
                                            if (p.getId() != null && p.getId().equals(profile.getId())) {
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        likesRef.addValueEventListener(likesListener);
    }

    private void handleMatch(ProfileModel otherUser) {
        String myId = mAuth.getCurrentUser().getUid();
        String otherId = otherUser.getId();

        // Add to both matchedUserIds
        addMatch(myId, otherId);
        addMatch(otherId, myId);

        // Mark as seen for Home/Discover so they never show in swipe queue
        mDatabase.child("users").child(myId).child("seenUids").push().setValue(otherId);

        // Remove from my likedByUids
        removeFromLikes(myId, otherId);

        Toast.makeText(getContext(), "It's a Match! 💞", Toast.LENGTH_SHORT).show();
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
