package com.example.link;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceLocationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DeviceLocationAdapter adapter;
    private List<DeviceLocation> deviceLocationList;
    private RequestQueue requestQueue;
    private SharedPrefManager sharedPrefManager;
    private View noDataLayout;
    private ImageView btnFilter;

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private boolean filterByMe = false;

    private static final String TAG = "DeviceLocationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activities);

        // Initialize SharedPrefManager and RequestQueue
        sharedPrefManager = SharedPrefManager.getInstance(this);
        requestQueue = Volley.newRequestQueue(this);
        deviceLocationList = new ArrayList<>();

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        noDataLayout = findViewById(R.id.noDataLayout);
        btnFilter = findViewById(R.id.btnFilter);

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new DeviceLocationAdapter(deviceLocationList);
        recyclerView.setAdapter(adapter);

        // Setup back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Setup filter button
        btnFilter.setOnClickListener(v -> {
            filterByMe = !filterByMe;
            updateFilterIcon();
            refreshData();
            String filterStatus = filterByMe ? "ON (Showing my devices)" : "OFF (Showing all devices)";
            Toast.makeText(DeviceLocationActivity.this, "Filter: " + filterStatus, Toast.LENGTH_SHORT).show();
        });

        // Initial filter icon update
        updateFilterIcon();

        // Debug logging
        int userId = sharedPrefManager.getUserId();
        String userType = sharedPrefManager.getUserType();
        Log.d(TAG, "User ID: " + userId + ", User Type: " + userType + ", Initial Filter: " + (filterByMe ? "ON" : "OFF"));

        // Setup pagination scroll listener
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && hasMoreData) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                            loadNextPage();
                        }
                    }
                }
            }
        });

        // Load first page
        loadDeviceLocations(currentPage);
    }

    private void updateFilterIcon() {
        if (filterByMe) {
            // Blue when filter is ON
            btnFilter.setColorFilter(getResources().getColor(android.R.color.holo_blue_dark));
        } else {
            // Gray when filter is OFF
            btnFilter.setColorFilter(getResources().getColor(android.R.color.darker_gray));
        }
    }

    private void loadDeviceLocations(int page) {
        if (isLoading) return;

        isLoading = true;

        // Get user ID
        int userId = sharedPrefManager.getUserId();

        // Build URL with parameters
        String url = ApiConfig.GET_DEVICE_ACTIVITIES +
            "?user_id=" + userId +
            "&filter_by_me=" + (filterByMe ? "1" : "0") +
            "&page=" + page +
            "&limit=5";

        Log.d(TAG, "Loading page " + page + " from URL: " + url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.GET, url, null,
            response -> {
                isLoading = false;

                try {
                    Log.d(TAG, "Response received successfully");

                    boolean success = response.getBoolean("success");

                    if (success) {
                        JSONArray dataArray = response.getJSONArray("data");

                        // Clear list if loading first page
                        if (page == 1) {
                            deviceLocationList.clear();
                        }

                        if (dataArray.length() > 0) {
                            // Hide no data layout, show recyclerview
                            runOnUiThread(() -> {
                                noDataLayout.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                            });

                            // Parse and add data
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject obj = dataArray.getJSONObject(i);

                                DeviceLocation deviceLocation = new DeviceLocation();

                                // Set all fields from JSON response
                                if (obj.has("id")) deviceLocation.setId(obj.getInt("id"));
                                if (obj.has("serial_number")) deviceLocation.setSerialNumber(obj.getString("serial_number"));
                                if (obj.has("latitude")) deviceLocation.setLatitude(obj.getString("latitude"));
                                if (obj.has("longitude")) deviceLocation.setLongitude(obj.getString("longitude"));
                                if (obj.has("recorded_at")) deviceLocation.setDateTime(obj.getString("recorded_at"));
                                if (obj.has("customer_name")) deviceLocation.setCustomerName(obj.getString("customer_name"));
                                if (obj.has("customer_contact")) deviceLocation.setCustomerContact(obj.getString("customer_contact"));
                                if (obj.has("battery_percent") && !obj.isNull("battery_percent")) {
                                    deviceLocation.setBatteryPercent(obj.getInt("battery_percent"));
                                }
                                if (obj.has("device_owner")) deviceLocation.setDeviceOwner(obj.getString("device_owner"));
                                if (obj.has("assigned_by")) deviceLocation.setAssignedBy(obj.getInt("assigned_by"));
                                if (obj.has("assigned_by_name")) deviceLocation.setAssignedByName(obj.getString("assigned_by_name"));
                                if (obj.has("assigned_at")) deviceLocation.setAssignedAt(obj.getString("assigned_at"));

                                deviceLocationList.add(deviceLocation);
                            }

                            // Update adapter
                            runOnUiThread(() -> {
                                if (page == 1) {
                                    adapter.notifyDataSetChanged();
                                } else {
                                    adapter.notifyItemRangeInserted(deviceLocationList.size() - dataArray.length(), dataArray.length());
                                }
                                Log.d(TAG, "Total items in list: " + deviceLocationList.size());
                            });

                            // Update pagination info
                            int totalItems = response.getInt("total_items");
                            int currentItems = deviceLocationList.size();
                            hasMoreData = currentItems < totalItems;

                            Log.d(TAG, "Total items in DB: " + totalItems + ", Loaded: " + currentItems + ", Has more: " + hasMoreData);

                        } else {
                            // No data returned
                            runOnUiThread(() -> {
                                if (page == 1) {
                                    // Show no data layout only on first page
                                    noDataLayout.setVisibility(View.VISIBLE);
                                    recyclerView.setVisibility(View.GONE);
                                    Log.d(TAG, "No data available, showing empty state");

                                    // Show helpful message
                                    if (response.has("filter_status")) {
                                        String filterStatus = null;
                                        try {
                                            filterStatus = response.getString("filter_status");
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }
                                        Toast.makeText(DeviceLocationActivity.this,
                                            "No devices found (" + filterStatus + ")",
                                            Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(DeviceLocationActivity.this,
                                            "No device activities found",
                                            Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            hasMoreData = false;
                        }
                    } else {
                        // API returned error
                        String errorMsg = response.getString("message");
                        runOnUiThread(() -> {
                            Toast.makeText(DeviceLocationActivity.this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();

                            if (page == 1) {
                                noDataLayout.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            }
                        });
                        Log.e(TAG, "API Error: " + errorMsg);
                    }
                } catch (JSONException e) {
                    isLoading = false;
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(DeviceLocationActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    });
                    Log.e(TAG, "JSON Parsing Error: " + e.getMessage());
                }
            },
            error -> {
                isLoading = false;
                runOnUiThread(() -> {
                    Toast.makeText(DeviceLocationActivity.this,
                        "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();

                    if (page == 1) {
                        noDataLayout.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                });
                Log.e(TAG, "Volley Network Error: " + error.getMessage());
            }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        // Add request to queue
        requestQueue.add(jsonObjectRequest);
    }

    private void loadNextPage() {
        if (hasMoreData && !isLoading) {
            currentPage++;
            loadDeviceLocations(currentPage);
        }
    }

    public void refreshData() {
        currentPage = 1;
        deviceLocationList.clear();
        adapter.notifyDataSetChanged();
        loadDeviceLocations(currentPage);
    }
}
