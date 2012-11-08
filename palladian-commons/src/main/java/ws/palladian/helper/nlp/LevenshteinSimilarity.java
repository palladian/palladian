package ws.palladian.helper.nlp;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Levenshtein Similarity using Apache {@link StringUtils}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class LevenshteinSimilarity implements StringSimilarity {

    @Override
    public double getSimilarity(String s1, String s2) {
        Validate.notNull(s1, "s1 must not be null");
        Validate.notNull(s2, "s2 must not be null");

        int distance = StringUtils.getLevenshteinDistance(s1, s2);
        return 1 - (float)distance / Math.max(s1.length(), s2.length());
    }

}
