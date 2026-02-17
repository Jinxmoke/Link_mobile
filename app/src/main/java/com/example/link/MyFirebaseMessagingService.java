package com.example.link;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String SOS_CHANNEL_ID = "sos_alerts_channel";
    private static final String GEOFENCE_CHANNEL_ID = "geofence_alerts_channel";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "═══════════════════════════════════════");
        Log.d(TAG, "FCM Message Received from: " + remoteMessage.getFrom());
        Log.d(TAG, "═══════════════════════════════════════");

        String alertType = "sos";
        String title = null;
        String body = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        if (remoteMessage.getData().containsKey("alertType")) {
            alertType = remoteMessage.getData().get("alertType");
        }

        if (title == null || body == null) {
            title = alertType.equals("geofence") ? "⚠️ Geofence Alert" : "🚨 SOS Alert";
            body = "Alert received - tap to view location";
        }

        sendNotification(title, body, alertType, remoteMessage.getData());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "═══════════════════════════════════════");
        Log.d(TAG, "New FCM Token: " + token);
        Log.d(TAG, "═══════════════════════════════════════");
    }

    private void sendNotification(String title, String body, String alertType,
                                  java.util.Map<String, String> data) {
        createNotificationChannels();

        String channelId = alertType.equals("geofence") ? GEOFENCE_CHANNEL_ID : SOS_CHANNEL_ID;

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setAction(alertType.equals("geofence") ? "GEOFENCE_ALERT_CLICK" : "SOS_ALERT_CLICK");

        if (data != null) {
            for (String key : data.keySet()) {
                intent.putExtra(key, data.get(key));
            }
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis() % 10000 > 0 ? (int) System.currentTimeMillis() : 1,
            intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        int color = alertType.equals("geofence") ? 0xFFFFA500 : 0xFFFF0000;

        NotificationCompat.Builder notificationBuilder =
            new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVibrate(alertType.equals("geofence") ?
                    new long[]{0, 500, 250, 500} :
                    new long[]{0, 1000, 500, 1000})
                .setColor(color)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body));

        NotificationManager notificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = alertType.equals("geofence") ? 1 : 2;
        notificationManager.notify(notificationId, notificationBuilder.build());

        Log.d(TAG, "✓ Notification sent - Type: " + alertType);
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            NotificationChannel sosChannel = new NotificationChannel(
                SOS_CHANNEL_ID,
                "SOS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            );
            sosChannel.setDescription("Emergency SOS notifications");
            sosChannel.enableVibration(true);
            sosChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            sosChannel.setSound(defaultSoundUri, null);

            NotificationChannel geofenceChannel = new NotificationChannel(
                GEOFENCE_CHANNEL_ID,
                "Geofence Alerts",
                NotificationManager.IMPORTANCE_HIGH
            );
            geofenceChannel.setDescription("Geofence boundary notifications");
            geofenceChannel.enableVibration(true);
            geofenceChannel.setVibrationPattern(new long[]{0, 500, 250, 500});
            geofenceChannel.setSound(defaultSoundUri, null);

            notificationManager.createNotificationChannel(sosChannel);
            notificationManager.createNotificationChannel(geofenceChannel);
        }
    }
}
