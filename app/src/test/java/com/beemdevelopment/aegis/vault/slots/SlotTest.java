package com.beemdevelopment.aegis.vault.slots;

import com.beemdevelopment.aegis.crypto.CryptParameters;
import com.beemdevelopment.aegis.crypto.CryptoUtils;
import com.beemdevelopment.aegis.crypto.MasterKey;
import com.beemdevelopment.aegis.crypto.SCryptParameters;
import com.beemdevelopment.aegis.encoding.Hex;
import com.beemdevelopment.aegis.util.IOUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class SlotTest {
    private MasterKey _masterKey;

    @Before
    public void init() {
        _masterKey = MasterKey.generate();
    }

    @Test
    public void testRawSlotCrypto() throws
            InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            InvalidKeyException, NoSuchPaddingException,
            SlotException, SlotIntegrityException {
        RawSlot slot = new RawSlot();
        SecretKey rawKey = CryptoUtils.generateKey();
        Cipher cipher = CryptoUtils.createEncryptCipher(rawKey);
        slot.setKey(_masterKey, cipher);

        cipher = slot.createDecryptCipher(rawKey);
        MasterKey decryptedKey = slot.getKey(cipher);

        assertArrayEquals(_masterKey.getBytes(), decryptedKey.getBytes());
    }

    @Test
    public void testPasswordSlotCrypto() throws
            InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            InvalidKeyException, NoSuchPaddingException,
            SlotException, SlotIntegrityException {
        final char[] password = "test".toCharArray();
        final SCryptParameters scryptParams = new SCryptParameters(
                CryptoUtils.CRYPTO_SCRYPT_N,
                CryptoUtils.CRYPTO_SCRYPT_p,
                CryptoUtils.CRYPTO_SCRYPT_r,
                new byte[CryptoUtils.CRYPTO_AEAD_KEY_SIZE]
        );

        PasswordSlot slot = new PasswordSlot();
        SecretKey passwordKey = slot.deriveKey(password, scryptParams);
        Cipher cipher = CryptoUtils.createEncryptCipher(passwordKey);
        slot.setKey(_masterKey, cipher);

        cipher = slot.createDecryptCipher(passwordKey);
        MasterKey decryptedKey = slot.getKey(cipher);

        assertArrayEquals(_masterKey.getBytes(), decryptedKey.getBytes());
    }

    @Test
    public void testPasswordSlotParse() throws IOException, JSONException, SlotException {
        PasswordSlot exSlot = new PasswordSlot(
                UUID.fromString("ac3f4ddf-4c90-44b4-883d-f2aebd3618ae"),
                Hex.decode("b29b119e66c0f3cc2e8478e3e13a7da085e466d6c25a05dff73bbe9de6083f4f"),
                new CryptParameters(
                        Hex.decode("2f76e670ec575437fceb0863"),
                        Hex.decode("a05c88a06c8b37c3010fadfaba6a5387")),
                new SCryptParameters(32768, 8, 1, Hex.decode("5ad4ee5ea0689b0d81f78076f0b0cf82aa492b23e41cf8b90ee63043bd78563f")),
                true
        );

        try (InputStream stream = getClass().getResourceAsStream("password.json")) {
            byte[] bytes = IOUtils.readAll(stream);
            JSONObject obj = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            PasswordSlot slot = (PasswordSlot) Slot.fromJson(obj);

            assertEquals(exSlot.getUUID(), slot.getUUID());
            assertEquals(exSlot.isRepaired(), slot.isRepaired());

            //slot.getSCryptParameters().
        }
    }

    @Test
    public void testSlotIntegrity() throws
            InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            InvalidKeyException, NoSuchPaddingException,
            SlotException, SlotIntegrityException {
        RawSlot slot = new RawSlot();
        SecretKey rawKey = CryptoUtils.generateKey();
        Cipher cipher = CryptoUtils.createEncryptCipher(rawKey);
        slot.setKey(_masterKey, cipher);

        // try to decrypt with good key/ciphertext first
        final Cipher decryptCipher = slot.createDecryptCipher(rawKey);
        slot.getKey(decryptCipher);

        // garble the first byte of the key and try to decrypt
        byte[] garbledKeyBytes = rawKey.getEncoded();
        garbledKeyBytes[0] = (byte) ~garbledKeyBytes[0];
        SecretKey garbledKey = new SecretKeySpec(garbledKeyBytes, "AES");
        final Cipher garbledDecryptCipher = slot.createDecryptCipher(garbledKey);
        assertThrows(SlotIntegrityException.class, () -> slot.getKey(garbledDecryptCipher));

        // garble the first byte of the ciphertext and try to decrypt
        byte[] garbledCiphertext = slot.getEncryptedMasterKey();
        garbledCiphertext[0] = (byte) ~garbledCiphertext[0];
        assertThrows(SlotIntegrityException.class, () -> slot.getKey(decryptCipher));
    }
}
