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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.eran.packcollect.DataBase.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import com.eran.packcollect.R;

public class LoginFragment extends Fragment {
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private NavController navController;
    private Context context;
    private Button login_BT;
    private TextView signUp_TV;


    private EditText fullName_ET;
    private EditText password_ET;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.login, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 'view' here is the root view of your fragment layout
        navController = Navigation.findNavController(view);

        context = view.getContext();

        fullName_ET = view.findViewById(R.id.user_name_et);
        password_ET = view.findViewById(R.id.password_et);

        fullName_ET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                login_BT.setEnabled(!s.toString().isEmpty() &&
                        !password_ET.toString().isBlank() && password_ET.toString().length() >= 6);
            }
        });
        fullName_ET.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) { // exit if gaining focus
                    return;
                }

                String result = fullName_ET.getText().toString();

                if (result.isBlank()) {
                    login_BT.setEnabled(false);
                    Toast.makeText(view.getContext(), getString(R.string.full_name_required), Toast.LENGTH_LONG).show();
                    // Do something like enable a button or show an error
                } else if (!result.contains(" ")) {
                    Toast.makeText(view.getContext(), getString(R.string.enter_first_last_name), Toast.LENGTH_LONG).show();
                } else {
                    login_BT.setEnabled(password_ET.getText().length() >= 6);
                }
            }
        });
        fullName_ET.setOnEditorActionListener(closeKeyboard);

        password_ET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                login_BT.setEnabled(!fullName_ET.getText().isEmpty() && !s.toString().isBlank() && s.toString().length() >= 6);
            }
        });
        password_ET.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) { // exit if gaining focus
                    return;
                }

                String result = password_ET.getText().toString();

                if (result.length() < 6) {
                    login_BT.setEnabled(false);
                    Toast.makeText(view.getContext(), "Password has to be at least 6 characters long", Toast.LENGTH_LONG).show();
                    // Do something like enable a button or show an error
                } else {
                    login_BT.setEnabled(!fullName_ET.getText().isEmpty());
                }
            }
        });
        password_ET.setOnEditorActionListener(closeKeyboard);

        login_BT = view.findViewById(R.id.login_bt);
        login_BT.setOnClickListener(view1 -> {
            String fullName = fullName_ET.getText().toString().trim();
            String editedFullName = User.userNameToEmail(fullName);
            String password = password_ET.getText().toString().trim();

            String validation = checkValidation(fullName, password);
            if (!validation.isEmpty()) {
                login_BT.setEnabled(false);
                Toast.makeText(view1.getContext(), validation, Toast.LENGTH_LONG).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(editedFullName, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (!task.isSuccessful()) {
                        Log.d("DataBase", task.getException().getMessage());
                        Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(getContext(), "User login!", Toast.LENGTH_SHORT).show();
                    navController.navigate(R.id.action_loginFragment_to_myPackagesFragment);
                }
            });
        });

        if (mAuth.getCurrentUser() != null) {
            Toast.makeText(getContext(), "User login!", Toast.LENGTH_SHORT).show();
            navController.navigate(R.id.action_loginFragment_to_myPackagesFragment);
        }
        // TODO: add loading when log in is pressed
        signUp_TV = view.findViewById(R.id.sign_in_text);
        signUp_TV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navController.navigate(R.id.action_loginFragment_to_signUpFragment);
            }
        });
    }

    private String checkValidation(String fullName, String password) {
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
                    fullName_ET.clearFocus();
                    password_ET.clearFocus();
                    return true;
                }

                return false;
            }
    };
}
