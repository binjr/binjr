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

package eu.binjr.common.io;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

public class SSLContextUtils {

    public enum PlatformKeyStore {
        NONE("none"),
        WINDOWS_ROOT("Windows-ROOT"),
        WINDOWS_MY("Windows-MY"),
        MACOS_KEYCHAIN("KeychainStore");

        private final String name;

        PlatformKeyStore(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static SSLContext withPlatformKeystore() throws SSLCustomContextException {
        var OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (OS_NAME.startsWith("windows")) {
            return withKeystore(PlatformKeyStore.WINDOWS_ROOT);
        }
        if (OS_NAME.startsWith("mac")) {
            return withKeystore(PlatformKeyStore.MACOS_KEYCHAIN);
        }
        return withKeystore(PlatformKeyStore.NONE);
    }

    public static SSLContext withKeystore(PlatformKeyStore keyStore) throws SSLCustomContextException {
        try {
            var sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustManagers = null;
            if (keyStore != PlatformKeyStore.NONE) {
                var tks = KeyStore.getInstance(keyStore.getName());
                tks.load(null, null);
                var tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmFactory.init(tks);
                trustManagers = tmFactory.getTrustManagers();
            }
            sslContext.init(null, trustManagers, null);
            return sslContext;
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | KeyManagementException |
                 IOException e) {
            throw new SSLCustomContextException("Failed to create custom SSL context: " + e.getMessage(), e);
        }
    }


}
