/*
 * Created by zhangxiangwei on 2019/12/27.
 * Copyright 2015－2021 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sensorsdata.analytics.android.sdk.util;


import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

public class WeakSet<T> implements Set<T> {
    private static final Object PRESENT = new Object();
    private transient WeakHashMap<T, Object> map;

    private static class EmptyIterator<E> implements Iterator<E> {
        private static EmptyIterator EMPTY_ITERATOR = new EmptyIterator();

        private EmptyIterator() {
        }

        public boolean hasNext() {
            return false;
        }

        public E next() {
            throw new UnsupportedOperationException("EmptyIterator should not call this method directly");
        }
    }

    private static class NonEmptyIterator<E> implements Iterator<E> {
        private final Iterator<WeakReference<E>> iterator;

        private NonEmptyIterator(Iterator<WeakReference<E>> iterator) {
            this.iterator = iterator;
        }

        public boolean hasNext() {
            return this.iterator.hasNext();
        }

        public E next() {
            return (E) ((WeakReference) this.iterator.next()).get();
        }
    }

    public int size() {
        if (this.map == null) {
            return 0;
        }
        return this.map.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean contains(Object o) {
        if (isEmpty() || o == null) {
            return false;
        }
        return this.map.containsKey(o);
    }


    public Iterator<T> iterator() {
        if (isEmpty()) {
            return EmptyIterator.EMPTY_ITERATOR;
        }
        return this.map.keySet().iterator();
    }


    public Object[] toArray() {
        throw new UnsupportedOperationException("method toArray() not supported");
    }


    public <T1> T1[] toArray(T1[] t1Arr) {
        throw new UnsupportedOperationException("method toArray(T[] a) not supported");
    }

    public boolean add(T t) {
        if (t == null) {
            throw new IllegalArgumentException("The argument t can't be null");
        }
        if (this.map == null) {
            this.map = new WeakHashMap();
        }
        return this.map.put(t, PRESENT) != null;
    }

    public boolean remove(Object o) {
        if (isEmpty() || o == null || this.map.remove(o) != PRESENT) {
            return false;
        }
        return true;
    }

    public boolean containsAll(Collection<?> collection) {
        throw new UnsupportedOperationException("method containsAll not supported");
    }

    public boolean addAll(Collection<? extends T> collection) {
        throw new UnsupportedOperationException("method addAll not supported now");
    }

    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException("method retainAll not supported now");
    }

    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException("method removeAll not supported now");
    }

    public void clear() {
        if (this.map != null) {
            this.map.clear();
        }
    }
}