package tud.iir.news;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import tud.iir.classification.page.ClassificationDocument;
import tud.iir.classification.page.ClassifierManager;
import tud.iir.classification.page.URLClassifier;
import tud.iir.classification.page.TextClassifier;
import tud.iir.classification.page.evaluation.ClassificationTypeSetting;
import tud.iir.helper.FileHelper;
import tud.iir.persistence.DatabaseManager;

/**
 * This code has been hacked together quickly for testing and experimental purposes. Basically it fetches feed entries from the database and converts them to
 * training data for the classifier. Then we run a train+test run with the classifier.
 * 
 * @author Philipp Katz
 * 
 */
public class ClassifierTesting {

    private static final String SEPARATOR = "#";

    public static void main(String[] args) throws Exception {

        /*
         * String test = "ABC"; boolean find = Pattern.compile("\\babc\\b", Pattern.CASE_INSENSITIVE).matcher(test).find(); System.out.println(find);
         */

        // String[] tags = new String[] { "apple", "iphone", "ipad", "microsoft", "kin", "htc", "evo", "phone", "smartphone", "mobile", "gadget", "palm",
        // "pixi",
        // "google", "nexus", "dell", "lightning", "droid", "android" };
        String fileName = "temp/training_gadgets_terms.txt";
        // int limitNumTestData = 10000;

        // writeTrainFileWithTerms(fileName, tags, limitNumTestData);
        // writeTrainFileWithTags(fileName, tags, limitNumTestData);

        trainClass(fileName);

        // classify("http://de.wikipedia.org/wiki/Apple");

    }

    private static void classify(String url) {

        System.out.println("classifying");

        // create the URL classifier
        TextClassifier classifier = new URLClassifier();
        // create a classification document
        ClassificationDocument classifiedDocument = null;

        // use the classifier to classify a web page to multiple categories
        // using URL features only
        classifiedDocument = classifier.classify(url);

        // print out classification results
        System.out.println(classifiedDocument);

    }

    /**
     * Creates a train file from the feed database.
     * 
     * @param fileName
     * @param tags
     * @param limit
     * @throws SQLException
     */
    private static void writeTrainFileWithTerms(String fileName, String[] tags, int limit) throws SQLException {

        System.out.println("writing train file");

        String tagStr = StringUtils.join(tags, " ");

        StringBuilder trainFile = new StringBuilder();

        ResultSet results = DatabaseManager.getInstance().runQuery(
                "SELECT text, pageText FROM entries WHERE MATCH(title,text,pageText) AGAINST('" + tagStr + "')");
        int counter = 0;
        while (results.next() && ++counter < limit) {

            String text;
            String entryText = results.getString("text");
            String pageText = results.getString("pageText");

            if (entryText == null && pageText == null) {
                continue;
            } else if (entryText != null && entryText.length() > 0) {
                text = entryText;
                if (pageText != null) {
                    if (pageText.length() > entryText.length()) {
                        text = pageText;
                    }
                }
            } else if (pageText != null && pageText.length() > 0) {
                text = pageText;
            } else {
                continue;
            }

            text = text.replaceAll("\n", "");
            text = text.replaceAll(SEPARATOR, " ");
            text = text.replaceAll("[^A-Za-z0-9 ]", "");
            text = text.replaceAll("\\s{2,}", " ");
            text = text.trim();

            // assign all tags
            List<String> assignTags = new LinkedList<String>();
            for (String temp : tags) {
                // if (text.toLowerCase().contains(" "+temp.toLowerCase()+" ")) {
                // tag must be between word bondaries
                if (Pattern.compile("\\b" + temp + "\\b", Pattern.CASE_INSENSITIVE).matcher(text).find()) {
                    assignTags.add(temp);
                }
            }

            String tagListStr = StringUtils.join(assignTags, SEPARATOR);
            if (tagListStr.length() > 0) {
                String line = text + SEPARATOR + tagListStr + "\n";
                trainFile.append(line);
            }
        }
        System.out.println("wrote " + counter + " test documents");
        FileHelper.writeToFile(fileName, trainFile);
    }

