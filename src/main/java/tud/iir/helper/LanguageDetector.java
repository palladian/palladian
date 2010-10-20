package tud.iir.helper;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.lingway.ld.GramTree;

/**
 * Wrapper for JLangDetect.
 * http://www.jroller.com/melix/entry/nlp_in_java_a_language
 * 
 * Model files are in the model repository -> JLangLanguageDetector
 * 
 * TODO I added this mainly for evaluation purposes; Palladian has its own language classifier. After we determined
 * Palladian as the winner, we can remove this again :)
 * 
 * @author Philipp Katz
 * 
 */
public class LanguageDetector {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(LanguageDetector.class);

    /** The confidence threshold for a language. This is the minimal percentage of n-grams which have to match. */
    private static final float THRESHOLD = 0.5f;

    /** In-memory cache for the n-grams. */
    private Map<String, GramTree> statsMap = new HashMap<String, GramTree>();

    /**
     * Instantiate a new LanguageDetector.
     * 
     * @param directoryPath The path to the directory with the model files. Their names need to comply with the
     *            following naming convention: <code>xx_tree.bin</code>, where xx denotes the language.
     */
    public LanguageDetector(String directoryPath) {
        loadTreeFiles(directoryPath);
    }

    private void loadTreeFiles(String directoryPath) {

        File[] files = FileHelper.getFiles(directoryPath, "_tree.bin");
        for (File file : files) {
            GramTree tree = (GramTree) FileHelper.deserialize(file.getPath());
            statsMap.put(file.getName().substring(0, 2), tree);
        }

    }

    public String detect(String aText, boolean explain) {
        StopWatch sw = new StopWatch();

        double best = 0;
        String bestLang = null;
        for (Map.Entry<String, GramTree> entry : statsMap.entrySet()) {
            if (explain) {
                LOGGER.debug("---------- testing : " + entry.getKey() + " -------------");
            }
            double score = entry.getValue().scoreText(aText, explain);

            // Added to original implementation -- normalize the scoring with the total number of n-grams that were
            // actually checked. The GramTrees store different lengths of n-grams, for example 1/2/3 grams, this
            // information is buried as private fields inside the GramTree class, so we have to use introspection to get
            // the values. In default GramTree set from europarl, there are 1, 2, and 3 grams, so we could also have
            // hardcoded this value here. This approach is flexible concerning different n-gram corpora we might add in
            // the future.
            int minNGramLength = (Integer) getPrivateField(entry.getValue(), "min");
            int maxNGramLength = (Integer) getPrivateField(entry.getValue(), "max");
            int numNGrams = 0;
            for (int i = minNGramLength; i <= maxNGramLength; i++) {
                numNGrams += aText.length() - i + 1;
            }

            score = score / numNGrams;
            // End addition -- Philipp.

            if (explain) {
                LOGGER.debug("---------- result : " + entry.getKey() + " : " + score + " -------------");
            }
            // Added threshold value. When texts cannot be classified clearly, we return null instead of the closest
            // match.
            if (score > best && score > THRESHOLD) {
                best = score;
                bestLang = entry.getKey();
            }
        }

        LOGGER.debug("detected in " + sw.getElapsedTimeString());
        return bestLang;
    }

    public String detect(String text) {
        return detect(text, false);
    }

    /**
     * Use introspection to get values of of private fields from objects.
     * 
     * @param object
     * @param name
     * @return
     */
    private static Object getPrivateField(Object object, String name) {
        Object value = null;
        try {
            Field field = object.getClass().getDeclaredField(name);
            field.setAccessible(true);
            value = field.get(object);
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return value;
    }

    @Override
    public String toString() {
        return "LanguageDetector; loaded languages: " + statsMap.keySet();
    }

    public static void main(String[] args) {

        LanguageDetector ld = new LanguageDetector("/home/pk/workspace/models/JLangLanguageDetector/europarl");

        System.out.println(ld);

        System.out.println(ld.detect("hello, world!"));
        System.out.println(ld.detect("grüß gott"));
        System.out.println(ld.detect("servus!"));
        System.out.println(ld.detect("bonjour"));
        System.out.println(ld.detect("ciao bella"));
        System.out.println(ld.detect("こんにちは")); // return null because we have no japanese gram tree.
        System.out.println(ld.detect("cogito ergo sum"));

    }
}
