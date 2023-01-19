package ws.palladian.helper.collection;

import org.apache.commons.lang3.Validate;

import java.util.Iterator;
import java.util.function.Predicate;

/**
 * A {@link FilterIterator} wraps another iterator and applies a given {@link Predicate}, which eliminates items from the
 * iteration, which do not pass the filter (i.e. they are simply skipped during iteration).
 *
 * @param <E> Type of the elements.
 * @author Philipp Katz
 * @see CollectionHelper#filter(Iterable, Predicate)
 * @see CollectionHelper#filter(Iterator, Predicate)
 */
class FilterIterator<E> extends AbstractIterator<E> {

    private final Iterator<? extends E> iterator;
    private final Predicate<? super E> filter;

    /**
     * Create a new {@link FilterIterator} wrapping the given {@link Iterator}.
     *
     * @param iterator The iterator to wrap, not <code>null</code>.
     * @param filter   The filter to apply, not <code>null</code>.
     */
    public FilterIterator(Iterator<? extends E> iterator, Predicate<? super E> filter) {
        Validate.notNull(iterator, "iterator must not be null");
        Validate.notNull(filter, "filter must not be null");
        this.iterator = iterator;
        this.filter = filter;
    }

    @Override
    protected E getNext() throws Finished {
        while (iterator.hasNext()) {
            E element = iterator.next();
            if (filter.test(element)) {
                return element;
            }
        }
        throw FINISHED;
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
