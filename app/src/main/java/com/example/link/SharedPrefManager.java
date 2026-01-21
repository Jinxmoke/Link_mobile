package com.example.link;

import android.content.Context;
import android.content.SharedPreferences;

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

    private static SharedPrefManager instance;
    private static Context mCtx;

    private SharedPrefManager(Context context) {
        mCtx = context;
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context.getApplicationContext());
        }
        return instance;
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
        editor.clear();
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
}
