package com.eran.packcollect.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class SignUpFragment extends Fragment {
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private NavController navController;
    private Context context;
    private Button signUp_BT;


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
        context = view.getContext();

        ProgressBar loadingBar_PB = view.findViewById(R.id.loading_spinner);
        loadingBar_PB.setVisibility(View.GONE);

        fullName_ET = view.findViewById(R.id.user_name_et);
        password_ET = view.findViewById(R.id.password_et);
        phoneNumber_ET = view.findViewById(R.id.phone_et);
        homeAddress_ET = view.findViewById(R.id.address_et);


        signUp_BT = view.findViewById(R.id.sign_in_bt);
        TextView login_TV = view.findViewById(R.id.login_text);


        fullName_ET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                signUp_BT.setEnabled(isEnabled());
            }
        });
        fullName_ET.setOnFocusChangeListener((view1, b) -> {
            if (b) { // exit if gaining focus
                return;
            }

            String result = fullName_ET.getText().toString();

            if (result.isBlank()) {
                signUp_BT.setEnabled(false);
                Toast.makeText(view1.getContext(), getString(R.string.full_name_required), Toast.LENGTH_LONG).show();
                // Do something like enable a button or show an error
            } else if (!result.contains(" ")) {
                Toast.makeText(view1.getContext(), getString(R.string.enter_first_last_name), Toast.LENGTH_LONG).show();
            } else {
                signUp_BT.setEnabled(isEnabled());
            }
        });

        password_ET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                signUp_BT.setEnabled(isEnabled());
            }
        });
        password_ET.setOnFocusChangeListener((view2, b) -> {
            if (b) { // exit if gaining focus
                return;
            }

            String result = password_ET.getText().toString();

            if (result.length() < 6) {
                signUp_BT.setEnabled(false);
                Toast.makeText(view2.getContext(), "Password has to be at least 6 characters long", Toast.LENGTH_LONG).show();
                // Do something like enable a button or show an error
            } else {
                signUp_BT.setEnabled(isEnabled());
            }
        });

        homeAddress_ET.setOnFocusChangeListener((view3, hasFocus) -> {
            signUp_BT.setEnabled(false);
            if (hasFocus) { // exit the function if the focus "begins"
                addressLocation = null;
                return;
            }

            Address.searchAddress(view3.getContext(), new SearchLocationCallback() {
                @Override
                public void onSuccess(Address location) {
                    Toast.makeText(view3.getContext(), location.address, Toast.LENGTH_SHORT).show();
                    addressLocation = location;
                    signUp_BT.setEnabled(isEnabled());
                }

                @Override
                public void onNoResult(String query) {
                    Toast.makeText(view3.getContext(), getString(R.string.location_not_found) + " '" + query + "'", Toast.LENGTH_LONG).show();
                    addressLocation = null;
                    signUp_BT.setEnabled(false);
                }

                @Override
                public void onError(Exception e) {
                    Log.e("OSM", e.getMessage());
                    addressLocation = null;
                }
            }, String.valueOf(homeAddress_ET.getText()));
        });

        phoneNumber_ET.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                signUp_BT.setEnabled(isEnabled());
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        });
        phoneNumber_ET.setOnFocusChangeListener((view4, b) -> {
            if (b) {return; }

            if (phoneNumber_ET.getText().toString().isBlank()) {
                Toast.makeText(context, getString(R.string.phone_number_required), Toast.LENGTH_LONG).show();
                signUp_BT.setEnabled(false);
            } else if (!phoneNumber_ET.getText().toString().matches("^\\+?\\d{9,15}$")) {
                Toast.makeText(context, getString(R.string.invalid_phone_number), Toast.LENGTH_LONG).show();
                signUp_BT.setEnabled(false);
            } else {
                signUp_BT.setEnabled(isEnabled());
            }
        });
        phoneNumber_ET.setOnEditorActionListener(closeKeyboard);

        signUp_BT.setEnabled(isEnabled());
        signUp_BT.setOnClickListener(view5 -> {
            String fullName = fullName_ET.getText().toString().trim();
            String editedFullName = User.userNameToEmail(fullName);
            String password = password_ET.getText().toString().trim();
            String phoneNumber = phoneNumber_ET.getText().toString().trim();

            String validation = checkValidation(fullName, password, phoneNumber);
            if (!validation.isEmpty()) {
                Toast.makeText(view5.getContext(), validation, Toast.LENGTH_LONG).show();
                return;
            }

            loadingBar_PB.setVisibility(View.VISIBLE);
            signUp_BT.setEnabled(false);

            mAuth.createUserWithEmailAndPassword( editedFullName, password)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Toast.makeText(getContext(), getString(R.string.sign_up_text) + ": " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("DataBase", "Signup failed: " + task.getException().getMessage());
                            return;
                        }

                        // Save extra user info in Realtime Database
                        OnSuccessListener onSuccessListener = o -> {
                            Log.d("DataBase", "User Created!");
                            navController.navigate(R.id.action_signUpFragment_to_myPackagesFragment);
                        };

                        OnFailureListener onFailureListener = e -> {
                            if (task.getException() != null) {
                                Log.e("DataBase", "DB error: " + task.getException().getMessage());
                            } else {
                                Log.e("Database", task.getResult().toString());
                            }
                        };

                        User.createUser(fullName, phoneNumber, addressLocation, onSuccessListener, onFailureListener);
                    });
        });

        login_TV.setOnClickListener(view6 -> navController.navigate(R.id.action_signUpFragment_to_loginFragment));

        signUp_BT.setEnabled(false);
    }
    
    private boolean isEnabled() {
        return !fullName_ET.getText().toString().isBlank() &&
               !password_ET.getText().toString().isBlank() && password_ET.getText().length() >= 6 &&
                addressLocation != null &&
               !phoneNumber_ET.getText().toString().isBlank() && phoneNumber_ET.getText().length() >= 10 &&
                phoneNumber_ET.getText().toString().matches("^\\+?\\d{9,15}$");
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
        if (!phoneNumber.matches("^\\+?\\d{9,15}$")) {
            return getString(R.string.invalid_phone_number);
        }

        // Check if the location is right
        if (addressLocation == null) {
            return getString(R.string.invalid_location);
        }
        
        return "";
    }

    TextView.OnEditorActionListener closeKeyboard = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // 1. Hide the keyboard
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);

                // Clear focus so the cursor disappears
                phoneNumber_ET.clearFocus();
                return true;
            }

            return false;
        }
    };
}
