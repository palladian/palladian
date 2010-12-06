package tud.iir.classification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections15.keyvalue.MultiKey;
import org.apache.commons.collections15.map.MultiKeyMap;

public class MKMWordCorrelationMatrix extends WordCorrelationMatrix {

    private static final long serialVersionUID = 1L;

    private MultiKeyMap<String, WordCorrelation> correlationMap = new MultiKeyMap<String, WordCorrelation>();

    @Override
    public WordCorrelation getCorrelation(String word1, String word2) {
        WordCorrelation correlation = correlationMap.get(word1, word2);
        return correlation;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Set<WordCorrelation> getCorrelations() {
        Collection correlations = correlationMap.values();
        return new HashSet<WordCorrelation>(correlations);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<WordCorrelation> getCorrelations(String word, int minCooccurrences) {

        List<WordCorrelation> correlations = new ArrayList<WordCorrelation>();
        Set<MultiKey<String>> multikeys = correlationMap.keySet();
        Set<String> allKeys = new HashSet<String>();

        for (MultiKey<String> multiKey : multikeys) {
            allKeys.add(multiKey.getKey(0));
            allKeys.add(multiKey.getKey(1));
        }

        for (String key : allKeys) {
            WordCorrelation wordCorrelation = correlationMap.get(word, key);
            if (key.equals(word) || wordCorrelation == null) {
                continue;
            }
            correlations.add(wordCorrelation);
        }
        return correlations;

    }

    @Override
    protected void createWordCorrelation(String word1, String word2) {
        WordCorrelation c = new WordCorrelation(getTerm(word1), getTerm(word2));
        c.setAbsoluteCorrelation(1.0);
        correlationMap.put(word1, word2, c);
        correlationMap.put(word2, word1, c);
    }

}
