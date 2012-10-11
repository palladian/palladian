package ws.palladian.classification.persistence;

import org.apache.log4j.Logger;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.text.Dictionary;

public abstract class DictionaryIndex {

    /** The logger for this class. */
    protected static final Logger LOGGER = Logger.getLogger(DictionaryIndex.class);

    /** Keep track of categories. */
    protected Dictionary dictionary = null;

    public abstract void write(String word, CategoryEntries categoryEntries);

    public abstract void write(String word, CategoryEntry categoryEntry);

    public abstract void update(String word, CategoryEntries categoryEntries);

    public abstract void update(String word, CategoryEntry categoryEntry);

    public abstract CategoryEntries read(String word);

    public abstract void empty();

    public abstract void close();

    public abstract void openWriter();

    public abstract boolean openReader();

    private String indexPath = "data/models/";

    public Dictionary getDictionary() {
        return dictionary;
    }

    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    public void setIndexPath(String indexPath) {
        this.indexPath = indexPath;
    }

    public String getIndexPath() {
        return indexPath;
    }
}