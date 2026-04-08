package com.eran.packcollect.Fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.eran.packcollect.DataBase.Notification;
import com.eran.packcollect.DataBase.NotificationModes;
import com.eran.packcollect.DataBase.Package;
import com.eran.packcollect.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ViewPackageFragment extends Fragment {
    private NavController navController;
    private Package currentPackage;
    private FragmentMode fragmentMode;
    private String ownerUid = "";
    private String ownerPhoneNumber = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentPackage = (Package) getArguments().getSerializable("package");
            fragmentMode = (FragmentMode) getArguments().getSerializable("mode");
        }

        if (fragmentMode == null) {
            fragmentMode = FragmentMode.MY_PACKAGES;
        }

        return inflater.inflate(R.layout.view_package, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        // 'view' here is the root view of your fragment layout
        navController = Navigation.findNavController(view);

        // Initialize Views
        ImageButton backBtn = view.findViewById(R.id.back_button);
        TextView tvOwner = view.findViewById(R.id.display_owner);
        TextView tvLocation = view.findViewById(R.id.display_location);
        TextView tvDescription = view.findViewById(R.id.display_description);
        TextView tvAdditional = view.findViewById(R.id.display_additional);
        FloatingActionButton editFab = view.findViewById(R.id.edit_package_fab);
        ImageButton dailButton = view.findViewById(R.id.dial_button);

        // Populate Data
        if (currentPackage != null) {
            // Check if address exists to avoid NullPointerException
            if (currentPackage.packageAddress != null) {
                // Assuming Address class has a toString or fields like 'city'
                tvLocation.setText(currentPackage.packageAddress.address);
            }

            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentPackage.ownerUid);

            userRef.get().addOnCompleteListener(task -> {
                String name = "null"; // In case the user isn't log in somehow
                if (task.isSuccessful() && task.getResult().exists()) {
                    name = task.getResult().child("fullName").getValue(String.class);
                    ownerUid = currentPackage.ownerUid;
                    ownerPhoneNumber = task.getResult().child("phoneNumber").getValue(String.class);
                }

                tvOwner.setText(name);
            });

            tvDescription.setText(currentPackage.description);
            tvAdditional.setText(currentPackage.additionalNotes);
        }

        // Back Button Logic
        backBtn.setOnClickListener(v -> {
            int action = R.id.action_viewPackageFragment_to_collectPackFragment;

            if (fragmentMode == FragmentMode.MY_PACKAGES) {
                action = R.id.action_viewPackageFragment_to_requestsFragments;
            }

            navController.navigate(action);
        });

        // Edit Button Logic
        editFab.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("package", currentPackage); // Passing the data
            bundle.putSerializable("mode", fragmentMode); // passing the mode

            navController.navigate(R.id.action_viewPackageFragment_to_newRequestFragment, bundle);
        });

        // Dial Button Logic
        dailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Notification.sendNotification(ownerUid, currentPackage.packageId, NotificationModes.APPROVE_PICK_UP);

                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + ownerPhoneNumber));

                // Standard check to make sure the user has a phone app installed
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });

        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getUid().equals(currentPackage.ownerUid)) { // if the user is the owner
            dailButton.setVisibility(View.GONE);
            editFab.setVisibility(View.VISIBLE);
        } else {
            dailButton.setVisibility(View.VISIBLE);
            editFab.setVisibility(View.GONE);
        }
    }

    private void fetchOwnerEmail(String uid, TextView targetTextView) {

    }
}