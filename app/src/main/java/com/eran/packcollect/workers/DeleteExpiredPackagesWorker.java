package com.eran.packcollect.workers;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class DeleteExpiredPackagesWorker extends Worker {

    public DeleteExpiredPackagesWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("Worker", LocalDate.now().toString());

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) return Result.failure();


//        DatabaseReference ref = FirebaseDatabase.getInstance()
//                .getReference("packages")
//                .child(uid);

//        // Delete all packages with expiresAt < now
//        long now = System.currentTimeMillis();

//        ref.orderByChild("expiresAt").endAt(now).get()
//                .addOnSuccessListener(snapshot -> {
//                    for (var child : snapshot.getChildren()) {
//                        child.getRef().removeValue();
//                    }
//                });
//
        return Result.success();
    }


    public static void scheduleDailyDelete(Context context) {

        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true) // optional
                .build();

        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(DeleteExpiredPackagesWorker.class, 24, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "DeleteExpiredPackagesWorker",
                ExistingPeriodicWorkPolicy.KEEP, // don’t schedule multiple copies
                request
        );
    }
}
