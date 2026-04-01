package com.hiketracker.ui.hike;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.hiketracker.MainActivity;
import com.hiketracker.R;

public class LocationTrackingService extends Service {

    private static final String CHANNEL_ID = "hike_tracking_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final long UPDATE_INTERVAL_MS = 3000L;   // 3 seconds
    private static final long FASTEST_INTERVAL_MS = 1000L; // 1 second

    private final IBinder binder = new LocalBinder();
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationListener locationListener;

    // Tracking state
    private Location lastLocation = null;
    private float totalDistanceMeters = 0f;
    private long startTimeMillis = 0L;

    public class LocalBinder extends Binder {
        public LocationTrackingService getService() {
            return LocationTrackingService.this;
        }
    }

    public interface LocationListener {
        void onLocationUpdate(Location location, float totalDistance, long elapsedSeconds);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        startLocationUpdates();
        startTimeMillis = System.currentTimeMillis();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setLocationListener(LocationListener listener) {
        this.locationListener = listener;
    }

    public float getTotalDistanceMeters() {
        return totalDistanceMeters;
    }

    public long getElapsedSeconds() {
        if (startTimeMillis == 0) return 0;
        return (System.currentTimeMillis() - startTimeMillis) / 1000;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
                .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    processNewLocation(location);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void processNewLocation(Location newLocation) {
        if (lastLocation != null) {
            float distance = lastLocation.distanceTo(newLocation);
            // Filter out GPS noise (ignore jumps < 2m or > 50m between updates)
            if (distance >= 2f && distance <= 50f) {
                totalDistanceMeters += distance;
            }
        }
        lastLocation = newLocation;

        if (locationListener != null) {
            locationListener.onLocationUpdate(newLocation, totalDistanceMeters, getElapsedSeconds());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Hike Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Tracks your hike in the background");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Hike in Progress")
                .setContentText("Tracking your location...")
                .setSmallIcon(R.drawable.ic_hiking)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
