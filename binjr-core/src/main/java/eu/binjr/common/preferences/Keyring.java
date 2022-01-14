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

import eu.binjr.common.io.AesHelper;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

final class Keyring extends ObservablePreferenceFactory {
    private static final Logger logger = LogManager.getLogger(Keyring.class);
   private final ObservablePreference<String> masterKey = keyPreference("application_id", "uninitialized");

    private Keyring() {
        super("binjr/local");
    }

    static Keyring getInstance() {
        return Keyring.KeyringHolder.instance;
    }

    SecretKey getMasterKey(){
        return AesHelper.getKeyFromEncoded(masterKey.get());
    }

    private String createDefaultKey() {
        try {
            return AesHelper.generateEncodedKey(256);
        } catch (NoSuchAlgorithmException e) {
            logger.fatal("Failed to generated master key: " + e.getMessage());
            logger.debug("Stack Trace", e);
            return "";
        }
    }

    private ObservablePreference<String> keyPreference(String key, String defaultValue) {
        var p = new ObservablePreference<>(String.class, key, defaultValue, backingStore) {
            @Override
            protected Property<String> makeProperty(String value) {
                return new SimpleStringProperty(value);
            }

            @Override
            protected String loadFromBackend() {
                var k = getBackingStore().get(getKey(), getDefaultValue());
                 if("uninitialized".equals(k)){
                     k = createDefaultKey();
                     getBackingStore().put(getKey(), k);
                 }
                 return k;
            }

            @Override
            protected void saveToBackend(String value) {
                getBackingStore().put(getKey(), value);
            }
        };
        storedItems.put(p.getKey(), p);
        return p;
    }

    private static class KeyringHolder {
        private final static Keyring instance = new Keyring();
    }
}
