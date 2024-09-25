package ws.palladian.semantics.stemmers;

import java.util.Arrays;
import java.util.List;

/**
 * This class is a very primitive stemmer for the Slovak language. It is based on a list of common suffixes in the Slovak language.
 *
 * @author David Urbansky
 * @since 19.09.2024 at 11:45
 **/
public class SlovakStemmer {

    // List of common Slovak suffixes
    private static final List<String> suffixes = Arrays.asList("ovať", "enie", "ácia", "ie", "í", "ý", "é", "á", "ať", "ov", "in", "ský", "cka", "ák", "ička", "ovať", "ete", "osť",
            "och", "ú", "ím", "om", "em", "ou", "ám", "mi", "es", "ieť", "úť", "ací", "ícia");

    // Basic stemmer method
    public String stem(String word) {
        word = word.toLowerCase();

        // Attempt to remove known suffixes
        for (String suffix : suffixes) {
            if (word.endsWith(suffix)) {
                return word.substring(0, word.length() - suffix.length());
            }
        }
        return word;
    }

    public static void main(String[] args) {
        SlovakStemmer stemmer = new SlovakStemmer();

        // Test the stemmer with some Slovak words
        String[] words = {"študentov", "učiteľka", "písanie", "jednoduchý", "priateľ", "informácie", "organizovať"};

        for (String word : words) {
            System.out.println("Original word: " + word + ", Stemmed word: " + stemmer.stem(word));
        }
    }
}
