package com.beemdevelopment.aegis.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.beemdevelopment.aegis.BuildConfig;
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
    public static final Size RESOLUTION = new Size(1200, 1600);

    private final QrCodeAnalyzer.Listener _listener;

    private boolean _captureNext;
    private final Object _captureNextObj = new Object();
    private Bitmap _bitmapBuffer;
    private YuvToRgbConverter _converter;

    public QrCodeAnalyzer(Context context, QrCodeAnalyzer.Listener listener) {
        _listener = listener;

        if (BuildConfig.DEBUG) {
            _converter = new YuvToRgbConverter(context);
        }
    }

    @Override
    @SuppressLint("UnsafeExperimentalUsageError")
    public void analyze(@NonNull ImageProxy image) {
        int format = image.getFormat();
        if (format != YUV_420_888 && format != YUV_422_888 && format != YUV_444_888) {
            Log.e(TAG, String.format("Expected YUV format, got %d instead", format));
            image.close();
            return;
        }

        if (_converter != null) {
            synchronized (_captureNextObj) {
                if (_captureNext && _listener != null) {
                    Log.i(TAG, String.format("Capturing image format=%d, width=%d, height=%d, rot=%d", image.getFormat(), image.getWidth(), image.getHeight(), image.getImageInfo().getRotationDegrees()));

                    if (_bitmapBuffer == null) {
                        _bitmapBuffer = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
                    }

                    _converter.yuvToRgb(image.getImage(), _bitmapBuffer);
                    _listener.onImageCaptured(_bitmapBuffer);
                    _captureNext = false;
                    image.close();
                    return;
                }
            }
        }

        Log.i(TAG, String.format("Analyzing image format=%d, width=%d, height=%d, rot=%d", image.getFormat(), image.getWidth(), image.getHeight(), image.getImageInfo().getRotationDegrees()));

        ByteBuffer buf = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[buf.remaining()];
        buf.get(data);
        buf.rewind();

        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                data, image.getWidth(), image.getHeight(), 0, 0, image.getWidth(), image.getHeight(), false
        );

        QRCodeReader reader = new QRCodeReader();
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            Result result = reader.decode(bitmap);
            Log.i(TAG, "QR code detected!");
            if (_listener != null) {
                new Handler(Looper.getMainLooper()).post(() -> _listener.onQrCodeDetected(result));
            }
        } catch (ChecksumException | FormatException | NotFoundException e) {
            Log.e(TAG, String.format("Error detecting QR code: %s", e));
        } finally {
            image.close();
        }
    }

    public void setCaptureNext(boolean captureNext) {
        synchronized (_captureNextObj) {
            _captureNext = captureNext;
        }
    }

    public interface Listener {
        void onQrCodeDetected(Result result);
        void onImageCaptured(Bitmap bitmap);
    }

    // source: https://github.com/android/camera-samples/blob/main/CameraUtils/lib/src/main/java/com/example/android/camera/utils/YuvToRgbConverter.kt
    private static class YuvToRgbConverter {
        private final RenderScript rs;
        private final ScriptIntrinsicYuvToRGB scriptYuvToRgb;

        private int pixelCount = -1;
        private byte[] yuvBuffer;
        private Allocation inputAllocation;
        private Allocation outputAllocation;

        YuvToRgbConverter(Context context) {
            rs = RenderScript.create(context);
            scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
        }

        public void yuvToRgb(Image image, Bitmap output) {
            // Ensure that the intermediate output byte buffer is allocated
            if (yuvBuffer == null) {
                pixelCount = image.getCropRect().width() * image.getCropRect().height();
                // Bits per pixel is an average for the whole image, so it's useful to compute the size
                // of the full buffer but should not be used to determine pixel offsets
                int pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888);
                yuvBuffer = new byte[pixelCount * pixelSizeBits / 8];
            }

            // Get the YUV data in byte array form using NV21 format
            imageToByteArray(image, yuvBuffer);

            // Ensure that the RenderScript inputs and outputs are allocated
            if (inputAllocation == null) {
                // Explicitly create an element with type NV21, since that's the pixel format we use
                Type elemType = new Type.Builder(rs, Element.YUV(rs)).setYuvFormat(ImageFormat.NV21).create();
                inputAllocation = Allocation.createSized(rs, elemType.getElement(), yuvBuffer.length);
            }
            if (outputAllocation == null) {
                outputAllocation = Allocation.createFromBitmap(rs, output);
            }

            // Convert NV21 format YUV to RGB
            inputAllocation.copyFrom(yuvBuffer);
            scriptYuvToRgb.setInput(inputAllocation);
            scriptYuvToRgb.forEach(outputAllocation);
            outputAllocation.copyTo(output);
        }

        private void imageToByteArray(Image image, byte[] outputBuffer) {
            if (image.getFormat() != ImageFormat.YUV_420_888) {
                throw new RuntimeException(String.format("Unexpected format: %d", image.getFormat()));
            }

            Rect imageCrop = image.getCropRect();
            Image.Plane[] imagePlanes = image.getPlanes();

            for (int planeIndex = 0; planeIndex < imagePlanes.length; planeIndex++) {
                Image.Plane plane = imagePlanes[planeIndex];
                // How many values are read in input for each output value written
                // Only the Y plane has a value for every pixel, U and V have half the resolution i.e.
                //
                // Y Plane            U Plane    V Plane
                // ===============    =======    =======
                // Y Y Y Y Y Y Y Y    U U U U    V V V V
                // Y Y Y Y Y Y Y Y    U U U U    V V V V
                // Y Y Y Y Y Y Y Y    U U U U    V V V V
                // Y Y Y Y Y Y Y Y    U U U U    V V V V
                // Y Y Y Y Y Y Y Y
                // Y Y Y Y Y Y Y Y
                // Y Y Y Y Y Y Y Y
                int outputStride;

                // The index in the output buffer the next value will be written at
                // For Y it's zero, for U and V we start at the end of Y and interleave them i.e.
                //
                // First chunk        Second chunk
                // ===============    ===============
                // Y Y Y Y Y Y Y Y    V U V U V U V U
                // Y Y Y Y Y Y Y Y    V U V U V U V U
                // Y Y Y Y Y Y Y Y    V U V U V U V U
                // Y Y Y Y Y Y Y Y    V U V U V U V U
                // Y Y Y Y Y Y Y Y
                // Y Y Y Y Y Y Y Y
                // Y Y Y Y Y Y Y Y
                int outputOffset;

                switch (planeIndex) {
                    case 0:
                        outputStride = 1;
                        outputOffset = 0;
                        break;
                    case 1:
                        outputStride = 2;
                        // For NV21 format, U is in odd-numbered indices
                        outputOffset = pixelCount + 1;
                        break;
                    case 2:
                        outputStride = 2;
                        // For NV21 format, V is in even-numbered indices
                        outputOffset = pixelCount;
                        break;
                    default:
                        throw new RuntimeException("Image contains more than 3 planes, something strange is going on");
                }

                ByteBuffer planeBuffer = plane.getBuffer();
                int rowStride = plane.getRowStride();
                int pixelStride = plane.getPixelStride();

                // We have to divide the width and height by two if it's not the Y plane
                Rect planeCrop = imageCrop;
                if (planeIndex != 0) {
                    planeCrop = new Rect(
                            imageCrop.left / 2,
                            imageCrop.top / 2,
                            imageCrop.right / 2,
                            imageCrop.bottom / 2
                    );
                }

                int planeWidth = planeCrop.width();
                int planeHeight = planeCrop.height();

                // Intermediate buffer used to store the bytes of each row
                byte[] rowBuffer = new byte[plane.getRowStride()];

                // Size of each row in bytes
                int rowLength;
                if (pixelStride == 1 && outputStride == 1) {
                    rowLength = planeWidth;
                } else {
                    // Take into account that the stride may include data from pixels other than this
                    // particular plane and row, and that could be between pixels and not after every
                    // pixel:
                    //
                    // |---- Pixel stride ----|                    Row ends here --> |
                    // | Pixel 1 | Other Data | Pixel 2 | Other Data | ... | Pixel N |
                    //
                    // We need to get (N-1) * (pixel stride bytes) per row + 1 byte for the last pixel
                    rowLength = (planeWidth - 1) * pixelStride + 1;
                }

                for (int row = 0; row < planeHeight; row++) {
                    // Move buffer position to the beginning of this row
                    planeBuffer.position(
                            (row + planeCrop.top) * rowStride + planeCrop.left * pixelStride);

                    if (pixelStride == 1 && outputStride == 1) {
                        // When there is a single stride value for pixel and output, we can just copy
                        // the entire row in a single step
                        planeBuffer.get(outputBuffer, outputOffset, rowLength);
                        outputOffset += rowLength;
                    } else {
                        // When either pixel or output have a stride > 1 we must copy pixel by pixel
                        planeBuffer.get(rowBuffer, 0, rowLength);
                        for (int col = 0; col < planeWidth; col++) {
                            outputBuffer[outputOffset] = rowBuffer[col * pixelStride];
                            outputOffset += outputStride;
                        }
                    }
                }
            }
        }
    }
}
