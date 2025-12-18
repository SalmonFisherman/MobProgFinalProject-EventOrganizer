package com.ergi.rusdihari;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

// JourneyApps modern scan API:
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScannerActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "extra_event_id";

    private AppDatabase database;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private MaterialToolbar toolbar;
    private TextView eventLabelTextView;
    private MaterialButton scanButton;

    private TextView totalInvitedTextView;
    private TextView rsvpYesTextView;
    private TextView checkedInTextView;
    private LinearProgressIndicator checkinProgressIndicator;
    private TextView progressHintTextView;

    private RecyclerView guestsRecyclerView;

    private long eventId = -1L;
    private AppDatabase.Event event;

    private final List<AppDatabase.Guest> guests = new ArrayList<>();
    private GuestsAdapter adapter;

    private final SimpleDateFormat pretty =
            new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());

    private final ActivityResultLauncher<String[]> permissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean cam = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                boolean fine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                boolean coarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));

                String msg = "Permission: camera=" + cam + ", location=" + (fine || coarse);
                Snackbar.make(scanButton, msg, Snackbar.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<ScanOptions> scanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result == null) {
                    Snackbar.make(scanButton, "Scan failed (no result).", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                String contents = result.getContents();
                if (contents == null || contents.trim().isEmpty()) {
                    Snackbar.make(scanButton, "Scan cancelled.", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                handleScannedContent(contents);
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        database = new AppDatabase(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, -1L);

        bindViews();
        setupToolbar();
        loadEventOrFinish();
        setupRecycler();
        setupScanButton();

        refreshGuestsAndStats();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        eventLabelTextView = findViewById(R.id.eventLabelTextView);
        scanButton = findViewById(R.id.scanButton);

        totalInvitedTextView = findViewById(R.id.totalInvitedTextView);
        rsvpYesTextView = findViewById(R.id.rsvpYesTextView);
        checkedInTextView = findViewById(R.id.checkedInTextView);
        checkinProgressIndicator = findViewById(R.id.checkinProgressIndicator);
        progressHintTextView = findViewById(R.id.progressHintTextView);

        guestsRecyclerView = findViewById(R.id.guestsRecyclerView);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadEventOrFinish() {
        event = database.getEventById(eventId);
        if (event == null) {
            Snackbar.make(toolbar, "Event not found.", Snackbar.LENGTH_LONG).show();
            finish();
            return;
        }
        String label = (event.title != null ? event.title : "Event") + " • Admin Scanner";
        eventLabelTextView.setText(label);
    }

    private void setupRecycler() {
        adapter = new GuestsAdapter(guests);
        guestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        guestsRecyclerView.setAdapter(adapter);
    }

    private void setupScanButton() {
        scanButton.setOnClickListener(v -> {
            if (!hasCameraPermission()) {
                permissionsLauncher.launch(new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
                Snackbar.make(scanButton, "Grant camera permission to scan.", Snackbar.LENGTH_SHORT).show();
                return;
            }

            startScan();
        });
    }

    private boolean hasCameraPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasAnyLocationPermission() {
        boolean fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        return fine || coarse;
    }

    private void startScan() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan QR Ticket (token)");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setBarcodeImageEnabled(true); // <-- ini penting
        scanLauncher.launch(options);
    }

    private void handleScannedContent(@NonNull String content) {
        String token = extractTokenFromQrContent(content);
        if (token == null) {
            Snackbar.make(scanButton, "Invalid QR content.", Snackbar.LENGTH_LONG).show();
            return;
        }

        AppDatabase.Guest guest = database.getGuestByToken(token);
        if (guest == null) {
            Snackbar.make(scanButton, "Token not found in local DB.", Snackbar.LENGTH_LONG).show();
            return;
        }

        if (guest.eventId != eventId) {
            Snackbar.make(scanButton, "This ticket belongs to another event.", Snackbar.LENGTH_LONG).show();
            return;
        }

        if (guest.isCheckedIn()) {
            Snackbar.make(scanButton, "Already checked-in: " + safe(guest.name), Snackbar.LENGTH_LONG).show();
            return;
        }

        // NEW: admin must input guest name if empty
        if (!guest.hasName()) {
            promptAdminForGuestNameThenCheckin(token, guest);
            return;
        }

        long now = System.currentTimeMillis();
        doCheckInWithBestEffortLocation(token, guest, now);
    }

    private void promptAdminForGuestNameThenCheckin(@NonNull String token, @NonNull AppDatabase.Guest guest) {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter guest name");
        input.setSingleLine(true);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Guest name required")
                .setMessage("This ticket has no name yet. Please enter guest name to continue check-in.")
                .setView(input)
                .setPositiveButton("Save & Check-in", (dialog, which) -> {
                    String name = input.getText() != null ? input.getText().toString().trim() : "";
                    if (name.isEmpty()) {
                        Snackbar.make(scanButton, "Name cannot be empty.", Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    boolean ok = database.updateGuestNameByToken(token, name);
                    if (!ok) {
                        Snackbar.make(scanButton, "Failed to save name.", Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    // refresh guest object for UI message consistency
                    guest.name = name;

                    long now = System.currentTimeMillis();
                    doCheckInWithBestEffortLocation(token, guest, now);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Snackbar.make(scanButton, "Cancelled. Name not saved.", Snackbar.LENGTH_SHORT).show();
                })
                .show();
    }

    @Nullable
    private String extractTokenFromQrContent(@NonNull String content) {
        String trimmed = content.trim();
        if (trimmed.isEmpty()) return null;

        if (trimmed.startsWith("rusdihari://ticket")) {
            try {
                Uri uri = Uri.parse(trimmed);
                String token = uri.getQueryParameter("token");
                if (token != null && !token.trim().isEmpty()) return token.trim();
            } catch (Exception ignored) { }
            return null;
        }

        return trimmed;
    }

    // IMPORTANT:
    // Lint sometimes can't prove the permission guard, so we suppress after guarding.
    @SuppressLint("MissingPermission")
    private void doCheckInWithBestEffortLocation(
            @NonNull String token,
            @NonNull AppDatabase.Guest guest,
            long nowMillis
    ) {
        if (!hasAnyLocationPermission()) {
            boolean ok = database.markCheckedIn(token, nowMillis, null, null);
            afterCheckIn(ok, guest, null);
            return;
        }

        try {
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        Double lat = null;
                        Double lng = null;

                        if (location != null) {
                            lat = location.getLatitude();
                            lng = location.getLongitude();
                        }

                        boolean ok = database.markCheckedIn(token, nowMillis, lat, lng);
                        afterCheckIn(ok, guest, location);
                    })
                    .addOnFailureListener(ex -> {
                        boolean ok = database.markCheckedIn(token, nowMillis, null, null);
                        afterCheckIn(ok, guest, null);
                    });
        } catch (SecurityException se) {
            // Extra safety: if permission revoked mid-flight
            boolean ok = database.markCheckedIn(token, nowMillis, null, null);
            afterCheckIn(ok, guest, null);
        }
    }

    private void afterCheckIn(boolean ok, @NonNull AppDatabase.Guest guest, @Nullable Location location) {
        if (!ok) {
            Snackbar.make(scanButton, "Failed to check-in.", Snackbar.LENGTH_LONG).show();
            return;
        }

        String locText = "";
        if (location != null) {
            locText = String.format(
                    Locale.getDefault(),
                    " (%.5f, %.5f)",
                    location.getLatitude(),
                    location.getLongitude()
            );
        }

        String msg = "Checked-in: " + safe(guest.name) + " • " + pretty.format(new Date()) + locText;
        Snackbar.make(scanButton, msg, Snackbar.LENGTH_LONG).show();

        refreshGuestsAndStats();
    }

    private void refreshGuestsAndStats() {
        guests.clear();
        guests.addAll(database.listGuestsForEvent(eventId));
        adapter.notifyDataSetChanged();

        int total = database.countInvited(eventId);
        int yes = database.countRsvpYes(eventId);
        int checked = database.countCheckedIn(eventId);

        totalInvitedTextView.setText("Total invited: " + total);
        rsvpYesTextView.setText("RSVP yes: " + yes);
        checkedInTextView.setText("Checked-in: " + checked);

        int max = Math.max(yes, 1);
        int progress = Math.min(checked, max);

        checkinProgressIndicator.setMax(max);
        checkinProgressIndicator.setProgress(progress);

        progressHintTextView.setText(
                "Progress: " + checked + " / " + yes + " (checked-in / RSVP yes)"
        );
    }

    @NonNull
    private String safe(@Nullable String s) {
        if (s == null) return "-";
        String t = s.trim();
        return t.isEmpty() ? "-" : t;
    }

    static class GuestsAdapter extends RecyclerView.Adapter<GuestsAdapter.Holder> {

        private final List<AppDatabase.Guest> items;

        GuestsAdapter(@NonNull List<AppDatabase.Guest> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_guest, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            private final TextView nameTextView;
            private final TextView subtitleTextView;
            private final TextView statusTextView;

            Holder(@NonNull View itemView) {
                super(itemView);
                nameTextView = itemView.findViewById(R.id.nameTextView);
                subtitleTextView = itemView.findViewById(R.id.subtitleTextView);
                statusTextView = itemView.findViewById(R.id.statusTextView);
            }

            void bind(@NonNull AppDatabase.Guest guest) {
                nameTextView.setText(guest.name != null ? guest.name : "-");

                String rsvp = guest.rsvpStatus != null ? guest.rsvpStatus : "-";
                String menu = guest.menuChoice != null ? guest.menuChoice : "-";
                subtitleTextView.setText("RSVP: " + rsvp + " • Menu: " + menu);

                if (guest.isCheckedIn()) {
                    statusTextView.setText("Checked-in: YES");
                    statusTextView.setTextColor(0xFF16A34A);
                } else {
                    statusTextView.setText("Checked-in: NO");
                    statusTextView.setTextColor(0xFFEF4444);
                }
            }
        }
    }
}