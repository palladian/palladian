package tud.iir.classification.language;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import tud.iir.helper.FileHelper;
import tud.iir.helper.StopWatch;

import com.lingway.ld.GramTree;

/**
 * <p>
 * Wrapper for JLangDetect. http://www.jroller.com/melix/entry/nlp_in_java_a_language
 * </p>
 * 
 * <p>
 * Model files are in the model repository -> JLangLanguageDetector
 * </p>
 * 
 * <p>
 * The model is trained for 11 languages:
 * <ol>
 * <li>Danish (da)</li>
 * <li>German (de)</li>
 * <li>Greek (el)</li>
 * <li>English (en)</li>
 * <li>Spanish (es)</li>
 * <li>Finnish (fi)</li>
 * <li>French (fr)</li>
 * <li>Italian (it)</li>
 * <li>Dutch (nl)</li>
 * <li>Portuguese (pt)</li>
 * <li>Swedish (sv)</li>
 * </ol>
 * </p>
 * 
 * TODO I added this mainly for evaluation purposes; Palladian has its own language classifier. After we determined
 * Palladian as the winner, we can remove this again :)
 * 
 * @author Philipp Katz
 * 
 */
public class JLangDetect extends LanguageClassifier {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(JLangDetect.class);

    /** The confidence threshold for a language. This is the minimal percentage of n-grams which have to match. */
    private static final float THRESHOLD = 0.5f;

    /** In-memory cache for the n-grams. */
    private Map<String, GramTree> statsMap = new HashMap<String, GramTree>();

    /**
     * Instantiate a new LanguageDetector.
     * 
     * @param modelPath The path to the directory with the model files. Their names need to comply with the
     *            following naming convention: <code>xx_tree.bin</code>, where xx denotes the language.
     */
    public JLangDetect(String modelPath) {
        loadTreeFiles(modelPath);
    }

    public JLangDetect() {
        loadTreeFiles("data/models/JLangLanguageDetector/europarl");
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

    @Override
    public String classify(String text) {
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

        JLangDetect ld = new JLangDetect("/home/pk/workspace/models/JLangLanguageDetector/europarl");

        System.out.println(ld);

        System.out.println(ld.classify("hello, world!"));
        System.out.println(ld.classify("grüß gott"));
        System.out.println(ld.classify("servus!"));
        System.out.println(ld.classify("bonjour"));
        System.out.println(ld.classify("ciao bella"));
        System.out.println(ld.classify("こんにちは")); // return null because we have no japanese gram tree.
        System.out.println(ld.classify("cogito ergo sum"));

    }
}
