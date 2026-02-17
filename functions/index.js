const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// ============================================
// SOS ALERT NOTIFICATION
// ============================================
exports.sendSOSNotification = functions.database
    .ref("/sos_alerts/{assignmentId}/{transmitterSerial}")
    .onWrite(async (change, context) => {
        const assignmentId = context.params.assignmentId;
        const transmitterSerial = context.params.transmitterSerial;
        
        const sosData = change.after.val();
        
        // If data was deleted, skip
        if (!sosData) {
            console.log("SOS data deleted, skipping notification");
            return null;
        }
        
        // Check if already acknowledged to avoid duplicate notifications
        if (sosData.acknowledged === true) {
            console.log("SOS already acknowledged, skipping notification");
            return null;
        }

        // Check if this is an update vs new alert
        const oldData = change.before.val();
        const isNewAlert = !oldData || oldData.id !== sosData.id;
        
        if (!isNewAlert) {
            console.log("SOS alert updated but not new, skipping notification");
            return null;
        }

        console.log("Sending SOS notification for:", transmitterSerial);

        const assignedName = sosData.assigned_name || 'Unknown Device';
        const lat = sosData.latitude || 0;
        const lng = sosData.longitude || 0;

        const payload = {
            notification: {
                title: "🚨 EMERGENCY SOS ALERT!",
                body: `${assignedName} needs help! Tap to view location.`,
                sound: "default"
            },
            data: {
                alertType: "sos",
                transmitter_serial: transmitterSerial,
                assignment_id: String(assignmentId),
                latitude: String(lat),
                longitude: String(lng),
                assigned_name: assignedName,
                assigned_contact: sosData.assigned_contact || "",
                alert_time: sosData.alert_time || "",
                battery_percent: String(sosData.battery_percent || 0),
                click_action: "SOS_ALERT_CLICK"
            }
        };

        // Send to topic
        try {
            const response = await admin.messaging().sendToTopic("sos_alerts", payload);
            console.log("✓ SOS notification sent successfully:", response);
            return null;
        } catch (error) {
            console.error("✗ Error sending SOS notification:", error);
            return null;
        }
    });

// ============================================
// GEOFENCE ALERT NOTIFICATION
// ============================================
exports.sendGeofenceNotification = functions.database
    .ref("/geofence_alerts/{assignmentId}/{transmitterSerial}")
    .onWrite(async (change, context) => {
        const assignmentId = context.params.assignmentId;
        const transmitterSerial = context.params.transmitterSerial;
        
        const geofenceData = change.after.val();
        
        // If data was deleted, skip
        if (!geofenceData) {
            console.log("Geofence data deleted, skipping notification");
            return null;
        }
        
        // If resolved, skip
        if (geofenceData.resolved === true) {
            console.log("Geofence alert resolved, skipping notification");
            return null;
        }
        
        // Check if already acknowledged
        if (geofenceData.acknowledged === true) {
            console.log("Geofence alert already acknowledged, skipping");
            return null;
        }
        
        // Only send notification if device is outside geofence
        if (geofenceData.is_outside !== true) {
            console.log("Device is inside geofence, skipping notification");
            return null;
        }

        // Check if this is a new alert (not just an update)
        const oldData = change.before.val();
        const isNewAlert = !oldData || oldData.id !== geofenceData.id;
        
        if (!isNewAlert) {
            console.log("Geofence alert updated but not new, skipping notification");
            return null;
        }

        console.log("Sending geofence notification for:", transmitterSerial);

        const assignedName = geofenceData.assigned_name || 'Unknown Device';
        const distance = Math.round(geofenceData.distance_from_base || 0);
        const radius = Math.round(geofenceData.geofence_radius || 0);
        const baseStationName = geofenceData.base_station?.name || "Base Station";
        const lat = geofenceData.latitude || 0;
        const lng = geofenceData.longitude || 0;

        const payload = {
            notification: {
                title: "⚠️ Geofence Boundary Alert",
                body: `${assignedName} is ${distance}m away from ${baseStationName} (limit: ${radius}m)`,
                sound: "default"
            },
            data: {
                alertType: "geofence",
                transmitter_serial: transmitterSerial,
                assignment_id: String(assignmentId),
                latitude: String(lat),
                longitude: String(lng),
                assigned_name: assignedName,
                assigned_contact: geofenceData.assigned_contact || "",
                distance_from_base: String(distance),
                geofence_radius: String(radius),
                base_station_name: baseStationName,
                alert_time: geofenceData.alert_time || "",
                click_action: "GEOFENCE_ALERT_CLICK"
            }
        };

        // Send to topic
        try {
            const response = await admin.messaging().sendToTopic("geofence_alerts", payload);
            console.log("✓ Geofence notification sent successfully:", response);
            return null;
        } catch (error) {
            console.error("✗ Error sending geofence notification:", error);
            return null;
        }
    });