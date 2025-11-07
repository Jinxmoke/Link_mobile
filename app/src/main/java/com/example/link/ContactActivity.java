package com.example.link;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import android.content.SharedPreferences;
import android.app.AlertDialog;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class ContactActivity extends AppCompatActivity {

    private EditText editFullName, editRelationship, editContactNumber, editEmail, editAddress;
    private AppCompatButton btnSaveContact;
    private LinearLayout listFamilyMembers;
    private RequestQueue queue;
    private String userId;
    private int editingContactId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        editFullName = findViewById(R.id.editFullName);
        editRelationship = findViewById(R.id.editRelationship);
        editContactNumber = findViewById(R.id.editContactNumber);
        editEmail = findViewById(R.id.editEmail);
        editAddress = findViewById(R.id.editAddress);
        btnSaveContact = findViewById(R.id.btnSaveContact);
        listFamilyMembers = findViewById(R.id.listFamilyMembers);

        queue = Volley.newRequestQueue(this);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = prefs.getString("user_id", "");

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnSaveContact.setOnClickListener(view -> saveFamilyMember());

        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        loadFamilyMembers();
    }

    private void saveFamilyMember() {
        String fullName = editFullName.getText().toString().trim();
        String relationship = editRelationship.getText().toString().trim();
        String contact = editContactNumber.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String address = editAddress.getText().toString().trim();

        if (fullName.isEmpty() || relationship.isEmpty() || contact.isEmpty()) {
            Toast.makeText(this, "Please fill out all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (editingContactId != -1) {
            updateFamilyMember(editingContactId, fullName, relationship, contact, email, address);
        } else {
            addFamilyMember(fullName, relationship, contact, email, address);
        }
    }

    private void addFamilyMember(String fullName, String relationship, String contact, String email, String address) {
        StringRequest request = new StringRequest(Request.Method.POST, ApiConfig.ADD_FAMILY_MEMBER_URL,
            response -> {
                Toast.makeText(ContactActivity.this, "Contact added successfully.", Toast.LENGTH_SHORT).show();
                clearFields();
                loadFamilyMembers();
            },
            error -> Toast.makeText(ContactActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", userId);
                params.put("full_name", fullName);
                params.put("relationship", relationship);
                params.put("contact_number", contact);
                params.put("email", email);
                params.put("address", address);
                return params;
            }
        };
        queue.add(request);
    }

    private void updateFamilyMember(int memberId, String fullName, String relationship, String contact, String email, String address) {
        StringRequest request = new StringRequest(Request.Method.POST, ApiConfig.UPDATE_FAMILY_MEMBER_URL,
            response -> {
                Toast.makeText(ContactActivity.this, "Contact updated successfully.", Toast.LENGTH_SHORT).show();
                clearFields();
                editingContactId = -1;
                btnSaveContact.setText("Add Contact");
                loadFamilyMembers();
            },
            error -> Toast.makeText(ContactActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("id", String.valueOf(memberId));
                params.put("full_name", fullName);
                params.put("relationship", relationship);
                params.put("contact_number", contact);
                params.put("email", email);
                params.put("address", address);
                return params;
            }
        };
        queue.add(request);
    }

    private void loadFamilyMembers() {
        listFamilyMembers.removeAllViews();

        StringRequest request = new StringRequest(Request.Method.POST, ApiConfig.GET_FAMILY_MEMBERS_URL,
            response -> {
                try {
                    JSONArray array = new JSONArray(response);
                    if (array.length() == 0) {
                        TextView emptyText = new TextView(this);
                        emptyText.setText("No contacts added yet.");
                        emptyText.setTextColor(getResources().getColor(android.R.color.darker_gray));
                        emptyText.setTextSize(13);
                        emptyText.setPadding(0, 24, 0, 0);
                        listFamilyMembers.addView(emptyText);
                        return;
                    }

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        int id = obj.getInt("id");
                        String name = obj.getString("full_name");
                        String relationship = obj.getString("relationship");
                        String contact = obj.getString("contact_number");
                        String email = obj.optString("email", "");
                        String address = obj.optString("address", "");

                        View contactView = createContactView(id, name, relationship, contact, email, address);
                        listFamilyMembers.addView(contactView);
                    }
                } catch (JSONException e) {
                    Toast.makeText(ContactActivity.this, "Error parsing data.", Toast.LENGTH_SHORT).show();
                }
            },
            error -> Toast.makeText(ContactActivity.this, "Load failed: " + error.getMessage(), Toast.LENGTH_LONG).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", userId);
                return params;
            }
        };
        queue.add(request);
    }

    private View createContactView(int contactId, String name, String relationship, String contact, String email, String address) {
        LinearLayout contactCard = new LinearLayout(this);
        contactCard.setOrientation(LinearLayout.VERTICAL);
        contactCard.setBackgroundResource(R.drawable.modern_card_bg);

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        contactCard.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int marginBottom = (int) (12 * getResources().getDisplayMetrics().density);
        cardParams.setMargins(0, 0, 0, marginBottom);
        contactCard.setLayoutParams(cardParams);

        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        ImageView avatar = new ImageView(this);
        avatar.setImageResource(R.drawable.ic_avatar);
        avatar.setColorFilter(getResources().getColor(R.color.blue_600));
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(
            (int) (44 * getResources().getDisplayMetrics().density),
            (int) (44 * getResources().getDisplayMetrics().density)
        );
        avatar.setLayoutParams(avatarParams);
        headerLayout.addView(avatar);

        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        );
        int marginStart = (int) (12 * getResources().getDisplayMetrics().density);
        textParams.setMargins(marginStart, 0, 0, 0);
        textContainer.setLayoutParams(textParams);

        TextView nameText = new TextView(this);
        nameText.setText(name);
        nameText.setTextColor(getResources().getColor(R.color.gray_900));
        nameText.setTextSize(16);
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView relationshipText = new TextView(this);
        relationshipText.setText(relationship);
        relationshipText.setTextColor(getResources().getColor(R.color.gray_400));
        relationshipText.setTextSize(12);
        LinearLayout.LayoutParams relationshipParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int marginTop = (int) (4 * getResources().getDisplayMetrics().density);
        relationshipParams.setMargins(0, marginTop, 0, 0);
        relationshipText.setLayoutParams(relationshipParams);

        textContainer.addView(nameText);
        textContainer.addView(relationshipText);
        headerLayout.addView(textContainer);

        LinearLayout actionsLayout = new LinearLayout(this);
        actionsLayout.setOrientation(LinearLayout.HORIZONTAL);
        actionsLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        ImageView editButton = new ImageView(this);
        editButton.setImageResource(R.drawable.ic_edit);
        editButton.setColorFilter(getResources().getColor(R.color.blue_600));
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
            (int) (24 * getResources().getDisplayMetrics().density),
            (int) (24 * getResources().getDisplayMetrics().density)
        );
        int editMargin = (int) (8 * getResources().getDisplayMetrics().density);
        editParams.setMargins(editMargin, 0, editMargin, 0);
        editButton.setLayoutParams(editParams);
        editButton.setClickable(true);
        editButton.setFocusable(true);
        editButton.setOnClickListener(v -> editContact(contactId, name, relationship, contact, email, address));
        actionsLayout.addView(editButton);

        ImageView deleteButton = new ImageView(this);
        deleteButton.setImageResource(R.drawable.ic_delete);
        deleteButton.setColorFilter(getResources().getColor(R.color.red_600));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
            (int) (24 * getResources().getDisplayMetrics().density),
            (int) (24 * getResources().getDisplayMetrics().density)
        );
        deleteParams.setMargins(editMargin, 0, 0, 0);
        deleteButton.setLayoutParams(deleteParams);
        deleteButton.setClickable(true);
        deleteButton.setFocusable(true);
        deleteButton.setOnClickListener(v -> showDeleteConfirmation(contactId, name));
        actionsLayout.addView(deleteButton);

        headerLayout.addView(actionsLayout);
        contactCard.addView(headerLayout);

        View divider = new View(this);
        divider.setBackgroundColor(getResources().getColor(R.color.gray_200));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (int) (1 * getResources().getDisplayMetrics().density)
        );
        int dividerMargin = (int) (12 * getResources().getDisplayMetrics().density);
        dividerParams.setMargins(0, dividerMargin, 0, dividerMargin);
        divider.setLayoutParams(dividerParams);
        contactCard.addView(divider);

        LinearLayout detailsLayout = new LinearLayout(this);
        detailsLayout.setOrientation(LinearLayout.VERTICAL);

        TextView contactLabel = new TextView(this);
        contactLabel.setText("Phone");
        contactLabel.setTextColor(getResources().getColor(R.color.gray_600));
        contactLabel.setTextSize(11);
        contactLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        contactLabel.setLetterSpacing(0.02f);
        detailsLayout.addView(contactLabel);

        TextView contactValue = new TextView(this);
        contactValue.setText(contact);
        contactValue.setTextColor(getResources().getColor(R.color.gray_900));
        contactValue.setTextSize(13);
        LinearLayout.LayoutParams contactValueParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        contactValueParams.setMargins(0, (int)(2 * getResources().getDisplayMetrics().density), 0, (int)(12 * getResources().getDisplayMetrics().density));
        contactValue.setLayoutParams(contactValueParams);
        detailsLayout.addView(contactValue);

        if (!email.isEmpty()) {
            TextView emailLabel = new TextView(this);
            emailLabel.setText("Email");
            emailLabel.setTextColor(getResources().getColor(R.color.gray_600));
            emailLabel.setTextSize(11);
            emailLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            emailLabel.setLetterSpacing(0.02f);
            detailsLayout.addView(emailLabel);

            TextView emailValue = new TextView(this);
            emailValue.setText(email);
            emailValue.setTextColor(getResources().getColor(R.color.gray_900));
            emailValue.setTextSize(13);
            LinearLayout.LayoutParams emailValueParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            emailValueParams.setMargins(0, (int)(2 * getResources().getDisplayMetrics().density), 0, (int)(12 * getResources().getDisplayMetrics().density));
            emailValue.setLayoutParams(emailValueParams);
            detailsLayout.addView(emailValue);
        }

        if (!address.isEmpty()) {
            TextView addressLabel = new TextView(this);
            addressLabel.setText("Address");
            addressLabel.setTextColor(getResources().getColor(R.color.gray_600));
            addressLabel.setTextSize(11);
            addressLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            addressLabel.setLetterSpacing(0.02f);
            detailsLayout.addView(addressLabel);

            TextView addressValue = new TextView(this);
            addressValue.setText(address);
            addressValue.setTextColor(getResources().getColor(R.color.gray_900));
            addressValue.setTextSize(13);
            LinearLayout.LayoutParams addressValueParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            addressValueParams.setMargins(0, (int)(2 * getResources().getDisplayMetrics().density), 0, 0);
            addressValue.setLayoutParams(addressValueParams);
            detailsLayout.addView(addressValue);
        }

        contactCard.addView(detailsLayout);

        return contactCard;
    }

    private void editContact(int contactId, String name, String relationship, String contact, String email, String address) {
        editFullName.setText(name);
        editRelationship.setText(relationship);
        editContactNumber.setText(contact);
        editEmail.setText(email);
        editAddress.setText(address);
        editingContactId = contactId;
        btnSaveContact.setText("Update Contact");

        ScrollView scrollView = findViewById(R.id.formScrollView);
        scrollView.smoothScrollTo(0, 0);
    }

    private void showDeleteConfirmation(int contactId, String name) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete " + name + "?")
            .setPositiveButton("Delete", (dialog, which) -> deleteFamilyMember(contactId))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteFamilyMember(int memberId) {
        StringRequest request = new StringRequest(Request.Method.POST, ApiConfig.DELETE_FAMILY_MEMBER_URL,
            response -> {
                Toast.makeText(ContactActivity.this, "Contact deleted.", Toast.LENGTH_SHORT).show();
                loadFamilyMembers();
            },
            error -> Toast.makeText(ContactActivity.this, "Delete failed: " + error.getMessage(), Toast.LENGTH_LONG).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("id", String.valueOf(memberId));
                return params;
            }
        };
        queue.add(request);
    }

    private void clearFields() {
        editFullName.setText("");
        editRelationship.setText("");
        editContactNumber.setText("");
        editEmail.setText("");
        editAddress.setText("");
        editingContactId = -1;
        btnSaveContact.setText("Add Contact");
    }
}
