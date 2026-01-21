package com.example.link;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SOSAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "SOSAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "SOS reminder triggered");

        // You can re-trigger the notification here if needed
        // This helps ensure the SOS alert is not missed

        // For now, just log it
        String serial = intent.getStringExtra("transmitter_serial");
        Log.d(TAG, "Reminder for device: " + serial);
    }
}
