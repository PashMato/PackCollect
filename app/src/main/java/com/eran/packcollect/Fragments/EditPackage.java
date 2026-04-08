package com.eran.packcollect.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.eran.packcollect.DataBase.Package;
import com.eran.packcollect.Location.Address;
import com.eran.packcollect.Location.SearchLocationCallback;
import com.eran.packcollect.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class EditPackage extends Fragment {
    private NavController navController;

    private EditText address_ET;
    private EditText description_ET;
    private EditText additionalNotes_ET;
    private Button save_BT;
    private ImageButton back_IB;

    private Address addressLocation = null;

    private Package aPackage;
    private FragmentMode fragmentMode;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        if (getArguments() != null) {
            aPackage = (Package) getArguments().getSerializable("package");
            fragmentMode = (FragmentMode) getArguments().getSerializable("mode");
        }

        if (fragmentMode == null) {
            fragmentMode = FragmentMode.MY_PACKAGES;
        }

        return inflater.inflate(R.layout.edit_package, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 'view' here is the root view of your fragment layout
        navController = Navigation.findNavController(view);


        address_ET = view.findViewById(R.id.package_location_et);
        address_ET.setOnFocusChangeListener(new View.OnFocusChangeListener() {
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
                }, String.valueOf(address_ET.getText()));
            }
        });

        description_ET = view.findViewById(R.id.package_description_et);
        additionalNotes_ET = view.findViewById(R.id.additional_details_et);

        if (aPackage != null) { // update the fields from the exiting package data
            addressLocation = aPackage.packageAddress;
            address_ET.setText(addressLocation != null ? addressLocation.toString() : "");
            description_ET.setText(aPackage.description);
            additionalNotes_ET.setText(aPackage.additionalNotes);
        }

        save_BT = view.findViewById(R.id.create_request_bt);
        save_BT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String packDescription = description_ET.getText().toString().trim();
                String packAdditionalNotes = additionalNotes_ET.getText().toString().trim();

                String validation = checkValidation(packDescription, packAdditionalNotes);

                OnSuccessListener onSuccessListener = new OnSuccessListener() {
                    @Override
                    public void onSuccess(Object o) {
                        int action = R.id.action_newRequestFragment_to_viewPackageFragment;

                        if (fragmentMode == FragmentMode.MY_PACKAGES && aPackage == null) {
                            action = R.id.action_newRequestFragment_to_requestsFragments2;
                        }

                        Bundle bundle = new Bundle();
                        bundle.putSerializable("package", aPackage);
                        bundle.putSerializable("mode", fragmentMode);

                        navController.navigate(action, bundle);
                        Toast.makeText(view.getContext(), "Package saved", Toast.LENGTH_SHORT).show();
                    }
                };

                OnFailureListener onFailureListener = new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(view.getContext(), "Failed to save package", Toast.LENGTH_SHORT).show();
                        Log.e("DATABASE", "Package write failed: ", e);
                    }
                };

                if (!validation.isEmpty()) {
                    Toast.makeText(view.getContext(), validation, Toast.LENGTH_LONG);
                }

                if (aPackage != null) { // Update the exiting package
                    aPackage.packageAddress = addressLocation;
                    aPackage.description = packDescription;
                    aPackage.additionalNotes = packAdditionalNotes;

                    aPackage.updateToDataBase(onSuccessListener, onFailureListener);
                } else {
                    Package.savePackageForUser(addressLocation, packDescription, packAdditionalNotes,
                            onSuccessListener, onFailureListener);
                }
            }
        });

        back_IB = view.findViewById(R.id.back_button);
        back_IB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int action = R.id.action_newRequestFragment_to_viewPackageFragment;

                if (fragmentMode == FragmentMode.MY_PACKAGES && aPackage == null) {
                    action = R.id.action_newRequestFragment_to_requestsFragments2;
                }

                Bundle bundle = new Bundle();
                bundle.putSerializable("package", aPackage);
                bundle.putSerializable("mode", fragmentMode);

                navController.navigate(action, bundle);
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
