package com.example.link;

public class CustomerDevice {
    private String serialNumber;
    private String deviceName;
    private String status;
    private int batteryPercent;
    private String customerName;
    private String customerContact;
    private int assignmentId;
    private double latitude;
    private double longitude;
    private String lastUpdate;
    private int minutesAgo;

    // Constructor
    public CustomerDevice() {
    }

    // Getters and Setters
    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getBatteryPercent() {
        return batteryPercent;
    }

    public void setBatteryPercent(int batteryPercent) {
        this.batteryPercent = batteryPercent;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerContact() {
        return customerContact;
    }

    public void setCustomerContact(String customerContact) {
        this.customerContact = customerContact;
    }

    public int getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(int assignmentId) {
        this.assignmentId = assignmentId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public int getMinutesAgo() {
        return minutesAgo;
    }

    public void setMinutesAgo(int minutesAgo) {
        this.minutesAgo = minutesAgo;
    }

    public String getFormattedLocation() {
        return String.format("%.5f, %.5f", latitude, longitude);
    }

    public String getFormattedLastUpdate() {
        if (minutesAgo == 0) {
            return "Just now";
        } else if (minutesAgo == 1) {
            return "1 minute ago";
        } else if (minutesAgo < 60) {
            return minutesAgo + " minutes ago";
        } else {
            int hours = minutesAgo / 60;
            if (hours == 1) {
                return "1 hour ago";
            } else {
                return hours + " hours ago";
            }
        }
    }
}
