package ws.palladian.retrieval.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Spell checks and auto-corrects text using the Palladian spell checker which is based on Peter Norvigs implementation.
 * </p>
 * 
 * <p>
 * Training data can be found on Palladian server under Datasets/SpellingCorrection
 * </p>
 * 
 * @see http://norvig.com/spell-correct.html
 * @author David Urbansky
 * 
 */
public class PalladianSpellChecker {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PalladianSpellChecker.class);

    private final CountMap<String> words = new CountMap<String>();

    public PalladianSpellChecker(String file) {

        StopWatch stopWatch = new StopWatch();

        // read the input file and create a P(w) model by counting the word occurrences
        final Pattern p = Pattern.compile("\\w+");
        LineAction lineAction = new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {
                Matcher m = p.matcher(line.toLowerCase());
                while (m.find()) {
                    String match = m.group();
                    words.add(match);
                }
            }

        };

        FileHelper.performActionOnEveryLine(file, lineAction);

        LOGGER.info("read file in " + stopWatch.getElapsedTimeString());
    }

    /**
     * <p>
     * Compute all edit distance 1 words. This list can get rather long.
     * </p>
     * <ol>
     * <li>n deletions</li>
     * <li>n-1 transpositions</li>
     * <li>26n alternations (replaced letter)</li>
     * <li>26(n+1) insertions (letter added at arbirary position)</li>
     * </ol>
     * 
     * @param word The word for which we create the edit distance words.
     * @return A list of possible variations.
     */
    private List<String> edits(String word) {
        List<String> result = new ArrayList<String>();

        // deletes, n
        for (int i = 0; i < word.length(); ++i) {
            result.add(word.substring(0, i) + word.substring(i + 1));
        }

        // transpositions, n-1
        for (int i = 0; i < word.length() - 1; ++i) {
            result.add(word.substring(0, i) + word.substring(i + 1, i + 2) + word.substring(i, i + 1)
                    + word.substring(i + 2));
        }

        // alternations, 26n
        for (int i = 0; i < word.length(); ++i) {
            for (char c = 'a'; c <= 'z'; ++c) {
                result.add(word.substring(0, i) + String.valueOf(c) + word.substring(i + 1));
            }
        }

        // insertions, 26(n+1)
        for (int i = 0; i <= word.length(); ++i) {
            for (char c = 'a'; c <= 'z'; ++c) {
                result.add(word.substring(0, i) + String.valueOf(c) + word.substring(i));
            }
        }

        return result;
    }

    /**
     * <p>
     * Automatically detect and correct spelling mistakes.
     * </p>
     * 
     * @param text The text to check for errors.
     * @return The auto-corrected text.
     */
    public String autoCorrect(String text) {
        StringBuilder correctedText = new StringBuilder();

        String[] textWords = text.split("\\s");
        for (String word : textWords) {
            word = StringHelper.trim(word);
            correctedText.append(correctWord(word)).append(" ");
        }

        return correctedText.toString().trim();
    }

    /**
     * <p>
     * Automatically detect and correct spelling mistakes in a word.
     * </p>
     * 
     * @param word The word to check for errors.
     * @return The auto-corrected word.
     */
    public String correctWord(String word) {

        boolean uppercase = StringHelper.startsUppercase(word);
        word = word.toLowerCase();

        // correct words don't need to be corrected
        if (words.contains(word)) {
            if (uppercase) {
                return StringHelper.upperCaseFirstLetter(word);
            }
            return word;
        }


        List<String> list = edits(word);
        Map<Integer, String> candidates = new HashMap<Integer, String>();
        for (String s : list) {
            if (words.contains(s)) {
                candidates.put(words.getCount(s), s);
            }
        }

        // check for edit distance 2 if we haven't found anything
        if (candidates.isEmpty()) {
            for (String s : list) {
                for (String w : edits(s)) {
                    if (words.contains(w)) {
                        candidates.put(words.getCount(w), w);
                    }
                }
            }
        }

        String corrected = word;
        if (!candidates.isEmpty()) {
            corrected = candidates.get(Collections.max(candidates.keySet()));
        }

        if (uppercase) {
            corrected = StringHelper.upperCaseFirstLetter(corrected);
        }

        return corrected;
    }

    public static void main(String[] args) throws IOException {
        System.out.println(new PalladianSpellChecker("en.txt").autoCorrect("caar"));
        System.out.println(new PalladianSpellChecker("en.txt").autoCorrect("This ls hoow the etxt is supossed to be"));
        System.out.println(new PalladianSpellChecker("de.txt").autoCorrect("Ist das nichk enn schoner Tetx"));
        System.out.println(new PalladianSpellChecker("de.txt").autoCorrect("blauess hadny"));
        System.out.println(new PalladianSpellChecker("de.txt").autoCorrect("oranes Hadny"));
    }
}
