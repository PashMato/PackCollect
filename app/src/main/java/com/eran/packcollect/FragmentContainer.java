package com.eran.packcollect;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class FragmentContainer extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_container);
        mAuth = FirebaseAuth.getInstance(); // Initialize Firebase Auth
    }
}