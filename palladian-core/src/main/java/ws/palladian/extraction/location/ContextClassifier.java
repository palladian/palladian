package ws.palladian.extraction.location;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMatrix;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.Tagger;
import ws.palladian.processing.features.Annotated;

public class ContextClassifier {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ContextClassifier.class);

    public static enum Mode {
        BOW, NGRAM
    }

    private final CountMatrix<String> leftContexts = CountMatrix.create();
    private final CountMatrix<String> rightContexts = CountMatrix.create();

    public ContextClassifier() {
        File patternDirectory = new File("/Users/pk/Desktop/contextPatterns");
        load(leftContexts, new File(patternDirectory, "location_left_prox_4.csv"), "LOCATION");
        load(rightContexts, new File(patternDirectory, "location_right_prox_4.csv"), "LOCATION");
        load(leftContexts, new File(patternDirectory, "person_left_prox_4.csv"), "PERSON");
        load(rightContexts, new File(patternDirectory, "person_right_prox_4.csv"), "PERSON");
        // load(leftContexts, new File(patternDirectory, "location_left_1.csv"), "LOCATION");
        // load(rightContexts, new File(patternDirectory, "location_right_1.csv"), "LOCATION");
        // load(leftContexts, new File(patternDirectory, "person_left_1.csv"), "PERSON");
        // load(rightContexts, new File(patternDirectory, "person_right_1.csv"), "PERSON");
    }

    private static void load(final CountMatrix<String> countMatrix, File file, final String type) {
        FileHelper.performActionOnEveryLine(file, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                countMatrix.add(type, line);
            }
        });
    }

    public CategoryEntries classify(String text, Annotated annotation) {
        CategoryEntriesMap result = new CategoryEntriesMap();
        String left = getLeftContext(annotation, text, 1).toLowerCase().trim();
        if (left != null) {
            if (leftContexts.getCount("PERSON", left) > 0) {
                LOGGER.info(annotation + " is a person!" + "(" + left + ")");
                result.add("PERSON", 1.);
            }
            if (leftContexts.getCount("LOCATION", left) > 0) {
                LOGGER.info(annotation + " is a location!" + "(" + left + ")");
                result.add("LOCATION", 1.);
            }
        }
        String right = getRightContext(annotation, text, 1).toLowerCase().trim();
        if (right != null) {
            if (rightContexts.getCount("PERSON", right) > 0) {
                LOGGER.info(annotation + " is a person!" + "(" + right + ")");
                result.add("PERSON", 1.);
            }
            if (rightContexts.getCount("LOCATION", right) > 0) {
                LOGGER.info(annotation + " is a location!" + "(" + right + ")");
                result.add("LOCATION", 1.);
            }
        }
        result.computeProbabilities();
        result.sort();
        return result;
    }

    public CategoryEntries classifyBow(String text, Annotated annotation) {
        CategoryEntriesMap result = new CategoryEntriesMap();
        List<String> left = getLeftContexts(annotation, text, 4);
        for (String leftToken : left) {
            if (leftContexts.getCount("PERSON", leftToken.toLowerCase().trim()) > 0) {
                LOGGER.debug(annotation + " is a person!" + "(" + left + ")");
                result.add("PERSON", 1.);
            }
            if (leftContexts.getCount("LOCATION", leftToken.toLowerCase().trim()) > 0) {
                LOGGER.debug(annotation + " is a location!" + "(" + left + ")");
                result.add("LOCATION", 1.);
            }
        }
        List<String> right = getRightContexts(annotation, text, 4);
        for (String rightToken : right) {

            if (rightContexts.getCount("PERSON", rightToken.toLowerCase().trim()) > 0) {
                LOGGER.debug(annotation + " is a person!" + "(" + right + ")");
                result.add("PERSON", 1.);
            }
            if (rightContexts.getCount("LOCATION", rightToken.toLowerCase().trim()) > 0) {
                LOGGER.debug(annotation + " is a location!" + "(" + right + ")");
                result.add("LOCATION", 1.);
            }
        }
        result.computeProbabilities();
        result.sort();
        return result;
    }

    public List<Annotated> filter(List<Annotated> annotations, String text) {
        List<Annotated> result = CollectionHelper.newArrayList();
        for (Annotated annotation : annotations) {
            CategoryEntries classification = classify(text, annotation);
            LOGGER.info("Classification for {}: {}", annotation.getValue(), classification);
            boolean remove = classification.getProbability("PERSON") > 0.5;
            if (!remove) {
                result.add(annotation);
            } else {
                // LOGGER.info("Remove {}", annotation);
            }
        }
        LOGGER.info("Context classification reduced from {} to {}", annotations.size(), result.size());
        return result;
    }

    public static String getLeftContext(Annotated annotation, String text, int numWords) {
        try {
            StringBuilder builder = new StringBuilder();
            int wordCounter = 0;
            int start = annotation.getStartPosition() - 1;
            for (int i = start; i >= 0; i--) {
                char current = text.charAt(i);
                if (current == ' ' && i < start) {
                    wordCounter++;
                }
                if (wordCounter >= numWords || current == '\n' || current == '.') {
                    break;
                }
                builder.append(current);
            }
            return StringHelper.reverseString(builder.toString()).trim();
        } catch (Exception e) {
            // this exception is only caused by nested annotations as far as i can see
            // System.out.println("Exception for:");
            // System.out.println("Text:\n");
            // System.out.println(text);
            // System.out.println(annotation);
            // throw new IllegalStateException(e);
        }
        return null;
    }

    public static String getRightContext(Annotated annotation, String text, int numWords) {
        try {
            StringBuilder builder = new StringBuilder();
            int wordCounter = 0;
            int start = annotation.getEndPosition();
            for (int i = start; i < text.length(); i++) {
                char current = text.charAt(i);
                if (current == ' ' && i > start) {
                    wordCounter++;
                }
                if (wordCounter >= numWords || current == '\n' || current == '.') {
                    break;
                }
                builder.append(current);
            }
            return builder.toString().trim();
        } catch (Exception e) {
            // see above.
        }
        return null;
    }

    public static List<String> getLeftContexts(Annotated annotated, String text, int numWords) {
        return Arrays.asList(getLeftContext(annotated, text, numWords).split("\\s"));
    }

    public static List<String> getRightContexts(Annotated annotated, String text, int numWords) {
        return Arrays.asList(getRightContext(annotated, text, numWords).split("\\s"));
    }

    public static void main(String[] args) {
        String text = FileHelper
                .readFileToString("/Users/pk/Dropbox/Uni/Dissertation_LocationLab/LGL-converted/2-validation/text_41515177.txt");
        // String text = FileHelper.readFileToString("src/test/resources/Dresden.wikipedia");
        // text = WikipediaUtil.stripMediaWikiMarkup(text);
        // String text = "ruler of Saxony Frederick Augustus I became King";
        text = HtmlHelper.stripHtmlTags(text);

        // Tagger tagger = new StringTagger();
        Tagger tagger = new EntityPreprocessingTagger();
        List<? extends Annotated> annotations = tagger.getAnnotations(text);
        ContextClassifier classifier = new ContextClassifier();
        for (Annotated annotation : annotations) {
            CategoryEntries result = classifier.classifyBow(text, annotation);
            // CategoryEntries result = classifier.classify(text, annotation);
            System.out.println(annotation.getValue() + " : " + result);
        }
    }

}
