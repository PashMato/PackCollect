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

public class User implements Serializable {
    public String fullName;
    public String phoneNumber;
    public Address homeAddress;

    public User() {}

    public User(String fullName, String phoneNumber, Address homeAddress) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.homeAddress = homeAddress;
    }

    public void updateToDataBase(@NonNull OnSuccessListener<Void> onSuccessListener, @NonNull OnFailureListener onFailureListener) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference packagesRef = FirebaseDatabase.getInstance().getReference("users");
        DatabaseReference targetRef;

//        // Update existing not create new
//        if (fullName != null && !packageId.isEmpty()) {
//            // We have an ID, so point to the EXISTING node
//            targetRef = packagesRef.child(packageId);
//        } else {
//            // if No ID exists creating a new package
//            targetRef = packagesRef.push();
//            this.packageId = targetRef.getKey();
//            this.ownerUid = uid;
//        }
//
//        targetRef.setValue(this)
//                .addOnSuccessListener(onSuccessListener)
//                .addOnFailureListener(onFailureListener);
    }
}
