/*
 * Copyright 2023 Frederic Thevenet
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
import eu.binjr.common.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;


import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

public class AesKeyring extends ObservablePreferenceFactory {
    private static final Logger logger = Logger.create(AesKeyring.class);
    private static final String UNINITIALIZED = "uninitialized";

    protected AesKeyring(String storeKey) {
        super(storeKey);
    }

    private SecretKey createDefaultKey() {
        try {
            return AesHelper.generateKey(256);
        } catch (NoSuchAlgorithmException e) {
            logger.fatal("Failed to generated key: " + e.getMessage());
            logger.debug("Stack Trace", e);
            throw new RuntimeException(e);
        }
    }

    public ObservablePreference<SecretKey> secretKeyPreference(String storePath) {
        var p = new ObservablePreference<>(SecretKey.class, storePath, null, backingStore) {
            @Override
            protected Property<SecretKey> makeProperty(SecretKey value) {
                return new SimpleObjectProperty<>(value);
            }

            @Override
            protected SecretKey loadFromBackend() {
                var encoded = getBackingStore().get(getKey(), UNINITIALIZED);
                SecretKey decoded;
                if (!UNINITIALIZED.equals(encoded)) {
                    try {
                        decoded = AesHelper.getKeyFromEncoded(encoded);
                        return decoded;
                    } catch (Exception e){
                        logger.debug("Stack trace", e);
                        logger.warn("Failed to decode key for " + storePath + ": a new key will be initialized");
                    }
                } else {
                    logger.debug("Initializing new key instance " + storePath);
                }
                // Key is uninitialized or invalid: generating a new one.
                decoded = createDefaultKey();
                getBackingStore().put(getKey(), AesHelper.encodeSecretKey(decoded));
                return decoded;

            }

            @Override
            protected void saveToBackend(SecretKey value) {
                getBackingStore().put(getKey(), AesHelper.encodeSecretKey(value));
            }
        };
        storedItems.put(p.getKey(), p);
        return p;
    }

}
