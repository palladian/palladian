package tud.iir.classification.controlledtagging;

import java.util.Comparator;

/**
 * Compare Tags based on their weights.
 * 
 * @author Philipp Katz
 * 
 */
public class TagComparator implements Comparator<Tag> {

    private short sign = 1;

    /**
     * Create new descending TagComparator.
     */
    public TagComparator() {
        this(true);
    }

    /**
     * Create new TagComparator.
     * 
     * @param descending if true, Tags are sorted descendingly by their weights, false ascendingly.
     */
    public TagComparator(boolean descending) {
        if (descending) {
            sign = -1;
        }
    }

    @Override
    public int compare(Tag t1, Tag t2) {
        return new Float(t1.getWeight()).compareTo(t2.getWeight()) * sign;
    }

}