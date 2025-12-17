package com.ergi.rusdihari;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView tvCurrentAddress;
    private MaterialButton btnSelectLocation;
    private AutocompleteSupportFragment autocompleteFragment;

    private double selectedLat = -6.2088;
    private double selectedLng = 106.8456;
    private String selectedAddressString = "";

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        // Inisialisasi API Key
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getApiKey());
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        tvCurrentAddress = findViewById(R.id.tvCurrentAddress);
        btnSelectLocation = findViewById(R.id.btnSelectLocation);
        btnSelectLocation.setEnabled(false);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupSearchBar();

        btnSelectLocation.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("ADDRESS_RESULT", selectedAddressString);
            resultIntent.putExtra("LAT_RESULT", selectedLat);
            resultIntent.putExtra("LNG_RESULT", selectedLng);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private String getApiKey() {
        try {
            android.content.pm.ApplicationInfo app = getPackageManager()
                    .getApplicationInfo(getPackageName(), android.content.pm.PackageManager.GET_META_DATA);
            android.os.Bundle bundle = app.metaData;
            return bundle.getString("com.google.android.geo.API_KEY");
        } catch (Exception e) {
            return "";
        }
    }

    private void setupSearchBar() {
        // Ambil fragment
        autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        if (autocompleteFragment != null) {

            autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    if (place.getLatLng() != null && mMap != null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 17f));
                    }
                }
                @Override
                public void onError(@NonNull Status status) {
                    Toast.makeText(LocationPickerActivity.this, "Search Error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        LatLng defaultLoc = new LatLng(selectedLat, selectedLng);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 10f));
        enableMyLocation();

        mMap.setOnCameraIdleListener(() -> {
            LatLng center = mMap.getCameraPosition().target;
            selectedLat = center.latitude;
            selectedLng = center.longitude;
            getAddressFromLocation(selectedLat, selectedLng);
        });
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng userLoc = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLoc, 17f));

                        // 2. SET LOCATION BIAS
                        double biasAmount = 0.05;
                        RectangularBounds bounds = RectangularBounds.newInstance(
                                new LatLng(location.getLatitude() - biasAmount, location.getLongitude() - biasAmount), // Pojok Kiri Bawah
                                new LatLng(location.getLatitude() + biasAmount, location.getLongitude() + biasAmount)  // Pojok Kanan Atas
                        );

                        if (autocompleteFragment != null) {
                            autocompleteFragment.setLocationBias(bounds);
                        }

                    } else {
                        Toast.makeText(this, "Lokasi tidak terdeteksi", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            }
        }
    }

    private void getAddressFromLocation(double lat, double lng) {
        tvCurrentAddress.setText("Memuat alamat...");
        btnSelectLocation.setEnabled(false);

        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            String finalAddress = "";
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    finalAddress = addresses.get(0).getAddressLine(0);
                } else {
                    finalAddress = String.format(Locale.getDefault(), "Lokasi: %.5f, %.5f", lat, lng);
                }
            } catch (IOException e) {
                e.printStackTrace();
                finalAddress = String.format(Locale.getDefault(), "%.5f, %.5f", lat, lng);
            }
            String resultToShow = finalAddress;
            runOnUiThread(() -> {
                selectedAddressString = resultToShow;
                tvCurrentAddress.setText(resultToShow);
                btnSelectLocation.setEnabled(true);
            });
        }).start();
    }
}