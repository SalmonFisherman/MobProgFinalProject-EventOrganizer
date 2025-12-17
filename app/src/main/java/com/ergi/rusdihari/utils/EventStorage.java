package com.ergi.rusdihari.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.ergi.rusdihari.models.Event;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class EventStorage {

    private static final String PREF_NAME = "my_events";
    private static final String EVENTS_KEY = "events";

    public static void saveEvent(Context context, Event event) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String eventsJson = prefs.getString(EVENTS_KEY, "[]");

        try {
            JSONArray jsonArray = new JSONArray(eventsJson);

            JSONObject obj = new JSONObject();
            obj.put("name", event.getEventName());
            obj.put("date", event.getEventDate());

            jsonArray.put(obj);

            prefs.edit().putString(EVENTS_KEY, jsonArray.toString()).apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Event> getEvents(Context context) {
        ArrayList<Event> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String eventsJson = prefs.getString(EVENTS_KEY, "[]");

        try {
            JSONArray jsonArray = new JSONArray(eventsJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                list.add(new Event(
                        obj.getString("name"),
                        obj.getString("date")
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return list;
    }
}
