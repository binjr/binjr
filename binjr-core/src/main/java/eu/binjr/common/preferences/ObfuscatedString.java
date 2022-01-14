/*
 *    Copyright 2022 Frederic Thevenet
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

package eu.binjr.common.preferences;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

public class ObfuscatedString {
    private static final Logger logger = LogManager.getLogger(ObfuscatedString.class);

    private final String obfuscated;

    ObfuscatedString(String obfuscated) {
        this.obfuscated = obfuscated;
    }

    public static ObfuscatedString of(String plainText) {
        return new ObfuscatedString(obfuscateString(plainText));
    }

    @Override
    public String toString() {
        return "************";
    }

    public String toPlainText() {
        return deObfuscateString(obfuscated);
    }

    public String getObfuscated() {
        return obfuscated;
    }

    //TODO generate and store random key
    private static SecretKey KEY = AesHelper.getKeyFromPassword("foobarfoo", "salty");

    private static String deObfuscateString(String obfuscated) {
        try {
            if (obfuscated.isEmpty()) {
                return obfuscated;
            }
            return AesHelper.decrypt(obfuscated, KEY);
        } catch (Exception e) {
            logger.error("Error while attempting to de-obfuscate string: " + e.getMessage());
            logger.debug(() -> "Stack trace", e);
            return "";
        }

    }

    private static String obfuscateString(String clearText) {
        try {
            if (clearText.isEmpty()) {
                return clearText;
            }
            return AesHelper.encrypt(clearText, KEY);
        } catch (Exception e) {
            logger.error("Error while attempting to obfuscate string: " + e.getMessage());
            logger.debug(() -> "Stack trace", e);
            return "";
        }
    }
}
