package com.ergi.rusdihari.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.ergi.rusdihari.AppDatabase;
import com.ergi.rusdihari.R;
import com.ergi.rusdihari.TicketActivity;
import com.ergi.rusdihari.utils.TicketStorage;

public class EnterCodeFragment extends Fragment {

    private AppDatabase database;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_enter_code, container, false);

        database = new AppDatabase(requireContext());

        EditText eventCodeInput = view.findViewById(R.id.event_code_input);
        Button joinEventButton = view.findViewById(R.id.join_event_button);

        joinEventButton.setOnClickListener(v -> {
            String code = eventCodeInput.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a code", Toast.LENGTH_SHORT).show();
                return;
            }

            Long eventId = parseEventIdFromCode(code);
            if (eventId == null || eventId <= 0) {
                Toast.makeText(getContext(), "Invalid code format. Use EVENT_ID:<number>", Toast.LENGTH_LONG).show();
                return;
            }

            AppDatabase.Event event = database.getEventById(eventId);
            if (event == null) {
                Toast.makeText(getContext(), "Event not found for this code", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create ticket (guest record) WITHOUT guest name (admin will fill later)
            String token = database.generateToken();

            AppDatabase.Guest g = new AppDatabase.Guest();
            g.eventId = eventId;
            g.name = null; // <- admin will set
            g.token = token;
            g.rsvpStatus = AppDatabase.RSVP_YES; // default YES (you can change)
            g.menuChoice = null;
            g.preferenceNote = null;
            g.createdAt = System.currentTimeMillis();

            long guestRowId = database.insertGuest(g);
            if (guestRowId <= 0) {
                Toast.makeText(getContext(), "Failed to join (maybe token conflict). Try again.", Toast.LENGTH_SHORT).show();
                return;
            }

            TicketStorage.saveToken(requireContext(), token);

            Toast.makeText(getContext(), "Joined! Ticket created.", Toast.LENGTH_SHORT).show();

            Intent i = new Intent(requireActivity(), TicketActivity.class);
            i.putExtra(TicketActivity.EXTRA_TOKEN, token);
            startActivity(i);
        });

        return view;
    }

    @Nullable
    private Long parseEventIdFromCode(@NonNull String code) {
        String c = code.trim();
        if (c.startsWith("EVENT_ID:")) {
            String rest = c.substring("EVENT_ID:".length()).trim();
            try {
                return Long.parseLong(rest);
            } catch (Exception ignored) {
                return null;
            }
        }

        // allow plain numeric id too
        try {
            return Long.parseLong(c);
        } catch (Exception ignored) {
            return null;
        }
    }
}