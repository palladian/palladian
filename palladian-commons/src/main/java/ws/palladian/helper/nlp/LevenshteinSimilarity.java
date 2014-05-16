package ws.palladian.helper.nlp;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Levenshtein similarity using Apache {@link StringUtils}. The similarity is calculated from the Levenshtein distance
 * (also known as edit distance) as follows:
 * 
 * <pre>
 *                                 LevenshteinDistance(S1, S2)
 * LevenshteinSimilarity(S1,S2) = ----------------------------- 
 *                                     max( | S1 |, | S2 |)
 * </pre>
 * 
 * @author Philipp Katz
 * @see <a href="http://en.wikipedia.org/wiki/Levenshtein_distance">Levenshtein distance</a>
 */
public class LevenshteinSimilarity implements StringSimilarity {

    private static final String NAME = "Levenshtein";

    @Override
    public double getSimilarity(String s1, String s2) {
        Validate.notNull(s1, "s1 must not be null");
        Validate.notNull(s2, "s2 must not be null");
        if (s1.equals(s2)) {
            return 1;
        }
        int distance = StringUtils.getLevenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 1;
        }
        return 1 - (double)distance / maxLength;
    }

    @Override
    public String toString() {
        return NAME;
    }

}
