package ws.palladian.classification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.apache.log4j.Logger;

/**
 * <p>
 * Hold a number of category entries. For example, a word could have a list of relevant categories attached. Each
 * category has a certain relevance for the word which is expressed in the {@link CategoryEntry}.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class CategoryEntries extends ArrayList<CategoryEntry> implements Serializable {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(CategoryEntries.class);

    private static final long serialVersionUID = 4321001999458490582L;

    // in order to avoid recalculating all relative relevance scores for each category entry
    // we update them only if new entries were added
    private boolean relevancesUpToDate = false;

    boolean isRelevancesUpToDate() {
        return relevancesUpToDate;
    }

    void setRelevancesUpToDate(boolean relevancesUpToDate) {
        this.relevancesUpToDate = relevancesUpToDate;
    }

    public CategoryEntry getCategoryEntry(String categoryName) {
        for (CategoryEntry ce : this) {
            if (ce.getCategory().getName().equals(categoryName)) {
                return ce;
            }
        }
        return null;
    }

    @Override
    public boolean add(CategoryEntry e) {
        if (e == null) {
            return false;
        }
        // If a CategoryEntry is entered, the relative relevances are not up to date anymore.
        setRelevancesUpToDate(false);
        return super.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends CategoryEntry> c) {
        boolean listChanged = false;

        setRelevancesUpToDate(false);

        for (CategoryEntry newCategoryEntry : c) {
            CategoryEntry ce = getCategoryEntry(newCategoryEntry.getCategory().getName());
            if (ce != null) {
                ce.addAbsoluteRelevance(newCategoryEntry.getAbsoluteRelevance());
            } else {
                super.add(new CategoryEntry(this, newCategoryEntry.getCategory(), newCategoryEntry
                        .getAbsoluteRelevance()));
            }
            listChanged = true;
        }

        return listChanged;
    }

    public boolean addAllRelative(Collection<? extends CategoryEntry> c) {
        return addAllRelative(1.0, c);
    }

    public boolean addAllRelative(double coefficient, Collection<? extends CategoryEntry> categoryEntries) {
        boolean listChanged = false;

        setRelevancesUpToDate(false);

        for (CategoryEntry newCategoryEntry : categoryEntries) {
            double relevance = newCategoryEntry.getRelevance();
            if (relevance < 0) {
                relevance = 0;
            }
            CategoryEntry ce = getCategoryEntry(newCategoryEntry.getCategory().getName());
            if (ce != null) {
                ce.addAbsoluteRelevance(coefficient * relevance);
            } else {
                this.add(new CategoryEntry(this, newCategoryEntry.getCategory(), coefficient * relevance));
            }
            listChanged = true;
        }

        return listChanged;
    }

    /**
     * The relevance for a category entry is a sum of absolute relevance scores so far. To normalize the relevance to a
     * value between 0 and 1 we need to divide
     * it by the total absolute relevances of all category entries that are in the same category entries group.
     */
    void calculateRelativeRelevances() {

        LOGGER.debug("recalculate category entries relevances");

        // normalize
        Double totalRelevance = 0.0;
        for (CategoryEntry entry : this) {
            totalRelevance += entry.getAbsoluteRelevance();
        }

        for (CategoryEntry entry : this) {
            if (totalRelevance > 0) {
                entry.setRelativeRelevance(entry.getAbsoluteRelevance() / totalRelevance);
            } else {
                entry.setRelativeRelevance(-1.0);
            }
        }

        setRelevancesUpToDate(true);

    }

    public void sortByRelevance() {
        Collections.sort(this, new Comparator<CategoryEntry>() {
            @Override
            public int compare(CategoryEntry o1, CategoryEntry o2) {
                return ((Comparable<Double>)o2.getRelevance()).compareTo(o1.getRelevance());
            }
        });
    }

    public CategoryEntry getMostLikelyCategoryEntry() {
        sortByRelevance();
        // XXX
        if (size() > 0) {
            return get(0);
        }
        LOGGER.warn("no most likey category entry found");
        return new CategoryEntry(this, new Category(""), 1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (CategoryEntry ce : this) {
            sb.append(ce).append(",");
        }
        return sb.toString();
    }

}
