package ws.palladian.classification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import ws.palladian.helper.StopWatch;

/**
 * <p>
 * Correlation matrix.
 * </p>
 * 
 * <p>
 * See corresponding test case for an example.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Sandro Reichert
 * @author Philipp Katz
 * 
 */
public class WordCorrelationMatrix implements Serializable {

    private static final long serialVersionUID = 8049650115039222181L;

    /** The class logger. */
    protected static final Logger LOGGER = Logger.getLogger(WordCorrelationMatrix.class);

    /** Internal cache for all Terms, to keep the number of instances small. */
    private Map<String, Term> termMap = new HashMap<String, Term>();

    /** Map with Term names and a List of their correlations. */
    private Map<String, List<WordCorrelation>> termCorrelations = new HashMap<String, List<WordCorrelation>>();

    /**
     * Add one to the correlation count of two terms.
     * The order of the terms does not matter: t1,t2 = t2,t1
     * 
     * @param word1 The first term.
     * @param word2 The second term.
     */
    public void updatePair(Term word1, Term word2) {
        updatePair(word1.getText(), word2.getText());
    }

    /**
     * Add one to the correlation count of two terms.
     * The order of the terms does not matter: t1,t2 = t2,t1
     * 
     * @param word1 The first term.
     * @param word2 The second term.
     */
    public void updatePair(String word1, String word2) {

        WordCorrelation wc = getCorrelation(word1, word2);

        if (wc == null) {
            createWordCorrelation(word1, word2);
        } else {
            wc.increaseAbsoluteCorrelation(1.0);
        }
    }
    
    /**
     * Add a list of tags to the WordCorrelationMatrix, for a set with size n, we will add
     * <code>(n - 1) + (n - 2) + ... + 1 = (n * (n - 1)) / 2</code> correlations.
     * 
     * @param tags
     */
    public void updateGroup(Set<String> words) {
        String[] tagArray = words.toArray(new String[words.size()]);

        for (int i = 0; i < tagArray.length; i++) {
            for (int j = i + 1; j < tagArray.length; j++) {
                updatePair(tagArray[i], tagArray[j]);
            }
        }
    }

    /**
     * Get Term from termMap if present, elsewise create new Term instance and put it in the Map for caching.
     * 
     * @param word
     * @return
     */
    protected Term getTerm(String word) {
        Term term = termMap.get(word);
        if (term == null) {
            term = new Term(word);
            termMap.put(word, term);
        }
        return term;
    }

    protected void createWordCorrelation(String word1, String word2) {
        WordCorrelation wc = new WordCorrelation(getTerm(word1), getTerm(word2));
        wc.setAbsoluteCorrelation(1.0);
        putToCorrelationsMap(word1, wc);
        putToCorrelationsMap(word2, wc);
    }

    private void putToCorrelationsMap(String word, WordCorrelation correlation) {
        List<WordCorrelation> correlations = termCorrelations.get(word);
        if (correlations == null) {
            correlations = new ArrayList<WordCorrelation>();
            termCorrelations.put(word, correlations);
        }
        correlations.add(correlation);
    }

    /*
     * private boolean contains(WordCorrelation wc) { if (getCorrelation(wc.getTerm1().getText(),
     * wc.getTerm2().getText()) != null) { return true; } return
     * false; }
     */

    /**
     * The co-occurrences are saved in the matrix as absolute values. They can be made relative by dividing through the
     * total number of documents.
     */
    public void makeRelativeScores() {

        // calculate all the row sums for the Terms in advance
        Map<Term, Integer> rowSums = new HashMap<Term, Integer>();

        StopWatch sw = new StopWatch();
        for (Term term : termMap.values()) {
            int rowSum = getRowSum(term);
            rowSums.put(term, rowSum);
        }
        LOGGER.trace("calculated row sums in " + sw.getElapsedTimeString());

        sw = new StopWatch();
        for (WordCorrelation entry : getCorrelations()) {
            // absolute correlation (frequency of co-occurrence)
            double absoluteCorrelation = entry.getAbsoluteCorrelation();

            double sumCorrelation = 0.0;
            if (entry.getTerm1().equals(entry.getTerm2())) {
                sumCorrelation = rowSums.get(entry.getTerm1());
            } else {
                sumCorrelation = rowSums.get(entry.getTerm1()) + rowSums.get(entry.getTerm2()) - absoluteCorrelation;
            }
            double relativeCorrelation = absoluteCorrelation / sumCorrelation;
            entry.setRelativeCorrelation(relativeCorrelation);
        }
        LOGGER.trace("calculated relative scores in " + sw.getElapsedTimeString());
    }

    protected int getRowSum(Term term) {
        int rowSum = 0;

        // List<WordCorrelation> correlations = termCorrelations.get(term.getText());
        List<WordCorrelation> correlations = getCorrelations(term.getText(), -1);
        for (WordCorrelation entry : correlations) {
            if (entry.getTerm1().getText().equals(term.getText()) || entry.getTerm2().getText().equals(term.getText())) {
                rowSum += entry.getAbsoluteCorrelation();
            }
        }

        return rowSum;
    }

    public WordCorrelation getCorrelation(Term word1, Term word2) {
        return getCorrelation(word1.getText(), word2.getText());
    }

    public WordCorrelation getCorrelation(String word1, String word2) {

        WordCorrelation correlation = null;
        List<WordCorrelation> correlations = termCorrelations.get(word1);

        if (correlations != null) {
            for (WordCorrelation entry : correlations) {
                if (entry.getTerm1().getText().equals(word1) && entry.getTerm2().getText().equals(word2)
                        || entry.getTerm1().getText().equals(word2) && entry.getTerm2().getText().equals(word1)) {
                    correlation = entry;
                    break;
                }
            }
        }

        return correlation;
    }

    public List<WordCorrelation> getCorrelations(String word, int minCooccurrences) {
        List<WordCorrelation> correlations = new ArrayList<WordCorrelation>();
        List<WordCorrelation> toCheck = termCorrelations.get(word);

        if (toCheck != null) {
            for (WordCorrelation entry : toCheck) {
                if ((entry.getTerm1().getText().equals(word) || entry.getTerm2().getText().equals(word))
                        && entry.getAbsoluteCorrelation() >= minCooccurrences) {
                    correlations.add(entry);
                }
            }
        }

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
    public List<WordCorrelation> getTopCorrelations(String word, int k) {
        List<WordCorrelation> correlations = termCorrelations.get(word);

        Collections.sort(correlations, new WordCorrelationComparator());

        return correlations.subList(0, Math.min(correlations.size(), k));
    }

    /**
     * Return all correlation pairs.
     * 
     * @return
     */
    public Set<WordCorrelation> getCorrelations() {
        Set<WordCorrelation> correlations = new HashSet<WordCorrelation>();

        Set<Entry<String, List<WordCorrelation>>> termCorrelationEntries = termCorrelations.entrySet();
        for (Entry<String, List<WordCorrelation>> wordCorrelation : termCorrelationEntries) {
            correlations.addAll(wordCorrelation.getValue());
        }

        return correlations;
    }
    
    /**
     * Clear all correlations.
     */
    public void clear() {
        termMap.clear();
        termCorrelations.clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (WordCorrelation entry : getCorrelations()) {
            sb.append(entry.getTerm1().getText()).append("+").append(entry.getTerm2().getText()).append("=>").append(
                    entry.getAbsoluteCorrelation()).append("\n");
        }
        return sb.toString();
    }

}