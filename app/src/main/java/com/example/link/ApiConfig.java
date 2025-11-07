package com.example.link;

public class ApiConfig {

//  public static final String BASE_URL = "https://cbhms.ucc-bsit.org/LinkApi/";
    public static final String BASE_URL = "http://192.168.1.5/LinkApi/";

    public static final String REGISTER_URL = BASE_URL + "register.php";
    public static final String LOGIN_URL = BASE_URL + "login.php";
    public static final String VERIFY_URL = BASE_URL + "verify.php";
    public static final String VERIFY_DEVICE_URL = BASE_URL + "verify_device.php";

    public static final String ADD_FAMILY_MEMBER_URL = BASE_URL + "add_family_members.php";
    public static final String GET_FAMILY_MEMBERS_URL = BASE_URL + "get_family_members.php";
    public static final String DELETE_FAMILY_MEMBER_URL = BASE_URL + "delete_family_members.php";
    public static final String UPDATE_FAMILY_MEMBER_URL = BASE_URL + "update_family_members.php";
}
