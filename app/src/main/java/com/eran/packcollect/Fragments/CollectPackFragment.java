package com.eran.packcollect.Fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eran.packcollect.R;
import com.eran.packcollect.Table.PackagesAdapter;
import com.eran.packcollect.Workers.LocationTrackingService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class CollectPackFragment extends Fragment {
    private NavController navController;
    PackagesAdapter adapter;
    RecyclerView recyclerView;
    LinearLayout emptyState_LL;
    ProgressBar loadingBar_PB;
    FloatingActionButton map_FAB;

    Context context;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.collect_pack, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        context = view.getContext();

        // make sure we don't have and notifications from the previous user
        askPermission();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // 'view' here is the root view of your fragment layout
        navController = Navigation.findNavController(view);
        new MainManu(this, view, context, navController, FragmentMode.COLLECT_PACKAGES);

        recyclerView = view.findViewById(R.id.collect_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        adapter = new PackagesAdapter(new ArrayList<>(), pkg -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("package", pkg); // Passing the data
            bundle.putSerializable("mode", FragmentMode.COLLECT_PACKAGES);

            navController.navigate(R.id.action_collectPackFragment_to_viewPackageFragment, bundle);
        });
        recyclerView.setAdapter(adapter);

        emptyState_LL = view.findViewById(R.id.emptyStateLayout);
        loadingBar_PB = view.findViewById(R.id.loading_spinner);

        map_FAB = view.findViewById(R.id.map_view_fab);
        map_FAB.setOnClickListener(view1 -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("mode", FragmentMode.COLLECT_PACKAGES);
            navController.navigate(R.id.action_collectPackFragment_to_mapFragment, bundle);
        });

        checkLocationPermissionAndFetch();
    }


    @Override
    public void onResume() {
        super.onResume();
        // This is called every time the fragment "gets retention" or focus
        checkLocationPermissionAndFetch();
    }



    private final CancellationTokenSource cancellationTokenSource = new CancellationTokenSource(); // CancellationToken to allow the system to cancel the request if needed

    private void checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission if not granted
            askPermission();
            return;
        }


        // Permission is granted, get the location
        emptyState_LL.setVisibility(View.GONE);
        loadingBar_PB.setVisibility(View.VISIBLE);
        
        OnSuccessListener<Location> successListener = location -> {
            if (location != null && adapter != null) {
                // Now you have a fresh location!
                LocationTrackingService.checkProximityToPackages(location, packages -> {
                    if (packages == null || packages.isEmpty()) { // if the list is null update to an empty list
                        packages = new ArrayList<>();
                        emptyState_LL.setVisibility(View.VISIBLE);
                        loadingBar_PB.setVisibility(View.GONE);
                    }

                    adapter.Packages = packages;
                    adapter.notifyDataSetChanged();
                    loadingBar_PB.setVisibility(View.GONE);
                });
            } else {
                emptyState_LL.setVisibility(View.VISIBLE);
                loadingBar_PB.setVisibility(View.GONE);
                Toast.makeText(getContext(), getString(R.string.cannot_get_location), Toast.LENGTH_SHORT).show();
            }
        };

        OnFailureListener failureListener = e -> {
            emptyState_LL.setVisibility(View.VISIBLE);
            loadingBar_PB.setVisibility(View.GONE);
            Log.e("Location", "Error getting fresh location: " + e.getMessage());
        };


        // check if the phone has any previous location
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null || (System.currentTimeMillis() - location.getTime() > 1000 * 60))
            { // if don't have any previous location from the last minute
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // Request permission if not granted
                    askPermission();
                    return;
                }

                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.getToken())
                        .addOnSuccessListener(successListener).addOnFailureListener(failureListener);
            } else {
                successListener.onSuccess(location);
            }
        }).addOnFailureListener(e -> {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Request permission if not granted
                askPermission();
                return;
            }

            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.getToken())
                    .addOnSuccessListener(successListener).addOnFailureListener(failureListener);
        });

    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            });
    private void askPermission() {
        final String[] permissions = new String[] {
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACCESS_FINE_LOCATION,
        };

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED) {
                continue;
            }

            requestPermissionLauncher.launch(permission);
        }
    }
}
