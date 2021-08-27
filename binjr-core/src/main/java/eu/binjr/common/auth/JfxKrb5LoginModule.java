/*
 *    Copyright 2017-2021 Frederic Thevenet
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

package eu.binjr.common.auth;

import com.sun.security.auth.module.Krb5LoginModule;
import eu.binjr.common.logging.Logger;
import eu.binjr.core.dialogs.Dialogs;
import javafx.application.Platform;
import javafx.stage.StageStyle;
import javafx.util.Pair;
import org.controlsfx.dialog.LoginDialog;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An extension of the Krb5LoginModule that displays a JavaFX dialog to obtain credentials from the end-user if need be.
 *
 * @author Frederic Thevenet
 */
public class JfxKrb5LoginModule extends Krb5LoginModule {
    private static final Logger logger = Logger.create(JfxKrb5LoginModule.class);
    private CredentialsEntry credentials = CredentialsEntry.EMPTY;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbacks -> {
            try {
                credentials = obtainCredentials(CredentialsEntry.copyOf(credentials));
                for (Callback callback : callbacks) {
                    if (callback instanceof NameCallback nc) {
                        nc.setName(credentials.getLogin());
                    } else if (callback instanceof PasswordCallback pc) {
                        pc.setPassword(credentials.getPwd());
                        credentials.clearPassword();
                    } else {
                        throw new UnsupportedCallbackException(callback, "Unknown Callback");
                    }
                }
            } catch (InterruptedException | TimeoutException e) {
                logger.error("An exception occurred while retrieving credentials - " + e.getMessage());
                if (logger.isDebugEnabled()) {
                    logger.debug("Stack trace", e);
                }
            }
        }, sharedState, options);
    }

    private synchronized CredentialsEntry obtainCredentials(CredentialsEntry credentialsEntry) throws InterruptedException, TimeoutException {
        if (credentialsEntry.isFilled()) {
            return credentialsEntry;
        }
        AsyncResult<CredentialsEntry> future = new AsyncResult<>();
        Platform.runLater(() -> {
            LoginDialog dlg = new LoginDialog(null, null);
            dlg.setHeaderText("Enter login credentials");
            dlg.setTitle("Login");
            dlg.initStyle(StageStyle.UTILITY);
            Dialogs.setAlwaysOnTop(dlg);
            Optional<Pair<String, String>> res = dlg.showAndWait();
            if (res.isPresent()) {
                CredentialsEntry newCreds = new CredentialsEntry(dlg.getResult().getKey(), dlg.getResult().getValue().toCharArray());
                future.put(newCreds);
            } else {
                future.put(CredentialsEntry.CANCELLED);
            }
        });
        return future.get();
    }

    private class AsyncResult<F> {
        private final CountDownLatch latch = new CountDownLatch(1);
        private F value;

        public boolean isDone() {
            return latch.getCount() == 0;
        }

        public F get() throws InterruptedException {
            latch.await();
            return value;
        }

        public F get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            if (latch.await(timeout, unit)) {
                return value;
            } else {
                throw new TimeoutException();
            }
        }

        private void put(F result) {
            value = result;
            latch.countDown();
        }
    }
}
