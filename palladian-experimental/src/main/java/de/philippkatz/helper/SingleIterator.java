package de.philippkatz.helper;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.collections15.IteratorUtils;

/**
 * <p>
 * The SingleIterator contains just one item.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <T>
 * @deprecated Use {@link IteratorUtils#singletonIterator(Object)} instead.
 */
@Deprecated
public class SingleIterator<T> implements Iterator<T> {

    private T item;

    public SingleIterator(T item) {
        if (item == null) {
            throw new NullPointerException("item must not be null");
        }
        this.item = item;
    }

    @Override
    public boolean hasNext() {
        return item != null;
    }

    @Override
    public T next() {
        if (item != null) {
            try {
                return item;
            } finally {
                item = null;
            }
        }
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
