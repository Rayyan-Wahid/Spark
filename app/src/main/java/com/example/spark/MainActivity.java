package com.example.spark;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is already logged in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // User is signed in
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
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
            btnGetStarted.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, AuthActivity.class));
            });
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

        // Setup Creative Gradient for Spark Text
        android.widget.TextView appNameText = findViewById(R.id.app_name_text);
        
        if (appNameText != null) {
            appNameText.post(() -> applyGradient(appNameText));
        }
    }

    private void applyGradient(android.widget.TextView textView) {
        android.graphics.Shader textShader = new android.graphics.LinearGradient(
                0, 0, textView.getPaint().measureText(textView.getText().toString()), textView.getTextSize(),
                new int[]{
                        getResources().getColor(R.color.pink_primary, getTheme()),
                        getResources().getColor(R.color.purple_outline, getTheme())
                },
                new float[]{0f, 1f},
                android.graphics.Shader.TileMode.CLAMP);
        textView.getPaint().setShader(textShader);
        textView.invalidate();
    }
}