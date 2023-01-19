package ws.palladian.helper.collection;

import org.apache.commons.lang3.Validate;

import java.util.*;

/**
 * <p>
 * The CompositeIterator allows to concatenate multiple Iterators and to iterate
 * them in one go. Modifications via {@link #remove()} are not allowed, and an
 * {@link UnsupportedOperationException} is thrown.
 * </p>
 *
 * @param <T>
 * @author Philipp Katz
 */
public class CompositeIterator<T> extends AbstractIterator2<T> implements Iterator<T> {

    private final Iterator<? extends Iterator<T>> iteratorsIterator;

    private Iterator<T> current;

    @SafeVarargs
    public CompositeIterator(Iterator<T>... iterators) {
        this(Arrays.asList(iterators));
    }

    public CompositeIterator(Collection<? extends Iterator<T>> iterators) {
        Validate.notNull(iterators, "iterators must not be null");
        this.iteratorsIterator = iterators.iterator();
        if (this.iteratorsIterator.hasNext()) {
            this.current = this.iteratorsIterator.next();
        } else {
            this.current = Collections.emptyIterator();
        }
    }

    public static <T> CompositeIterator<T> fromIterable(Collection<? extends Iterable<T>> iterables) {
        Validate.notNull(iterables, "iterables must not be null");
        List<Iterator<T>> iterators = new ArrayList<>();
        for (Iterable<T> iterable : iterables) {
            iterators.add(iterable.iterator());
        }
        return new CompositeIterator<>(iterators);
    }

    @Override
    protected T getNext() {
        if (current.hasNext()) {
            return current.next();
        }
        while (!current.hasNext() && iteratorsIterator.hasNext()) {
            current = iteratorsIterator.next();
            if (current.hasNext()) {
                return current.next();
            }
        }
        return finished();
    }

}
