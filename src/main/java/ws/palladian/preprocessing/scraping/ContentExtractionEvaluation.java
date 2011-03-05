package ws.palladian.preprocessing.scraping;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.LineAction;
import ws.palladian.helper.StringHelper;
import ws.palladian.helper.XPathHelper;
import ws.palladian.web.Crawler;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.ExtractorBase;

/**
 * Evaluates two page content extraction techniques:
 * 
 * 1) Boilerpipe, as described in "Boilerplate Detection using Shallow Text Features"; Kohlschütter, Christian;
 * Fankhauser, Peter; Nejdl, Wolfgang; 2010 and available on http://code.google.com/p/boilerpipe/ and
 * http://www.l3s.de/~kohlschuetter/boilerplate/
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

    /** Base path with the evaluation data set. */
    private final String BASE_PATH;

    /** Use the Crawler to retrieve documents. */
    private Crawler crawler = new Crawler();

    /**
     * Whether only a comparison of the main content block should be done. If false, also user generated content is
     * compared.
     */
    private boolean mainContentOnly = true;

    public ContentExtractionEvaluation() {
        crawler.setFeedAutodiscovery(false);
        BASE_PATH = ConfigHolder.getInstance().getConfig().getString("datasets.boilerplate");
    }

    /**
     * Reads the index file from the data set, returns Map with data.
     * 
     * @return
     */
    public Map<String, String> readIndexFile() {

        final Pattern split = Pattern.compile("<urn:uuid:([a-z0-9\\-]*?)>\\s(.*?)");
        final Map<String, String> data = new HashMap<String, String>();

        FileHelper.performActionOnEveryLine(BASE_PATH + "/url-mapping.txt", new LineAction() {

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

    public boolean isMainContentOnly() {
        return mainContentOnly;
    }

    public void setMainContentOnly(boolean mainContentOnly) {
        this.mainContentOnly = mainContentOnly;
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
        LOGGER.info("File name in test set\tURL\tBoilerplate score\tPalladian score");
        int count = 0;
        int[] stats = { 0, 0, 0 };
        float[] score = { 0, 0 };
        int[] errors = { 0, 0 };

        for (Entry<String, String> entry : dataset.entrySet()) {

            float[] result = evaluate(entry.getKey());

            String resultStr = entry.getKey() + "\t"; // file UUID
            resultStr += entry.getValue() + "\t"; // URL
            resultStr += (result[0] != -1 ? result[0] : "fail") + "\t"; // Boilerplate result
            resultStr += result[1] != -1 ? result[1] : "fail"; // Palladian result

            if (result[0] > result[1]) {
                stats[0]++;
            } else if (result[0] < result[1]) {
                stats[1]++;
            } else {
                stats[2]++;
            }

            if (result[0] == -1) {
                errors[0]++;
            } else {
                score[0] += result[0];
            }
            if (result[1] == -1) {
                errors[1]++;
            } else {
                score[1] += result[1];
            }

            count++;

            LOGGER.info(resultStr);
            sb.append(resultStr).append("\n");

        }

        LOGGER.info("------------- and the winner is --------------");
        LOGGER.info(" # Boilerpipe  : " + stats[0]);
        LOGGER.info(" # Palladian   : " + stats[1]);
        LOGGER.info(" # Equality    : " + stats[2]);
        LOGGER.info("------------------ score ---------------------");
        LOGGER.info(" Boilerpipe    : " + score[0] / (count - errors[0]));
        LOGGER.info(" Palladian     : " + score[1] / (count - errors[1]));
        LOGGER.info("----------------- errors ---------------------");
        LOGGER.info(" Boilerpipe    : " + errors[0]);
        LOGGER.info(" Palladian     : " + errors[1]);

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
    private float[] evaluate(String uuid) {

        float[] result = { -1, -1 };

        String realText = getRealText(uuid);
        String testDataPath = BASE_PATH + "original/" + uuid + ".html";

        // use Boilerplate technique ////////////////////////////////////////
        try {
            // Boilerplate provides different Extractor implementations.
            // I use the ArticleExtractor here which gave best results at first glance.
            ExtractorBase boilerplateExtractor = ArticleExtractor.INSTANCE;
            String boilerplateExtracted = boilerplateExtractor.getText(new URL("file://" + testDataPath));
            // String boilerplateExtracted = new PageSentenceExtractor().setDocument(testDataPath).getMainContentText();
            result[0] = getScore(realText, boilerplateExtracted);
        } catch (Exception e) {
            // ignore. show error in results.
        }

        // use our Palladian's PageContentExtractor /////////////////////////
        try {
            PageContentExtractor palladianExtractor = new PageContentExtractor();
            palladianExtractor.setDocument(testDataPath);
            String palladianExtracted = palladianExtractor.getResultText();
            result[1] = getScore(realText, palladianExtracted);
        } catch (Exception e) {
            // ignore. show error in results.
        }

        return result;

    }

    private String getRealText(String uuid) {

        StringBuilder sb = new StringBuilder();
        
        // get the manually annotated document from the data set
        Document annotatedDocument = crawler.getWebDocument(BASE_PATH + "annotated/" + uuid + ".html");

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

    /**
     * Normalizes the supplied string by stripping new lines, multiple white spaces. Also protected white space
     * characters are removed, as they cause trouble in conjunction with Simmetrics library.
     * 
     * @param input
     * @return
     */
    public static String normalizeString(String input) {

        input = StringHelper.removeProtectedSpace(input);
        input = input.replace("\n", " ");
        input = input.replaceAll(" {2,}", " ");
        return input;

    }

    public static void main(String[] args) {

        ContentExtractionEvaluation evaluation = new ContentExtractionEvaluation();

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

        evaluation.setMainContentOnly(false);
        result = evaluation.evaluate(dataset);
        FileHelper.writeToFile("data/evaluation/ContentExtractionEvaluation_mainAndUserContent.tsv", result);
    }

}
