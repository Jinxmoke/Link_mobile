package com.example.link;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailInput, passwordInput;
    private MaterialButton loginButton;
    private TextView signUpLink, forgotPasswordText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.loginEmailInput);
        passwordInput = findViewById(R.id.loginPasswordInput);
        loginButton = findViewById(R.id.loginButton);
        signUpLink = findViewById(R.id.signUpLink);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter your email and password", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });

        signUpLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, DeviceVerificationActivity.class);
            startActivity(intent);
        });

        forgotPasswordText.setOnClickListener(v ->
            Toast.makeText(this, "Forgot password clicked", Toast.LENGTH_SHORT).show()
        );
    }

    private void loginUser(String email, String password) {
        StringRequest request = new StringRequest(Request.Method.POST, ApiConfig.LOGIN_URL,
            response -> {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    boolean success = jsonObject.getBoolean("success");
                    String message = jsonObject.getString("message");
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                    if (success) {
                        String userType = jsonObject.getString("user_type");

                        if (userType.equals("rescuer")) {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("userType", userType); // ✅ send userType to MainActivity
                            startActivity(intent);
                        } else if (userType.equals("user")) {
                            Intent intent = new Intent(LoginActivity.this, UserActivity.class);
                            intent.putExtra("userType", userType); // optional but consistent
                            startActivity(intent);
                        }
                        finish();
                    }

                } catch (JSONException e) {
                    Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                }
            },
            error -> Toast.makeText(this, "Network error: " + error.getMessage(), Toast.LENGTH_LONG).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("email", email);
                params.put("password", password);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}
