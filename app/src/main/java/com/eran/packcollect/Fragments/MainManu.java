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
        DrawerLayout drawer = view.findViewById(mode == FragmentMode.MY_PACKAGES ? R.id.my_packages : R.id.drawer_layout_collect);
        MaterialToolbar toolbar = view.findViewById(mode == FragmentMode.MY_PACKAGES ? R.id.top_toolbar : R.id.top_toolbar1);

        ActionBarDrawerToggle toggle =
                new ActionBarDrawerToggle(
                        fragment.requireActivity(),
                        drawer,
                        toolbar,
                        R.string.open,
                        R.string.close);

        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView nav = view.findViewById(R.id.navigation_view);

        nav.setNavigationItemSelectedListener(item -> {

            if (item.getItemId() == R.id.nav_collect && mode != FragmentMode.COLLECT_PACKAGES) {
                navController.navigate(R.id.action_requestsFragments_to_collectPackFragment);
            }
            else if (item.getItemId() == R.id.nav_my_pack && mode != FragmentMode.MY_PACKAGES) {
                navController.navigate(R.id.action_collectPackFragment_to_requestsFragments);
            }
            else if (item.getItemId() == R.id.nav_logout) {
                PackageAlertReceiver.cancelAllNotifications(context); // Cancel all of the user's notifications

                FirebaseAuth.getInstance().signOut();
                if (mode == FragmentMode.MY_PACKAGES) {
                    navController.navigate(R.id.action_requestsFragments_to_loginFragment2);
                } else {
                    navController.navigate(R.id.action_collectPackFragment_to_loginFragment2);
                }
            }

            drawer.closeDrawers();
            return true;
        });
    }
}
