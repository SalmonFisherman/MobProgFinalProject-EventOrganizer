package com.ergi.rusdihari;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
/**
 * AppDatabase - SQLiteOpenHelper (simple, no Room).
 *
 * Tables:
 * - events: id, title, description, location, datetimeMillis, coverUri, createdAt
 * - guests: id, eventId, name, token UNIQUE, rsvpStatus, menuChoice, preferenceNote,
 *           createdAt, checkedInAt, checkedInLat, checkedInLng
 *
 * Notes:
 * - This database is local to one device. Deep links (rusdihari://ticket?token=...)
 *   can only resolve if the token exists in this device database.
 * - For demo convenience, we can seed one sample event on first run.
 */
public class AppDatabase extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "rusdihari_local.db";
    public static final int DATABASE_VERSION = 1;
    // Events table
    public static final String TABLE_EVENTS = "events";
    public static final String COL_EVENT_ID = "id";
    public static final String COL_EVENT_TITLE = "title";
    public static final String COL_EVENT_DESCRIPTION = "description";
    public static final String COL_EVENT_LOCATION = "location";
    public static final String COL_EVENT_DATETIME_MILLIS = "datetimeMillis";
    public static final String COL_EVENT_COVER_URI = "coverUri";
    public static final String COL_EVENT_CREATED_AT = "createdAt";
    // Guests table
    public static final String TABLE_GUESTS = "guests";
    public static final String COL_GUEST_ID = "id";
    public static final String COL_GUEST_EVENT_ID = "eventId";
    public static final String COL_GUEST_NAME = "name";
    public static final String COL_GUEST_TOKEN = "token";
    public static final String COL_GUEST_RSVP_STATUS = "rsvpStatus";
    public static final String COL_GUEST_MENU_CHOICE = "menuChoice";
    public static final String COL_GUEST_PREFERENCE_NOTE = "preferenceNote";
    public static final String COL_GUEST_CREATED_AT = "createdAt";
    public static final String COL_GUEST_CHECKED_IN_AT = "checkedInAt";
    public static final String COL_GUEST_CHECKED_IN_LAT = "checkedInLat";
    public static final String COL_GUEST_CHECKED_IN_LNG = "checkedInLng";
    // RSVP enum-ish values
    public static final String RSVP_YES = "YES";
    public static final String RSVP_NO = "NO";
    public enum EventSortMode {
        CREATED_DESC,
        DATE_ASC,
        TITLE_ASC
    }
    public AppDatabase(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        String createEvents = "CREATE TABLE " + TABLE_EVENTS + " ("
                + COL_EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_EVENT_TITLE + " TEXT NOT NULL, "
                + COL_EVENT_DESCRIPTION + " TEXT, "
                + COL_EVENT_LOCATION + " TEXT, "
                + COL_EVENT_DATETIME_MILLIS + " INTEGER NOT NULL, "
                + COL_EVENT_COVER_URI + " TEXT, "
                + COL_EVENT_CREATED_AT + " INTEGER NOT NULL"
                + ");";
        String createGuests = "CREATE TABLE " + TABLE_GUESTS + " ("
                + COL_GUEST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_GUEST_EVENT_ID + " INTEGER NOT NULL, "
                + COL_GUEST_NAME + " TEXT NOT NULL, "
                + COL_GUEST_TOKEN + " TEXT NOT NULL UNIQUE, "
                + COL_GUEST_RSVP_STATUS + " TEXT NOT NULL, "
                + COL_GUEST_MENU_CHOICE + " TEXT, "
                + COL_GUEST_PREFERENCE_NOTE + " TEXT, "
                + COL_GUEST_CREATED_AT + " INTEGER NOT NULL, "
                + COL_GUEST_CHECKED_IN_AT + " INTEGER, "
                + COL_GUEST_CHECKED_IN_LAT + " REAL, "
                + COL_GUEST_CHECKED_IN_LNG + " REAL"
                + ");";
        db.execSQL(createEvents);
        db.execSQL(createGuests);
        // Useful indices for token lookup and per-event list
        db.execSQL("CREATE INDEX idx_guests_eventId ON " + TABLE_GUESTS + "(" + COL_GUEST_EVENT_ID + ");");
        db.execSQL("CREATE INDEX idx_guests_token ON " + TABLE_GUESTS + "(" + COL_GUEST_TOKEN + ");");
    }
    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        // Simple strategy for demo: drop and recreate.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GUESTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        onCreate(db);
    }
    // -----------------------------
    // Seed data (optional)
    // -----------------------------
    public void ensureSeedDataIfEmpty() {
        if (countEvents() > 0) return;
        long now = System.currentTimeMillis();
        long sevenDays = 7L * 24L * 60L * 60L * 1000L;
        // Set demo time 19:00 local for "now + 7d"
        long base = now + sevenDays;
        long demoMillis = base - (base % (24L * 60L * 60L * 1000L)) + (19L * 60L * 60L * 1000L);
        Event e = new Event();
        e.title = "Rusdihari Launch Party";
        e.description = "A fun modern demo event for your first run.\n\nFeel free to delete or edit this later.";
        e.location = "Jakarta";
        e.datetimeMillis = demoMillis;
        e.coverUri = null;
        e.createdAt = now;
        insertEvent(e);
    }
    public int countEvents() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_EVENTS, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }
    // -----------------------------
    // Event CRUD
    // -----------------------------
    public long insertEvent(@NonNull Event event) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_EVENT_TITLE, safeTrim(event.title));
        cv.put(COL_EVENT_DESCRIPTION, safeTrim(event.description));
        cv.put(COL_EVENT_LOCATION, safeTrim(event.location));
        cv.put(COL_EVENT_DATETIME_MILLIS, event.datetimeMillis);
        cv.put(COL_EVENT_COVER_URI, event.coverUri != null ? event.coverUri : null);
        cv.put(COL_EVENT_CREATED_AT, event.createdAt > 0 ? event.createdAt : System.currentTimeMillis());
        return db.insert(TABLE_EVENTS, null, cv);
    }
    public boolean updateEvent(@NonNull Event event) {
        if (event.id <= 0) return false;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_EVENT_TITLE, safeTrim(event.title));
        cv.put(COL_EVENT_DESCRIPTION, safeTrim(event.description));
        cv.put(COL_EVENT_LOCATION, safeTrim(event.location));
        cv.put(COL_EVENT_DATETIME_MILLIS, event.datetimeMillis);
        cv.put(COL_EVENT_COVER_URI, event.coverUri != null ? event.coverUri : null);
        int rows = db.update(TABLE_EVENTS, cv, COL_EVENT_ID + "=?", new String[]{String.valueOf(event.id)});
        return rows > 0;
    }
    public boolean deleteEvent(long eventId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_GUESTS, COL_GUEST_EVENT_ID + "=?", new String[]{String.valueOf(eventId)});
        int rows = db.delete(TABLE_EVENTS, COL_EVENT_ID + "=?", new String[]{String.valueOf(eventId)});
        return rows > 0;
    }
    @Nullable
    public Event getEventById(long eventId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_EVENTS, null, COL_EVENT_ID + "=?",
                new String[]{String.valueOf(eventId)}, null, null, null);
        Event e = null;
        if (c.moveToFirst()) e = readEvent(c);
        c.close();
        return e;
    }
    @NonNull
    public List<Event> listEvents(@Nullable String query, @NonNull EventSortMode sortMode) {
        SQLiteDatabase db = getReadableDatabase();
        List<String> args = new ArrayList<>();
        String selection = null;
        if (query != null && query.trim().length() > 0) {
            String q = "%" + query.trim() + "%";
            selection = "(" + COL_EVENT_TITLE + " LIKE ? OR " + COL_EVENT_LOCATION + " LIKE ? OR " + COL_EVENT_DESCRIPTION + " LIKE ?)";
            args.add(q);
            args.add(q);
            args.add(q);
        }
        String orderBy;
        if (sortMode == EventSortMode.DATE_ASC) {
            orderBy = COL_EVENT_DATETIME_MILLIS + " ASC";
        } else if (sortMode == EventSortMode.TITLE_ASC) {
            orderBy = COL_EVENT_TITLE + " COLLATE NOCASE ASC";
        } else {
            orderBy = COL_EVENT_CREATED_AT + " DESC";
        }
        Cursor c = db.query(TABLE_EVENTS, null, selection,
                args.isEmpty() ? null : args.toArray(new String[0]),
                null, null, orderBy);
        List<Event> out = new ArrayList<>();
        while (c.moveToNext()) out.add(readEvent(c));
        c.close();
        return out;
    }
    // -----------------------------
    // Guest CRUD + queries
    // -----------------------------
    @NonNull
    public String generateToken() {
        // Shorter than UUID sometimes is nice, but keep UUID for uniqueness and simplicity.
        return UUID.randomUUID().toString();
    }
    public long insertGuest(@NonNull Guest guest) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_GUEST_EVENT_ID, guest.eventId);
        cv.put(COL_GUEST_NAME, safeTrim(guest.name));
        cv.put(COL_GUEST_TOKEN, safeTrim(guest.token));
        cv.put(COL_GUEST_RSVP_STATUS, safeTrim(guest.rsvpStatus));
        cv.put(COL_GUEST_MENU_CHOICE, safeTrim(guest.menuChoice));
        cv.put(COL_GUEST_PREFERENCE_NOTE, safeTrim(guest.preferenceNote));
        cv.put(COL_GUEST_CREATED_AT, guest.createdAt > 0 ? guest.createdAt : System.currentTimeMillis());
        if (guest.checkedInAt != null) cv.put(COL_GUEST_CHECKED_IN_AT, guest.checkedInAt);
        if (guest.checkedInLat != null) cv.put(COL_GUEST_CHECKED_IN_LAT, guest.checkedInLat);
        if (guest.checkedInLng != null) cv.put(COL_GUEST_CHECKED_IN_LNG, guest.checkedInLng);
        return db.insert(TABLE_GUESTS, null, cv);
    }
    @Nullable
    public Guest getGuestByToken(@NonNull String token) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_GUESTS, null, COL_GUEST_TOKEN + "=?",
                new String[]{token}, null, null, null);
        Guest g = null;
        if (c.moveToFirst()) g = readGuest(c);
        c.close();
        return g;
    }
    @NonNull
    public List<Guest> listGuestsForEvent(long eventId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_GUESTS, null, COL_GUEST_EVENT_ID + "=?",
                new String[]{String.valueOf(eventId)}, null, null,
                COL_GUEST_CREATED_AT + " DESC");
        List<Guest> out = new ArrayList<>();
        while (c.moveToNext()) out.add(readGuest(c));
        c.close();
        return out;
    }
    public boolean markCheckedIn(@NonNull String token, long checkedInAt, @Nullable Double lat, @Nullable Double lng) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_GUEST_CHECKED_IN_AT, checkedInAt);
        if (lat != null) cv.put(COL_GUEST_CHECKED_IN_LAT, lat);
        if (lng != null) cv.put(COL_GUEST_CHECKED_IN_LNG, lng);
        int rows = db.update(TABLE_GUESTS, cv, COL_GUEST_TOKEN + "=?", new String[]{token});
        return rows > 0;
    }
    public int countInvited(long eventId) {
        return countGuestsByWhere(COL_GUEST_EVENT_ID + "=?", new String[]{String.valueOf(eventId)});
    }
    public int countRsvpYes(long eventId) {
        return countGuestsByWhere(COL_GUEST_EVENT_ID + "=? AND " + COL_GUEST_RSVP_STATUS + "=?",
                new String[]{String.valueOf(eventId), RSVP_YES});
    }
    public int countCheckedIn(long eventId) {
        return countGuestsByWhere(COL_GUEST_EVENT_ID + "=? AND " + COL_GUEST_CHECKED_IN_AT + " IS NOT NULL",
                new String[]{String.valueOf(eventId)});
    }
    private int countGuestsByWhere(@NonNull String where, @NonNull String[] args) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_GUESTS + " WHERE " + where, args);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }
    // Join helper for Ticket screen (guest + event)
    @Nullable
    public TicketBundle getTicketBundleByToken(@NonNull String token) {
        Guest g = getGuestByToken(token);
        if (g == null) return null;
        Event e = getEventById(g.eventId);
        if (e == null) return null;
        TicketBundle b = new TicketBundle();
        b.guest = g;
        b.event = e;
        return b;
    }
    // -----------------------------
    // Cursor mappers
    // -----------------------------
    @NonNull
    private Event readEvent(@NonNull Cursor c) {
        Event e = new Event();
        e.id = c.getLong(c.getColumnIndexOrThrow(COL_EVENT_ID));
        e.title = c.getString(c.getColumnIndexOrThrow(COL_EVENT_TITLE));
        e.description = c.getString(c.getColumnIndexOrThrow(COL_EVENT_DESCRIPTION));
        e.location = c.getString(c.getColumnIndexOrThrow(COL_EVENT_LOCATION));
        e.datetimeMillis = c.getLong(c.getColumnIndexOrThrow(COL_EVENT_DATETIME_MILLIS));
        e.coverUri = c.getString(c.getColumnIndexOrThrow(COL_EVENT_COVER_URI));
        e.createdAt = c.getLong(c.getColumnIndexOrThrow(COL_EVENT_CREATED_AT));
        return e;
    }
    @NonNull
    private Guest readGuest(@NonNull Cursor c) {
        Guest g = new Guest();
        g.id = c.getLong(c.getColumnIndexOrThrow(COL_GUEST_ID));
        g.eventId = c.getLong(c.getColumnIndexOrThrow(COL_GUEST_EVENT_ID));
        g.name = c.getString(c.getColumnIndexOrThrow(COL_GUEST_NAME));
        g.token = c.getString(c.getColumnIndexOrThrow(COL_GUEST_TOKEN));
        g.rsvpStatus = c.getString(c.getColumnIndexOrThrow(COL_GUEST_RSVP_STATUS));
        g.menuChoice = c.getString(c.getColumnIndexOrThrow(COL_GUEST_MENU_CHOICE));
        g.preferenceNote = c.getString(c.getColumnIndexOrThrow(COL_GUEST_PREFERENCE_NOTE));
        g.createdAt = c.getLong(c.getColumnIndexOrThrow(COL_GUEST_CREATED_AT));
        int idxChecked = c.getColumnIndex(COL_GUEST_CHECKED_IN_AT);
        if (idxChecked >= 0 && !c.isNull(idxChecked)) g.checkedInAt = c.getLong(idxChecked);
        int idxLat = c.getColumnIndex(COL_GUEST_CHECKED_IN_LAT);
        if (idxLat >= 0 && !c.isNull(idxLat)) g.checkedInLat = c.getDouble(idxLat);
        int idxLng = c.getColumnIndex(COL_GUEST_CHECKED_IN_LNG);
        if (idxLng >= 0 && !c.isNull(idxLng)) g.checkedInLng = c.getDouble(idxLng);
        return g;
    }
    // -----------------------------
    // Utilities
    // -----------------------------
    @Nullable
    public static String safeTrim(@Nullable String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() == 0 ? null : t;
    }
    @NonNull
    public static String formatTokenShort(@NonNull String token) {
        // Show first 8 for human readability; token full is used for validation/QR.
        String t = token.replace("-", "");
        if (t.length() <= 8) return t;
        return t.substring(0, 8).toUpperCase(Locale.US);
    }
    @Nullable
    public static Uri parseUriNullable(@Nullable String uriString) {
        if (uriString == null) return null;
        try {
            return Uri.parse(uriString);
        } catch (Exception ex) {
            return null;
        }
    }
    // -----------------------------
    // Models (simple public fields)
    // -----------------------------
    public static class Event {
        public long id;
        public String title;
        public String description;
        public String location;
        public long datetimeMillis;
        public String coverUri;
        public long createdAt;
    }
    public static class Guest {
        public long id;
        public long eventId;
        public String name;
        public String token;
        public String rsvpStatus;
        public String menuChoice;
        public String preferenceNote;
        public long createdAt;
        public Long checkedInAt;
        public Double checkedInLat;
        public Double checkedInLng;
        public boolean isCheckedIn() {
            return checkedInAt != null && checkedInAt > 0;
        }
        public boolean isRsvpYes() {
            return RSVP_YES.equalsIgnoreCase(rsvpStatus);
        }
    }
    public static class TicketBundle {
        public Event event;
        public Guest guest;
    }
    /*
     * Extra notes to keep this file self-contained and easier to modify:
     * - If you want remote-friendly links, you'd need a backend or shared storage.
     * - If you want multiple admins/guests on different phones, SQLite-only is not enough.
     * - For now, token uniqueness is enforced at DB level (UNIQUE token).
     * - You can add migrations later by bumping DATABASE_VERSION and handling onUpgrade carefully.
     */
}