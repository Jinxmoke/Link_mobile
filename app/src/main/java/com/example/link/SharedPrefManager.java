package com.example.link;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

public class SharedPrefManager {
    // Use the SAME keys as your LoginActivity
    private static final String PREFS_NAME = "LinkPrefs"; // Must match LoginActivity
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USER_TYPE = "user_type";
    private static final String KEY_STAFF_ID = "staff_id";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_STAFF_NAME = "staff_name";
    private static final String KEY_CONTACT = "contact";

    // Permission related keys
    private static final String KEY_PERMISSIONS_REQUESTED = "permissions_requested";
    private static final String KEY_FIRST_TIME_LAUNCH = "first_time_launch";
    private static final String KEY_CAMERA_PERMISSION_GRANTED = "camera_permission_granted";
    private static final String KEY_LOCATION_PERMISSION_GRANTED = "location_permission_granted";
    private static final String KEY_PERMISSION_SCREEN_SHOWN = "permission_screen_shown";
    private static final String KEY_PERMISSION_DENIED_COUNT = "permission_denied_count";

    private static SharedPrefManager instance;
    private Context mCtx;

    private SharedPrefManager(Context context) {
        mCtx = context.getApplicationContext();
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context);
        }
        return instance;
    }

    // Method to get context safely
    public Context getContext() {
        return mCtx;
    }

    // Method to save user data (compatible with your LoginActivity)
    public void saveUser(int userId, String username, String email, String userType,
                         String contact, String staffName, int staffId) {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_USER_TYPE, userType);
        editor.putString(KEY_CONTACT, contact);
        editor.putString(KEY_STAFF_NAME, staffName);
        editor.putInt(KEY_STAFF_ID, staffId);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    // Method to check if user is logged in
    public boolean isLoggedIn() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Method to get user ID
    public int getUserId() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, 0);
    }

    // Method to get username
    public String getUsername() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USERNAME, "");
    }

    // Method to get user type
    public String getUserType() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_TYPE, "");
    }

    // Method to get email
    public String getEmail() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_EMAIL, "");
    }

    // Method to get contact
    public String getContact() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CONTACT, "");
    }

    // Method to get staff ID
    public int getStaffId() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_STAFF_ID, 0);
    }

    // Method to get staff name
    public String getStaffName() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_STAFF_NAME, "");
    }

    // Method to clear user data (logout)
    public void logout() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Clear user data but keep permission settings
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_USER_TYPE);
        editor.remove(KEY_STAFF_ID);
        editor.remove(KEY_STAFF_NAME);
        editor.remove(KEY_CONTACT);
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();
    }

    // Helper method to check if user is staff
    public boolean isStaff() {
        return "staff".equals(getUserType());
    }

    // Helper method to check if user is admin
    public boolean isAdmin() {
        return "admin".equals(getUserType());
    }

    // ==================== PERMISSION MANAGEMENT METHODS ====================

    // Check if it's first time launch
    public boolean isFirstTimeLaunch() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_FIRST_TIME_LAUNCH, true);
    }

    // Mark first time launch as completed
    public void setFirstTimeLaunchCompleted() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_FIRST_TIME_LAUNCH, false);
        editor.apply();
    }

    // Check if permission screen was shown
    public boolean isPermissionScreenShown() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_PERMISSION_SCREEN_SHOWN, false);
    }

    // Mark permission screen as shown
    public void setPermissionScreenShown() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_PERMISSION_SCREEN_SHOWN, true);
        editor.apply();
    }

    // Check if permissions were requested
    public boolean arePermissionsRequested() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_PERMISSIONS_REQUESTED, false);
    }

    // Mark permissions as requested
    public void setPermissionsRequested(boolean requested) {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_PERMISSIONS_REQUESTED, requested);
        editor.apply();
    }

    // Save camera permission status
    public void setCameraPermissionGranted(boolean granted) {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_CAMERA_PERMISSION_GRANTED, granted);
        editor.apply();
    }

    // Save location permission status
    public void setLocationPermissionGranted(boolean granted) {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_LOCATION_PERMISSION_GRANTED, granted);
        editor.apply();
    }

    // Check if camera permission is granted (from storage)
    public boolean isCameraPermissionGranted() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_CAMERA_PERMISSION_GRANTED, false);
    }

    // Check if location permission is granted (from storage)
    public boolean isLocationPermissionGranted() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_LOCATION_PERMISSION_GRANTED, false);
    }

    // Get permission denied count
    public int getPermissionDeniedCount() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_PERMISSION_DENIED_COUNT, 0);
    }

    // Increment permission denied count
    public void incrementPermissionDeniedCount() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int currentCount = getPermissionDeniedCount();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_PERMISSION_DENIED_COUNT, currentCount + 1);
        editor.apply();
    }

    // Reset permission denied count
    public void resetPermissionDeniedCount() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_PERMISSION_DENIED_COUNT, 0);
        editor.apply();
    }

    // Check if critical permissions are missing
    public boolean areCriticalPermissionsMissing() {
        // Check runtime permissions
        boolean hasLocationPermission = ContextCompat.checkSelfPermission(mCtx,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // Update stored values
        setLocationPermissionGranted(hasLocationPermission);

        // Return true if critical permission is missing
        return !hasLocationPermission;
    }

    // Check all required permissions
    public boolean areAllPermissionsGranted() {
        boolean cameraGranted = ContextCompat.checkSelfPermission(mCtx,
            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        boolean fineLocationGranted = ContextCompat.checkSelfPermission(mCtx,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        boolean coarseLocationGranted = ContextCompat.checkSelfPermission(mCtx,
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // Update stored values
        setCameraPermissionGranted(cameraGranted);
        setLocationPermissionGranted(fineLocationGranted || coarseLocationGranted);

        return cameraGranted && (fineLocationGranted || coarseLocationGranted);
    }

    // Method to check if we should show permission screen
    public boolean shouldShowPermissionScreen() {
        // Always show on first launch
        if (isFirstTimeLaunch()) {
            return true;
        }

        // Show if permissions were never requested
        if (!arePermissionsRequested()) {
            return true;
        }

        // Show if critical permissions are missing
        if (areCriticalPermissionsMissing()) {
            // Check if user denied too many times
            if (getPermissionDeniedCount() >= 2) {
                // Don't bother user if they denied twice
                return false;
            }
            return true;
        }

        // Don't show if permissions are already granted
        return false;
    }

    // Get permission status summary
    public String getPermissionStatus() {
        boolean cameraGranted = isCameraPermissionGranted();
        boolean locationGranted = isLocationPermissionGranted();

        if (cameraGranted && locationGranted) {
            return "All permissions granted";
        } else if (!cameraGranted && !locationGranted) {
            return "Camera and Location permissions needed";
        } else if (!cameraGranted) {
            return "Camera permission needed";
        } else {
            return "Location permission needed";
        }
    }

    // Clear all preferences (for testing)
    public void clearAllPreferences() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    // Get permission statistics
    public String getPermissionStats() {
        SharedPreferences prefs = mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        StringBuilder stats = new StringBuilder();

        stats.append("First Launch: ").append(isFirstTimeLaunch()).append("\n");
        stats.append("Permissions Requested: ").append(arePermissionsRequested()).append("\n");
        stats.append("Permission Screen Shown: ").append(isPermissionScreenShown()).append("\n");
        stats.append("Camera Permission: ").append(isCameraPermissionGranted()).append("\n");
        stats.append("Location Permission: ").append(isLocationPermissionGranted()).append("\n");
        stats.append("Denied Count: ").append(getPermissionDeniedCount());

        return stats.toString();
    }

    // Save current permission states
    public void saveCurrentPermissionStates() {
        boolean cameraGranted = ContextCompat.checkSelfPermission(mCtx,
            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        boolean fineLocationGranted = ContextCompat.checkSelfPermission(mCtx,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        boolean coarseLocationGranted = ContextCompat.checkSelfPermission(mCtx,
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        setCameraPermissionGranted(cameraGranted);
        setLocationPermissionGranted(fineLocationGranted || coarseLocationGranted);

        // Mark as requested if any permission was asked
        if (!arePermissionsRequested() && (cameraGranted || fineLocationGranted || coarseLocationGranted)) {
            setPermissionsRequested(true);
        }
    }
}
