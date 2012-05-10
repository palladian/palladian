package de.philippkatz.helper;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.collections15.IteratorUtils;

/**
 * <p>
 * The CompositeIterator allows to concatenate multiple Iterators and to iterate them in one go. Modifications via
 * {@link #remove()} are not allowed, and an {@link UnsupportedOperationException} is thrown.
 * </p>
 * 
 * @author Philipp Katz
 * @param <T>
 * @deprecated Use {@link IteratorUtils#chainedIterator(java.util.Collection)} instead.
 */
@Deprecated
public final class CompositeIterator<T> implements Iterator<T> {

    private final List<Iterator<T>> iterators;

    public CompositeIterator(Iterator<T>... iterators) {
        if (iterators == null) {
            throw new NullPointerException("iterators must not be null");
        }
        this.iterators = Arrays.asList(iterators);
    }

    public CompositeIterator(List<Iterator<T>> iterators) {
        if (iterators == null) {
            throw new NullPointerException("iterators must not be null");
        }
        this.iterators = iterators;
    }

    @Override
    public boolean hasNext() {
        for (Iterator<T> iterator : iterators) {
            if (iterator.hasNext()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public T next() {
        for (Iterator<T> iterator : iterators) {
            if (iterator.hasNext()) {
                return iterator.next();
            }
        }
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
