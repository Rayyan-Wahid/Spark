package com.example.spark;

import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AuthActivity extends AppCompatActivity {

    private LinearLayout authContainer;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private TextView sparkText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Hide System UI for Immersive Mode
        hideSystemUI();
        setContentView(R.layout.activity_auth);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.auth_container).getParent() instanceof android.view.View ? (android.view.View) findViewById(R.id.auth_container).getParent() : null, (v, insets) -> {
            if (v == null) return insets;
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        authContainer = findViewById(R.id.auth_container);
        viewPager = findViewById(R.id.auth_view_pager);
        tabLayout = findViewById(R.id.auth_tab_layout);
        sparkText = findViewById(R.id.auth_spark_text);

        // Setup ViewPager2
        AuthPagerAdapter pagerAdapter = new AuthPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Setup TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Login");
            } else {
                tab.setText("Sign Up");
            }
        }).attach();

        // Apply Gradient to Spark Text
        applyGradient(sparkText);

        // Animation: Container comes from below
        authContainer.post(() -> {
            authContainer.setTranslationY(authContainer.getHeight());
            authContainer.animate()
                    .translationY(0)
                    .setDuration(600)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        });
    }

    private void applyGradient(TextView textView) {
        if (textView == null) return;
        textView.post(() -> {
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