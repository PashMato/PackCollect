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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.eran.packcollect.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginFragment extends Fragment {
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();;
    private NavController navController;
    private Button login_bt;
    private TextView sign_in_tv;


    private EditText fullName_et;
    private EditText password_et;

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

        fullName_et = view.findViewById(R.id.user_name_et);
        password_et = view.findViewById(R.id.password_et);

        login_bt = view.findViewById(R.id.login_bt);
        login_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String fullName = fullName_et.getText().toString().trim();
                String editedFullName = fullName.trim()
                        .replaceAll("\\s+", "."); // handles multiple spaces
                String password = password_et.getText().toString().trim();

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

        sign_in_tv = view.findViewById(R.id.sign_in_text);
        sign_in_tv.setOnClickListener(new View.OnClickListener() {
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
