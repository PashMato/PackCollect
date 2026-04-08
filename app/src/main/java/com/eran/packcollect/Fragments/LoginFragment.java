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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import com.eran.packcollect.R;

public class LoginFragment extends Fragment {
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private NavController navController;
    private Button login_BT;
    private TextView signIn_TV;


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

        fullName_ET = view.findViewById(R.id.user_name_et);
        password_ET = view.findViewById(R.id.password_et);

        login_BT = view.findViewById(R.id.login_bt);
        login_BT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String fullName = fullName_ET.getText().toString().trim();
                String editedFullName = fullName.trim()
                        .replaceAll("\\s+", "."); // handles multiple spaces
                String password = password_ET.getText().toString().trim();

                String validation = checkValidation(fullName, password);
                if (!validation.isEmpty()) {
                    Toast.makeText(view.getContext(), validation, Toast.LENGTH_LONG).show();
                    return;
                }

                mAuth.signInWithEmailAndPassword(editedFullName + "@gmail.com", password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            Log.d("DataBase", task.getException().getMessage());
                            Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Toast.makeText(getContext(), "User login!", Toast.LENGTH_SHORT).show();
                        navController.navigate(R.id.action_loginFragment2_to_requestsFragments);
                    }
                });
            }
        });

        if (mAuth.getCurrentUser() != null) {
            Toast.makeText(getContext(), "User login!", Toast.LENGTH_SHORT).show();
            navController.navigate(R.id.action_loginFragment2_to_requestsFragments);
        }

        signIn_TV = view.findViewById(R.id.sign_in_text);
        signIn_TV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navController.navigate(R.id.action_loginFragment2_to_signInFragment);
            }
        });

    }

    private String checkValidation(String fullName, String password) {
        // --- Validation ---
        if (fullName.isEmpty()) {
            return "Full name is required";
        }

        if (!fullName.contains(" ")) {
            return "Please enter first and last name";
        }

        if (password.isEmpty()) {
            return "Password is required";
        }

        return "";
    }
}
