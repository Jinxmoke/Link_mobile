package com.example.link;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class PermissionActivity extends AppCompatActivity {

    private static final String TAG = "PermissionActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private MaterialButton btnGrantPermissions;
    private TextView btnSkipForNow; // FIXED: Changed from Button to TextView
    private TextView tvPermissionStatus;
    private SharedPrefManager sharedPrefManager;
    private boolean isFromMainActivity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate started");

        try {
            // Set status bar color to purple
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                try {
                    // Try to get color from resources
                    int color = getResources().getColor(R.color.purple_700);
                    window.setStatusBarColor(color);
                } catch (Exception e) {
                    // Fallback color
                    window.setStatusBarColor(0xFF3700B3); // Purple 700
                }
            }

            setContentView(R.layout.activity_permission);
            Log.d(TAG, "Layout inflated");

            sharedPrefManager = SharedPrefManager.getInstance(this);

            // Check if coming from MainActivity
            if (getIntent() != null && getIntent().hasExtra("from_main")) {
                isFromMainActivity = true;
                Log.d(TAG, "Coming from MainActivity");
            }

            // Initialize UI - FIXED: Match types with XML
            btnGrantPermissions = findViewById(R.id.btn_grant_permissions);
            Log.d(TAG, "btnGrantPermissions found: " + (btnGrantPermissions != null));

            btnSkipForNow = findViewById(R.id.btn_skip_for_now); // This is TextView in XML
            Log.d(TAG, "btnSkipForNow found: " + (btnSkipForNow != null));
            Log.d(TAG, "btnSkipForNow class: " + (btnSkipForNow != null ? btnSkipForNow.getClass().getName() : "null"));

            tvPermissionStatus = findViewById(R.id.tv_permission_status);
            Log.d(TAG, "tvPermissionStatus found: " + (tvPermissionStatus != null));

            // Check current permission status
            updatePermissionStatus();

            // Check if all permissions are already granted
            if (allPermissionsGranted()) {
                Log.d(TAG, "All permissions already granted, proceeding to next activity");
                proceedToNextActivity();
                return;
            }

            setupClickListeners();
            Log.d(TAG, "Click listeners setup complete");

            // Mark permission screen as shown
            sharedPrefManager.setPermissionScreenShown();
            sharedPrefManager.setFirstTimeLaunchCompleted();
            Log.d(TAG, "Permission screen marked as shown");

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing permissions screen", Toast.LENGTH_SHORT).show();
            // Try to proceed anyway
            proceedToNextActivity();
        }
    }

    private void setupClickListeners() {
        if (btnGrantPermissions != null) {
            btnGrantPermissions.setOnClickListener(v -> {
                Log.d(TAG, "Grant Permissions button clicked");
                requestPermissions();
            });
        } else {
            Log.e(TAG, "btnGrantPermissions is null!");
        }

        if (btnSkipForNow != null) {
            btnSkipForNow.setOnClickListener(v -> {
                Log.d(TAG, "Skip button clicked");
                if (sharedPrefManager != null) {
                    sharedPrefManager.setPermissionsRequested(true);
                    sharedPrefManager.incrementPermissionDeniedCount();
                }
                showSkipWarning();
            });
        } else {
            Log.e(TAG, "btnSkipForNow is null!");
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        Log.d(TAG, "Requesting permissions");

        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
                Log.d(TAG, "Need permission: " + permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            Log.d(TAG, "Requesting " + permissionsToRequest.size() + " permissions");
            ActivityCompat.requestPermissions(this,
                permissionsToRequest.toArray(new String[0]),
                PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "All permissions already granted");
            Toast.makeText(this, "All permissions already granted", Toast.LENGTH_SHORT).show();
            proceedToNextActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: requestCode=" + requestCode);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            boolean anyGranted = false;

            Log.d(TAG, "Processing " + permissions.length + " permission results");

            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                Log.d(TAG, "Permission " + permission + ": " + (granted ? "GRANTED" : "DENIED"));

                // Update permission status
                if (permission.equals(Manifest.permission.CAMERA)) {
                    sharedPrefManager.setCameraPermissionGranted(granted);
                } else if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) ||
                    permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    sharedPrefManager.setLocationPermissionGranted(granted);
                }

                if (!granted) {
                    allGranted = false;
                } else {
                    anyGranted = true;
                }
            }

            sharedPrefManager.setPermissionsRequested(true);

            if (allGranted) {
                Log.d(TAG, "All permissions granted");
                sharedPrefManager.resetPermissionDeniedCount();
                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
                proceedToNextActivity();
            } else if (anyGranted) {
                Log.d(TAG, "Some permissions granted, some denied");
                sharedPrefManager.incrementPermissionDeniedCount();
                Toast.makeText(this, "Some permissions were granted", Toast.LENGTH_SHORT).show();
                updatePermissionStatus();

                // Ask again for missing permissions
                showMissingPermissionDialog();
            } else {
                Log.d(TAG, "All permissions denied");
                sharedPrefManager.incrementPermissionDeniedCount();
                Toast.makeText(this, "Permissions were denied", Toast.LENGTH_LONG).show();
                updatePermissionStatus();

                // Show explanation
                showPermissionExplanationDialog();
            }
        }
    }

    private void updatePermissionStatus() {
        if (tvPermissionStatus == null) return;

        StringBuilder status = new StringBuilder("Permission Status:\n");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            status.append("✓ Camera\n");
        } else {
            status.append("✗ Camera\n");
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            status.append("✓ Location (Fine)\n");
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            status.append("✓ Location (Coarse)\n");
        } else {
            status.append("✗ Location\n");
        }

        tvPermissionStatus.setText(status.toString());
        Log.d(TAG, "Updated permission status");
    }

    private void showMissingPermissionDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Additional Permissions Needed")
            .setMessage("Some permissions are still needed for full functionality. Do you want to grant them now?")
            .setPositiveButton("Grant Missing", (dialog, which) -> {
                Log.d(TAG, "User chose to grant missing permissions");
                requestPermissions();
            })
            .setNegativeButton("Skip", (dialog, which) -> {
                Log.d(TAG, "User chose to skip");
                proceedToNextActivity();
            })
            .setCancelable(false)
            .show();
    }

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("For the best emergency response experience, this app needs:\n\n" +
                "• Camera: For emergency photo documentation\n" +
                "• Location: For accurate emergency response mapping\n\n" +
                "You can grant permissions from Settings if you change your mind.")
            .setPositiveButton("Grant Again", (dialog, which) -> {
                Log.d(TAG, "User chose to grant permissions again");
                requestPermissions();
            })
            .setNegativeButton("Skip For Now", (dialog, which) -> {
                Log.d(TAG, "User chose to skip for now");
                proceedToNextActivity();
            })
            .setCancelable(false)
            .show();
    }

    private void showSkipWarning() {
        new AlertDialog.Builder(this)
            .setTitle("Warning")
            .setMessage("Some features may not work properly without permissions:\n\n" +
                "• Without Camera: Cannot take emergency photos\n" +
                "• Without Location: Cannot show your exact location on map\n\n" +
                "Are you sure you want to continue?")
            .setPositiveButton("Yes, Continue", (dialog, which) -> {
                Log.d(TAG, "User confirmed skipping permissions");
                proceedToNextActivity();
            })
            .setNegativeButton("Go Back", (dialog, which) -> {
                Log.d(TAG, "User went back to permission screen");
            })
            .setCancelable(false)
            .show();
    }

    private void proceedToNextActivity() {
        Log.d(TAG, "Proceeding to next activity, isFromMainActivity: " + isFromMainActivity);

        // Save current permission states
        if (sharedPrefManager != null) {
            sharedPrefManager.saveCurrentPermissionStates();
        }

        Intent intent;

        if (isFromMainActivity) {
            // If coming from MainActivity, go back
            Log.d(TAG, "Going back to MainActivity");
            intent = new Intent(this, MainActivity.class);
        } else {
            // If fresh launch, go to Login or Main based on login status
            if (sharedPrefManager != null && sharedPrefManager.isLoggedIn()) {
                Log.d(TAG, "User is logged in, going to MainActivity");
                intent = new Intent(this, MainActivity.class);
            } else {
                Log.d(TAG, "User is not logged in, going to LoginActivity");
                intent = new Intent(this, LoginActivity.class);
            }
        }

        try {
            startActivity(intent);
            finish();
            Log.d(TAG, "Activity started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error starting activity: " + e.getMessage(), e);
            Toast.makeText(this, "Error starting application", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button pressed, isFromMainActivity: " + isFromMainActivity);

        if (isFromMainActivity) {
            // If coming from MainActivity, allow back to Main
            Log.d(TAG, "Allowing back to MainActivity");
            proceedToNextActivity();
        } else {
            // If fresh launch, ask about exiting
            new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit? Some features may not work without permissions.")
                .setPositiveButton("Exit", (dialog, which) -> {
                    Log.d(TAG, "User chose to exit app");
                    finish();
                })
                .setNegativeButton("Stay", (dialog, which) -> {
                    Log.d(TAG, "User chose to stay");
                })
                .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}
