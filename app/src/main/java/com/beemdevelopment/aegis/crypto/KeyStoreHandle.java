package com.beemdevelopment.aegis.crypto;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import static android.content.pm.PackageManager.FEATURE_STRONGBOX_KEYSTORE;

public class KeyStoreHandle {
    private final KeyStore _keyStore;
    private static final String STORE_NAME = "AndroidKeyStore";

    public KeyStoreHandle() throws KeyStoreHandleException {
        try {
            _keyStore = KeyStore.getInstance(STORE_NAME);
            _keyStore.load(null);
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new KeyStoreHandleException(e);
        }
    }

    public boolean containsKey(String id) throws KeyStoreHandleException {
        try {
            return _keyStore.containsAlias(id);
        } catch (KeyStoreException e) {
            throw new KeyStoreHandleException(e);
        }
    }

    public SecretKey generateKey(Context context, String id) throws KeyStoreHandleException {
        if (!isSupported()) {
            throw new KeyStoreHandleException("Symmetric KeyStore keys are not supported in this version of Android");
        }

        try {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE_NAME);
            KeyGenParameterSpec.Builder specBuilder = new KeyGenParameterSpec.Builder(id,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(true)
                    .setRandomizedEncryptionRequired(true)
                    .setKeySize(CryptoUtils.CRYPTO_AEAD_KEY_SIZE * 8);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    && context.getPackageManager().hasSystemFeature(FEATURE_STRONGBOX_KEYSTORE)
                    && !isStrongBoxBugPresent()) {
                specBuilder.setIsStrongBoxBacked(true);
            }

            generator.init(specBuilder.build());
            return generator.generateKey();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            throw new KeyStoreHandleException(e);
        }
    }

    public SecretKey getKey(String id) throws KeyStoreHandleException {
        SecretKey key;

        try {
            key = (SecretKey) _keyStore.getKey(id, null);
        } catch (UnrecoverableKeyException e) {
            return null;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            throw new KeyStoreHandleException(e);
        }

        if (isSupported() && isKeyPermanentlyInvalidated(key)) {
            return null;
        }

        return key;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static boolean isKeyPermanentlyInvalidated(SecretKey key) {
        // try to initialize a dummy cipher
        // and see if KeyPermanentlyInvalidatedException is thrown
        try {
            Cipher cipher = Cipher.getInstance(CryptoUtils.CRYPTO_AEAD);
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            // apparently KitKat doesn't like KeyPermanentlyInvalidatedException, even when guarded with a version check
            // it will throw a java.lang.VerifyError when its listed in a 'catch' statement
            // so instead, check for it here
            if (e instanceof KeyPermanentlyInvalidatedException) {
                return true;
            }
            throw new RuntimeException(e);
        }

        return false;
    }

    public void deleteKey(String id) throws KeyStoreHandleException {
        try {
            _keyStore.deleteEntry(id);
        } catch (KeyStoreException e) {
            throw new KeyStoreHandleException(e);
        }
    }

    public void clear() throws KeyStoreHandleException {
        try {
            for (String alias : Collections.list(_keyStore.aliases())) {
                deleteKey(alias);
            }
        } catch (KeyStoreException e) {
            throw new KeyStoreHandleException(e);
        }
    }

    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * Reports whether CVE-2019-9465 is present on this device.
     * If system property "ro.build.version.security_patch" is not set, this will also returns true.
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    private static boolean isStrongBoxBugPresent() {
        final TimeZone timeZone = TimeZone.getTimeZone("UTC");
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(timeZone);

        final Calendar safePatchDate = new GregorianCalendar(timeZone);
        safePatchDate.clear();
        safePatchDate.set(2019, 11, 5);

        Date date = null;
        try {
            date = format.parse(Build.VERSION.SECURITY_PATCH);
        } catch (ParseException ignored) {

        }

        if (date == null) {
            return true;
        }

        Calendar patchDate = new GregorianCalendar(timeZone);
        patchDate.setTime(date);
        return patchDate.getTimeInMillis() < safePatchDate.getTimeInMillis();
    }
}
