package com.example.link;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

public class UserActivity extends AppCompatActivity implements UserBottomNavigationFragment.OnNavigationItemSelectedListener {

    private UserBottomNavigationFragment bottomNavFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        String userType = getIntent().getStringExtra("userType");

        if (userType != null && userType.equals("user")) {
            if (savedInstanceState == null) {
                bottomNavFragment = new UserBottomNavigationFragment();
                bottomNavFragment.setOnNavigationItemSelectedListener(this);

                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.bottomNavContainer, bottomNavFragment)
                    .commit();

                loadFragment(new UserHomeFragment());
            }
        }
    }

    @Override
    public void onNavigationItemSelected(String item) {
        Fragment fragment = null;

        switch (item) {
            case "home":
                fragment = new UserHomeFragment();
                break;
            case "map":
                // Create MapFragment
                break;
            case "notification":
                // Create NotificationFragment
                break;
            case "profile":
                // Create ProfileFragment
                break;
            case "qr_scanner":
                // Create QRScannerFragment
                break;
        }

        if (fragment != null) {
            loadFragment(fragment);
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.mainContent, fragment)
            .addToBackStack(null)
            .commit();
    }

    public void loadUserMapFragment(UserMapFragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.mainContent, fragment)
            .addToBackStack(null)
            .commit();
    }
}
