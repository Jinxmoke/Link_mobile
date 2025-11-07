package com.example.link;

import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

public class UserActivity extends AppCompatActivity implements UserBottomNavigationFragment.OnNavigationItemSelectedListener {

    private UserBottomNavigationFragment bottomNavFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        // ✅ Retrieve user info from LoginActivity
        String userType = getIntent().getStringExtra("user_type");
        String userId = getIntent().getStringExtra("user_id");
        String username = getIntent().getStringExtra("username");
        String email = getIntent().getStringExtra("email");

        // ✅ Store in SharedPreferences for global access
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_id", userId);
        editor.putString("username", username);
        editor.putString("email", email);
        editor.putString("user_type", userType);
        editor.apply();

        if (userType != null && userType.equals("user")) {
            if (savedInstanceState == null) {
                // ✅ Initialize bottom navigation fragment
                bottomNavFragment = new UserBottomNavigationFragment();
                bottomNavFragment.setOnNavigationItemSelectedListener(this);

                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.bottomNavContainer, bottomNavFragment)
                    .commit();

                // ✅ Load UserHomeFragment with data from login
                UserHomeFragment homeFragment = new UserHomeFragment();
                Bundle args = new Bundle();
                args.putString("userName", username);
                args.putString("userId", userId);
                args.putString("email", email);
                homeFragment.setArguments(args);

                loadFragment(homeFragment);
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
                fragment = new UserMapFragment();
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

    // ✅ Called from UserHomeFragment when "View Map" is pressed
    public void loadUserMapFragment(UserMapFragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.mainContent, fragment)
            .addToBackStack(null)
            .commit();
    }
}
