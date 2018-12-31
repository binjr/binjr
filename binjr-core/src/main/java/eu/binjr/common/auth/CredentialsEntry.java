/*
 *    Copyright 2017-2018 Frederic Thevenet
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

/**
 * Defines an immutable set of credentials
 *
 * @author Frederic Thevenet
 */
public class CredentialsEntry {
    private final String login;
    private final char[] pwd;
    private final boolean filled;

    /**
     * An empty credential set.
     * <p>This specific credential set should be used to intend that no credentials where explicitly provided</p>
     */
    public static final CredentialsEntry EMPTY = new CredentialsEntry("", new char[0], false);

    /**
     * A canceled  credential set.
     * <p>This specific credential set should be used to intend that the query for explicit credentials was initiated but canceled</p>
     */
    public static final CredentialsEntry CANCELLED = new CredentialsEntry("", new char[0], true);

    /**
     * Initializes a new instance of the {@link CredentialsEntry} class
     *
     * @param login    the principle's login
     * @param password the principle's password
     */
    public CredentialsEntry(String login, char[] password) {
        this(login, password, true);
    }

    /**
     * Creates a deep clone of the provided {@link CredentialsEntry} instance.
     *
     * @param credentials the  {@link CredentialsEntry} instance to copy.
     * @return a deep clone of the provided {@link CredentialsEntry} instance.
     */
    public static CredentialsEntry copyOf(CredentialsEntry credentials) {
        return new CredentialsEntry(credentials.login, credentials.pwd, credentials.filled);
    }


    private CredentialsEntry(String login, char[] password, boolean filled) {
        this.login = login;
        this.pwd = password;
        this.filled = filled;
    }

    /**
     * Returns the principle's password
     *
     * @return the password
     */
    public char[] getPwd() {
        return pwd;
    }

    /**
     * Returns the principle's login
     *
     * @return the principle's login
     */
    public String getLogin() {
        return login;
    }

    /**
     * Returns true if the current set of credentials have been filled-in, false otherwise.
     *
     * @return true if the current set of credentials have been filled-in, false otherwise.
     */
    public boolean isFilled() {
        return filled;
    }

    /**
     * Overwrites the array backing the principle's password property.
     */
    public void clearPassword() {
        for (int i = 0; i < pwd.length; i++) {
            pwd[i] = '#';
        }
    }
}
