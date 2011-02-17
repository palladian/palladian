package ws.palladian.classification.page;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import ws.palladian.helper.CollectionHelper;

public class NGramIndex extends HashMap<String, NGram> implements Serializable {

    private static final long serialVersionUID = 6872211728730694789L;

    private int numberOfDocuments = 0;

    @Override
    public NGram put(String key, NGram value) {

        key = key.toLowerCase();

        if (this.containsKey(key)) {
            NGram ngram = get(key);
            ngram.increaseFrequency();
            return ngram;
        } else {
            value.setIndex(this.size());
            return super.put(key, value);
        }

    }

    public NGram getNGram(String ngramString) {
        NGram ngram = null;

        ngramString = ngramString.toLowerCase();

        if (this.containsKey(ngramString)) {
            ngram = this.get(ngramString);
            ngram.calculateIdf(getNumberOfDocuments());
        }

        return ngram;
    }

    public Set<NGram> getTop(int k) {
        Set<NGram> topKSet = new LinkedHashSet<NGram>();

        Map<NGram, Double> ngramMap = new HashMap<NGram, Double>();
        for (String key : this.keySet()) {
            double idf = this.get(key).getIdf();
            ngramMap.put(new NGram(key), idf);
        }

        LinkedHashMap<NGram, Double> sortedMap = CollectionHelper.sortByValue(ngramMap.entrySet(), false);

        Iterator<Map.Entry<NGram, Double>> iterator = sortedMap.entrySet().iterator();
        for (int i = 0; i < k; i++) {
            topKSet.add(iterator.next().getKey());
        }

        return topKSet;
    }

    public int getNumberOfDocuments() {
        return numberOfDocuments;
    }

    public void setNumberOfDocuments(int numberOfDocuments) {
        this.numberOfDocuments = numberOfDocuments;
    }

    public void increasNumberOfDocuments() {
        this.numberOfDocuments++;
    }
}