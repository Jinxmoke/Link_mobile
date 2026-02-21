package com.example.link;

public class ApiConfig {
    public static final String BASE_URL    = "https://cbhms.ucc-bsit.org/LinkApi/";
    public static final String UPLOADS_URL = "https://cbhms.ucc-bsit.org/";  // sibling of LinkApi/

    // Authentication
    public static final String LOGIN_URL           = BASE_URL + "login.php";
    public static final String CHANGE_PASSWORD_URL = BASE_URL + "change_password.php";

    // Map & Location
    public static final String GET_BASE_STATIONS_URL     = BASE_URL + "get_base_stations.php";
    public static final String UPDATE_STAFF_LOCATION_URL = BASE_URL + "update_staff_location.php";

    // Device Management
    public static final String GET_DEVICES_URL       = BASE_URL + "get_devices.php";
    public static final String ASSIGN_DEVICE_URL     = BASE_URL + "assign_device.php";
    public static final String END_ASSIGNMENT_URL    = BASE_URL + "end_assignment.php";
    public static final String GET_DEVICE_ACTIVITIES = BASE_URL + "get_device_activities.php";

    // Customer Status
    public static final String GET_ACTIVE_CUSTOMERS_URL = BASE_URL + "get_active_customers.php";

    // SOS
    public static final String GET_RESOLVED_SOS_URL  = BASE_URL + "get_resolved_sos.php";
    public static final String RESOLVE_SOS_BY_QR_URL = BASE_URL + "resolve_sos_by_qr.php";

    // Profile
    public static final String UPDATE_PROFILE_URL = BASE_URL + "update_profile.php";
}
