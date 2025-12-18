package com.ergi.rusdihari;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Wajib implements OnMapReadyCallback
public class TicketActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_TOKEN = "extra_token";

    private AppDatabase database;

    // UI
    private ImageView ivEventCover, ivQrCode;
    private ImageButton btnBack;
    private TextView tvEventTitle, tvEventDate, tvEventLocation, tvEventDescription;
    private TextView tvGuestName, tvToken, tvCheckInStatus;
    private MaterialButton btnShare, btnCopyToken;

    // Maps
    private GoogleMap mMap;
    private Double eventLat = null;
    private Double eventLng = null;
    private String eventLocationName = "";

    private String token;
    private final SimpleDateFormat prettyFormat =
            new SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        database = new AppDatabase(this);
        bindViews();

        // Setup Map Fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnBack.setOnClickListener(v -> finish());

        token = resolveTokenFromIntent(getIntent());
        if (token == null) {
            Toast.makeText(this, "Ticket token not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        renderTicket(token);
        setupButtons();

        androidx.core.widget.NestedScrollView mainScrollView = findViewById(R.id.mainScrollView);
        View mapOverlay = findViewById(R.id.viewMapOverlay);

        if (mainScrollView != null && mapOverlay != null) {
            mapOverlay.setOnTouchListener((v, event) -> {
                // Saat jari menyentuh area peta (ACTION_DOWN)
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Matikan fungsi scroll pada ScrollView induk agar peta bisa digeser
                    mainScrollView.requestDisallowInterceptTouchEvent(true);
                }

                // PENTING: Return false agar sentuhan diteruskan ke Google Map di bawahnya
                // Jika return true, sentuhan berhenti di overlay dan peta tidak akan merespons.
                return false;
            });
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        updateMapLocation();
    }

    private void updateMapLocation() {
        if (mMap != null && eventLat != null && eventLng != null) {
            // Cek biar ga kelempar ke 0,0
            if (eventLat != 0.0 && eventLng != 0.0) {
                LatLng loc = new LatLng(eventLat, eventLng);
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(loc).title(eventLocationName));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f));
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String newToken = resolveTokenFromIntent(intent);
        if (newToken != null && !newToken.equals(token)) {
            token = newToken;
            renderTicket(token);
        }
    }

    private void bindViews() {
        ivEventCover = findViewById(R.id.ivEventCover);
        btnBack = findViewById(R.id.btnBack);

        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvEventDate = findViewById(R.id.tvEventDate);
        tvEventLocation = findViewById(R.id.tvEventLocation);
        tvGuestName = findViewById(R.id.tvGuestName);
        tvCheckInStatus = findViewById(R.id.tvCheckInStatus);

        ivQrCode = findViewById(R.id.ivQrCode);
        tvToken = findViewById(R.id.tvToken);

        tvEventDescription = findViewById(R.id.tvEventDescription);

        btnShare = findViewById(R.id.btnShare);
        btnCopyToken = findViewById(R.id.btnCopyToken);
    }

    @Nullable
    private String resolveTokenFromIntent(@Nullable Intent intent) {
        if (intent == null) return null;
        String t = intent.getStringExtra(EXTRA_TOKEN);
        if (t != null && !t.trim().isEmpty()) return t.trim();
        Uri data = intent.getData();
        if (data != null) {
            String q = data.getQueryParameter("token");
            if (q != null && !q.trim().isEmpty()) return q.trim();
        }
        return null;
    }

    private void renderTicket(@NonNull String token) {
        AppDatabase.TicketBundle bundle = database.getTicketBundleByToken(token);

        // QR Code
        Bitmap qrBitmap = generateQrBitmap(token, 512, 512);
        if (qrBitmap != null) {
            ivQrCode.setImageBitmap(qrBitmap);
        }

        if (bundle == null) return;

        AppDatabase.Event event = bundle.event;
        AppDatabase.Guest guest = bundle.guest;

        tvEventTitle.setText(event.title);
        tvEventDate.setText(prettyFormat.format(new Date(event.datetimeMillis)));
        tvEventLocation.setText(event.location != null ? event.location : "Location TBA");
        tvEventDescription.setText(event.description != null ? event.description : "No description.");

        // SIMPAN DATA BUAT MAP
        this.eventLat = event.latitude;
        this.eventLng = event.longitude;
        this.eventLocationName = event.location;
        updateMapLocation();

        // LOAD FOTO & FIX TINT
        if (event.coverUri != null) {
            try {
                Uri uri = Uri.parse(event.coverUri);
                InputStream in = getContentResolver().openInputStream(uri);
                Bitmap coverBmp = BitmapFactory.decodeStream(in);

                ivEventCover.setImageBitmap(coverBmp);

                // --- INI KUNCI BIAR FOTO MUNCUL JELAS ---
                ivEventCover.setBackgroundColor(Color.TRANSPARENT);
                ivEventCover.setImageTintList(null);
                // ----------------------------------------

            } catch (Exception e) {
                Log.e("TicketActivity", "Error loading cover: " + e.getMessage());
            }
        }

        tvGuestName.setText(guest.name != null ? guest.name : "Guest");
        tvToken.setText("TOKEN: " + AppDatabase.formatTokenShort(token));

        if (guest.isCheckedIn()) {
            tvCheckInStatus.setText("Checked-in at " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(guest.checkedInAt)));
            tvCheckInStatus.setTextColor(Color.parseColor("#059669"));
        } else {
            tvCheckInStatus.setText("Not checked-in yet");
            tvCheckInStatus.setTextColor(Color.parseColor("#EF4444"));
        }
    }

    private void setupButtons() {
        btnShare.setOnClickListener(v -> {
            String link = "rusdihari://ticket?token=" + Uri.encode(token);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, "Here is my ticket for " + tvEventTitle.getText() + ":\n" + link);
            startActivity(Intent.createChooser(share, "Share Ticket"));
        });

        btnCopyToken.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Ticket Token", token));
                Snackbar.make(btnCopyToken, "Token copied!", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Nullable
    private Bitmap generateQrBitmap(@NonNull String content, int width, int height) {
        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(content, BarcodeFormat.QR_CODE, width, height);
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.TRANSPARENT);
                }
            }
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }
}