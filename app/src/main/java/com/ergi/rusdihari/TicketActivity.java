package com.ergi.rusdihari;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TicketActivity extends AppCompatActivity {

    public static final String EXTRA_TOKEN = "extra_token";

    private AppDatabase database;
    private MaterialToolbar toolbar;

    private TextView confirmedTextView;
    private TextView guestNameTextView;
    private TextView eventTitleTextView;
    private ImageView qrImageView;
    private TextView tokenTextView;
    private TextView checkinStatusTextView;

    private MaterialButton shareLinkButton;
    private MaterialButton copyTokenButton;

    private String token;
    private final SimpleDateFormat pretty =
            new SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        database = new AppDatabase(this);
        bindViews();
        setupToolbar();

        token = resolveTokenFromIntent(getIntent());
        if (token == null) {
            Snackbar.make(toolbar, "Ticket token not found.", Snackbar.LENGTH_LONG).show();
            finish();
            return;
        }

        renderTicket(token);
        setupButtons();
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
        toolbar = findViewById(R.id.toolbar);
        confirmedTextView = findViewById(R.id.confirmedTextView);
        guestNameTextView = findViewById(R.id.guestNameTextView);
        eventTitleTextView = findViewById(R.id.eventTitleTextView);
        qrImageView = findViewById(R.id.qrImageView);
        tokenTextView = findViewById(R.id.tokenTextView);
        shareLinkButton = findViewById(R.id.shareLinkButton);
        copyTokenButton = findViewById(R.id.copyTokenButton);
        checkinStatusTextView = findViewById(R.id.checkinStatusTextView);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        } else {
            Log.e("TicketActivity", "Toolbar is null!");
        }
    }

    @Nullable
    private String resolveTokenFromIntent(@Nullable Intent intent) {
        if (intent == null) return null;

        // From intent extra
        String t = intent.getStringExtra(EXTRA_TOKEN);
        if (t != null && !t.trim().isEmpty()) return t.trim();

        // From deep link
        Uri data = intent.getData();
        if (data != null) {
            String q = data.getQueryParameter("token");
            if (q != null && !q.trim().isEmpty()) return q.trim();
        }
        return null;
    }

    private void renderTicket(@NonNull String token) {
        AppDatabase.TicketBundle bundle = database.getTicketBundleByToken(token);
        Bitmap qrBitmap = generateQrBitmap(token, 220, 220);
        if (qrBitmap != null) {
            qrImageView.setImageBitmap(qrBitmap);
        } else {
            qrImageView.setImageDrawable(null);
        }

        if (bundle == null) {
            confirmedTextView.setText("Ticket not found");
            guestNameTextView.setText("-");
            eventTitleTextView.setText("No local record for this ticket");
            tokenTextView.setText("Token: " + token);
            checkinStatusTextView.setText("Check-in status: unknown");
            return;
        }

        AppDatabase.Guest guest = bundle.guest;
        AppDatabase.Event event = bundle.event;

        confirmedTextView.setText(
                guest.isRsvpYes()
                        ? "You're confirmed 🎉"
                        : "RSVP recorded (Not attending)"
        );

        guestNameTextView.setText(
                guest.name != null ? guest.name : "-"
        );

        String eventLine =
                (event.title != null ? event.title : "Event")
                        + " • "
                        + pretty.format(new Date(event.datetimeMillis));
        eventTitleTextView.setText(eventLine);

        tokenTextView.setText(
                "Token: " + AppDatabase.formatTokenShort(token)
        );

        if (guest.isCheckedIn()) {
            checkinStatusTextView.setText(
                    "Checked-in at " + new Date(guest.checkedInAt)
            );
        } else {
            checkinStatusTextView.setText(
                    "Not checked-in yet"
            );
        }
    }

    private void setupButtons() {
        shareLinkButton.setOnClickListener(v -> {
            String link = buildTicketLink(token);
            shareText("My event ticket:\n" + link);
        });

        copyTokenButton.setOnClickListener(v -> {
            copyToClipboard("Ticket Token", token);
            Snackbar.make(copyTokenButton, "Token copied", Snackbar.LENGTH_SHORT).show();
        });
    }

    @NonNull
    private String buildTicketLink(@NonNull String token) {
        return "rusdihari://ticket?token=" + Uri.encode(token);
    }

    private void shareText(@NonNull String text) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(share, "Share via"));
    }

    private void copyToClipboard(@NonNull String label, @NonNull String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText(label, text));
        }
    }

    @Nullable
    private Bitmap generateQrBitmap(@NonNull String content, int width, int height) {
        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(content, BarcodeFormat.QR_CODE, width, height);

            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            int black = 0xFF111827;
            int white = 0xFFFFFFFF;

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? black : white);
                }
            }
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }
}
