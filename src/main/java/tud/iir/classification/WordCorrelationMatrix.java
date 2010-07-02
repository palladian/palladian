package tud.iir.classification;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Correlation matrix.
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Sandro Reichert
 * 
 */
public class WordCorrelationMatrix extends HashSet<WordCorrelation> {

    private static final long serialVersionUID = 1L;

    /**
     * Add one to the correlation count of two terms.
     * The order of the terms does not matter: t1,t2 = t2,t1
     * 
     * @param word1 The first term.
     * @param word2 The second term.
     */
    public void updatePair(Term word1, Term word2) {
        WordCorrelation wc = getCorrelation(word1, word2);

        if (wc == null) {
            wc = new WordCorrelation(word1, word2);
            wc.setAbsoluteCorrelation(1.0);
            add(wc);
        } else {
            wc.increaseAbsoluteCorrelation(1.0);
        }
    }

    /*
     * private boolean contains(WordCorrelation wc) { if (getCorrelation(wc.getTerm1().getText(), wc.getTerm2().getText()) != null) { return true; } return
     * false; }
     */

    /**
     * The co-occurrences are saved in the matrix as absolute values. They can be made relative by dividing through the total number of documents.
     */
    public void makeRelativeScores() {

        for (WordCorrelation entry : this) {
            // absolute correlation (frequency of co-occurrence)
            double absoluteCorrelation = entry.getAbsoluteCorrelation();

            double sumCorrelation = 0.0;
            if (entry.getTerm1().equals(entry.getTerm2())) {
                sumCorrelation = getRowSum(entry.getTerm1());
            } else {
                sumCorrelation = getRowSum(entry.getTerm1()) + getRowSum(entry.getTerm2()) - absoluteCorrelation;
            }
            double relativeCorrelation = absoluteCorrelation / sumCorrelation;
            entry.setRelativeCorrelation(relativeCorrelation);
        }
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
            if ((entry.getTerm1().getText().equals(word1) || entry.getTerm2().getText().equals(word1)) && entry.getAbsoluteCorrelation() >= minCooccurrences) {
                correlations.add(entry);
            }
        }

        return correlations;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (WordCorrelation entry : this) {
            sb.append(entry.getTerm1().getText()).append("+").append(entry.getTerm2().getText()).append("=>").append(entry.getAbsoluteCorrelation()).append(
                    "\n");
        }
        return sb.toString();
    }

}