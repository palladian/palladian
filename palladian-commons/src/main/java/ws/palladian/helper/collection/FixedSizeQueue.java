package ws.palladian.helper.collection;

import org.apache.commons.lang3.Validate;

import java.util.LinkedList;

/**
 * <p>
 * A queue with a fixed size, new items are appended to the end, and in case the specified maximum size is exceeded, the
 * oldest (i.e. first added elements) are removed.
 * </p>
 *
 * @param <E> Type of items.
 * @author Philipp Katz
 */
public class FixedSizeQueue<E> extends LinkedList<E> {
    private static final long serialVersionUID = 1L;

    private final int maxSize;

    /**
     * <p>
     * Create a new {@link FixedSizeQueue} with the specified maximum size.
     * </p>
     *
     * @param maxSize The maximum size, i.e. the maximum number of items to keep. Must be greater zero.
     * @return The {@link FixedSizeQueue}.
     */
    public static <E> FixedSizeQueue<E> create(int maxSize) {
        return new FixedSizeQueue<E>(maxSize);
    }

    private FixedSizeQueue(int maxSize) {
        Validate.isTrue(maxSize > 0, "maxSize must be greater zero.");
        this.maxSize = maxSize;
    }

    @Override
    public boolean add(E e) {
        super.add(e);
        while (size() > maxSize) {
            super.remove();
        }
        return true;
    }

    @Override
    public void add(int index, E e) {
        super.add(index, e);
        while (size() > maxSize) {
            super.removeLast();
        }
    }
}
