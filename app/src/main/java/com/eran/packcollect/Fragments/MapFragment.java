package com.eran.packcollect.Fragments;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.eran.packcollect.DataBase.Package;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.eran.packcollect.Location.Address;
import com.eran.packcollect.Location.SearchLocationCallback;
import com.eran.packcollect.R;
import com.eran.packcollect.Workers.LocationTrackingService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MapFragment extends Fragment {

    private NavController navController;

    private MapView map = null;
    private MyLocationNewOverlay locationOverlay;
    private final List<Marker> packageMyPackagesMarkers = new ArrayList<>();
    private final List<Marker> packageCollectMarkers = new ArrayList<>();
    private GpsMyLocationProvider provider; // Keep a reference to the provider

    private FragmentMode fragmentMode;

    private Context context;

    private ImageView refreshFAB;

    private List<Package> myPackages = null;
    private List<Package> collectPackages = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue("PackCollect");
    }



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_map, container, false);

        if (getArguments() != null) {
            fragmentMode = (FragmentMode) getArguments().getSerializable("mode");
        }

        if (fragmentMode == null) {
            fragmentMode = FragmentMode.MY_PACKAGES;
        }

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        context = view.getContext();

        // 'view' here is the root view of your fragment layout
        navController = Navigation.findNavController(view);

        map = view.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        // create a provider with a sane update interval (e.g., 10 seconds and 5 meters)
        provider = new GpsMyLocationProvider(requireContext());
        provider.setLocationUpdateMinTime(10 * 1000); // 10 seconds
        provider.setLocationUpdateMinDistance(5); // 5 meters

        // initialize the overlay using our custom provider
        locationOverlay = new MyLocationNewOverlay(provider, map);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();

        locationOverlay.runOnFirstFix(() -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::updatePackages);
            }
        });

        map.getOverlays().add(locationOverlay);
        map.setBuiltInZoomControls(false);

        // Get the controller
        IMapController mapController = map.getController();

        // set the zoom level (higher number = closer to the ground)
        mapController.setZoom(10.0); // set on zoom out

        // center the map on a specific point
        GeoPoint startPoint = new GeoPoint(31.418f, 35.073f); // focus on israel
        mapController.setCenter(startPoint);

        ImageButton back_IB = view.findViewById(R.id.back_button);
        back_IB.setOnClickListener(view1 -> {
            int action = R.id.action_mapFragment_to_collectPackFragment;

            if (fragmentMode == FragmentMode.MY_PACKAGES) {
                action = R.id.action_mapFragment_to_MyPackagesFragment;
            }

            Bundle bundle = new Bundle();
            bundle.putSerializable("mode", fragmentMode);

            navController.navigate(action, bundle);
        });

        refreshFAB = view.findViewById(R.id.fab_refresh);
        refreshFAB.setOnClickListener(view2 -> updatePackages());

        Drawable drawable = refreshFAB.getDrawable();
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).start();
        }

        FloatingActionButton fab_my_location = view.findViewById(R.id.fab_my_location);
        fab_my_location.setOnClickListener(view3 -> recenterOnUser());

        EditText search_input = view.findViewById(R.id.search_input);

        ImageButton search_button = view.findViewById(R.id.search_button);
        search_button.setOnClickListener(view4 -> {
            // Hide the keyboard
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view4.getWindowToken(), 0);
            }

            search_input.clearFocus();

            Address.searchAddress(context, new SearchLocationCallback() {
                @Override
                public void onSuccess(Address location) {
                    // Get the controller
                    IMapController mapController1 = map.getController();

                    // set the zoom level (higher number = closer to the ground)
                    mapController1.setZoom(15.0);

                    // center the map on a specific point
                    GeoPoint startPoint1 = new GeoPoint(location.lat, location.lon);
                    mapController1.setCenter(startPoint1);
                }

                @Override
                public void onNoResult(String query) {
                    Toast.makeText(context, "Couldn't find location", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(context, "Couldn't find location", Toast.LENGTH_SHORT).show();
                    Log.e("SEARCH_LOCATION", Objects.requireNonNull(e.getMessage()));
                }
            },  search_input.getText().toString());
        });


        View headerCard = view.findViewById(R.id.header_card);

        ViewCompat.setOnApplyWindowInsetsListener(headerCard, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Apply the status bar height as a top margin so it sits below the battery
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.topMargin = insets.top + (int)(6 * getResources().getDisplayMetrics().density); // Inset + 16dp margin
            v.setLayoutParams(mlp);

            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void recenterOnUser() {
        // Use the provider's last known location directly
        Location userLocation = provider.getLastKnownLocation();
        if (userLocation == null) return;

        // Get the controller
        IMapController mapController = map.getController();

        // set the zoom level (higher number = closer to the ground)
        mapController.setZoom(15.0);

        // center the map on a specific point
        GeoPoint startPoint = new GeoPoint(userLocation.getLatitude(), userLocation.getLongitude());
        mapController.setCenter(startPoint);
    }

    private void updatePackages() {
        // Use the provider's last known location directly
        Location userLocation = provider.getLastKnownLocation();
        if (userLocation == null) return;

        Drawable drawable = refreshFAB.getDrawable();
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).start();
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return;
        }

        String ui = user.getUid();
        if (ui.isBlank()) {
            return;
        }

        myPackages = null;
        collectPackages = null;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("packages")
                .orderByChild("ownerUid").equalTo(user.getUid()).getRef();

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Package> userPackages = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Package pkg = snapshot.getValue(Package.class);
                    if (pkg != null) {
                        userPackages.add(pkg);
                    }
                }

                // Now you can send this list to your RecyclerView adapter or Map
                myPackages = userPackages;
                displayPackagesOnMap();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FIREBASE_ERROR", "Query failed to load packages to map: " + databaseError.getMessage());
            }
        });

        LocationTrackingService.checkProximityToPackages(userLocation, packages -> {
            collectPackages = packages;
            displayPackagesOnMap();
        });
    }

    private void displayPackagesOnMap() {
        if (myPackages == null && collectPackages == null) {
            return;
        }

        if (myPackages != null && collectPackages != null) {
            // Stop the Animation only if both of the calls are done
            Drawable drawable = refreshFAB.getDrawable();
            if (drawable instanceof Animatable) {
                ((Animatable) drawable).stop();
            }
        }

        removeMarkers();

        // Add new markers for found packages
        Drawable icon_red = ContextCompat.getDrawable(context, R.drawable.map_pin_red);
        Drawable icon_green = ContextCompat.getDrawable(context, R.drawable.map_pin_green);

        PackageInfoWindow sharedInfoWindow = new PackageInfoWindow(
                R.layout.custom_info_window,
                map,
                navController,
                fragmentMode
        );

        if (myPackages != null && collectPackages != null && !myPackages.isEmpty()) {
            String ownerUid = myPackages.get(0).ownerUid;

            for (Package collectPkg : collectPackages) {
                if (!Objects.equals(collectPkg.ownerUid, ownerUid)) {
                    continue;
                }

                myPackages.remove(collectPkg);
            }
        }

        // if it's null the function got a built it null check
        createMarkers(myPackages, packageMyPackagesMarkers, sharedInfoWindow, icon_red);
        createMarkers(collectPackages, packageCollectMarkers, sharedInfoWindow, icon_green);

        map.invalidate(); // Refresh the map
    }

    private void createMarkers(List<Package> toDraw, List<Marker> markers,
                               PackageInfoWindow sharedInfoWindow, Drawable icon) {
        if (toDraw == null) {
            return;
        }

        for (Package pkg : toDraw) {
            if (pkg == null || pkg.packageAddress == null || map == null) {
                continue;
            }

            Marker marker = new Marker(map);
            marker.setPosition(new GeoPoint(pkg.packageAddress.lat, pkg.packageAddress.lon));
            marker.setIcon(icon);

            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);

            // attach your data directly to the marker
            marker.setRelatedObject(pkg);

            // give it the shared InfoWindow
            marker.setInfoWindow(sharedInfoWindow);

            map.getOverlays().add(marker);
            markers.add(marker);
        }
    }

    private void removeMarkers() {
        // Clear old markers first so they don't stack
        if (myPackages != null) {
            for (Marker m : packageMyPackagesMarkers) {
                map.getOverlays().remove(m);
            }
            packageMyPackagesMarkers.clear();
        }

        // Clear old markers first so they don't stack
        if (collectPackages != null) {
            for (Marker m : packageCollectMarkers) {
                map.getOverlays().remove(m);
            }
            packageCollectMarkers.clear();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        if (locationOverlay != null) {
            locationOverlay.enableMyLocation();
        }
    }

    @Override
    public void onPause() {
        // DO NOT call map.onPause() or disable location if
        // the fragment is just being covered but not destroyed.
        // But for standard fragments, we do:
        super.onPause();
        map.onPause();
        if (locationOverlay != null) {
            locationOverlay.disableMyLocation();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up the provider to stop the GPS hardware entirely
        if (provider != null) {
            provider.stopLocationProvider();
        }
    }
}