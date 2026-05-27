package com.medication.guide;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class MedicationApp extends Application {
    public static final String CHANNEL_REMINDER = "medication_reminder";
    public static final String CHANNEL_RISK = "risk_alert";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel reminderChannel = new NotificationChannel(
                    CHANNEL_REMINDER,
                    "服药提醒",
                    NotificationManager.IMPORTANCE_HIGH
            );
            reminderChannel.setDescription("按时服药提醒通知");
            reminderChannel.enableVibration(true);

            NotificationChannel riskChannel = new NotificationChannel(
                    CHANNEL_RISK,
                    "风险预警",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            riskChannel.setDescription("用药依从性风险提醒");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(reminderChannel);
            manager.createNotificationChannel(riskChannel);
        }
    }
}
