package ws.palladian.helper.nlp;

import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.SetSimilarities;
import ws.palladian.helper.math.SetSimilarity;

/**
 * <p>
 * {@link StringSimilarity} which measures the similarity between two strings by applying a {@link SetSimilarity}
 * measure (such as Jaccard, Overlap, etc. See {@link SetSimilarities} for available implementations.)
 * 
 * @author pk
 */
public class TokenSimilarity implements StringSimilarity {

    private final SetSimilarity similarity;

    public TokenSimilarity() {
        this(SetSimilarities.JACCARD);
    }

    public TokenSimilarity(SetSimilarity similarity) {
        Validate.notNull(similarity, "similarity must not be null");
        this.similarity = similarity;
    }

    @Override
    public double getSimilarity(String s1, String s2) {
        Validate.notNull(s1, "s1 must not be null");
        Validate.notNull(s2, "s2 must not be null");

        String s1lower = s1.toLowerCase().trim();
        String s2lower = s2.toLowerCase().trim();
        if (s1lower.equals(s2lower)) {
            return 1;
        }

        Set<String> split1 = CollectionHelper.newHashSet(s1lower.split("\\s"));
        Set<String> split2 = CollectionHelper.newHashSet(s2lower.split("\\s"));
        return similarity.calculate(split1, split2);
    }

    @Override
    public String toString() {
        return new StringBuilder().append("token-").append(similarity).append("-similarity").toString();
    }

}
