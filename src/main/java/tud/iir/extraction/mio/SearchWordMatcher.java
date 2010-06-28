/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The SearchWordMatcher checks if and how deep a given String contains an EntityName or a morpheme of it.
 * 
 * @author Martin Werner
 */
public class SearchWordMatcher {

    List<String> wordList;

    /**
     * By instantiating a list of words is generated out of the given searchwords (entityName).
     * 
     * @param searchWords the search words
     */
    public SearchWordMatcher(String searchWords) {

        wordList = (prepareWordList(searchWords, false));

        // System.out.println("wordList ready: " + wordList.size()
        // + wordList.toString());
    }

    /**
     * Check how deep a searchword or a kind of morphing is contained in the string ("samsung" vs. "samsung S8500")
     * 
     * @param src the src
     * @return the number of search word matches
     */
    public int getNumberOfSearchWordMatches(final String src) {

        int counter = 0;
        if (!("").equals(src)) {
            final String modSrc = src.toLowerCase(Locale.ENGLISH);
            // check if parts of searchwords are contained
            for (String word : wordList) {
                if (modSrc.contains(word)) {
                    counter++;
                }
            }
        }

        return counter;

    }

    /**
     * Gets the number of search word matches.
     * 
     * @param src the src
     * @param withoutSpecialWords the without special words
     * @param searchWords the search words
     * @return the number of search word matches
     */
    public int getNumberOfSearchWordMatches(final String src, boolean withoutSpecialWords, String searchWords) {
        int counter = 0;
        if (!("").equals(src)) {
            final String modSrc = src.toLowerCase(Locale.ENGLISH);
            // check if parts of searchwords are contained
            final List<String> tempWordList = prepareWordList(searchWords, withoutSpecialWords);
            for (String word : tempWordList) {
                if (modSrc.contains(word)) {
                    counter++;
                }
            }
        }

        return counter;
    }

    /**
     * generate a wordList from the given SearchWords(EntityName).
     * 
     * @param searchWords the search words
     * @param withoutSpecialWords the without special words
     * @return the list
     */
    private List<String> prepareWordList(String searchWords, boolean withoutSpecialWords) {

        final List<String> wordList = new ArrayList<String>();
        final List<String> morphResults = new ArrayList<String>();
        final String modSearchWords = searchWords.toLowerCase(Locale.ENGLISH);
        final String[] elements = modSearchWords.split("\\s");

        for (String element : elements) {

            // add the given SearchWords
            morphResults.addAll(morphSearchWord(element, withoutSpecialWords));
        }

        // no multiple words in wordList
        for (String word : morphResults) {
            if (!wordList.contains(word)) {
                wordList.add(word);
            }
        }
        return wordList;
    }

    /**
     * morph specialSearchWords like s500 to s_500 or s-500.
     * 
     * @param word the word
     * @param withoutSpecialWords the without special words
     * @return the list
     */
    private List<String> morphSearchWord(String word, boolean withoutSpecialWords) {
        String[] separators = { "_", "-" };
        List<String> morphList = new ArrayList<String>();

        if (withoutSpecialWords) {

            if (!word.matches("\\w+\\d+\\w*")) {
                morphList.add(word);
            }

        } else {
            // add the original word
            morphList.add(word);

            // get words like "S500" or "500Si"
            if (word.matches("\\w+\\d+\\w*")) {
                for (int i = 1; i < word.length(); i++) {
                    if (word.charAt(i - 1) != word.charAt(i)) {

                        String morphPart1 = word.substring(0, i);
                        String morphPart2 = word.substring(i);
                        // use every separator-sign
                        for (int x = 0; x < separators.length; x++) {
                            String morphWord = morphPart1 + separators[x] + morphPart2;
                            morphList.add(morphWord);
                        }

                    }

                }

            }
        }

        return morphList;
    }

    /**
     * Check if a searchword or a kind of morphing is contained in the string If the name of entity consists of more
     * than one word, more than one word must be
     * contained in the given string.
     * 
     * @param src the src
     * @return true, if successful
     */
    public boolean containsSearchWordOrMorphs(String src) {
        if (!("").equals(src)) {
            int matches = getNumberOfSearchWordMatches(src);

            if (wordList.size() == 1 && matches == 1) {
                return true;
            }
            if (wordList.size() > 1 && matches > 1) {
                return true;
            }

        }
        return false;
    }

    /**
     * The main method.
     * 
     * @param args the arguments
     */
    public static void main(String[] args) {

        // SearchWordMatcher matcher = new SearchWordMatcher("samsung s8500 wave");
        // matcher.matchSearchWords("http://pic.gsmarena.com/vv/spin/samsung-wave-s-8500-final.swf");
        // System.out.println("result: "
        // + matcher.getNumberOfSearchWordMatches("http://www.gsmarena.com/SAMSUNG_s8500_Wave-3d-spin-3146.php"));
        // System.out.println("result: "
        // + matcher.getNumberOfSearchWordMatches("http://www.gsmarena.com/SAMSUNG_s8500_Wave-3d-spin-3146.php",
        // true, "samsung s8500 wave"));
    }

}
