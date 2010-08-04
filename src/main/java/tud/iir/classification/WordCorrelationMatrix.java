package tud.iir.classification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import tud.iir.helper.StopWatch;

/**
 * Correlation matrix.
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Sandro Reichert
 * @author Philipp Katz
 */
public class WordCorrelationMatrix extends HashSet<WordCorrelation> {

    private static final Logger LOGGER = Logger.getLogger(WordCorrelationMatrix.class);

    private static final long serialVersionUID = 2L;

    /** Internal Cache for all Terms, to keep the number instances small. */
    private Map<String, Term> termMap = new HashMap<String, Term>();

    /**
     * Add one to the correlation count of two terms.
     * The order of the terms does not matter: t1,t2 = t2,t1
     * 
     * @param word1 The first term.
     * @param word2 The second term.
     */
    public void updatePair(Term word1, Term word2) {
        updatePair(word1.getText(), word2.getText());
        // WordCorrelation wc = getCorrelation(word1, word2);
        //
        // if (wc == null) {
        // wc = new WordCorrelation(word1, word2);
        // wc.setAbsoluteCorrelation(1.0);
        // add(wc);
        // } else {
        // wc.increaseAbsoluteCorrelation(1.0);
        // }
    }

    /**
     * Add one to the correlation count of two terms.
     * The order of the terms does not matter: t1,t2 = t2,t1
     * 
     * @param word1 The first term.
     * @param word2 The second term.
     */
    public void updatePair(String word1, String word2) {
        Term term1 = getTerm(word1);
        Term term2 = getTerm(word2);

        WordCorrelation wc = getCorrelation(word1, word2);

        if (wc == null) {
            wc = new WordCorrelation(term1, term2);
            wc.setAbsoluteCorrelation(1.0);
            add(wc);
        } else {
            wc.increaseAbsoluteCorrelation(1.0);
        }
    }

    /**
     * Get Term from termMap if present, elsewise create new Term instance and put it in the Map for caching.
     * 
     * @param word
     * @return
     */
    private Term getTerm(String word) {
        Term term = termMap.get(word);
        if (term == null) {
            term = new Term(word);
            termMap.put(word, term);
        }
        return term;
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
        LOGGER.debug("calculated row sums in " + sw.getElapsedTimeString());

        sw = new StopWatch();
        for (WordCorrelation entry : this) {
            // absolute correlation (frequency of co-occurrence)
            double absoluteCorrelation = entry.getAbsoluteCorrelation();

            double sumCorrelation = 0.0;
            if (entry.getTerm1().equals(entry.getTerm2())) {
                // sumCorrelation = getRowSum(entry.getTerm1());
                sumCorrelation = rowSums.get(entry.getTerm1());
            } else {
                // sumCorrelation = getRowSum(entry.getTerm1()) + getRowSum(entry.getTerm2()) - absoluteCorrelation;
                sumCorrelation = rowSums.get(entry.getTerm1()) + rowSums.get(entry.getTerm2()) - absoluteCorrelation;
            }
            double relativeCorrelation = absoluteCorrelation / sumCorrelation;
            entry.setRelativeCorrelation(relativeCorrelation);
        }
        LOGGER.debug("calculated relative scores in " + sw.getElapsedTimeString());
    }

    private int getRowSum(Term term) {
        int rowSum = 0;
        for (WordCorrelation entry : this) {
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

        for (WordCorrelation entry : this) {
            if (entry.getTerm1().getText().equals(word1) && entry.getTerm2().getText().equals(word2)
                    || entry.getTerm1().getText().equals(word2) && entry.getTerm2().getText().equals(word1)) {
                return entry;
            }
        }

        return null;
    }

    public List<WordCorrelation> getCorrelations(String word1, int minCooccurrences) {
        ArrayList<WordCorrelation> correlations = new ArrayList<WordCorrelation>();

        for (WordCorrelation entry : this) {
            if ((entry.getTerm1().getText().equals(word1) || entry.getTerm2().getText().equals(word1))
                    && entry.getAbsoluteCorrelation() >= minCooccurrences) {
                correlations.add(entry);
            }
        }

        return correlations;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (WordCorrelation entry : this) {
            sb.append(entry.getTerm1().getText()).append("+").append(entry.getTerm2().getText()).append("=>").append(
                    entry.getAbsoluteCorrelation()).append("\n");
        }
        return sb.toString();
    }

}