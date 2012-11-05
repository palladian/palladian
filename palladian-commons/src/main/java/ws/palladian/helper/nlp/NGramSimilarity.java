package ws.palladian.helper.nlp;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * Similarity measure based on n-gram overlap (without considering positional attributes).
 * </p>
 * 
 * @author Philipp Katz
 */
public class NGramSimilarity implements StringSimilarity {

    private final int n;

    /**
     * <p>
     * Create a new n-gram based {@link StringSimilarity} with the specified length for the n-grams.
     * </p>
     * 
     * @param n The length of the n-grams, must be greater or equal 2.
     */
    public NGramSimilarity(int n) {
        Validate.isTrue(n >= 2, "n must be greater or equal 2.");
        this.n = n;
    }

    @Override
    public double getSimilarity(String s1, String s2) {
        Validate.notNull(s1, "s1 must not be null");
        Validate.notNull(s2, "s2 must not be null");
        List<String> nGrams1 = createNGrams(s1, n);
        List<String> nGrams2 = createNGrams(s2, n);
        List<String> nGramsCommon = new ArrayList<String>(nGrams1);
        nGramsCommon.retainAll(nGrams2);
        return (double)(2 * nGramsCommon.size()) / (nGrams1.size() + nGrams2.size());
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
    private List<String> createNGrams(String s, int n) {
        StringBuilder temp = new StringBuilder();
        temp.append(StringUtils.repeat('#', n - 1));
        temp.append(s);
        temp.append(StringUtils.repeat('#', n - 1));
        s = temp.toString();
        List<String> ret = CollectionHelper.newArrayList();
        for (int i = 0; i <= s.length() - n; i++) {
            ret.add(s.substring(i, i + n));
        }
        return ret;
    }

}
