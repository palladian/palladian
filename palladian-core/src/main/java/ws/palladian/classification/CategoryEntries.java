package ws.palladian.classification;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * Hold a number of category entries. For example, a word could have a list of relevant categories attached. Each
 * category has a certain relevance for the word which is expressed in the {@link CategoryEntry}.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class CategoryEntries implements Serializable, Iterable<CategoryEntry> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(CategoryEntries.class);
    
    private final List<CategoryEntry> entries = CollectionHelper.newArrayList();

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
        for (CategoryEntry ce : entries) {
            if (ce.getCategory().equals(categoryName)) {
                return ce;
            }
        }
        return null;
    }

    public boolean add(CategoryEntry e) {
        if (e == null) {
            return false;
        }
        // If a CategoryEntry is entered, the relative relevances are not up to date anymore.
        setRelevancesUpToDate(false);
        return entries.add(e);
    }

    public boolean addAllRelative(CategoryEntries c) {
        return addAllRelative(1.0, c);
    }

    public boolean addAllRelative(double coefficient, CategoryEntries categoryEntries) {
        boolean listChanged = false;

        setRelevancesUpToDate(false);

        for (CategoryEntry newCategoryEntry : categoryEntries) {
            double relevance = newCategoryEntry.getRelevance();
            if (relevance < 0) {
                relevance = 0;
            }
            CategoryEntry ce = getCategoryEntry(newCategoryEntry.getCategory());
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
        for (CategoryEntry entry : entries) {
            totalRelevance += entry.getAbsoluteRelevance();
        }

        for (CategoryEntry entry : entries) {
            if (totalRelevance > 0) {
                entry.setRelativeRelevance(entry.getAbsoluteRelevance() / totalRelevance);
            } else {
                entry.setRelativeRelevance(-1.0);
            }
        }

        setRelevancesUpToDate(true);

    }

    public void sortByRelevance() {
        Collections.sort(entries, new Comparator<CategoryEntry>() {
            @Override
            public int compare(CategoryEntry o1, CategoryEntry o2) {
                return ((Comparable<Double>)o2.getRelevance()).compareTo(o1.getRelevance());
            }
        });
    }

    public CategoryEntry getMostLikelyCategoryEntry() {
        sortByRelevance();
        // XXX
        if (entries.size() > 0) {
            return entries.get(0);
        }
        LOGGER.warn("no most likey category entry found");
        return new CategoryEntry(this, "", 1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (CategoryEntry ce : entries) {
            sb.append(ce).append(",");
        }
        return sb.toString();
    }

    @Override
    public Iterator<CategoryEntry> iterator() {
        return entries.iterator();
    }

    public int size() {
        return entries.size();
    }

}
