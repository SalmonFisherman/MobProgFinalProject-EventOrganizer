package com.ergi.rusdihari;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
/**
 * AdminDashboardActivity
 * - List events (RecyclerView)
 * - Search
 * - Sort
 * - FAB create event
 *
 * UI goal: fun modern, gradient header, card list.
 */
public class AdminDashboardActivity extends AppCompatActivity {
    private AppDatabase database;
    private MaterialToolbar toolbar;
    private com.google.android.material.textfield.TextInputEditText searchEditText;
    private Spinner sortSpinner;
    private RecyclerView eventsRecyclerView;
    private FloatingActionButton createFab;
    private final List<AppDatabase.Event> items = new ArrayList<>();
    private EventsAdapter adapter;
    private AppDatabase.EventSortMode currentSort = AppDatabase.EventSortMode.CREATED_DESC;
    private String currentQuery = "";
    private final SimpleDateFormat prettyDateTime = new SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault());
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);
        database = new AppDatabase(this);
        database.ensureSeedDataIfEmpty();
        bindViews();
        setupToolbar();
        setupRecycler();      // pindahin ke atas
        setupSortSpinner();   // setelah adapter ready
        setupSearch();
        setupFab();

        loadEventsAndRender();
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh list when returning from CreateEventActivity or after edits.
        loadEventsAndRender();
    }
    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        searchEditText = findViewById(R.id.searchEditText);
        sortSpinner = findViewById(R.id.sortSpinner);
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView);
        createFab = findViewById(R.id.createFab);
    }
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        // No special menu here; search is in header field.
    }
    private void setupSortSpinner() {
        List<String> labels = new ArrayList<>();
        labels.add("Newest");
        labels.add("Event date (ascending)");
        labels.add("Title (A-Z)");
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                labels
        );
        sortSpinner.setAdapter(arrayAdapter);
        sortSpinner.setSelection(0);
        sortSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) currentSort = AppDatabase.EventSortMode.DATE_ASC;
                else if (position == 2) currentSort = AppDatabase.EventSortMode.TITLE_ASC;
                else currentSort = AppDatabase.EventSortMode.CREATED_DESC;
                loadEventsAndRender();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // no-op
            }
        });
    }
    private void setupRecycler() {
        adapter = new EventsAdapter(items, new EventsAdapter.Listener() {
            @Override
            public void onEventClicked(@NonNull AppDatabase.Event event) {
                openEventAsAdmin(event.id);
            }
            @Override
            public void onEventLongPressed(@NonNull AppDatabase.Event event) {
                // Keep long-press available for future actions (delete/edit).
                // For now, open as admin too.
                openEventAsGuest(event.id);
            }
        });
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventsRecyclerView.setAdapter(adapter);
    }
    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s == null ? "" : s.toString();
                loadEventsAndRender();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }
    private void setupFab() {
        createFab.setOnClickListener(v -> {
            Intent i = new Intent(this, CreateEventActivity.class);
            startActivity(i);
        });
    }
    private void loadEventsAndRender() {
        List<AppDatabase.Event> fromDb = database.listEvents(currentQuery, currentSort);
        items.clear();
        items.addAll(fromDb);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
    private void openEventAsAdmin(long eventId) {
        Intent i = new Intent(this, EventDetailActivity.class);
        i.putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId);
        i.putExtra(EventDetailActivity.EXTRA_MODE, EventDetailActivity.MODE_ADMIN);
        startActivity(i);
    }
    private void openEventAsGuest(long eventId) {
        Intent i = new Intent(this, EventDetailActivity.class);
        i.putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId);
        i.putExtra(EventDetailActivity.EXTRA_MODE, EventDetailActivity.MODE_GUEST);
        startActivity(i);
    }
    // ---------------------------------------------
    // Adapter (inner class to keep file self-contained)
    // ---------------------------------------------
    static class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.Holder> {
        interface Listener {
            void onEventClicked(@NonNull AppDatabase.Event event);
            void onEventLongPressed(@NonNull AppDatabase.Event event);
        }
        private final List<AppDatabase.Event> items;
        private final Listener listener;
        EventsAdapter(@NonNull List<AppDatabase.Event> items, @NonNull Listener listener) {
            this.items = items;
            this.listener = listener;
        }
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_event, parent, false);
            return new Holder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            AppDatabase.Event e = items.get(position);
            holder.bind(e, listener);
        }
        @Override
        public int getItemCount() {
            return items.size();
        }
        static class Holder extends RecyclerView.ViewHolder {
            private final ImageView coverImageView;
            private final TextView titleTextView;
            private final TextView subtitleTextView;
            Holder(@NonNull View itemView) {
                super(itemView);
                coverImageView = itemView.findViewById(R.id.coverImageView);
                titleTextView = itemView.findViewById(R.id.titleTextView);
                subtitleTextView = itemView.findViewById(R.id.subtitleTextView);
            }
            void bind(@NonNull AppDatabase.Event event, @NonNull Listener listener) {
                titleTextView.setText(event.title != null ? event.title : "(untitled)");
                String subtitle = buildSubtitle(itemView.getContext(), event);
                subtitleTextView.setText(subtitle);
                renderCoverUri(event.coverUri);
                itemView.setOnClickListener(v -> listener.onEventClicked(event));
                itemView.setOnLongClickListener(v -> {
                    listener.onEventLongPressed(event);
                    return true;
                });
            }
            private void renderCoverUri(@Nullable String coverUri) {
                coverImageView.setImageDrawable(null);
                coverImageView.setBackgroundColor(0xFF111827);

                if (coverUri == null) return;

                try {
                    android.net.Uri uri = android.net.Uri.parse(coverUri);

                    android.graphics.Bitmap bmp;
                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                        android.graphics.ImageDecoder.Source src =
                                android.graphics.ImageDecoder.createSource(itemView.getContext().getContentResolver(), uri);
                        bmp = android.graphics.ImageDecoder.decodeBitmap(src);
                    } else {
                        bmp = android.provider.MediaStore.Images.Media.getBitmap(
                                itemView.getContext().getContentResolver(), uri
                        );
                    }

                    if (bmp != null) {
                        coverImageView.setImageBitmap(bmp);
                    }
                } catch (SecurityException se) {
                    // URI picker lama -> abaikan (jangan crash)
                    coverImageView.setImageDrawable(null);
                    coverImageView.setBackgroundColor(0xFF111827);
                } catch (Exception ignored) {
                    coverImageView.setImageDrawable(null);
                    coverImageView.setBackgroundColor(0xFF111827);
                }
            }
            private String buildSubtitle(@NonNull android.content.Context context, @NonNull AppDatabase.Event event) {
                String loc = event.location == null ? "Unknown location" : event.location;
                String dt = new java.text.SimpleDateFormat("dd MMM yyyy • HH:mm", java.util.Locale.getDefault())
                        .format(new Date(event.datetimeMillis));
                return loc + " • " + dt;
            }
        }
    }
    /*
     * Extra implementation notes (kept as comments to make file standalone & future-friendly):
     * - Search: currently live filtering via DB query LIKE.
     * - Sort: spinner changes orderBy.
     * - For huge datasets, consider paging (Paging3) and indexing.
     * - For cover images, a proper image loader is recommended.
     * - For admin delete/edit, add contextual menu on long-press.
     */
}