package com.eran.packcollect.Fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.eran.packcollect.DataBase.Package;
import com.eran.packcollect.DataBase.User;
import com.eran.packcollect.Location.Address;
import com.eran.packcollect.Location.SearchLocationCallback;
import com.eran.packcollect.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class EditProfile extends Fragment {
    private NavController navController;

    private EditText address_ET;
    private EditText fullName_ET;
    private EditText phoneNumber_ET;
    private Button save_BT;
    private ImageButton back_IB;

    private Address addressLocation = null;

    private User aUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        if (getArguments() != null) {
            aUser = (User) getArguments().getSerializable("user");
        }

        return inflater.inflate(R.layout.edit_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 'view' here is the root view of your fragment layout
        navController = Navigation.findNavController(view);

        address_ET = view.findViewById(R.id.home_address_et);
        fullName_ET = view.findViewById(R.id.full_name_et);
        phoneNumber_ET = view.findViewById(R.id.phone_number_et);

        save_BT = view.findViewById(R.id.update_details_bt);
        back_IB = view.findViewById(R.id.back_button);

        address_ET.setOnFocusChangeListener((view1, hasFocus) -> {
            save_BT.setEnabled(false);

            if (hasFocus) { // exit the function if the focus "begins"
                addressLocation = null;
                return;
            }

            Address.searchAddress(view1.getContext(), new SearchLocationCallback() {
                @Override
                public void onSuccess(Address location) {
                    Toast.makeText(view1.getContext(), location.address, Toast.LENGTH_SHORT).show();
                    save_BT.setEnabled(!fullName_ET.getText().isEmpty());
                    addressLocation = location;
                }

                @Override
                public void onNoResult(String query) {
                    Toast.makeText(view1.getContext(), getString(R.string.location_not_found) + " '" + query + "' ", Toast.LENGTH_LONG).show();
                    save_BT.setEnabled(false);
                    addressLocation = null;
                }

                @Override
                public void onError(Exception e) {
                    Log.e("OSM", e.getMessage());
                    addressLocation = null;
                }
            }, String.valueOf(address_ET.getText()));
        });

        fullName_ET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String result = s.toString();

                save_BT.setEnabled(addressLocation != null && !result.isBlank());
            }
        });

        if (aUser != null) { // update the fields from the exiting package data
            addressLocation = aUser.homeAddress;
            address_ET.setText(addressLocation != null ? addressLocation.toString() : "");
            fullName_ET.setText(aUser.fullName);
            phoneNumber_ET.setText(aUser.phoneNumber);
        }

        save_BT.setOnClickListener(view2 -> {
            String userFullName = fullName_ET.getText().toString().trim();
            String userPhoneNumber = phoneNumber_ET.getText().toString().trim();

            String validation = checkValidation(userFullName);

            if (!validation.isBlank()) {
                Toast.makeText(view2.getContext(), validation, Toast.LENGTH_LONG).show();
                save_BT.setEnabled(false);
                return;
            }

            OnSuccessListener onSuccessListener = o -> {
                navController.navigate(R.id.action_editProfile_to_viewProfileFragment);
                Toast.makeText(view2.getContext(), getString(R.string.package_saved), Toast.LENGTH_SHORT).show();
            };

            OnFailureListener onFailureListener = e -> {
                Toast.makeText(view2.getContext(), getString(R.string.package_save_failed), Toast.LENGTH_SHORT).show();
                Log.e("DATABASE", "Package write failed: ", e);
            };

            if (aUser != null) { // Update the exiting user
                if (!Objects.equals(aUser.fullName, userFullName)) {
                    showReAuthDialog(User.userNameToEmail(userFullName), task -> {
                        if (task.isSuccessful()) {
                            aUser.homeAddress = addressLocation;
                            aUser.fullName = userFullName;
                            aUser.phoneNumber = userPhoneNumber;

                            aUser.updateToDatabase(onSuccessListener, onFailureListener);
                            Log.d("FIREBASE", "Email changed successfully");
                        } else {
                            Log.e("FIREBASE", "Failed to change email: " + task.getException().getMessage());
                        }
                    });
                }
            }
        });

        back_IB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int action = R.id.action_editProfile_to_viewProfileFragment;

                Bundle bundle = new Bundle();
                bundle.putSerializable("package", aUser);

                navController.navigate(action, bundle);
            }
        });
    }

    private String checkValidation(String packDescription) {
        if (addressLocation == null) {
            return getString(R.string.package_location_invalid);
        }

        if (packDescription.isBlank()) {
            return getString(R.string.package_description_empty);
        }

        return "";
    }

    private boolean isEnabled() {
        return true;
    }

    private void showReAuthDialog(String newUsername, OnCompleteListener onCompleteListener) {
        // 1. Inflate the custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_reauth, null);
        TextInputEditText passwordEt = dialogView.findViewById(R.id.reauth_password_et);

        // 2. Build the Material Dialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext())
                .setView(dialogView)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String password = passwordEt.getText().toString();
                    if (!password.isEmpty()) {
                        reauthenticateAndChangeUsername(password, newUsername, onCompleteListener);
                    } else {
                        Toast.makeText(getContext(), "Password required", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void reauthenticateAndChangeUsername(String password, String newUsername, OnCompleteListener onCompleteListener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        // 1. Create the credential using the CURRENT email and password
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

        // 2. Re-authenticate
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (user != null) {
                    user.updateEmail(newUsername).addOnCompleteListener(onCompleteListener);
                }
            } else {
                Toast.makeText(getContext(), "Authentication failed. Check your password.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

