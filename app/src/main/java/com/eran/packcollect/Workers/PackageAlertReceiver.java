package com.eran.packcollect.Workers;

import static androidx.core.content.ContextCompat.getSystemService;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class PackageAlertReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "package_alerts_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Verify the action matches what we declared in the manifest
        if (intent.getAction() != null && intent.getAction().equals("com.example.your_app.PACKAGE_UPDATE")) {

            // Extract the package data sent via the Intent
            String packageAddress = intent.getStringExtra("PACKAGE_ADDRESS");
            String notificationString = "Your package ";
            if (packageAddress != null) {
                notificationString += "at `" + packageAddress + "` ";
            }

            showNotification(context, notificationString + "was deleted because it's expiration date has passed");
        }
    }

    public static void createNotification(Context context, String address, long expirationDate) {
        Intent intent = new Intent(context, PackageAlertReceiver.class);
        intent.setAction("com.example.your_app.PACKAGE_UPDATE");
        intent.putExtra("PACKAGE_ADDRESS", address);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, expirationDate, pendingIntent);
        }
    }

    public static void cancelAllNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll(); // This clears the visible UI alerts
    }

    private void showNotification(Context context, String packageName) {
        createNotificationChannel(context);

        // Double-check permission before notifying
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // If the user denied permission, silently fail or handle it elsewhere
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app's actual drawable
                .setContentTitle("Package Deleted!")
                .setContentText(packageName)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Use HIGH for alerts
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        notificationManager.notify(101, builder.build());
    }

    private void createNotificationChannel(Context context) {
        CharSequence name = "Package Alerts";
        String description = "Notifications for package tracking updates";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }
}