package com.example.link;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONObject;

public class QRScannerFragment extends Fragment {

    private DecoratedBarcodeView barcodeView;
    private static final int CAMERA_PERMISSION_REQUEST = 101;
    private boolean isScanning = false;
    private boolean isProcessing = false;

    private SharedPrefManager sharedPrefManager;
    private RequestQueue requestQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr_scanner, container, false);

        barcodeView = view.findViewById(R.id.barcodeScannerView);
        sharedPrefManager = SharedPrefManager.getInstance(requireContext());
        requestQueue = Volley.newRequestQueue(requireContext());

        checkCameraPermission();
        return view;
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST);
        } else {
            startScanning();
        }
    }

    private void startScanning() {
        if (isScanning) return; // Prevent multiple starts
        isScanning = true;

        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null && !isProcessing) {
                    isProcessing = true;
                    barcodeView.pause();

                    String scannedData = result.getText();
                    android.util.Log.d("QRScanner", "Scanned: " + scannedData);

                    // Process the scanned QR code
                    handleScannedQR(scannedData);
                }
            }
        });

        barcodeView.resume();
    }

    private void handleScannedQR(String qrData) {
        // QR data should contain the transmitter serial number
        // Format could be: "HELTEC-TR" or "SERIAL:HELTEC-TR" or JSON

        String transmitterSerial = extractSerialNumber(qrData);

        if (transmitterSerial == null || transmitterSerial.isEmpty()) {
            showError("Invalid QR code format");
            resumeScanning();
            return;
        }

        // Show confirmation dialog
        showResolveConfirmation(transmitterSerial);
    }

    private String extractSerialNumber(String qrData) {
        // Try to extract serial number from different formats

        // If it's just the serial number directly
        if (qrData.matches("^[A-Z0-9-]+$")) {
            return qrData;
        }

        // If it's in format "SERIAL:HELTEC-TR"
        if (qrData.contains(":")) {
            String[] parts = qrData.split(":");
            if (parts.length >= 2) {
                return parts[1].trim();
            }
        }

        // If it's JSON
        try {
            JSONObject json = new JSONObject(qrData);
            if (json.has("serial_number")) {
                return json.getString("serial_number");
            }
            if (json.has("serialNumber")) {
                return json.getString("serialNumber");
            }
            if (json.has("serial")) {
                return json.getString("serial");
            }
        } catch (Exception e) {
            // Not JSON, continue
        }

        // Default: assume the whole string is the serial
        return qrData.trim();
    }

    private void showResolveConfirmation(String transmitterSerial) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Resolve SOS Alert")
            .setMessage("Resolve SOS alert for device:\n\n" + transmitterSerial + "\n\nAre you sure?")
            .setPositiveButton("Resolve", (dialog, which) -> {
                resolveSOSAlert(transmitterSerial);
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                resumeScanning();
            })
            .setCancelable(false)
            .show();
    }

    private void resolveSOSAlert(String transmitterSerial) {
        int staffId = sharedPrefManager.getUserId();

        if (staffId == 0) {
            showError("User not logged in");
            resumeScanning();
            return;
        }

        android.util.Log.d("QRScanner", "=== Starting SOS Resolution ===");
        android.util.Log.d("QRScanner", "Transmitter Serial: " + transmitterSerial);
        android.util.Log.d("QRScanner", "Staff ID: " + staffId);
        android.util.Log.d("QRScanner", "URL: " + ApiConfig.RESOLVE_SOS_BY_QR_URL);

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("transmitter_serial", transmitterSerial);
            requestBody.put("staff_id", staffId);
            requestBody.put("resolution_notes", "Resolved via QR scan by " + sharedPrefManager.getUsername());

            android.util.Log.d("QRScanner", "Request Body: " + requestBody.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiConfig.RESOLVE_SOS_BY_QR_URL,
                requestBody,
                response -> {
                    android.util.Log.d("QRScanner", "=== SUCCESS Response ===");
                    android.util.Log.d("QRScanner", "Response: " + response.toString());

                    try {
                        boolean success = response.getBoolean("success");
                        String message = response.getString("message");

                        if (success) {
                            // Show success dialog
                            showSuccessDialog(message, transmitterSerial);
                        } else {
                            showError(message);
                            resumeScanning();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        android.util.Log.e("QRScanner", "Parse error: " + e.getMessage());
                        showError("Error processing response");
                        resumeScanning();
                    }
                },
                error -> {
                    android.util.Log.e("QRScanner", "=== ERROR Response ===");
                    android.util.Log.e("QRScanner", "Error: " + error.toString());

                    String errorMessage = "Network error";

                    if (error.networkResponse != null) {
                        android.util.Log.e("QRScanner", "Status Code: " + error.networkResponse.statusCode);

                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            android.util.Log.e("QRScanner", "Error Response Body: " + responseBody);

                            JSONObject errorJson = new JSONObject(responseBody);
                            if (errorJson.has("message")) {
                                errorMessage = errorJson.getString("message");
                            }
                        } catch (Exception e) {
                            android.util.Log.e("QRScanner", "Error parsing error response", e);
                        }
                    } else {
                        android.util.Log.e("QRScanner", "No network response - connection issue");
                        if (error.getCause() != null) {
                            android.util.Log.e("QRScanner", "Cause: " + error.getCause().getMessage());
                        }
                        errorMessage = "Cannot connect to server. Check:\n" +
                            "1. Internet connection\n" +
                            "2. Server URL: " + ApiConfig.RESOLVE_SOS_BY_QR_URL;
                    }

                    showError(errorMessage);
                    resumeScanning();
                }
            ) {
                @Override
                public java.util.Map<String, String> getHeaders() {
                    java.util.Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Accept", "application/json");
                    android.util.Log.d("QRScanner", "Headers: " + headers.toString());
                    return headers;
                }
            };

            // Set timeout
            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                30000,  // 30 seconds timeout
                0,      // No retries
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            android.util.Log.d("QRScanner", "Adding request to queue...");
            requestQueue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("QRScanner", "Exception creating request: " + e.getMessage());
            showError("Error creating request: " + e.getMessage());
            resumeScanning();
        }
    }

    private void showSuccessDialog(String message, String serial) {
        new AlertDialog.Builder(requireContext())
            .setTitle("✓ Success")
            .setMessage(message + "\n\nDevice: " + serial)
            .setPositiveButton("OK", (dialog, which) -> {
                closeScanner();
            })
            .setCancelable(false)
            .show();
    }

    private void showError(String message) {
        if (getActivity() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    private void resumeScanning() {
        isProcessing = false;
        if (barcodeView != null) {
            barcodeView.resume();
        }
    }

    private void closeScanner() {
        if (getActivity() != null) {
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .remove(QRScannerFragment.this)
                .commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (barcodeView != null && !isProcessing) {
            barcodeView.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (barcodeView != null) {
            barcodeView.pause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                showError("Camera permission is required to scan QR codes");
                closeScanner();
            }
        }
    }
}
