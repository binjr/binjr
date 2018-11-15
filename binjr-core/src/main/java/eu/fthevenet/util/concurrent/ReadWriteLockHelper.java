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

package eu.fthevenet.util.concurrent;

import eu.fthevenet.util.function.*;

import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * An helper class that wraps a {@link ReadWriteLock} instance and provides methods to streamline usage and avoid common inattention mistakes,
 * such as locking one the lock and unlocking the other in the {@code finally} block.
 *
 * @author Frederic Thevenet
 */
public class ReadWriteLockHelper {
    private final ReadWriteLock lock;
    private final LockHelper readLockHelper;
    private final LockHelper writeLockHelper;

    /**
     * Creates a new instance of the {@link ReadWriteLockHelper} class that wraps a new instance of {@link ReentrantReadWriteLock}
     */
    public ReadWriteLockHelper() {
        this(new ReentrantReadWriteLock());
    }

    /**
     * Creates a new instance of the {@link ReadWriteLockHelper} class for the provided lock.
     *
     * @param lock the {@link ReadWriteLock} instance to wrap.
     */
    public ReadWriteLockHelper(ReadWriteLock lock) {
        this.lock = lock;
        this.readLockHelper = new LockHelper(lock.readLock());
        this.writeLockHelper = new LockHelper(lock.writeLock());
    }

    /**
     * Gets the underlying ReadWriteLock instance.
     *
     * @return the underlying ReadWriteLock instance.
     */
    public ReadWriteLock getLock() {
        return lock;
    }

    /**
     * Returns an helper providing methods to run lambdas within the context of the read lock.
     *
     * @return an helper providing methods to run lambdas within the context of the read lock.
     */
    public LockHelper read() {
        return readLockHelper;
    }

    /**
     * Returns an helper providing methods to run lambdas within the context of the write lock.
     *
     * @return an helper providing methods to run lambdas within the context of the write lock.
     */
    public LockHelper write() {
        return writeLockHelper;
    }

    /**
     * An helper providing methods to run lambdas within the context of supplied lock.
     */
    public static class LockHelper {
        private final Lock lock;

        /**
         * Constructor
         *
         * @param lock Lock
         */
        private LockHelper(Lock lock) {
            this.lock = lock;
        }

        //region *** lock method overloads ***

        /**
         * Encapsulated the evaluation of the supplied {@link CheckedSupplier} within the boundaries of the lock.
         *
         * @param operation the {@link CheckedSupplier} to evaluate.
         * @param <R>       the return type for the {@link CheckedSupplier}.
         * @param <E>       the type of the exception thrown by the {@link CheckedSupplier}.
         * @return the return of the {@link CheckedSupplier}.
         * @throws E the exception thrown by the {@link CheckedSupplier}.
         */
        public <R, E extends Exception> R lock(CheckedSupplier<R, E> operation) throws E {
            lock.lock();
            try {
                return operation.get();
            } finally {
                lock.unlock();
            }
        }

        /**
         * Encapsulated the evaluation of the supplied {@link CheckedFunction} within the boundaries of the lock.
         *
         * @param operation the {@link CheckedFunction} to apply.
         * @param t         the parameter to pass the {@link CheckedFunction}
         * @param <T>       the type of he parameter to pass the {@link CheckedFunction}
         * @param <R>       the return type for the {@link CheckedFunction}.
         * @param <E>       the type of the exception thrown by the {@link CheckedFunction}.
         * @return the result of the {@link CheckedFunction}.
         * @throws E the exception thrown by the {@link CheckedFunction}.
         */
        public <T, R, E extends Exception> R lock(CheckedFunction<T, R, E> operation, T t) throws E {
            lock.lock();
            try {
                return operation.apply(t);
            } finally {
                lock.unlock();
            }
        }

        /**
         * Encapsulated the evaluation of the supplied {@link CheckedBiFunction} within the boundaries of the lock.
         *
         * @param operation the {@link CheckedBiFunction} to apply.
         * @param t         the first parameter to pass the {@link CheckedBiFunction}.
         * @param u         the second parameter to pass the {@link CheckedBiFunction}.
         * @param <T>       the type of the first parameter of the {@link CheckedBiFunction}.
         * @param <U>       the type of the first parameter of the {@link CheckedBiFunction}.
         * @param <R>       the return type for the {@link CheckedBiFunction}.
         * @param <E>       the type of the exception thrown by the {@link CheckedBiFunction}.
         * @return the result of the {@link CheckedFunction}.
         * @throws E the exception thrown by the  {@link CheckedBiFunction}.
         */
        public <T, U, R, E extends Exception> R lock(CheckedBiFunction<T, U, R, E> operation, T t, U u) throws E {
            lock.lock();
            try {
                return operation.apply(t, u);
            } finally {
                lock.unlock();
            }
        }

        /**
         * Encapsulated the evaluation of the supplied {@link CheckedRunnable} within the boundaries of the lock.
         *
         * @param operation the {@link CheckedRunnable} to evaluate.
         * @param <E>       the type of the exception thrown by the {@link CheckedRunnable}.
         * @throws E the exception thrown by the {@link CheckedRunnable}.
         */
        public <E extends Exception> void lock(CheckedRunnable<E> operation) throws E {
            lock.lock();
            try {
                operation.run();
            } finally {
                lock.unlock();
            }
        }

