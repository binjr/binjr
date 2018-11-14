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

package eu.fthevenet.util.function;

/**
 * A functional interface equivalent to {@link Runnable}, whose method
 * throws a checked exception.
 *
 * @param <E> the type of checked exception thrown.
 * @author Frederic Thevenet
 */
@FunctionalInterface
public interface CheckedRunnable<E extends Exception> {
    /**
     * Evaluates the lambda.
     *
     * @throws E a checked exception
     */
    void run() throws E;
}
