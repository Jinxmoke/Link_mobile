package com.example.link;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class MapFragment extends Fragment {

    private WebView leafletWebView;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        leafletWebView = view.findViewById(R.id.leafletWebView);

        // Enable JavaScript for Leaflet map
        WebSettings webSettings = leafletWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        leafletWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Once map is loaded, get user location
                checkLocationPermissionAndGetLocation();
            }
        });

        // Load your map HTML file from assets folder
        leafletWebView.loadUrl("file:///android_asset/map.html");
    }

    private void checkLocationPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, get location
            getUserLocation();
        } else {
            // Request permission
            requestPermissions(
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get location
                getUserLocation();
            } else {
                // Permission denied
                Toast.makeText(requireContext(),
                    "Location permission denied. Showing default location.",
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        // Got location, send to map
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        Toast.makeText(requireContext(),
                            "Latitude: " + latitude + ", Longitude: " + longitude,
                            Toast.LENGTH_SHORT).show();

                        updateMapLocation(latitude, longitude);
                    } else {
                        Toast.makeText(requireContext(),
                            "Unable to get location. Showing default location.",
                            Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    private void updateMapLocation(double latitude, double longitude) {
        String javascript = "javascript:updateUserLocation(" + latitude + ", " + longitude + ")";
        leafletWebView.evaluateJavascript(javascript, null);
    }
}
