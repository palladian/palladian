package ws.palladian.helper.nlp;

import org.apache.commons.lang3.Validate;
import ws.palladian.helper.math.SetSimilarities;
import ws.palladian.helper.math.SetSimilarity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * {@link StringMetric} which measures the similarity between two strings by applying a {@link SetSimilarity}
 * measure (such as Jaccard, Overlap, etc. See {@link SetSimilarities} for available implementations.)
 *
 * @author Philipp Katz
 */
public class TokenSimilarity extends AbstractStringMetric {
    private final SetSimilarity similarity;

    // lowercasing can take time so we can turn this off if the strings passed are already lowercase
    private boolean lowerCase = true;

    public TokenSimilarity() {
        this(SetSimilarities.JACCARD);
    }

    public TokenSimilarity(boolean lowerCase) {
        this(SetSimilarities.JACCARD);
        this.setLowerCase(lowerCase);
    }

    public TokenSimilarity(SetSimilarity similarity) {
        Validate.notNull(similarity, "similarity must not be null");
        this.similarity = similarity;
    }

    public boolean isLowerCase() {
        return lowerCase;
    }

    public void setLowerCase(boolean lowerCase) {
        this.lowerCase = lowerCase;
    }

    public double getSimilarity(String[] splits1, String[] splits2) {
        Set<String> split1 = new HashSet<>(Arrays.asList(splits1));
        Set<String> split2 = new HashSet<>(Arrays.asList(splits2));
        return similarity.getSimilarity(split1, split2);
    }

    @Override
    public double getSimilarity(String s1, String s2) {
        Validate.notNull(s1, "s1 must not be null");
        Validate.notNull(s2, "s2 must not be null");

        String s1lower = s1.trim();
        String s2lower = s2.trim();

        if (lowerCase) {
            s1lower = s1.toLowerCase();
            s2lower = s2.toLowerCase();
        }

        if (s1lower.equals(s2lower)) {
            return 1;
        }

        return getSimilarity(s1lower.split("\\s+"), s2lower.split("\\s+"));
    }

    @Override
    public String toString() {
        return new StringBuilder().append("token-").append(similarity).append("-similarity").toString();
    }
}
