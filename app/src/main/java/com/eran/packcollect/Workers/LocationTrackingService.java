package com.eran.packcollect.Workers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.eran.packcollect.FragmentContainer; // Ensure this points to your actual Main Activity
import com.eran.packcollect.R;
import com.eran.packcollect.DataBase.NotificationFB;
import com.eran.packcollect.DataBase.Package;
import com.eran.packcollect.Location.Address;
import com.google.android.gms.location.*;
        import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

        import java.util.ArrayList;
import java.util.List;

public class LocationTrackingService extends Service {

    // IDs for different notification types
    private static final int SERVICE_NOTIFICATION_ID = 1;        // Keeps service alive
    private static final int PACKAGES_FOUND_NOTIFICATION_ID = 2; // Popup for packages

    private static final String CHANNEL_ID = "LocationServiceChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Update every 5 minutes (300,000ms) for battery efficiency
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000 * 60 * 5)
                .setMinUpdateIntervalMillis(60 * 1000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    checkProximityToPackages(location, packages -> {
                        if (packages != null && !packages.isEmpty()) {
                            // Only 1 notification for all found packages
                            sendPackageFoundNotification(packages.size());
                        } else {
                            // Remove the popup if no packages are nearby anymore
                            cancelPackageNotification();
                        }
                    });
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        // start the persistent "I am running" notification
        Notification persistentNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.package_tracker_active))
                .setContentText(getString(R.string.monitoring_packages))
                .setSmallIcon(R.drawable.ic_location_service)
                .setPriority(NotificationCompat.PRIORITY_LOW) // Quiet
                .setOngoing(true)
                .build();

        startForeground(SERVICE_NOTIFICATION_ID, persistentNotification);

        startLocationUpdates();
        return START_STICKY;
    }

    private void sendPackageFoundNotification(int count) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Intent to open the app when clicked
        Intent intent = new Intent(this, FragmentContainer.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String text = getString(R.string.there_are) + count + getString(R.string.packages_nearby);

        Notification popup = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_package)
                .setContentTitle(getString(R.string.packages_found))
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Pops up on screen
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)     // Removes itself when clicked
                .setOnlyAlertOnce(true)  // Don't keep buzzing every 5 seconds
                .build();

        manager.notify(PACKAGES_FOUND_NOTIFICATION_ID, popup);
    }

    private void cancelPackageNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(PACKAGES_FOUND_NOTIFICATION_ID);
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID, getString(R.string.location_service), NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(serviceChannel);
    }

    private void startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } catch (SecurityException e) {
            Log.e("Service", "Location permission missing: " + e.getMessage());
        }
    }

    // --- Static Firebase Logic ---

    public interface OnPackagesFoundListener {
        void onReceived(List<Package> packages);
    }

    public static void checkProximityToPackages(Location userLocation, OnPackagesFoundListener listener) {
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) return;


        DatabaseReference packagesRef = FirebaseDatabase.getInstance().getReference("packages");
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        // reading the users location for later
        usersRef.child(myUid).child("homeAddress").get().addOnCompleteListener(myTask -> {
            if (!myTask.isSuccessful() || !myTask.getResult().exists()) {
                return;
            }

            Address myHome = myTask.getResult().getValue(Address.class);
            if (myHome == null) {
                Log.e("Firebase", "couldn't get the user's home address");
                return;
            }

            final int[] processed = {0}; // to be able to count all of the answers from the data base

            // read the all of the packages
            packagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<Package> matches = new ArrayList<>();
                    long total = snapshot.getChildrenCount();
                    if (total == 0) { listener.onReceived(matches); return; }

                    // for over all the packages
                    for (DataSnapshot pkgSnapshot : snapshot.getChildren()) {
                        Package pkg = pkgSnapshot.getValue(Package.class);

                        if (pkg == null || pkg.packageAddress == null) {
                            checkEnd(processed, (int) total, matches, listener);
                            continue;
                        }

                        // read check the distance between the user and the package
                        float[] results = new float[1];
                        Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                                pkg.packageAddress.lat, pkg.packageAddress.lon, results);

                        if (results[0] > 1_500) { // If within 1500 meters
                            checkEnd(processed, (int)total, matches, listener);
                            continue;
                        }

                        // Get the OWNER'S home address to make sure the user an the package's owner live near enough
                        usersRef.child(pkg.ownerUid).child("homeAddress").get().addOnCompleteListener(ownerTask -> {
                            if (!ownerTask.isSuccessful() || !ownerTask.getResult().exists()) {
                                checkEnd(processed, (int)total, matches, listener);
                                return;
                            }

                            Address ownerHome = ownerTask.getResult().getValue(Address.class);

                            // Compare My Home to Owner Home
                            float[] result = new float[] {Float.MAX_VALUE};

                            if (ownerHome != null) {
                                Location.distanceBetween(myHome.lat, myHome.lon,
                                        ownerHome.lat, ownerHome.lon, result);
                            }

                            if (result[0] < 1000) { // 1km
                                matches.add(pkg);
                            }
                            checkEnd(processed, (int)total, matches, listener);
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                }
            });
        });
    }

    private static void checkEnd(int[] count, int total, List<Package> list, OnPackagesFoundListener listener) {
        count[0]++;
        if (count[0] == total) {
            listener.onReceived(list);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, LocationTrackingService.class);
        context.startForegroundService(intent);
    }
}
