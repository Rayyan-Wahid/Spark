package com.example.spark;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CreateProfileActivity extends AppCompatActivity {

    private EditText etFullName, etAge, etBio;
    private TextView tvBioCount;
    private ChipGroup chipGroupInterests;
    private SwitchMaterial switchPublic;
    private MaterialButton btnComplete;
    private ImageView ivProfilePic;
    private FloatingActionButton btnAddPhoto;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private Uri selectedImageUri;

    private ProgressBar progressBar;
    private boolean isEditMode = false;
    private String existingImageUrl = null;

    private String[] commonInterests = {"Music", "Art", "Travel", "Dogs", "Coffee", "Cooking", "Photography", "Design", "Gaming", "Sports"};
    private List<String> selectedInterests = new ArrayList<>();

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        ivProfilePic.setImageURI(selectedImageUri);
                        ivProfilePic.setPadding(0, 0, 0, 0);
                        ivProfilePic.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        ivProfilePic.setImageTintList(null); // Remove any tint
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide System UI for Immersive Mode
        hideSystemUI();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize UI
        etFullName = findViewById(R.id.et_full_name);
        etAge = findViewById(R.id.et_age);
        etBio = findViewById(R.id.et_bio);
        tvBioCount = findViewById(R.id.tv_bio_count);
        chipGroupInterests = findViewById(R.id.chip_group_interests);
        switchPublic = findViewById(R.id.switch_public);
        btnComplete = findViewById(R.id.btn_complete_profile);
        ivProfilePic = findViewById(R.id.iv_profile_pic);
        btnAddPhoto = findViewById(R.id.btn_add_photo);
        progressBar = findViewById(R.id.progress_bar);

        isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);
        if (isEditMode) {
            btnComplete.setText("Save Changes");
            TextView tvTitle = findViewById(R.id.tv_toolbar_title);
            if (tvTitle != null) tvTitle.setText("Edit Profile");
            fetchExistingData();
        }

        // Pre-fill name if available
        if (mAuth.getCurrentUser() != null && !isEditMode) {
            etFullName.setText(mAuth.getCurrentUser().getDisplayName());
        }

        setupInterests();
        setupBioCounter();

        btnAddPhoto.setOnClickListener(v -> pickImage());
        btnComplete.setOnClickListener(v -> processAndSaveProfile());

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void fetchExistingData() {
        String userId = mAuth.getCurrentUser().getUid();
        showLoading(true);
        mDatabase.child("users").child(userId).get().addOnSuccessListener(snapshot -> {
            showLoading(false);
            if (snapshot.exists()) {
                ProfileModel profile = snapshot.getValue(ProfileModel.class);
                if (profile != null) {
                    etFullName.setText(profile.getName());
                    etAge.setText(String.valueOf(profile.getAge()));
                    etBio.setText(profile.getBio());
                    switchPublic.setChecked(profile.isPublic());
                    existingImageUrl = profile.getProfileImageUrl();

                    // Pre-select interests
                    if (profile.getInterests() != null) {
                        for (int i = 0; i < chipGroupInterests.getChildCount(); i++) {
                            Chip chip = (Chip) chipGroupInterests.getChildAt(i);
                            if (profile.getInterests().contains(chip.getText().toString())) {
                                chip.setChecked(true);
                            }
                        }
                    }

                    // Load image
                    if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
                        com.bumptech.glide.Glide.with(this)
                                .load(existingImageUrl)
                                .centerCrop()
                                .into(ivProfilePic);
                        ivProfilePic.setPadding(0, 0, 0, 0);
                        ivProfilePic.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        ivProfilePic.setImageTintList(null);
                    }
                }
            }
        }).addOnFailureListener(e -> {
            showLoading(false);
            Toast.makeText(this, "Failed to load existing profile", Toast.LENGTH_SHORT).show();
        });
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void setupInterests() {
        for (String interest : commonInterests) {
            Chip chip = new Chip(this);
            chip.setText(interest);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.bg_color);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedInterests.add(interest);
                    chip.setChipBackgroundColorResource(R.color.pink_primary);
                    chip.setTextColor(ContextCompat.getColor(this, R.color.white));
                } else {
                    selectedInterests.remove(interest);
                    chip.setChipBackgroundColorResource(R.color.bg_color);
                    chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                }
            });
            chipGroupInterests.addView(chip);
        }
    }

    private void setupBioCounter() {
        etBio.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvBioCount.setText(s.length() + "/250");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void processAndSaveProfile() {
        String name = etFullName.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etFullName.setError("Required");
            return;
        }

        if (TextUtils.isEmpty(ageStr)) {
            etAge.setError("Required");
            return;
        }

        if (selectedImageUri == null) {
            String userId = mAuth.getCurrentUser().getUid();
            if (isEditMode && existingImageUrl != null) {
                saveProfileToDatabase(userId, name, Integer.parseInt(ageStr), bio, existingImageUrl);
            } else {
                Toast.makeText(this, "Please upload a photo", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        showLoading(true);
        String userId = mAuth.getCurrentUser().getUid();

        // Run in background to avoid blocking UI thread
        new Thread(() -> {
            try {
                // Convert URI to Bitmap and compress
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                
                // Resize if too large
                int maxWidth = 400;
                int maxHeight = 400;
                if (bitmap.getWidth() > maxWidth || bitmap.getHeight() > maxHeight) {
                    float ratio = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());
                    int width = Math.round(ratio * bitmap.getWidth());
                    int height = Math.round(ratio * bitmap.getHeight());
                    bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos); // 70% quality for smaller size
                byte[] b = baos.toByteArray();
                String base64Image = "data:image/jpeg;base64," + Base64.encodeToString(b, Base64.NO_WRAP);

                runOnUiThread(() -> saveProfileToDatabase(userId, name, Integer.parseInt(ageStr), bio, base64Image));

            } catch (IOException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(CreateProfileActivity.this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        btnComplete.setEnabled(!loading);
        btnAddPhoto.setEnabled(!loading);
    }

    private void saveProfileToDatabase(String userId, String name, int age, String bio, String imageUrl) {
        boolean isPublic = switchPublic.isChecked();
        
        // Creating the full profile model with all info and UID
        ProfileModel profile = new ProfileModel(userId, name, age, bio, selectedInterests, isPublic, imageUrl);
        profile.setProfileCompleted(true);

        mDatabase.child("users").child(userId).setValue(profile)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        String msg = isEditMode ? "Profile Updated Successfully!" : "Profile Completed Successfully!";
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        
                        if (!isEditMode) {
                            Intent intent = new Intent(CreateProfileActivity.this, DashboardActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                        finish();
                    } else {
                        Toast.makeText(this, "Database Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
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