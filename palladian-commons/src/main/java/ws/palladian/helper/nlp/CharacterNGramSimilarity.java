package ws.palladian.helper.nlp;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.SetSimilarities;
import ws.palladian.helper.math.SetSimilarity;

/**
 * <p>
 * Similarity measure based on n-gram overlap (without considering positional attributes).
 * </p>
 * 
 * @author Philipp Katz
 */
public class CharacterNGramSimilarity extends AbstractStringMetric {

    private final int n;
    private final SetSimilarity setSimilarity;

    /**
     * <p>
     * Create a new n-gram based {@link StringMetric} with the specified length for the n-grams.
     * </p>
     * 
     * @param n The length of the n-grams, must be greater or equal 2.
     * @param setSimilarity The similarity measure used for the sets, not <code>null</code>.
     */
    public CharacterNGramSimilarity(int n, SetSimilarity setSimilarity) {
        Validate.isTrue(n >= 2, "n must be greater or equal 2.");
        Validate.notNull(setSimilarity, "setSimilarity must not be null");
        this.n = n;
        this.setSimilarity = setSimilarity;
    }

    /**
     * <p>
     * Create a new n-gram based {@link StringMetric} with the specified length for the n-grams.
     * </p>
     * 
     * @param n The length of the n-grams, must be greater or equal 2.
     */
    public CharacterNGramSimilarity(int n) {
        this(n, SetSimilarities.DICE);
    }

    @Override
    public double getSimilarity(String s1, String s2) {
        Validate.notNull(s1, "s1 must not be null");
        Validate.notNull(s2, "s2 must not be null");
        String s1lower = s1.toLowerCase();
        String s2lower = s2.toLowerCase();
        if (s1lower.equals(s2lower)) {
            return 1;
        }
        Set<String> nGrams1 = createNGrams(s1lower, n);
        Set<String> nGrams2 = createNGrams(s2lower, n);
        return setSimilarity.calculate(nGrams1, nGrams2);
    }

    /**
     * <p>
     * Create n-grams with padding at beginning/end. E.g. for "word", the following n-grams are created: ["##w", "#wo",
     * "wor", "ord", "rd#", "d##"].
     * </p>
     * 
     * @param s The string.
     * @param n The length of the n-grams to create.
     * @return The n-grams.
     */
    private Set<String> createNGrams(String s, int n) {
        StringBuilder temp = new StringBuilder();
        temp.append(StringUtils.repeat('#', n - 1));
        temp.append(s);
        temp.append(StringUtils.repeat('#', n - 1));
        s = temp.toString();
        Set<String> ret = CollectionHelper.newHashSet();
        for (int i = 0; i <= s.length() - n; i++) {
            ret.add(s.substring(i, i + n));
        }
        return ret;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(n).append("-gram-").append(setSimilarity).append("-similarity").toString();
    }

}
