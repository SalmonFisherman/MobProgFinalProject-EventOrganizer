package com.ergi.rusdihari.fragments;

import com.ergi.rusdihari.QRCodeActivity;
import com.ergi.rusdihari.models.Event;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ergi.rusdihari.R;
import com.ergi.rusdihari.adapter.EventAdapter;
import com.ergi.rusdihari.utils.EventStorage;

import java.util.ArrayList;

public class MyEventsFragment extends Fragment {

    private RecyclerView eventsRecyclerView;
    private TextView noEventsText;
    private EventAdapter adapter;
    private ArrayList<Event> eventList = new ArrayList<>(); // Your list of events

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_events, container, false);

        eventsRecyclerView = view.findViewById(R.id.events_recycler_view);
        noEventsText = view.findViewById(R.id.no_events_text);

        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // TODO: Replace this with actual data fetching from a database or API
        loadEvents(); // We'll simulate loading events for now

        if (eventList.isEmpty()) {
            // Show "No Events" text and hide the RecyclerView
            noEventsText.setVisibility(View.VISIBLE);
            eventsRecyclerView.setVisibility(View.GONE);
        } else {
            // Show the RecyclerView and hide the "No Events" text
            noEventsText.setVisibility(View.GONE);
            eventsRecyclerView.setVisibility(View.VISIBLE);
            adapter = new EventAdapter(eventList, event -> {
                Intent intent = new Intent(getContext(), QRCodeActivity.class);
                intent.putExtra("EVENT_NAME", event.getEventName());
                startActivity(intent);
            });

            eventsRecyclerView.setAdapter(adapter);
        }

        return view;
    }

    private void loadEvents() {
        eventList.clear();
        eventList.addAll(EventStorage.getEvents(requireContext()));
    }
}