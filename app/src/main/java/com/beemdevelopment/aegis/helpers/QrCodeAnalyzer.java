package com.beemdevelopment.aegis.helpers;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.nio.ByteBuffer;

import static android.graphics.ImageFormat.YUV_420_888;
import static android.graphics.ImageFormat.YUV_422_888;
import static android.graphics.ImageFormat.YUV_444_888;

public class QrCodeAnalyzer implements ImageAnalysis.Analyzer {
    private static final String TAG = QrCodeAnalyzer.class.getSimpleName();
    public static final Size RESOLUTION = new Size(1920, 1080);

    private final QrCodeAnalyzer.Listener _listener;

    public QrCodeAnalyzer(QrCodeAnalyzer.Listener listener) {
        _listener = listener;
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        int format = image.getFormat();
        if (format != YUV_420_888 && format != YUV_422_888 && format != YUV_444_888) {
            Log.e(TAG, String.format("Expected YUV format, got %d instead", format));
            image.close();
            return;
        }

        ByteBuffer buf = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[buf.remaining()];
        buf.get(data);
        buf.rewind();

        Rect cropRect = getCropRect(image.getWidth(), image.getHeight());
        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                data, image.getWidth(), image.getHeight(), cropRect.left, cropRect.top, cropRect.width(), cropRect.height(), false
        );

        QRCodeReader reader = new QRCodeReader();
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            Result result = reader.decode(bitmap);
            if (_listener != null) {
                new Handler(Looper.getMainLooper()).post(() -> _listener.onQrCodeDetected(result));
            }
        } catch (ChecksumException | FormatException | NotFoundException ignored) {

        } finally {
            image.close();
        }
    }

    public static Rect getCropRect(int width, int height) {
        int parts = 6;
        int xOff = width / parts;
        int yCenter = height / 2;
        int yOff = (xOff * (parts - 2)) / 2;
        return new Rect(xOff, yCenter - yOff, xOff * (parts - 1), yCenter + yOff);
    }

    public interface Listener {
        void onQrCodeDetected(Result result);
    }
}
