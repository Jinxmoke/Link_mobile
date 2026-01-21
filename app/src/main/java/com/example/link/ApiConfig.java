package com.example.link;

public class ApiConfig {
    public static final String BASE_URL = "https://cbhms.ucc-bsit.org/LinkApi/";

    // Existing URLs
    public static final String LOGIN_URL = BASE_URL + "login.php";
    public static final String GET_BASE_STATIONS_URL = BASE_URL + "get_base_stations.php";
    public static final String UPDATE_STAFF_LOCATION_URL = BASE_URL + "update_staff_location.php";

    // New URLs for device management
    public static final String GET_DEVICES_URL = BASE_URL + "get_devices.php";
    public static final String ASSIGN_DEVICE_URL = BASE_URL + "assign_device.php";


}
