package com.hiketracker.ui.hike;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.hiketracker.R;
import com.hiketracker.data.repository.HikeRepository;
import com.hiketracker.databinding.FragmentHikeBinding;
import com.hiketracker.model.Hike;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HikeFragment extends Fragment implements OnMapReadyCallback,
        LocationTrackingService.LocationListener {

    private FragmentHikeBinding binding;
    private GoogleMap googleMap;
    private LocationTrackingService trackingService;
    private boolean serviceBound = false;
    private boolean isTracking = false;
    private HikeRepository hikeRepository;

    private final List<LatLng> pathPoints = new ArrayList<>();
    private Polyline routePolyline;

    // Timer
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long startTimeMillis;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fine = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarse = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
                    startTracking();
                } else {
                    Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            LocationTrackingService.LocalBinder localBinder = (LocationTrackingService.LocalBinder) binder;
            trackingService = localBinder.getService();
            trackingService.setLocationListener(HikeFragment.this);
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHikeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        hikeRepository = new HikeRepository(requireActivity().getApplication());

        // Setup Google Map
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        binding.btnStartHike.setOnClickListener(v -> onStartClicked());
        binding.btnStopHike.setOnClickListener(v -> onStopClicked());

        updateUI(false);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        if (hasLocationPermission()) {
            try {
                googleMap.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    private void onStartClicked() {
        if (hasLocationPermission()) {
            startTracking();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void startTracking() {
        isTracking = true;
        pathPoints.clear();
        startTimeMillis = System.currentTimeMillis();

        // Start foreground service
        Intent serviceIntent = new Intent(getContext(), LocationTrackingService.class);
        ContextCompat.startForegroundService(requireContext(), serviceIntent);
        requireContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        startTimer();
        updateUI(true);
        Toast.makeText(getContext(), "Hike started!", Toast.LENGTH_SHORT).show();
    }

    private void onStopClicked() {
        if (!isTracking) return;
        isTracking = false;
        stopTimer();

        float distance = serviceBound ? trackingService.getTotalDistanceMeters() : 0f;
        long elapsed = serviceBound ? trackingService.getElapsedSeconds() : 0L;
        long hikeStart = serviceBound ? trackingService.getStartTimeMillis() : startTimeMillis;

        // Stop service
        if (serviceBound) {
            requireContext().unbindService(serviceConnection);
            serviceBound = false;
        }
        Intent serviceIntent = new Intent(getContext(), LocationTrackingService.class);
        requireContext().stopService(serviceIntent);

        saveHike(distance, elapsed, hikeStart);
        updateUI(false);
        Toast.makeText(getContext(), "Hike saved!", Toast.LENGTH_SHORT).show();
    }

    private void saveHike(float distance, long durationSeconds, long hikeStart) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "local";

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date(hikeStart));

        Hike hike = new Hike(userId, date, hikeStart,
                System.currentTimeMillis(), distance, durationSeconds);

        hikeRepository.insertHike(hike, id ->
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Hike #" + id + " saved!", Toast.LENGTH_SHORT).show()
                )
        );
    }

    @Override
    public void onLocationUpdate(Location location, float totalDistance, long elapsedSeconds) {
        if (!isTracking || getActivity() == null) return;

        requireActivity().runOnUiThread(() -> {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            pathPoints.add(latLng);

            // Update map
            if (googleMap != null) {
                if (routePolyline != null) routePolyline.remove();
                routePolyline = googleMap.addPolyline(new PolylineOptions()
                        .addAll(pathPoints)
                        .color(0xFF4CAF50)
                        .width(8f));

                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f));
            }

            // Update distance
            updateDistanceUI(totalDistance);
        });
    }

    private void startTimer() {
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = (System.currentTimeMillis() - startTimeMillis) / 1000;
                updateTimerUI(elapsed);
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void updateTimerUI(long elapsedSeconds) {
        long hours = elapsedSeconds / 3600;
        long minutes = (elapsedSeconds % 3600) / 60;
        long seconds = elapsedSeconds % 60;
        String time = hours > 0
                ? String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                : String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        binding.tvTime.setText(time);
    }

    private void updateDistanceUI(float meters) {
        String dist = meters >= 1000
                ? String.format(Locale.getDefault(), "%.2f km", meters / 1000f)
                : String.format(Locale.getDefault(), "%.0f m", meters);
        binding.tvDistance.setText(dist);
    }

    private void updateUI(boolean tracking) {
        binding.btnStartHike.setVisibility(tracking ? View.GONE : View.VISIBLE);
        binding.btnStopHike.setVisibility(tracking ? View.VISIBLE : View.GONE);
        binding.cardMetrics.setVisibility(tracking ? View.VISIBLE : View.GONE);
        if (!tracking) {
            binding.tvTime.setText("00:00");
            binding.tvDistance.setText("0 m");
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTimer();
        if (serviceBound) {
            requireContext().unbindService(serviceConnection);
            serviceBound = false;
        }
        binding = null;
    }
}
