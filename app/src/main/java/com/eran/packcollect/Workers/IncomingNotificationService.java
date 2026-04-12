package com.eran.packcollect.Workers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.eran.packcollect.DataBase.NotificationFB;
import com.eran.packcollect.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class IncomingNotificationService extends Service {

    private static final int SERVICE_ID = 101; // ID for the persistent service notification
    private static final String CHANNEL_ID = "IncomingNotesChannel";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Messages", NotificationManager.IMPORTANCE_HIGH);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        // Keep service alive
        Notification persistentNote = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Checking for messages")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();

        startForeground(SERVICE_ID, persistentNote);
        startFirebaseListener();

        return START_STICKY;
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, IncomingNotificationService.class);
        context.startForegroundService(intent);
    }

    private void startFirebaseListener() {
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) return;

        // Listen for notifications where YOU are the receiver
        Query query = FirebaseDatabase.getInstance().getReference("notifications")
                .orderByChild("receiverUid")
                .equalTo(myUid);

        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                NotificationFB note = snapshot.getValue(NotificationFB.class);
                if (note != null) {
                    showDecisionNotification(note);
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void showDecisionNotification(NotificationFB note) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Intent for YES
        Intent yesIntent = new Intent(this, NotificationActionReceiver.class);
        yesIntent.setAction("ACTION_YES");
        yesIntent.putExtra("notificationId", note.getNotificationId());
        PendingIntent yesPending = PendingIntent.getBroadcast(this, (note.getNotificationId() + "yes").hashCode(),
                yesIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Intent for NO
        Intent noIntent = new Intent(this, NotificationActionReceiver.class);
        noIntent.setAction("ACTION_NO");
        noIntent.putExtra("notificationId", note.getNotificationId());
        PendingIntent noPending = PendingIntent.getBroadcast(this, (note.getNotificationId() + "no").hashCode(),
                noIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification popup = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_package)
                .setContentTitle("Package Collected?")
                .setContentText("A user is at your location to collect a package.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(android.R.drawable.ic_input_add, "Yes", yesPending)
                .addAction(android.R.drawable.ic_delete, "No", noPending)
                .setAutoCancel(true)
                .build();

        manager.notify(note.getNotificationId().hashCode(), popup);
    }
}