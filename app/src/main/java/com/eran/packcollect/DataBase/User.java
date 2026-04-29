package com.eran.packcollect.DataBase;

import androidx.annotation.NonNull;

import com.eran.packcollect.Location.Address;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.Serializable;

public class User implements Serializable {
    public String userUid = "";
    public String fullName;
    public String phoneNumber;
    public Address homeAddress;

    public User() {}

    private User(String fullName, String phoneNumber, Address homeAddress) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.homeAddress = homeAddress;
    }

    public static User createUser(String fullName, String phoneNumber, Address homeAddress, @NonNull OnSuccessListener<Void> onSuccessListener, @NonNull OnFailureListener onFailureListener) {
        User user = new User(fullName, phoneNumber, homeAddress);
        user.updateToDatabase(onSuccessListener, onFailureListener);

        return user;
    }

    public void updateToDatabase(@NonNull OnSuccessListener<Void> onSuccessListener, @NonNull OnFailureListener onFailureListener) {
        DatabaseReference packagesRef = FirebaseDatabase.getInstance().getReference("users");
        DatabaseReference targetRef;

        // Update existing not create new
        if (fullName == null || userUid.isEmpty()) {
            // if No ID exists creating a new package
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user == null) {
                onFailureListener.onFailure(new NullPointerException("user is null"));
                return;
            }

            userUid = user.getUid();
        }

        targetRef = packagesRef.child(userUid);
        targetRef.setValue(this)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener);
    }

    public static String userNameToEmail(String userName) {
        return userName.trim()
                .replaceAll("\\s+", ".") + "@packcollect.internal"; // handles multiple spaces
    }
}
