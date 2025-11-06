package com.example.link;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.link.databinding.ActivityDeviceVerificationBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DeviceVerificationActivity extends AppCompatActivity {

    private ActivityDeviceVerificationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeviceVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String deviceId = binding.deviceIdInput.getText().toString().trim();

                if (deviceId.isEmpty()) {
                    binding.deviceIdInput.setError("Please enter your device ID");
                    return;
                }

                verifyDevice(deviceId);
            }
        });
    }

    private void verifyDevice(final String deviceId) { // <-- add 'final' here
        StringRequest request = new StringRequest(Request.Method.POST, ApiConfig.VERIFY_DEVICE_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject json = new JSONObject(response);
                        boolean success = json.getBoolean("success");
                        String status = json.getString("status");
                        String message = json.getString("message");

                        if (success && status.equalsIgnoreCase("unregistered")) {
                            Toast.makeText(DeviceVerificationActivity.this,
                                "Device verified! Redirecting to signup...", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(DeviceVerificationActivity.this, SignupActivity.class);
                            intent.putExtra("device_id", deviceId); // now works reliably
                            startActivity(intent);
                            finish();

                        } else {
                            Toast.makeText(DeviceVerificationActivity.this, message, Toast.LENGTH_LONG).show();
                        }

                    } catch (JSONException e) {
                        Toast.makeText(DeviceVerificationActivity.this,
                            "Invalid server response", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(DeviceVerificationActivity.this,
                        "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                }
            }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("device_id", deviceId);
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(DeviceVerificationActivity.this);
        queue.add(request);
    }
}
