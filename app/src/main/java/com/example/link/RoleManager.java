package com.example.link;

import android.content.Context;
import android.content.SharedPreferences;

public class RoleManager {

    private static final String PREFS_NAME = "LinkPrefs";
    private static final String KEY_ROLE = "role";
    private static final String KEY_USER_TYPE = "user_type";
    private static final String KEY_PERMISSION = "permission";

    // Role constants - match database permission values
    public static final String ROLE_FULL_ACCESS = "full-access";
    public static final String ROLE_SOS_MONITOR = "map-only";
    public static final String ROLE_LOGS_ACCESS = "logs-only";

    // Check if user is staff
    public static boolean isStaff(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String userType = prefs.getString(KEY_USER_TYPE, "");
        return "staff".equals(userType);
    }

    // Get current role
    public static String getRole(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String role = prefs.getString(KEY_ROLE, "");
        if (role.isEmpty()) {
            role = prefs.getString(KEY_PERMISSION, ROLE_FULL_ACCESS);
        }
        return role;
    }

    // Check role methods
    public static boolean isFullAccess(Context context) {
        return getRole(context).equals(ROLE_FULL_ACCESS);
    }

    public static boolean isSosMonitor(Context context) {
        return getRole(context).equals(ROLE_SOS_MONITOR);
    }

    public static boolean isLogsAccess(Context context) {
        return getRole(context).equals(ROLE_LOGS_ACCESS);
    }

    // Permission check methods - FIXED FOR LOGS-ONLY
    public static boolean canAssignDevice(Context context) {
        // Only FULL ACCESS can assign devices
        return isFullAccess(context);
    }

    public static boolean canViewMap(Context context) {
        // FULL ACCESS and SOS MONITOR can view map
        // LOGS-ONLY CANNOT VIEW MAP
        return isFullAccess(context) || isSosMonitor(context);
    }

    public static boolean canAcknowledgeSOS(Context context) {
        // FULL ACCESS and SOS MONITOR can acknowledge SOS
        // LOGS-ONLY CANNOT ACKNOWLEDGE SOS
        return isFullAccess(context) || isSosMonitor(context);
    }

    public static boolean canViewHistory(Context context) {
        // FIXED: LOGS-ONLY CAN VIEW HISTORY!
        // FULL ACCESS, SOS MONITOR, and LOGS ACCESS can view history
        return isFullAccess(context) || isSosMonitor(context) || isLogsAccess(context);
    }

    public static boolean canViewActivities(Context context) {
        // FIXED: LOGS-ONLY CAN VIEW ACTIVITIES!
        // FULL ACCESS, SOS MONITOR, and LOGS ACCESS can view activities
        return isFullAccess(context) || isSosMonitor(context) || isLogsAccess(context);
    }

    public static boolean canViewCustomers(Context context) {
        // Only FULL ACCESS can view/manage customers (device assignments)
        // LOGS-ONLY CANNOT VIEW CUSTOMERS
        return isFullAccess(context);
    }

    public static boolean canChangeSettings(Context context) {
        // All staff can change their password and view profile
        return isStaff(context);
    }

    // Save role to SharedPreferences
    public static void saveRole(Context context, String role) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_ROLE, role);
        editor.apply();
    }

    // Save permission (from database) to SharedPreferences
    public static void savePermission(Context context, String permission) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_PERMISSION, permission);
        editor.apply();
    }

    // Get role display name
    public static String getRoleDisplayName(Context context) {
        String role = getRole(context);
        switch (role) {
            case ROLE_FULL_ACCESS:
                return "Full Access";
            case ROLE_SOS_MONITOR:
                return "SOS Monitor";
            case ROLE_LOGS_ACCESS:
                return "Logs Access";
            default:
                return "Staff";
        }
    }

    // Get role color
    public static String getRoleColor(Context context) {
        String role = getRole(context);
        switch (role) {
            case ROLE_FULL_ACCESS:
                return "#059669"; // Green
            case ROLE_SOS_MONITOR:
                return "#DC2626"; // Red
            case ROLE_LOGS_ACCESS:
                return "#2563EB"; // Blue
            default:
                return "#6B7280"; // Gray
        }
    }

    // Get role description
    public static String getRoleDescription(Context context) {
        String role = getRole(context);
        switch (role) {
            case ROLE_FULL_ACCESS:
                return "Full system access including device assignment, map viewing, SOS response, and log viewing";
            case ROLE_SOS_MONITOR:
                return "Can view map and respond to SOS alerts, but cannot assign devices";
            case ROLE_LOGS_ACCESS:
                return "Can view history and activity logs only";
            default:
                return "Basic staff access";
        }
    }
}
