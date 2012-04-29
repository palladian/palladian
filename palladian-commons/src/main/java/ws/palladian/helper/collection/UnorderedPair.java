package ws.palladian.helper.collection;

import java.io.Serializable;

/**
 * <p>
 * An unordered tuple of items, so that UnorderedPair(Apple, Banana) == UnorderedPair(Banana, Apple). Useful for word
 * co-occurrences, etc.
 * </p>
 * 
 * @param <T> Type of the items in this {@link UnorderedPair}.
 * 
 * @author Philipp Katz
 */
public class UnorderedPair<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final T left;
    private final T right;

    public static <T> UnorderedPair<T> of(T left, T right) {
        return new UnorderedPair<T>(left, right);
    }

    public UnorderedPair(T left, T right) {
        this.left = left;
        this.right = right;
    }

    public T getLeft() {
        return left;
    }

    public T getRight() {
        return right;
    }

    @Override
    public int hashCode() {
        return left.hashCode() + right.hashCode();
    }

    @Override
    public boolean equals(Object obj) {

        boolean equals = false;

        if (obj instanceof UnorderedPair<?>) {
            UnorderedPair<?> that = (UnorderedPair<?>)obj;
            if (this.getLeft().equals(that.getLeft())) {
                equals = this.getRight().equals(that.getRight());
            } else if (this.getRight().equals(that.getLeft())) {
                equals = this.getLeft().equals(that.getRight());
            }
        }

        return equals;

    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Pair [left=");
        builder.append(left);
        builder.append(", right=");
        builder.append(right);
        builder.append("]");
        return builder.toString();
    }

}
