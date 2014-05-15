package ws.palladian.extraction.content.evaluation;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.content.WebPageContentExtractor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.nlp.CharacterNGramSimilarity;
import ws.palladian.helper.nlp.JaroWinklerSimilarity;
import ws.palladian.helper.nlp.LevenshteinSimilarity;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.helper.nlp.StringSimilarity;

public class ContentExtractorEvaluation {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentExtractorEvaluation.class);

    private static final List<StringSimilarity> SIMILARITIES = createSimilarities();

    private static List<StringSimilarity> createSimilarities() {
        List<StringSimilarity> similarities = CollectionHelper.newArrayList();
        similarities.add(new LevenshteinSimilarity());
        similarities.add(new CharacterNGramSimilarity(5));
        similarities.add(new JaroWinklerSimilarity());
        return similarities;
    }

    private final List<WebPageContentExtractor> extractors = CollectionHelper.newArrayList();

    private final List<ContentExtractionDataset> datasets = CollectionHelper.newArrayList();

    public void add(WebPageContentExtractor extractor) {
        Validate.notNull(extractor, "extractor must not be null");
        extractors.add(extractor);
    }

    public void add(ContentExtractionDataset dataset) {
        Validate.notNull(dataset, "dataset must not be null");
        datasets.add(dataset);
    }

    public void evaluate() {

        for (WebPageContentExtractor extractor : extractors) {
            for (ContentExtractionDataset dataset : datasets) {
                for (ContentExtractionDatasetItem item : dataset) {

                    File htmlFile = item.getHtmlFile();

                    try {
                        extractor.setDocument(htmlFile);
                    } catch (Exception e) {
                        LOGGER.warn("Encountered {} for {}", e, item);
                    }

                    String expectedText = item.getExpectedText();
                    expectedText = cleanup(expectedText);

                    String extractedText = extractor.getResultText();
                    extractedText = cleanup(extractedText);

                    double[] similarities = new double[SIMILARITIES.size()];
                    boolean startCorrect = false;
                    boolean endCorrect = false;

                    for (int i = 0; i < SIMILARITIES.size(); i++) {
                        StringSimilarity similarity = SIMILARITIES.get(i);
                        similarities[i] = similarity.getSimilarity(expectedText, extractedText);
                    }

                    if (expectedText.length() > 25 && extractedText.length() > 25) {
                        // check, whether the beginning/end of the text were extracted correctly:
                        String expectedStart = expectedText.substring(0, 25);
                        String extractedStart = extractedText.substring(0, 25);
                        String expectedEnd = expectedText.substring(expectedText.length() - 25, expectedText.length());
                        String extractedEnd = extractedText.substring(extractedText.length() - 25,
                                extractedText.length());
                        startCorrect = expectedStart.equals(extractedStart);
                        endCorrect = expectedEnd.equals(extractedEnd);
                    }

                    String resultLine = String.format("%s;%f;%f;%f;%b;%b\n", htmlFile.getName(), similarities[0],
                            similarities[1], similarities[2], startCorrect, endCorrect);

                    System.out.println(resultLine);

                }
            }
        }
    }

    private static final String cleanup(String expectedText) {
        expectedText = expectedText.replaceAll("URL: [^ ]+", "");
        expectedText = expectedText.replaceAll("\\<.+?\\>", "");
        expectedText = StringHelper.replaceProtectedSpace(expectedText);
        expectedText = StringHelper.removeLineBreaks(expectedText);
        expectedText = expectedText.replaceAll("\\s+", " ");
        expectedText = expectedText.trim();
        return expectedText;
    }
}
