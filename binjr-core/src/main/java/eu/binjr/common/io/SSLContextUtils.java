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

package eu.binjr.common.io;

import eu.binjr.common.function.CheckedConsumer;
import eu.binjr.common.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

public class SSLContextUtils {
    private static final Logger logger = Logger.create(SSLContextUtils.class);

    public enum PlatformKeyStore {
        NONE(),
        AUTO(),
        WINDOWS_ALL("Windows-MY", "Windows-ROOT"),
        WINDOWS_ROOT("Windows-ROOT"),
        WINDOWS_MY("Windows-MY"),
        MACOS_ALL("KeychainStore", "KeychainStore-ROOT"),
        MACOS_KEYCHAIN("KeychainStore"),
        MACOS_KEYCHAIN_ROOT("KeychainStore-ROOT");

        private final String[] names;

        PlatformKeyStore(String... names) {
            this.names = names;
        }

        public String[] getNames() {
            return names;
        }
    }

    public static SSLContext withKeystore(PlatformKeyStore keyStore) throws SSLCustomContextException {
        try {
            var sslContext = SSLContext.getInstance("TLSv1.2");
            logger.debug(() -> "Using platform specific keystore: " + keyStore);
            TrustManager[] tms = switch (keyStore) {
                case NONE -> null;
                case AUTO -> {
                    var OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ROOT);
                    if (OS_NAME.startsWith("windows")) {
                        yield getTrustManager(PlatformKeyStore.WINDOWS_ALL);
                    }
                    if (OS_NAME.startsWith("mac")) {
                        yield getTrustManager(PlatformKeyStore.MACOS_ALL);
                    }
                    yield null;
                }
                default -> getTrustManager(keyStore);
            };
            sslContext.init(null, tms, null);
            return sslContext;
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | KeyManagementException |
                 IOException e) {
            throw new SSLCustomContextException("Failed to create custom SSL context: " + e.getMessage(), e);
        }
    }

    private static TrustManager[] getTrustManager(PlatformKeyStore keyStore)
            throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        List<X509TrustManager> x509TmList = new ArrayList<>();
        for (var ksName : keyStore.getNames()) {
            var tks = KeyStore.getInstance(ksName);
            tks.load(null, null);
            var tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmFactory.init(tks);
            for (var tm : tmFactory.getTrustManagers()) {
                if (tm instanceof X509TrustManager xTm) {
                    x509TmList.add(xTm);
                }
            }
        }
        return new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        doTrustCheck((tm) -> tm.checkClientTrusted(chain, authType));
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        doTrustCheck((tm) -> tm.checkServerTrusted(chain, authType));
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return x509TmList.stream()
                                .flatMap(tm -> Arrays.stream(tm.getAcceptedIssuers()))
                                .toArray(X509Certificate[]::new);
                    }

                    private void doTrustCheck(CheckedConsumer<X509TrustManager, CertificateException> checkDelegate)
                            throws CertificateException {
                        for (var tm : x509TmList) {
                            if (logger.isTraceEnabled()) {
                                Arrays.stream(tm.getAcceptedIssuers()).forEach(cert -> logger.trace(cert::toString));
                            }
                            try {
                                checkDelegate.accept(tm);
                                return;
                            } catch (CertificateException e) {
                                logger.debug(() -> "Unable to find certification path in current TrustManager. Keep trying");
                            }
                        }
                        throw new CertificateException("PKIX path building failed: unable to find valid certification " +
                                "path to requested target in any of the declared platform TrustManagers for: " + keyStore);
                    }
                }
        };
    }
}
