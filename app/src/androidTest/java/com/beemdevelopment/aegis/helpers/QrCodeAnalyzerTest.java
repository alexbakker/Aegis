package com.beemdevelopment.aegis.helpers;

import static android.graphics.ImageFormat.YUV_420_888;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Rect;
import android.media.Image;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.core.impl.utils.ExifData;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.beemdevelopment.aegis.AegisTest;
import com.beemdevelopment.aegis.util.IOUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

import dagger.hilt.android.testing.HiltAndroidTest;

@RunWith(AndroidJUnit4.class)
@HiltAndroidTest
@SmallTest
public class QrCodeAnalyzerTest extends AegisTest {
    @Test
    public void testScanQrCode() {
        final String expectedUri = "otpauth://totp/neo4j:Charlotte?secret=B33WS2ALPT34K4BNY24AYROE4M&issuer=neo4j&algorithm=SHA1&digits=6&period=30";

        boolean found = scan("qr.y.gz", 1600, 1200, 1600, expectedUri);
        assertTrue("QR code not found", found);
    }

    @Test
    public void testScanIssue802Code() {

        final String expectedUri = "otpauth://totp/Alice?secret=E54722XDV6I4C7PF5WGUZEM7IHEYCOB6&issuer=Google";
        boolean found = scan("qr.issue-802.y.gz", 4608, 3456, 4608, expectedUri);
        assertTrue("QR code not found", found);
    }

    @Test
    public void testScanStridedQrCode() {
        final String expectedUri = "otpauth://totp/neo4j:Charlotte?secret=B33WS2ALPT34K4BNY24AYROE4M&issuer=neo4j&algorithm=SHA1&digits=6&period=30";

        boolean found = scan("qr.strided.y.gz", 1840, 1380, 1840, expectedUri);
        assertFalse("QR code found", found);

        found = scan("qr.strided.y.gz", 1840, 1380, 1856, expectedUri);
        assertTrue("QR code not found", found);
    }

    private boolean scan(String fileName, int width, int height, int rowStride, String expectedUri) {
        AtomicBoolean found = new AtomicBoolean();
        QrCodeAnalyzer analyzer = new QrCodeAnalyzer(qrText -> {
            if (expectedUri != null) {
                assertEquals(expectedUri, qrText);
            }
            found.set(true);
        });

        FakeImageProxy imgProxy;
        try (InputStream inStream = getClass().getResourceAsStream(fileName);
             GZIPInputStream zipStream = new GZIPInputStream(inStream)) {
            imgProxy = new FakeImageProxy(IOUtils.readAll(zipStream), width, height, rowStride);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        analyzer.analyze(imgProxy);
        return found.get();
    }

    private static class FakePlaneProxy implements ImageProxy.PlaneProxy {
        private final byte[] _y;
        private final int _rowStride;

        public FakePlaneProxy(byte[] y, int rowStride) {
            _y = y;
            _rowStride = rowStride;
        }

        @Override
        public int getRowStride() {
            return _rowStride;
        }

        @Override
        public int getPixelStride() {
            return 1;
        }

        @NonNull
        @Override
        public ByteBuffer getBuffer() {
            return ByteBuffer.allocateDirect(_y.length).put(_y);
        }
    }

    private static class FakeImageProxy implements ImageProxy {
        private final byte[] _y;
        private final int _width;
        private final int _height;
        private final int _rowStride;

        public FakeImageProxy(byte[] y, int width, int height, int rowStride) {
            _y = y;
            _width = width;
            _height = height;
            _rowStride = rowStride;
        }

        @Override
        public void close() {

        }

        @NonNull
        @Override
        public Rect getCropRect() {
            return new Rect(0, 0, _width, _height);
        }

        @Override
        public void setCropRect(@Nullable @org.jetbrains.annotations.Nullable Rect rect) {

        }

        @Override
        public int getFormat() {
            return YUV_420_888;
        }

        @Override
        public int getHeight() {
            return _height;
        }

        @Override
        public int getWidth() {
            return _width;
        }

        @NonNull
        @Override
        public ImageProxy.PlaneProxy[] getPlanes() {
            return new PlaneProxy[]{new FakePlaneProxy(_y, _rowStride)};
        }

        @NonNull
        @Override
        public ImageInfo getImageInfo() {
            return new ImageInfo() {
                @NonNull
                @Override
                public TagBundle getTagBundle() {
                    return TagBundle.emptyBundle();
                }

                @Override
                public long getTimestamp() {
                    return 0;
                }

                @Override
                public int getRotationDegrees() {
                    return 90;
                }

                @Override
                public void populateExifData(@NonNull ExifData.Builder exifBuilder) {

                }
            };
        }

        @Nullable
        @Override
        public Image getImage() {
            return null;
        }
    }
}
