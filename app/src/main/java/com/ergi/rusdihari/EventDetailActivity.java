package com.ergi.rusdihari;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
/**
 * EventDetailActivity
 * - Show event detail (cover, title, meta, description)
 * - Mode:
 *   - Admin: show scanner button + share invitation text
 *   - Guest: show RSVP form and submit -> create guest + token
 *
 * Limitation:
 * - RSVP submission stores to local SQLite only.
 */
public class EventDetailActivity extends AppCompatActivity {
    public static final String EXTRA_EVENT_ID = "extra_event_id";
    public static final String EXTRA_MODE = "extra_mode";
    public static final String MODE_ADMIN = "mode_admin";
    public static final String MODE_GUEST = "mode_guest";
    private AppDatabase database;
    private MaterialToolbar toolbar;
    private ImageView coverImageView;
    private TextView titleTextView;
    private TextView metaTextView;
    private TextView descriptionTextView;
    private LinearLayout adminActionsContainer;
    private MaterialButton openScannerButton;
    private MaterialButton shareInvitationButton;
    private LinearLayout guestRsvpContainer;
    private com.google.android.material.textfield.TextInputEditText guestNameEditText;
    private RadioGroup rsvpRadioGroup;
    private RadioButton rsvpYesRadio;
    private RadioButton rsvpNoRadio;
    private RadioGroup menuRadioGroup;
    private RadioButton menuRegularRadio;
    private RadioButton menuVegetarianRadio;
    private RadioButton menuHalalRadio;
    private com.google.android.material.textfield.TextInputEditText preferenceEditText;
    private MaterialButton submitRsvpButton;
    private LinearLayout postRsvpActionsContainer;
    private MaterialButton openTicketButton;
    private MaterialButton shareTicketLinkButton;
    private long eventId = -1;
    private String mode = MODE_GUEST;
    private AppDatabase.Event event;
    private String createdTokenForThisScreen = null;
    private final SimpleDateFormat pretty = new SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault());
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);
        database = new AppDatabase(this);
        readIntent();
        bindViews();
        setupToolbar();
        loadEvent();
        setupModeUi();
        setupGuestForm();
        setupAdminActions();
    }
    private void readIntent() {
        Intent i = getIntent();
        eventId = i.getLongExtra(EXTRA_EVENT_ID, -1);
        mode = i.getStringExtra(EXTRA_MODE);
        if (mode == null) mode = MODE_GUEST;
    }
    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        coverImageView = findViewById(R.id.coverImageView);
        titleTextView = findViewById(R.id.titleTextView);
        metaTextView = findViewById(R.id.metaTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        adminActionsContainer = findViewById(R.id.adminActionsContainer);
        openScannerButton = findViewById(R.id.openScannerButton);
        shareInvitationButton = findViewById(R.id.shareInvitationButton);
        guestRsvpContainer = findViewById(R.id.guestRsvpContainer);
        guestNameEditText = findViewById(R.id.guestNameEditText);
        rsvpRadioGroup = findViewById(R.id.rsvpRadioGroup);
        rsvpYesRadio = findViewById(R.id.rsvpYesRadio);
        rsvpNoRadio = findViewById(R.id.rsvpNoRadio);
        menuRadioGroup = findViewById(R.id.menuRadioGroup);
        menuRegularRadio = findViewById(R.id.menuRegularRadio);
        menuVegetarianRadio = findViewById(R.id.menuVegetarianRadio);
        menuHalalRadio = findViewById(R.id.menuHalalRadio);
        preferenceEditText = findViewById(R.id.preferenceEditText);
        submitRsvpButton = findViewById(R.id.submitRsvpButton);
        postRsvpActionsContainer = findViewById(R.id.postRsvpActionsContainer);
        openTicketButton = findViewById(R.id.openTicketButton);
        shareTicketLinkButton = findViewById(R.id.shareTicketLinkButton);
    }
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    private void loadEvent() {
        event = database.getEventById(eventId);
        if (event == null) {
            Snackbar.make(toolbar, "Event not found.", Snackbar.LENGTH_LONG).show();
            finish();
            return;
        }
        titleTextView.setText(event.title != null ? event.title : "(untitled)");
        String meta = (event.location != null ? event.location : "Unknown location")
                + " • " + pretty.format(new Date(event.datetimeMillis));
        metaTextView.setText(meta);
        descriptionTextView.setText(event.description != null ? event.description : "No description.");
        renderCover(event.coverUri);
    }
    private void renderCover(@Nullable String coverUri) {
        if (coverUri == null) {
            coverImageView.setImageDrawable(null);
            coverImageView.setBackgroundColor(0xFF111827);
            return;
        }
        try {
            coverImageView.setImageURI(Uri.parse(coverUri));
        } catch (Exception ex) {
            coverImageView.setImageDrawable(null);
            coverImageView.setBackgroundColor(0xFF111827);
        }
    }
    private void setupModeUi() {
        boolean isAdmin = MODE_ADMIN.equals(mode);
        adminActionsContainer.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        guestRsvpContainer.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
    }
    private void setupAdminActions() {
        openScannerButton.setOnClickListener(v -> {
            Intent i = new Intent(this, ScannerActivity.class);
            i.putExtra(ScannerActivity.EXTRA_EVENT_ID, eventId);
            startActivity(i);
        });
        shareInvitationButton.setOnClickListener(v -> {
            String text = buildInvitationTextForSharing();
            shareText(text);
        });
    }
    private String buildInvitationTextForSharing() {
        // No network: just share a text invitation. For real app, you’d share a web link.
        String when = pretty.format(new Date(event.datetimeMillis));
        String where = event.location != null ? event.location : "-";
        String title = event.title != null ? event.title : "Event";
        return "You're invited!\n\n"
                + title + "\n"
                + "When: " + when + "\n"
                + "Where: " + where + "\n\n"
                + "RSVP & Ticket are stored locally on the device in this demo app.\n"
                + "If you RSVP here, you can open your ticket and show QR for check-in.";
    }
    private void setupGuestForm() {
        // Defaults
        rsvpYesRadio.setChecked(true);
        menuRegularRadio.setChecked(true);
        submitRsvpButton.setOnClickListener(v -> submitRsvp());
        openTicketButton.setOnClickListener(v -> {
            if (createdTokenForThisScreen == null) {
                Snackbar.make(openTicketButton, "No ticket yet.", Snackbar.LENGTH_LONG).show();
                return;
            }
            openTicketForToken(createdTokenForThisScreen);
        });
        shareTicketLinkButton.setOnClickListener(v -> {
            if (createdTokenForThisScreen == null) {
                Snackbar.make(shareTicketLinkButton, "No link yet.", Snackbar.LENGTH_LONG).show();
                return;
            }
            shareText(buildTicketLink(createdTokenForThisScreen));
        });
    }
    private void submitRsvp() {
        String guestName = safeText(guestNameEditText);
        if (TextUtils.isEmpty(guestName)) {
            guestNameEditText.setError("Name is required");
            return;
        }
        String rsvp = rsvpYesRadio.isChecked() ? AppDatabase.RSVP_YES : AppDatabase.RSVP_NO;
        String menu = resolveMenuChoice();
        String note = safeText(preferenceEditText);
        String token = database.generateToken();
        AppDatabase.Guest g = new AppDatabase.Guest();
        g.eventId = eventId;
        g.name = guestName;
        g.token = token;
        g.rsvpStatus = rsvp;
        g.menuChoice = menu;
        g.preferenceNote = note;
        g.createdAt = System.currentTimeMillis();
        long id = database.insertGuest(g);
        if (id <= 0) {
            Snackbar.make(submitRsvpButton, "Failed to submit RSVP (token may already exist).", Snackbar.LENGTH_LONG).show();
            return;
        }
        createdTokenForThisScreen = token;
        postRsvpActionsContainer.setVisibility(View.VISIBLE);
        Snackbar.make(submitRsvpButton, "RSVP saved. Ticket token: " + AppDatabase.formatTokenShort(token), Snackbar.LENGTH_LONG).show();
    }
    @NonNull
    private String resolveMenuChoice() {
        if (menuVegetarianRadio.isChecked()) return "Vegetarian";
        if (menuHalalRadio.isChecked()) return "Halal";
        return "Regular";
    }
    private void openTicketForToken(@NonNull String token) {
        Intent i = new Intent(this, TicketActivity.class);
        i.putExtra(TicketActivity.EXTRA_TOKEN, token);
        startActivity(i);
    }
    @NonNull
    private String buildTicketLink(@NonNull String token) {
        // Deep link requested: rusdihari://ticket?token=...
        return "rusdihari://ticket?token=" + Uri.encode(token);
    }
    private void shareText(@NonNull String text) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(share, "Share via"));
    }
    @Nullable
    private String safeText(@Nullable com.google.android.material.textfield.TextInputEditText et) {
        if (et == null || et.getText() == null) return null;
        String s = et.getText().toString().trim();
        return s.length() == 0 ? null : s;
    }
    /*
     * Extra notes:
     * - In a real-world invitation flow, guest would open a web link, RSVP remotely,
     *   and ticket would be verifiable across devices.
     * - Here, RSVP creates a local guest entry and token.
     * - Admin scanner validates token against local SQLite.
     */
}