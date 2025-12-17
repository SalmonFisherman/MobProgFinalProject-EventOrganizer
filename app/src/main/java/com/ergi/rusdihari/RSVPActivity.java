package com.ergi.rusdihari;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.ergi.rusdihari.models.Event;
import com.ergi.rusdihari.utils.EventStorage;

public class RSVPActivity extends AppCompatActivity {

    TextView tvEventName;
    RadioGroup rgRsvp;
    Button btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rsvpactivity);

        tvEventName = findViewById(R.id.tvEventName);
        rgRsvp = findViewById(R.id.rgRsvp);
        btnConfirm = findViewById(R.id.btnConfirm);

        String eventName = getIntent().getStringExtra("EVENT_NAME");
        tvEventName.setText(eventName);

        btnConfirm.setOnClickListener(v -> {
            int selectedId = rgRsvp.getCheckedRadioButtonId();

            if (selectedId == -1) {
                Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selected = findViewById(selectedId);
            String response = selected.getText().toString();

            Toast.makeText(this, "RSVP: " + response, Toast.LENGTH_SHORT).show();

            if (response.equals("Yes")) {
                Event event = new Event(
                        tvEventName.getText().toString(),
                        "December 10, 2025" // temp value
                );

                EventStorage.saveEvent(this, event);

                Intent intent = new Intent(this, QRCodeActivity.class);
                intent.putExtra("EVENT_NAME", event.getEventName());
                startActivity(intent);
            } else {
                Toast.makeText(this, "RSVP saved", Toast.LENGTH_SHORT).show();
            }

            // Later: save RSVP locally or to Firebase
            finish();
        });
    }
}