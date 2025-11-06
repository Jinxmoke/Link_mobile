package com.example.link;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<NotificationFragment.NotificationItem> notificationsList;

    public NotificationAdapter(List<NotificationFragment.NotificationItem> notificationsList) {
        this.notificationsList = notificationsList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.notification_item, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationFragment.NotificationItem notification = notificationsList.get(position);
        holder.titleTextView.setText(notification.title);
        holder.messageTextView.setText(notification.message);
        if (notification.timestamp != null && !notification.timestamp.isEmpty()) {
            holder.timestampTextView.setText(notification.timestamp);
        } else {
            holder.timestampTextView.setText("Just now");
        }
    }

    @Override
    public int getItemCount() {
        return notificationsList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView messageTextView;
        TextView timestampTextView;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.notificationTitle);
            messageTextView = itemView.findViewById(R.id.notificationMessage);
            timestampTextView = itemView.findViewById(R.id.notificationTimestamp);
        }
    }
}
