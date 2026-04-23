package com.eran.packcollect.Fragments;

import android.content.Context;
import android.view.View;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;

import com.eran.packcollect.R;
import com.eran.packcollect.Workers.PackageAlertReceiver;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainManu {
    public MainManu(Fragment fragment, View view, Context context, NavController navController, FragmentMode mode) {
        DrawerLayout drawer = view.findViewById(mode == FragmentMode.MY_PACKAGES ? R.id.my_packages :
                (mode == FragmentMode.COLLECT_PACKAGES ? R.id.drawer_layout_collect : R.id.drawer_layout_profile));
        MaterialToolbar toolbar = view.findViewById(R.id.top_toolbar);

        ActionBarDrawerToggle toggle =
                new ActionBarDrawerToggle(
                        fragment.requireActivity(),
                        drawer,
                        toolbar,
                        R.string.nav_open,
                        R.string.nav_close);

        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView nav = view.findViewById(R.id.navigation_view);

        nav.setNavigationItemSelectedListener(item -> {

            if (item.getItemId() == R.id.nav_collect && mode != FragmentMode.COLLECT_PACKAGES) {
                navController.navigate(R.id.action_myPackagesFragments_to_collectPackFragment);
            }
            else if (item.getItemId() == R.id.nav_my_pack && mode != FragmentMode.MY_PACKAGES) {
                navController.navigate(R.id.action_collectPackFragment_to_myPackagesFragment);
            }
            else if (item.getItemId() == R.id.nav_logout) {
                PackageAlertReceiver.cancelAllNotifications(context); // Cancel all of the user's notifications

                FirebaseAuth.getInstance().signOut();
                if (mode == FragmentMode.MY_PACKAGES) {
                    navController.navigate(R.id.action_myPackagesFragment_to_loginFragment);
                } else {
                    navController.navigate(R.id.action_collectPackFragment_to_loginFragment);
                }
            }

            drawer.closeDrawers();
            return true;
        });
    }
}
