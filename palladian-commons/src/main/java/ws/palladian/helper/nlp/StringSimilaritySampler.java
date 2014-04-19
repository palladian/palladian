package ws.palladian.helper.nlp;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.SetSimilarities;

final class StringSimilaritySampler {

    /**
     * Try out different {@link StringSimilarity} measures on a given set of Strings. The method prints out the
     * similarity measures for each combination of the given strings.
     * 
     * @param strings The example strings.
     * @param similarities The similarity measures to test.
     */
    public static void printSimilarities(List<String> strings, List<? extends StringSimilarity> similarities) {
        Validate.notNull(strings, "strings must not be null");
        Validate.notNull(similarities, "similarities must not be null");
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        StringBuilder headerBuilder = new StringBuilder();
        for (StringSimilarity similarity : similarities) {
            headerBuilder.append('\t');
            headerBuilder.append(similarity.toString());
        }
        System.out.println(headerBuilder);
        for (int i = 0; i < strings.size(); i++) {
            String string1 = strings.get(i);
            for (int j = i + 1; j < strings.size(); j++) {
                String string2 = strings.get(j);
                StringBuilder lineBuilder = new StringBuilder();
                lineBuilder.append("sim(" + i + "," + j + ")");
                for (StringSimilarity similarity : similarities) {
                    double similarityValue = similarity.getSimilarity(string1, string2);
                    lineBuilder.append('\t');
                    lineBuilder.append(format.format(similarityValue));
                }
                System.out.println(lineBuilder);
            }
        }
    }

    public static void main(String[] args) {
        List<StringSimilarity> similarities = CollectionHelper.newArrayList();
        // similarities.add(new CharacterNGramSimilarity(3));
        // similarities.add(new CharacterNGramSimilarity(4));
        // similarities.add(new CharacterNGramSimilarity(5));
        // similarities.add(new CharacterNGramSimilarity(6));
        // similarities.add(new CharacterNGramSimilarity(7));
        // similarities.add(new CharacterNGramSimilarity(8));

        similarities.add(new CharacterNGramSimilarity(5, SetSimilarities.DICE));
        similarities.add(new CharacterNGramSimilarity(5, SetSimilarities.JACCARD));
        similarities.add(new CharacterNGramSimilarity(5, SetSimilarities.OVERLAP));

        similarities.add(new TokenSimilarity(SetSimilarities.DICE));
        similarities.add(new TokenSimilarity(SetSimilarities.JACCARD));
        similarities.add(new TokenSimilarity(SetSimilarities.OVERLAP));

        similarities.add(new JaroWinklerSimilarity());
        similarities.add(new LevenshteinSimilarity());

        List<String> strings = CollectionHelper.newArrayList();
        strings.add("Earthquake Shakes Mexico City");
        strings.add("Panic as earthquake hits Mexico City");
        strings.add("Powerful Quake Rattles Mexico");
        strings.add("Ukraine protesters reject Geneva peace deal");
        strings.add("Ukraine calls Easter truce in east ");

        printSimilarities(strings, similarities);
    }

}
