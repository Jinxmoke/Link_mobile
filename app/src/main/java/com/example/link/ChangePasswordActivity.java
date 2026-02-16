package com.example.link;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.regex.Pattern;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etOldPassword, etNewPassword, etConfirmPassword;
    private ImageView btnToggleOldPassword, btnToggleNewPassword, btnToggleConfirmPassword;
    private TextView tvPasswordStrength, tvPasswordMatch;
    private View strengthBar1, strengthBar2, strengthBar3, strengthBar4;
    private androidx.appcompat.widget.AppCompatButton btnSavePassword;

    private boolean isOldPasswordVisible = false;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    private SharedPrefManager sharedPrefManager;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPrefManager = SharedPrefManager.getInstance(this);
        requestQueue = Volley.newRequestQueue(this);

        initViews();
        setupClickListeners();
        setupTextWatchers();
    }

    private void initViews() {
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnToggleOldPassword = findViewById(R.id.btnToggleOldPassword);
        btnToggleNewPassword = findViewById(R.id.btnToggleNewPassword);
        btnToggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword);
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);
        tvPasswordMatch = findViewById(R.id.tvPasswordMatch);
        strengthBar1 = findViewById(R.id.strengthBar1);
        strengthBar2 = findViewById(R.id.strengthBar2);
        strengthBar3 = findViewById(R.id.strengthBar3);
        strengthBar4 = findViewById(R.id.strengthBar4);
        btnSavePassword = findViewById(R.id.btnSavePassword);

        updatePasswordStrength("");
    }

    private void setupClickListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        btnToggleOldPassword.setOnClickListener(v -> {
            togglePasswordVisibility(etOldPassword, btnToggleOldPassword, isOldPasswordVisible);
            isOldPasswordVisible = !isOldPasswordVisible;
        });

        btnToggleNewPassword.setOnClickListener(v -> {
            togglePasswordVisibility(etNewPassword, btnToggleNewPassword, isNewPasswordVisible);
            isNewPasswordVisible = !isNewPasswordVisible;
        });

        btnToggleConfirmPassword.setOnClickListener(v -> {
            togglePasswordVisibility(etConfirmPassword, btnToggleConfirmPassword, isConfirmPasswordVisible);
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
        });

        btnSavePassword.setOnClickListener(v -> {
            if (validateInputs()) {
                changePassword();
            }
        });
    }

    private void setupTextWatchers() {
        etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updatePasswordStrength(s.toString());
                checkPasswordMatch();
            }
        });

        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                checkPasswordMatch();
            }
        });
    }

    private void togglePasswordVisibility(EditText editText, ImageView imageView, boolean isVisible) {
        if (isVisible) {
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            imageView.setImageResource(R.drawable.ic_eye);
        } else {
            editText.setTransformationMethod(null);
            imageView.setImageResource(R.drawable.ic_eye_off);
        }
        editText.setSelection(editText.getText().length());
    }

    private void updatePasswordStrength(String password) {
        int strength = calculatePasswordStrength(password);

        switch (strength) {
            case 0:
                setStrengthBars(ContextCompat.getColor(this, R.color.red_400), 0);
                tvPasswordStrength.setText("Password is too weak");
                tvPasswordStrength.setTextColor(ContextCompat.getColor(this, R.color.red_400));
                break;
            case 1:
                setStrengthBars(ContextCompat.getColor(this, R.color.red_400), 1);
                tvPasswordStrength.setText("Weak password");
                tvPasswordStrength.setTextColor(ContextCompat.getColor(this, R.color.red_400));
                break;
            case 2:
                setStrengthBars(ContextCompat.getColor(this, R.color.orange_400), 2);
                tvPasswordStrength.setText("Fair password");
                tvPasswordStrength.setTextColor(ContextCompat.getColor(this, R.color.orange_400));
                break;
            case 3:
                setStrengthBars(ContextCompat.getColor(this, R.color.blue_400), 3);
                tvPasswordStrength.setText("Good password");
                tvPasswordStrength.setTextColor(ContextCompat.getColor(this, R.color.blue_400));
                break;
            case 4:
                setStrengthBars(ContextCompat.getColor(this, R.color.green_400), 4);
                tvPasswordStrength.setText("Strong password");
                tvPasswordStrength.setTextColor(ContextCompat.getColor(this, R.color.green_400));
                break;
        }
    }

    private void setStrengthBars(int color, int filledBars) {
        View[] bars = {strengthBar1, strengthBar2, strengthBar3, strengthBar4};

        for (int i = 0; i < bars.length; i++) {
            if (i < filledBars) {
                bars[i].setBackgroundColor(color);
            } else {
                bars[i].setBackgroundColor(ContextCompat.getColor(this, R.color.gray_300));
            }
        }
    }

    private int calculatePasswordStrength(String password) {
        if (password.isEmpty()) return 0;

        int score = 0;

        // Length check
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;

        // Character variety checks
        Pattern uppercase = Pattern.compile("[A-Z]");
        Pattern lowercase = Pattern.compile("[a-z]");
        Pattern digit = Pattern.compile("[0-9]");
        Pattern special = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

        boolean hasUppercase = uppercase.matcher(password).find();
        boolean hasLowercase = lowercase.matcher(password).find();
        boolean hasDigit = digit.matcher(password).find();
        boolean hasSpecial = special.matcher(password).find();

        if (hasUppercase && hasLowercase) score++;
        if (hasDigit) score++;
        if (hasSpecial) score++;

        return Math.min(score, 4);
    }

    private void checkPasswordMatch() {
        String newPassword = etNewPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (confirmPassword.isEmpty()) {
            tvPasswordMatch.setVisibility(View.GONE);
            return;
        }

        if (newPassword.equals(confirmPassword)) {
            tvPasswordMatch.setText("✓ Passwords match");
            tvPasswordMatch.setTextColor(ContextCompat.getColor(this, R.color.green_400));
            tvPasswordMatch.setVisibility(View.VISIBLE);
        } else {
            tvPasswordMatch.setText("✗ Passwords don't match");
            tvPasswordMatch.setTextColor(ContextCompat.getColor(this, R.color.red_400));
            tvPasswordMatch.setVisibility(View.VISIBLE);
        }
    }

    private boolean validateInputs() {
        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (oldPassword.isEmpty()) {
            showError("Please enter your current password");
            return false;
        }

        if (newPassword.isEmpty()) {
            showError("Please enter a new password");
            return false;
        }

        if (newPassword.length() < 8) {
            showError("Password must be at least 8 characters long");
            return false;
        }

        // Check for uppercase
        if (!Pattern.compile("[A-Z]").matcher(newPassword).find()) {
            showError("Password must contain at least one uppercase letter");
            return false;
        }

        // Check for lowercase
        if (!Pattern.compile("[a-z]").matcher(newPassword).find()) {
            showError("Password must contain at least one lowercase letter");
            return false;
        }

        // Check for digit
        if (!Pattern.compile("[0-9]").matcher(newPassword).find()) {
            showError("Password must contain at least one number");
            return false;
        }

        if (confirmPassword.isEmpty()) {
            showError("Please confirm your new password");
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Passwords do not match");
            return false;
        }

        if (oldPassword.equals(newPassword)) {
            showError("New password must be different from current password");
            return false;
        }

        return true;
    }

    private void changePassword() {
        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        int userId = sharedPrefManager.getUserId();

        if (userId == 0) {
            showError("User not logged in");
            return;
        }

        btnSavePassword.setEnabled(false);
        btnSavePassword.setText("Changing...");

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("user_id", userId);
            requestBody.put("old_password", oldPassword);
            requestBody.put("new_password", newPassword);
            requestBody.put("confirm_password", confirmPassword);

            android.util.Log.d("ChangePassword", "Request: " + requestBody.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiConfig.CHANGE_PASSWORD_URL,
                requestBody,
                response -> {
                    android.util.Log.d("ChangePassword", "Response: " + response.toString());

                    try {
                        boolean success = response.getBoolean("success");
                        String message = response.getString("message");

                        if (success) {
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                            // Clear input fields
                            etOldPassword.setText("");
                            etNewPassword.setText("");
                            etConfirmPassword.setText("");

                            // Close activity after a delay
                            new android.os.Handler().postDelayed(() -> {
                                finish();
                            }, 1500);
                        } else {
                            showError(message);
                            btnSavePassword.setEnabled(true);
                            btnSavePassword.setText("Change Password");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("Error processing response");
                        btnSavePassword.setEnabled(true);
                        btnSavePassword.setText("Change Password");
                    }
                },
                error -> {
                    android.util.Log.e("ChangePassword", "Error: " + error.toString());

                    String errorMessage = "Network error. Please try again.";

                    if (error.networkResponse != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            android.util.Log.e("ChangePassword", "Error response: " + responseBody);

                            JSONObject errorJson = new JSONObject(responseBody);
                            if (errorJson.has("message")) {
                                errorMessage = errorJson.getString("message");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    showError(errorMessage);
                    btnSavePassword.setEnabled(true);
                    btnSavePassword.setText("Change Password");
                }
            );

            requestQueue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error creating request");
            btnSavePassword.setEnabled(true);
            btnSavePassword.setText("Change Password");
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
    }
}
