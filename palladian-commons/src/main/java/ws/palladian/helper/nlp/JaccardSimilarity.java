package ws.palladian.helper.nlp;

import org.apache.commons.lang3.Validate;
import ws.palladian.helper.math.MathHelper;

import java.util.Arrays;
import java.util.HashSet;

/**
 * <p>
 * Implementation of the Jaccard Similarity.
 * </p>
 *
 * @author David Urbansky
 * @deprecated Use {@link TokenSimilarity} instead.
 */
@Deprecated
public class JaccardSimilarity extends AbstractStringMetric {

    @Override
    public double getSimilarity(String s1, String s2) {
        Validate.notNull(s1, "s1 must not be null");
        Validate.notNull(s2, "s2 must not be null");

        // trim and transform both strings to lowercase
        s1 = s1.trim().toLowerCase();
        s2 = s2.trim().toLowerCase();

        // strings are exactly equal
        if (s1.equals(s2)) {
            return 1;
        }

        String[] setA = s1.split("\\s+");
        String[] setB = s2.split("\\s+");

        return MathHelper.computeJaccardSimilarity(new HashSet<>(Arrays.asList(setA)), new HashSet<>(Arrays.asList(setB)));
    }

}
