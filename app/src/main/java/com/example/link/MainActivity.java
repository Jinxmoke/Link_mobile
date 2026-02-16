package com.example.link;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SharedPrefManager sharedPrefManager;

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Log.d(TAG, "Notification permission granted");
                setupFirebaseMessaging();
            } else {
                Log.d(TAG, "Notification permission denied");
                setupFirebaseMessaging();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if permissions are required before showing main app
        checkPermissions();

        setContentView(R.layout.activity_main);

        setupSystemUI();
        setupBottomNavInsets();
        loadDefaultFragment(savedInstanceState);
        loadBottomNavigation();
        requestNotificationPermission();
    }

    private void checkPermissions() {
        sharedPrefManager = SharedPrefManager.getInstance(this);

        // Check if we need to show permission screen
        if (sharedPrefManager.shouldShowPermissionScreen()) {
            // Redirect to PermissionActivity
            Intent intent = new Intent(this, PermissionActivity.class);
            intent.putExtra("from_main", true); // Flag to indicate coming from MainActivity
            startActivity(intent);
            finish(); // Close MainActivity
            return;
        }

        // If permissions are missing but user skipped, show warning
        if (sharedPrefManager.areCriticalPermissionsMissing()) {
            Log.w(TAG, "Critical permissions missing, app may not function properly");
            // You could show a warning dialog here
        }
    }

    private void setupSystemUI() {
        Window window = getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        } else {
            window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        window.setNavigationBarColor(Color.WHITE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int flags = window.getDecorView().getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void setupBottomNavInsets() {
        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            View bottomNav = findViewById(R.id.bottomNavContainer);
            if (bottomNav != null) {
                bottomNav.setPadding(0, 0, 0, bottomInset);
            }
            return insets;
        });
    }

    private void loadDefaultFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mapContainer, new TestFragment())
                .commit();
        }
    }

    private void loadBottomNavigation() {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.bottomNavContainer, new BottomNavigationFragment())
            .commit();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting notification permission...");
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Log.d(TAG, "Notification permission already granted");
                setupFirebaseMessaging();
            }
        } else {
            Log.d(TAG, "Android version < 13, no permission needed");
            setupFirebaseMessaging();
        }
    }

    private void setupFirebaseMessaging() {
        Log.d(TAG, "Setting up Firebase Messaging...");

        // Subscribe to SOS alerts topic
        FirebaseMessaging.getInstance().subscribeToTopic("sos_alerts")
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "✓ Successfully subscribed to 'sos_alerts' topic");
                } else {
                    Log.e(TAG, "✗ Failed to subscribe to 'sos_alerts' topic", task.getException());
                }
            });

        // Get FCM token for debugging/logging
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String token = task.getResult();
                    Log.d(TAG, "═══════════════════════════════════════");
                    Log.d(TAG, "FCM Token (Device): " + token);
                    Log.d(TAG, "═══════════════════════════════════════");
                } else {
                    Log.e(TAG, "Failed to get FCM token", task.getException());
                }
            });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check permissions again when returning to app
        if (sharedPrefManager == null) {
            sharedPrefManager = SharedPrefManager.getInstance(this);
        }

        // If permissions were granted while app was in background
        if (sharedPrefManager.shouldShowPermissionScreen()) {
            Intent intent = new Intent(this, PermissionActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
