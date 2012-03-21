package ws.palladian.classification;

import java.io.Serializable;
import java.util.Comparator;

public class CategoryEntryComparator implements Comparator<CategoryEntry>, Serializable {

    /** The serial version id. */
    private static final long serialVersionUID = -4240727604739212045L;

    @Override
    public int compare(CategoryEntry o1, CategoryEntry o2) {
        return ((Comparable<Double>) o2.getRelevance()).compareTo(o1.getRelevance());
    }

}
