package com.example.spark;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FrameLayout navHome, navDiscover, navMessages, navLikes, navProfile;
    private View homeIndicator, discoverIndicator, messagesIndicator, likesIndicator, profileIndicator;
    private ImageView homeIcon, discoverIcon, messagesIcon, likesIcon, profileIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide System UI for Immersive Mode
        hideSystemUI();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            redirectToAuth();
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dashboard_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0); // Bottom padding handled by card
            return insets;
        });

        // Initialize UI components
        navHome = findViewById(R.id.nav_home);
        navDiscover = findViewById(R.id.nav_discover);
        navMessages = findViewById(R.id.nav_messages);
        navLikes = findViewById(R.id.nav_likes);
        navProfile = findViewById(R.id.nav_profile);

        homeIndicator = findViewById(R.id.home_indicator);
        discoverIndicator = findViewById(R.id.discover_indicator);
        messagesIndicator = findViewById(R.id.messages_indicator);
        likesIndicator = findViewById(R.id.likes_indicator);
        profileIndicator = findViewById(R.id.profile_indicator);

        homeIcon = findViewById(R.id.home_icon);
        discoverIcon = findViewById(R.id.discover_icon);
        messagesIcon = findViewById(R.id.messages_icon);
        likesIcon = findViewById(R.id.likes_icon);
        profileIcon = findViewById(R.id.profile_icon);

        // Default fragment
        loadFragment(new HomeFragment(), "HOME");

        // Click listeners
        navHome.setOnClickListener(v -> selectTab("HOME"));
        navDiscover.setOnClickListener(v -> selectTab("DISCOVER"));
        navMessages.setOnClickListener(v -> selectTab("MESSAGES"));
        navLikes.setOnClickListener(v -> selectTab("LIKES"));
        navProfile.setOnClickListener(v -> selectTab("PROFILE"));
    }

    private void selectTab(String tab) {
        // Reset all
        homeIndicator.setVisibility(View.GONE);
        discoverIndicator.setVisibility(View.GONE);
        messagesIndicator.setVisibility(View.GONE);
        likesIndicator.setVisibility(View.GONE);
        profileIndicator.setVisibility(View.GONE);

        homeIcon.setColorFilter(getResources().getColor(R.color.text_secondary));
        discoverIcon.setColorFilter(getResources().getColor(R.color.text_secondary));
        messagesIcon.setColorFilter(getResources().getColor(R.color.text_secondary));
        likesIcon.setColorFilter(getResources().getColor(R.color.text_secondary));
        profileIcon.setColorFilter(getResources().getColor(R.color.text_secondary));

        Fragment fragment = null;
        switch (tab) {
            case "HOME":
                homeIndicator.setVisibility(View.VISIBLE);
                homeIcon.setColorFilter(getResources().getColor(R.color.white));
                fragment = new HomeFragment();
                break;
            case "DISCOVER":
                discoverIndicator.setVisibility(View.VISIBLE);
                discoverIcon.setColorFilter(getResources().getColor(R.color.white));
                fragment = new DiscoverFragment();
                break;
            case "MESSAGES":
                messagesIndicator.setVisibility(View.VISIBLE);
                messagesIcon.setColorFilter(getResources().getColor(R.color.white));
                fragment = new MessagesFragment();
                break;
            case "LIKES":
                likesIndicator.setVisibility(View.VISIBLE);
                likesIcon.setColorFilter(getResources().getColor(R.color.white));
                fragment = new LikesFragment();
                break;
            case "PROFILE":
                profileIndicator.setVisibility(View.VISIBLE);
                profileIcon.setColorFilter(getResources().getColor(R.color.white));
                fragment = new ProfileFragment();
                break;
        }

        if (fragment != null) {
            loadFragment(fragment, tab);
        }
    }

    private void loadFragment(Fragment fragment, String tag) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        
        // Add fade transition for "excellent and modern" look
        ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        ft.replace(R.id.nav_host_fragment, fragment, tag);
        ft.commit();
    }

    private void redirectToAuth() {
        Intent intent = new Intent(DashboardActivity.this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
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

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}