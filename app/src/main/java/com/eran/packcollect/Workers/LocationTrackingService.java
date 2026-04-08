package com.eran.packcollect.Workers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import com.eran.packcollect.DataBase.Package;
import com.eran.packcollect.Location.Address;

import java.util.ArrayList;
import java.util.List;

public class LocationTrackingService extends Service {
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private static final String CHANNEL_ID = "LocationServiceChannel";


    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Setup location request (High Accuracy)
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000 * 5) // 5 minutes TODO: change back to 5 min
                .setMinUpdateIntervalMillis(1000) // max interval time 2 minutes
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) { // Android batches locations to save battery
                    checkProximityToPackages(location, new OnPackagesFoundListener() {
                        @Override
                        public void onReceived(List<Package> packages) {
                            if (packages == null) {
                                return;
                            }

                            for (Package pkg : packages) {
                                sendArrivalNotification(pkg.additionalNotes);
                            }
                        }
                    });
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Package Tracker Running")
                .setContentText("Monitoring location for nearby packages...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();

        startForeground(1, notification);
        startLocationUpdates();

        return START_STICKY;
    }

    private void startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } catch (SecurityException e) {
            Log.e("Service", "Location permission missing");
        }
    }

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
            packagesRef.orderByChild("ownerUid").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<Package> matches = new ArrayList<>();
                    long total = snapshot.getChildrenCount();
                    if (total == 0) { listener.onReceived(matches); return; }

                    // for over all the packages
                    for (DataSnapshot pkgSnapshot : snapshot.getChildren()) {
                        Package pkg = pkgSnapshot.getValue(Package.class);

                        if (pkg == null || pkg.packageAddress == null) {
                            checkEnd(processed, (int)total, matches, listener);
                            continue;
                        }

                        // read check the distance between the user and the package
                        float[] results = new float[1];
                        Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                                pkg.packageAddress.lat, pkg.packageAddress.lon, results);

                        if (results[0] > 150000) { // If within 1500 meters TODO: bring back to 1500m
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

    private void sendArrivalNotification(String info) {
        // Use the notification logic we built earlier here!
        Log.i("Service", "Near package: " + info);
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID, "Location Service", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(serviceChannel);
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    public static void start(Context context) {
        Intent serviceIntent = new Intent(context, LocationTrackingService.class);
        context.startForegroundService(serviceIntent);
    }
}
