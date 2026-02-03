package com.eran.packcollect.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.eran.packcollect.DataBase.Package;
import com.eran.packcollect.DataBase.User;
import com.eran.packcollect.Location.Address;
import com.eran.packcollect.Location.SearchLocationCallback;
import com.eran.packcollect.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class NewRequestFragment extends Fragment {
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private NavController navController;

    private EditText packAddress_et;
    private EditText packDescription_et;
    private EditText packAdditionalDetails_et;
    private Button create_request_bt;

    private Address addressLocation = null;

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


        packAddress_et = view.findViewById(R.id.package_location_et);
        packAddress_et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
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
                        Toast.makeText(view.getContext(), "No location found named '" + query + "'", Toast.LENGTH_LONG).show();
                        addressLocation = null;
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("OSM", e.getMessage());
                        addressLocation = null;
                    }
                }, String.valueOf(packAddress_et.getText()));
            }
        });

        packDescription_et = view.findViewById(R.id.package_description_et);
        packAdditionalDetails_et = view.findViewById(R.id.additional_details_et);

        create_request_bt = view.findViewById(R.id.create_request_bt);
        create_request_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String packDescription = packDescription_et.getText().toString().trim();
                String packAdditionalDetails = packAdditionalDetails_et.getText().toString().trim();

                String validation = checkValidation(packDescription, packAdditionalDetails);
                if (!validation.isEmpty()) {
                    Toast.makeText(view.getContext(), validation, Toast.LENGTH_LONG);
                }

                FirebaseUser user = mAuth.getCurrentUser();
                String uid = user.getUid(); // now not null

                DatabaseReference packagesRef = FirebaseDatabase.getInstance().getReference("packages");

                Package pack = Package.savePackageForUser(addressLocation,
                        packDescription, packAdditionalDetails, new OnSuccessListener() {
                            @Override
                            public void onSuccess(Object o) {
                                navController.navigate(R.id.action_newRequestFragment_to_requestsFragments2);
                                Toast.makeText(view.getContext(), "Package saved", Toast.LENGTH_SHORT).show();
                            }
                        },
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(view.getContext(), "Failed to save package", Toast.LENGTH_SHORT).show();
                                Log.e("DATABASE", "Package write failed: ", e);
                            }
                    });

            }
        });
    }

    private String checkValidation(String packDescription, String packAdditionalDetails) {
       if (addressLocation == null) {
           return "Package Location is invalid";
       }

        if (packDescription.isBlank()) {
            return "Package Description is empty";
        }

        if (packAdditionalDetails.isBlank()) {
            return "Package Additional Details is empty";
        }

        return "";
    }
}
