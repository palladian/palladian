package ws.palladian.helper.nlp;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /** A similarity measure between two sets. */
    public static interface SetSimilarity {
        <T> double calculate(Collection<T> c1, Collection<T> c2);
    }

    public static final SetSimilarity DICE = new SetSimilarity() {
        public <T> double calculate(Collection<T> c1, Collection<T> c2) {
            Set<T> nGramsCommon = new HashSet<T>(c1);
            nGramsCommon.retainAll(c2);
            return (double)(2 * nGramsCommon.size()) / (c1.size() + c2.size());
        }
    };

    public static final SetSimilarity JACCARD = new SetSimilarity() {
        @Override
        public <T> double calculate(Collection<T> c1, Collection<T> c2) {
            Set<T> intersection = new HashSet<T>(c1);
            intersection.retainAll(c2);
            if (intersection.size() == 0) {
                return 0;
            }
            Set<T> union = new HashSet<T>(c1);
            union.addAll(c2);
            return (double)intersection.size() / union.size();
        }
    };

    public static final SetSimilarity OVERLAP = new SetSimilarity() {
        @Override
        public <T> double calculate(Collection<T> c1, Collection<T> c2) {
            if (c1.size() == 0 || c2.size() == 0) {
                return 0;
            }
            Set<T> intersection = new HashSet<T>(c1);
            intersection.retainAll(c2);
            return (double)intersection.size() / Math.min(c1.size(), c2.size());
        }
    };

    private final int n;
    private final SetSimilarity setSimilarity;

    /**
     * <p>
     * Create a new n-gram based {@link StringSimilarity} with the specified length for the n-grams.
     * </p>
     * 
     * @param n The length of the n-grams, must be greater or equal 2.
     * @param setSimilarity The similarity measure used for the sets, not <code>null</code>.
     */
    public NGramSimilarity(int n, SetSimilarity setSimilarity) {
        Validate.isTrue(n >= 2, "n must be greater or equal 2.");
        Validate.notNull(setSimilarity, "setSimilarity must not be null");
        this.n = n;
        this.setSimilarity = setSimilarity;
    }

    /**
     * <p>
     * Create a new n-gram based {@link StringSimilarity} with the specified length for the n-grams.
     * </p>
     * 
     * @param n The length of the n-grams, must be greater or equal 2.
     */
    public NGramSimilarity(int n) {
        this(n, DICE);
    }

    @Override
    public double getSimilarity(String s1, String s2) {
        Validate.notNull(s1, "s1 must not be null");
        Validate.notNull(s2, "s2 must not be null");
        if (s1.equals(s2)) {
            return 1;
        }
        List<String> nGrams1 = createNGrams(s1, n);
        List<String> nGrams2 = createNGrams(s2, n);
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NGramSimilarity [n=");
        builder.append(n);
        builder.append(", setSimilarity=");
        builder.append(setSimilarity.getClass().getSimpleName());
        builder.append("]");
        return builder.toString();
    }

}
