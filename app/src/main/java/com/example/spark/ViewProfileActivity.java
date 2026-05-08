package com.example.spark;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.ViewCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ViewProfileActivity extends AppCompatActivity {

    private ImageView ivProfilePic, btnBack;
    private TextView tvNameAge, tvDistance, tvBio, tvGender;
    private ChipGroup chipGroup;
    private MaterialButton btnMatch;
    private String otherUserId;
    private DatabaseReference mDatabase;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);
        
        hideSystemUI();

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        otherUserId = getIntent().getStringExtra("USER_ID");

        if (otherUserId == null) {
            finish();
            return;
        }

        initUI();
        fetchProfileData();
        checkButtonState();
    }

    private void initUI() {
        ivProfilePic = findViewById(R.id.iv_view_profile_pic);
        btnBack = findViewById(R.id.btn_view_profile_back);
        tvNameAge = findViewById(R.id.tv_view_profile_name_age);
        tvDistance = findViewById(R.id.tv_view_profile_distance);
        tvBio = findViewById(R.id.tv_view_profile_bio);
        tvGender = findViewById(R.id.tv_view_profile_gender);
        chipGroup = findViewById(R.id.view_profile_chip_group);
        btnMatch = findViewById(R.id.btn_view_profile_match);

        btnBack.setOnClickListener(v -> finish());
        btnMatch.setOnClickListener(v -> handleMatchAction());
    }

    private void fetchProfileData() {
        mDatabase.child("users").child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ProfileModel profile = snapshot.getValue(ProfileModel.class);
                if (profile != null) {
                    tvNameAge.setText(profile.getName() + ", " + profile.getAge());
                    tvDistance.setText(profile.getDistance() != null ? profile.getDistance() : "Near you");
                    tvGender.setText(profile.getGender() != null ? profile.getGender() : "Not specified");
                    tvBio.setText(profile.getBio() != null ? profile.getBio() : "No bio provided.");
                    
                    if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
                        Glide.with(ViewProfileActivity.this).load(profile.getProfileImageUrl()).into(ivProfilePic);
                    }

                    if (profile.getInterests() != null) {
                        chipGroup.removeAllViews();
                        for (String interest : profile.getInterests()) {
                            Chip chip = new Chip(ViewProfileActivity.this);
                            chip.setText(interest);
                            chip.setChipBackgroundColorResource(R.color.bg_color);
                            chip.setTextColor(ContextCompat.getColor(ViewProfileActivity.this, R.color.pink_primary));
                            chipGroup.addView(chip);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /** Single method that sets the correct button state based on match + pending like. */
    private void checkButtonState() {
        // Check if already matched
        mDatabase.child("users").child(currentUserId).child("matchedUserIds").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (otherUserId.equals(ds.getValue(String.class))) {
                        btnMatch.setText("Already Matched 💞");
                        btnMatch.setEnabled(false);
                        btnMatch.setAlpha(0.7f);
                        return;
                    }
                }
                // Not matched — check if we already liked them (pending)
                mDatabase.child("users").child(otherUserId).child("likedByUids").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            if (currentUserId.equals(ds.getValue(String.class))) {
                                btnMatch.setText("Like Pending ⏳");
                                btnMatch.setEnabled(false);
                                btnMatch.setAlpha(0.7f);
                                return;
                            }
                        }
                        // Not matched, not pending — allow action
                        btnMatch.setText("Send Like 💖");
                        btnMatch.setEnabled(true);
                        btnMatch.setAlpha(1f);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void handleMatchAction() {
        // Logic similar to HomeFragment's likeProfile
        mDatabase.child("users").child(otherUserId).child("likedByUids").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> likedByList = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String id = ds.getValue(String.class);
                    if (id != null) likedByList.add(id);
                }
                
                if (!likedByList.contains(currentUserId)) {
                    likedByList.add(currentUserId);
                    mDatabase.child("users").child(otherUserId).child("likedByUids").setValue(likedByList);
                }
                checkForMutualMatch();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkForMutualMatch() {
        mDatabase.child("users").child(currentUserId).child("likedByUids").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean theyLikedMe = false;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (otherUserId.equals(ds.getValue(String.class))) {
                        theyLikedMe = true;
                        break;
                    }
                }

                if (theyLikedMe) {
                    addMatchToBothUsers();
                } else {
                    // Show pending state without finishing
                    btnMatch.setText("Like Pending ⏳");
                    btnMatch.setEnabled(false);
                    btnMatch.setAlpha(0.7f);
                    Toast.makeText(ViewProfileActivity.this, "Like sent! Waiting for them to like back.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addMatchToBothUsers() {
        // Add otherId to my matches
        mDatabase.child("users").child(currentUserId).child("matchedUserIds").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> myMatches = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String id = ds.getValue(String.class);
                    if (id != null) myMatches.add(id);
                }
                if (!myMatches.contains(otherUserId)) {
                    myMatches.add(otherUserId);
                    mDatabase.child("users").child(currentUserId).child("matchedUserIds").setValue(myMatches);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Add myId to other user's matches
        mDatabase.child("users").child(otherUserId).child("matchedUserIds").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> otherMatches = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String id = ds.getValue(String.class);
                    if (id != null) otherMatches.add(id);
                }
                if (!otherMatches.contains(currentUserId)) {
                    otherMatches.add(currentUserId);
                    mDatabase.child("users").child(otherUserId).child("matchedUserIds").setValue(otherMatches);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Remove from likedByUids
        mDatabase.child("users").child(currentUserId).child("likedByUids").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> likedByList = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String id = ds.getValue(String.class);
                    if (id != null && !id.equals(otherUserId)) likedByList.add(id);
                }
                mDatabase.child("users").child(currentUserId).child("likedByUids").setValue(likedByList);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Mark matched user as seen so they are excluded from Home/Discover swipe queue
        mDatabase.child("users").child(currentUserId).child("seenUids").push().setValue(otherUserId);

        Toast.makeText(this, "It's a Mutual Match! 💞", Toast.LENGTH_LONG).show();
        finish();
    }

    private void hideSystemUI() {
        WindowInsetsControllerCompat windowInsetsController =
                ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        }
    }
}
