package com.eran.packcollect.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpFragment extends Fragment {
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private NavController navController;
    private Button signIn_BT;
    private TextView login_TV;


    private EditText fullName_ET;
    private EditText password_ET;
    private EditText phoneNumber_ET;
    private EditText homeAddress_ET;

    private Address addressLocation = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sign_up, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 'view' here is the root view of your fragment layout
        navController = Navigation.findNavController(view);


        fullName_ET = view.findViewById(R.id.user_name_et);
        password_ET = view.findViewById(R.id.password_et);
        phoneNumber_ET = view.findViewById(R.id.phone_et);
        homeAddress_ET = view.findViewById(R.id.address_et);
        homeAddress_ET.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) { // exit the function if the focus "begins"
                    return;
                }

                Address.searchAddress(view.getContext(), new SearchLocationCallback() {
                    @Override
                    public void onSuccess(Address location) {
                        Toast.makeText(view.getContext(), location.address, Toast.LENGTH_SHORT).show();
                        addressLocation = location;
                    }

                    @Override
                    public void onNoResult(String query) {
                        Toast.makeText(view.getContext(), getString(R.string.location_not_found) + " '" + query + "'", Toast.LENGTH_LONG).show();
                        addressLocation = null;
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("OSM", e.getMessage());
                        addressLocation = null;
                    }
                }, String.valueOf(homeAddress_ET.getText()));
            }
        });

        signIn_BT = view.findViewById(R.id.sign_in_bt);
        signIn_BT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String fullName = fullName_ET.getText().toString().trim();
                String editedFullName = fullName.trim()
                        .replaceAll("\\s+", "."); // handles multiple spaces
                String password = password_ET.getText().toString().trim();
                String phoneNumber = phoneNumber_ET.getText().toString().trim();
                
                String validation = checkValidation(fullName, password, phoneNumber);
                if (!validation.isEmpty()) {
                    Toast.makeText(view.getContext(), validation, Toast.LENGTH_LONG).show();
                    return;
                }

                mAuth.createUserWithEmailAndPassword( editedFullName + "@gmail.com", password)
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                Toast.makeText(getContext(), getString(R.string.sign_up_failed) + ": " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("DataBase", "Signup failed: " + task.getException().getMessage());
                                return;
                            }

                            FirebaseUser user = mAuth.getCurrentUser();
                            String uid = user.getUid(); // now not null

                            // Save extra user info in Realtime Database
                            DatabaseReference database = FirebaseDatabase.getInstance().getReference("users");
                            User newUser = new User(fullName, phoneNumber, addressLocation);

                            database.child(uid).setValue(newUser).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.d("DataBase", "User Created!");
                                        navController.navigate(R.id.action_signInFragment_to_requestsFragments);
                                    } else {
                                        Log.e("DataBase", "DB error: " + task.getException().getMessage());
                                    }
                                }
                            });
                        });
            }
        });

        login_TV = view.findViewById(R.id.login_text);
        login_TV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navController.navigate(R.id.action_signInFragment_to_loginFragment2);
            }
        });

    }
    
    
    private String checkValidation(String fullName, String password, String phoneNumber) {
        // --- Validation ---
        if (fullName.isEmpty()) {
            return getString(R.string.full_name_required);
        }

        if (!fullName.contains(" ")) {
            return getString(R.string.enter_first_last_name);
        }

        if (password.isEmpty()) {
            return getString(R.string.password_required);
        }

        if (phoneNumber.isEmpty()) {
            return getString(R.string.phone_number_required);
        }

        // Basic phone check (digits only, 9–15 digits)
        if (!phoneNumber.matches("\\d{9,15}")) {
            return getString(R.string.invalid_phone_number);
        }

        // Check if the location is right
        if (addressLocation == null) {
            return getString(R.string.invalid_location);
        }
        
        return "";
    }
}
