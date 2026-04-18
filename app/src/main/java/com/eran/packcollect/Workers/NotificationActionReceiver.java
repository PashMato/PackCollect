package com.eran.packcollect.Workers;

import static androidx.core.content.ContextCompat.getString;

import android.app.Fragment;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.eran.packcollect.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;

public class NotificationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        String noteId = intent.getStringExtra("notificationId");
        String packId = intent.getStringExtra("packageId");

        if (noteId != null) {
            // delete the notification from Firebase immediately
            FirebaseDatabase.getInstance().getReference("notifications")
                    .child(noteId).removeValue();

            // clear the notification from the phone tray
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(noteId.hashCode());
        }
        // deleting the package from the firebase
        if ("ACTION_YES".equals(action) && packId != null) {
            NotificationActionReceiver.confirmCollection(context, packId, null);
        } else if ("ACTION_NO".equals(action)) {
            // Logic for rejection
            Toast.makeText(context, getString(context, R.string.cancelled), Toast.LENGTH_SHORT).show();
        }
    }

    public static void confirmCollection(Context context, String packId, OnCompleteListener onCompleteListener) {
        FirebaseDatabase.getInstance().getReference("packages")
                .child(packId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(context, context.getString(R.string.confirmed), Toast.LENGTH_SHORT).show();
                        if (onCompleteListener != null) {
                            onCompleteListener.onComplete(task);
                        }
                    }
                });
    }
}