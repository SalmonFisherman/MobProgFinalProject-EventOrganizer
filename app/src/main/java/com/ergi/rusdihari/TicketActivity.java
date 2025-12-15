package com.ergi.rusdihari;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
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
/**
 * TicketActivity
 * - Can be opened by:
 *   - Intent extra token (from RSVP flow)
 *   - Deep link: rusdihari://ticket?token=...
 * - Shows guest + event and a QR code representing the token.
 */
public class TicketActivity extends AppCompatActivity {
    public static final String EXTRA_TOKEN = "extra_token";
    private AppDatabase database;
    private MaterialToolbar toolbar;
    private TextView confirmedTextView;
    private TextView guestNameTextView;
    private TextView eventTitleTextView;
    private ImageView qrImageView;
    private TextView tokenTextView;
    private MaterialButton shareLinkButton;
    private MaterialButton copyTokenButton;
    private TextView checkinStatusTextView;
    private String token;
    private final SimpleDateFormat pretty = new SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault());
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);
        database = new AppDatabase(this);
        bindViews();
        setupToolbar();
        token = resolveTokenFromIntent(getIntent());
        if (token == null) {
            Snackbar.make(toolbar, "Missing token.", Snackbar.LENGTH_LONG).show();
            finish();
            return;
        }
        renderTicket(token);
        setupButtons();
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Handle if activity is singleTop in future; not required now but safe.
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
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    @Nullable
    private String resolveTokenFromIntent(@Nullable Intent intent) {
        if (intent == null) return null;
        // 1) Extras
        String t = intent.getStringExtra(EXTRA_TOKEN);
        if (t != null && t.trim().length() > 0) return t.trim();
        // 2) Deep link: rusdihari://ticket?token=...
        Uri data = intent.getData();
        if (data != null) {
            String q = data.getQueryParameter("token");
            if (q != null && q.trim().length() > 0) return q.trim();
        }
        return null;
    }
    private void renderTicket(@NonNull String token) {
        AppDatabase.TicketBundle bundle = database.getTicketBundleByToken(token);
        if (bundle == null) {
            confirmedTextView.setText("Ticket not found");
            guestNameTextView.setText("-");
            eventTitleTextView.setText("No local record for token.");
            tokenTextView.setText("Token: " + token);
            qrImageView.setImageBitmap(generateQrBitmap(token, 220, 220));
            checkinStatusTextView.setText("Check-in status: unknown");
            return;
        }
        AppDatabase.Guest guest = bundle.guest;
        AppDatabase.Event event = bundle.event;
        String headline = guest.isRsvpYes() ? "You're confirmed" : "RSVP recorded (Not attending)";
        confirmedTextView.setText(headline);
        guestNameTextView.setText(guest.name != null ? guest.name : "-");
        String eventLine = (event.title != null ? event.title : "Event")
                + " • " + pretty.format(new Date(event.datetimeMillis));
        eventTitleTextView.setText(eventLine);
        tokenTextView.setText("Token: " + token + " (" + AppDatabase.formatTokenShort(token) + ")");
        qrImageView.setImageBitmap(generateQrBitmap(token, 220, 220));
        if (guest.isCheckedIn()) {
            checkinStatusTextView.setText("Check-in status: CHECKED-IN at " + new Date(guest.checkedInAt));
        } else {
            checkinStatusTextView.setText("Check-in status: not checked-in yet");
        }
    }
    private void setupButtons() {
        shareLinkButton.setOnClickListener(v -> {
            String link = buildTicketLink(token);
            shareText("My ticket link:\n" + link + "\n\nNote: works on the same device (local DB).");
        });
        copyTokenButton.setOnClickListener(v -> {
            copyToClipboard("rusdihari token", token);
            Snackbar.make(copyTokenButton, "Token copied.", Snackbar.LENGTH_SHORT).show();
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
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboardManager == null) return;
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text));
    }
    // QR generator without extra libs (uses zxing-core)
    @Nullable
    private Bitmap generateQrBitmap(@NonNull String content, int width, int height) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height);
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            int black = 0xFF111827;
            int white = 0xFFFFFFFF;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? black : white);
                }
            }
            return bmp;
        } catch (Exception ex) {
            return null;
        }
    }
    /*
     * Extra notes:
     * - QR content is the raw token. Scanner reads token and validates in SQLite.
     * - You can extend content to "rusdihari://ticket?token=..." inside the QR
     *   if you want the scan result to open the app ticket directly.
     * - For now we scan token and do DB lookup.
     */
}