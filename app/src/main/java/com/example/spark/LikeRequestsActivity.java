package com.example.spark;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class LikeRequestsActivity extends AppCompatActivity {

    private RecyclerView rvLikes;
    private DiscoverAdapter adapter;
    private List<ProfileModel> profileList;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_like_requests);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        rvLikes = findViewById(R.id.rv_like_requests);
        rvLikes.setLayoutManager(new GridLayoutManager(this, 2));

        profileList = new ArrayList<>();
        adapter = new DiscoverAdapter(profileList, new DiscoverAdapter.OnProfileActionListener() {
            @Override
            public void onLike(ProfileModel profile) {
                likeBackUser(profile);
            }

            @Override
            public void onProfileClick(ProfileModel profile) {
                // To be implemented: Show full profile
            }
        });
        rvLikes.setAdapter(adapter);

        ImageView btnBack = findViewById(R.id.btn_back_requests);
        btnBack.setOnClickListener(v -> finish());

        fetchLikedByProfiles();
    }

    private void fetchLikedByProfiles() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        // Listen for users who liked me
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
                                profileList.add(profile);
                                adapter.notifyDataSetChanged();
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

    private void likeBackUser(ProfileModel otherUser) {
        String myId = mAuth.getCurrentUser().getUid();
        String otherId = otherUser.getId();

        // 1. Add otherId to my matches
        mDatabase.child("users").child(myId).child("matchedUserIds").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> myMatches = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String id = ds.getValue(String.class);
                    if (id != null) myMatches.add(id);
                }
                if (!myMatches.contains(otherId)) {
                    myMatches.add(otherId);
                    mDatabase.child("users").child(myId).child("matchedUserIds").setValue(myMatches);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 2. Add myId to other user's matches
        mDatabase.child("users").child(otherId).child("matchedUserIds").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> otherMatches = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String id = ds.getValue(String.class);
                    if (id != null) otherMatches.add(id);
                }
                if (!otherMatches.contains(myId)) {
                    otherMatches.add(myId);
                    mDatabase.child("users").child(otherId).child("matchedUserIds").setValue(otherMatches);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 3. Remove otherId from my likedByUids (since it's now a match)
        mDatabase.child("users").child(myId).child("likedByUids").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> likedByList = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String id = ds.getValue(String.class);
                    if (id != null && !id.equals(otherId)) likedByList.add(id);
                }
                mDatabase.child("users").child(myId).child("likedByUids").setValue(likedByList);
                Toast.makeText(LikeRequestsActivity.this, "It's a Match!", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