    /**
     * Creates a train file from the feed database.
     * 
     * @param fileName
     * @param tags
     * @param limit
     * @throws SQLException
     */
    private static void writeTrainFileWithTags(String fileName, String[] tags, int limit) throws SQLException {

        System.out.println("writing train file");

        StringBuilder trainFile = new StringBuilder();

        // store IDs from entries which we already added
        HashSet<String> added = new HashSet<String>();

        int counter = 0;

        for (String tag : tags) {

            ResultSet results = DatabaseManager.getInstance().runQuery("SELECT id, title, text, pageText, tags FROM entries WHERE tags LIKE '%" + tag + "%'");
            while (results.next() && counter < limit) {

                System.out.println("added " + counter);

                String text;
                String entryText = results.getString("text");
                String pageText = results.getString("pageText");
                String title = results.getString("title");
                String tagsStr = results.getString("tags");
                String id = results.getString("id");

                if (added.contains(id)) {
                    continue;
                }

                if (entryText == null && pageText == null) {
                    continue;
                } else if (entryText != null && entryText.length() > 0) {
                    text = entryText;
                    if (pageText != null) {
                        if (pageText.length() > entryText.length()) {
                            text = pageText;
                        }
                    }
                } else if (pageText != null && pageText.length() > 0) {
                    text = pageText;
                } else {
                    continue;
                }

                text = text.replaceAll("\n", "");
                text = text.replaceAll(SEPARATOR, " ");
                text = text.replaceAll("[^A-Za-z0-9 ]", "");
                text = text.replaceAll("\\s{2,}", " ");
                text = text.trim();

                // assign all tags
                List<String> assignTags = new LinkedList<String>();
                for (String temp : tags) {
                    if (tagsStr.toLowerCase().contains(temp.toLowerCase())) {
                        assignTags.add(temp);
                    }
                }

                String tagListStr = StringUtils.join(assignTags, SEPARATOR);
                if (tagListStr.length() > 0) {
                    String line = text + SEPARATOR + tagListStr + "\n";
                    trainFile.append(line);
                    added.add(id);
                    counter++;
                }
            }
        }

        System.out.println("wrote " + counter + " test documents");
        FileHelper.writeToFile(fileName, trainFile);
    }

    /**
     * Trains the Classifier using train file.
     * 
     * @param ttFile
     */
    private static void trainClass(String ttFile) {

        // create a classifier mananger object
        ClassifierManager classifierManager = new ClassifierManager();

        // use 80% of the data in the training/testing file as training data
        // the rest is used for testing
        classifierManager.setTrainingDataPercentage(80);

        // set separation string between training data and class
        // classifierManager.setSeparationString(SEPARATOR);

        // build and test the model
        // the second parameter specifies that we want to use URL features only
        // the third parameter specifies that we want to use
        // multi−category classification (tagging)
        // the last parameter is set to true in order to train not just test it
        // classifierManager.trainAndTestClassifier(ttFile, WebPageClassifier.URL, WebPageClassifier.TAG,
        // ClassifierManager.CLASSIFICATION_TRAIN_TEST_SERIALIZE);
        // classifierManager.trainAndTestClassifier(ttFile, WebPageClassifier.URL, WebPageClassifier.TAG,
        // ClassifierManager.CLASSIFICATION_TRAIN_TEST_SERIALIZE);
        // classifierManager.trainAndTestClassifier(ttFile, 123, WebPageClassifier.TAG, ClassifierManager.CLASSIFICATION_TRAIN_TEST_SERIALIZE);
        classifierManager.trainAndTestClassifier(ttFile, 123, ClassificationTypeSetting.TAG,
                ClassifierManager.CLASSIFICATION_TRAIN_TEST_VOLATILE);

    }

}
