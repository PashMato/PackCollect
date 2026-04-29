package com.eran.packcollect.Fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.eran.packcollect.DataBase.User;
import com.eran.packcollect.Location.Address;
import com.eran.packcollect.Location.SearchLocationCallback;
import com.eran.packcollect.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class EditProfile extends Fragment {
    private NavController navController;

    private ProgressBar loadingBar_PB;
    private EditText address_ET;
    private EditText fullName_ET;
    private EditText phoneNumber_ET;
    private Button save_BT;

    private Address addressLocation;

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

        loadingBar_PB = view.findViewById(R.id.loading_spinner);
        loadingBar_PB.setVisibility(View.GONE);

        address_ET = view.findViewById(R.id.home_address_et);
        fullName_ET = view.findViewById(R.id.full_name_et);
        phoneNumber_ET = view.findViewById(R.id.phone_number_et);

        save_BT = view.findViewById(R.id.update_details_bt);
        ImageButton back_IB = view.findViewById(R.id.back_button);

        address_ET.setOnFocusChangeListener((view1, hasFocus) -> {
            if (hasFocus) { // exit the function if the focus "begins"
                return;
            }

            addressLocation = null;
            Address.searchAddress(view1.getContext(), new SearchLocationCallback() {
                @Override
                public void onSuccess(Address location) {
                    Toast.makeText(view1.getContext(), location.address, Toast.LENGTH_SHORT).show();
                    addressLocation = location;
                    save_BT.setEnabled(isEnabled());
                }

                @Override
                public void onNoResult(String query) {
                    Toast.makeText(view1.getContext(), getString(R.string.location_not_found) + " '" + query + "' ", Toast.LENGTH_LONG).show();
                    addressLocation = null;
                    save_BT.setEnabled(isEnabled());
                }

                @Override
                public void onError(Exception e) {
                    Log.e("OSM", Objects.requireNonNull(e.getMessage()));
                    addressLocation = null;
                    save_BT.setEnabled(isEnabled());
                }
            }, String.valueOf(address_ET.getText()));
        });
        address_ET.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                if (!address_ET.hasFocus()) {
                    return;
                }

                addressLocation = null;
                save_BT.setEnabled(isEnabled());
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        });

        fullName_ET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                save_BT.setEnabled(isEnabled());
            }
        });

        phoneNumber_ET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                save_BT.setEnabled(isEnabled());
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

            String validation = checkValidation(userFullName, userPhoneNumber);

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

            if (aUser == null) { // Update the exiting user
                return;
            }

            showReAuthDialog(User.userNameToEmail(userFullName), task -> {
                if (task.isSuccessful()) {
                    aUser.homeAddress = addressLocation;
                    aUser.fullName = userFullName;
                    aUser.phoneNumber = userPhoneNumber;

                    aUser.updateToDatabase(onSuccessListener, onFailureListener);
                    Log.d("FIREBASE", "Email changed successfully");
                } else {
                    Log.e("FIREBASE", "Failed to change email: " + Objects.requireNonNull(task.getException()).getMessage());
                }
            });
        });

        back_IB.setOnClickListener(view3 -> {
            int action = R.id.action_editProfile_to_viewProfileFragment;

            Bundle bundle = new Bundle();
            bundle.putSerializable("package", aUser);

            navController.navigate(action, bundle);
        });
    }

    private String checkValidation(String fullName, String phoneNumber) {
        // --- Validation ---
        if (fullName.isEmpty()) {
            return getString(R.string.full_name_required);
        }

        if (!fullName.contains(" ")) {
            return getString(R.string.enter_first_last_name);
        }

        if (phoneNumber.isEmpty()) {
            return getString(R.string.phone_number_required);
        }

        // Basic phone check (digits only, 9–15 digits)
        if (!phoneNumber.matches("^\\+?\\d{9,15}$")) {
            return getString(R.string.invalid_phone_number);
        }

        // Check if the location is right
        if (addressLocation == null) {
            return getString(R.string.invalid_location);
        }

        return "";
    }

    private boolean isEnabled() {
        return addressLocation != null &&
                !fullName_ET.getText().toString().isBlank() &&
                !phoneNumber_ET.getText().toString().isBlank() && phoneNumber_ET.getText().length() >= 10 &&
                phoneNumber_ET.getText().toString().matches("^\\+?\\d{9,15}$");
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
        save_BT.setEnabled(false);
        loadingBar_PB.setVisibility(View.VISIBLE);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        // 1. Create the credential using the CURRENT email and password
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

        // 2. Re-authenticate
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                user.updateEmail(newUsername).addOnCompleteListener(onCompleteListener);
            } else {
                Toast.makeText(getContext(), "Authentication failed. Check your password.", Toast.LENGTH_SHORT).show();
                save_BT.setEnabled(isEnabled());
            }
        });
    }
}

