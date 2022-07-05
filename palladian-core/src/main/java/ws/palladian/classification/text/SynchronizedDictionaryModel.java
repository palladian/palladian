package ws.palladian.classification.text;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Set;

import ws.palladian.core.CategoryEntries;

public class SynchronizedDictionaryModel implements DictionaryModel {
    
    private static final long serialVersionUID = 1L;
    
    private final DictionaryModel dictionaryModel;

    public SynchronizedDictionaryModel(DictionaryModel dictionaryModel) {
        this.dictionaryModel = dictionaryModel;
    }

    @Override
    public Set<String> getCategories() {
        synchronized (this) { return dictionaryModel.getCategories(); }
    }

    @Override
    public Iterator<DictionaryEntry> iterator() {
        synchronized (this) { return dictionaryModel.iterator(); }
    }

    @Override
    public String getName() {
        synchronized (this) { return dictionaryModel.getName(); }
    }

    @Override
    public FeatureSetting getFeatureSetting() {
        synchronized (this) { return dictionaryModel.getFeatureSetting(); }
    }

    @Override
    public CategoryEntries getCategoryEntries(String term) {
        synchronized (this) { return dictionaryModel.getCategoryEntries(term); }
    }

    @Override
    public int getNumUniqTerms() {
        synchronized (this) { return dictionaryModel.getNumUniqTerms(); }
    }

    @Override
    public int getNumTerms() {
        synchronized (this) { return dictionaryModel.getNumTerms(); }
    }

    @Override
    public int getNumCategories() {
        synchronized (this) { return dictionaryModel.getNumCategories(); }
    }

    @Override
    public int getNumEntries() {
        synchronized (this) { return dictionaryModel.getNumEntries(); }
    }

    @Override
    public int getNumDocuments() {
        synchronized (this) { return dictionaryModel.getNumDocuments(); }
    }

    @Override
    public CategoryEntries getDocumentCounts() {
        synchronized (this) { return dictionaryModel.getDocumentCounts(); }
    }

    @Override
    public CategoryEntries getTermCounts() {
        synchronized (this) { return dictionaryModel.getTermCounts(); }
    }

    @Override
    public void toCsv(PrintStream printStream) {
        synchronized (this) { dictionaryModel.toCsv(printStream); }
    }

}
