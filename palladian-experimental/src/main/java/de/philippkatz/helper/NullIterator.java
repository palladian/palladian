package de.philippkatz.helper;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.collections15.IteratorUtils;

/**
 * <p>
 * A NullIterator which contains no elements.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <T>
 * @deprecated Use {@link IteratorUtils#EMPTY_ITERATOR} instead.
 */
@Deprecated
public final class NullIterator<T> implements Iterator<T> {

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public T next() {
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
