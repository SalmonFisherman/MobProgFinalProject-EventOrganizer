package com.ergi.rusdihari;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class QRCodeActivity extends AppCompatActivity {

    TextView tvEventName;
    ImageView imgQrCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qrcode);

        tvEventName = findViewById(R.id.tvEventName);
        imgQrCode = findViewById(R.id.imgQrCode);

        String eventName = getIntent().getStringExtra("EVENT_NAME");
        tvEventName.setText(eventName);
    }
}