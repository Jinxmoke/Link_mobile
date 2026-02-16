package com.example.link;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SOSAlert {
    private int id;
    private String transmitterSerial;
    private int assignmentId;
    private String customerName;
    private String customerContact;
    private double latitude;
    private double longitude;
    private int batteryPercent;
    private int rssi;
    private String alertTime;
    private String acknowledgedAt;
    private String acknowledgedByName;
    private String resolvedAt;
    private String resolvedByName;
    private String resolutionNotes;

    // Constructor
    public SOSAlert(int id, String transmitterSerial, int assignmentId, String customerName,
                    String customerContact, double latitude, double longitude, int batteryPercent,
                    int rssi, String alertTime, String acknowledgedAt, String acknowledgedByName,
                    String resolvedAt, String resolvedByName, String resolutionNotes) {
        this.id = id;
        this.transmitterSerial = transmitterSerial;
        this.assignmentId = assignmentId;
        this.customerName = customerName;
        this.customerContact = customerContact;
        this.latitude = latitude;
        this.longitude = longitude;
        this.batteryPercent = batteryPercent;
        this.rssi = rssi;
        this.alertTime = alertTime;
        this.acknowledgedAt = acknowledgedAt;
        this.acknowledgedByName = acknowledgedByName;
        this.resolvedAt = resolvedAt;
        this.resolvedByName = resolvedByName;
        this.resolutionNotes = resolutionNotes;
    }

    // Getters
    public int getId() { return id; }
    public String getTransmitterSerial() { return transmitterSerial; }
    public int getAssignmentId() { return assignmentId; }
    public String getCustomerName() {
        return (customerName != null && !customerName.isEmpty()) ? customerName : "Unknown Customer";
    }
    public String getCustomerContact() { return customerContact; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getBatteryPercent() { return batteryPercent; }
    public int getRssi() { return rssi; }
    public String getAlertTime() { return alertTime; }
    public String getAcknowledgedAt() { return acknowledgedAt; }
    public String getAcknowledgedByName() { return acknowledgedByName; }
    public String getResolvedAt() { return resolvedAt; }
    public String getResolvedByName() { return resolvedByName; }
    public String getResolutionNotes() { return resolutionNotes; }

    // Helper methods
    public String getFormattedLocation() {
        return String.format(Locale.US, "%.6f, %.6f", latitude, longitude);
    }

    public String getFormattedDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("M/d/yyyy", Locale.US);

            // For resolved alerts, show resolved date
            String dateString = (resolvedAt != null && !resolvedAt.isEmpty() && !resolvedAt.equals("null"))
                ? resolvedAt : alertTime;

            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return "N/A";
        } catch (Exception e) {
            return "N/A";
        }
    }

    public String getFormattedTime() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm a", Locale.US);

            // For resolved alerts, show resolved time
            String dateString = (resolvedAt != null && !resolvedAt.isEmpty() && !resolvedAt.equals("null"))
                ? resolvedAt : alertTime;

            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return "N/A";
        } catch (Exception e) {
            return "N/A";
        }
    }
}
