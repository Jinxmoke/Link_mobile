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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class BottomNavigationFragment extends Fragment {

    // ── Sections (touch targets) ──────────────────────────────────────────────
    private LinearLayout homeSection, mapSection, profileSection, logoutSection;

    // ── Icons ─────────────────────────────────────────────────────────────────
    // XML IDs:  homeIcon | mapIcon | profileIcon (in notificationSection) | LogoutIcon
    private ImageView homeIcon, mapIcon, profileIcon, logoutIcon;

    // ── Labels ────────────────────────────────────────────────────────────────
    private TextView homeLabel, mapLabel, profileLabel, logoutLabel;

    // ── Indicator dots ────────────────────────────────────────────────────────
    private View homeIndicator, mapIndicator, profileIndicator, logoutIndicator;

    // ── Center QR button ──────────────────────────────────────────────────────
    private FrameLayout centerButton;
    private ImageView   centerIcon;

    private String  currentFragment = "home";
    private int     selectedColor, unselectedColor;
    private boolean isQRScannerOpen = false;

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

        selectedColor   = Color.parseColor("#F97316");
        unselectedColor = Color.parseColor("#94A3B8");

        // ── Sections ──────────────────────────────────────────────────────────
        homeSection    = view.findViewById(R.id.homeSection);
        mapSection     = view.findViewById(R.id.mapSection);
        // XML "notificationSection" is visually the Profile tab
        profileSection = view.findViewById(R.id.notificationSection);
        // XML "profileSection" is visually the Logout tab
        logoutSection  = view.findViewById(R.id.profileSection);
        centerButton   = view.findViewById(R.id.centerButton);
        centerIcon     = view.findViewById(R.id.centerIcon);

        // ── Icons ─────────────────────────────────────────────────────────────
        homeIcon    = view.findViewById(R.id.homeIcon);
        mapIcon     = view.findViewById(R.id.mapIcon);
        // profileIcon is inside notificationSection in the XML
        profileIcon = view.findViewById(R.id.profileIcon);
        // LogoutIcon is inside profileSection in the XML
        logoutIcon  = view.findViewById(R.id.LogoutIcon);

        // ── Labels ────────────────────────────────────────────────────────────
        homeLabel    = view.findViewById(R.id.homeLabel);
        mapLabel     = view.findViewById(R.id.mapLabel);
        // notificationLabel = "Profile" in the XML
        profileLabel = view.findViewById(R.id.notificationLabel);
        // profileLabel      = "Logout"  in the XML
        logoutLabel  = view.findViewById(R.id.profileLabel);

        // ── Indicators ────────────────────────────────────────────────────────
        homeIndicator    = view.findViewById(R.id.homeIndicator);
        mapIndicator     = view.findViewById(R.id.mapIndicator);
        profileIndicator = view.findViewById(R.id.notificationIndicator);
        logoutIndicator  = view.findViewById(R.id.profileIndicator);

        // ── Initial state ─────────────────────────────────────────────────────
        Bundle args      = getArguments();
        boolean skipHome = args != null && args.getBoolean("skipHome", false);

        if (skipHome) {
            selectNavItem(mapIcon, mapLabel, mapIndicator);
            currentFragment = "map";
        } else {
            selectNavItem(homeIcon, homeLabel, homeIndicator);
            getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.mapContainer, new TestFragment())
                .commit();
        }

        setupNavClickListeners();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Click listeners
    // ─────────────────────────────────────────────────────────────────────────
    private void setupNavClickListeners() {

        // 🏠 Home → TestFragment
        homeSection.setOnClickListener(v -> {
            if (!currentFragment.equals("home")) {
                currentFragment = "home";
                selectNavItem(homeIcon, homeLabel, homeIndicator);
                deselectOthers(
                    mapIcon,     mapLabel,     mapIndicator,
                    profileIcon, profileLabel, profileIndicator,
                    logoutIcon,  logoutLabel,  logoutIndicator);
                closeQRScannerIfOpen();
                animateNavClick(homeSection);
                animateIndicator(homeIndicator);
                getParentFragmentManager().beginTransaction()
                    .replace(R.id.mapContainer, new TestFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            }
        });

        // 🗺 Map → MapFragment
        mapSection.setOnClickListener(v -> {
            if (!currentFragment.equals("map")) {
                navigateToMap();
            }
        });

        // 👤 Profile (XML notificationSection) → ProfileFragment
        profileSection.setOnClickListener(v -> {
            if (!currentFragment.equals("profile")) {
                currentFragment = "profile";
                selectNavItem(profileIcon, profileLabel, profileIndicator);
                deselectOthers(
                    homeIcon,   homeLabel,   homeIndicator,
                    mapIcon,    mapLabel,    mapIndicator,
                    logoutIcon, logoutLabel, logoutIndicator);
                closeQRScannerIfOpen();
                animateNavClick(profileSection);
                animateIndicator(profileIndicator);
                getParentFragmentManager().beginTransaction()
                    .replace(R.id.mapContainer, new ProfileFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            }
        });

        // 🔓 Logout (XML profileSection) → confirmation dialog then LoginActivity
        logoutSection.setOnClickListener(v -> showLogoutConfirmation());

        // 🧭 Center QR button
        centerButton.setOnClickListener(v -> toggleQRScanner());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Logout confirmation dialog
    // ─────────────────────────────────────────────────────────────────────────
    private void showLogoutConfirmation() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout", (dialog, which) -> performLogout())
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .create()
            .show();
    }

    private void performLogout() {
        if (getContext() == null) return;

        // Clear the login session
        SharedPrefManager.getInstance(getContext()).logout();

        // Navigate to LoginActivity and clear the back stack
        android.content.Intent intent = new android.content.Intent(
            getContext(), LoginActivity.class);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Called by MainActivity when a notification is tapped while app is running
    // ─────────────────────────────────────────────────────────────────────────
    public void selectMapTab() {
        if (mapIcon == null || mapLabel == null || mapIndicator == null) return;
        if (currentFragment.equals("map")) return;

        currentFragment = "map";
        selectNavItem(mapIcon, mapLabel, mapIndicator);
        deselectOthers(
            homeIcon,    homeLabel,    homeIndicator,
            profileIcon, profileLabel, profileIndicator,
            logoutIcon,  logoutLabel,  logoutIndicator);
        animateIndicator(mapIndicator);
        closeQRScannerIfOpen();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private map navigation (used by map tab click)
    // ─────────────────────────────────────────────────────────────────────────
    private void navigateToMap() {
        currentFragment = "map";
        selectNavItem(mapIcon, mapLabel, mapIndicator);
        deselectOthers(
            homeIcon,    homeLabel,    homeIndicator,
            profileIcon, profileLabel, profileIndicator,
            logoutIcon,  logoutLabel,  logoutIndicator);
        closeQRScannerIfOpen();
        animateNavClick(mapSection);
        animateIndicator(mapIndicator);
        getParentFragmentManager().beginTransaction()
            .replace(R.id.mapContainer, new MapFragment())
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  QR Scanner
    // ─────────────────────────────────────────────────────────────────────────
    private void toggleQRScanner() {
        if (isQRScannerOpen) closeQRScanner();
        else                 openQRScanner();
    }

    private void openQRScanner() {
        ObjectAnimator rotate = ObjectAnimator.ofFloat(centerIcon, "rotation", 0f, 180f);
        rotate.setDuration(300);
        rotate.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                centerIcon.setImageResource(R.drawable.ic_close);
            }
        });
        rotate.start();

        getParentFragmentManager().beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .add(R.id.mapContainer, new QRScannerFragment())
            .addToBackStack(null)
            .commit();

        isQRScannerOpen = true;
    }

    private void closeQRScanner() {
        ObjectAnimator rotate = ObjectAnimator.ofFloat(centerIcon, "rotation", 180f, 0f);
        rotate.setDuration(300);
        rotate.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                centerIcon.setImageResource(R.drawable.ic_qr_code);
            }
        });
        rotate.start();

        Fragment fragment = getParentFragmentManager().findFragmentById(R.id.mapContainer);
        if (fragment instanceof QRScannerFragment) {
            getParentFragmentManager().beginTransaction()
                .remove(fragment)
                .commit();
        }
        isQRScannerOpen = false;
    }

    private void closeQRScannerIfOpen() {
        if (isQRScannerOpen) closeQRScanner();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UI helpers
    // ─────────────────────────────────────────────────────────────────────────
    private void selectNavItem(ImageView icon, TextView label, View indicator) {
        icon.setColorFilter(selectedColor);
        label.setTextColor(selectedColor);
        indicator.setVisibility(View.VISIBLE);
    }

    private void deselectOthers(Object... others) {
        for (int i = 0; i < others.length; i += 3) {
            ((ImageView) others[i    ]).setColorFilter(unselectedColor);
            ((TextView)  others[i + 1]).setTextColor(unselectedColor);
            ((View)      others[i + 2]).setVisibility(View.INVISIBLE);
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
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
            ObjectAnimator.ofFloat(indicator, View.SCALE_X, 0f, 1f),
            ObjectAnimator.ofFloat(indicator, View.ALPHA,   0f, 1f)
        );
        set.setDuration(250);
        set.start();
    }
}
