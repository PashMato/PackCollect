package com.eran.packcollect.DataBase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.eran.packcollect.Location.Address;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.Serializable;
import java.util.Calendar;

public class Package implements Serializable {
    static final short EXPIRATION_IN_WEEKS = 2;

    public String packageId;
    public String ownerUid;
    public long expiresAt;
    public Address packageAddress;
    public String description;
    public String additionalNotes;

    public Package() {}

    private Package(Address packageAddress, String description, String additionalInfo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_YEAR, EXPIRATION_IN_WEEKS);
        expiresAt = cal.getTimeInMillis();
        this.packageAddress = packageAddress;
        this.description = description;
        this.additionalNotes = additionalInfo;
    }

    public static Package savePackageForUser(Address packageAddress, String description, String additionalInfo,
                           @NonNull OnSuccessListener onSuccessListener, @NonNull OnFailureListener onFailureListener) {
        Package pack = new Package(packageAddress, description, additionalInfo);

        pack.updateToDataBase(onSuccessListener, onFailureListener);
        return pack;
    }

    public void updateToDataBase(@NonNull OnSuccessListener<Void> onSuccessListener, @NonNull OnFailureListener onFailureListener) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Ensure the user owns this package before writing
        if (ownerUid != null && !ownerUid.equals(uid)) {
            Log.e("FireBase", "Security Breach: Attempting to write a package owned by another user.");
            onFailureListener.onFailure(new Exception("Permission denied: You do not own this package."));
            return;
        }

        DatabaseReference packagesRef = FirebaseDatabase.getInstance().getReference("packages");
        DatabaseReference targetRef;

        // Update existing not create new
        if (packageId != null && !packageId.isEmpty()) {
            // We have an ID, so point to the EXISTING node
            targetRef = packagesRef.child(packageId);
        } else {
            // if No ID exists creating a new package
            targetRef = packagesRef.push();
            this.packageId = targetRef.getKey();
            this.ownerUid = uid;
        }

        targetRef.setValue(this)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener);
    }
}
