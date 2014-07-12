package ws.palladian.extraction.content.evaluation;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.content.PalladianContentExtractor;
import ws.palladian.extraction.content.ReadabilityContentExtractor;
import ws.palladian.extraction.content.WebPageContentExtractor;
import ws.palladian.extraction.content.evaluation.BoilerpipeDataset.Mode;
import ws.palladian.extraction.content.evaluation.ContentExtractionDataset.ContentExtractionPage;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.CharacterNGramSimilarity;
import ws.palladian.helper.nlp.JaroWinklerSimilarity;
import ws.palladian.helper.nlp.LevenshteinSimilarity;
import ws.palladian.helper.nlp.StringMetric;

public final class ContentExtractorEvaluation {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentExtractorEvaluation.class);

    private static final List<StringMetric> SIMILARITIES = createSimilarities();

    private static List<StringMetric> createSimilarities() {
        List<StringMetric> similarities = CollectionHelper.newArrayList();
        similarities.add(new LevenshteinSimilarity());
        similarities.add(new CharacterNGramSimilarity(5));
        similarities.add(new JaroWinklerSimilarity());
        return similarities;
    }

    private final List<WebPageContentExtractor> extractors = CollectionHelper.newArrayList();

    private final List<ContentExtractionDataset> datasets = CollectionHelper.newArrayList();

    public void addExtractor(WebPageContentExtractor extractor) {
        Validate.notNull(extractor, "extractor must not be null");
        extractors.add(extractor);
    }

    public void addDataset(ContentExtractionDataset dataset) {
        Validate.notNull(dataset, "dataset must not be null");
        datasets.add(dataset);
    }

    public void evaluate() {

        ProgressReporter progress = new ProgressMonitor();
        long numSteps = 0;
        for (ContentExtractionDataset dataset : datasets) {
            numSteps += dataset.size();
        }
        numSteps *= extractors.size();
        progress.startTask("ContentExtractorEvaluation", numSteps);

        String summaryFileName = "_contentExtractorEvaluation_" + System.currentTimeMillis() + ".csv";
        StringBuilder csvHeader = new StringBuilder().append("extractor;dataset;");
        for (StringMetric similarity : SIMILARITIES) {
            csvHeader.append(similarity.toString()).append(';');
        }
        csvHeader.append("startCorrect;endCorrect;time\n");
        FileHelper.appendFile(summaryFileName, csvHeader);

        for (WebPageContentExtractor extractor : extractors) {

            for (ContentExtractionDataset dataset : datasets) {

                StringBuilder resultCsv = new StringBuilder();
                double[] avgSimilarities = new double[SIMILARITIES.size()];
                int numStartCorrect = 0;
                int numEndCorrect = 0;
                StopWatch stopWatch = new StopWatch();

                for (ContentExtractionPage page : dataset) {

                    File htmlFile = page.getHtmlFile();

                    try {
                        extractor.setDocument(htmlFile);
                    } catch (Exception e) {
                        LOGGER.warn("Encountered {} for {}", e, page);
                    }

                    String expectedText = page.getExpectedText().trim();
                    String extractedText = extractor.getResultText().trim();

                    double[] similarities = new double[SIMILARITIES.size()];
                    boolean startCorrect = false;
                    boolean endCorrect = false;

                    for (int i = 0; i < SIMILARITIES.size(); i++) {
                        double similarity = SIMILARITIES.get(i).getSimilarity(expectedText, extractedText);
                        similarities[i] = similarity;
                        avgSimilarities[i] += similarity;
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
                    if (startCorrect) {
                        numStartCorrect++;
                    }
                    if (endCorrect) {
                        numEndCorrect++;
                    }

                    resultCsv.append(htmlFile.getName()).append(';');
                    for (double similarity : similarities) {
                        resultCsv.append(similarity).append(';');
                    }
                    resultCsv.append(startCorrect).append(';');
                    resultCsv.append(endCorrect);
                    resultCsv.append('\n');
                    progress.increment();
                }
                StringBuilder fileContent = new StringBuilder();
                fileContent.append(extractor.getExtractorName()).append('\n');
                fileContent.append(dataset.toString()).append("\n\n");
                fileContent.append("Time: ").append(stopWatch.getElapsedTime());
                fileContent.append(" (").append(stopWatch.getElapsedTimeString()).append(')').append('\n');
                fileContent.append('\n');
                fileContent.append("Average similarities:\n");
                int numPages = dataset.size();
                double startCorrectPercentage = 100 * (double)numStartCorrect / numPages;
                double endCorrectPercentage = 100 * (double)numEndCorrect / numPages;
                for (int i = 0; i < SIMILARITIES.size(); i++) {
                    fileContent.append(SIMILARITIES.get(i).toString());
                    fileContent.append(": ");
                    fileContent.append(avgSimilarities[i] / numPages).append('\n');
                }
                fileContent.append('\n');
                fileContent.append("# pages: ").append(numPages).append('\n');
                fileContent.append("% correct start: ").append(startCorrectPercentage).append('\n');
                fileContent.append("% correct end: ").append(endCorrectPercentage).append('\n');
                fileContent.append('\n');
                fileContent.append("Individual similarities:\n");
                fileContent.append(resultCsv);
                String fileName = "ContentExtractorEvaluation_" + extractor.getExtractorName() + "_"
                        + dataset.toString() + "_" + System.currentTimeMillis() + ".csv";
                FileHelper.writeToFile(fileName, fileContent);
                // summary
                StringBuilder summaryCsv = new StringBuilder();
                summaryCsv.append(extractor.getExtractorName()).append(';');
                summaryCsv.append(dataset.toString()).append(';');
                for (int i = 0; i < SIMILARITIES.size(); i++) {
                    summaryCsv.append(avgSimilarities[i] / numPages).append(';');
                }
                summaryCsv.append(startCorrectPercentage).append(';');
                summaryCsv.append(endCorrectPercentage).append(';');
                summaryCsv.append(stopWatch.getElapsedTime()).append('\n');
                FileHelper.appendFile(summaryFileName, summaryCsv);
            }
        }
    }

    public static void main(String[] args) {
        ContentExtractorEvaluation evaluation = new ContentExtractorEvaluation();
        evaluation.addExtractor(new ReadabilityContentExtractor());
        evaluation.addExtractor(new PalladianContentExtractor());
        evaluation.addDataset(new CleanevalDataset(new File("/Users/pk/Desktop/CleanEval")));
        evaluation.addDataset(new TudContentExtractionDataset(new File("/Users/pk/Desktop/TUD_ContentExtractionDataset_2014-01-28")));
        evaluation.addDataset(new BoilerpipeDataset(new File("/Users/pk/Desktop/L3S-GN1-20100130203947-00001"), Mode.MAIN_CONTENT));
        evaluation.addDataset(new BoilerpipeDataset(new File("/Users/pk/Desktop/L3S-GN1-20100130203947-00001"), Mode.WHOLE_CONTENT));
        evaluation.evaluate();
    }

}
