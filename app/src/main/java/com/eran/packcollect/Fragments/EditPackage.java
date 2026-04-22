package com.eran.packcollect.Fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
        description_ET = view.findViewById(R.id.package_description_et);
        additionalNotes_ET = view.findViewById(R.id.additional_details_et);
        save_BT = view.findViewById(R.id.create_request_bt);
        back_IB = view.findViewById(R.id.back_button);

        address_ET.setOnFocusChangeListener((view1, hasFocus) -> {
            save_BT.setEnabled(false);

            if (hasFocus) { // exit the function if the focus "begins"
                addressLocation = null;
                return;
            }

            Address.searchAddress(view1.getContext(), new SearchLocationCallback() {
                @Override
                public void onSuccess(Address location) {
                    Toast.makeText(view1.getContext(), location.address, Toast.LENGTH_SHORT).show();
                    save_BT.setEnabled(!description_ET.getText().isEmpty());
                    addressLocation = location;
                }

                @Override
                public void onNoResult(String query) {
                    Toast.makeText(view1.getContext(), getString(R.string.location_not_found) + " '" + query + "' ", Toast.LENGTH_LONG).show();
                    save_BT.setEnabled(true);
                    addressLocation = null;
                }

                @Override
                public void onError(Exception e) {
                    Log.e("OSM", e.getMessage());
                    addressLocation = null;
                }
            }, String.valueOf(address_ET.getText()));
        });

        description_ET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String result = s.toString();

                save_BT.setEnabled(addressLocation != null && !result.isBlank());
            }
        });

        if (aPackage != null) { // update the fields from the exiting package data
            addressLocation = aPackage.packageAddress;
            address_ET.setText(addressLocation != null ? addressLocation.toString() : "");
            description_ET.setText(aPackage.description);
            additionalNotes_ET.setText(aPackage.additionalNotes);
        }

        save_BT.setOnClickListener(view2 -> {
            String packDescription = description_ET.getText().toString().trim();
            String packAdditionalNotes = additionalNotes_ET.getText().toString().trim();

            String validation = checkValidation(packDescription);

            OnSuccessListener onSuccessListener = o -> {
                exitToDestination();
                Toast.makeText(view2.getContext(), getString(R.string.package_saved), Toast.LENGTH_SHORT).show();
            };

            OnFailureListener onFailureListener = e -> {
                Toast.makeText(view2.getContext(), getString(R.string.package_save_failed), Toast.LENGTH_SHORT).show();
                Log.e("DATABASE", "Package write failed: ", e);
            };

            if (!validation.isBlank()) {
                Toast.makeText(view2.getContext(), validation, Toast.LENGTH_LONG).show();
                save_BT.setEnabled(false);
                return;
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
        });

        back_IB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int action = R.id.action_editPackageFragment_to_viewPackageFragment;

                if (fragmentMode == FragmentMode.MY_PACKAGES && aPackage == null) {
                    action = R.id.action_editPackageFragment_to_myPackagesFragment;
                }

                Bundle bundle = new Bundle();
                bundle.putSerializable("package", aPackage);
                bundle.putSerializable("mode", fragmentMode);

                navController.navigate(action, bundle);
            }
        });
    }

    private String checkValidation(String packDescription) {
       if (addressLocation == null) {
           return getString(R.string.package_location_invalid);
       }

        if (packDescription.isBlank()) {
            return getString(R.string.package_description_empty);
        }

        return "";
    }

    private void exitToDestination() {
        int action = R.id.action_editPackageFragment_to_viewPackageFragment;
        Bundle bundle = new Bundle();
        bundle.putSerializable("mode", fragmentMode);

        if (fragmentMode == FragmentMode.MY_PACKAGES) {
            action = R.id.action_editPackageFragment_to_myPackagesFragment;
        }

        navController.navigate(action, bundle);
    }
}
