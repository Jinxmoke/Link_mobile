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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_home, container, false);

        initializeViews(view);
        setupClickListeners();
        loadUserData();

        return view;
    }

    private void initializeViews(View view) {
        profileImage = view.findViewById(R.id.profileImage);
        userName = view.findViewById(R.id.userName);
        deviceId = view.findViewById(R.id.deviceId);
        deviceStatus = view.findViewById(R.id.deviceStatus);
        lastSignal = view.findViewById(R.id.lastSignal);
        gpsCoordinates = view.findViewById(R.id.gpsCoordinates);
        locationCoordinates = view.findViewById(R.id.locationCoordinates);
        locationUpdateTime = view.findViewById(R.id.locationUpdateTime);
        viewMapButton = view.findViewById(R.id.viewMapButton);
        sendSosButton = view.findViewById(R.id.sendSosButton);
        powerOnButton = view.findViewById(R.id.powerOnButton);
        addContactButton = view.findViewById(R.id.addContactButton);
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

}
