package com.example.link;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CustomerActivity extends AppCompatActivity {

    private FrameLayout btnBack;
    private TextView tvAvailableCount, tvActiveCount;
    private LinearLayout availableDevicesContainer, activeDevicesContainer;
    private RequestQueue requestQueue;
    private SharedPrefManager sharedPrefManager;
    private int currentStaffId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);

        // Initialize Volley request queue
        requestQueue = Volley.newRequestQueue(this);

        // Initialize SharedPrefManager
        sharedPrefManager = SharedPrefManager.getInstance(this);

        // Debug: Print all stored preferences
        debugUserData();

        // Get current STAFF ID (NOT user ID)
        currentStaffId = sharedPrefManager.getStaffId(); // This should be 36

        // Debug log
        Log.d("CustomerActivity", "Using Staff ID: " + currentStaffId);
        Log.d("CustomerActivity", "User Type: " + sharedPrefManager.getUserType());

        // Check if user is staff
        String userType = sharedPrefManager.getUserType();
        if (!"staff".equals(userType) && !"admin".equals(userType)) {
            Toast.makeText(this, "Access denied. Staff only.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Check if staff ID is valid
        if (currentStaffId <= 0) {
            Toast.makeText(this, "Staff ID not found. You may not be registered as staff.", Toast.LENGTH_LONG).show();
            Log.e("CustomerActivity", "Invalid Staff ID: " + currentStaffId);
            finish();
            return;
        }

        initViews();
        setupListeners();
        loadDevices();
    }

    private void debugUserData() {
        // Print all stored preferences for debugging
        Map<String, ?> allPrefs = getSharedPreferences("LinkPrefs", MODE_PRIVATE).getAll();
        Log.d("PREFS_DEBUG", "=== Stored Preferences ===");
        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            Log.d("PREFS_DEBUG", entry.getKey() + " = " + entry.getValue());
        }

        // Log user data
        Log.d("USER_DATA",
            "User ID: " + sharedPrefManager.getUserId() + "\n" +
                "Staff ID: " + sharedPrefManager.getStaffId() + "\n" +
                "User Type: " + sharedPrefManager.getUserType() + "\n" +
                "Username: " + sharedPrefManager.getUsername() + "\n" +
                "Staff Name: " + sharedPrefManager.getStaffName()
        );
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvAvailableCount = findViewById(R.id.tvAvailableCount);
        tvActiveCount = findViewById(R.id.tvActiveCount);
        availableDevicesContainer = findViewById(R.id.availableDevicesContainer);
        activeDevicesContainer = findViewById(R.id.activeDevicesContainer);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadDevices() {
        if (currentStaffId <= 0) {
            Toast.makeText(this, "Staff ID not found. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String url = ApiConfig.GET_DEVICES_URL + "?staff_id=" + currentStaffId;

        Log.d("DeviceRequest", "URL: " + url);
        Log.d("DeviceRequest", "Staff ID being sent: " + currentStaffId);

        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
                try {
                    Log.d("DeviceResponse", response.toString());

                    if (response.getBoolean("success")) {
                        // Update counts
                        int availableCount = response.getInt("available_count");
                        int activeCount = response.getInt("active_count");

                        runOnUiThread(() -> {
                            tvAvailableCount.setText(String.valueOf(availableCount));
                            tvActiveCount.setText(String.valueOf(activeCount));
                        });

                        // Load available devices
                        JSONArray availableDevices = response.getJSONArray("available_devices");
                        loadAvailableDevices(availableDevices);

                        // Load active devices
                        JSONArray activeDevices = response.getJSONArray("active_devices");
                        loadActiveDevices(activeDevices);

                    } else {
                        String error = response.optString("error", "Failed to load devices");
                        Log.e("DeviceResponse", "API Error: " + error);
                        runOnUiThread(() ->
                            Toast.makeText(CustomerActivity.this, "Error: " + error, Toast.LENGTH_LONG).show()
                        );
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e("DeviceResponse", "JSON Parse Error: " + e.getMessage());
                    runOnUiThread(() ->
                        Toast.makeText(CustomerActivity.this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            },
            error -> {
                error.printStackTrace();
                Log.e("DeviceRequest", "Volley Error: " + error.getMessage());
                if (error.networkResponse != null) {
                    Log.e("DeviceRequest", "Status Code: " + error.networkResponse.statusCode);
                    Log.e("DeviceRequest", "Response Data: " + new String(error.networkResponse.data));
                }
                runOnUiThread(() ->
                    Toast.makeText(CustomerActivity.this, "Network error: " + error.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        );

        requestQueue.add(request);
    }

    private void loadAvailableDevices(JSONArray devices) throws JSONException {
        runOnUiThread(() -> {
            availableDevicesContainer.removeAllViews();

            try {
                for (int i = 0; i < devices.length(); i++) {
                    JSONObject device = devices.getJSONObject(i);
                    String serialNumber = device.getString("serial_number");
                    String statusText = device.getString("status_text");

                    // Inflate device item layout
                    View deviceItem = LayoutInflater.from(CustomerActivity.this)
                        .inflate(R.layout.item_available_device, null);

                    TextView tvSerial = deviceItem.findViewById(R.id.tvDeviceSerial);
                    TextView tvStatus = deviceItem.findViewById(R.id.tvDeviceStatus);
                    AppCompatButton btnAssign = deviceItem.findViewById(R.id.btnAssign);
                    View divider = deviceItem.findViewById(R.id.divider);

                    tvSerial.setText(serialNumber);
                    tvStatus.setText(statusText);

                    // Hide divider for last item
                    if (i == devices.length() - 1) {
                        divider.setVisibility(View.GONE);
                    }

                    btnAssign.setOnClickListener(v -> showUserInputModal(serialNumber));

                    availableDevicesContainer.addView(deviceItem);
                }

                // Show message if no devices available
                if (devices.length() == 0) {
                    TextView noDevicesText = new TextView(CustomerActivity.this);
                    noDevicesText.setText("No available devices");
                    noDevicesText.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    noDevicesText.setTextSize(14);
                    noDevicesText.setPadding(20, 40, 20, 40);
                    noDevicesText.setGravity(android.view.Gravity.CENTER);
                    availableDevicesContainer.addView(noDevicesText);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private void loadActiveDevices(JSONArray devices) throws JSONException {
        runOnUiThread(() -> {
            activeDevicesContainer.removeAllViews();

            try {
                for (int i = 0; i < devices.length(); i++) {
                    JSONObject device = devices.getJSONObject(i);
                    String serialNumber = device.getString("serial_number");
                    String assignedName = device.getString("assigned_name");
                    String assignedContact = device.getString("assigned_contact");
                    String assignedBy = device.getString("assigned_by");
                    String status = device.getString("status");

                    // Inflate active device card
                    View activeCard = LayoutInflater.from(CustomerActivity.this)
                        .inflate(R.layout.item_active_device, null);

                    TextView tvUserName = activeCard.findViewById(R.id.tvUserName);
                    TextView tvDeviceSerial = activeCard.findViewById(R.id.tvDeviceSerial);
                    TextView tvContact = activeCard.findViewById(R.id.tvContact);
                    TextView tvAssignedBy = activeCard.findViewById(R.id.tvAssignedBy);
                    LinearLayout statusBadge = activeCard.findViewById(R.id.statusBadge);
                    TextView tvStatus = activeCard.findViewById(R.id.tvStatus);

                    tvUserName.setText(assignedName);
                    tvDeviceSerial.setText(serialNumber);
                    tvContact.setText(assignedContact);
                    tvAssignedBy.setText(assignedBy);

                    // Set status
                    if ("online".equals(status)) {
                        tvStatus.setText("Active");
                        tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    } else {
                        tvStatus.setText("Offline");
                        tvStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    }

                    activeDevicesContainer.addView(activeCard);
                }

                // Show message if no active devices
                if (devices.length() == 0) {
                    TextView noDevicesText = new TextView(CustomerActivity.this);
                    noDevicesText.setText("No active devices");
                    noDevicesText.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    noDevicesText.setTextSize(14);
                    noDevicesText.setPadding(20, 40, 20, 40);
                    noDevicesText.setGravity(android.view.Gravity.CENTER);
                    activeDevicesContainer.addView(noDevicesText);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private void showUserInputModal(String serialNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.modal_user_input, null);
        builder.setView(dialogView);

        EditText inputName = dialogView.findViewById(R.id.inputUserName);
        EditText inputContact = dialogView.findViewById(R.id.inputContactNumber);
        AppCompatButton btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        AppCompatButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        View btnClose = dialogView.findViewById(R.id.btnClose);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnConfirm.setOnClickListener(v -> {
            String userName = inputName.getText().toString().trim();
            String contactNumber = inputContact.getText().toString().trim();

            if (userName.isEmpty()) {
                inputName.setError("Name is required");
                return;
            }

            if (contactNumber.isEmpty()) {
                inputContact.setError("Contact number is required");
                return;
            }

            // Validate contact number format (Philippines: 09XXXXXXXXX)
            if (!contactNumber.matches("^09[0-9]{9}$")) {
                inputContact.setError("Invalid format. Use 09XXXXXXXXX");
                return;
            }

            assignDevice(serialNumber, userName, contactNumber);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void assignDevice(String serialNumber, String userName, String contactNumber) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("serial_number", serialNumber);
            requestBody.put("assigned_name", userName);
            requestBody.put("assigned_contact", contactNumber);
            requestBody.put("assigned_by", currentStaffId); // Using staff_id here

            Log.d("AssignRequest", "Request Body: " + requestBody.toString());
            Log.d("AssignRequest", "Using Staff ID: " + currentStaffId);

            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiConfig.ASSIGN_DEVICE_URL,
                requestBody,
                response -> {
                    try {
                        Log.d("AssignResponse", response.toString());

                        if (response.getBoolean("success")) {
                            runOnUiThread(() -> {
                                Toast.makeText(CustomerActivity.this,
                                    "Device assigned successfully", Toast.LENGTH_SHORT).show();
                                // Reload devices
                                loadDevices();
                            });
                        } else {
                            String message = response.getString("message");
                            runOnUiThread(() ->
                                Toast.makeText(CustomerActivity.this,
                                    "Failed: " + message, Toast.LENGTH_SHORT).show()
                            );
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() ->
                            Toast.makeText(CustomerActivity.this,
                                "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    }
                },
                error -> {
                    error.printStackTrace();
                    runOnUiThread(() ->
                        Toast.makeText(CustomerActivity.this,
                            "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            requestQueue.add(request);

        } catch (JSONException e) {
            e.printStackTrace();
            runOnUiThread(() ->
                Toast.makeText(this, "Error creating request: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh devices when activity resumes
        loadDevices();
    }
}
