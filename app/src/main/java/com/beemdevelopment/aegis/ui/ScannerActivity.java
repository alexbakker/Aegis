package com.beemdevelopment.aegis.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.Theme;
import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.helpers.SquareFinderView;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.GoogleAuthInfoException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.util.Collections;

import me.dm7.barcodescanner.core.IViewFinder;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScannerActivity extends AegisActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView _scannerView;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        _scannerView = new ZXingScannerView(this) {
            @Override
            protected IViewFinder createViewFinderView(Context context) {
                return new SquareFinderView(context);
            }
        };
        _scannerView.setFormats(Collections.singletonList(BarcodeFormat.QR_CODE));
        setContentView(_scannerView);
    }

    @Override
    protected void setPreferredTheme(Theme theme) {
        setTheme(R.style.AppTheme_Fullscreen);
    }

    @Override
    public void onResume() {
        super.onResume();
        _scannerView.setResultHandler(this);
        _scannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        _scannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        try {
            // parse google auth uri
            GoogleAuthInfo info = GoogleAuthInfo.parseUri(rawResult.getText());
            DatabaseEntry entry = new DatabaseEntry(info);

            Intent intent = new Intent();
            intent.putExtra("entry", entry);
            setResult(RESULT_OK, intent);
            finish();
        } catch (GoogleAuthInfoException e) {
            Toast.makeText(this, getString(R.string.read_qr_error), Toast.LENGTH_SHORT).show();
        }

        _scannerView.resumeCameraPreview(this);
    }
}
