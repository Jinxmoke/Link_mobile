package com.example.link;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

public class SharedPrefManager {
    // Use the SAME keys as your LoginActivity
    private static final String PREFS_NAME = "LinkPrefs";
    private static final String KEY_USER_ID   = "user_id";
    private static final String KEY_USERNAME  = "username";
    private static final String KEY_EMAIL     = "email";
    private static final String KEY_USER_TYPE = "user_type";
    private static final String KEY_STAFF_ID  = "staff_id";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_STAFF_NAME  = "staff_name";
    private static final String KEY_CONTACT     = "contact";

    // Permission related keys
    private static final String KEY_PERMISSIONS_REQUESTED        = "permissions_requested";
    private static final String KEY_FIRST_TIME_LAUNCH            = "first_time_launch";
    private static final String KEY_CAMERA_PERMISSION_GRANTED    = "camera_permission_granted";
    private static final String KEY_LOCATION_PERMISSION_GRANTED  = "location_permission_granted";
    private static final String KEY_PERMISSION_SCREEN_SHOWN      = "permission_screen_shown";
    private static final String KEY_PERMISSION_DENIED_COUNT      = "permission_denied_count";

    private static SharedPrefManager instance;
    private final Context mCtx;

    private SharedPrefManager(Context context) {
        mCtx = context.getApplicationContext();
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context);
        }
        return instance;
    }

    public Context getContext() {
        return mCtx;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  User data
    // ──────────────────────────────────────────────────────────────────────────

    public void saveUser(int userId, String username, String email, String userType,
                         String contact, String staffName, int staffId) {
        mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt   (KEY_USER_ID,      userId)
            .putString(KEY_USERNAME,     username)
            .putString(KEY_EMAIL,        email)
            .putString(KEY_USER_TYPE,    userType)
            .putString(KEY_CONTACT,      contact)
            .putString(KEY_STAFF_NAME,   staffName)
            .putInt   (KEY_STAFF_ID,     staffId)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply();
    }

    public boolean isLoggedIn() {
        return mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public int getUserId() {
        return mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_USER_ID, 0);
    }

    public String getUsername() {
        return mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USERNAME, "");
    }

    public String getUserType() {
        return mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_TYPE, "");
    }

    public String getEmail() {
        return mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_EMAIL, "");
    }

    public String getContact() {
        return mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CONTACT, "");
    }

    public int getStaffId() {
        return mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_STAFF_ID, 0);
    }

    public String getStaffName() {
        return mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_STAFF_NAME, "");
    }

    // ── NEW: setters used by ProfileFragment to persist edits ─────────────────

    /** Update the staff/display name without touching any other field. */
    public void setStaffName(String name) {
        mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STAFF_NAME, name)
            .apply();
    }

    /** Update the contact number without touching any other field. */
    public void setContact(String contact) {
        mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CONTACT, contact)
            .apply();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Logout
    // ──────────────────────────────────────────────────────────────────────────

    public void logout() {
        mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_EMAIL)
            .remove(KEY_USER_TYPE)
            .remove(KEY_STAFF_ID)
            .remove(KEY_STAFF_NAME)
            .remove(KEY_CONTACT)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Role helpers
    // ──────────────────────────────────────────────────────────────────────────

    public boolean isStaff() {
        return "staff".equals(getUserType());
    }

    public boolean isAdmin() {
        return "admin".equals(getUserType());
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Permission management
    // ──────────────────────────────────────────────────────────────────────────

    public boolean isFirstTimeLaunch() {
        return mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_FIRST_TIME_LAUNCH, true);
    }

    public void setFirstTimeLaunchCompleted() {
        mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_FIRST_TIME_LAUNCH, false).apply();
    }

    public boolean isPermissionScreenShown() {
        return mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PERMISSION_SCREEN_SHOWN, false);
    }

    public void setPermissionScreenShown() {
        mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PERMISSION_SCREEN_SHOWN, true).apply();
    }

    public boolean arePermissionsRequested() {
        return mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PERMISSIONS_REQUESTED, false);
    }

    public void setPermissionsRequested(boolean requested) {
        mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PERMISSIONS_REQUESTED, requested).apply();
    }

    public void setCameraPermissionGranted(boolean granted) {
        mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_CAMERA_PERMISSION_GRANTED, granted).apply();
    }

    public void setLocationPermissionGranted(boolean granted) {
        mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_LOCATION_PERMISSION_GRANTED, granted).apply();
    }

    public boolean isCameraPermissionGranted() {
        return mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_CAMERA_PERMISSION_GRANTED, false);
    }

    public boolean isLocationPermissionGranted() {
        return mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_LOCATION_PERMISSION_GRANTED, false);
    }

    public int getPermissionDeniedCount() {
        return mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_PERMISSION_DENIED_COUNT, 0);
    }

    public void incrementPermissionDeniedCount() {
        mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_PERMISSION_DENIED_COUNT, getPermissionDeniedCount() + 1).apply();
    }

    public void resetPermissionDeniedCount() {
        mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_PERMISSION_DENIED_COUNT, 0).apply();
    }

    public boolean areCriticalPermissionsMissing() {
        boolean hasLocation = ContextCompat.checkSelfPermission(
            mCtx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        setLocationPermissionGranted(hasLocation);
        return !hasLocation;
    }

    public boolean areAllPermissionsGranted() {
        boolean cameraGranted = ContextCompat.checkSelfPermission(
            mCtx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean fineLocation = ContextCompat.checkSelfPermission(
            mCtx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseLocation = ContextCompat.checkSelfPermission(
            mCtx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        setCameraPermissionGranted(cameraGranted);
        setLocationPermissionGranted(fineLocation || coarseLocation);

        return cameraGranted && (fineLocation || coarseLocation);
    }

    public boolean shouldShowPermissionScreen() {
        if (isFirstTimeLaunch())        return true;
        if (!arePermissionsRequested()) return true;
        if (areCriticalPermissionsMissing()) {
            return getPermissionDeniedCount() < 2;
        }
        return false;
    }

    public String getPermissionStatus() {
        boolean camera   = isCameraPermissionGranted();
        boolean location = isLocationPermissionGranted();
        if (camera && location)   return "All permissions granted";
        if (!camera && !location) return "Camera and Location permissions needed";
        if (!camera)              return "Camera permission needed";
        return                           "Location permission needed";
    }

    public void clearAllPreferences() {
        mCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply();
    }

    public String getPermissionStats() {
        return "First Launch: "           + isFirstTimeLaunch()          + "\n" +
            "Permissions Requested: "  + arePermissionsRequested()    + "\n" +
            "Permission Screen Shown: "+ isPermissionScreenShown()    + "\n" +
            "Camera Permission: "      + isCameraPermissionGranted()  + "\n" +
            "Location Permission: "    + isLocationPermissionGranted()+ "\n" +
            "Denied Count: "           + getPermissionDeniedCount();
    }

    public void saveCurrentPermissionStates() {
        boolean camera  = ContextCompat.checkSelfPermission(
            mCtx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean fine    = ContextCompat.checkSelfPermission(
            mCtx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse  = ContextCompat.checkSelfPermission(
            mCtx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        setCameraPermissionGranted(camera);
        setLocationPermissionGranted(fine || coarse);

        if (!arePermissionsRequested() && (camera || fine || coarse)) {
            setPermissionsRequested(true);
        }
    }
}
