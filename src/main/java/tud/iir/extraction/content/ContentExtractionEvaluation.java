package tud.iir.extraction.content;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import tud.iir.helper.FileHelper;
import tud.iir.helper.LineAction;
import tud.iir.helper.StringHelper;
import tud.iir.helper.XPathHelper;
import tud.iir.web.Crawler;
// import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
// import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;
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
 * from the data set which the extracted text by Boilerpipe and PageContentExtractor. Therefor we use the Levenshtein
 * similarity, the higher the similarity, the better the extraction result, obviously.
 * 
 * @author Philipp Katz
 * 
 */
public class ContentExtractionEvaluation {

    /** base path with the evaluation data set. */
    private static final String BASE_PATH = "/home/pk/Desktop/L3S-GN1-20100130203947-00001/";

    private Crawler crawler = new Crawler();

    /** the similarity metric to be used for scoring. */
    // private AbstractStringMetric similarityMetric = new Levenshtein();

    public ContentExtractionEvaluation() {
        crawler.setFeedAutodiscovery(false);
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

    /**
     * Do the evaluation for the dataset.
     * Map contains file's UUIDs as keys, real URLs as values.
     * 
     * @param dataset
     * @return
     */
    public String evaluate(Map<String, String> dataset) {

        StringBuilder sb = new StringBuilder();
        System.out.println("File name in test set\tURL\tBoilerplate score\tPalladian score");
        int count = 0;
        int[] stats = { 0, 0, 0 };
        float[] score = { 0, 0 };
        int[] errors = { 0, 0 };

        for (Entry<String, String> entry : dataset.entrySet()) {

            float[] result = evaluate(entry.getKey());

            String resultStr = entry.getKey() + "\t"; // file UUID
            resultStr += entry.getValue() + "\t"; // URL
            resultStr += (result[0] != -1 ? result[0] : "fail") + "\t"; // Boilerplate result
            resultStr += (result[1] != -1 ? result[1] : "fail"); // Palladian result

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
            
            

            System.out.println(resultStr);
            sb.append(resultStr).append("\n");

        }

        System.out.println("------------- and the winner is --------------");
        System.out.println(" # Boilerpipe  : " + stats[0]);
        System.out.println(" # Palladian   : " + stats[1]);
        System.out.println(" # Equality    : " + stats[2]);
        System.out.println("------------------ score ---------------------");
        System.out.println(" Boilerpipe    : " + score[0] / (count - errors[0]));
        System.out.println(" Palladian     : " + score[1] / (count - errors[1]));
        System.out.println("----------------- errors ---------------------");
        System.out.println(" Boilerpipe    : " + errors[0]);
        System.out.println(" Palladian     : " + errors[1]);

        return sb.toString();

    }

    /**
     * Return:
     * 
     * float[0] -> Boilerplate score
     * float[1] -> Palladian score
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

        // get the manually annotated document from the data set
        Document annotatedDocument = crawler.getWebDocument(BASE_PATH + "annotated/" + uuid + ".html");

        // get the real content data, which is wrapped in <SPAN> tags with class 'x-nc-sel2'
        StringBuilder sb = new StringBuilder();
        List<Node> nodes = XPathHelper.getNodes(annotatedDocument, "//SPAN[@class='x-nc-sel2']/text()");
        for (Node node : nodes) {
            sb.append(node.getTextContent()).append(" ");
        }

        return sb.toString();

    }

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
        Map<String, String> dataset = evaluation.readIndexFile();
        String result = evaluation.evaluate(dataset);
        FileHelper.writeToFile("data/ContentExtractionEvaluation.tsv", result);

    }

}
