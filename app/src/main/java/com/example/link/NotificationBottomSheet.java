package com.example.link;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;
import java.util.List;

public class NotificationBottomSheet extends Fragment {

    private RecyclerView notificationsRecyclerView;
    private NotificationAdapter notificationAdapter;
    private List<NotificationFragment.NotificationItem> notificationsList;
    private View bottomSheetView;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notificationsRecyclerView = view.findViewById(R.id.notificationsRecyclerView);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        notificationsList = new ArrayList<>();
        notificationsList.add(new NotificationFragment.NotificationItem("Payment Received", "You received $50.00 from John Doe", "2 mins ago"));
        notificationsList.add(new NotificationFragment.NotificationItem("Order Confirmed", "Your order #12345 has been confirmed", "1 hour ago"));
        notificationsList.add(new NotificationFragment.NotificationItem("Delivery Update", "Your package is out for delivery", "3 hours ago"));
        notificationsList.add(new NotificationFragment.NotificationItem("New Message", "You have a new message from support", "5 hours ago"));
        notificationsList.add(new NotificationFragment.NotificationItem("Account Alert", "New login detected from Chrome", "1 day ago"));

        notificationAdapter = new NotificationAdapter(notificationsList);
        notificationsRecyclerView.setAdapter(notificationAdapter);

        View dragHandle = view.findViewById(R.id.dragHandle);
        dragHandle.setOnClickListener(v -> toggleBottomSheet());
    }

    public void setupBottomSheet(View bottomSheetView) {
        this.bottomSheetView = bottomSheetView;
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
        bottomSheetBehavior.setPeekHeight(0);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setDraggable(true);
    }

    public void showBottomSheet() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            bottomSheetBehavior.setPeekHeight((int) (450 * getResources().getDisplayMetrics().density));
        }
    }

    public void hideBottomSheet() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    private void toggleBottomSheet() {
        if (bottomSheetBehavior != null) {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }
}
