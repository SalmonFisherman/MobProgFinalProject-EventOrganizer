package com.ergi.rusdihari;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ergi.rusdihari.R;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class QRCodeActivity extends AppCompatActivity {

    ImageView imgQRCode;
    TextView tvEventName, tvEventCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);

        imgQRCode = findViewById(R.id.imgQRCode);

        tvEventName = findViewById(R.id.tvEventName);
        tvEventCode = findViewById(R.id.tvEventCode);

        String eventName = getIntent().getStringExtra("EVENT_NAME");
        if (eventName != null) {
            tvEventName.setText(eventName);
        }

        String eventCode = getIntent().getStringExtra("EVENT_CODE");
        if (eventCode == null || eventCode.isEmpty()) {
            eventCode = "RUSDHARI-123";
        }

        tvEventCode.setText(eventCode);

        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.encodeBitmap(
                    eventCode,
                    BarcodeFormat.QR_CODE,
                    600,
                    600
            );
            imgQRCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
