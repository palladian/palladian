package ws.palladian.extraction.location;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.extraction.entity.StringTagger;
import ws.palladian.helper.collection.CountMatrix;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.features.Annotated;

public class ContextClassifier {

    private final CountMatrix<String> leftContexts = CountMatrix.create();
    private final CountMatrix<String> rightContexts = CountMatrix.create();

    public ContextClassifier() {
        load(leftContexts, "/Users/pk/Desktop/WikipediaContextsPruned/leftContexts_1.csv");
        load(leftContexts, "/Users/pk/Desktop/WikipediaContextsPruned/leftContexts_2.csv");
        load(leftContexts, "/Users/pk/Desktop/WikipediaContextsPruned/leftContexts_3.csv");
        load(leftContexts, "/Users/pk/Desktop/WikipediaContextsPruned/leftContexts_4.csv");
        load(rightContexts, "/Users/pk/Desktop/WikipediaContextsPruned/rightContexts_1.csv");
        load(rightContexts, "/Users/pk/Desktop/WikipediaContextsPruned/rightContexts_2.csv");
        load(rightContexts, "/Users/pk/Desktop/WikipediaContextsPruned/rightContexts_3.csv");
        load(rightContexts, "/Users/pk/Desktop/WikipediaContextsPruned/rightContexts_4.csv");
    }

    private static void load(final CountMatrix<String> countMap, String filePath) {
        FileHelper.performActionOnEveryLine(filePath, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split("###");
                String value = split[0];
                countMap.set(value, "LOC", Integer.valueOf(split[1]));
                countMap.set(value, "MISC", Integer.valueOf(split[2]));
                countMap.set(value, "ORG", Integer.valueOf(split[3]));
                countMap.set(value, "PER", Integer.valueOf(split[4]));
            }
        });
    }

    public CategoryEntries classify(String text, Annotated annotation) {
        CategoryEntriesMap result = new CategoryEntriesMap();
        for (int i = 1; i <= 4; i++) {
            String left = getLeftContext(annotation, text, i).toLowerCase().trim();
            if (!left.contains(".")) {
                List<Pair<String, Integer>> probabilities = leftContexts.getColumn(left);
                for (Pair<String, Integer> probability : probabilities) {
                    result.add(probability.getKey(), probability.getValue());
                }
            }
            String right = getRightContext(annotation, text, i).toLowerCase().trim();
            if (!right.contains(".")) {
                List<Pair<String, Integer>> probabilities = rightContexts.getColumn(right);
                for (Pair<String, Integer> probability : probabilities) {
                    result.add(probability.getKey(), probability.getValue());
                }
            }
        }
        result.sort();
        result.computeProbabilities();
        return result;
    }

    private static String getLeftContext(Annotated annotation, String text, int numWords) {
        String temp = text.substring(0, annotation.getStartPosition());
        Pattern leftPattern = Pattern.compile(String.format("(\\w+[^\\w]{1,5}){0,%s}$", numWords));
        return StringHelper.getRegexpMatch(leftPattern, temp);
    }

    private static String getRightContext(Annotated annotation, String text, int numWords) {
        String temp = text.substring(annotation.getEndPosition());
        Pattern rightPattern = Pattern.compile(String.format("^([^\\w]{1,5}\\w+){0,%s}", numWords));
        return StringHelper.getRegexpMatch(rightPattern, temp);
    }

    public static void main(String[] args) {
        String text = FileHelper.readFileToString("src/test/resources/testTextAddresses.txt");
        // String text = FileHelper.readFileToString("src/test/resources/Dresden.wikipedia");
        // text = WikipediaUtil.stripMediaWikiMarkup(text);
        // String text = "ruler of Saxony Frederick Augustus I became King";
        text = HtmlHelper.stripHtmlTags(text);

        StringTagger tagger = new StringTagger();
        List<Annotated> annotations = tagger.getAnnotations(text);
        ContextClassifier classifier = new ContextClassifier();
        for (Annotated annotation : annotations) {
            CategoryEntries result = classifier.classify(text, annotation);
            if (result != null) {
                System.out.println(annotation.getValue() + " : " + result.getProbability("LOC"));
            }
        }
    }

}
