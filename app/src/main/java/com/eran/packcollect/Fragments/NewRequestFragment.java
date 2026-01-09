package com.eran.packcollect.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.eran.packcollect.R;

public class NewRequestFragment extends Fragment {
    private NavController navController;
    private Button create_request_bt;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.new_request, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 'view' here is the root view of your fragment layout
        navController = Navigation.findNavController(view);


        create_request_bt = view.findViewById(R.id.create_request_bt);
        create_request_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navController.navigate(R.id.action_newRequestFragment_to_requestsFragments2);
            }
        });
    }
}
