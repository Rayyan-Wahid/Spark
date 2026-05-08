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
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;

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
    private RadioGroup rgGender;
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
                        ivProfilePic.setImageTintList(null);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        rgGender = findViewById(R.id.rg_gender);

        isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);
        if (isEditMode) {
            btnComplete.setText("Save Changes");
            TextView tvTitle = findViewById(R.id.tv_toolbar_title);
            if (tvTitle != null) tvTitle.setText("Edit Profile");
            fetchExistingData();
        }

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

                    if (profile.getGender() != null) {
                        if (profile.getGender().equals("Male")) rgGender.check(R.id.rb_male);
                        else if (profile.getGender().equals("Female")) rgGender.check(R.id.rb_female);
                        else rgGender.check(R.id.rb_other);
                    }

                    if (profile.getInterests() != null) {
                        for (int i = 0; i < chipGroupInterests.getChildCount(); i++) {
                            View child = chipGroupInterests.getChildAt(i);
                            if (child instanceof Chip) {
                                Chip chip = (Chip) child;
                                if (profile.getInterests().contains(chip.getText().toString())) {
                                    chip.setChecked(true);
                                }
                            }
                        }
                    }

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
            addChipToGroup(interest, false);
        }

        Chip addCustomChip = new Chip(this);
        addCustomChip.setText("+ Add Custom");
        addCustomChip.setChipBackgroundColorResource(R.color.bg_color);
        addCustomChip.setTextColor(ContextCompat.getColor(this, R.color.pink_primary));
        addCustomChip.setChipStrokeColorResource(R.color.pink_primary);
        addCustomChip.setChipStrokeWidth(2f);
        addCustomChip.setOnClickListener(v -> showAddCustomInterestDialog());
        chipGroupInterests.addView(addCustomChip);
    }

    private void addChipToGroup(String interest, boolean isChecked) {
        Chip chip = new Chip(this);
        chip.setText(interest);
        chip.setCheckable(true);
        chip.setChecked(isChecked);
        updateChipStyle(chip, isChecked);

        chip.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked) {
                selectedInterests.add(interest);
            } else {
                selectedInterests.remove(interest);
            }
            updateChipStyle(chip, checked);
        });

        int childCount = chipGroupInterests.getChildCount();
        chipGroupInterests.addView(chip, childCount > 0 ? childCount - 1 : 0);
        
        if (isChecked && !selectedInterests.contains(interest)) {
            selectedInterests.add(interest);
        }
    }

    private void updateChipStyle(Chip chip, boolean isChecked) {
        if (isChecked) {
            chip.setChipBackgroundColorResource(R.color.pink_primary);
            chip.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            chip.setChipBackgroundColorResource(R.color.bg_color);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
    }

    private void showAddCustomInterestDialog() {
        EditText etCustomInterest = new EditText(this);
        etCustomInterest.setHint("Enter interest (e.g. Hiking)");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = padding;
        params.rightMargin = padding;
        etCustomInterest.setLayoutParams(params);
        container.addView(etCustomInterest);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Add Custom Interest")
                .setView(container)
                .setPositiveButton("Add", (dialog, which) -> {
                    String custom = etCustomInterest.getText().toString().trim();
                    if (!TextUtils.isEmpty(custom)) {
                        boolean exists = false;
                        for (int i = 0; i < chipGroupInterests.getChildCount(); i++) {
                            View view = chipGroupInterests.getChildAt(i);
                            if (view instanceof Chip) {
                                if (((Chip) view).getText().toString().equalsIgnoreCase(custom)) {
                                    ((Chip) view).setChecked(true);
                                    exists = true;
                                    break;
                                }
                            }
                        }
                        if (!exists) {
                            addChipToGroup(custom, true);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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
            String gender = getSelectedGender();
            if (isEditMode && existingImageUrl != null) {
                saveProfileToDatabase(userId, name, Integer.parseInt(ageStr), bio, existingImageUrl, gender);
            } else {
                Toast.makeText(this, "Please upload a photo", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        showLoading(true);
        String userId = mAuth.getCurrentUser().getUid();

        new Thread(() -> {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                int maxWidth = 1024;
                int maxHeight = 1024;
                if (bitmap.getWidth() > maxWidth || bitmap.getHeight() > maxHeight) {
                    float ratio = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());
                    int width = Math.round(ratio * bitmap.getWidth());
                    int height = Math.round(ratio * bitmap.getHeight());
                    bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                byte[] b = baos.toByteArray();
                String base64Image = "data:image/jpeg;base64," + Base64.encodeToString(b, Base64.NO_WRAP);
                String gender = getSelectedGender();

                runOnUiThread(() -> saveProfileToDatabase(userId, name, Integer.parseInt(ageStr), bio, base64Image, gender));

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

    private void saveProfileToDatabase(String userId, String name, int age, String bio, String imageUrl, String gender) {
        boolean isPublic = switchPublic.isChecked();
        ProfileModel profile = new ProfileModel(userId, name, age, bio, selectedInterests, isPublic, imageUrl, gender);
        profile.setProfileCompleted(true);

        mDatabase.child("users").child(userId).setValue(profile)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        // Write location to GeoFire (only if lat/lng were seeded for this user)
                        mDatabase.child("users").child(userId).child("latitude")
                                .get().addOnSuccessListener(latSnap -> {
                            mDatabase.child("users").child(userId).child("longitude")
                                    .get().addOnSuccessListener(lngSnap -> {
                                Double lat = latSnap.getValue(Double.class);
                                Double lng = lngSnap.getValue(Double.class);
                                if (lat != null && lng != null) {
                                    DatabaseReference geoFireRef =
                                            FirebaseDatabase.getInstance().getReference("geofire");
                                    GeoFire geoFire = new GeoFire(geoFireRef);
                                    geoFire.setLocation(userId, new GeoLocation(lat, lng),
                                            (key, error) -> {
                                                if (error != null) {
                                                    android.util.Log.e("GeoFire",
                                                            "GeoFire write failed: " + error.getMessage());
                                                } else {
                                                    android.util.Log.d("GeoFire",
                                                            "Location saved for " + key);
                                                }
                                            });
                                }
                            });
                        });

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

    private String getSelectedGender() {
        int selectedId = rgGender.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_male) return "Male";
        if (selectedId == R.id.rb_female) return "Female";
        return "Prefer not to say";
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