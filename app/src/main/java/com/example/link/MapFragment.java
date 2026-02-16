// MapFragment.java
package com.example.link;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapFragment extends Fragment {

    private WebView leafletWebView;
    private FusedLocationProviderClient fusedLocationClient;
    private RequestQueue requestQueue;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private SharedPrefManager sharedPrefManager;
    private boolean isFragmentActive = false;

    // JavaScript Interface Class for bidirectional communication
    public class JavaScriptInterface {
        private Context context;

        JavaScriptInterface(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public String getStaffId() {
            if (!isFragmentValid()) return "STAFF001";
            sharedPrefManager = SharedPrefManager.getInstance(context);
            return String.valueOf(sharedPrefManager.getStaffId());
        }

        @JavascriptInterface
        public String getStaffName() {
            if (!isFragmentValid()) return "Staff Member";
            sharedPrefManager = SharedPrefManager.getInstance(context);
            String name = sharedPrefManager.getStaffName();
            return name != null && !name.isEmpty() ? name : sharedPrefManager.getUsername();
        }

        @JavascriptInterface
        public String getStaffRole() {
            if (!isFragmentValid()) return "STAFF";
            sharedPrefManager = SharedPrefManager.getInstance(context);
            String userType = sharedPrefManager.getUserType();
            if ("admin".equals(userType)) {
                return "Administrator";
            } else if ("staff".equals(userType)) {
                return "STAFF";
            } else {
                return "User";
            }
        }

        @JavascriptInterface
        public String getFullStaffInfo() {
            try {
                if (!isFragmentValid()) {
                    return "{\"id\":\"STAFF001\",\"name\":\"Staff Member\",\"role\":\"STAFF\",\"isLoggedIn\":false}";
                }

                sharedPrefManager = SharedPrefManager.getInstance(context);
                JSONObject staffData = new JSONObject();
                staffData.put("id", String.valueOf(sharedPrefManager.getStaffId()));
                staffData.put("name", sharedPrefManager.getStaffName());
                staffData.put("role", getStaffRole());
                staffData.put("email", sharedPrefManager.getEmail());
                staffData.put("contact", sharedPrefManager.getContact());
                staffData.put("username", sharedPrefManager.getUsername());
                staffData.put("isLoggedIn", sharedPrefManager.isLoggedIn());
                staffData.put("userType", sharedPrefManager.getUserType());
                return staffData.toString();
            } catch (JSONException e) {
                return "{\"id\":\"STAFF001\",\"name\":\"Staff Member\",\"role\":\"STAFF\",\"isLoggedIn\":false}";
            }
        }

        @JavascriptInterface
        public boolean isLoggedIn() {
            if (!isFragmentValid()) return false;
            sharedPrefManager = SharedPrefManager.getInstance(context);
            return sharedPrefManager.isLoggedIn();
        }

        @JavascriptInterface
        public void onMapReady() {
            if (isFragmentValid() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(context, "Map ready with staff data", Toast.LENGTH_SHORT).show();
                });
            }
        }

        @JavascriptInterface
        public void showToast(String message) {
            if (isFragmentValid() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        requestQueue = Volley.newRequestQueue(requireContext());
        sharedPrefManager = SharedPrefManager.getInstance(requireContext());

        leafletWebView = view.findViewById(R.id.leafletWebView);

        // Configure WebView settings
        WebSettings webSettings = leafletWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);

        // Enable mixed content for Firebase
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        // Add JavaScript Interface
        leafletWebView.addJavascriptInterface(
            new JavaScriptInterface(requireContext()),
            "AndroidApp"
        );

        leafletWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // Check if fragment is still attached
                if (!isFragmentValid()) {
                    return;
                }

                // Inject staff data immediately after page loads
                injectStaffData();

                showToast("Emergency Response Map Loaded", Toast.LENGTH_SHORT);

                // Wait for map initialization
                leafletWebView.postDelayed(() -> {
                    // Double-check fragment is still attached before proceeding
                    if (!isFragmentValid()) {
                        return;
                    }

                    // Get user location
                    checkLocationPermissionAndGetLocation();

                    // Load base stations
                    loadBaseStations();

                    // Check Firebase connection
                    checkFirebaseConnection();
                }, 1000);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Handle all URLs within WebView
                return false;
            }
        });

        // Load the map HTML file from assets
        leafletWebView.loadUrl("file:///android_asset/map.html");
    }

    private boolean isFragmentValid() {
        return isFragmentActive &&
            isAdded() &&
            !isDetached() &&
            getContext() != null &&
            getActivity() != null &&
            !getActivity().isFinishing() &&
            !getActivity().isDestroyed();
    }

    private void injectStaffData() {
        if (!isFragmentValid()) {
            return;
        }

        if (!sharedPrefManager.isLoggedIn()) {
            showToast("Please log in to access full features", Toast.LENGTH_SHORT);
            return;
        }

        String staffId = String.valueOf(sharedPrefManager.getStaffId());
        String staffName = sharedPrefManager.getStaffName();
        if (staffName == null || staffName.isEmpty()) {
            staffName = sharedPrefManager.getUsername();
        }
        String staffRole = getStaffRole();
        String email = sharedPrefManager.getEmail();
        String contact = sharedPrefManager.getContact();

        // Create JavaScript to inject staff data into localStorage
        String javascript = String.format(
            "javascript:(function() {" +
                "try {" +
                "   localStorage.setItem('staff_id', '%s');" +
                "   localStorage.setItem('staff_name', '%s');" +
                "   localStorage.setItem('staff_role', '%s');" +
                "   localStorage.setItem('staff_email', '%s');" +
                "   localStorage.setItem('staff_contact', '%s');" +
                "   console.log('[Android] Staff data injected:', '%s', '%s', '%s');" +
                "   " +
                "   // Notify map that staff data is ready" +
                "   if (typeof window.initStaffInfo === 'function') {" +
                "       window.initStaffInfo();" +
                "   }" +
                "   " +
                "   // Update UI with staff info" +
                "   if (typeof window.updateStaffUI === 'function') {" +
                "       window.updateStaffUI();" +
                "   }" +
                "} catch(e) {" +
                "   console.error('[Android] Error injecting staff data:', e);" +
                "}" +
                "})();",
            escapeJavaScriptString(staffId),
            escapeJavaScriptString(staffName),
            escapeJavaScriptString(staffRole),
            escapeJavaScriptString(email),
            escapeJavaScriptString(contact),
            staffId, staffName, staffRole
        );

        if (leafletWebView != null) {
            leafletWebView.evaluateJavascript(javascript, value -> {
                // JavaScript execution callback
            });
        }
    }

    private String getStaffRole() {
        if (!isFragmentValid()) return "STAFF";

        String userType = sharedPrefManager.getUserType();
        if ("admin".equals(userType)) {
            return "Administrator";
        } else if ("staff".equals(userType)) {
            return "STAFF";
        } else {
            return "User";
        }
    }

    private void checkLocationPermissionAndGetLocation() {
        if (!isFragmentValid()) return;

        if (ContextCompat.checkSelfPermission(getContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getUserLocation();
        } else {
            requestPermissions(
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && isFragmentValid()) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation();
            } else {
                showToast("Location permission denied. Using default location.", Toast.LENGTH_SHORT);
                updateMapLocation(14.5995, 120.9842);
            }
        }
    }

    private void getUserLocation() {
        if (!isFragmentValid()) return;

        if (ActivityCompat.checkSelfPermission(getContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            updateMapLocation(14.5995, 120.9842);
            return;
        }

        Task<android.location.Location> locationTask = fusedLocationClient.getLastLocation();
        locationTask.addOnSuccessListener(getActivity(), location -> {
            if (isFragmentValid()) {
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    updateMapLocation(latitude, longitude);
                } else {
                    updateMapLocation(14.5995, 120.9842);
                }
            }
        });
        locationTask.addOnFailureListener(e -> {
            if (isFragmentValid()) {
                showToast("Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT);
                updateMapLocation(14.5995, 120.9842);
            }
        });
    }

    private void updateMapLocation(double latitude, double longitude) {
        if (!isFragmentValid() || leafletWebView == null) return;

        String javascript = String.format(
            "javascript:(function() {" +
                "if (typeof window.MapFunctions !== 'undefined' && " +
                "    typeof window.MapFunctions.updateUserLocation === 'function') {" +
                "   window.MapFunctions.updateUserLocation(%f, %f);" +
                "   console.log('[Android] Location updated:', %f, %f);" +
                "} else {" +
                "   console.error('[Android] MapFunctions.updateUserLocation not available');" +
                "}" +
                "})();",
            latitude, longitude, latitude, longitude
        );

        leafletWebView.evaluateJavascript(javascript, null);
    }

    private void loadBaseStations() {
        if (!isFragmentValid()) return;

        sharedPrefManager = SharedPrefManager.getInstance(getContext());
        int userId = sharedPrefManager.getUserId();
        int staffId = sharedPrefManager.getStaffId();

        // Build URL with user_id and staff_id
        String url = ApiConfig.BASE_URL + "get_base_stations.php?user_id=" + userId;
        if (staffId > 0) {
            url += "&staff_id=" + staffId;
        }

        android.util.Log.d("MapFragment", "Loading base stations for user_id=" + userId + ", staff_id=" + staffId);
        android.util.Log.d("MapFragment", "Base stations URL: " + url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
                if (!isFragmentValid()) return;

                try {
                    boolean success = response.getBoolean("success");
                    if (success) {
                        sendBaseStationsToMap(response.toString());

                        // Log debug info
                        if (response.has("debug")) {
                            android.util.Log.d("MapFragment", "Base stations debug: " + response.getJSONObject("debug").toString());
                        }

                        showToast("Base stations loaded successfully", Toast.LENGTH_SHORT);
                    } else {
                        String message = response.optString("message", "Unknown error");
                        showToast("Failed to load base stations: " + message, Toast.LENGTH_SHORT);
                    }
                } catch (JSONException e) {
                    showToast("Error parsing base stations response", Toast.LENGTH_SHORT);
                    android.util.Log.e("MapFragment", "Error parsing base stations", e);
                }
            },
            error -> {
                if (!isFragmentValid()) return;

                showToast("Network error loading base stations", Toast.LENGTH_SHORT);
                android.util.Log.e("MapFragment", "Network error loading base stations", error);
                error.printStackTrace();
            }
        );

        requestQueue.add(jsonObjectRequest);
    }

    private void sendBaseStationsToMap(String jsonData) {
        if (!isFragmentValid() || leafletWebView == null) return;

        try {
            JSONObject response = new JSONObject(jsonData);
            if (response.getBoolean("success")) {
                JSONArray baseStationsArray = response.getJSONArray("data");

                StringBuilder jsArray = new StringBuilder("[");
                for (int i = 0; i < baseStationsArray.length(); i++) {
                    if (i > 0) jsArray.append(",");

                    JSONObject station = baseStationsArray.getJSONObject(i);
                    jsArray.append("{")
                        .append("\"latitude\":").append(station.optDouble("latitude", 0)).append(",")
                        .append("\"longitude\":").append(station.optDouble("longitude", 0)).append(",")
                        .append("}");
                }
                jsArray.append("]");

                String javascript = String.format(
                    "javascript:(function() {" +
                        "if (typeof window.MapFunctions !== 'undefined' && " +
                        "    typeof window.MapFunctions.addBaseStationMarkers === 'function') {" +
                        "   window.MapFunctions.addBaseStationMarkers(%s);" +
                        "   console.log('[Android] Base stations sent to map');" +
                        "} else {" +
                        "   console.error('[Android] MapFunctions.addBaseStationMarkers not available');" +
                        "}" +
                        "})();",
                    jsArray.toString()
                );

                leafletWebView.evaluateJavascript(javascript, null);
            }
        } catch (JSONException e) {
            showToast("Error processing base stations data", Toast.LENGTH_SHORT);
            e.printStackTrace();
        }
    }

    private String escapeJavaScriptString(String input) {
        if (input == null) return "";
        return input
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    private void checkFirebaseConnection() {
        if (!isFragmentValid() || leafletWebView == null) return;

        leafletWebView.postDelayed(() -> {
            if (!isFragmentValid()) return;

            String javascript = "javascript:(function() {" +
                "if (typeof window.checkFirebaseStatus === 'function') {" +
                "   window.checkFirebaseStatus();" +
                "}" +
                "})();";

            leafletWebView.evaluateJavascript(javascript, null);
        }, 2000);
    }

    private void showToast(String message, int duration) {
        if (isFragmentValid() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), message, duration).show();
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive = true;
        if (leafletWebView != null && isFragmentValid()) {
            leafletWebView.postDelayed(() -> {
                if (!isFragmentValid()) return;

                // Refresh staff data
                injectStaffData();

                // Refresh base stations
                loadBaseStations();

                // Check Firebase connection
                checkFirebaseConnection();
            }, 500);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentActive = false;

        // Clear WebView callbacks and references
        if (leafletWebView != null) {
            leafletWebView.setWebViewClient(null);
            leafletWebView.destroy();
            leafletWebView = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isFragmentActive = false;
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
    }
}
