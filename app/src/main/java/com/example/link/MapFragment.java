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

    // Pending alert args – stashed here so we can call zoomToAlert after the
    // page and Firebase have both finished loading.
    private String pendingAlertLat    = null;
    private String pendingAlertLng    = null;
    private String pendingAlertSerial = null;
    private String pendingAlertType   = null;
    private String pendingAlertName   = null;

    // ─────────────────────────────────────────────────────────
    //  JavaScript Interface
    // ─────────────────────────────────────────────────────────

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
            return resolveStaffRole(sharedPrefManager.getUserType());
        }

        /**
         * Returns the owning admin's user_id so the map JS can scope
         * Firebase listeners and API calls to the correct tenant.
         *
         * - Admin logging in  → returns their own user_id
         * - Staff logging in  → returns the admin's user_id (stored at login)
         */
        @JavascriptInterface
        public String getAdminUserId() {
            if (!isFragmentValid()) return "0";
            sharedPrefManager = SharedPrefManager.getInstance(context);
            return String.valueOf(sharedPrefManager.getAdminUserId());
        }

        @JavascriptInterface
        public String getFullStaffInfo() {
            try {
                if (!isFragmentValid()) {
                    return "{\"id\":\"STAFF001\",\"name\":\"Staff Member\",\"role\":\"STAFF\"," +
                        "\"adminUserId\":\"0\",\"isLoggedIn\":false}";
                }
                sharedPrefManager = SharedPrefManager.getInstance(context);
                JSONObject staffData = new JSONObject();
                staffData.put("id",          String.valueOf(sharedPrefManager.getStaffId()));
                staffData.put("name",        sharedPrefManager.getStaffName());
                staffData.put("role",        resolveStaffRole(sharedPrefManager.getUserType()));
                staffData.put("email",       sharedPrefManager.getEmail());
                staffData.put("contact",     sharedPrefManager.getContact());
                staffData.put("username",    sharedPrefManager.getUsername());
                staffData.put("isLoggedIn",  sharedPrefManager.isLoggedIn());
                staffData.put("userType",    sharedPrefManager.getUserType());
                // KEY ADDITION: include the admin user ID so JS can filter Firebase
                staffData.put("adminUserId", String.valueOf(sharedPrefManager.getAdminUserId()));
                return staffData.toString();
            } catch (JSONException e) {
                return "{\"id\":\"STAFF001\",\"name\":\"Staff Member\",\"role\":\"STAFF\"," +
                    "\"adminUserId\":\"0\",\"isLoggedIn\":false}";
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
                getActivity().runOnUiThread(() ->
                    Toast.makeText(context, "Map ready", Toast.LENGTH_SHORT).show()
                );
            }
        }

        @JavascriptInterface
        public void showToast(String message) {
            if (isFragmentValid() && getActivity() != null) {
                getActivity().runOnUiThread(() ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                );
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────

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
        requestQueue        = Volley.newRequestQueue(requireContext());
        sharedPrefManager   = SharedPrefManager.getInstance(requireContext());

        readAlertArguments();

        leafletWebView = view.findViewById(R.id.leafletWebView);

        WebSettings webSettings = leafletWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        leafletWebView.addJavascriptInterface(
            new JavaScriptInterface(requireContext()),
            "AndroidApp"
        );

        leafletWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!isFragmentValid()) return;

                // 1. Inject staff + admin context into JS localStorage FIRST
                injectStaffData();

                // 2. Give Leaflet + Firebase time to initialise, then load data
                leafletWebView.postDelayed(() -> {
                    if (!isFragmentValid()) return;

                    checkLocationPermissionAndGetLocation();
                    loadBaseStations();
                    checkFirebaseConnection();

                    // Zoom to alert if one arrived via notification
                    handleAlertArguments();

                }, 1500);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });

        leafletWebView.loadUrl("file:///android_asset/map.html");
    }

    // ─────────────────────────────────────────────────────────
    //  Alert argument helpers
    // ─────────────────────────────────────────────────────────

    private void readAlertArguments() {
        Bundle args = getArguments();
        if (args == null) return;

        pendingAlertType   = args.getString("alertType");
        pendingAlertLat    = args.getString("latitude");
        pendingAlertLng    = args.getString("longitude");
        pendingAlertSerial = args.getString("transmitter_serial");
        pendingAlertName   = args.getString("assigned_name");

        android.util.Log.d("MapFragment",
            "Alert args: type=" + pendingAlertType
                + " serial=" + pendingAlertSerial
                + " lat=" + pendingAlertLat
                + " lng=" + pendingAlertLng);
    }

    /**
     * Called after the page loads. Injects a JS retry loop that keeps trying
     * to call MapFunctions.zoomToAlert() until the Firebase listener has
     * rendered the marker.
     */
    private void handleAlertArguments() {
        if (pendingAlertType == null || pendingAlertLat == null || pendingAlertLng == null) return;
        if (!isFragmentValid() || leafletWebView == null) return;

        android.util.Log.d("MapFragment", "Calling zoomToAlert for " + pendingAlertSerial);

        String javascript = String.format(
            "javascript:(function() {" +
                "  var lat    = %s;" +
                "  var lng    = %s;" +
                "  var serial = '%s';" +
                "  var type   = '%s';" +
                "  var name   = '%s';" +
                "  var attempts = 15;" +
                "  function tryZoom() {" +
                "    if (window.MapFunctions && window.MapFunctions.zoomToAlert) {" +
                "      window.MapFunctions.zoomToAlert(lat, lng, serial, type, name);" +
                "    } else if (attempts-- > 0) {" +
                "      setTimeout(tryZoom, 800);" +
                "    }" +
                "  }" +
                "  tryZoom();" +
                "})();",
            pendingAlertLat,
            pendingAlertLng,
            escapeJavaScriptString(pendingAlertSerial != null ? pendingAlertSerial : ""),
            escapeJavaScriptString(pendingAlertType),
            escapeJavaScriptString(pendingAlertName != null ? pendingAlertName : "")
        );

        leafletWebView.evaluateJavascript(javascript, null);

        // Clear so onResume doesn't re-zoom
        pendingAlertType   = null;
        pendingAlertLat    = null;
        pendingAlertLng    = null;
        pendingAlertSerial = null;
        pendingAlertName   = null;
    }

    // ─────────────────────────────────────────────────────────
    //  Fragment validity guard
    // ─────────────────────────────────────────────────────────

    private boolean isFragmentValid() {
        return isFragmentActive &&
            isAdded() &&
            !isDetached() &&
            getContext() != null &&
            getActivity() != null &&
            !getActivity().isFinishing() &&
            !getActivity().isDestroyed();
    }

    // ─────────────────────────────────────────────────────────
    //  Staff data injection
    //  Injects staff info AND the admin_user_id into JS localStorage
    //  so the map can scope all Firebase listeners to the correct tenant.
    // ─────────────────────────────────────────────────────────

    private void injectStaffData() {
        if (!isFragmentValid()) return;
        if (!sharedPrefManager.isLoggedIn()) {
            showToast("Please log in to access full features", Toast.LENGTH_SHORT);
            return;
        }

        String staffId      = String.valueOf(sharedPrefManager.getStaffId());
        String staffName    = sharedPrefManager.getStaffName();
        if (staffName == null || staffName.isEmpty()) staffName = sharedPrefManager.getUsername();
        String staffRole    = resolveStaffRole(sharedPrefManager.getUserType());
        String email        = sharedPrefManager.getEmail();
        String contact      = sharedPrefManager.getContact();
        // KEY: use admin user ID, NOT the staff's own user ID
        String adminUserId  = String.valueOf(sharedPrefManager.getAdminUserId());

        String javascript = String.format(
            "javascript:(function() {" +
                "try {" +
                "   localStorage.setItem('staff_id',      '%s');" +
                "   localStorage.setItem('staff_name',    '%s');" +
                "   localStorage.setItem('staff_role',    '%s');" +
                "   localStorage.setItem('staff_email',   '%s');" +
                "   localStorage.setItem('staff_contact', '%s');" +
                "   localStorage.setItem('admin_user_id', '%s');" +  // TENANT KEY
                "   console.log('[Android] Staff injected: id=%s name=%s role=%s adminId=%s');" +
                "   if (typeof window.initStaffInfo === 'function') window.initStaffInfo();" +
                "} catch(e) { console.error('[Android] Error injecting staff data:', e); }" +
                "})();",
            escapeJavaScriptString(staffId),
            escapeJavaScriptString(staffName),
            escapeJavaScriptString(staffRole),
            escapeJavaScriptString(email),
            escapeJavaScriptString(contact),
            escapeJavaScriptString(adminUserId),
            staffId, staffName, staffRole, adminUserId
        );

        if (leafletWebView != null) {
            leafletWebView.evaluateJavascript(javascript, value -> { /* no-op */ });
        }
    }

    private String resolveStaffRole(String userType) {
        if ("admin".equals(userType))       return "Administrator";
        else if ("staff".equals(userType))  return "STAFF";
        else if ("super_admin".equals(userType)) return "Super Admin";
        else                                return "User";
    }

    // ─────────────────────────────────────────────────────────
    //  Location
    // ─────────────────────────────────────────────────────────

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
                    updateMapLocation(location.getLatitude(), location.getLongitude());
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
                "} else { console.error('[Android] MapFunctions.updateUserLocation not available'); }" +
                "})();",
            latitude, longitude
        );
        leafletWebView.evaluateJavascript(javascript, null);
    }

    // ─────────────────────────────────────────────────────────
    //  Base stations
    //  Always fetched using the ADMIN's user_id, never the staff's own id.
    // ─────────────────────────────────────────────────────────

    private void loadBaseStations() {
        if (!isFragmentValid()) return;
        sharedPrefManager = SharedPrefManager.getInstance(getContext());

        // KEY FIX: use the admin's user_id so staff see their admin's base station
        int adminUserId = sharedPrefManager.getAdminUserId();

        if (adminUserId == 0) {
            android.util.Log.w("MapFragment", "adminUserId is 0 – base stations will not load");
            showToast("Could not determine admin – base station not loaded", Toast.LENGTH_SHORT);
            return;
        }

        String url = ApiConfig.BASE_URL + "get_base_stations.php?user_id=" + adminUserId;
        android.util.Log.d("MapFragment", "Loading base stations for admin_user_id=" + adminUserId + " url=" + url);

        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET, url, null,
            response -> {
                if (!isFragmentValid()) return;
                try {
                    if (response.getBoolean("success")) {
                        sendBaseStationsToMap(response.toString());
                        android.util.Log.d("MapFragment", "Base stations loaded for admin " + adminUserId);
                    } else {
                        android.util.Log.w("MapFragment",
                            "get_base_stations failed: " + response.optString("message"));
                        showToast("No base station found for this admin", Toast.LENGTH_SHORT);
                    }
                } catch (JSONException e) {
                    android.util.Log.e("MapFragment", "Error parsing base stations", e);
                }
            },
            error -> {
                if (!isFragmentValid()) return;
                android.util.Log.e("MapFragment", "Network error loading base stations", error);
                showToast("Network error loading base station", Toast.LENGTH_SHORT);
            }
        );
        requestQueue.add(request);
    }

    private void sendBaseStationsToMap(String jsonData) {
        if (!isFragmentValid() || leafletWebView == null) return;
        try {
            JSONObject response = new JSONObject(jsonData);
            if (!response.getBoolean("success")) return;

            JSONArray baseStationsArray = response.getJSONArray("data");
            StringBuilder jsArray = new StringBuilder("[");

            for (int i = 0; i < baseStationsArray.length(); i++) {
                if (i > 0) jsArray.append(",");
                JSONObject station = baseStationsArray.getJSONObject(i);
                jsArray.append("{")
                    .append("\"id\":").append(station.optInt("id", 0)).append(",")
                    .append("\"serial_number\":\"").append(escapeJsonString(station.optString("serial_number", ""))).append("\",")
                    .append("\"station_name\":\"").append(escapeJsonString(station.optString("station_name", "Base Station"))).append("\",")
                    .append("\"latitude\":").append(station.optDouble("latitude", 0)).append(",")
                    .append("\"longitude\":").append(station.optDouble("longitude", 0)).append(",")
                    .append("\"status\":\"").append(escapeJsonString(station.optString("status", "offline"))).append("\",")
                    .append("\"address\":\"").append(escapeJsonString(station.optString("address", ""))).append("\",")
                    .append("\"altitude\":").append(station.optDouble("altitude", 0)).append(",")
                    .append("\"online_devices\":").append(station.optInt("online_devices", 0)).append(",")
                    .append("\"total_devices\":").append(station.optInt("total_devices", 0)).append(",")
                    .append("\"geofence_radius_m\":").append(station.optInt("geofence_radius_m", 500))
                    .append("}");
            }
            jsArray.append("]");

            String javascript = String.format(
                "javascript:(function() {" +
                    "if (typeof window.MapFunctions !== 'undefined' && " +
                    "    typeof window.MapFunctions.addBaseStationMarkers === 'function') {" +
                    "   window.MapFunctions.addBaseStationMarkers(%s);" +
                    "} else { console.error('[Android] MapFunctions.addBaseStationMarkers not available'); }" +
                    "})();",
                jsArray.toString()
            );
            leafletWebView.evaluateJavascript(javascript, null);

        } catch (JSONException e) {
            android.util.Log.e("MapFragment", "Error processing base stations", e);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Firebase status check
    // ─────────────────────────────────────────────────────────

    private void checkFirebaseConnection() {
        if (!isFragmentValid() || leafletWebView == null) return;
        leafletWebView.postDelayed(() -> {
            if (!isFragmentValid()) return;
            String javascript = "javascript:(function() {" +
                "if (typeof window.checkFirebaseStatus === 'function') window.checkFirebaseStatus();" +
                "})();";
            leafletWebView.evaluateJavascript(javascript, null);
        }, 2000);
    }

    // ─────────────────────────────────────────────────────────
    //  Utility
    // ─────────────────────────────────────────────────────────

    private String resolveStaffRoleLocal() {
        return resolveStaffRole(sharedPrefManager.getUserType());
    }

    private String escapeJsonString(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String escapeJavaScriptString(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("'", "\\'")
            .replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private void showToast(String message, int duration) {
        if (isFragmentValid() && getActivity() != null) {
            getActivity().runOnUiThread(() ->
                Toast.makeText(getContext(), message, duration).show()
            );
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Fragment lifecycle
    // ─────────────────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive = true;
        if (leafletWebView != null && isFragmentValid()) {
            leafletWebView.postDelayed(() -> {
                if (!isFragmentValid()) return;
                injectStaffData();
                loadBaseStations();
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
    public void onDestroy() {
        super.onDestroy();
        if (leafletWebView != null) {
            leafletWebView.destroy();
        }
    }
}
