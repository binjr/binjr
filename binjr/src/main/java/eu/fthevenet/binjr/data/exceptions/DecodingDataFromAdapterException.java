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

package eu.fthevenet.binjr.data.exceptions;

/**
 * Signals that an error occurred while decoding data received from the adapter.
 *
 * @author Frederic Thevenet
 */
public class DecodingDataFromAdapterException extends DataAdapterException {
    /**
     * Creates a new instance of the {@link DecodingDataFromAdapterException} class.
     */
    public DecodingDataFromAdapterException() {
        super();
    }

    /**
     * Creates a new instance of the {@link DecodingDataFromAdapterException} class with the provided message.
     *
     * @param message the message of the exception.
     */
    public DecodingDataFromAdapterException(String message) {
        super(message);
    }

    /**
     * Creates a new instance of the {@link DecodingDataFromAdapterException} class with the provided message and cause {@link Throwable}
     *
     * @param message the message of the exception.
     * @param cause   the cause for the exception.
     */
    public DecodingDataFromAdapterException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new instance of the {@link DecodingDataFromAdapterException} class with the provided cause {@link Throwable}
     *
     * @param cause the cause for the exception.
     */
    public DecodingDataFromAdapterException(Throwable cause) {
        super(cause);
    }
}
