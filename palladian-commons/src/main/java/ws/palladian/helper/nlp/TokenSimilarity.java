package ws.palladian.helper.nlp;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;

public class TokenSimilarity implements StringSimilarity {

    private final SetSimilarity setSimilarity;

    public TokenSimilarity() {
        this(SetSimilarities.JACCARD);
    }

    public TokenSimilarity(SetSimilarity setSimilarity) {
        Validate.notNull(setSimilarity, "setSimilarity must not be null");
        this.setSimilarity = setSimilarity;
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

        List<String> split1 = Arrays.asList(s1lower.split("\\s"));
        List<String> split2 = Arrays.asList(s2lower.split("\\s"));
        return setSimilarity.calculate(split1, split2);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TokenSimilarity [setSimilarity=");
        builder.append(setSimilarity);
        builder.append("]");
        return builder.toString();
    }

}
