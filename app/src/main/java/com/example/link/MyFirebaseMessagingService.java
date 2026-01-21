package com.example.link;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "fcm_default_channel";
    private static final String SOS_CHANNEL_ID = "sos_alerts_channel";
    private static final int SOS_NOTIFICATION_ID = 999;
    private static final int REGULAR_NOTIFICATION_ID = 0;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "═══════════════════════════════════════");
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());
        Log.d(TAG, "═══════════════════════════════════════");

        Map<String, String> data = remoteMessage.getData();

        // Check if this is an SOS alert
        if (data.containsKey("type") && "sos_alert".equals(data.get("type"))) {
            Log.d(TAG, "🚨 SOS ALERT RECEIVED!");
            handleSOSAlert(data, remoteMessage.getNotification());
            return;
        }

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            Log.d(TAG, "Notification Title: " + title);
            Log.d(TAG, "Notification Body: " + body);

            sendRegularNotification(
                title != null ? title : "New Message",
                body != null ? body : "",
                data
            );
        }

        // Handle data-only messages
        if (remoteMessage.getData().size() > 0 && remoteMessage.getNotification() == null) {
            Log.d(TAG, "Data-only message received: " + remoteMessage.getData());
        }
    }

    private void handleSOSAlert(Map<String, String> data, RemoteMessage.Notification notification) {
        String transmitterSerial = data.get("transmitter_serial");
        String assignedTo = data.get("assigned_to");
        String username = data.get("username");
        String latitude = data.get("latitude");
        String longitude = data.get("longitude");
        String batteryPercent = data.get("battery_percent");
        String alertTime = data.get("alert_time");
        String userId = data.get("user_id");
        String sosAlertId = data.get("sos_alert_id");

        // Use assigned name if available, otherwise use username
        String displayName = (assignedTo != null && !assignedTo.isEmpty()) ? assignedTo : username;

        // Get title and body from notification payload or use defaults
        String title = "🚨 SOS ALERT!";
        String body = displayName + " needs immediate assistance!";

        if (notification != null) {
            title = notification.getTitle() != null ? notification.getTitle() : title;
            body = notification.getBody() != null ? notification.getBody() : body;
        }

        Log.d(TAG, "Processing SOS Alert:");
        Log.d(TAG, "  - Device: " + transmitterSerial);
        Log.d(TAG, "  - Person: " + displayName);
        Log.d(TAG, "  - Location: " + latitude + ", " + longitude);
        Log.d(TAG, "  - Battery: " + batteryPercent + "%");
        Log.d(TAG, "  - Alert ID: " + sosAlertId);

        // Create notification channels
        createNotificationChannels();

        // Create intent for clicking the notification
        Intent sosIntent = new Intent(this, MainActivity.class);
        sosIntent.setAction("SOS_ALERT_CLICK");
        sosIntent.putExtra("is_sos", true);
        sosIntent.putExtra("transmitter_serial", transmitterSerial);
        sosIntent.putExtra("latitude", latitude);
        sosIntent.putExtra("longitude", longitude);
        sosIntent.putExtra("battery", batteryPercent);
        sosIntent.putExtra("assigned_to", displayName);
        sosIntent.putExtra("user_id", userId);
        sosIntent.putExtra("sos_alert_id", sosAlertId);
        sosIntent.putExtra("alert_time", alertTime);
        sosIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            SOS_NOTIFICATION_ID,
            sosIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create map view action intent
        Intent mapIntent = new Intent(this, MainActivity.class);
        mapIntent.putExtra("open_map", true);
        mapIntent.putExtra("sos_location", true);
        mapIntent.putExtra("latitude", latitude);
        mapIntent.putExtra("longitude", longitude);
        PendingIntent mapPendingIntent = PendingIntent.getActivity(
            this,
            SOS_NOTIFICATION_ID + 1,
            mapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Alarm sound for SOS
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }

        // Build expanded text
        String expandedText = body + "\n\n" +
            "Device: " + transmitterSerial + "\n" +
            "Battery: " + batteryPercent + "%\n" +
            "Location: " + latitude + ", " + longitude;

        // Build SOS notification with high priority
        NotificationCompat.Builder notificationBuilder =
            new NotificationCompat.Builder(this, SOS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_siren) // Use your SOS icon if available
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(new long[]{0, 1000, 500, 1000, 500, 1000})
                .setLights(Color.RED, 1000, 1000)
                .setSound(alarmSound)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_siren, "View on Map", mapPendingIntent)
                .setOngoing(false) // Allow dismissal
                .setTimeoutAfter(5 * 60 * 1000) // Auto-cancel after 5 minutes
                .setStyle(new NotificationCompat.BigTextStyle().bigText(expandedText));

        if (alertTime != null) {
            notificationBuilder.setWhen(System.currentTimeMillis()).setShowWhen(true);
        }

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(SOS_NOTIFICATION_ID, notificationBuilder.build());
                    Log.d(TAG, "✓ SOS notification displayed successfully");
                } else {
                    Log.w(TAG, "✗ Notification permission not granted, cannot show SOS alert");
                }
            } else {
                notificationManager.notify(SOS_NOTIFICATION_ID, notificationBuilder.build());
                Log.d(TAG, "✓ SOS notification displayed successfully (no permission needed)");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "✗ Security exception showing notification: " + e.getMessage());
        }
    }

    private void sendRegularNotification(String title, String messageBody, Map<String, String> data) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Add data extras if available
        if (data != null && !data.isEmpty()) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            REGULAR_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
            new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_siren)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        if (messageBody.length() > 50) {
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody));
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(REGULAR_NOTIFICATION_ID, notificationBuilder.build());
                    Log.d(TAG, "✓ Regular notification displayed");
                } else {
                    Log.w(TAG, "✗ Notification permission not granted");
                }
            } else {
                notificationManager.notify(REGULAR_NOTIFICATION_ID, notificationBuilder.build());
                Log.d(TAG, "✓ Regular notification displayed");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "✗ Security exception showing notification: " + e.getMessage());
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            // SOS Alert Channel
            NotificationChannel sosChannel = new NotificationChannel(
                SOS_CHANNEL_ID,
                "SOS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            );
            sosChannel.setDescription("Critical SOS alert notifications");
            sosChannel.enableLights(true);
            sosChannel.setLightColor(Color.RED);
            sosChannel.enableVibration(true);
            sosChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000, 500, 1000});
            sosChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            sosChannel.setBypassDnd(true);
            sosChannel.setImportance(NotificationManager.IMPORTANCE_HIGH);

            // Regular Notification Channel
            NotificationChannel regularChannel = new NotificationChannel(
                CHANNEL_ID,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            regularChannel.setDescription("General app notifications");
            regularChannel.enableLights(true);
            regularChannel.setLightColor(Color.BLUE);

            notificationManager.createNotificationChannel(sosChannel);
            notificationManager.createNotificationChannel(regularChannel);

            Log.d(TAG, "✓ Notification channels created");
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "═══════════════════════════════════════");
        Log.d(TAG, "New FCM token generated: " + token);
        Log.d(TAG, "═══════════════════════════════════════");

        // Send token to your server
        // sendTokenToServer(token);
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
        Log.d(TAG, "⚠ Messages deleted on server");
    }

    @Override
    public void onMessageSent(String msgId) {
        super.onMessageSent(msgId);
        Log.d(TAG, "✓ Message sent: " + msgId);
    }

    @Override
    public void onSendError(String msgId, Exception exception) {
        super.onSendError(msgId, exception);
        Log.e(TAG, "✗ Send error for message: " + msgId, exception);
    }
}
