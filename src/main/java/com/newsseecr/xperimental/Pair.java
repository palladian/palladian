package com.newsseecr.xperimental;

/**
 * An unordered pair of items, so that Pair(Apple, Banana) == Pair(Banana, Apple). Useful for word correlations, etc.
 * Inspired by org.knime.ext.textprocessing.util.UnorderedPair<T>.
 * 
 * @param <T> Type of the items in this pair.
 * 
 * @author Philipp Katz
 */
public class Pair<T> {

    private T firstItem;
    private T secondItem;

    public Pair(T firstItem, T secondItem) {
        this.firstItem = firstItem;
        this.secondItem = secondItem;
    }

    public T getFirstItem() {
        return firstItem;
    }

    public T getSecondItem() {
        return secondItem;
    }

    @Override
    public int hashCode() {
        return firstItem.hashCode() + secondItem.hashCode();
    }

    @Override
    public boolean equals(Object obj) {

        boolean equals = false;

        if (obj instanceof Pair<?>) {
            Pair<?> that = (Pair<?>) obj;
            if (this.getFirstItem().equals(that.getFirstItem())) {
                equals = this.getSecondItem().equals(that.getSecondItem());
            } else if (this.getSecondItem().equals(that.getFirstItem())) {
                equals = this.getFirstItem().equals(that.getSecondItem());
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
        builder.append("Pair [firstItem=");
        builder.append(firstItem);
        builder.append(", secondItem=");
        builder.append(secondItem);
        builder.append("]");
        return builder.toString();
    }

}
