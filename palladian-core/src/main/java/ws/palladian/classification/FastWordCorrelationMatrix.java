package ws.palladian.classification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This implementation is about twice as fast as the {@link WordCorrelationMatrix}, by using nested HashMaps to
 * accelerate the look up of correlations, but therefore also consumes twice as much memory.
 * 
 * @author Philipp Katz
 * 
 */
public class FastWordCorrelationMatrix extends WordCorrelationMatrix {

    private static final long serialVersionUID = 4153160190882629647L;

    /**
     * Dual nested Map with : Word1 -> Word2 -> WordCorrelation.
     * We also need to keep : Word2 -> Word1 -> WordCorrelation.
     */
    private Map<String, Map<String, WordCorrelation>> termTermCorrelations = new HashMap<String, Map<String, WordCorrelation>>();

    @Override
    public WordCorrelation getCorrelation(String word1, String word2) {

        WordCorrelation correlation = null;

        Map<String, WordCorrelation> termCorrelations = termTermCorrelations.get(word1);
        if (termCorrelations != null) {
            correlation = termCorrelations.get(word2);
        }

        return correlation;
    }

    @Override
    public Set<WordCorrelation> getCorrelations() {
        Set<WordCorrelation> correlations = new HashSet<WordCorrelation>();

        for (Map<String, WordCorrelation> termCorrelation : termTermCorrelations.values()) {
            correlations.addAll(termCorrelation.values());
        }

        LOGGER.trace("# of correlations " + correlations.size());
        return correlations;
    }

    @Override
    public List<WordCorrelation> getCorrelations(String word, int minCooccurrences) {

        List<WordCorrelation> correlations = new ArrayList<WordCorrelation>();

        Map<String, WordCorrelation> termCorrelations = termTermCorrelations.get(word);
        if (termCorrelations != null) {
            correlations.addAll(termCorrelations.values());
        }

        LOGGER.trace("correlations for " + word + " " + correlations);
        return correlations;
    }

    /**
     * <p>
     * Get the top k correlations for a given word.
     * </p>
     * 
     * @param word The word.
     * @param k The number of top correlations we are looking for.
     * @return The top k correlations sorted for the given word.
     */
    @Override
    public List<WordCorrelation> getTopCorrelations(String word, int k) {

        List<WordCorrelation> correlations = new ArrayList<WordCorrelation>();

        Map<String, WordCorrelation> termCorrelations = termTermCorrelations.get(word);
        if (termCorrelations != null) {
            correlations.addAll(termCorrelations.values());
        }

        Collections.sort(correlations, new WordCorrelationComparator());

        return correlations.subList(0, Math.min(correlations.size(), k));
    }

    @Override
    protected void createWordCorrelation(String word1, String word2) {
        WordCorrelation wc = new WordCorrelation(getTerm(word1), getTerm(word2));
        wc.setAbsoluteCorrelation(1.0);
        putToCorrelationsMap(word1, word2, wc);
        putToCorrelationsMap(word2, word1, wc);
    }
    
    @Override
    public void clear() {
        super.clear();
        termTermCorrelations.clear();
    }

    private void putToCorrelationsMap(String word1, String word2, WordCorrelation correlation) {

        Map<String, WordCorrelation> termCorrelation = termTermCorrelations.get(word1);
        if (termCorrelation == null) {
            termCorrelation = new HashMap<String, WordCorrelation>();
            termTermCorrelations.put(word1, termCorrelation);
        }
        termCorrelation.put(word2, correlation);
    }

}
