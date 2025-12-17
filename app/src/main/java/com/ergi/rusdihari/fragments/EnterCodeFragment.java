package com.ergi.rusdihari.fragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.ergi.rusdihari.R;
import com.ergi.rusdihari.RSVPActivity;

public class EnterCodeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_enter_code, container, false);

        EditText eventCodeInput = view.findViewById(R.id.event_code_input);
        Button joinEventButton = view.findViewById(R.id.join_event_button);

        joinEventButton.setOnClickListener(v -> {
            String code = eventCodeInput.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a code", Toast.LENGTH_SHORT).show();
            }

            if (code.equalsIgnoreCase("PLAN123")) {
                Intent intent = new Intent(requireActivity(), RSVPActivity.class);
                intent.putExtra("EVENT_NAME", "Wedding Reception");
                intent.putExtra("EVENT_CODE", code);
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Invalid event code", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}