package ws.palladian.iirmodel.helper;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>
 * A NullIterator which contains no elements.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <T>
 */
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
