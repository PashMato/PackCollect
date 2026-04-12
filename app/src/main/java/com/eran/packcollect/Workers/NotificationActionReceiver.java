package com.eran.packcollect.Workers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import com.google.firebase.database.FirebaseDatabase;

public class NotificationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String notificationId = intent.getStringExtra("notificationId");

        if (notificationId != null) {
            // 1. Delete the notification from Firebase immediately
            FirebaseDatabase.getInstance().getReference("notifications")
                    .child(notificationId).removeValue();

            // 2. Clear the notification from the phone tray
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(notificationId.hashCode());
        }

        if ("ACTION_YES".equals(action)) {
            // Logic for when the owner confirms collection
            Toast.makeText(context, "Confirmed!", Toast.LENGTH_SHORT).show();
        } else if ("ACTION_NO".equals(action)) {
            // Logic for rejection
            Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show();
        }
    }
}