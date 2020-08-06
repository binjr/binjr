/*
 *    Copyright 2020 Frederic Thevenet
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

package eu.binjr.common.navigation;

import java.util.Iterator;
import java.util.List;

public class RingIterator<E> implements Iterator<E> {
    private final List<E> wrappedList;
    private final int lastIdx;
    private int idx = -1;

    private RingIterator(List<E> wrappedList) {
        this.wrappedList = wrappedList;
        lastIdx = wrappedList.size() - 1;
    }

    public static <T> RingIterator<T> of(List<T> list) {
        return new RingIterator<>(list);
    }

    @Override
    public boolean hasNext() {
        return lastIdx > -1;
    }

    @Override
    public E next() {
        return wrappedList.get(nextIndex());
    }

    public boolean hasPrevious() {
        return lastIdx > -1;
    }

    public E previous() {
        return wrappedList.get(previousIndex());
    }

    public int nextIndex() {
        idx = (idx == lastIdx) ? 0 : idx + 1;
        return idx;
    }

    public int previousIndex() {
        idx = (idx <= 0) ? lastIdx : idx - 1;
        return idx;
    }

    public int peekCurrentIndex() {
        return idx;
    }

    public int peekLastIndex() {
        return lastIdx;
    }
}

