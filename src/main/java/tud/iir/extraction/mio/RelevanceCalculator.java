package tud.iir.extraction.mio;

import java.util.Locale;

import tud.iir.knowledge.Entity;

public class RelevanceCalculator {

    /**
     * Calculates string relevance.
     * 
     * @param inputString the input string
     * @param entity the entity
     * @return the double
     */
    public static double calcStringRelevance(final String inputString, final Entity entity) {
        return calcStringRelevance(inputString, entity.getName());
    }

    /**
     * Calculates the relevance of a string by checking how many terms or morphs of the entityName are included in the
     * string. A special role play words like
     * d500 or x500i. Returns: a value from 0 to 1
     * 
     * @param inputString the string
     * @param entityName the entity name
     * @return the double
     */
    public static double calcStringRelevance(final String inputString, final String entityName) {

        final SearchWordMatcher swm = new SearchWordMatcher(entityName);

        final String[] elements = entityName.split("\\s");
        // String input[] = inputString.split("\\s");
        // calculate the number of searchWord-Matches
        final double numOfMatches = (double) swm.getNumberOfSearchWordMatches(inputString);
        // System.out.println("number of swm: " + NumOfMatches);
        // calculate the number of searchWord-Matches with ignoring specialWords
        // like D500x
        final double numOfMatchesWithoutSW = (double) swm.getNumberOfSearchWordMatches(inputString, true,
                entityName.toLowerCase(Locale.ENGLISH));
        // System.out.println("number of swm without SW: " + NumOfMatchesWithoutSW);
        final double diff = numOfMatches - numOfMatchesWithoutSW;

        double result = ((numOfMatchesWithoutSW * 2) + (diff * 3)) / (double) (elements.length * 2);

        if (diff > 0) {

            result = (numOfMatches - 1) / elements.length + (diff / elements.length)
                    + (diff / (2 * elements.length));
            //
            result = numOfMatches / elements.length + (diff / (2 * elements.length));
            result = ((numOfMatchesWithoutSW * 2) + (diff * 3)) / (double) (elements.length * 2);
        } else {
            result = numOfMatches / elements.length;
        }

        // final JaroWinkler jaroWinkler = new JaroWinkler();
        // double result = (double) jaroWinkler.getSimilarity(entityName, inputString);

        if (result > 1) {
            result = 1;
        }
        return result;
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
