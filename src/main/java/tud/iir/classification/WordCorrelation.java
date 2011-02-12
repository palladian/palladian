package tud.iir.classification;

import java.io.Serializable;




/**
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Sandro Reichert
 * 
 */
public class WordCorrelation implements Serializable {

    private static final long serialVersionUID = -4598016968757300406L;
    private Term word1;
    private Term word2;
    private double absoluteCorrelation = 0.0;
    private double relativeCorrelation = 0.0;

    public WordCorrelation(Term word1, Term word2) {
        this.word1 = word1;
        this.word2 = word2;
    }

    public void setWordPair(Term word1, Term word2) {
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
        WordCorrelation other = (WordCorrelation) obj;
        boolean word1Equality = !(word1.equals(other.word1) || word1.equals(other.word2));
        boolean word2Equality = !(word2.equals(other.word2) || word2.equals(other.word1));
        if (word1 == null) {
            if (other.word1 != null) {
                return false;
            }
        } else if (word1Equality) {
            return false;
        }
        if (word2 == null) {
            if (other.word2 != null) {
                return false;
            }
        } else if (word2Equality) {
            return false;
        }
        return true;
    }

    public Term getTerm1() {
        return word1;
    }

    public Term getTerm2() {
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
        return "WordCorrelation [correlation=" + absoluteCorrelation + ", word1=" + word1 + ", word2=" + word2 + "]";
    }
}