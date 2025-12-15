package com.ergi.rusdihari;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * CreateEventActivity
 * - Input: title, description, location
 * - Pick: date & time (Material pickers)
 * - Pick cover image: GetContent image/* then COPY to internal storage
 * - Publish: insert event into SQLite
 *
 * Why copy to internal storage?
 * - Photo Picker / GetContent can return temporary content:// URIs (picker_get_content)
 * - After app restart, permission may be gone -> SecurityException -> crash when ImageView decodes
 * - By copying to internal storage, we store file:// Uri that stays readable for this app.
 */
public class CreateEventActivity extends AppCompatActivity {

    private AppDatabase database;

    private MaterialToolbar toolbar;
    private ImageView coverPreviewImageView;
    private MaterialButton pickCoverButton;

    private com.google.android.material.textfield.TextInputEditText titleEditText;
    private com.google.android.material.textfield.TextInputEditText descriptionEditText;
    private com.google.android.material.textfield.TextInputEditText locationEditText;
    private com.google.android.material.textfield.TextInputEditText dateEditText;
    private com.google.android.material.textfield.TextInputEditText timeEditText;

    private MaterialButton publishButton;

    // We store INTERNAL file uri (file://...) here after copying.
    private Uri selectedCoverUri = null;

    // Selected date/time components
    private Integer selectedYear = null;
    private Integer selectedMonth = null; // 0-based
    private Integer selectedDay = null;

    private Integer selectedHour = null;
    private Integer selectedMinute = null;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;

                View anchor = (publishButton != null) ? publishButton : coverPreviewImageView;

                Uri saved = copyUriToInternalStorage(uri);
                if (saved != null) {
                    selectedCoverUri = saved;
                    renderCoverPreview();
                    Snackbar.make(anchor, "Cover saved locally.", Snackbar.LENGTH_SHORT).show();
                } else {
                    selectedCoverUri = null;
                    renderCoverPreview();
                    Snackbar.make(anchor, "Failed to save cover image. Using default.", Snackbar.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        database = new AppDatabase(this);

        bindViews();
        setupToolbar();
        setupPickCover();
        setupDateAndTimePickers();
        setupPublish();

        renderCoverPreview();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        coverPreviewImageView = findViewById(R.id.coverPreviewImageView);
        pickCoverButton = findViewById(R.id.pickCoverButton);

        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        locationEditText = findViewById(R.id.locationEditText);

        dateEditText = findViewById(R.id.dateEditText);
        timeEditText = findViewById(R.id.timeEditText);

        publishButton = findViewById(R.id.publishButton);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupPickCover() {
        pickCoverButton.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    private void setupDateAndTimePickers() {
        dateEditText.setFocusable(false);
        timeEditText.setFocusable(false);

        dateEditText.setOnClickListener(v -> openDatePicker());
        timeEditText.setOnClickListener(v -> openTimePicker());
    }

    private void openDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Pick event date")
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(selection);

            selectedYear = c.get(Calendar.YEAR);
            selectedMonth = c.get(Calendar.MONTH);
            selectedDay = c.get(Calendar.DAY_OF_MONTH);

            dateEditText.setText(dateFormat.format(c.getTime()));
        });

        picker.show(getSupportFragmentManager(), "datePicker");
    }

    private void openTimePicker() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTitleText("Pick event time")
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(selectedHour != null ? selectedHour : 19)
                .setMinute(selectedMinute != null ? selectedMinute : 0)
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            selectedHour = picker.getHour();
            selectedMinute = picker.getMinute();
            timeEditText.setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute));
        });

        picker.show(getSupportFragmentManager(), "timePicker");
    }

    private void setupPublish() {
        publishButton.setOnClickListener(v -> {
            if (!validateInputs()) return;

            AppDatabase.Event e = buildEventFromInputs();
            long id = database.insertEvent(e);

            if (id > 0) {
                Snackbar.make(publishButton, "Event published locally.", Snackbar.LENGTH_LONG).show();
                finish();
            } else {
                Snackbar.make(publishButton, "Failed to save event.", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateInputs() {
        String title = textOf(titleEditText);
        if (TextUtils.isEmpty(title)) {
            titleEditText.setError("Title is required");
            return false;
        }

        if (selectedYear == null || selectedMonth == null || selectedDay == null) {
            Snackbar.make(publishButton, "Pick a date first.", Snackbar.LENGTH_LONG).show();
            return false;
        }

        if (selectedHour == null || selectedMinute == null) {
            Snackbar.make(publishButton, "Pick a time first.", Snackbar.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    @NonNull
    private AppDatabase.Event buildEventFromInputs() {
        AppDatabase.Event e = new AppDatabase.Event();
        e.title = textOf(titleEditText);
        e.description = textOf(descriptionEditText);
        e.location = textOf(locationEditText);
        e.coverUri = (selectedCoverUri != null) ? selectedCoverUri.toString() : null;
        e.createdAt = System.currentTimeMillis();
        e.datetimeMillis = buildDateTimeMillis();
        return e;
    }

    private long buildDateTimeMillis() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, selectedYear);
        c.set(Calendar.MONTH, selectedMonth);
        c.set(Calendar.DAY_OF_MONTH, selectedDay);
        c.set(Calendar.HOUR_OF_DAY, selectedHour);
        c.set(Calendar.MINUTE, selectedMinute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /**
     * IMPORTANT: do NOT use ImageView.setImageURI() here.
     * It can decode later during measurement and crash if a bad content Uri slips in.
     * We decode a bitmap ourselves and setImageBitmap().
     */
    private void renderCoverPreview() {
        coverPreviewImageView.setImageDrawable(null);
        coverPreviewImageView.setBackgroundColor(0xFF111827);

        if (selectedCoverUri == null) return;

        try {
            Bitmap bmp = decodeBitmapFromUri(this, selectedCoverUri);
            if (bmp != null) {
                coverPreviewImageView.setImageBitmap(bmp);
            }
        } catch (Exception ignored) {
            coverPreviewImageView.setImageDrawable(null);
            coverPreviewImageView.setBackgroundColor(0xFF111827);
        }
    }

    @Nullable
    private static Bitmap decodeBitmapFromUri(@NonNull Context context, @NonNull Uri uri) {
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
            if (in == null) return null;

            // Optional: downsample if you want (keeps memory low)
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.RGB_565;

            return BitmapFactory.decodeStream(in, null, opts);
        } catch (Exception e) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Copy picked image content://... to internal storage files/covers/ as jpg bytes.
     * Returns a stable file:// Uri.
     */
    @Nullable
    private Uri copyUriToInternalStorage(@NonNull Uri sourceUri) {
        InputStream in = null;
        OutputStream out = null;

        try {
            in = getContentResolver().openInputStream(sourceUri);
            if (in == null) return null;

            File dir = new File(getFilesDir(), "covers");
            if (!dir.exists()) dir.mkdirs();

            File destFile = new File(dir, "cover_" + System.currentTimeMillis() + ".jpg");
            out = new FileOutputStream(destFile);

            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();

            return Uri.fromFile(destFile);
        } catch (Exception e) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
        }
    }

    @Nullable
    private String textOf(@Nullable com.google.android.material.textfield.TextInputEditText editText) {
        if (editText == null) return null;
        if (editText.getText() == null) return null;

        String s = editText.getText().toString().trim();
        return s.isEmpty() ? null : s;
    }
}