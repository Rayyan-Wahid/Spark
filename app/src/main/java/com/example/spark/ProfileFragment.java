package com.example.spark;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private ImageView ivProfilePic, ivProfilePicSmall;
    private TextView tvNameAge, tvBioSummary, tvAboutMe;
    private ChipGroup chipGroupInterests;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        ivProfilePic = view.findViewById(R.id.profile_image);
        ivProfilePicSmall = view.findViewById(R.id.profile_image_small);
        tvNameAge = view.findViewById(R.id.tv_profile_name_age);
        tvBioSummary = view.findViewById(R.id.tv_profile_bio_summary);
        tvAboutMe = view.findViewById(R.id.tv_profile_about_me);
        chipGroupInterests = view.findViewById(R.id.profile_chip_group_interests);

        LinearLayout btnLogout = view.findViewById(R.id.btn_logout_section);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getActivity(), AuthActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
            });
        }

        com.google.android.material.button.MaterialButton btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), CreateProfileActivity.class);
                intent.putExtra("IS_EDIT_MODE", true);
                startActivity(intent);
            });
        }

        fetchProfileData();

        return view;
    }

    private void fetchProfileData() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    ProfileModel profile = snapshot.getValue(ProfileModel.class);
                    if (profile != null) {
                        updateUI(profile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(ProfileModel profile) {
        if (profile == null) return;
        
        if (profile.getName() != null) {
            tvNameAge.setText(profile.getName() + (profile.getAge() > 0 ? ", " + profile.getAge() : ""));
        }
        
        if (profile.getBio() != null && !profile.getBio().isEmpty()) {
            tvAboutMe.setText(profile.getBio());
        } else {
            tvAboutMe.setText("No bio provided yet.");
        }
        
        // Show short bio summary (e.g., first interest or role)
        if (profile.getInterests() != null && !profile.getInterests().isEmpty()) {
            tvBioSummary.setText(profile.getInterests().get(0) + " Enthusiast");
        } else {
            tvBioSummary.setText("Spark Member");
        }

        // Load profile image
        if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
            if (ivProfilePic != null) {
                Glide.with(this)
                        .load(profile.getProfileImageUrl())
                        .circleCrop()
                        .placeholder(R.drawable.heart)
                        .error(R.drawable.heart)
                        .into(ivProfilePic);
            }
            if (ivProfilePicSmall != null) {
                Glide.with(this)
                        .load(profile.getProfileImageUrl())
                        .circleCrop()
                        .placeholder(R.drawable.heart)
                        .error(R.drawable.heart)
                        .into(ivProfilePicSmall);
            }
        } else {
            if (ivProfilePic != null) ivProfilePic.setImageResource(R.drawable.heart);
            if (ivProfilePicSmall != null) ivProfilePicSmall.setImageResource(R.drawable.heart);
        }

        // Update interests chips
        if (chipGroupInterests != null) {
            chipGroupInterests.removeAllViews();
            if (profile.getInterests() != null) {
                for (String interest : profile.getInterests()) {
                    Chip chip = new Chip(getContext());
                    chip.setText(interest);
                    chip.setChipBackgroundColorResource(R.color.bg_color);
                    chipGroupInterests.addView(chip);
                }
            }
        }
    }
}