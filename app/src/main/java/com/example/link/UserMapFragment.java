package com.example.link;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class UserMapFragment extends Fragment {

    private WebView leafletWebView;
    private ImageView backButton;
    private ImageView refreshButton;

    private static final String DEFAULT_LATITUDE = "14.5547";
    private static final String DEFAULT_LONGITUDE = "121.0244";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.user_fragment_map, container, false);

        initializeViews(view);
        setupClickListeners();
        loadMap();

        return view;
    }

    private void initializeViews(View view) {
        leafletWebView = view.findViewById(R.id.mapWebView);
        backButton = view.findViewById(R.id.backButton);
        refreshButton = view.findViewById(R.id.refreshButton);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());

        refreshButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Refreshing location...", Toast.LENGTH_SHORT).show();
            loadMap();
        });
    }

    private void loadMap() {
        leafletWebView.setWebViewClient(new WebViewClient());
        leafletWebView.getSettings().setJavaScriptEnabled(true);

        // Get coordinates (from arguments or default)
        String latitude = DEFAULT_LATITUDE;
        String longitude = DEFAULT_LONGITUDE;

        Bundle args = getArguments();
        if (args != null) {
            latitude = args.getString("latitude", DEFAULT_LATITUDE);
            longitude = args.getString("longitude", DEFAULT_LONGITUDE);
        }

        // Load local Leaflet HTML file
        leafletWebView.loadUrl("file:///android_asset/map.html");

        // Inject coordinates into the web page once it loads
        String finalLatitude = latitude;
        String finalLongitude = longitude;

        leafletWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                // Call a JavaScript function inside map.html
                view.loadUrl("javascript:updateMarker(" + finalLatitude + ", " + finalLongitude + ")");
            }
        });
    }
}
