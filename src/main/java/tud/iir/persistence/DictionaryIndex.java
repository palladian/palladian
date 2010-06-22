package tud.iir.persistence;

import org.apache.log4j.Logger;

import tud.iir.classification.CategoryEntries;
import tud.iir.classification.CategoryEntry;
import tud.iir.classification.Dictionary;

public abstract class DictionaryIndex {

    protected static final Logger LOGGER = Logger.getLogger(DictionaryIndex.class);

    /** keep track of categories */
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

    public Dictionary getDictionary() {
        return dictionary;
    }

    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
    }
}