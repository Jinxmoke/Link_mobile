package com.example.link;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;

    // UI Components
    private ImageView    profilePicture;
    private ImageButton  cameraButton;
    private ImageButton  editButton;
    private EditText     fullNameEditText;
    private TextView     emailTextView;
    private EditText     contactEditText;
    private TextView     roleTextView;
    private Button       cancelButton;
    private Button       saveButton;
    private LinearLayout actionButtonsContainer;

    // State
    private boolean isEditMode = false;
    private Uri     selectedImageUri;

    // App's shared pref manager (same source as the rest of the app)
    private SharedPrefManager sharedPrefManager;

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

        // Use the app-wide SharedPrefManager so data matches the logged-in account
        sharedPrefManager = SharedPrefManager.getInstance(requireContext());

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

    // ─────────────────────────────────────────────────────────
    //  Load from SharedPrefManager (same data source as login)
    // ─────────────────────────────────────────────────────────
    private void loadProfileData() {
        // Staff name: prefer staffName, fall back to username
        String staffName = sharedPrefManager.getStaffName();
        if (staffName == null || staffName.isEmpty()) {
            staffName = sharedPrefManager.getUsername();
        }

        String email   = sharedPrefManager.getEmail();
        String contact = sharedPrefManager.getContact();

        // Role label based on user type
        String userType = sharedPrefManager.getUserType();
        String role;
        if ("admin".equals(userType))       role = "Administrator";
        else if ("staff".equals(userType))  role = "Staff";
        else                                role = "User";

        fullNameEditText.setText(staffName  != null ? staffName  : "");
        emailTextView   .setText(email      != null ? email      : "");
        contactEditText .setText(contact    != null ? contact    : "");
        roleTextView    .setText(role);
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
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Save / Cancel
    // ─────────────────────────────────────────────────────────
    private void handleCancel() {
        isEditMode = false;
        updateUIState();
        loadProfileData();   // restore original values
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

        // Persist back into SharedPrefManager so the rest of the app sees the update
        sharedPrefManager.setStaffName(fullName);
        sharedPrefManager.setContact(contact);

        isEditMode = false;
        updateUIState();
        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
    }
}
