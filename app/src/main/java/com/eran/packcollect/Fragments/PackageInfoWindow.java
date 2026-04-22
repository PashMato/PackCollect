package com.eran.packcollect.Fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.navigation.NavController;

import com.eran.packcollect.R;
import com.eran.packcollect.DataBase.Package;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

public class PackageInfoWindow extends InfoWindow {

    private final NavController navController;
    private Package currentPackage;
    private FragmentMode mode;

    // Pro-Tip: Pass the layout and the navController via constructor
    public PackageInfoWindow(int layoutResId, MapView mapView, NavController navController, FragmentMode mode) {
        super(layoutResId, mapView);
        this.navController = navController;

        View view = mView; // mView is the protected root view inside InfoWindow

        float scale = mapView.getContext().getResources().getDisplayMetrics().density;
        int pixels = (int) (280 * scale + 0.5f);

        // This permanently locks the view size in memory
        view.setMinimumWidth(pixels);

        View card = view.findViewById(R.id.bubble_card);
        if (card != null) {
            card.setMinimumWidth(pixels);
        }

        this.mode = mode == FragmentMode.MAP_FROM_COLLECT_PACKAGES ? FragmentMode.MAP_FROM_COLLECT_PACKAGES : FragmentMode.MAP_FROM_MY_PACKAGES;
    }

    @Override
    public void onOpen(Object item) {
        // 'item' is the Marker that the user just clicked
        Marker marker = (Marker) item;

        // Retrieve our custom Package object from the marker
        currentPackage = (Package) marker.getRelatedObject();
        if (currentPackage == null) return;

        // Bind your views
        TextView title = mView.findViewById(R.id.bubble_title);
        TextView description = mView.findViewById(R.id.bubble_description);
        Button btn = mView.findViewById(R.id.bubble_more_info);

        if (title != null) title.setText(currentPackage.description);
        if (description != null) description.setText(currentPackage.packageAddress.toString());

        // Handle the click
        if (btn != null) {
            btn.setOnClickListener(v -> {
                if (navController != null) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("package", currentPackage);
                    bundle.putSerializable("mode", mode);

                    navController.navigate(R.id.action_mapFragment_to_viewPackageFragment, bundle);
                }
                close(); // Close the window after navigating
            });
        }
    }

    @Override
    public void onClose() {
        // Optional: Clear out data to free memory when closed
        currentPackage = null;
    }
}