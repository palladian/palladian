package ws.palladian.classification;

import java.io.Serializable;

/**
 * <p>
 * The word correlation stores the absolute and relative correlation of two terms.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class WordCorrelation implements Serializable {

    private static final long serialVersionUID = -4598016968757300406L;
    private String word1;
    private String word2;
    private double absoluteCorrelation = 0.0;
    private double relativeCorrelation = 0.0;

    public WordCorrelation(String word1, String word2) {
        this.word1 = word1;
        this.word2 = word2;
    }

    public void setWordPair(String word1, String word2) {
        this.word1 = word1;
        this.word2 = word2;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * (result + ((word1 == null) ? 0 : word1.hashCode()) + ((word2 == null) ? 0 : word2.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        WordCorrelation other = (WordCorrelation)obj;
        if (word1 == null) {
            if (other.word1 != null) {
                return false;
            }
        } else if (!(word1.equals(other.word1) || word1.equals(other.word2))) {
            return false;
        }
        if (word2 == null) {
            if (other.word2 != null) {
                return false;
            }
        } else if (!(word2.equals(other.word2) || word2.equals(other.word1))) {
            return false;
        }
        return true;
    }

    public String getTerm1() {
        return word1;
    }

    public String getTerm2() {
        return word2;
    }

    public double getAbsoluteCorrelation() {
        return absoluteCorrelation;
    }

    public void setAbsoluteCorrelation(double absoluteCorrelation) {
        this.absoluteCorrelation = absoluteCorrelation;
    }

    public void increaseAbsoluteCorrelation(double d) {
        this.absoluteCorrelation += d;
    }

    public void setRelativeCorrelation(double relativeCorrelation) {
        this.relativeCorrelation = relativeCorrelation;
    }

    public double getRelativeCorrelation() {
        return relativeCorrelation;
    }

    @Override
    public String toString() {
        return "WordCorrelation [abs. correlation=" + absoluteCorrelation + ", rel. correlation=" + relativeCorrelation
                + ", word1=" + word1 + ", word2=" + word2 + "]";
    }
}