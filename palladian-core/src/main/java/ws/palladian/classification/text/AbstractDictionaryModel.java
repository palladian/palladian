package ws.palladian.classification.text;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.helper.collection.CollectionHelper;

public abstract class AbstractDictionaryModel implements DictionaryModel {

    private static final long serialVersionUID = 1L;

    private static final char CSV_SEPARATOR = ';';

    /**
     * Version number which is written/checked when serializing/deserializing, if you make incompatible changes, update
     * this constant and provide backwards compatibility, so that existing models do not break.
     */
    protected static final int VERSION = 1;

    @Override
    public Set<String> getCategories() {
        return CollectionHelper.convertSet(getDocumentCounts(), input -> input.getName());
    }

    @Override
    public void toCsv(PrintStream printStream) {
        Validate.notNull(printStream, "printStream must not be null");
        printStream.print("Term");
        List<String> categories = new ArrayList<>(getCategories());
        Collections.sort(categories); // sort category names alphabetically
        for (String category : categories) {
            printStream.print(CSV_SEPARATOR);
            printStream.print(category);
            printStream.print('=');
            printStream.print(getDocumentCounts().getCount(category));
        }
        printStream.print(CSV_SEPARATOR);
        printStream.print("sum=" + getDocumentCounts().getTotalCount() + "\n");
        printStream.flush();
        for (DictionaryEntry entry : this) {
            printStream.print(entry.getTerm());
            CategoryEntries categoryEntries = entry.getCategoryEntries();
            CategoryEntries temp = new CountingCategoryEntriesBuilder().add(categoryEntries).create();
            for (String category : categories) {
                printStream.print(CSV_SEPARATOR);
                int count = temp.getCount(category);
                if (count > 0) { // do not print zeros
                    printStream.print(count);
                }
            }
            printStream.print(CSV_SEPARATOR);
            printStream.print(categoryEntries.getTotalCount());
            printStream.print('\n');
        }
        printStream.flush();
    }

    @Override
    public int getNumCategories() {
        return getCategories().size();
    }

    @Override
    public int getNumEntries() {
        int numEntries = 0;
        for (DictionaryEntry entry : this) {
            numEntries += entry.getCategoryEntries().size();
        }
        return numEntries;
    }

    @Override
    public int getNumDocuments() {
        return getDocumentCounts().getTotalCount();
    }

    @Override
    public int getNumTerms() {
        return getTermCounts().getTotalCount();
    }

    // toString

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(" [");
        if (getFeatureSetting() != null) {
            builder.append("featureSetting=").append(getFeatureSetting()).append(", ");
        }
        builder.append("#terms=").append(getNumUniqTerms());
        builder.append(", #categories=").append(getNumCategories());
        builder.append(", #entries=").append(getNumEntries()).append("]");
        return builder.toString();
    }

    // hashCode + equals

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        for (DictionaryEntry entries : this) {
            result += entries.hashCode();
        }
        result = prime * result + (getFeatureSetting() == null ? 0 : getFeatureSetting().hashCode());
        result = prime * result + getNumUniqTerms();
        result = prime * result + getDocumentCounts().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AbstractDictionaryModel other = (AbstractDictionaryModel)obj;
        if (getFeatureSetting() == null) {
            if (other.getFeatureSetting() != null) {
                return false;
            }
        } else if (!getFeatureSetting().equals(other.getFeatureSetting())) {
            return false;
        }
        if (getNumUniqTerms() != other.getNumUniqTerms()) {
            return false;
        }
        if (!getDocumentCounts().equals(other.getDocumentCounts())) {
            return false;
        }
        if (!getTermCounts().equals(other.getTermCounts())) {
            return false;
        }
        for (DictionaryEntry thisEntries : this) {
            CategoryEntries thisCategoryEntries = thisEntries.getCategoryEntries();
            CategoryEntries otherEntries = other.getCategoryEntries(thisEntries.getTerm());
            if (!thisCategoryEntries.equals(otherEntries)) {
                return false;
            }
        }
        return true;
    }
    
    // serialization code

    // Implementation note: in case you make any incompatible changes to the serialization protocol, provide backwards
    // compatibility by using the #VERSION constant. Add a test case for the new version and make sure, deserialization
    // of existing models still works (we keep a serialized form of each version from now on for the tests).

    protected void writeObject_(ObjectOutputStream out) throws IOException {
        // map the category names to numeric indices, so that we can use "1" instead of "aVeryLongCategoryName"
        List<Category> sortedCategories = CollectionHelper.newArrayList(getDocumentCounts());
        Collections.sort(sortedCategories, (c1, c2) -> c1.getName().compareTo(c2.getName()));
        Map<String, Integer> categoryIndices = new HashMap<>();
        int idx = 0;
        for (Category category : sortedCategories) {
            categoryIndices.put(category.getName(), idx++);
        }
        // version (for being able to provide backwards compatibility from now on)
        out.writeInt(VERSION);
        // header; number of categories; [ (categoryName, count) , ...]
        out.writeInt(sortedCategories.size());
        for (Category category : sortedCategories) {
            out.writeObject(category.getName());
            out.writeInt(category.getCount());
        }
        // number of terms; list of terms: [ ( term, numProbabilityEntries, [ (categoryIdx, count), ... ] ), ... ]
        out.writeInt(getNumUniqTerms());
        for (DictionaryEntry termEntry : this) {
            out.writeObject(termEntry.getTerm());
            CategoryEntries categoryEntries = termEntry.getCategoryEntries();
            out.writeInt(categoryEntries.size());
            for (Category category : categoryEntries) {
                int categoryIdx = categoryIndices.get(category.getName());
                out.writeInt(categoryIdx);
                out.writeInt(category.getCount());
            }
        }
        // feature setting
        out.writeObject(getFeatureSetting());
        // name
        out.writeObject(getName());
    }

}
