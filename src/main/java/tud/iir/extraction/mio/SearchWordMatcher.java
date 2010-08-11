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

    /** The word list. */
    private final List<String> wordList;

    /** The initial word list. */
    private final List<String> initialWordList;

    /**
     * By instantiating a list of words is generated out of the given searchwords (entityName).
     * 
     * @param searchWords the search words
     */
    public SearchWordMatcher(final String searchWords) {

        initialWordList = new ArrayList<String>();
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
    public int getNumberOfSearchWordMatches(final String src, final boolean withoutSpecialWords,
            final String searchWords) {
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
    private List<String> prepareWordList(final String searchWords, final boolean withoutSpecialWords) {

        final List<String> wordList = new ArrayList<String>();
        final List<String> morphResults = new ArrayList<String>();
        String modSearchWords = searchWords.toLowerCase(Locale.ENGLISH);
        modSearchWords = modSearchWords.replaceAll(":", "");
        modSearchWords = modSearchWords.replaceAll("\\s-\\s", "\\s");
        modSearchWords = modSearchWords.replaceAll("-", "\\s");
        final String[] elements = modSearchWords.split("\\s");

        for (String element : elements) {

            initialWordList.add(element);

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
    private List<String> morphSearchWord(final String word, final boolean withoutSpecialWords) {
        final String[] separators = { "_", "-" };
        final List<String> morphList = new ArrayList<String>();

        if (withoutSpecialWords) {

            if (!word.matches("[A-Za-z]+[\\d]+\\w*|[\\d]+[A-Za-z]+\\w*")) {
                morphList.add(word);
            }

        } else {
            // add the original word
            morphList.add(word);

            // get words like "S500" or "500Si"
            if (word.matches("[A-Za-z]+[\\d]+\\w*|[\\d]+[A-Za-z]+\\w*")) {
                for (int i = 1; i < word.length(); i++) {
                    if (word.charAt(i - 1) != word.charAt(i)) {

                        final String morphPart1 = word.substring(0, i);
                        final String morphPart2 = word.substring(i);
                        // use every separator-sign
                        for (int x = 0; x < separators.length; x++) {
                            final String morphWord = morphPart1 + separators[x] + morphPart2;
                            morphList.add(morphWord);
                        }

                    }

                }

            }
        }

        return morphList;
    }

    /**
     * Check if a searchword or a kind of morphing is contained in the string.
     * If the name of entity consists of more words, than the half of them must minimally be
     * contained in the given string.
     * 
     * @param src the src
     * @return true, if successful
     */
    public boolean containsSearchWordOrMorphs(final String src) {

        boolean returnValue = false;

        if (!("").equals(src)) {
           final int matches = getNumberOfSearchWordMatches(src);
            // System.out.println("searchWMatches: " + matches + "iniwordListSize: " + initialWordList.size());

            // case "avatar"
            if (initialWordList.size() == 1 && matches == 1) {
                returnValue = true;
            } else {
                // case "canon mp990"
                if (!(initialWordList.size() == 2 && matches == 1)) {

                    // case more words
                    final double wordFactor = ((double) initialWordList.size() / 2);
                    if (matches >= wordFactor) {
                        returnValue = true;
                    }
                }
            }

        }
        return returnValue;
    }

    /**
     * The main method.
     * 
     * @param args the arguments
     */
//    public static void main(String[] args) {

        // SearchWordMatcher matcher = new SearchWordMatcher("canon");
        // // matcher.matchSearchWords("http://pic.gsmarena.com/vv/spin/samsung-wave-s-8500-final.swf");
        // System.out.println(matcher.getNumberOfSearchWordMatches(" http://www.jr.com/canon/pe/CAN_MP980/"));
        // if (matcher.containsSearchWordOrMorphs("http://www.jr.com/canon/pe/CAN_MP980/")) {
        // System.out.println("is relevant link!");
        // } else {
        // System.out.println("is no relevant link!");
        // }

        // System.out.println("result: "
        // + matcher.getNumberOfSearchWordMatches("http://www.gsmarena.com/SAMSUNG_s8500_Wave-3d-spin-3146.php"));
        // System.out.println("result: "
        // + matcher.getNumberOfSearchWordMatches("http://www.gsmarena.com/SAMSUNG_s8500_Wave-3d-spin-3146.php",
        // true, "samsung s8500 wave"));
//    }

}
