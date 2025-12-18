package com.ergi.rusdihari.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ergi.rusdihari.AppDatabase;
import com.ergi.rusdihari.R;
import com.ergi.rusdihari.TicketActivity;
import com.ergi.rusdihari.adapter.MyTicketsAdapter;
import com.ergi.rusdihari.utils.TicketStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MyEventsFragment extends Fragment {

    private AppDatabase database;

    private RecyclerView ticketsRecyclerView;
    private TextView noEventsText;

    private MyTicketsAdapter adapter;
    private final List<MyTicketsAdapter.TicketRow> rows = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_events, container, false);

        database = new AppDatabase(requireContext());

        ticketsRecyclerView = view.findViewById(R.id.events_recycler_view);
        noEventsText = view.findViewById(R.id.no_events_text);

        ticketsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new MyTicketsAdapter(rows, row -> {
            Intent i = new Intent(getContext(), TicketActivity.class);
            i.putExtra(TicketActivity.EXTRA_TOKEN, row.token);
            startActivity(i);
        });

        ticketsRecyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTickets();
    }

    private void loadTickets() {
        rows.clear();

        Set<String> tokens = TicketStorage.getTokens(requireContext());
        for (String token : tokens) {
            AppDatabase.TicketBundle bundle = database.getTicketBundleByToken(token);
            if (bundle == null || bundle.event == null || bundle.guest == null) continue;

            MyTicketsAdapter.TicketRow r = new MyTicketsAdapter.TicketRow();
            r.token = token;
            r.eventTitle = bundle.event.title != null ? bundle.event.title : "Event";
            r.eventWhenMillis = bundle.event.datetimeMillis;
            r.checkedIn = bundle.guest.isCheckedIn();
            r.guestHasName = bundle.guest.hasName();

            rows.add(r);
        }

        if (rows.isEmpty()) {
            noEventsText.setVisibility(View.VISIBLE);
            ticketsRecyclerView.setVisibility(View.GONE);
        } else {
            noEventsText.setVisibility(View.GONE);
            ticketsRecyclerView.setVisibility(View.VISIBLE);
        }

        adapter.notifyDataSetChanged();
    }
}