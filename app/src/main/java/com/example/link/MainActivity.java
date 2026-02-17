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
import android.widget.Toast;

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
                subscribeToTopics();
            } else {
                Log.d(TAG, "Notification permission denied");
                Toast.makeText(this, "Notification permission is required for alerts", Toast.LENGTH_LONG).show();
                subscribeToTopics();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermissions();
        setContentView(R.layout.activity_main);
        setupSystemUI();
        setupBottomNavInsets();
        requestNotificationPermission();

        if (savedInstanceState == null) {
            Intent launchIntent = getIntent();
            boolean isAlert = isAlertIntent(launchIntent);

            // ── THE ROOT-CAUSE FIX ────────────────────────────────────────────
            // BottomNavigationFragment.onViewCreated() always calls
            //   replace(R.id.mapContainer, new TestFragment())
            // which runs AFTER handleNotificationIntent() and overwrites the
            // MapFragment we just pushed.  We pass a boolean arg so it can
            // skip that replace when we are doing a cold-start from a notification.
            // ─────────────────────────────────────────────────────────────────
            loadBottomNavigation(isAlert);   // isAlert=true → BottomNav skips TestFragment

            if (isAlert) {
                handleNotificationIntent(launchIntent);
            }
            // When isAlert==false BottomNavigationFragment loads TestFragment itself.
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Returns true when the intent is an SOS / geofence tap
    // ─────────────────────────────────────────────────────────
    private boolean isAlertIntent(Intent intent) {
        if (intent == null) return false;
        String action = intent.getAction();
        return "SOS_ALERT_CLICK".equals(action)
            || "SOS_VIEW_LOCATION".equals(action)
            || "GEOFENCE_ALERT_CLICK".equals(action)
            || "GEOFENCE_VIEW_LOCATION".equals(action);
    }

    private void checkPermissions() {
        sharedPrefManager = SharedPrefManager.getInstance(this);
        if (sharedPrefManager.shouldShowPermissionScreen()) {
            Intent intent = new Intent(this, PermissionActivity.class);
            intent.putExtra("from_main", true);
            startActivity(intent);
            finish();
            return;
        }
        if (sharedPrefManager.areCriticalPermissionsMissing()) {
            Log.w(TAG, "Critical permissions missing, app may not function properly");
        }
    }

    private void setupSystemUI() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        } else {
            window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
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

    /**
     * @param skipHome  true  = launched from a notification; tell BottomNav NOT
     *                          to load TestFragment so MapFragment isn't overwritten.
     *                  false = normal launch; BottomNav loads TestFragment as usual.
     */
    private void loadBottomNavigation(boolean skipHome) {
        Bundle args = new Bundle();
        args.putBoolean("skipHome", skipHome);

        BottomNavigationFragment bnf = new BottomNavigationFragment();
        bnf.setArguments(args);

        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.bottomNavContainer, bnf)
            .commit();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                subscribeToTopics();
            }
        } else {
            subscribeToTopics();
        }
    }

    private void subscribeToTopics() {
        FirebaseMessaging.getInstance().subscribeToTopic("sos_alerts")
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) Log.d(TAG, "✓ Subscribed to sos_alerts");
                else Log.e(TAG, "✗ Failed sos_alerts", task.getException());
            });

        FirebaseMessaging.getInstance().subscribeToTopic("geofence_alerts")
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) Log.d(TAG, "✓ Subscribed to geofence_alerts");
                else Log.e(TAG, "✗ Failed geofence_alerts", task.getException());
            });

        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    Log.d(TAG, "FCM Token: " + task.getResult());
                } else {
                    Log.e(TAG, "Failed to get FCM token", task.getException());
                }
            });
    }

    // ─────────────────────────────────────────────────────────
    //  App already running — notification tapped (singleTop)
    // ─────────────────────────────────────────────────────────
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (action == null) return;

        Log.d(TAG, "Notification clicked! Action: " + action);

        if ("SOS_ALERT_CLICK".equals(action) || "SOS_VIEW_LOCATION".equals(action)) {
            Bundle bundle = new Bundle();
            bundle.putString("latitude",           intent.getStringExtra("latitude"));
            bundle.putString("longitude",          intent.getStringExtra("longitude"));
            bundle.putString("transmitter_serial", intent.getStringExtra("transmitter_serial"));
            bundle.putString("assignment_id",      intent.getStringExtra("assignment_id"));
            bundle.putString("assigned_name",      intent.getStringExtra("assigned_name"));
            bundle.putString("alertType",          "sos");

            navigateToMapFragment(bundle);
            Toast.makeText(this,
                "SOS Alert from: " + intent.getStringExtra("assigned_name"),
                Toast.LENGTH_LONG).show();

        } else if ("GEOFENCE_ALERT_CLICK".equals(action) || "GEOFENCE_VIEW_LOCATION".equals(action)) {
            Bundle bundle = new Bundle();
            bundle.putString("latitude",           intent.getStringExtra("latitude"));
            bundle.putString("longitude",          intent.getStringExtra("longitude"));
            bundle.putString("transmitter_serial", intent.getStringExtra("transmitter_serial"));
            bundle.putString("assignment_id",      intent.getStringExtra("assignment_id"));
            bundle.putString("assigned_name",      intent.getStringExtra("assigned_name"));
            bundle.putString("distance_from_base", intent.getStringExtra("distance_from_base"));
            bundle.putString("base_station_name",  intent.getStringExtra("base_station_name"));
            bundle.putString("alertType",          "geofence");

            navigateToMapFragment(bundle);
            Toast.makeText(this,
                "Geofence Alert: " + intent.getStringExtra("assigned_name"),
                Toast.LENGTH_LONG).show();
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Push MapFragment (with alert bundle) into mapContainer
    // ─────────────────────────────────────────────────────────
    private void navigateToMapFragment(Bundle data) {
        Log.d(TAG, "Navigating to MapFragment with alert data");

        // Highlight the Map tab in the bottom bar
        androidx.fragment.app.Fragment navFrag =
            getSupportFragmentManager().findFragmentById(R.id.bottomNavContainer);
        if (navFrag instanceof BottomNavigationFragment) {
            ((BottomNavigationFragment) navFrag).selectMapTab();
        }

        MapFragment mapFragment = new MapFragment();
        mapFragment.setArguments(data);

        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.mapContainer, mapFragment, "MAP_FRAGMENT")
            .addToBackStack("alert_navigation")
            .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sharedPrefManager == null) {
            sharedPrefManager = SharedPrefManager.getInstance(this);
        }
        if (sharedPrefManager.shouldShowPermissionScreen()) {
            startActivity(new Intent(this, PermissionActivity.class));
            finish();
        }
    }
}
