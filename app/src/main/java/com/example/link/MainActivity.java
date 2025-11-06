package com.example.link;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class MainActivity extends AppCompatActivity {

    private NotificationBottomSheet notificationFragment;
    private BottomSheetBehavior<View> notificationSheetBehavior;
    private View notificationSheetView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ System UI setup (transparent nav with light icons)
        Window window = getWindow();
        window.setNavigationBarColor(Color.parseColor("#F5F7FA"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        } else {
            window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        int navColor = ContextCompat.getColor(this, R.color.bottom_nav_background);
        window.setNavigationBarColor(navColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int flags = window.getDecorView().getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            window.getDecorView().setSystemUiVisibility(flags);
        }

        // ✅ Ensure bottom nav adjusts with insets
        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

            View bottomNav = findViewById(R.id.bottomNavContainer);
            if (bottomNav != null) {
                bottomNav.setPadding(0, 0, 0, bottomInset);
            }

            return insets;
        });

        // ✅ Load the default Home fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mapContainer, new HomeFragment())
                .commit();
        }

        // ✅ Load bottom navigation (only for rescuer user type)
        String userType = getIntent().getStringExtra("userType");
        if ("rescuer".equals(userType)) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.bottomNavContainer, new BottomNavigationFragment())
                .commit();
        }

        // ✅ Setup Notification Bottom Sheet
        notificationSheetView = findViewById(R.id.notificationBottomSheet);

        if (notificationSheetView != null) {
            // Hide immediately before layout pass
            notificationSheetView.setVisibility(View.GONE);

            notificationSheetBehavior = BottomSheetBehavior.from(notificationSheetView);
            notificationSheetBehavior.setHideable(true);
            notificationSheetBehavior.setPeekHeight(0, false);
            notificationSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

            // Ensure it's never blocking touch when hidden
            notificationSheetView.setOnTouchListener((v, event) ->
                notificationSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN
            );

            // Bind fragment setup safely
            notificationFragment = (NotificationBottomSheet)
                getSupportFragmentManager().findFragmentById(R.id.notificationBottomSheet);
            if (notificationFragment != null) {
                notificationFragment.setupBottomSheet(notificationSheetView);
            }
        }
    }

    // ✅ Show Notification Sheet
    public void showNotificationSheet() {
        if (notificationSheetView == null || notificationSheetBehavior == null) return;

        // Make it visible first
        notificationSheetView.setVisibility(View.VISIBLE);

        // Collapse state for half-height appearance
        notificationSheetBehavior.setPeekHeight(
            (int) (450 * getResources().getDisplayMetrics().density)
        );
        notificationSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Bring to front in case nav overlaps
        notificationSheetView.bringToFront();
    }

    // ✅ Hide Notification Sheet
    public void hideNotificationSheet() {
        if (notificationSheetView == null || notificationSheetBehavior == null) return;

        notificationSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // Delay hiding the view to allow animation to complete
        notificationSheetView.postDelayed(() ->
            notificationSheetView.setVisibility(View.GONE), 250
        );
    }

    // ✅ Check if sheet is visible
    public boolean isNotificationSheetVisible() {
        return notificationSheetBehavior != null &&
            notificationSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN;
    }
}
