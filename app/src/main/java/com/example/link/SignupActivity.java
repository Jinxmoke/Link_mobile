package com.example.link;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.link.databinding.ActivitySignupBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private static final String TAG = "SignupDebug";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String deviceId = getIntent() != null ? getIntent().getStringExtra("device_id") : "";
        Log.d(TAG, "Device ID: " + deviceId);

        binding.signUpButton.setOnClickListener(v -> handleSignUp(deviceId));
        binding.signInLink.setOnClickListener(v -> finish());
    }

    private void handleSignUp(String deviceId) {
        String fullName = binding.fullNameInput.getText() != null ? binding.fullNameInput.getText().toString().trim() : "";
        String contact = binding.contactInput.getText() != null ? binding.contactInput.getText().toString().trim() : "";
        String email = binding.emailInput.getText() != null ? binding.emailInput.getText().toString().trim() : "";
        String password = binding.passwordInput.getText() != null ? binding.passwordInput.getText().toString().trim() : "";
        String confirmPassword = binding.confirmPasswordInput.getText() != null ? binding.confirmPasswordInput.getText().toString().trim() : "";

        // Validations
        if (fullName.isEmpty()) { binding.fullNameInput.setError("Full name is required"); return; }
        if (contact.isEmpty()) { binding.contactInput.setError("Contact is required"); return; }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.emailInput.setError("Valid email is required"); return; }
        if (!isValidPassword(password)) { binding.passwordInput.setError("Password must contain uppercase, lowercase, digit, special character and min 5 chars"); return; }
        if (!password.equals(confirmPassword)) { binding.confirmPasswordInput.setError("Passwords do not match"); return; }

        if (deviceId == null || deviceId.isEmpty()) {
            Toast.makeText(this, "Device ID is missing", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Device ID is null or empty");
            return;
        }

        sendTemporaryRegistration(fullName, contact, email, password, deviceId);
    }

    private boolean isValidPassword(String password) {
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*+=]).{5,}$";
        return password != null && password.matches(passwordPattern);
    }

    // Step 1: Send temporary registration to server
    private void sendTemporaryRegistration(String fullName, String contact, String email, String password, String deviceId) {
        StringRequest request = new StringRequest(Request.Method.POST, ApiConfig.REGISTER_URL,
            response -> {
                try {
                    JSONObject json = new JSONObject(response);
                    boolean success = json.getBoolean("success");
                    String message = json.getString("message");

                    Toast.makeText(SignupActivity.this, message, Toast.LENGTH_LONG).show();

                    if (success) {
                        // Show verification fragment using only deviceId
                        showVerificationFragment(deviceId);
                    }
                } catch (JSONException e) {
                    Toast.makeText(SignupActivity.this, "Invalid server response", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "JSON parsing error", e);
                }
            },
            error -> {
                Toast.makeText(SignupActivity.this, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Volley error", error);
            }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("full_name", fullName);
                params.put("contact", contact);
                params.put("email", email);
                params.put("password", password);
                params.put("device_id", deviceId);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    // Step 2: Show verification bottom sheet fragment
    private void showVerificationFragment(String deviceId) {
        VerificationFragment fragment = VerificationFragment.newInstance(
            deviceId,
            () -> finish()
        );
        fragment.show(getSupportFragmentManager(), "VerificationFragment");
    }
}
