package tud.iir.helper;

import java.util.ArrayList;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class WordNet {

    /**
     * Return noun synonyms for the given word by looking it up in the WordNet database.
     * 
     * @param word The word.
     * @param number The number.
     * @return An array of synonyms.
     */
    public static String[] getSynonyms(String word, int number) {
        return WordNet.getSynonyms(word, number, false);
    }

    public static String[] getSynonyms(String word, int number, boolean includeBaseWord) {

        ArrayList<String> synonyms = new ArrayList<String>();
        if (includeBaseWord)
            synonyms.add(word.toLowerCase());

        WordNetDatabase database = WordNetDatabase.getFileInstance();

        Synset[] nouns = database.getSynsets(word, SynsetType.NOUN);

        int wordCounter = 0;
        for (int i = 0; i < nouns.length; ++i) {
            String[] wordNetSynonyms = nouns[i].getWordForms();
            for (int j = 0; j < wordNetSynonyms.length; ++j) {
                // do not add words that are equal to the word given and do not add duplicates
                if (synonyms.contains(nouns[i].getWordForms()[j]))
                    continue;
                System.out.println(nouns[i].getWordForms()[j]);
                synonyms.add(nouns[i].getWordForms()[j]);
                ++wordCounter;
                if (wordCounter >= number)
                    break;
            }
            if (wordCounter >= number)
                break;
        }

        String str[] = new String[synonyms.size()];
        synonyms.toArray(str);
        return str;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        WordNet.getSynonyms("Country", 3, true);

        String[] synonyms = WordNet.getSynonyms("mobile phone", 3, true);
        System.out.println(synonyms.length + " " + synonyms[0] + " " + synonyms[1] + " " + synonyms[2] + " " + synonyms[3]);
    }
}