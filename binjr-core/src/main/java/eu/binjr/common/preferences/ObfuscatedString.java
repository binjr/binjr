/*
 *    Copyright 2022-2025 Frederic Thevenet
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

import java.util.Objects;

/**
 * Encapsulates a String and obfuscates its content.
 * <p>
 * <b>Remark:</b> if an empty string is used to initialize the {@link ObfuscatedString}, then it will not be obfuscated (i.e.
 * its obfuscated value will also be an empty string).
 *
 * @author Frederic Thevenet
 */
public class ObfuscatedString {
    private static final Logger logger = LogManager.getLogger(ObfuscatedString.class);

    private final String obfuscated;
    private final Obfuscator obfuscator;

    private ObfuscatedString(String obfuscated, Obfuscator obfuscator) {
        Objects.requireNonNull(obfuscated, "Cannot create an ObfuscatedString instance from a null value");
        Objects.requireNonNull(obfuscator, "factory cannot be null");
        this.obfuscator = obfuscator;
        this.obfuscated = obfuscated;
    }

    @Override
    public String toString() {
        return "************";
    }

    /**
     * Returns the plain text representation of the encapsulated string
     *
     * @return the plain text representation of the encapsulated string
     */
    public String toPlainText() {
        return obfuscator.deObfuscateString(obfuscated);
    }

    /**
     * Returns the obfuscated representation of the encapsulated string
     *
     * @return the obfuscated representation of the encapsulated string
     */
    public String getObfuscated() {
        return obfuscated;
    }

    public abstract static class Obfuscator {

        /**
         * Returns an {@link ObfuscatedString} instance that stores the value of the provided String.
         * <p>
         * <b>Remark:</b> if an empty string is used to initialize the {@link ObfuscatedString}, then it will not be obfuscated (i.e.
         * its obfuscated value will also be an empty string).
         *
         * @param plainText The value stored by the {@link ObfuscatedString} instance.
         * @return an {@link ObfuscatedString} instance that stores the value of the provided String.
         */
        public ObfuscatedString fromPlainText(String plainText) {
            return fromObfuscatedText(obfuscateString(plainText));
        }

        public ObfuscatedString fromPlainText(char[] plainText) {
            return fromObfuscatedText(obfuscateString(new String(plainText)));
        }

        public ObfuscatedString fromObfuscatedText(String obfuscated) {
            return new ObfuscatedString(obfuscated, this);
        }


        public abstract String deObfuscateString(String obfuscated);

        public abstract String obfuscateString(String clearText);

    }
}
