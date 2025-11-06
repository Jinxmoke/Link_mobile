package com.example.link;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class UserBottomNavigationFragment extends Fragment {

    private LinearLayout homeSection;
    private LinearLayout mapSection;
    private LinearLayout notificationSection;
    private LinearLayout profileSection;
    private FrameLayout centerButton;

    private View homeIndicator;
    private View mapIndicator;
    private View notificationIndicator;
    private View profileIndicator;

    private OnNavigationItemSelectedListener listener;

    public interface OnNavigationItemSelectedListener {
        void onNavigationItemSelected(String item);
    }

    public void setOnNavigationItemSelectedListener(OnNavigationItemSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_bottom_navigation, container, false);

        initializeViews(view);
        setupClickListeners();
        setActiveTab("home");

        return view;
    }

    private void initializeViews(View view) {
        homeSection = view.findViewById(R.id.homeSection);
        mapSection = view.findViewById(R.id.mapSection);
        notificationSection = view.findViewById(R.id.notificationSection);
        profileSection = view.findViewById(R.id.profileSection);
        centerButton = view.findViewById(R.id.centerButton);

        homeIndicator = view.findViewById(R.id.homeIndicator);
        mapIndicator = view.findViewById(R.id.mapIndicator);
        notificationIndicator = view.findViewById(R.id.notificationIndicator);
        profileIndicator = view.findViewById(R.id.profileIndicator);
    }

    private void setupClickListeners() {
        homeSection.setOnClickListener(v -> {
            setActiveTab("home");
            if (listener != null) listener.onNavigationItemSelected("home");
        });

        mapSection.setOnClickListener(v -> {
            setActiveTab("map");
            if (listener != null) listener.onNavigationItemSelected("map");
        });

        notificationSection.setOnClickListener(v -> {
            setActiveTab("notification");
            if (listener != null) listener.onNavigationItemSelected("notification");
        });

        profileSection.setOnClickListener(v -> {
            setActiveTab("profile");
            if (listener != null) listener.onNavigationItemSelected("profile");
        });

        centerButton.setOnClickListener(v -> {
            if (listener != null) listener.onNavigationItemSelected("qr_scanner");
        });
    }

    public void setActiveTab(String tab) {
        homeIndicator.setVisibility(View.INVISIBLE);
        mapIndicator.setVisibility(View.INVISIBLE);
        notificationIndicator.setVisibility(View.INVISIBLE);
        profileIndicator.setVisibility(View.INVISIBLE);

        switch (tab) {
            case "home":
                homeIndicator.setVisibility(View.VISIBLE);
                break;
            case "map":
                mapIndicator.setVisibility(View.VISIBLE);
                break;
            case "notification":
                notificationIndicator.setVisibility(View.VISIBLE);
                break;
            case "profile":
                profileIndicator.setVisibility(View.VISIBLE);
                break;
        }
    }
}
