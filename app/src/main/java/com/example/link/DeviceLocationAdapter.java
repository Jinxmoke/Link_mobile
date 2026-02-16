package com.example.link;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DeviceLocationAdapter extends RecyclerView.Adapter<DeviceLocationAdapter.ViewHolder> {

    private List<DeviceLocation> deviceLocationList;

    public DeviceLocationAdapter(List<DeviceLocation> deviceLocationList) {
        this.deviceLocationList = deviceLocationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_devices_location, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceLocation deviceLocation = deviceLocationList.get(position);

        // Set serial number
        holder.tvSerialNumber.setText(deviceLocation.getSerialNumber());

        // Set customer name
        if (deviceLocation.getCustomerName() != null && !deviceLocation.getCustomerName().isEmpty()) {
            holder.tvCustomerName.setText(deviceLocation.getCustomerName());
        } else {
            holder.tvCustomerName.setText("Not Assigned");
        }

        // Set latitude and longitude
        if (deviceLocation.getLongitude() != null && !deviceLocation.getLongitude().isEmpty()) {
            holder.tvLocation.setText(deviceLocation.getLongitude());
        } else {
            holder.tvLocation.setText("N/A");
        }

        if (deviceLocation.getLatitude() != null && !deviceLocation.getLatitude().isEmpty()) {
            holder.tvResolvedBy.setText(deviceLocation.getLatitude());
        } else {
            holder.tvResolvedBy.setText("N/A");
        }

        // Format date and time
        if (deviceLocation.getDateTime() != null && !deviceLocation.getDateTime().isEmpty()) {
            String formattedDateTime = formatDateTime(deviceLocation.getDateTime());
            holder.tvDateTime.setText(formattedDateTime);
        } else {
            holder.tvDateTime.setText("Never");
        }

        // Optional: Show battery percentage if you want to add it to the layout
        // You would need to add a TextView for battery in your item layout
        /*
        if (deviceLocation.getBatteryPercent() > 0) {
            holder.tvBattery.setText(deviceLocation.getBatteryPercent() + "%");
        }
        */
    }

    @Override
    public int getItemCount() {
        return deviceLocationList.size();
    }

    // Helper method to format date and time
    private String formatDateTime(String dateTimeStr) {
        try {
            // Try parsing with different formats
            SimpleDateFormat[] possibleFormats = {
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            };

            Date date = null;
            for (SimpleDateFormat format : possibleFormats) {
                try {
                    date = format.parse(dateTimeStr);
                    if (date != null) break;
                } catch (ParseException e) {
                    // Try next format
                }
            }

            if (date != null) {
                // Format to desired output
                SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault());
                return outputFormat.format(date);
            }

            return dateTimeStr; // Return original if parsing fails
        } catch (Exception e) {
            e.printStackTrace();
            return dateTimeStr;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSerialNumber;
        TextView tvCustomerName;
        TextView tvLocation;
        TextView tvDateTime;
        TextView tvResolvedBy;
        // Add more TextViews if needed
        // TextView tvBattery;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvSerialNumber = itemView.findViewById(R.id.tvSerialNumber);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvResolvedBy = itemView.findViewById(R.id.tvResolvedBy);
            // Initialize other TextViews if added
            // tvBattery = itemView.findViewById(R.id.tvBattery);
        }
    }

    // Method to update data
    public void updateData(List<DeviceLocation> newList) {
        deviceLocationList.clear();
        deviceLocationList.addAll(newList);
        notifyDataSetChanged();
    }
}
