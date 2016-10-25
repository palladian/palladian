package ws.palladian.helper.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * "Deflate" simple regular expression to represent all the inputs it would match.
 * </p>
 *
 * @author David Urbansky
 */
public class RegexPermuter {

    public static Collection<String> permute(String regexp) {

        // everything needs to be in parentheses, so lets normalize it if it isn't, e.g. (how) (are|is) => (how)(
        // )(are|is)
        regexp = regexp.replaceAll("\\)\\?([^()]+)\\(", ")($1)(");
        regexp = regexp.replaceAll("\\)([^?][^()]*)\\(", ")($1)(");

        List<String> bracketMatches = StringHelper.getRegexpMatches("\\(.*?\\)\\??", regexp);

        // CollectionHelper.print(bracketMatches);

        List<String[]> bracketSplits = new ArrayList<>();
        for (String bracket : bracketMatches) {

            // bracket = bracket.replace("(", "").replace(")?", "-?");
            bracket = bracket.replace("(", "").replace(")", "");

            // if (bracket.endsWith("-?")) {
            // bracket = bracket.replace("-?", "").trim();
            // bracket+="|_";
            // }

            if (bracket.endsWith("?")) {
                bracket = bracket.replace("?", "").trim();
                bracket += "|_";
            }

            String[] splits = bracket.split("\\|");

            bracketSplits.add(splits);
        }

        List<String> permutations = new ArrayList<>();
        permuteRecursively(permutations, bracketSplits, "");

        return permutations;
    }

    private static List<String> permuteRecursively(List<String> permutations, List<String[]> bracketSplits,
            String currentPermutation) {

        if (bracketSplits.isEmpty()) {
            permutations.add(currentPermutation.replace("_", "").trim());
            return permutations;
        }

        String[] split = bracketSplits.get(0);
        for (String string : split) {
            permuteRecursively(permutations, bracketSplits.subList(1, bracketSplits.size()), currentPermutation + ""
                    + string);
        }

        return permutations;
    }

}
