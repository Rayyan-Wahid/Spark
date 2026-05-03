package com.example.spark;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private MaterialCardView profileCard;
    private ImageView ivProfileImage;
    private TextView tvName, tvDistance, tvBio;
    private com.google.android.material.chip.ChipGroup chipGroupInterests;
    private FloatingActionButton btnRewind, btnDislike, btnLike;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private List<ProfileModel> profileList = new ArrayList<>();
    private int currentIndex = 0;
    private ProfileModel currentProfile;
    private List<String> myMatches = new ArrayList<>();
    private boolean isAnimating = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize UI
        profileCard = view.findViewById(R.id.profile_card);
        profileCard.setOnClickListener(v -> {
            if (currentProfile != null) {
                Intent intent = new Intent(getActivity(), ViewProfileActivity.class);
                intent.putExtra("USER_ID", currentProfile.getId());
                startActivity(intent);
            }
        });
        ivProfileImage = view.findViewById(R.id.profile_image);
        tvName = view.findViewById(R.id.tv_profile_name);
        tvDistance = view.findViewById(R.id.tv_profile_distance);
        tvBio = view.findViewById(R.id.tv_profile_bio);
        chipGroupInterests = view.findViewById(R.id.home_chip_group_interests);

        btnRewind = view.findViewById(R.id.btn_rewind);
        btnDislike = view.findViewById(R.id.btn_dislike);
        btnLike = view.findViewById(R.id.btn_like);

        // Fetch My Current Matches First
        fetchMyMatches();

        // Action Listeners
        btnRewind.setOnClickListener(v -> skipProfile());
        btnDislike.setOnClickListener(v -> dislikeProfile());
        btnLike.setOnClickListener(v -> likeProfile());

        return view;
    }

    private void fetchMyMatches() {
        String myId = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(myId).child("matchedUserIds").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                myMatches.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String matchId = ds.getValue(String.class);
                    if (matchId != null) myMatches.add(matchId);
                }
                fetchProfiles();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchProfiles() {
        String myId = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                profileList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ProfileModel profile = ds.getValue(ProfileModel.class);
                    if (profile != null && !profile.getId().equals(myId) 
                        && profile.isProfileCompleted() && !myMatches.contains(profile.getId())) {
                        profileList.add(profile);
                    }
                }
                showNextProfile();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showNextProfile() {
        if (currentIndex < profileList.size()) {
            currentProfile = profileList.get(currentIndex);
            currentIndex++; // Increment only when we are actually picking the next one
            
            profileCard.setVisibility(View.VISIBLE);
            
            // Reset position before showing
            profileCard.setTranslationX(0);
            profileCard.setTranslationY(1000);
            profileCard.setRotation(0);
            profileCard.setAlpha(0);
            
            // Update UI with new profile
            tvName.setText(currentProfile.getName() + ", " + currentProfile.getAge());
            tvBio.setText(currentProfile.getBio());
            tvDistance.setText(currentProfile.getDistance() != null ? currentProfile.getDistance() : "Near you");

            // Update interests chips
            if (chipGroupInterests != null) {
                chipGroupInterests.removeAllViews();
                if (currentProfile.getInterests() != null) {
                    for (String interest : currentProfile.getInterests()) {
                        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(getContext());
                        chip.setText(interest);
                        chip.setChipBackgroundColorResource(R.color.white);
                        chip.setTextColor(getResources().getColor(R.color.pink_primary));
                        chip.setChipCornerRadius(20f);
                        chip.setChipStrokeWidth(0f);
                        chipGroupInterests.addView(chip);
                    }
                }
            }

            if (currentProfile.getProfileImageUrl() != null && !currentProfile.getProfileImageUrl().isEmpty()) {
                Glide.with(this)
                        .load(currentProfile.getProfileImageUrl())
                        .centerCrop()
                        .placeholder(R.drawable.heart)
                        .into(ivProfileImage);
            } else {
                ivProfileImage.setImageResource(R.drawable.heart);
            }

            // Animate card in from bottom
            profileCard.animate()
                    .translationY(0)
                    .alpha(1)
                    .setDuration(500)
                    .setInterpolator(new AccelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            isAnimating = false;
                        }
                    })
                    .start();
            
        } else {
            // No more profiles
            isAnimating = false;
            profileCard.setVisibility(View.GONE);
            Toast.makeText(getContext(), "No more profiles for now!", Toast.LENGTH_LONG).show();
        }
    }

    private void likeProfile() {
        if (currentProfile == null || isAnimating) return;
        isAnimating = true;
        
        String myId = mAuth.getCurrentUser().getUid();
        String otherId = currentProfile.getId();

        // 1. Add me to the other user's "likedByUids" list
        mDatabase.child("users").child(otherId).child("likedByUids").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> likedByList = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String id = ds.getValue(String.class);
                    if (id != null) likedByList.add(id);
                }
                
                if (!likedByList.contains(myId)) {
                    likedByList.add(myId);
                    mDatabase.child("users").child(otherId).child("likedByUids").setValue(likedByList);
                }

                // 2. Check if I am already in the other user's "likedByUids" (This means they already liked me)
                // Actually, the logic should be: Check if otherId is in MY "likedByUids"
                checkForMutualMatch(myId, otherId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isAnimating = false;
            }
        });

        // Animation: Slide Out Right
        profileCard.animate()
                .translationX(1000)
                .rotation(30)
                .alpha(0)
                .setDuration(400)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        showNextProfile();
                    }
                }).start();
    }

    private void checkForMutualMatch(String myId, String otherId) {
        // Check if the other user has already liked me
        mDatabase.child("users").child(myId).child("likedByUids").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean theyLikedMe = false;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (otherId.equals(ds.getValue(String.class))) {
                        theyLikedMe = true;
                        break;
                    }
                }

                if (theyLikedMe) {
                    // IT'S A MATCH!
                    addMatchToBothUsers(myId, otherId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addMatchToBothUsers(String myId, String otherId) {
        // Add otherId to my matches
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

        // Add myId to other user's matches
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

        Toast.makeText(getContext(), "It's a Mutual Match! Conversation unlocked.", Toast.LENGTH_LONG).show();
    }

    private void dislikeProfile() {
        if (currentProfile == null || isAnimating) return;
        isAnimating = true;

        // Animation: Slide Out Left
        profileCard.animate()
                .translationX(-1000)
                .rotation(-30)
                .alpha(0)
                .setDuration(400)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        showNextProfile();
                    }
                }).start();
    }

    private void skipProfile() {
        if (currentProfile == null || isAnimating) return;
        isAnimating = true;

        profileCard.animate()
                .translationY(-1000)
                .alpha(0)
                .setDuration(400)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // Add current back to end of list to "push to last"
                        profileList.add(currentProfile);
                        showNextProfile();
                    }
                }).start();
    }
}