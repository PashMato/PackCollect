package com.eran.packcollect.DataBase;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

public class Notification {
    private String notificationId;
    private String senderUid;
    private String receiverUid;
    private String packageUid;
    private NotificationModes notificationMode;

    public Notification() {} // fo the firebase
    private Notification(String senderUid, String receiverUid, String packageUid, NotificationModes notificationMode) {
        this.senderUid = senderUid;
        this.receiverUid = receiverUid;
        this.packageUid = packageUid;
        this.notificationMode = notificationMode;
    }

    public static boolean sendNotification(String receiverUid, String packageUid, NotificationModes notificationMode) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (uid.isBlank() || uid.equals(receiverUid)) {
            return false;
        }

        //TODO: check if the details are actually exists
        Notification notification = new Notification(uid, receiverUid, packageUid, notificationMode);

        notification.updateToDataBase();
        return true;
    }

    private void updateToDataBase() {
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");
        String duplicatePreventerId = senderUid + "_" + receiverUid + "_" + packageUid + "_" + notificationMode;
        DatabaseReference targetRef = notificationsRef.child(duplicatePreventerId);

        this.notificationId = duplicatePreventerId;

        // 1. Check if it exists first
        targetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // If it exists, just stop here. No rewrite = No crash!
                    Log.d("Firebase", "Notification already exists. Skipping write.");
                } else {
                    // 2. If it doesn't exist, it's safe to write
                    targetRef.setValue(Notification.this)
                            .addOnSuccessListener(aVoid -> Log.d("Firebase", "Notification sent!"))
                            .addOnFailureListener(e -> Log.e("Firebase", "Security Rules blocked it: " + e.getMessage()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Read failed: " + error.getMessage());
            }
        });
    }

    public String getNotificationId() {
        return notificationId;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public String getReceiverUid() {
        return receiverUid;
    }

    public String getPackageUid() {
        return packageUid;
    }

    public NotificationModes getNotificationMode() {
        return notificationMode;
    }
}
