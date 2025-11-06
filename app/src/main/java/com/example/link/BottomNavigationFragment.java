package com.example.link;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class BottomNavigationFragment extends Fragment {

    private LinearLayout homeSection, mapSection, notificationSection, profileSection;
    private ImageView homeIcon, mapIcon, notificationIcon, profileIcon;
    private TextView homeLabel, mapLabel, notificationLabel, profileLabel;
    private View homeIndicator, mapIndicator, notificationIndicator, profileIndicator;
    private FrameLayout centerButton;

    private String currentFragment = "home";
    private int selectedColor, unselectedColor;

    // QR button state
    private boolean isQRScannerOpen = false;
    private ImageView centerIcon;

    public BottomNavigationFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bottom_navigation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        selectedColor = Color.parseColor("#F97316"); // Orange
        unselectedColor = Color.parseColor("#94A3B8"); // Gray

        // Navigation sections
        homeSection = view.findViewById(R.id.homeSection);
        mapSection = view.findViewById(R.id.mapSection);
        notificationSection = view.findViewById(R.id.notificationSection);
        profileSection = view.findViewById(R.id.profileSection);
        centerButton = view.findViewById(R.id.centerButton);

        // QR Icon inside centerButton
        centerIcon = view.findViewById(R.id.centerIcon); // ImageView inside the FrameLayout

        // Icons
        homeIcon = view.findViewById(R.id.homeIcon);
        mapIcon = view.findViewById(R.id.mapIcon);
        notificationIcon = view.findViewById(R.id.notificationIcon);
        profileIcon = view.findViewById(R.id.profileIcon);

        // Labels
        homeLabel = view.findViewById(R.id.homeLabel);
        mapLabel = view.findViewById(R.id.mapLabel);
        notificationLabel = view.findViewById(R.id.notificationLabel);
        profileLabel = view.findViewById(R.id.profileLabel);

        // Indicators
        homeIndicator = view.findViewById(R.id.homeIndicator);
        mapIndicator = view.findViewById(R.id.mapIndicator);
        notificationIndicator = view.findViewById(R.id.notificationIndicator);
        profileIndicator = view.findViewById(R.id.profileIndicator);

        // Default selected
        selectNavItem(homeIcon, homeLabel, homeIndicator);
        getParentFragmentManager().beginTransaction()
            .replace(R.id.mapContainer, new HomeFragment())
            .commit();

        setupNavClickListeners();
    }

    private void setupNavClickListeners() {
        // 🏠 Home
        homeSection.setOnClickListener(v -> {
            if (!currentFragment.equals("home")) {
                currentFragment = "home";
                selectNavItem(homeIcon, homeLabel, homeIndicator);
                deselectOthers(mapIcon, mapLabel, mapIndicator,
                    notificationIcon, notificationLabel, notificationIndicator,
                    profileIcon, profileLabel, profileIndicator);
                hideNotificationSheetIfVisible();
                closeQRScannerIfOpen();
                animateNavClick(homeSection);
                animateIndicator(homeIndicator);

                getParentFragmentManager().beginTransaction()
                    .replace(R.id.mapContainer, new HomeFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            }
        });

        // 🗺 Map
        mapSection.setOnClickListener(v -> {
            if (!currentFragment.equals("map")) {
                currentFragment = "map";
                selectNavItem(mapIcon, mapLabel, mapIndicator);
                deselectOthers(homeIcon, homeLabel, homeIndicator,
                    notificationIcon, notificationLabel, notificationIndicator,
                    profileIcon, profileLabel, profileIndicator);
                hideNotificationSheetIfVisible();
                closeQRScannerIfOpen();
                animateNavClick(mapSection);
                animateIndicator(mapIndicator);

                getParentFragmentManager().beginTransaction()
                    .replace(R.id.mapContainer, new MapFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            }
        });

        // 🔔 Notification
        notificationSection.setOnClickListener(v -> {
            if (!currentFragment.equals("notification")) {
                currentFragment = "notification";
                selectNavItem(notificationIcon, notificationLabel, notificationIndicator);
                deselectOthers(homeIcon, homeLabel, homeIndicator,
                    mapIcon, mapLabel, mapIndicator,
                    profileIcon, profileLabel, profileIndicator);
                closeQRScannerIfOpen();
                animateNavClick(notificationSection);
                animateIndicator(notificationIndicator);
            }
            toggleNotificationSheet();
        });

        // 👤 Profile
        profileSection.setOnClickListener(v -> {
            if (!currentFragment.equals("profile")) {
                currentFragment = "profile";
                selectNavItem(profileIcon, profileLabel, profileIndicator);
                deselectOthers(homeIcon, homeLabel, homeIndicator,
                    mapIcon, mapLabel, mapIndicator,
                    notificationIcon, notificationLabel, notificationIndicator);
                closeQRScannerIfOpen();
                animateNavClick(profileSection);
                animateIndicator(profileIndicator);

                getParentFragmentManager().beginTransaction()
                    .replace(R.id.mapContainer, new ProfileFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            }
        });

        // 🧭 Center QR Button toggle
        centerButton.setOnClickListener(v -> toggleQRScanner());
    }

    // 🔹 Toggle QR Scanner with rotation and icon change
    private void toggleQRScanner() {
        if (isQRScannerOpen) {
            // Close QR scanner
            closeQRScanner();
        } else {
            // Open QR scanner
            openQRScanner();
        }
    }

    private void openQRScanner() {
        hideNotificationSheetIfVisible();

        // Animate rotation
        ObjectAnimator rotate = ObjectAnimator.ofFloat(centerIcon, "rotation", 0f, 180f);
        rotate.setDuration(300);
        rotate.start();

        // Change icon to X after rotation
        rotate.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                centerIcon.setImageResource(R.drawable.ic_close); // X icon
            }
        });

        getParentFragmentManager().beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
            .add(R.id.mapContainer, new QRScannerFragment())
            .addToBackStack(null)
            .commit();

        isQRScannerOpen = true;
    }

    private void closeQRScanner() {
        // Animate rotation back
        ObjectAnimator rotate = ObjectAnimator.ofFloat(centerIcon, "rotation", 180f, 0f);
        rotate.setDuration(300);
        rotate.start();

        rotate.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                centerIcon.setImageResource(R.drawable.ic_qr_code); // QR icon
            }
        });

        // Remove QR fragment
        Fragment qrFragment = getParentFragmentManager().findFragmentById(R.id.mapContainer);
        if (qrFragment instanceof QRScannerFragment) {
            getParentFragmentManager().beginTransaction()
                .remove(qrFragment)
                .commit();
        }

        isQRScannerOpen = false;
    }

    private void closeQRScannerIfOpen() {
        if (isQRScannerOpen) {
            closeQRScanner();
        }
    }

    // ✅ Selection helpers
    private void selectNavItem(ImageView icon, TextView label, View indicator) {
        icon.setColorFilter(selectedColor);
        label.setTextColor(selectedColor);
        indicator.setVisibility(View.VISIBLE);
    }

    private void deselectOthers(Object... others) {
        for (int i = 0; i < others.length; i += 3) {
            ImageView icon = (ImageView) others[i];
            TextView label = (TextView) others[i + 1];
            View indicator = (View) others[i + 2];
            icon.setColorFilter(unselectedColor);
            label.setTextColor(unselectedColor);
            indicator.setVisibility(View.INVISIBLE);
        }
    }

    private void animateNavClick(View view) {
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.1f, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.1f, 1f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY);
        animator.setInterpolator(new OvershootInterpolator());
        animator.setDuration(300);
        animator.start();
    }

    private void animateIndicator(View indicator) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(indicator, View.SCALE_X, 0f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(indicator, View.ALPHA, 0f, 1f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, alpha);
        set.setDuration(250);
        set.start();
    }

    // ✅ Bottom Sheet Logic
    private void toggleNotificationSheet() {
        View sheet = requireActivity().findViewById(R.id.notificationBottomSheet);
        if (sheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
            if (behavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                sheet.setVisibility(View.VISIBLE);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                sheet.postDelayed(() -> sheet.setVisibility(View.GONE), 300);
            }
        }
    }

    private void hideNotificationSheetIfVisible() {
        View sheet = requireActivity().findViewById(R.id.notificationBottomSheet);
        if (sheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
            if (behavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
                behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                sheet.postDelayed(() -> sheet.setVisibility(View.GONE), 300);
            }
        }
    }
}
