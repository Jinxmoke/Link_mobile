package com.example.link;

public class DeviceLocation {
    private int id;
    private String serialNumber;
    private String customerName;
    private String customerContact;
    private String latitude;
    private String longitude;
    private String dateTime;
    private int batteryPercent;
    private String deviceOwner;
    private String altitude;
    private String speed;
    private int assignedBy;
    private String assignedByName;
    private String assignedAt;

    public DeviceLocation() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerContact() { return customerContact; }
    public void setCustomerContact(String customerContact) { this.customerContact = customerContact; }

    public String getLatitude() { return latitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }

    public String getLongitude() { return longitude; }
    public void setLongitude(String longitude) { this.longitude = longitude; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public int getBatteryPercent() { return batteryPercent; }
    public void setBatteryPercent(int batteryPercent) { this.batteryPercent = batteryPercent; }

    public String getDeviceOwner() { return deviceOwner; }
    public void setDeviceOwner(String deviceOwner) { this.deviceOwner = deviceOwner; }

    public String getAltitude() { return altitude; }
    public void setAltitude(String altitude) { this.altitude = altitude; }

    public String getSpeed() { return speed; }
    public void setSpeed(String speed) { this.speed = speed; }

    public int getAssignedBy() { return assignedBy; }
    public void setAssignedBy(int assignedBy) { this.assignedBy = assignedBy; }

    public String getAssignedByName() { return assignedByName; }
    public void setAssignedByName(String assignedByName) { this.assignedByName = assignedByName; }

    public String getAssignedAt() { return assignedAt; }
    public void setAssignedAt(String assignedAt) { this.assignedAt = assignedAt; }
}
