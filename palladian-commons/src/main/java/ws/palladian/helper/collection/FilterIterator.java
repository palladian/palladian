package ws.palladian.helper.collection;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;

/**
 * A {@link FilterIterator} wraps another iterator and applies a given {@link Filter}, which eliminates items from the
 * iteration, which do not pass the filter (i.e. they are simply skipped during iteration).
 * 
 * @author pk
 * 
 * @param <E> Type of the elements.
 * @see CollectionHelper#filter(Iterable, Filter)
 * @see CollectionHelper#filter(Iterator, Filter)
 */
class FilterIterator<E> extends AbstractIterator<E> {

    private final Iterator<E> iterator;
    private final Filter<? super E> filter;

    /**
     * Create a new {@link FilterIterator} wrapping the given {@link Iterator}.
     * 
     * @param iterator The iterator to wrap, not <code>null</code>.
     * @param filter The filter to apply, not <code>null</code>.
     */
    public FilterIterator(Iterator<E> iterator, Filter<? super E> filter) {
        Validate.notNull(iterator, "iterator must not be null");
        Validate.notNull(filter, "filter must not be null");
        this.iterator = iterator;
        this.filter = filter;
    }

    @Override
    protected E getNext() throws Finished {
        while (iterator.hasNext()) {
            E element = iterator.next();
            if (filter.accept(element)) {
                return element;
            }
        }
        throw new Finished();
    }

    @Override
    public void remove() {
        iterator.remove();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FilterIterator [iterator=");
        builder.append(iterator);
        builder.append(", filter=");
        builder.append(filter);
        builder.append("]");
        return builder.toString();
    }

}
