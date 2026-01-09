package com.eran.packcollect.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.eran.packcollect.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class RequestsFragment extends Fragment {
    private NavController navController;
    private FloatingActionButton new_request_fab;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.requests, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 'view' here is the root view of your fragment layout
        navController = Navigation.findNavController(view);


        new_request_fab = view.findViewById(R.id.add_request_fab);
        new_request_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navController.navigate(R.id.action_requestsFragments_to_newRequestFragment);
            }
        });
    }
}
