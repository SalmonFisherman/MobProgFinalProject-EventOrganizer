package com.ergi.rusdihari.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ergi.rusdihari.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyTicketsAdapter extends RecyclerView.Adapter<MyTicketsAdapter.Holder> {

    public interface Listener {
        void onTicketClicked(@NonNull TicketRow row);
    }

    public static class TicketRow {
        public String token;
        public String eventTitle;
        public long eventWhenMillis;
        public boolean checkedIn;
        public boolean guestHasName;
    }

    private final List<TicketRow> items;
    private final Listener listener;

    private final SimpleDateFormat pretty =
            new SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault());

    public MyTicketsAdapter(@NonNull List<TicketRow> items, @NonNull Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_ticket, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        TicketRow row = items.get(position);

        holder.title.setText(row.eventTitle != null ? row.eventTitle : "Event");
        holder.subtitle.setText(pretty.format(new Date(row.eventWhenMillis)));

        String status;
        if (row.checkedIn) status = "Checked-in: YES";
        else status = "Checked-in: NO";

        if (!row.guestHasName) status += " • Name: pending (admin will fill)";
        holder.status.setText(status);

        holder.itemView.setOnClickListener(v -> listener.onTicketClicked(row));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView title, subtitle, status;

        Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.ticketTitleText);
            subtitle = itemView.findViewById(R.id.ticketSubtitleText);
            status = itemView.findViewById(R.id.ticketStatusText);
        }
    }
}