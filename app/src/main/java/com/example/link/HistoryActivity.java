package com.example.link;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private TextView tvCountBadge;
    private RecyclerView recyclerView;
    private LinearLayout noDataLayout;
    private View btnBack;

    private SOSAlertAdapter adapter;
    private RequestQueue requestQueue;
    private SharedPrefManager sharedPrefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        sharedPrefManager = SharedPrefManager.getInstance(this);
        requestQueue = Volley.newRequestQueue(this);

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadResolvedSOS();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvCountBadge = findViewById(R.id.tvCountBadge);
        recyclerView = findViewById(R.id.recyclerView);
        noDataLayout = findViewById(R.id.noDataLayout);
    }

    private void setupRecyclerView() {
        adapter = new SOSAlertAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(alert -> {
            // Handle item click
            String message = String.format("SOS Details:\nCustomer: %s\nDevice: %s\nResolved by: %s\nLocation: %s",
                alert.getCustomerName(),
                alert.getTransmitterSerial(),
                alert.getResolvedByName(),
                alert.getFormattedLocation());
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void loadResolvedSOS() {
        int userId = sharedPrefManager.getUserId();
        int staffId = sharedPrefManager.getStaffId();

        if (userId == 0) {
            showError("User not logged in");
            return;
        }

        // Build URL with user_id and staff_id
        String url = ApiConfig.GET_RESOLVED_SOS_URL + "?user_id=" + userId;
        if (staffId > 0) {
            url += "&staff_id=" + staffId;
        }

        android.util.Log.d("HistoryActivity", "Loading resolved SOS from: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
                try {
                    android.util.Log.d("HistoryActivity", "Response: " + response.toString());

                    boolean success = response.getBoolean("success");
                    if (success) {
                        int count = response.getInt("count");
                        JSONArray alertsArray = response.getJSONArray("alerts");

                        List<SOSAlert> alerts = new ArrayList<>();
                        for (int i = 0; i < alertsArray.length(); i++) {
                            JSONObject alertObj = alertsArray.getJSONObject(i);

                            SOSAlert alert = new SOSAlert(
                                alertObj.getInt("id"),
                                alertObj.getString("transmitter_serial"),
                                alertObj.getInt("assignment_id"),
                                alertObj.getString("customer_name"),
                                alertObj.optString("customer_contact", ""),
                                alertObj.getDouble("latitude"),
                                alertObj.getDouble("longitude"),
                                alertObj.getInt("battery_percent"),
                                alertObj.getInt("rssi"),
                                alertObj.getString("alert_time"),
                                alertObj.optString("acknowledged_at", null),
                                alertObj.optString("acknowledged_by_name", "Unknown"),
                                alertObj.optString("resolved_at", null),
                                alertObj.optString("resolved_by_name", "Unknown"),
                                alertObj.optString("resolution_notes", "")
                            );

                            alerts.add(alert);
                        }

                        updateUI(alerts, count);
                    } else {
                        String message = response.optString("message", "Failed to load data");
                        showError(message);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    android.util.Log.e("HistoryActivity", "Parse error: " + e.getMessage());
                    showError("Error parsing response");
                }
            },
            error -> {
                android.util.Log.e("HistoryActivity", "Network error: " + error.toString());

                String errorMessage = "Network error. Please check your connection.";

                if (error.networkResponse != null) {
                    try {
                        String responseBody = new String(error.networkResponse.data, "utf-8");
                        android.util.Log.e("HistoryActivity", "Error response: " + responseBody);

                        JSONObject errorJson = new JSONObject(responseBody);
                        if (errorJson.has("message")) {
                            errorMessage = errorJson.getString("message");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                showError(errorMessage);
            }
        );

        // Set timeout to 15 seconds
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
            15000,
            0,
            com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private void updateUI(List<SOSAlert> alerts, int count) {
        tvCountBadge.setText(String.valueOf(count));

        if (alerts.isEmpty()) {
            noDataLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noDataLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.setAlerts(alerts);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        noDataLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvCountBadge.setText("0");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
    }
}
