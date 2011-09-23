package ws.palladian.classification.sentiment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * German Sentiment Classifier that uses SentiWS: http://wortschatz.informatik.uni-leipzig.de/download/sentiws.html
 * </p>
 * <p>
 * The model uses this dictionary and imports it from the file.
 * </p>
 * <p>
 * Texts can be classified into positive and negative.
 * </p>
 * 
 * @author David Urbansky
 */
public class GermanSentimentClassifier implements Serializable {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(GermanSentimentClassifier.class);
    
    /** Serial Version UID. */
    private static final long serialVersionUID = 3611658830894765273L;

    /** Sentiment Map. It contains the word and the sentiment between [-1,1] (-1 is negative, 1 is positive). */
    private Map<String, Double> sentimentMap = new HashMap<String, Double>();

    /** Some words can emphasize the sentiment. In this map we store these words and their multiplier. */
    private Map<String, Double> emphasizeMap = new HashMap<String, Double>();

    public GermanSentimentClassifier(String modelPath) {
        GermanSentimentClassifier gsc = FileHelper.deserialize(modelPath);
        this.sentimentMap = gsc.sentimentMap;

        emphasizeMap.put("bisschen", 0.9);
        emphasizeMap.put("sehr", 2.0);
        emphasizeMap.put("unheimlich", 3.0);
        emphasizeMap.put("extrem", 3.0);
    }

    /**
     * <p>
     * Use this constructor only to build a model.
     * </p>
     */
    public GermanSentimentClassifier() {
    }

    public void buildModel(String dictionaryFilePath, String modelPath) {
        loadDictionary(dictionaryFilePath);
        saveDictionary(modelPath);
    }

    /**
     * <p>
     * Import the dictionary from the SentiWS file.
     * </p>
     * 
     * @param dictionaryFilePath The path should be the prefix of the positive and negative file.
     */
    private void loadDictionary(String dictionaryFilePath) {
        List<String> positiveLines = FileHelper.readFileToArray(dictionaryFilePath + "Positive.txt");
        List<String> negativeLines = FileHelper.readFileToArray(dictionaryFilePath + "Negative.txt");

        List<String> sentimentLines = new ArrayList<String>();
        sentimentLines.addAll(positiveLines);
        sentimentLines.addAll(negativeLines);

        for (String sentimentLine : sentimentLines) {
            String[] parts = sentimentLine.split("\t");

            String mainWord = parts[0];
            
//            if (sentimentLine.indexOf("unschön") > -1) {
//                System.out.println("stop");
//            }

            double sentimentValue = Double.valueOf(parts[1]);

            // remove POS tag
            mainWord = mainWord.replaceAll("\\|.*", "");

            // get synonyms for the main word
            if (parts.length > 2) {
                String[] words = parts[2].split(",");
                for (String word : words) {
                    sentimentMap.put(word, sentimentValue);
                }
            }

            sentimentMap.put(mainWord, sentimentValue);
        }
    }

    private void saveDictionary(String modelPath) {
        FileHelper.serialize(this, modelPath);
    }

    /**
     * <p>
     * Classify a text as rather positive or negative.
     * </p>
     * <p>
     * We simply look up the sentiment for each word, negate the sentiment if we find a "nicht" before the word, and
     * emphasize the sentiment if we find and emphasizing word such as "sehr".
     * </p>
     * 
     * @param text The text to be classified.
     * @return A CategoryEntry with the likelihood.
     */
    public CategoryEntry classify(String text) {

        Category positiveCategory = new Category("positive");
        Category negativeCategory = new Category("negative");
        
        double positiveSentimentSum = 0;
        double negativeSentimentSum = 0;
        
        String[] tokens = text.split("\\s");
        String lastToken = "";
        for (String token : tokens) {
            
            token = StringHelper.trim(token);
            
            // check whether we should emphasize the sentiment
            double emphasizeWeight = 1.0;
            if (emphasizeMap.get(lastToken) != null) {
                emphasizeWeight = emphasizeMap.get(lastToken);
            }
            
            // check whether we need to negate the sentiment
            if (lastToken.equalsIgnoreCase("nicht")) {
                emphasizeWeight *= -1;
            }
            
            Double sentiment = sentimentMap.get(token);
            if (sentiment != null) {
                sentiment *= emphasizeWeight;
                if (sentiment > 0) {
                    LOGGER.debug("positive word: " + token + " ("+sentiment+")");
                    positiveSentimentSum += sentiment;
                } else {
                    LOGGER.debug("negative word: " + token + " ("+sentiment+")");
                    negativeSentimentSum += Math.abs(sentiment);
                }
            }
            
            lastToken = token;
        }
        
        CategoryEntries categoryEntries = new CategoryEntries();
        CategoryEntry positiveCategoryEntry = new CategoryEntry(categoryEntries, positiveCategory, positiveSentimentSum);
        CategoryEntry negativeCategoryEntry = new CategoryEntry(categoryEntries, negativeCategory, negativeSentimentSum);
        categoryEntries.add(positiveCategoryEntry);
        categoryEntries.add(negativeCategoryEntry);
        
        return categoryEntries.getMostLikelyCategoryEntry();
    }

    public static void main(String[] args) {
        Logger.getLogger(GermanSentimentClassifier.class).setLevel(Level.DEBUG);
        GermanSentimentClassifier gsc = null;
        
        // build the model
//        gsc = new GermanSentimentClassifier();
//        gsc.buildModel("data/temp/SentiWS_v1.8b_", "data/temp/gsc.gz");

        gsc = new GermanSentimentClassifier("data/temp/gsc.gz");
        CategoryEntry result = gsc.classify("Das finde ich nicht extrem toll aber manchmal ist das unschön.");
        result = gsc.classify("Angaben zu rechtlichen und/oder wirtschaftlichen Verknüpfungen zu anderen Büros oder Unternehmen, 3.");
        System.out.println(result);
    }

}