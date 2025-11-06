package com.example.link;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends BottomSheetDialogFragment {

    private RecyclerView notificationsRecyclerView;
    private NotificationAdapter notificationAdapter;
    private List<NotificationItem> notificationsList;
    private View dragHandle;
    private boolean isExpanded = false;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setFitToContents(false);
                behavior.setPeekHeight(500);
                behavior.setHalfExpandedRatio(0.7f);
                behavior.setExpandedOffset(100);
                behavior.setDraggable(true);
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                behavior.setSkipCollapsed(false);
            }
        });

        return dialog;
    }

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

        dragHandle = view.findViewById(R.id.dragHandle);
        notificationsRecyclerView = view.findViewById(R.id.notificationsRecyclerView);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        notificationsList = new ArrayList<>();
        notificationsList.add(new NotificationItem("Payment Received", "You received $50.00 from John Doe", "2 mins ago"));
        notificationsList.add(new NotificationItem("Order Confirmed", "Your order #12345 has been confirmed", "1 hour ago"));
        notificationsList.add(new NotificationItem("Delivery Update", "Your package is out for delivery", "3 hours ago"));
        notificationsList.add(new NotificationItem("New Message", "You have a new message from support", "5 hours ago"));
        notificationsList.add(new NotificationItem("Account Alert", "New login detected from Chrome", "1 day ago"));

        notificationAdapter = new NotificationAdapter(notificationsList);
        notificationsRecyclerView.setAdapter(notificationAdapter);

        dragHandle.setOnClickListener(v -> toggleBottomSheet());
    }

    private void toggleBottomSheet() {
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
            View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

                if (isExpanded) {
                    behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    isExpanded = false;
                } else {
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    isExpanded = true;
                }
            }
        }
    }

    public static class NotificationItem {
        public String title;
        public String message;
        public String timestamp;

        public NotificationItem(String title, String message, String timestamp) {
            this.title = title;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
}
