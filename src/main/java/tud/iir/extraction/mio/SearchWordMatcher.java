package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;

/**
 * The SearchWordMatcher checks if and how deep a given String contains an EntityName or a morpheme of it.
 * 
 * @author Martin Werner
 */
public class SearchWordMatcher {

    List<String> wordList;

    /**
     * By instantiating a list of words is generated out of the given searchwords (entityName)
     */
    public SearchWordMatcher(String SearchWords) {

        wordList = (prepareWordList(SearchWords, false));

        // System.out.println("wordList ready: " + wordList.size()
        // + wordList.toString());
    }

    /**
     * Check how deep a searchword or a kind of morphing is contained in the string ("samsung" vs. "samsung S8500")
     */
    public int getNumberOfSearchWordMatches(String src) {

        int counter = 0;
        if (!src.equals("")) {
            src = src.toLowerCase();
            // check if parts of searchwords are contained
            for (String word : wordList) {
                if (src.contains(word)) {
                    counter++;
                }
            }
        }

        return counter;

    }

    public int getNumberOfSearchWordMatches(String src, boolean withoutSpecialWords, String searchWords) {
        int counter = 0;
        if (!src.equals("")) {
            src = src.toLowerCase();
            // check if parts of searchwords are contained
            List<String> tempWordList = prepareWordList(searchWords, withoutSpecialWords);
            for (String word : tempWordList) {
                if (src.contains(word)) {
                    counter++;
                }
            }
        }

        return counter;
    }

    /**
     * generate a wordList from the given SearchWords(EntityName)
     */
    private List<String> prepareWordList(String searchWords, boolean withoutSpecialWords) {

        List<String> wordList = new ArrayList<String>();
        List<String> morphResults = new ArrayList<String>();
        searchWords = searchWords.toLowerCase();
        String Elements[] = searchWords.split("\\s");

        for (String element : Elements) {

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
     * morph specialSearchWords like s500 to s_500 or s-500
     * 
     */
    private List<String> morphSearchWord(String word, boolean withoutSpecialWords) {
        String separators[] = { "_", "-" };
        List<String> morphList = new ArrayList<String>();

        if (!withoutSpecialWords) {
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
        } else {
            if (!word.matches("\\w+\\d+\\w*")) {
                morphList.add(word);
            }
        }

        return morphList;
    }

    /**
     * Check if a searchword or a kind of morphing is contained in the string If the name of entity consists of more than one word, more than one word must be
     * contained in the given string.
     */
    public boolean containsSearchWordOrMorphs(String src) {
        if (!src.equals("")) {
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

    public static void main(String[] args) {

        SearchWordMatcher matcher = new SearchWordMatcher("samsung s8500 wave");
        // matcher.matchSearchWords("http://pic.gsmarena.com/vv/spin/samsung-wave-s-8500-final.swf");
        System.out.println("result: " + matcher.getNumberOfSearchWordMatches("http://www.gsmarena.com/SAMSUNG_s8500_Wave-3d-spin-3146.php"));
        System.out.println("result: "
                + matcher.getNumberOfSearchWordMatches("http://www.gsmarena.com/SAMSUNG_s8500_Wave-3d-spin-3146.php", true, "samsung s8500 wave"));
    }

}
