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

import java.util.function.BiFunction;

/**
 * A functional interface equivalent to {@link BiFunction}, whose method
 * throws a checked exception.
 *
 * @param <T> the type of the first input to the function.
 * @param <U> the type of the second input to the function.
 * @param <R> the type of the result of the function.
 * @param <E> the type of checked exception thrown.
 * @author Frederic Thevenet
 */
@FunctionalInterface
public interface CheckedBiFunction<T, U, R, E extends Exception> {
    /**
     * Applies this function to the given argument.
     *
     * @param t the first function argument
     * @param u the second function argument.
     * @return the function result
     * @throws E a checked exception
     */
    R apply(T t, U u) throws E;
}
