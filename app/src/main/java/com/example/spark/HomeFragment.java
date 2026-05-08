package com.example.spark;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
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
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final double RADIUS_KM = 20.0;

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
    private Set<String> seenUids = new HashSet<>();
    private boolean isAnimating = false;

    private GeoQuery geoQuery;
    private ValueEventListener myLocationListener;
    private DatabaseReference myUserRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

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

        loadNearbyProfiles();

        btnRewind.setOnClickListener(v -> skipProfile());
        btnDislike.setOnClickListener(v -> dislikeProfile());
        btnLike.setOnClickListener(v -> likeProfile());

        return view;
    }

    /** Load my location from Firebase, then run a 20km GeoFire query. */
    private void loadNearbyProfiles() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        // First load all seenUids (matched + passed + disliked)
        mDatabase.child("users").child(myId).child("seenUids").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                seenUids.clear();
                seenUids.add(myId); // always exclude self
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String uid = ds.getValue(String.class);
                    if (uid != null) seenUids.add(uid);
                }
                // Also exclude already matched
                mDatabase.child("users").child(myId).child("matchedUserIds").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        if (!isAdded()) return;
                        for (DataSnapshot ds : snap.getChildren()) {
                            String uid = ds.getValue(String.class);
                            if (uid != null) seenUids.add(uid);
                        }
                        fetchMyLocation(myId);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void fetchMyLocation(String myId) {
        myUserRef = mDatabase.child("users").child(myId);
        myLocationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                Double lat = snapshot.child("latitude").getValue(Double.class);
                Double lng = snapshot.child("longitude").getValue(Double.class);
                if (lat == null || lng == null) {
                    Log.w(TAG, "Location not set — loading all profiles.");
                    fallbackLoadAll(myId);
                    return;
                }
                runGeoQuery(myId, lat, lng);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Location read cancelled: " + error.getMessage());
            }
        };
        myUserRef.addListenerForSingleValueEvent(myLocationListener);
    }

    private void runGeoQuery(String myId, double myLat, double myLng) {
        if (!isAdded()) return;
        if (geoQuery != null) geoQuery.removeAllListeners();

        DatabaseReference geoRef = mDatabase.getRoot().child("geofire");
        GeoFire geoFire = new GeoFire(geoRef);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(myLat, myLng), RADIUS_KM);

        profileList.clear();
        currentIndex = 0;

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String uid, GeoLocation location) {
                if (!isAdded() || seenUids.contains(uid)) return;

                mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        ProfileModel profile = snapshot.getValue(ProfileModel.class);
                        if (profile == null || !profile.isProfileCompleted()) return;

                        float[] results = new float[1];
                        Location.distanceBetween(myLat, myLng,
                                location.latitude, location.longitude, results);
                        int distKm = Math.round(results[0] / 1000f);
                        profile.setDistance(String.format(Locale.getDefault(), "%d km away", distKm));

                        for (ProfileModel p : profileList) {
                            if (p.getId() != null && p.getId().equals(uid)) return;
                        }
                        profileList.add(profile);
                        if (profileList.size() == 1) showNextProfile();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override public void onKeyExited(String uid) {}
            @Override public void onKeyMoved(String uid, GeoLocation location) {}
            @Override
            public void onGeoQueryReady() {
                if (isAdded() && profileList.isEmpty()) {
                    profileCard.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "No nearby profiles right now!", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e(TAG, "GeoQuery error: " + error.getMessage());
            }
        });
    }

    private void fallbackLoadAll(String myId) {
        mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                profileList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ProfileModel profile = ds.getValue(ProfileModel.class);
                    if (profile != null && profile.isProfileCompleted()
                            && !seenUids.contains(profile.getId())) {
                        profileList.add(profile);
                    }
                }
                currentIndex = 0;
                showNextProfile();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /** Mark a UID as seen so it is never shown again. */
    private void markSeen(String uid) {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();
        seenUids.add(uid);
        mDatabase.child("users").child(myId).child("seenUids").push().setValue(uid);
    }

    private void showNextProfile() {
        if (currentIndex < profileList.size()) {
            currentProfile = profileList.get(currentIndex);
            currentIndex++; 
            
            profileCard.setVisibility(View.VISIBLE);
            
            profileCard.setTranslationX(0);
            profileCard.setTranslationY(1000);
            profileCard.setRotation(0);
            profileCard.setAlpha(0);
            
            tvName.setText(currentProfile.getName() + ", " + currentProfile.getAge());
            tvBio.setText(currentProfile.getBio());
            tvDistance.setText(currentProfile.getDistance() != null ? currentProfile.getDistance() : "Near you");

            if (chipGroupInterests != null) {
                chipGroupInterests.removeAllViews();
                if (currentProfile.getInterests() != null) {
                    int max = Math.min(currentProfile.getInterests().size(), 2);
                    for (int i = 0; i < max; i++) {
                        Chip chip = new Chip(getContext());
                        chip.setText(currentProfile.getInterests().get(i));
                        chip.setChipBackgroundColorResource(R.color.pink_primary);
                        chip.setTextColor(getResources().getColor(R.color.white));
                        chip.setChipStrokeWidth(0f);
                        chip.setTextSize(10f);
                        chip.setClickable(false);
                        chip.setFocusable(false);
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
        markSeen(otherId);

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

                checkForMutualMatch(myId, otherId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isAnimating = false;
            }
        });

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
        markSeen(currentProfile.getId());

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
        // Skip = requeue at end of local list only (not marked as permanently seen)

        ProfileModel skipped = currentProfile;
        profileCard.animate()
                .translationY(-1000)
                .alpha(0)
                .setDuration(400)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        profileList.add(skipped);
                        showNextProfile();
                    }
                }).start();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (geoQuery != null) {
            geoQuery.removeAllListeners();
            geoQuery = null;
        }
        if (myUserRef != null && myLocationListener != null) {
            myUserRef.removeEventListener(myLocationListener);
        }
    }
}