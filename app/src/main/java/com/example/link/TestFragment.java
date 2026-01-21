package com.example.link;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class TestFragment extends Fragment implements LocationListener {

    // UI Components
    private TextView activeDevicesCount;
    private TextView activeSuccessfulCount;
    private LinearLayout historyLayout;
    private LinearLayout settingsLayout;
    private LinearLayout customersLayout;
    private LinearLayout activitiesLayout;
    private TextView seeAllText;
    private Button acknowledgeButton;

    // Profile Header Components
    private TextView welcomeText;
    private TextView locationText;

    // Location
    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;

    // SharedPreferences keys
    private static final String PREFS_NAME = "LinkPrefs";
    private static final String KEY_STAFF_NAME = "staff_name";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_test, container, false);

        initializeViews(view);
        setupProfileHeader();
        setupClickListeners();
        loadData();

        // Request location permission
        requestLocationPermission();

        return view;
    }

    private void initializeViews(View view) {
        // Dashboard counters
        activeDevicesCount = view.findViewById(R.id.activeDevicesCount);
        activeSuccessfulCount = view.findViewById(R.id.activeSuccesfulCount);

        // Quick Action Cards
        historyLayout = view.findViewById(R.id.historyLayout);
        settingsLayout = view.findViewById(R.id.settingsLayout);
        customersLayout = view.findViewById(R.id.customersLayout);
        activitiesLayout = view.findViewById(R.id.activitiesLayout);

        // Optional elements
        seeAllText = view.findViewById(R.id.seeAllText);
        acknowledgeButton = view.findViewById(R.id.acknowledgeButton);

        // Profile Header Views
        welcomeText = view.findViewById(R.id.welcomeText);
        locationText = view.findViewById(R.id.locationText);

        // If the TextView IDs don't exist in your XML, you'll need to add them
        // For now, let's find them by their current IDs in your XML
        // In your XML, the profile section doesn't have IDs for TextViews, so let's add them:

        // Temporary fix - we'll get references from the existing structure
        // Let's update the XML first, then come back here
    }

    private void setupProfileHeader() {
        // Get user data from SharedPreferences
        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Get staff name (preferred) or username as fallback
        String staffName = prefs.getString(KEY_STAFF_NAME, "");
        String username = prefs.getString(KEY_USERNAME, "");
        String email = prefs.getString(KEY_EMAIL, "");

        // Determine what name to display
        String displayName;
        if (staffName != null && !staffName.isEmpty()) {
            displayName = staffName;
        } else if (username != null && !username.isEmpty()) {
            displayName = username;
        } else if (email != null && !email.isEmpty()) {
            // Extract name from email (before @)
            displayName = email.split("@")[0];
        } else {
            displayName = "Staff Member";
        }

        // Update welcome text
        if (welcomeText != null) {
            welcomeText.setText("Welcome, " + displayName);
        }

        // Update location text (will be updated when we get location)
        if (locationText != null) {
            if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                locationText.setText(String.format("%.5f, %.5f", currentLatitude, currentLongitude));
            } else {
                locationText.setText("Getting location...");
            }
        }
    }

    private void setupClickListeners() {
        if (historyLayout != null) {
            historyLayout.setOnClickListener(v -> onHistoryClicked());
        }

        if (settingsLayout != null) {
            settingsLayout.setOnClickListener(v -> onSettingsClicked());
        }

        if (customersLayout != null) {
            customersLayout.setOnClickListener(v -> onCustomersClicked());
        }

        if (activitiesLayout != null) {
            activitiesLayout.setOnClickListener(v -> onActivitiesClicked());
        }

        if (seeAllText != null) {
            seeAllText.setOnClickListener(v -> onSeeAllClicked());
        }

        if (acknowledgeButton != null) {
            acknowledgeButton.setOnClickListener(v -> onAcknowledgeClicked());
        }
    }

    private void loadData() {
        updateDashboardStats(15, 15);
    }

    private void updateDashboardStats(int activeDevices, int successfulRescues) {
        if (activeDevicesCount != null) {
            activeDevicesCount.setText(String.valueOf(activeDevices));
        }
        if (activeSuccessfulCount != null) {
            activeSuccessfulCount.setText(String.valueOf(successfulRescues));
        }
    }

    // Location Permission Methods
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                if (locationText != null) {
                    locationText.setText("Location permission required");
                }
            }
        }
    }

    private void startLocationUpdates() {
        try {
            locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);

            // Check if GPS or Network provider is available
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                Toast.makeText(requireContext(), "Please enable location services", Toast.LENGTH_SHORT).show();
                return;
            }

            // Request location updates
            if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // Try GPS first, then network
                if (isGPSEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        10000, // 10 seconds
                        10, // 10 meters
                        this);
                } else if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        10000, // 10 seconds
                        10, // 10 meters
                        this);
                }

                // Get last known location
                Location lastKnownLocation = null;
                if (isGPSEnabled) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
                if (lastKnownLocation == null && isNetworkEnabled) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

                if (lastKnownLocation != null) {
                    updateLocationUI(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error getting location", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        new Handler(Looper.getMainLooper()).post(() -> {
            updateLocationUI(location.getLatitude(), location.getLongitude());
        });
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        // Provider enabled
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        // Provider disabled
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Status changed
    }

    private void updateLocationUI(double latitude, double longitude) {
        currentLatitude = latitude;
        currentLongitude = longitude;

        if (locationText != null) {
            locationText.setText(String.format("%.5f, %.5f", latitude, longitude));
        }

        // You can also send this location to your server if needed
        // sendLocationToServer(latitude, longitude);
    }

    // =======================
    // Navigation Handlers
    // =======================

    private void onHistoryClicked() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), HistoryActivity.class);
            startActivity(intent);
        }
    }

    private void onSettingsClicked() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), CustomerActivity.class);
            startActivity(intent);
        }
    }

    private void onCustomersClicked() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), CustomerActivity.class);
            startActivity(intent);
        }
    }

    private void onActivitiesClicked() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), CustomerActivity.class);
            startActivity(intent);
        }
    }

    private void onSeeAllClicked() {
        // TODO: Implement see-all functionality
    }

    private void onAcknowledgeClicked() {
        // TODO: Implement acknowledge logic
    }

    // =======================
    // Lifecycle
    // =======================

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
        if (locationManager != null &&
            ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    public void refreshData() {
        loadData();
        setupProfileHeader();
    }
}
