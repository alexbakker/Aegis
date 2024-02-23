package com.beemdevelopment.aegis.helpers;

import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.common.collect.Sets;

import java.util.List;

import zxingcpp.BarcodeReader;

public class QrCodeAnalyzer implements ImageAnalysis.Analyzer {
    public static final Size RESOLUTION = new Size(1200, 1600);

    private final QrCodeAnalyzer.Listener _listener;
    private final BarcodeReader _reader;

    public QrCodeAnalyzer(QrCodeAnalyzer.Listener listener) {
        _listener = listener;

        BarcodeReader.Options opts = new BarcodeReader.Options();
        opts.setFormats(Sets.newHashSet(BarcodeReader.Format.QR_CODE));
        opts.setTryInvert(true);
        opts.setTryDownscale(true);
        _reader = new BarcodeReader();
        _reader.setOptions(opts);
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        try {
            List<BarcodeReader.Result> results = _reader.read(image);
            if (_listener != null && results.size() > 0) {
                _listener.onQrCodeDetected(results.get(0).getText());
            }
        } finally {
            image.close();
        }
    }

    public interface Listener {
        void onQrCodeDetected(String qrText);
    }
}
