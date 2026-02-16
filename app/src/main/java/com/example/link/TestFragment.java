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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TestFragment extends Fragment implements LocationListener {

    // UI Components
    private TextView activeDevicesCount;
    private TextView activeSuccessfulCount;
    private LinearLayout historyLayout;
    private LinearLayout settingsLayout;
    private LinearLayout customersLayout;
    private LinearLayout activitiesLayout;
    private TextView seeAllText;
    private LinearLayout customerCardsContainer;
    private TextView noDataText;

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
    private static final String KEY_USER_ID = "user_id";

    // API
    private RequestQueue requestQueue;
    private List<CustomerDevice> customerDevices = new ArrayList<>();
    private Handler refreshHandler = new Handler(Looper.getMainLooper());
    private static final long REFRESH_INTERVAL = 30000; // 30 seconds

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_test, container, false);

        requestQueue = Volley.newRequestQueue(requireContext());

        initializeViews(view);
        setupProfileHeader();
        setupClickListeners();
        loadData();

        // Request location permission
        requestLocationPermission();

        // Start auto-refresh
        startAutoRefresh();

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

        // Customer status section
        seeAllText = view.findViewById(R.id.seeAllText);
        customerCardsContainer = view.findViewById(R.id.customerCardsContainer);
        noDataText = view.findViewById(R.id.noDataText);

        // Profile Header Views
        welcomeText = view.findViewById(R.id.welcomeText);
        locationText = view.findViewById(R.id.locationText);
    }

    private void setupProfileHeader() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String staffName = prefs.getString(KEY_STAFF_NAME, "");
        String username = prefs.getString(KEY_USERNAME, "");
        String email = prefs.getString(KEY_EMAIL, "");

        String displayName;
        if (staffName != null && !staffName.isEmpty()) {
            displayName = staffName;
        } else if (username != null && !username.isEmpty()) {
            displayName = username;
        } else if (email != null && !email.isEmpty()) {
            displayName = email.split("@")[0];
        } else {
            displayName = "Staff Member";
        }

        if (welcomeText != null) {
            welcomeText.setText("Welcome, " + displayName);
        }

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
    }

    private void loadData() {
        fetchActiveCustomers();
    }

    private void fetchActiveCustomers() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int userId = prefs.getInt(KEY_USER_ID, 0);
        String userType = prefs.getString("user_type", "");
        int staffId = prefs.getInt("staff_id", 0);

        android.util.Log.d("TestFragment", "User Type: " + userType);
        android.util.Log.d("TestFragment", "User ID: " + userId);
        android.util.Log.d("TestFragment", "Staff ID: " + staffId);

        if (userId == 0) {
            android.util.Log.e("TestFragment", "User ID is 0 - cannot fetch customers");
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Build URL with both user_id and staff_id
        String url = ApiConfig.GET_ACTIVE_CUSTOMERS_URL + "?user_id=" + userId;

        // Only add staff_id if it's actually set (staff members will have this)
        if (staffId > 0) {
            url += "&staff_id=" + staffId;
        }

        android.util.Log.d("TestFragment", "Request URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
                try {
                    android.util.Log.d("TestFragment", "Response received: " + response.toString());
                    boolean success = response.getBoolean("success");

                    if (success) {
                        JSONArray devicesArray = response.getJSONArray("devices");
                        customerDevices.clear();

                        android.util.Log.d("TestFragment", "Found " + devicesArray.length() + " devices");

                        for (int i = 0; i < devicesArray.length(); i++) {
                            JSONObject deviceObj = devicesArray.getJSONObject(i);

                            CustomerDevice device = new CustomerDevice();
                            device.setSerialNumber(deviceObj.getString("serial_number"));
                            device.setDeviceName(deviceObj.getString("device_name"));
                            device.setStatus(deviceObj.getString("status"));
                            device.setBatteryPercent(deviceObj.getInt("battery_percent"));
                            device.setCustomerName(deviceObj.getString("customer_name"));
                            device.setCustomerContact(deviceObj.optString("customer_contact", ""));
                            device.setAssignmentId(deviceObj.getInt("assignment_id"));
                            device.setLatitude(deviceObj.getDouble("latitude"));
                            device.setLongitude(deviceObj.getDouble("longitude"));
                            device.setLastUpdate(deviceObj.getString("last_update"));
                            device.setMinutesAgo(deviceObj.getInt("minutes_ago"));

                            customerDevices.add(device);
                        }

                        updateUI();
                        updateDashboardStats(customerDevices.size(), 15); // Update active devices count
                    } else {
                        String message = response.optString("message", "Failed to load data");
                        android.util.Log.e("TestFragment", "API returned success=false: " + message);
                        showNoData();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    android.util.Log.e("TestFragment", "Error parsing response: " + e.getMessage());
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Error parsing data", Toast.LENGTH_SHORT).show();
                    }
                    showNoData();
                }
            },
            error -> {
                error.printStackTrace();

                // Detailed error logging
                if (error.networkResponse != null) {
                    android.util.Log.e("TestFragment", "Status Code: " + error.networkResponse.statusCode);
                    try {
                        String responseBody = new String(error.networkResponse.data, "utf-8");
                        android.util.Log.e("TestFragment", "Error Response Body: " + responseBody);
                    } catch (Exception e) {
                        android.util.Log.e("TestFragment", "Error reading response: " + e.getMessage());
                    }
                } else {
                    android.util.Log.e("TestFragment", "Network response is null - likely connection error");
                }

                if (error.getMessage() != null) {
                    android.util.Log.e("TestFragment", "Error Message: " + error.getMessage());
                }

                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                }
                showNoData();
            }
        );

        requestQueue.add(request);
    }

    private void updateUI() {
        if (customerCardsContainer == null) return;

        customerCardsContainer.removeAllViews();

        if (customerDevices.isEmpty()) {
            showNoData();
            return;
        }

        // Filter out devices with invalid location (0,0)
        List<CustomerDevice> devicesWithValidLocation = new ArrayList<>();
        for (CustomerDevice device : customerDevices) {
            if (hasValidLocation(device)) {
                devicesWithValidLocation.add(device);
            }
        }

        if (devicesWithValidLocation.isEmpty()) {
            showNoData();
            return;
        }

        // Hide no data message
        if (noDataText != null) {
            noDataText.setVisibility(View.GONE);
        }

        // Add cards only for devices with valid location
        for (CustomerDevice device : devicesWithValidLocation) {
            View cardView = createCustomerCard(device);
            customerCardsContainer.addView(cardView);
        }

        // Update dashboard stats with filtered count
        updateDashboardStats(devicesWithValidLocation.size(), 15);
    }

    private boolean hasValidLocation(CustomerDevice device) {
        // Return true only if both latitude and longitude are not 0.0
        return device.getLatitude() != 0.0 && device.getLongitude() != 0.0;
    }

    private View createCustomerCard(CustomerDevice device) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        // Create the card layout programmatically
        androidx.cardview.widget.CardView cardView = new androidx.cardview.widget.CardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(
            dpToPx(8),  // start
            0,          // top
            dpToPx(8),  // end
            dpToPx(24)  // bottom
        );
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(dpToPx(20));
        cardView.setCardElevation(dpToPx(3));
        cardView.setCardBackgroundColor(0xFFFFFFFF);

        // Create main container
        android.widget.FrameLayout frameLayout = new android.widget.FrameLayout(requireContext());
        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Add orange border
        View orangeBorder = new View(requireContext());
        android.widget.FrameLayout.LayoutParams borderParams = new android.widget.FrameLayout.LayoutParams(dpToPx(5), dpToPx(80));
        borderParams.topMargin = dpToPx(30);
        borderParams.leftMargin = dpToPx(-2);
        orangeBorder.setLayoutParams(borderParams);
        orangeBorder.setBackgroundResource(R.drawable.orange_left_border);
        frameLayout.addView(orangeBorder);

        // Create content LinearLayout
        LinearLayout contentLayout = new LinearLayout(requireContext());
        contentLayout.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));

        // Header section
        LinearLayout headerLayout = createHeaderSection(device);
        contentLayout.addView(headerLayout);

        // Location section
        LinearLayout locationLayout = createLocationSection(device);
        contentLayout.addView(locationLayout);

        // Divider
        View divider = new View(requireContext());
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(1)
        );
        dividerParams.bottomMargin = dpToPx(12);
        divider.setLayoutParams(dividerParams);
        divider.setBackgroundColor(0xFFEEEEEE);
        contentLayout.addView(divider);

        // Customer info section
        LinearLayout customerLayout = createCustomerSection(device);
        contentLayout.addView(customerLayout);

        frameLayout.addView(contentLayout);
        cardView.addView(frameLayout);

        // Add click listener for the entire card
        cardView.setOnClickListener(v -> {
            // TODO: Open map view with this device location
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(),
                    "View map for " + device.getCustomerName(),
                    Toast.LENGTH_SHORT).show();
            }
        });

        return cardView;
    }

    private LinearLayout createHeaderSection(CustomerDevice device) {
        LinearLayout headerLayout = new LinearLayout(requireContext());
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        headerParams.bottomMargin = dpToPx(8);
        headerLayout.setLayoutParams(headerParams);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Left side content
        LinearLayout leftContent = new LinearLayout(requireContext());
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1.0f
        );
        leftContent.setLayoutParams(leftParams);
        leftContent.setOrientation(LinearLayout.VERTICAL);

        // Title row with live icon
        LinearLayout titleRow = new LinearLayout(requireContext());
        titleRow.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Live icon
        android.widget.ImageView liveIcon = new android.widget.ImageView(requireContext());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(25), dpToPx(25));
        iconParams.rightMargin = dpToPx(5);
        liveIcon.setLayoutParams(iconParams);
        liveIcon.setImageResource(R.drawable.live);
        liveIcon.setAlpha(0.85f);
        titleRow.addView(liveIcon);

        // Device name
        TextView deviceName = new TextView(requireContext());
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1.0f
        );
        deviceName.setLayoutParams(nameParams);
        deviceName.setText(device.getDeviceName());
        deviceName.setTextSize(13);
        deviceName.setTextColor(0xFF1a1a1a);
        deviceName.setTypeface(null, android.graphics.Typeface.BOLD);

        titleRow.addView(deviceName);

        // View Map text
        TextView viewMapText = new TextView(requireContext());
        viewMapText.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        viewMapText.setText("View Map  →");
        viewMapText.setTextSize(12);
        viewMapText.setTextColor(0xFFFF7A4D);
        viewMapText.setTypeface(null, android.graphics.Typeface.BOLD);
        titleRow.addView(viewMapText);

        leftContent.addView(titleRow);

        // Timestamp
        TextView timestamp = new TextView(requireContext());
        timestamp.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        timestamp.setText("Last Update: " + device.getFormattedLastUpdate());
        timestamp.setTextSize(10);
        timestamp.setTextColor(0xFF999999);
        leftContent.addView(timestamp);

        headerLayout.addView(leftContent);

        return headerLayout;
    }

    private LinearLayout createLocationSection(CustomerDevice device) {
        LinearLayout locationLayout = new LinearLayout(requireContext());
        LinearLayout.LayoutParams locationParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        locationParams.bottomMargin = dpToPx(10);
        locationParams.topMargin = dpToPx(-5);
        locationLayout.setLayoutParams(locationParams);
        locationLayout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout innerLayout = new LinearLayout(requireContext());
        LinearLayout.LayoutParams innerParams = new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1.0f
        );
        innerLayout.setLayoutParams(innerParams);
        innerLayout.setOrientation(LinearLayout.HORIZONTAL);

        // Map pin icon
        android.widget.ImageView mapPin = new android.widget.ImageView(requireContext());
        LinearLayout.LayoutParams pinParams = new LinearLayout.LayoutParams(dpToPx(14), dpToPx(14));
        pinParams.rightMargin = dpToPx(2);
        mapPin.setLayoutParams(pinParams);
        mapPin.setImageResource(R.drawable.map_pin);
        mapPin.setAlpha(0.85f);
        innerLayout.addView(mapPin);

        // Location coordinates
        TextView locationText = new TextView(requireContext());
        locationText.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        locationText.setText(device.getFormattedLocation());
        locationText.setTextSize(9);
        locationText.setTextColor(0xFF6B6B6B);
        innerLayout.addView(locationText);

        locationLayout.addView(innerLayout);

        return locationLayout;
    }

    private LinearLayout createCustomerSection(CustomerDevice device) {
        LinearLayout customerLayout = new LinearLayout(requireContext());
        customerLayout.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        customerLayout.setOrientation(LinearLayout.HORIZONTAL);
        customerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Customer details
        LinearLayout detailsLayout = new LinearLayout(requireContext());
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1.0f
        );
        detailsLayout.setLayoutParams(detailsParams);
        detailsLayout.setOrientation(LinearLayout.VERTICAL);

        // Customer name
        TextView customerName = new TextView(requireContext());
        customerName.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        customerName.setText(device.getCustomerName());
        customerName.setTextSize(10);
        customerName.setTextColor(0xFF1a1a1a);
        detailsLayout.addView(customerName);

        // Customer label
        TextView customerLabel = new TextView(requireContext());
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        labelParams.topMargin = dpToPx(2);
        customerLabel.setLayoutParams(labelParams);
        customerLabel.setText("Customer");
        customerLabel.setTextSize(8);
        customerLabel.setTextColor(0xFF999999);
        detailsLayout.addView(customerLabel);

        customerLayout.addView(detailsLayout);

        return customerLayout;
    }

    private void showNoData() {
        if (customerCardsContainer != null) {
            customerCardsContainer.removeAllViews();
        }
        if (noDataText != null) {
            noDataText.setVisibility(View.VISIBLE);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void updateDashboardStats(int activeDevices, int successfulRescues) {
        if (activeDevicesCount != null) {
            activeDevicesCount.setText(String.valueOf(activeDevices));
        }
        if (activeSuccessfulCount != null) {
            activeSuccessfulCount.setText(String.valueOf(successfulRescues));
        }
    }

    private void startAutoRefresh() {
        refreshHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAdded() && getActivity() != null) {
                    fetchActiveCustomers();
                    refreshHandler.postDelayed(this, REFRESH_INTERVAL);
                }
            }
        }, REFRESH_INTERVAL);
    }

    private void stopAutoRefresh() {
        refreshHandler.removeCallbacksAndMessages(null);
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
                if (isAdded() && getContext() != null) {
                    Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                }
                if (locationText != null) {
                    locationText.setText("Location permission required");
                }
            }
        }
    }

    private void startLocationUpdates() {
        try {
            locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);

            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(requireContext(), "Please enable location services", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                if (isGPSEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        10000, 10, this);
                } else if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        10000, 10, this);
                }

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
            if (isAdded() && getContext() != null) {
                Toast.makeText(requireContext(), "Error getting location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        new Handler(Looper.getMainLooper()).post(() -> {
            updateLocationUI(location.getLatitude(), location.getLongitude());
        });
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    private void updateLocationUI(double latitude, double longitude) {
        currentLatitude = latitude;
        currentLongitude = longitude;

        if (locationText != null) {
            locationText.setText(String.format("%.5f, %.5f", latitude, longitude));
        }
    }

    // Navigation Handlers
    private void onHistoryClicked() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), HistoryActivity.class);
            startActivity(intent);
        }
    }

    private void onSettingsClicked() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
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
            Intent intent = new Intent(getActivity(), DeviceLocationActivity.class);
            startActivity(intent);
        }
    }

    private void onSeeAllClicked() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), CustomerActivity.class);
            startActivity(intent);
        }
    }

    // Lifecycle
    @Override
    public void onResume() {
        super.onResume();
        refreshData();
        if (locationManager != null &&
            ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
        startAutoRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        stopAutoRefresh();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        stopAutoRefresh();
    }

    public void refreshData() {
        loadData();
        setupProfileHeader();
    }
}
