package com.example.spark;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private TextView titleText;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide System UI for Immersive Mode
        hideSystemUI();

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // If already logged in, skip onboarding and go straight to dashboard
        if (mAuth.getCurrentUser() != null) {
            Intent dashboardIntent = new Intent(MainActivity.this, DashboardActivity.class);
            dashboardIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(dashboardIntent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        titleText = findViewById(R.id.title_text);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup Animations
        LinearLayout logoContainer = findViewById(R.id.logo_container);
        LinearLayout bottomContainer = findViewById(R.id.bottom_container);
        MaterialButton btnGetStarted = findViewById(R.id.btn_get_started);

        if (btnGetStarted != null) {
            btnGetStarted.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, AuthActivity.class)));
        }

        Animation zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        Animation fadeInUp = AnimationUtils.loadAnimation(this, R.anim.fade_in_up);

        // Apply animations
        if (logoContainer != null) {
            logoContainer.startAnimation(zoomIn);
        }
        
        if (bottomContainer != null) {
            bottomContainer.startAnimation(fadeInUp);
        }

        // Setup Heart Pounding Animation
        android.widget.ImageView heartImage = findViewById(R.id.heart_image);
        Animation pound = AnimationUtils.loadAnimation(this, R.anim.pound);

        zoomIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (heartImage != null) {
                    heartImage.startAnimation(pound);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });


        TextView appNameText = findViewById(R.id.app_name_text);
        
        if (appNameText != null) {
            appNameText.post(() -> applyGradient(appNameText));
        }
    }

    private void fetchAndDisplayUserName() {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    if (name != null && !name.isEmpty()) {
                        titleText.setText("Welcome,\n" + name + "!");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void applyGradient(android.widget.TextView textView) {
        android.graphics.Shader textShader = new android.graphics.LinearGradient(
                0, 0, textView.getPaint().measureText(textView.getText().toString()), textView.getTextSize(),
                new int[]{
                        getResources().getColor(R.color.pink_primary, getTheme()),
                        getResources().getColor(R.color.pink_light, getTheme())
                },
                new float[]{0f, 1f},
                android.graphics.Shader.TileMode.CLAMP);
        textView.getPaint().setShader(textShader);
        textView.invalidate();
    }

    private void hideSystemUI() {
        WindowInsetsControllerCompat windowInsetsController =
                ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (windowInsetsController != null) {
            // Configure the behavior of the hidden system bars
            windowInsetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
            // Hide both the status bar and the navigation bar
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        }
    }
}