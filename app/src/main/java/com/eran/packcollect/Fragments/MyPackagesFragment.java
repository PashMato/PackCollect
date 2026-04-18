package com.eran.packcollect.Fragments;

import android.Manifest;
import android.content.pm.PackageManager;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eran.packcollect.DataBase.Package;
import com.eran.packcollect.R;
import com.eran.packcollect.Table.OnItemClick;
import com.eran.packcollect.Table.PackagesAdapter;
import com.eran.packcollect.Workers.IncomingNotificationService;
import com.eran.packcollect.Workers.LocationTrackingService;
import com.eran.packcollect.Workers.PackageAlertReceiver;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MyPackagesFragment extends Fragment {
    private NavController navController;
    private FloatingActionButton newPackage_FAB;
    private FloatingActionButton map_FAB;

    PackagesAdapter adapter;
    RecyclerView recyclerView;

    LinearLayout emptyState_LL;
    ProgressBar loadingBar_PB;

    Context context;

    ItemTouchHelper.SimpleCallback swipeHandler = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            // Return false because we are not implementing drag-and-drop
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            // get the position of the swiped item
            if (adapter == null) {
                return;
            }

            int position = viewHolder.getAdapterPosition();
            Package pack = adapter.getPackagesAt(position);

            // remove the package from the data base
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("packages").child(pack.packageId);

            ref.removeValue().addOnSuccessListener(aVoid -> {
                // remove the package on the ui side
                adapter.notifyItemRemoved(position);
                adapter.Packages.remove(position);
            }).addOnFailureListener(e -> {
                Log.e("FireBase", "Couldn't delete package: " + e.getMessage());
                Toast.makeText(context, getString(R.string.cannot_delete_package), Toast.LENGTH_SHORT).show();
            });

        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.my_packages, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        context = view.getContext();

        // make sure we don't have and notifications from the previous user
        PackageAlertReceiver.cancelAllNotifications(context);

        // loading UI
        emptyState_LL = view.findViewById(R.id.emptyStateLayout);
        loadingBar_PB = view.findViewById(R.id.loading_spinner);

        // 'view' here is the root view of your fragment layout
        navController = Navigation.findNavController(view);

        recyclerView = view.findViewById(R.id.packages_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeHandler);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        adapter = new PackagesAdapter(new ArrayList<Package>(), new OnItemClick() {
            @Override
            public void OnClick(Package pkg) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("package", pkg); // Passing the data

                navController.navigate(R.id.action_requestsFragments_to_viewPackageFragment, bundle);
            }
        });

        recyclerView.setAdapter(adapter);

        new MainManu(this, view, context, navController, FragmentMode.MY_PACKAGES);

        newPackage_FAB = view.findViewById(R.id.add_request_fab);
        newPackage_FAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navController.navigate(R.id.action_requestsFragments_to_newRequestFragment);
            }
        });

        map_FAB = view.findViewById(R.id.map_view_fab);
        map_FAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("mode", FragmentMode.MY_PACKAGES);
                navController.navigate(R.id.action_MyPackagesFragment_to_mapFragment);
            }
        });

        askPermission(new PermissionCallback() {
            @Override
            public void onSuccess() {
                updateFromDatabase();

                LocationTrackingService.start(context);
                IncomingNotificationService.start(context);
            }

            @Override
            public void onFailure() {
                Toast.makeText(context, getString(R.string.required_permissions), Toast.LENGTH_LONG).show();
            }
        });
    }


    private void updateFromDatabase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            // not logged in
            return;
        }

        List<Package> packageList = new ArrayList<>();
        String uid = user.getUid();

        DatabaseReference packagesRef = FirebaseDatabase.getInstance().getReference("packages");
        Query query = packagesRef.orderByChild("ownerUid").equalTo(uid);

        emptyState_LL.setVisibility(View.GONE);
        loadingBar_PB.setVisibility(View.VISIBLE);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long timeNow = System.currentTimeMillis();

                packageList.clear();

                for (DataSnapshot pkgSnapshot : snapshot.getChildren()) {
                    Package pkg = pkgSnapshot.getValue(Package.class);

                    if (pkg == null) {
                        continue;
                    }

                    if (pkg.expiresAt <= timeNow) {
                        Log.i("Firebase", getString(R.string.delete_package) + " `" + pkg.additionalNotes + "`");
                        pkgSnapshot.getRef().removeValue();
                        continue;
                    }
                        pkg.packageId = pkgSnapshot.getKey();
                        packageList.add(pkg);
                }

                PackageAlertReceiver.cancelAllNotifications(context);

                // create notifications for all of the packages
                for (Package pack : packageList) {
                    String address = null;
                    if (pack.packageAddress != null) {
                        address = pack.packageAddress.address;
                    }
                    PackageAlertReceiver.createNotification(context, address, pack.expiresAt);
                }

                adapter.Packages = packageList;
                adapter.notifyDataSetChanged();

                emptyState_LL.setVisibility(packageList.isEmpty() ? View.VISIBLE : View.GONE);
                loadingBar_PB.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", error.getMessage());
            }
        });
    }

    // Define a variable to hold your listeners temporarily
    private PermissionCallback permissionCallback;
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                // Check if all requested permissions were granted
                if (result.isEmpty()) {
                    // if calling this function twice before the user answered the result will be empty
                    return;
                }

                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }

                if (permissionCallback != null) {
                    if (allGranted) {
                        permissionCallback.onSuccess();
                    } else {
                        permissionCallback.onFailure();
                    }

                    permissionCallback = null;
                }
            });

    private void askPermission(@NonNull PermissionCallback callback) {
        /// if calling this function twice before the user answered the result will be empty
        this.permissionCallback = callback;

        String[] permissions = {
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        List<String> permissionsToRequest = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(p);
            }
        }

        if (permissionsToRequest.isEmpty()) { // Everything is already granted, call the callback immediately
            callback.onSuccess();
        } else {
            // Launch the system dialog for all missing permissions at once
            requestPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }
}
