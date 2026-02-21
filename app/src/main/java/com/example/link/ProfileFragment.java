package com.example.link;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final int PICK_IMAGE_REQUEST = 1;

    // UI Components
    private ShapeableImageView profilePicture;
    private ImageButton        cameraButton;
    private ImageButton        editButton;
    private EditText           fullNameEditText;
    private TextView           emailTextView;
    private EditText           contactEditText;
    private TextView           roleTextView;
    private Button             cancelButton;
    private Button             saveButton;
    private LinearLayout       actionButtonsContainer;

    // State
    private boolean isEditMode       = false;
    private Uri     selectedImageUri = null;
    private String  base64Image      = null;

    private SharedPrefManager sharedPrefManager;
    private RequestQueue      requestQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPrefManager = SharedPrefManager.getInstance(requireContext());
        requestQueue      = Volley.newRequestQueue(requireContext());

        profilePicture         = view.findViewById(R.id.profile_picture);
        cameraButton           = view.findViewById(R.id.camera_button);
        editButton             = view.findViewById(R.id.edit_button);
        fullNameEditText       = view.findViewById(R.id.full_name_input);
        emailTextView          = view.findViewById(R.id.email_input);
        contactEditText        = view.findViewById(R.id.contact_input);
        roleTextView           = view.findViewById(R.id.role_input);
        cancelButton           = view.findViewById(R.id.cancel_button);
        saveButton             = view.findViewById(R.id.save_button);
        actionButtonsContainer = view.findViewById(R.id.action_buttons_container);

        loadProfileData();

        cameraButton.setOnClickListener(v -> openImagePicker());
        editButton  .setOnClickListener(v -> toggleEditMode());
        cancelButton.setOnClickListener(v -> handleCancel());
        saveButton  .setOnClickListener(v -> handleSave());

        updateUIState();
    }

    private void loadProfileData() {
        String staffName = sharedPrefManager.getStaffName();
        if (staffName == null || staffName.isEmpty()) {
            staffName = sharedPrefManager.getUsername();
        }

        String email    = sharedPrefManager.getEmail();
        String contact  = sharedPrefManager.getContact();
        String userType = sharedPrefManager.getUserType();

        String role;
        if ("admin".equals(userType) || "super_admin".equals(userType)) {
            role = "Administrator";
        } else if ("staff".equals(userType)) {
            String permission = sharedPrefManager.getStaffPermission();
            switch (permission) {
                case "full-access": role = "Full Access"; break;
                case "map-only":    role = "Map Only";    break;
                case "logs-only":   role = "Logs Only";   break;
                default:            role = "Staff";
            }
        } else {
            role = "User";
        }

        fullNameEditText.setText(staffName != null ? staffName : "");
        emailTextView   .setText(email     != null ? email     : "");
        contactEditText .setText(contact   != null ? contact   : "");
        roleTextView    .setText(role);

        // Load profile picture from server using cached path
        String picturePath = sharedPrefManager.getProfilePicture();
        if (picturePath != null && !picturePath.isEmpty()) {
            // uploads/ folder is a sibling of LinkApi/, so use UPLOADS_URL as the base
            String fullUrl = ApiConfig.UPLOADS_URL + picturePath;
            Log.d(TAG, "Loading profile picture from: " + fullUrl);
            Glide.with(this)
                .load(fullUrl)
                .circleCrop()
                .placeholder(R.drawable.profile_picture_bg)
                .error(R.drawable.profile_picture_bg)
                .into(profilePicture);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Edit / view mode toggle
    // ─────────────────────────────────────────────────────────
    private void toggleEditMode() {
        isEditMode = !isEditMode;
        updateUIState();
    }

    private void updateUIState() {
        if (isEditMode) {
            editButton            .setVisibility(View.GONE);
            fullNameEditText      .setEnabled(true);
            contactEditText       .setEnabled(true);
            cameraButton          .setVisibility(View.VISIBLE);
            actionButtonsContainer.setVisibility(View.VISIBLE);
        } else {
            editButton            .setVisibility(View.VISIBLE);
            fullNameEditText      .setEnabled(false);
            contactEditText       .setEnabled(false);
            cameraButton          .setVisibility(View.GONE);
            actionButtonsContainer.setVisibility(View.GONE);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Image picker
    // ─────────────────────────────────────────────────────────
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST
            && resultCode == Activity.RESULT_OK
            && data != null) {

            selectedImageUri = data.getData();
            profilePicture.setImageURI(selectedImageUri);

            base64Image = encodeImageToBase64(selectedImageUri);
            if (base64Image == null) {
                Toast.makeText(requireContext(),
                    "Failed to process image. Please try another.",
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String encodeImageToBase64(Uri imageUri) {
        try {
            InputStream inputStream = requireContext()
                .getContentResolver()
                .openInputStream(imageUri);

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();
            if (bitmap == null) return null;

            bitmap = scaleBitmap(bitmap, 512);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);

        } catch (Exception e) {
            Log.e(TAG, "encodeImageToBase64 error: ", e);
            return null;
        }
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int maxPx) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w <= maxPx && h <= maxPx) return bitmap;
        float scale = Math.min((float) maxPx / w, (float) maxPx / h);
        return Bitmap.createScaledBitmap(bitmap,
            Math.round(w * scale), Math.round(h * scale), true);
    }

    // ─────────────────────────────────────────────────────────
    //  Save / Cancel
    // ─────────────────────────────────────────────────────────
    private void handleCancel() {
        isEditMode       = false;
        selectedImageUri = null;
        base64Image      = null;
        updateUIState();
        loadProfileData();
        Toast.makeText(requireContext(), "Changes discarded", Toast.LENGTH_SHORT).show();
    }

    private void handleSave() {
        String fullName = fullNameEditText.getText().toString().trim();
        String contact  = contactEditText .getText().toString().trim();

        if (fullName.isEmpty()) {
            Toast.makeText(requireContext(), "Full name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (contact.isEmpty()) {
            Toast.makeText(requireContext(), "Contact number cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!contact.matches("^09[0-9]{9}$")) {
            Toast.makeText(requireContext(),
                "Invalid contact number (must be 09XXXXXXXXX)",
                Toast.LENGTH_SHORT).show();
            return;
        }

        sharedPrefManager.setStaffName(fullName);
        sharedPrefManager.setContact(contact);

        updateProfileOnServer(fullName, contact);
    }

    // ─────────────────────────────────────────────────────────
    //  API call
    // ─────────────────────────────────────────────────────────
    private void updateProfileOnServer(String fullName, String contact) {
        int userId = sharedPrefManager.getUserId();

        try {
            JSONObject body = new JSONObject();
            body.put("user_id",   userId);
            body.put("full_name", fullName);
            body.put("contact",   contact);

            if (base64Image != null) {
                body.put("profile_picture", base64Image);
                Log.d(TAG, "Sending profile picture (" + base64Image.length() + " chars)");
            }

            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiConfig.UPDATE_PROFILE_URL,
                body,
                response -> {
                    try {
                        Log.d(TAG, "Server response: " + response);

                        if (response.optBoolean("success")) {
                            isEditMode       = false;
                            selectedImageUri = null;
                            base64Image      = null;
                            updateUIState();
                            Toast.makeText(requireContext(),
                                "Profile updated successfully",
                                Toast.LENGTH_SHORT).show();

                            // Cache the returned picture path and reload it
                            if (response.has("profile")) {
                                JSONObject profile = response.getJSONObject("profile");
                                String savedPicture = profile.optString("profile_picture", "");
                                if (!savedPicture.isEmpty()) {
                                    sharedPrefManager.setProfilePicture(savedPicture);
                                    // Immediately display the newly saved picture
                                    String fullUrl = ApiConfig.UPLOADS_URL + savedPicture;
                                    Glide.with(this)
                                        .load(fullUrl)
                                        .circleCrop()
                                        .placeholder(R.drawable.profile_picture_bg)
                                        .error(R.drawable.profile_picture_bg)
                                        .into(profilePicture);
                                }
                            }
                        } else {
                            String error = response.optString("error",
                                response.optString("message", "Update failed"));
                            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Parse error: ", e);
                        Toast.makeText(requireContext(),
                            "Parse error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error: ", error);
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Status: " + error.networkResponse.statusCode);
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Log.e(TAG, "Body: " + responseBody);
                        } catch (Exception ignored) {}
                    }
                    Toast.makeText(requireContext(),
                        "Network error. Changes saved locally only.",
                        Toast.LENGTH_SHORT).show();
                    isEditMode = false;
                    updateUIState();
                }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Accept",       "application/json");
                    return headers;
                }
            };

            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                30_000,
                com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(request);

        } catch (Exception e) {
            Log.e(TAG, "Error creating request: ", e);
            Toast.makeText(requireContext(),
                "Error: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }
}
