package com.example.link;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailInput, passwordInput;
    private MaterialButton loginButton;
    private TextView forgotPasswordText;

    private RequestQueue requestQueue;

    // SharedPreferences keys
    private static final String PREFS_NAME = "LinkPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USER_TYPE = "user_type";
    private static final String KEY_STAFF_ID = "staff_id";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_STAFF_NAME = "staff_name";
    private static final String KEY_CONTACT = "contact";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Check if user is already logged in
        if (isLoggedIn()) {
            redirectToMainActivity();
            return;
        }

        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(this);

        initViews();
        setupListeners();
    }

    private boolean isLoggedIn() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    private void redirectToMainActivity() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String userType = prefs.getString(KEY_USER_TYPE, "");

        // Double-check: If somehow admin got saved, force logout
        if ("admin".equals(userType) || "user".equals(userType)) {
            logout(this);
            return;
        }

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra(KEY_USER_ID, prefs.getInt(KEY_USER_ID, 0));
        intent.putExtra(KEY_USER_TYPE, userType);
        intent.putExtra(KEY_STAFF_ID, prefs.getInt(KEY_STAFF_ID, 0));
        intent.putExtra(KEY_IS_LOGGED_IN, true);
        intent.putExtra(KEY_STAFF_NAME, prefs.getString(KEY_STAFF_NAME, ""));

        startActivity(intent);
        finish();
    }

    private void initViews() {
        emailLayout = findViewById(R.id.loginEmailLayout);
        passwordLayout = findViewById(R.id.loginPasswordLayout);

        emailInput = findViewById(R.id.loginEmailInput);
        passwordInput = findViewById(R.id.loginPasswordInput);

        loginButton = findViewById(R.id.loginButton);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
    }

    private void setupListeners() {

        loginButton.setOnClickListener(v -> {
            if (validateInputs()) {
                performLogin();
            }
        });

        forgotPasswordText.setOnClickListener(v -> {
            Toast.makeText(this, "Please contact administrator to reset password", Toast.LENGTH_LONG).show();
        });
    }

    private boolean validateInputs() {
        emailLayout.setError(null);
        passwordLayout.setError(null);

        String email = emailInput.getText() != null
            ? emailInput.getText().toString().trim()
            : "";

        String password = passwordInput.getText() != null
            ? passwordInput.getText().toString().trim()
            : "";

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Enter a valid email");
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required");
            return false;
        }

        return true;
    }

    private void performLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Show loading
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        // Create JSON request body
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", email);
            jsonBody.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
            loginButton.setEnabled(true);
            loginButton.setText("Login");
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create Volley request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
            Request.Method.POST,
            ApiConfig.LOGIN_URL,
            jsonBody,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");

                    try {
                        boolean success = response.getBoolean("success");
                        String message = response.getString("message");

                        if (success) {
                            // Get user data
                            JSONObject userObject = response.getJSONObject("user");
                            String userType = userObject.getString("user_type");

                            // ONLY allow staff users to login to the app
                            if ("staff".equals(userType)) {
                                int userId = userObject.getInt("id");
                                String username = userObject.getString("username");
                                String email = userObject.getString("email");
                                String contact = userObject.optString("contact", "");
                                String status = userObject.optString("status", "active");

                                // Save user data to SharedPreferences
                                saveUserData(userId, username, email, userType, contact, status, userObject);

                                // Show success message
                                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();

                                // Redirect to MainActivity
                                redirectToMainActivity(userObject);

                            } else {
                                // Admin or regular user trying to login to app - NOT ALLOWED
                                Toast.makeText(LoginActivity.this,
                                    "Only staff members can login to the app. Please use the web portal.",
                                    Toast.LENGTH_LONG).show();

                                // Clear password field
                                passwordInput.setText("");
                            }

                        } else {
                            // Show error message from server
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();

                            // Clear password field on error
                            passwordInput.setText("");
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(LoginActivity.this, "Invalid server response", Toast.LENGTH_SHORT).show();
                        Log.e("LoginActivity", "JSON parsing error: " + e.getMessage());
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");

                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        String errorMsg = new String(error.networkResponse.data);
                        Log.e("LoginActivity", "Volley error: " + errorMsg);
                        try {
                            JSONObject errorJson = new JSONObject(errorMsg);
                            String serverMessage = errorJson.getString("message");
                            Toast.makeText(LoginActivity.this, serverMessage, Toast.LENGTH_SHORT).show();
                        } catch (JSONException e) {
                            Toast.makeText(LoginActivity.this,
                                "Network error. Please check your connection.",
                                Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("LoginActivity", "Volley error: " + error.toString());
                        Toast.makeText(LoginActivity.this,
                            "Network error. Please check your connection.",
                            Toast.LENGTH_SHORT).show();
                    }
                }
            }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Accept", "application/json");
                return headers;
            }
        };

        // Add request to the RequestQueue
        requestQueue.add(jsonObjectRequest);
    }

    private void saveUserData(int userId, String username, String email, String userType,
                              String contact, String status, JSONObject userObject) throws JSONException {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_USER_TYPE, userType);
        editor.putString(KEY_CONTACT, contact);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);

        // Save additional data if available
        if (userObject.has("staff_id") && !userObject.isNull("staff_id")) {
            editor.putInt(KEY_STAFF_ID, userObject.getInt("staff_id"));
        }

        if (userObject.has("staff_name") && !userObject.isNull("staff_name")) {
            editor.putString(KEY_STAFF_NAME, userObject.getString("staff_name"));
        }

        if (userObject.has("added_by") && !userObject.isNull("added_by")) {
            editor.putInt("added_by", userObject.getInt("added_by"));
        }

        editor.apply();

        Log.d("LoginActivity", "Staff data saved: " + username + " (" + userType + ")");
    }

    private void redirectToMainActivity(JSONObject userObject) throws JSONException {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);

        // Pass user data to MainActivity
        intent.putExtra(KEY_USER_ID, userObject.getInt("id"));
        intent.putExtra(KEY_USER_TYPE, userObject.getString("user_type"));
        intent.putExtra(KEY_EMAIL, userObject.getString("email"));
        intent.putExtra(KEY_USERNAME, userObject.getString("username"));

        if (userObject.has("staff_id") && !userObject.isNull("staff_id")) {
            intent.putExtra(KEY_STAFF_ID, userObject.getInt("staff_id"));
        }

        if (userObject.has("staff_name") && !userObject.isNull("staff_name")) {
            intent.putExtra(KEY_STAFF_NAME, userObject.getString("staff_name"));
        }

        if (userObject.has("contact") && !userObject.isNull("contact")) {
            intent.putExtra(KEY_CONTACT, userObject.getString("contact"));
        }

        // Add flags to prevent going back to login
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear password field when activity resumes
        if (passwordInput != null) {
            passwordInput.setText("");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel all requests when activity is destroyed
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
    }

    // Helper method to clear user session (logout)
    public static void logout(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Redirect to LoginActivity
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    // Helper method to get current user ID
    public static int getCurrentUserId(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, 0);
    }

    // Helper method to get current user type
    public static String getCurrentUserType(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_USER_TYPE, "");
    }

    // Helper method to check if user is staff
    public static boolean isStaff(android.content.Context context) {
        String userType = getCurrentUserType(context);
        return "staff".equals(userType);
    }

    // Helper method to get staff ID
    public static int getStaffId(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(KEY_STAFF_ID, 0);
    }

    // Helper method to get staff name
    public static String getStaffName(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_STAFF_NAME, "");
    }

    // Helper method to get contact number
    public static String getContact(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_CONTACT, "");
    }
}
