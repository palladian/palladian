package ws.palladian.helper.io;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;

public class CloseableIteratorAdapter<T> implements CloseableIterator<T> {

    private final Iterator<? extends T> iterator;

    public CloseableIteratorAdapter(Iterator<? extends T> iterator) {
        this.iterator = Objects.requireNonNull(iterator, "iterator must not be null");
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

    @Override
    public void close() throws IOException {
        if (iterator instanceof Closeable) {
            Closeable closeableIterator = (Closeable) iterator;
            closeableIterator.close();
        }
    }

}
