package com.ergi.rusdihari;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.ergi.rusdihari.R;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class QRCodeActivity extends AppCompatActivity {

    ImageView imgQRCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);

        imgQRCode = findViewById(R.id.imgQRCode);

        String eventCode = getIntent().getStringExtra("EVENT_CODE");
        if (eventCode == null || eventCode.isEmpty()) {
            eventCode = "RUSDHARI-123";
        }

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
