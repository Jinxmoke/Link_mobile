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
import com.android.volley.DefaultRetryPolicy;

import org.json.JSONArray;
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

    /* =====================================================
       ACTIVITY LIFECYCLE
       ===================================================== */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);

        requestQueue = Volley.newRequestQueue(this);
        sharedPrefManager = SharedPrefManager.getInstance(this);

        currentStaffId = sharedPrefManager.getStaffId();

        Log.d("CustomerActivity", "Staff ID: " + currentStaffId);
        Log.d("CustomerActivity", "User Type: " + sharedPrefManager.getUserType());

        if (!"staff".equals(sharedPrefManager.getUserType())
            && !"admin".equals(sharedPrefManager.getUserType())) {
            Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (currentStaffId <= 0) {
            Toast.makeText(this, "Invalid staff account", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
        loadDevices();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDevices();
    }

    /* =====================================================
       INIT
       ===================================================== */

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

    /* =====================================================
       LOAD DEVICES
       ===================================================== */

    private void loadDevices() {
        String url = ApiConfig.GET_DEVICES_URL + "?staff_id=" + currentStaffId;
        Log.d("DeviceRequest", url);

        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
                try {
                    if (!response.getBoolean("success")) {
                        Toast.makeText(this, "Failed to load devices", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int availableCount = response.optInt("available_count");
                    int activeCount = response.optInt("active_count");

                    tvAvailableCount.setText(String.valueOf(availableCount));
                    tvActiveCount.setText(String.valueOf(activeCount));

                    loadAvailableDevices(response.optJSONArray("available_devices"));
                    loadActiveDevices(response.optJSONArray("active_devices"));

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Parse error", Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                Log.e("DeviceRequest", error.toString());
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
            }
        );

        requestQueue.add(request);
    }

    /* =====================================================
       AVAILABLE DEVICES
       ===================================================== */

    private void loadAvailableDevices(JSONArray devices) {
        availableDevicesContainer.removeAllViews();

        if (devices == null || devices.length() == 0) {
            showEmpty(availableDevicesContainer, "No available devices");
            return;
        }

        for (int i = 0; i < devices.length(); i++) {
            JSONObject device = devices.optJSONObject(i);
            String serial = device.optString("serial_number");
            String status = device.optString("device_status");

            View item = LayoutInflater.from(this)
                .inflate(R.layout.item_available_device, null);

            TextView tvSerial = item.findViewById(R.id.tvDeviceSerial);
            TextView tvStatus = item.findViewById(R.id.tvDeviceStatus);
            AppCompatButton btnAssign = item.findViewById(R.id.btnAssign);

            tvSerial.setText(serial);
            tvStatus.setText(status);
            btnAssign.setOnClickListener(v -> showUserInputModal(serial));

            availableDevicesContainer.addView(item);
        }
    }

   /* =====================================================
   ACTIVE DEVICES - UPDATED WITH END BUTTON
   ===================================================== */

    private void loadActiveDevices(JSONArray devices) {
        activeDevicesContainer.removeAllViews();

        if (devices == null || devices.length() == 0) {
            showEmpty(activeDevicesContainer, "No active devices");
            return;
        }

        for (int i = 0; i < devices.length(); i++) {
            JSONObject device = devices.optJSONObject(i);

            View card = LayoutInflater.from(this)
                .inflate(R.layout.item_active_device, null);

            // Set device information
            ((TextView) card.findViewById(R.id.tvUserName))
                .setText(device.optString("assigned_name"));

            ((TextView) card.findViewById(R.id.tvDeviceSerial))
                .setText(device.optString("serial_number"));

            ((TextView) card.findViewById(R.id.tvContact))
                .setText(device.optString("assigned_contact"));

            ((TextView) card.findViewById(R.id.tvAssignedBy))
                .setText(device.optString("assigned_by"));

            // Update status
            String status = device.optString("device_status");
            TextView tvStatus = card.findViewById(R.id.tvStatus);
            LinearLayout statusBadge = card.findViewById(R.id.statusBadge);

            if ("registered".equals(status)) {
                tvStatus.setText("Active");
                tvStatus.setTextColor(getResources().getColor(R.color.green_600));
                // Update background if needed for offline status
                // statusBadge.setBackgroundResource(R.drawable.offline_badge_elevated);
            } else {
                tvStatus.setText("Offline");
                tvStatus.setTextColor(getResources().getColor(R.color.gray_500));
                // Update the check_mark icon if needed
            }

            // Get assignment ID and serial number for the End button
            int assignmentId = device.optInt("assignment_id");
            String serialNumber = device.optString("serial_number");

            // Set up End button
            AppCompatButton btnEndAssignment = card.findViewById(R.id.btnEndAssignment);
            if (btnEndAssignment != null) {
                btnEndAssignment.setOnClickListener(v ->
                    showEndAssignmentConfirmation(assignmentId, serialNumber));
            }

            activeDevicesContainer.addView(card);
        }
    }

    /* =====================================================
       END ASSIGNMENT CONFIRMATION DIALOG
       ===================================================== */

    private void showEndAssignmentConfirmation(int assignmentId, String serialNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("End Assignment");
        builder.setMessage("Are you sure you want to end the assignment for device: " + serialNumber + "?");

        builder.setPositiveButton("Yes, End", (dialog, which) -> {
            endAssignment(assignmentId, serialNumber);
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

/* =====================================================
   END ASSIGNMENT API CALL - WITH BETTER DEBUGGING
   ===================================================== */

    private void endAssignment(int assignmentId, String serialNumber) {
        try {
            JSONObject body = new JSONObject();
            body.put("assignment_id", assignmentId);
            body.put("staff_id", currentStaffId);

            Log.d("EndAssignment", "Request URL: " + ApiConfig.END_ASSIGNMENT_URL);
            Log.d("EndAssignment", "Request Body: " + body.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiConfig.END_ASSIGNMENT_URL,
                body,
                response -> {
                    try {
                        Log.d("EndAssignment", "Response: " + response.toString());

                        if (response.optBoolean("success")) {
                            Toast.makeText(this,
                                "Assignment ended successfully",
                                Toast.LENGTH_SHORT).show();
                            loadDevices(); // Refresh the list
                        } else {
                            String error = response.optString("error",
                                response.optString("message", "Failed to end assignment"));
                            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("EndAssignment", "Parse error: ", e);
                    }
                },
                error -> {
                    Log.e("EndAssignment", "Volley Error: ", error);

                    // Detailed error logging
                    if (error.networkResponse != null) {
                        Log.e("EndAssignment", "Status Code: " + error.networkResponse.statusCode);
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Log.e("EndAssignment", "Response Body: " + responseBody);
                        } catch (Exception e) {
                            Log.e("EndAssignment", "Error reading response: ", e);
                        }
                    }

                    if (error.getMessage() != null) {
                        Log.e("EndAssignment", "Error Message: " + error.getMessage());
                    }

                    Toast.makeText(this, "Network error. Please check connection.", Toast.LENGTH_SHORT).show();
                }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Accept", "application/json");
                    return headers;
                }

                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };

            // Add timeout settings
            request.setRetryPolicy(new DefaultRetryPolicy(
                10000, // 10 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("EndAssignment", "Request creation error: ", e);
        }
    }

    /* =====================================================
       ASSIGN DEVICE
       ===================================================== */

    private void showUserInputModal(String serialNumber) {
        View dialogView = LayoutInflater.from(this)
            .inflate(R.layout.modal_user_input, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create();

        EditText inputName = dialogView.findViewById(R.id.inputUserName);
        EditText inputContact = dialogView.findViewById(R.id.inputContactNumber);

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            String contact = inputContact.getText().toString().trim();

            if (name.isEmpty()) {
                inputName.setError("Required");
                return;
            }

            if (!contact.matches("^09[0-9]{9}$")) {
                inputContact.setError("Invalid number");
                return;
            }

            assignDevice(serialNumber, name, contact);
            dialog.dismiss();
        });

        dialog.show();
    }

    /* =====================================================
       API CALL
       ===================================================== */

    private void assignDevice(String serial, String name, String contact) {
        try {
            JSONObject body = new JSONObject();
            body.put("serial_number", serial);
            body.put("assigned_name", name);
            body.put("assigned_contact", contact);
            body.put("staff_id", currentStaffId);

            Log.d("AssignRequest", body.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiConfig.ASSIGN_DEVICE_URL,
                body,
                response -> {
                    if (response.optBoolean("success")) {
                        Toast.makeText(this, "Assigned successfully", Toast.LENGTH_SHORT).show();
                        loadDevices();
                    } else {
                        Toast.makeText(this,
                            response.optString("message"),
                            Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            requestQueue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =====================================================
       UTIL
       ===================================================== */

    private void showEmpty(LinearLayout container, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(30, 40, 30, 40);
        container.addView(tv);
    }
}
