package ws.palladian.helper.collection;

import org.apache.commons.lang3.Validate;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator for an (object) array.
 *
 * @param <T>
 * @author Philipp Katz
 */
public final class ArrayIterator<T> implements Iterator<T> {

    private final T[] array;
    private int idx;

    public ArrayIterator(T[] array) {
        Validate.notNull(array, "array must not be null");
        this.array = array;
        this.idx = 0;
    }

    @Override
    public boolean hasNext() {
        return idx < array.length;
    }

    @Override
    public T next() {
        if (idx >= array.length) {
            throw new NoSuchElementException();
        }
        return array[idx++];
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "ArrayIterator " + array + " (" + array.length + ")";
    }

}
