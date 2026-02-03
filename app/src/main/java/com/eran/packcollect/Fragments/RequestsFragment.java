package com.eran.packcollect.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eran.packcollect.DataBase.Package;
import com.eran.packcollect.R;
import com.eran.packcollect.Table.PackagesAdapter;
import com.eran.packcollect.Workers.DeleteExpiredPackagesWorker;
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

public class RequestsFragment extends Fragment {
    private NavController navController;
    private FloatingActionButton new_request_fab;

    PackagesAdapter adapter;
    RecyclerView recyclerView;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.requests, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DeleteExpiredPackagesWorker.scheduleDailyDelete(view.getContext());

        // 'view' here is the root view of your fragment layout
        navController = Navigation.findNavController(view);

        recyclerView = view.findViewById(R.id.requests_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        adapter = new PackagesAdapter(new ArrayList<Package>(), view.getContext());
        recyclerView.setAdapter(adapter);
        updateRecyclerView(view.getContext());


        new_request_fab = view.findViewById(R.id.add_request_fab);
        new_request_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navController.navigate(R.id.action_requestsFragments_to_newRequestFragment);
            }
        });
    }


    private void updateRecyclerView(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            // not logged in
            return;
        }

        List<Package> packageList = new ArrayList<>();
        String uid = user.getUid();

        DatabaseReference packagesRef = FirebaseDatabase.getInstance().getReference("packages");
        Query query = packagesRef.orderByChild("ownerUid").equalTo(uid);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                packageList.clear();

                for (DataSnapshot pkgSnapshot : snapshot.getChildren()) {
                    Package pkg = pkgSnapshot.getValue(Package.class);

                    if (pkg != null) {
                        pkg.packageId = pkgSnapshot.getKey(); // important!
                        packageList.add(pkg);
                    }
                }


                adapter.Packages = packageList;
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", error.getMessage());
            }
        });
    }
}
