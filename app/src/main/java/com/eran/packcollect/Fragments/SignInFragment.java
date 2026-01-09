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
import com.eran.packcollect.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignInFragment extends Fragment {
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();;
    private NavController navController;
    private Button sign_in_bt;
    private TextView login_tv;


    private EditText fullName_et;
    private EditText password_et;
    private EditText phoneNumber_et;
    private EditText homeAddresss_et;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sign_in, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 'view' here is the root view of your fragment layout
        navController = Navigation.findNavController(view);


        fullName_et = view.findViewById(R.id.user_name_et);
        password_et = view.findViewById(R.id.password_et);
        phoneNumber_et = view.findViewById(R.id.phone_et);
        homeAddresss_et = view.findViewById(R.id.address_et);

        sign_in_bt = view.findViewById(R.id.sign_in_bt);
        sign_in_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String fullName = String.valueOf(fullName_et.getText());
                String editedFullName = fullName.replace(" ", ".");
                String password = String.valueOf(password_et.getText());
                String phoneNumber = String.valueOf(phoneNumber_et.getText());
                String homeAddress = String.valueOf(homeAddresss_et.getText());


                mAuth.createUserWithEmailAndPassword( editedFullName + "@gmail.com", password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                String uid = user.getUid(); // now not null

                                // Save extra user info in Realtime Database
                                DatabaseReference database = FirebaseDatabase.getInstance().getReference("users");
                                User newUser = new User(fullName, phoneNumber, homeAddress);

                                database.child(uid).setValue(newUser).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(getContext(), "User created!", Toast.LENGTH_SHORT).show();
                                            Log.d("DataBase", "User Created!");
                                            navController.navigate(R.id.action_signInFragment_to_requestsFragments);
                                        } else {
                                            Toast.makeText(getContext(), "DB error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            Log.e("DataBase", "DB error: " + task.getException().getMessage());
                                        }
                                    }
                                });
                            } else {
                                Toast.makeText(getContext(), "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("DataBase", "Signup failed: " + task.getException().getMessage());
                            }
                        });
            }
        });

        login_tv = view.findViewById(R.id.login_text);
        login_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navController.navigate(R.id.action_signInFragment_to_loginFragment2);
            }
        });

    }
}
