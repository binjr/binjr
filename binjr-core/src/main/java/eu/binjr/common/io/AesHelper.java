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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

public class AesHelper {

    private static final String AES = "AES";
    public static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final String PBKDF_2_WITH_HMAC_SHA_256 = "PBKDF2WithHmacSHA256";
    private static final int IV_LEN = 16;
    private static final Logger logger = LogManager.getLogger(AesHelper.class);


    public static String encrypt(String input, SecretKey key)
            throws GeneralSecurityException {
        var params = generateIv();
        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        cipher.init(Cipher.ENCRYPT_MODE, key, params);
        byte[] cipherText = cipher.doFinal(input.getBytes());
        return Base64.getEncoder().encodeToString(IOUtils.concat(params.getIV(), cipherText));
    }

    public static String decrypt(String input, SecretKey key) throws GeneralSecurityException {
        var b = IOUtils.split(Base64.getDecoder().decode(input), IV_LEN);
        var params = new GCMParameterSpec(IV_LEN * 8, b.first());
        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        cipher.init(Cipher.DECRYPT_MODE, key, params);
        byte[] plainText = cipher.doFinal(b.second());
        return new String(plainText);
    }

    public static boolean validateKey(SecretKey key) {
        try {
            var params = generateIv();
            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            cipher.init(Cipher.DECRYPT_MODE, key, params);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException |
                 InvalidKeyException e) {
            logger.error("Cannot initialize Cipher with provided key:" + e.getMessage());
            logger.debug("Stack trace", e);
            return false;
        }
        return true;
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
        return encodeSecretKey(generateKey(n));
    }

    public static String encodeSecretKey(SecretKey secretKey) {
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
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

    public static GCMParameterSpec generateIv() {
        byte[] iv = new byte[IV_LEN];
        new SecureRandom().nextBytes(iv);
        return new GCMParameterSpec(IV_LEN * 8, iv);
    }
}
