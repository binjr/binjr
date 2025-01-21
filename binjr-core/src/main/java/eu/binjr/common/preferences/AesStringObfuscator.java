/*
 * Copyright 2023-2025 Frederic Thevenet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.binjr.common.preferences;

import eu.binjr.common.io.AesHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;

public class AesStringObfuscator extends ObfuscatedString.Obfuscator {
    private static final Logger logger = LogManager.getLogger(AesStringObfuscator.class);
    private final SecretKey secretKey;

    public AesStringObfuscator(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public String deObfuscateString(String obfuscated) {
        try {
            if (obfuscated.isEmpty()) {
                return obfuscated;
            }
            return AesHelper.decrypt(obfuscated, this.secretKey);
        } catch (Exception e) {
            logger.error("Error while attempting to de-obfuscate string: " + e.getMessage());
            logger.debug(() -> "Stack trace", e);
            return "";
        }
    }

    public String obfuscateString(String clearText) {
        try {
            if (clearText.isEmpty()) {
                return clearText;
            }
            return AesHelper.encrypt(clearText, this.secretKey);
        } catch (Exception e) {
            logger.error("Error while attempting to obfuscate string: " + e.getMessage());
            logger.debug(() -> "Stack trace", e);
            return "";
        }
    }
}
