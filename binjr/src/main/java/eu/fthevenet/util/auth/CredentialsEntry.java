/*
 *    Copyright 2017 Frederic Thevenet
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
 *
 */

package eu.fthevenet.util.auth;

/**
 * Defines an immutable set of credentials
 *
 * @author Frederic Thevenet
 */
public class CredentialsEntry {
    public static CredentialsEntry EMPTY = new CredentialsEntry("", new char[0], false);
    public static CredentialsEntry CANCELLED = new CredentialsEntry("", new char[0], true);
    private final String login;
    private final char[] pwd;
    private final boolean filled;

    public CredentialsEntry(String login, char[] password) {
        this(login, password, true);
    }

    public static CredentialsEntry copyOf(CredentialsEntry credentials) {
        return new CredentialsEntry(credentials.login, credentials.pwd, credentials.filled);
    }

    private CredentialsEntry(String login, char[] password, boolean filled) {
        this.login = login;
        this.pwd = password;
        this.filled = filled;
    }

    public char[] getPwd() {
        return pwd;
    }

    public String getLogin() {
        return login;
    }

    public boolean isFilled() {
        return filled;
    }

    public void clearPassword() {
        for (int i = 0; i < pwd.length; i++) {
            pwd[i] = '#';
        }
    }
}
