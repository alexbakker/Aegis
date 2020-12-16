package com.beemdevelopment.aegis.crypto;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CryptoUtilsTest {
    @Test
    public void testToBytes() {
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < 1024; i++) {
            input.append("\uD83D\uDE0B");

            char[] chars = input.toString().toCharArray();
            byte[] bytes = CryptoUtils.toBytes(chars);

            String s = new String(bytes, StandardCharsets.UTF_8);
            assertEquals(input.toString(), s);
            assertArrayEquals(chars, s.toCharArray());
        }
    }
}
