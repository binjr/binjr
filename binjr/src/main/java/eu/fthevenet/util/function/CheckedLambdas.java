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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A series of methods that wrap functional interfaces that throw checked
 * exceptions inside their standard (non throwing) counterparts, so they can be
 * used with streams or other classes expecting standards functional interfaces
 * and have them acting transparent to the exception thrown within the lambda.
 */
public final class CheckedLambdas {
    /**
     * Override constructor
     */
    private CheckedLambdas() {
    }

    /**
     * Wraps a {@link CheckedConsumer} inside a {@link Consumer} and rethrow the
     * original exception.
     *
     * @param consumer the {@link Consumer} to wrap.
     * @param <T>      the type for the consumer's parameter.
     * @param <E>      the type for the checked exception.
     * @return a {@link Consumer} instance.
     * @throws E the checked exception thrown in the lambda.
     */
    public static <T, E extends Exception> Consumer<T> wrap(CheckedConsumer<T, E> consumer) throws E {
        return t -> {
            try {
                consumer.accept(t);
            } catch (Exception exception) {
                throw throwActualException(exception);
            }
        };
    }

    /**
     * Wraps a {@link CheckedRunnable} inside a {@link Runnable} and rethrow the
     * original exception.
     *
     * @param runnable the {@link Runnable} to wrap.
     * @param <E>      the type for the checked exception.
     * @return a {@link Runnable} instance.
     * @throws E the checked exception thrown in the lambda.
     */
    public static <E extends Exception> Runnable wrap(CheckedRunnable<E> runnable) throws E {
        return () -> {
            try {
                runnable.run();
            } catch (Exception exception) {
                throw throwActualException(exception);
            }
        };
    }

    /**
     * Wraps a {@link CheckedSupplier} inside a {@link Supplier} and rethrow the
     * original exception.
     *
     * @param supplier the {@link Supplier} to wrap.
     * @param <T>      the type for the supplier's parameter.
     * @param <E>      the type for the checked exception.
     * @return a {@link Supplier} instance.
     * @throws E the checked exception thrown in the lambda.
     */
    public static <T, E extends Exception> Supplier<T> wrap(CheckedSupplier<T, E> supplier) throws E {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception exception) {
                throw throwActualException(exception);
            }
        };
    }

    /**
     * Wraps a {@link CheckedFunction} inside a {@link Function} and rethrow the
     * original exception.
     *
     * @param function the {@link Function} to wrap.
     * @param <T>      the type for the function's parameter.
     * @param <R>      the return type of the function.
     * @param <E>      the type for the checked exception.
     * @return a {@link Function} instance.
     * @throws E the checked exception thrown in the lambda.
     */
    public static <T, R, E extends Exception> Function<T, R> wrap(CheckedFunction<T, R, E> function) throws E {
        return t -> {
            try {
                return function.apply(t);
            } catch (Exception exception) {
                throw throwActualException(exception);
            }
        };
    }

    /**
     * Forge runtime exception
     *
     * @param exception exception
     * @return Runtime exception
     * @throws E Exception
     */
    @SuppressWarnings("unchecked")
    private static <E extends Exception> RuntimeException throwActualException(Exception exception) throws E {
        throw (E) exception;
    }

}
