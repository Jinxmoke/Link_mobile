package com.example.link;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SOSAlertAdapter extends RecyclerView.Adapter<SOSAlertAdapter.ViewHolder> {

    private List<SOSAlert> alertList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(SOSAlert alert);
    }

    public SOSAlertAdapter() {
        this.alertList = new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setAlerts(List<SOSAlert> alerts) {
        this.alertList = alerts;
        notifyDataSetChanged();
    }

    public void clearAlerts() {
        this.alertList.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_sos_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SOSAlert alert = alertList.get(position);
        holder.bind(alert, listener);
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName;
        TextView tvLocation;
        TextView tvDateTime;
        TextView tvResolvedBy;
        TextView tvSerialNumber;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvResolvedBy = itemView.findViewById(R.id.tvResolvedBy);
            tvSerialNumber = itemView.findViewById(R.id.tvSerialNumber);
        }

        public void bind(SOSAlert alert, OnItemClickListener listener) {
            // Set customer name
            tvCustomerName.setText(alert.getCustomerName());

            // Set location
            tvLocation.setText(alert.getFormattedLocation());

            // Set date and time (using resolved time for resolved alerts)
            tvDateTime.setText(alert.getFormattedDate() + " " + alert.getFormattedTime());

            // Set resolved by name
            String resolvedBy = alert.getResolvedByName();
            if (resolvedBy == null || resolvedBy.isEmpty() || resolvedBy.equals("null")) {
                resolvedBy = "Unknown";
            }
            tvResolvedBy.setText(resolvedBy);

            // Set serial number
            String serialNumber = alert.getTransmitterSerial();
            if (serialNumber == null || serialNumber.isEmpty()) {
                serialNumber = "N/A";
            }
            tvSerialNumber.setText(serialNumber);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(alert);
                }
            });
        }
    }
}
