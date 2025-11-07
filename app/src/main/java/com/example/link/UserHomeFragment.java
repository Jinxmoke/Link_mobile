package com.example.link;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class UserHomeFragment extends Fragment {

    private TextView userName;
    private TextView deviceId;
    private TextView deviceStatus;
    private TextView lastSignal;
    private TextView gpsCoordinates;
    private TextView locationCoordinates;
    private TextView locationUpdateTime;
    private AppCompatButton viewMapButton;
    private LinearLayout sendSosButton;
    private LinearLayout powerOnButton;
    private AppCompatButton addContactButton;
    private ImageView profileImage;
    private LinearLayout emergencyContactsCard;
    private LinearLayout emergencyContactsContainer;

    private RequestQueue requestQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_home, container, false);

        initializeViews(view);
        setupClickListeners();
        loadUserData();

        requestQueue = Volley.newRequestQueue(requireContext());
//        loadEmergencyContacts();

        return view;
    }

    private void initializeViews(View view) {
        profileImage = view.findViewById(R.id.profileImage);
        userName = view.findViewById(R.id.userName);
        deviceId = view.findViewById(R.id.deviceId);
        deviceStatus = view.findViewById(R.id.deviceStatus);
        gpsCoordinates = view.findViewById(R.id.gpsCoordinates);
        locationUpdateTime = view.findViewById(R.id.locationUpdateTime);
        viewMapButton = view.findViewById(R.id.viewMapButton);
        sendSosButton = view.findViewById(R.id.sendSosButton);
        powerOnButton = view.findViewById(R.id.powerOnButton);
        addContactButton = view.findViewById(R.id.addContactButton);
        emergencyContactsCard = view.findViewById(R.id.emergencyContactsCard);
        emergencyContactsContainer = view.findViewById(R.id.emergencyContactsContainer);
    }

    private void setupClickListeners() {
        viewMapButton.setOnClickListener(v -> openMapView());

        sendSosButton.setOnClickListener(v -> sendSOS());

        powerOnButton.setOnClickListener(v -> togglePower());

        addContactButton.setOnClickListener(v -> addEmergencyContact());
    }

    private void loadUserData() {
        if (getActivity() != null) {
            Bundle args = getArguments();
            if (args != null) {
                String name = args.getString("userName", "John Doe");
                String device = args.getString("deviceId", "LINK2025");
                userName.setText(name);
                deviceId.setText("Device ID: " + device);
            }
        }
    }

    private void loadEmergencyContacts() {
        emergencyContactsContainer.removeAllViews();

        StringRequest request = new StringRequest(Request.Method.POST, ApiConfig.GET_FAMILY_MEMBERS_URL,
            response -> {
                try {
                    JSONArray array = new JSONArray(response);

                    if (array.length() == 0) {
                        emergencyContactsCard.setVisibility(View.VISIBLE);
                        TextView emptyText = new TextView(requireContext());
                        emptyText.setText("No emergency contacts found.");
                        emptyText.setTextColor(getResources().getColor(android.R.color.darker_gray));
                        emptyText.setTextSize(13);
                        emergencyContactsContainer.addView(emptyText);
                        return;
                    }

                    emergencyContactsCard.setVisibility(View.VISIBLE);

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);

                        String name = obj.getString("full_name");
                        String relationship = obj.getString("relationship");

                        View contactView = createContactView(name, relationship);
                        emergencyContactsContainer.addView(contactView);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    emergencyContactsContainer.removeAllViews();
                    TextView errorText = new TextView(requireContext());
                    errorText.setText("Error parsing data.");
                    errorText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    emergencyContactsContainer.addView(errorText);
                }
            },
            error -> {
                emergencyContactsContainer.removeAllViews();
                TextView errorText = new TextView(requireContext());
                errorText.setText("Load failed: " + error.getMessage());
                errorText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                emergencyContactsContainer.addView(errorText);
            }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", 0);
                String userId = sharedPreferences.getString("user_id", "");
                params.put("user_id", userId);
                return params;
            }
        };

        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(requireContext());
        }
        requestQueue.add(request);
    }

    private View createContactView(String name, String relationship) {
        LinearLayout contactLayout = new LinearLayout(requireContext());
        contactLayout.setOrientation(LinearLayout.HORIZONTAL);
        contactLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        contactLayout.setBackgroundResource(R.drawable.member_item_bg);

        int padding = (int) (12 * getResources().getDisplayMetrics().density);
        contactLayout.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int marginBottom = (int) (16 * getResources().getDisplayMetrics().density); // updated margin
        layoutParams.setMargins(0, 0, 0, marginBottom);
        contactLayout.setLayoutParams(layoutParams);

        // Avatar
        ImageView avatar = new ImageView(requireContext());
        avatar.setImageResource(R.drawable.ic_avatar);
        avatar.setColorFilter(getResources().getColor(R.color.blue_600)); // #2563EB
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(
            (int) (32 * getResources().getDisplayMetrics().density),
            (int) (32 * getResources().getDisplayMetrics().density)
        );
        avatar.setLayoutParams(avatarParams);
        contactLayout.addView(avatar);

        // Text container
        LinearLayout textContainer = new LinearLayout(requireContext());
        textContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        );
        int marginStart = (int) (12 * getResources().getDisplayMetrics().density);
        textParams.setMargins(marginStart, 0, 0, 0);
        textContainer.setLayoutParams(textParams);

        TextView nameText = new TextView(requireContext());
        nameText.setText(name);
        nameText.setTextColor(getResources().getColor(R.color.gray_900)); // #1F2937
        nameText.setTextSize(14);
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView relationshipText = new TextView(requireContext());
        relationshipText.setText(relationship);
        relationshipText.setTextColor(getResources().getColor(R.color.gray_400)); // #6B7280
        relationshipText.setTextSize(12);
        LinearLayout.LayoutParams relationshipParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int marginTop = (int) (2 * getResources().getDisplayMetrics().density);
        relationshipParams.setMargins(0, marginTop, 0, 0);
        relationshipText.setLayoutParams(relationshipParams);

        textContainer.addView(nameText);
        textContainer.addView(relationshipText);

        contactLayout.addView(textContainer);

        return contactLayout;
    }


    private void sendSOS() {
        Toast.makeText(getContext(), "Sending SOS alert...", Toast.LENGTH_SHORT).show();
    }

    private void togglePower() {
        Toast.makeText(getContext(), "Toggling device power...", Toast.LENGTH_SHORT).show();
    }

    private void addEmergencyContact() {
        Intent intent = new Intent(getActivity(), ContactActivity.class);
        startActivity(intent);
    }

    public void updateDeviceStatus(String status, String signal, String coordinates) {
        if (deviceStatus != null) {
            deviceStatus.setText(status);
        }
        if (lastSignal != null) {
            lastSignal.setText("Last signal: " + signal);
        }
        if (gpsCoordinates != null) {
            gpsCoordinates.setText("GPS: " + coordinates);
        }
        if (locationCoordinates != null) {
            locationCoordinates.setText(coordinates);
        }
    }

    private void openMapView() {
        UserMapFragment mapFragment = new UserMapFragment();
        Bundle args = new Bundle();
        args.putString("latitude", "14.5547");
        args.putString("longitude", "121.0244");
        mapFragment.setArguments(args);

        if (getActivity() instanceof UserActivity) {
            UserActivity activity = (UserActivity) getActivity();
            activity.loadUserMapFragment(mapFragment);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadEmergencyContacts();
    }
}