        /**
         * Encapsulated the evaluation of the supplied {@link CheckedConsumer} within the boundaries of the lock.
         *
         * @param operation the {@link CheckedConsumer} to evaluate.
         * @param t         the parameter to pass the {@link CheckedConsumer}
         * @param <T>       the type of the parameter to pass the {@link CheckedConsumer}
         * @param <E>       the type of the exception thrown by the {@link CheckedConsumer}.
         * @throws E the exception thrown by the {@link CheckedConsumer}.
         */
        public <T, E extends Exception> void lock(CheckedConsumer<T, E> operation, T t) throws E {
            lock.lock();
            try {
                operation.accept(t);
            } finally {
                lock.unlock();
            }
        }
        //endregion

        //region *** tryLock method overloads ***

        /**
         * Tries to acquire the lock and if successful, evaluates the supplied {@link CheckedSupplier} within the boundaries of the lock.
         *
         * @param operation the {@link CheckedSupplier} to evaluate.
         * @param <R>       the return type for the {@link CheckedSupplier}.
         * @param <E>       the type of the exception thrown by the {@link CheckedSupplier}.
         * @return An {@link Optional} that contains the return value of the {@link CheckedSupplier} if the lock could be acquired
         * @throws E the exception thrown by the {@link CheckedSupplier}.
         */
        public <R, E extends Exception> Optional<R> tryLock(CheckedSupplier<R, E> operation) throws E {
            if (lock.tryLock()) {
                try {
                    return Optional.ofNullable(operation.get());
                } finally {
                    lock.unlock();
                }
            }
            return Optional.empty();
        }

        /**
         * Tries to acquire the lock and if successful, evaluates the supplied {@link CheckedFunction} within the boundaries of the lock.
         *
         * @param operation the {@link CheckedFunction} to apply.
         * @param t         the parameter to pass the {@link CheckedFunction}
         * @param <T>       the type of he parameter to pass the {@link CheckedFunction}
         * @param <R>       the return type for the {@link CheckedFunction}.
         * @param <E>       the type of the exception thrown by the {@link CheckedFunction}.
         * @return the result of the {@link CheckedFunction}.
         * @throws E the exception thrown by the {@link CheckedFunction}.
         */
        public <T, R, E extends Exception> Optional<R> tryLock(CheckedFunction<T, R, E> operation, T t) throws E {
            if (lock.tryLock()) {
                try {
                    return Optional.ofNullable(operation.apply(t));
                } finally {
                    lock.unlock();
                }
            }
            return Optional.empty();
        }

        /**
         * Tries to acquire the lock and if successful, evaluates the supplied {@link CheckedBiFunction} within the boundaries of the lock.
         *
         * @param operation the {@link CheckedBiFunction} to apply.
         * @param t         the first parameter to pass the {@link CheckedBiFunction}.
         * @param u         the second parameter to pass the {@link CheckedBiFunction}.
         * @param <T>       the type of the first parameter of the {@link CheckedBiFunction}.
         * @param <U>       the type of the first parameter of the {@link CheckedBiFunction}.
         * @param <R>       the return type for the {@link CheckedBiFunction}.
         * @param <E>       the type of the exception thrown by the {@link CheckedBiFunction}.
         * @return the result of the {@link CheckedFunction}.
         * @throws E the exception thrown by the  {@link CheckedBiFunction}.
         */
        public <T, U, R, E extends Exception> Optional<R> tryLock(CheckedBiFunction<T, U, R, E> operation, T t, U u) throws E {
            if (lock.tryLock()) {
                try {
                    return Optional.ofNullable(operation.apply(t, u));
                } finally {
                    lock.unlock();
                }
            }
            return Optional.empty();
        }

        /**
         * Tries to acquire the lock and if successful, evaluates the supplied {@link CheckedRunnable} within the boundaries of the lock.
         *
         * @param operation the {@link CheckedRunnable} to evaluate.
         * @param <E>       the type of the exception thrown by the {@link CheckedRunnable}.
         * @throws E the exception thrown by the {@link CheckedRunnable}.
         */
        public <E extends Exception> boolean tryLock(CheckedRunnable<E> operation) throws E {
            if (lock.tryLock()) {
                try {
                    operation.run();
                } finally {
                    lock.unlock();
                }
                return true;
            }
            return false;
        }

        /**
         * Tries to acquire the lock and if successful, evaluates the supplied {@link CheckedConsumer} within the boundaries of the lock.
         *
         * @param operation the {@link CheckedConsumer} to evaluate.
         * @param t         the parameter to pass the {@link CheckedConsumer}
         * @param <T>       the type of the parameter to pass the {@link CheckedConsumer}
         * @param <E>       the type of the exception thrown by the {@link CheckedConsumer}.
         * @throws E the exception thrown by the {@link CheckedConsumer}.
         */
        public <T, E extends Exception> boolean tryLock(CheckedConsumer<T, E> operation, T t) throws E {
            if (lock.tryLock()) {
                try {
                    operation.accept(t);
                } finally {
                    lock.unlock();
                }
                return true;
            }
            return false;
        }
        //endregion
    }
}
