package com.eran.packcollect.DataBase;

import androidx.annotation.NonNull;

import com.eran.packcollect.Location.Address;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class Package {
    static final short EXPIRATION_IN_WEEKS = 2;

    public String ownerUid;
    public long expiresAt;
    public Address packageAddress;
    public String description;
    public String additional_info;

    public Package() {}

    private Package(Address packageAddress, String description, String additionalInfo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_YEAR, EXPIRATION_IN_WEEKS);
        expiresAt = cal.getTimeInMillis();

        this.packageAddress = packageAddress;
        this.description = description;
        this.additional_info = additionalInfo;
    }

    public static Package savePackageForUser(Address packageAddress, String description, String additionalInfo,
                           @NonNull OnSuccessListener onSuccessListener, @NonNull OnFailureListener onFailureListener) {
        Package pack = new Package(packageAddress, description, additionalInfo);
        //  TODO: add a limit on how many packages you can own
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        pack.ownerUid = uid; // save owner UID in the package

        DatabaseReference packagesRef = FirebaseDatabase.getInstance().getReference("packages");

        // push generates a unique key for this package
        DatabaseReference newPackageRef = packagesRef.push();

        newPackageRef.setValue(pack)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener);
        return pack;
    }
}
