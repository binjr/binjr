/*
 *    Copyright 2018 Frederic Thevenet
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

package eu.fthevenet.util.diagnositic;

/**
 * Signals that an error occurred while invoking a diagnostic command.
 *
 * @author Frederic Thevenet
 */
public class DiagnosticException extends Exception {
    /**
     * Creates a new instance of the {@link DiagnosticException} class.
     */
    public DiagnosticException() {
        super("An error occurred while invoking a diagnostic command.");
    }

    /**
     * Creates a new instance of the {@link DiagnosticException} class with the provided message.
     *
     * @param message the message of the exception.
     */
    public DiagnosticException(String message) {
        super(message);
    }

    /**
     * Creates a new instance of the {@link DiagnosticException} class with the provided message and cause {@link Throwable}
     *
     * @param message the message of the exception.
     * @param cause   the cause for the exception.
     */
    public DiagnosticException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new instance of the {@link DiagnosticException} class with the provided cause {@link Throwable}
     *
     * @param cause the cause for the exception.
     */
    public DiagnosticException(Throwable cause) {
        super(cause);
    }
}
