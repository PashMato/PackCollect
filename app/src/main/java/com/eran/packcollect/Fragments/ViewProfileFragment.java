package com.eran.packcollect.Fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.eran.packcollect.DataBase.NotificationFB;
import com.eran.packcollect.DataBase.NotificationModes;
import com.eran.packcollect.DataBase.Package;
import com.eran.packcollect.DataBase.User;
import com.eran.packcollect.R;
import com.eran.packcollect.Workers.NotificationActionReceiver;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ViewProfileFragment extends Fragment {
    private NavController navController;
    private User currentUser;
    private FragmentMode fragmentMode;
    private String ownerUid = "";
    private String ownerPhoneNumber = "";
    private Context context;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        return inflater.inflate(R.layout.fragment_view_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        ownerUid = mAuth.getUid();

        // 'view' here is the root view of your fragment layout
        navController = Navigation.findNavController(view);

        context = view.getContext();

        // Initialize Views
        TextView tvUser = view.findViewById(R.id.full_name_tv);
        TextView tvLocation = view.findViewById(R.id.home_address_tv);
        TextView tvPhoneNumber = view.findViewById(R.id.phone_number_tv);
        FloatingActionButton editFab = view.findViewById(R.id.edit_package_fab);

        new MainManu(this, view, context, navController, FragmentMode.VIEW_PROFILE);

        // Loading the data
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(ownerUid);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                currentUser = task.getResult().getValue(User.class);
                if (currentUser == null) {
                    return;
                }

                tvUser.setText(currentUser.fullName);
                tvLocation.setText(currentUser.homeAddress.toString());
                tvPhoneNumber.setText(currentUser.phoneNumber);
            }
        });

        // Edit Button Logic
        editFab.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("user", currentUser); // Passing the data

            navController.navigate(R.id.action_viewProfileFragment_to_editProfile, bundle);
        });
    }
}