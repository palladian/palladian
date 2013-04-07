package ws.palladian.extraction.content;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.ConstantFactory;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.LevenshteinSimilarity;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.helper.nlp.StringSimilarity;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * <p>
 * Evaluates different {@link WebPageContentExtractor}s. The data set is provided by the authors of Boilerpipe. It
 * contains 621 web pages where humans manually annotated relevant content areas. For the evaluation we compare the data
 * from the data set which the extracted text using the Levenshtein similarity; the higher the similarity, the better
 * the extraction result, obviously.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public final class ContentExtractionEvaluation {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentExtractionEvaluation.class);

    /**
     * Whether only a comparison of the main content block should be done. If false, also user generated content is
     * compared.
     */
    private final Mode mode;

    public static enum Mode {
        MAIN_CONTENT, WHOLE_CONTENT
    }

    /** Base path with the evaluation data set. */
    private final String datasetPath;

    private final List<WebPageContentExtractor> extractors;

    public ContentExtractionEvaluation(String datasetPath, Mode mode, List<WebPageContentExtractor> extractors) {
        this.datasetPath = datasetPath;
        this.mode = mode;
        this.extractors = extractors;
    }

    /**
     * Do the evaluation for the dataset.
     * Map contains file's UUIDs as keys, real URLs as values.
     * 
     * @param dataset
     * @return
     */
    public void evaluate(Map<String, String> dataset, String outputFile) {

        FileHelper.delete(outputFile);

        boolean writeHeader = true;

        // keep statistics
        CountMap<WebPageContentExtractor> wins = CountMap.create();
        CountMap<WebPageContentExtractor> errors = CountMap.create();
        LazyMap<WebPageContentExtractor, Double> stats = LazyMap.create(ConstantFactory.create(0.));
        int totalSize = dataset.size();
        int index = 0;

        // loop through the dataset
        for (Entry<String, String> entry : dataset.entrySet()) {

            ProgressHelper.printProgress(index++, totalSize, 0);

            // evaluate all provided implementations
            LinkedHashMap<WebPageContentExtractor, Float> result = evaluate(entry.getKey());

            // write header for results file
            if (writeHeader) {
                String head = "UUID\tURL\t";
                for (WebPageContentExtractor technique : result.keySet()) {
                    head += technique.getExtractorName() + "\t";
                }
                FileHelper.appendFile(outputFile, head + "\n");
                writeHeader = false;
            }

            // write result data and score, determine winner
            String resultStr = entry.getKey() + "\t"; // file UUID
            resultStr += entry.getValue() + "\t"; // URL

            WebPageContentExtractor winner = null;
            float maxScore = -1;
            for (Entry<WebPageContentExtractor, Float> techniqueScore : result.entrySet()) {

                Float score = techniqueScore.getValue();
                WebPageContentExtractor technique = techniqueScore.getKey();

                resultStr += (score != -1 ? score : "### fail ### ") + "\t";

                if (score > maxScore) {
                    maxScore = score;
                    winner = technique;
                }
                if (score == -1) {
                    errors.add(technique);
                } else {
                    stats.put(technique, stats.get(technique) + score);
                }
            }

            if (winner != null) {
                wins.add(winner);
            }

            FileHelper.appendFile(outputFile, resultStr + "\n");

        }

        FileHelper.appendFile(outputFile, "------------- stats ------------------\n");
        for (WebPageContentExtractor extractor : extractors) {
            String extractorStats = " " + extractor.getExtractorName() + "\t#wins:" + wins.getCount(extractor) + "\t#errors:"
                    + errors.getCount(extractor) + "\tavg. score:" + stats.get(extractor) / dataset.size();
            FileHelper.appendFile(outputFile, extractorStats + "\n");
        }
    }

    private LinkedHashMap<WebPageContentExtractor, Float> evaluate(String uuid) {

        LinkedHashMap<WebPageContentExtractor, Float> result = new LinkedHashMap<WebPageContentExtractor, Float>();
        String realText = getRealText(uuid);
        String testDataPath = datasetPath + "/original/" + uuid + ".html";

        for (WebPageContentExtractor extractor : extractors) {

            float score = -1;

            try {
                String resultText = extractor.setDocument(testDataPath).getResultText();
                score = (float) getScore(realText, resultText);
            } catch (Exception e) {
                // ignore. show error in results.
            }

            result.put(extractor, score);

        }

        return result;

    }

    private String getRealText(String uuid) {

        StringBuilder sb = new StringBuilder();

        // get the manually annotated document from the data set
        String fileName = datasetPath + "/annotated/" + uuid + ".html";
        Document annotatedDocument = null;
        try {
            annotatedDocument = ParserFactory.createHtmlParser().parse(new File(fileName));
        } catch (ParserException e) {
            LOGGER.warn("Error parsing " + fileName);
        }

        if (annotatedDocument != null) {

            // get the real content data, which is wrapped in <SPAN> tags with class 'x-nc-sel2' and the additional user
            // data from tags with 'x-nc-sel5'
            String xPath;
            if (mode == Mode.MAIN_CONTENT) {
                xPath = "//text()[ancestor::*[contains(@class,'x-nc-sel')][1]/@class='x-nc-sel2']";
            } else {
                xPath = "//text()[ancestor::*[contains(@class,'x-nc-sel')][1]/@class='x-nc-sel2' or ancestor::*[contains(@class,'x-nc-sel')][1]/@class='x-nc-sel5']";
            }

            // get nodes containing the tagged text
            List<Node> nodes = XPathHelper.getXhtmlNodes(annotatedDocument, xPath);
            for (Node node : nodes) {
                sb.append(node.getTextContent()).append(" ");
            }
        }

        return sb.toString();
    }

    /**
     * Get the score for the extracted text.
     * 
     * @param real The expected text as tagged in the dataset.
     * @param extracted The extracted text.
     * @return The similarity score between the two texts.
     */
    private double getScore(String real, String extracted) {
        real = normalizeString(real);
        extracted = normalizeString(extracted);

        StringSimilarity similarity = new LevenshteinSimilarity();
        return similarity.getSimilarity(real, extracted);
    }

    /**
     * Reads the index file from the data set, returns Map with data.
     * 
     * @return
     */
    public Map<String, String> readIndexFile() {

        final Pattern split = Pattern.compile("<urn:uuid:([a-z0-9\\-]*?)>\\s(.*?)");
        final Map<String, String> data = new HashMap<String, String>();

        FileHelper.performActionOnEveryLine(datasetPath + "/url-mapping.txt", new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                Matcher matcher = split.matcher(line);

                if (matcher.matches() && matcher.groupCount() == 2) {
                    String uuid = matcher.group(1);
                    String url = matcher.group(2);
                    data.put(uuid, url);
                }

            }
        });

        return data;

    }

    /**
     * Normalizes the supplied string by stripping new lines, multiple white spaces. Also protected white space
     * characters are removed, as they cause trouble in conjunction with Simmetrics library.
     * 
     * @param input
     * @return
     */
    private static String normalizeString(String input) {
        input = StringHelper.replaceProtectedSpace(input);
        input = input.replace("\n", " ");
        return input.replaceAll(" {2,}", " ");
    }

    public static void main(String[] args) {

        List<WebPageContentExtractor> extractors = CollectionHelper.newArrayList();
        // extractors to evaluate
        // extractors.add(new BoilerpipeContentExtractor());
        extractors.add(new ReadabilityContentExtractor());
        extractors.add(new PalladianContentExtractor());
        // extractors.add(new NewsseecrContentExtractor());

        String datasetPath = "/Users/pk/Dropbox/Uni/Datasets/L3S-GN1-20100130203947-00001";

        // ////////////////////////////////////////////////////////////////////////////////

        ContentExtractionEvaluation evaluation = new ContentExtractionEvaluation(datasetPath, Mode.MAIN_CONTENT, extractors);
        Map<String, String> dataset = evaluation.readIndexFile();
        String filePath = "data/evaluation/ContentExtractionEvaluation_mainContentOnly.tsv";
        evaluation.evaluate(dataset, filePath);

        // evaluation = new ContentExtractionEvaluation(datasetPath, Mode.WHOLE_CONTENT, extractors);
        // filePath = "data/evaluation/ContentExtractionEvaluation_mainAndUserContent.tsv";
        // evaluation.evaluate(dataset, filePath);
    }

}
