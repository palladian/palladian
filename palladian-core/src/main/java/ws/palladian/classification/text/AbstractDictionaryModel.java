package ws.palladian.classification.text;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.Category;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Function;

public abstract class AbstractDictionaryModel implements DictionaryModel {

    private static final long serialVersionUID = 1L;
    
    private static final char CSV_SEPARATOR = ';';

    @Override
    public Set<String> getCategories() {
        return CollectionHelper.convertSet(getDocumentCounts(), new Function<Category, String>() {
            @Override
            public String compute(Category input) {
                return input.getName();
            }
        });
    }

    @Override
    public void toCsv(PrintStream printStream) {
        Validate.notNull(printStream, "printStream must not be null");
        printStream.print("Term");
        Set<String> categories = new TreeSet<String>(getCategories());
        for (String category : categories) {
            printStream.print(CSV_SEPARATOR);
            printStream.print(category);
            printStream.print('=');
            printStream.print(getDocumentCounts().getCount(category));
        }
        printStream.print(CSV_SEPARATOR);
        printStream.print("sum=" + getDocumentCounts().getTotalCount() + "\n");
        for (TermCategoryEntries entries : this) {
            printStream.print(entries.getTerm());
            for (String category : categories) {
                int count = entries.getCount(category);
                printStream.print(CSV_SEPARATOR);
                printStream.print(count);
            }
            printStream.print(CSV_SEPARATOR);
            printStream.print(entries.getTotalCount());
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
        for (TermCategoryEntries entries : this) {
            numEntries += entries.size();
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

    // deprecated functionality

    @Override
    public void addDocument(Collection<String> terms, String category) {
        throw new UnsupportedOperationException("Use a builder to create the model");
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException("Use a builder to set the name of the model");
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
        for (TermCategoryEntries entries : this) {
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
        for (TermCategoryEntries thisEntries : this) {
            TermCategoryEntries otherEntries = other.getCategoryEntries(thisEntries.getTerm());
            if (!thisEntries.equals(otherEntries)) {
                return false;
            }
        }
        return true;
    }

}
