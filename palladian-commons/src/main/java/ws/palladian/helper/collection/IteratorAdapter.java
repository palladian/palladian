package ws.palladian.helper.collection;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;

/**
 * Adapts an iterator of a specific type to a more common type (e.g. <code>Iterator&lt;Double&gt;</code> can be
 * converted to <code>Iterator&lt;Number&gt;</code> like this).
 * 
 * @author pk
 * 
 * @param <T>
 */
public final class IteratorAdapter<T> implements Iterator<T> {

    private final Iterator<? extends T> iterator;

    public IteratorAdapter(Iterator<? extends T> iterator) {
        Validate.notNull(iterator, "iterator must not be null");
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public T next() {
        return iterator.next();
    }

    @Override
    public void remove() {
        iterator.remove();
    }

}
