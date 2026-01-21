package com.example.link; // Change to your actual package name

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class HistoryActivity extends AppCompatActivity {

    private TextView tvAvailableTitle;
    private TextView tvCountBadge;
    private CardView historyCard;
    private View btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history); // Assuming your layout file is named activity_history.xml

        // Initialize views
        initializeViews();

        // Set up click listeners
        setupClickListeners();

        // You can load data here if needed
        loadHistoryData();
    }

    private void initializeViews() {
        tvAvailableTitle = findViewById(R.id.tvAvailableTitle);
        tvCountBadge = findViewById(R.id.tvCountBadge); // You'll need to add this ID to your count TextView
        historyCard = findViewById(R.id.historyCard); // You'll need to add this ID to your CardView
        btnBack = findViewById(R.id.btnBack);

        // Initialize other views if you have them
        // For example, if you want to dynamically update the list items,
        // you would need to add IDs to them in the layout
    }

    private void setupClickListeners() {
        // Back button click listener
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // You can add click listeners to individual history items
        // First, you need to add IDs to each item container in your layout
        // For example: android:id="@+id/item1", "@+id/item2", "@+id/item3"

        // Example:
        // View item1 = findViewById(R.id.item1);
        // item1.setOnClickListener(new View.OnClickListener() {
        //     @Override
        //     public void onClick(View v) {
        //         // Handle item click
        //         showToast("Clicked on Michael James Ochea's emergency");
        //     }
        // });
    }

    private void loadHistoryData() {
        // This is where you would load real data from your data source
        // For now, we're using static data from the layout

        // You could update the count badge based on real data
        // tvCountBadge.setText(String.valueOf(getEmergencyCount()));

        // Or update the title based on some condition
        // tvAvailableTitle.setText(getCustomTitle());
    }

    // Example methods for data manipulation
    private int getEmergencyCount() {
        // Return actual count from your data source
        return 4; // This is the hardcoded value from your layout
    }

    private String getCustomTitle() {
        // Return custom title based on your logic
        return "Assigned Customer (SOS)";
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // You can add custom back press animation or logic here
        // overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    // If you want to update the UI dynamically, you can create methods like:
    public void updateEmergencyCount(int count) {
        if (tvCountBadge != null) {
            tvCountBadge.setText(String.valueOf(count));
        }
    }

    public void setTitleText(String title) {
        if (tvAvailableTitle != null) {
            tvAvailableTitle.setText(title);
        }
    }

    // You might want to handle item clicks in a RecyclerView instead
    // Consider converting to RecyclerView if you have dynamic data
    /*
    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        HistoryAdapter adapter = new HistoryAdapter(getHistoryItems());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    */
}
