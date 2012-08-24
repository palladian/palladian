package ws.palladian.extraction.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * Evaluates two page content extraction techniques:
 * 
 * 
 * 2) PageContentExtractor from Palladian which was ported from Readability available from
 * http://lab.arc90.com/experiments/readability
 * 
 * We use the data set which is provided by the authors of Boilerpipe and available on the project's web page. It
 * contains 621 web pages where humans manually annotated relevant content areas. For the evaluation we compare the data
 * from the data set which the extracted text by Boilerpipe and PageContentExtractor. Therefore we use the Levenshtein
 * similarity, the higher the similarity, the better the extraction result, obviously.
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public class ContentExtractionEvaluation {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ContentExtractionEvaluation.class);

    public static void main(String[] args) {

        ContentExtractionEvaluation evaluation = new ContentExtractionEvaluation();

        // extractors to evaluate
        evaluation.addExtractor(new BoilerpipeContentExtractor());
        evaluation.addExtractor(new ReadabilityContentExtractor());
        evaluation.addExtractor(new PalladianContentExtractor());

        // String text = evaluation.getRealText("ec407a8c-7d1b-4485-816d-39a1887f84b3");
        // text = normalizeString(text);
        // System.out.println(text);
        //
        // System.exit(0);

        // ////////////////////////////////////////////////////////////////////////////////

        Map<String, String> dataset = evaluation.readIndexFile();
        evaluation.setMainContentOnly(true);
        String result = evaluation.evaluate(dataset);
        FileHelper.writeToFile("data/evaluation/ContentExtractionEvaluation_mainContentOnly.tsv", result);

        System.exit(0);

        evaluation.setMainContentOnly(false);
        result = evaluation.evaluate(dataset);
        FileHelper.writeToFile("data/evaluation/ContentExtractionEvaluation_mainAndUserContent.tsv", result);
    }

    /**
     * Normalizes the supplied string by stripping new lines, multiple white spaces. Also protected white space
     * characters are removed, as they cause trouble in conjunction with Simmetrics library.
     * 
     * @param input
     * @return
     */
    public static String normalizeString(String input) {

        input = StringHelper.replaceProtectedSpace(input);
        input = input.replace("\n", " ");
        input = input.replaceAll(" {2,}", " ");
        return input;

    }

    /** Base path with the evaluation data set. */
    private final String datasetPath;

    /** Use the Crawler to retrieve documents. */
    private DocumentRetriever crawler = new DocumentRetriever();

    private List<WebPageContentExtractor> extractors = new ArrayList<WebPageContentExtractor>();

    /**
     * Whether only a comparison of the main content block should be done. If false, also user generated content is
     * compared.
     */
    private boolean mainContentOnly = true;

    public ContentExtractionEvaluation() {
        datasetPath = ConfigHolder.getInstance().getConfig().getString("datasets.boilerplate");
    }

    public void addExtractor(WebPageContentExtractor extractor) {
        this.extractors.add(extractor);
    }

    /**
     * Do the evaluation for the dataset.
     * Map contains file's UUIDs as keys, real URLs as values.
     * 
     * @param dataset
     * @return
     */
    public String evaluate(Map<String, String> dataset) {

        StringBuilder sb = new StringBuilder();

        boolean writeHeader = true;

        // keep statistics
        Bag<WebPageContentExtractor> wins = new HashBag<WebPageContentExtractor>();
        Bag<WebPageContentExtractor> errors = new HashBag<WebPageContentExtractor>();
        Map<WebPageContentExtractor, Double> stats = new HashMap<WebPageContentExtractor, Double>();

        // loop through the dataset
        for (Entry<String, String> entry : dataset.entrySet()) {

            // evaluate all provided implementations
            LinkedHashMap<WebPageContentExtractor, Float> result = evaluate(entry.getKey());

            // write header for results file
            if (writeHeader) {
                String head = "UUID\tURL\t";
                for (WebPageContentExtractor technique : result.keySet()) {
                    head += technique.getExtractorName() + "\t";
                }
                LOGGER.info(head);
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
                    Double scoreSum = stats.get(technique);
                    if (scoreSum == null) {
                        scoreSum = 0.;
                    }
                    stats.put(technique, scoreSum + score);
                }
            }

            if (winner != null) {
                wins.add(winner);
            }

            LOGGER.info(resultStr);
            sb.append(resultStr).append("\n");

        }

        LOGGER.info("------------- stats ------------------");
        for (WebPageContentExtractor extractor : extractors) {
            LOGGER.info(" " + extractor.getExtractorName() + "\t#wins:" + wins.getCount(extractor) + "\t#errors:"
                    + errors.getCount(extractor) + "\tavg. score:" + (double) stats.get(extractor) / dataset.size());
        }

        return sb.toString();
    }

    /**
     * Return:<br>
     * 
     * float[0] -> Boilerplate score<br>
     * float[1] -> Palladian score<br>
     * 
     * @param uuid
     * @return
     */
    private LinkedHashMap<WebPageContentExtractor, Float> evaluate(String uuid) {

        LinkedHashMap<WebPageContentExtractor, Float> result = new LinkedHashMap<WebPageContentExtractor, Float>();
        String realText = getRealText(uuid);
        String testDataPath = datasetPath + "original/" + uuid + ".html";

        for (WebPageContentExtractor extractor : extractors) {

            float score = -1;

            try {
                String resultText = extractor.setDocument(testDataPath).getResultText();
                score = getScore(realText, resultText);
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
        Document annotatedDocument = crawler.getWebDocument(datasetPath + "annotated/" + uuid + ".html");

        if (annotatedDocument != null) {

            // get the real content data, which is wrapped in <SPAN> tags with class 'x-nc-sel2' and the additional user
            // data from tags with 'x-nc-sel5'
            String xPath;
            if (isMainContentOnly()) {
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
    private float getScore(String real, String extracted) {

        real = normalizeString(real);
        extracted = normalizeString(extracted);

        // getting StackOverflow for Simmetrics, use StringUtils instead.
        // return similarityMetric.getSimilarity(real, extracted);

        return StringHelper.getLevenshteinSim(real, extracted);
    }

    public boolean isMainContentOnly() {
        return mainContentOnly;
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

    public void setMainContentOnly(boolean mainContentOnly) {
        this.mainContentOnly = mainContentOnly;
    }

}
