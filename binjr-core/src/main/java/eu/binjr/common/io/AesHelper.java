/*
 *    Copyright 2022-2023 Frederic Thevenet
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.binjr.common.io;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

public class AesHelper {

    private static final String AES = "AES";
    private static final String AES_CBC_PKCS_5_PADDING = "AES/CBC/PKCS5Padding";
    private static final String PBKDF_2_WITH_HMAC_SHA_256 = "PBKDF2WithHmacSHA256";
    private static final int IV_LEN = 16;

    public static String encrypt(String input, SecretKey key)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(AES_CBC_PKCS_5_PADDING);
        var iv = generateIv();
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] cipherText = cipher.doFinal(input.getBytes());
        return Base64.getEncoder().encodeToString(IOUtils.concat(iv.getIV(), cipherText));
    }

    public static String decrypt(String input, SecretKey key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(AES_CBC_PKCS_5_PADDING);
        var b = IOUtils.split(Base64.getDecoder().decode(input), IV_LEN);
        var iv = new IvParameterSpec(b.first());
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] plainText = cipher.doFinal(b.second());
        return new String(plainText);
    }

    public static SecretKey getKeyFromEncoded(String encoded) {
        byte[] buffer = Base64.getDecoder().decode(encoded);
        return new SecretKeySpec(buffer, AES);
    }

    public static SecretKey generateKey(int n) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES);
        keyGenerator.init(n);
        return keyGenerator.generateKey();
    }

    public static String generateEncodedKey(int n) throws NoSuchAlgorithmException {
        return Base64.getEncoder().encodeToString(generateKey(n).getEncoded());
    }

    public static SecretKey deriveKeyFromPassword(String password, String salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        return deriveKeyFromPassword(password.toCharArray(), salt.getBytes(StandardCharsets.UTF_8));
    }

    public static SecretKey deriveKeyFromPassword(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF_2_WITH_HMAC_SHA_256);
        KeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), AES);
    }

    public static IvParameterSpec generateIv() {
        byte[] iv = new byte[IV_LEN];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }
}
