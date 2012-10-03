package ws.palladian.preprocessing.segmentation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.math.MathHelper;

/**
 * The SimilarityCalculator provides functions to calculate the similarity between texts, DOM-nodes
 * and whole documents.
 * 
 * @author Silvio Rabe
 * 
 */
public class SimilarityCalculator {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(SimilarityCalculator.class);

    /**
     * Calculates the similarity between two documents by counting their tag-q-grams.
     * 
     * @param page1 Map of q-grams of the given document.
     * @param page2 Map of q-grams of the document to compare.
     * @return The similarity value between 0 and 1.
     */
    public static double calculateSimilarity(CountMap<String> page1, CountMap<String> page2) {

        double result = 0;
        List<Double> variance = new ArrayList<Double>();

        for (String qGram : page1.uniqueItems()) {
            // If both maps contain same key, exermine if there is a difference in the value
            if (page2.contains(qGram)) {
                // Calculate the difference in the value
                if (page2.get(qGram) == page1.get(qGram)) {
                    variance.add(new Double(0));
                } else {
                    Integer value = page1.get(qGram);
                    Integer value2 = page2.get(qGram);

                    double d = 0;
                    if (value > value2)
                        d = (double)value2 / value;
                    if (value < value2)
                        d = (double)value / value2;
                    d = 1 - d;
                    variance.add(d);
                }
            }
            // if both maps do not contain the same key
            else {
                variance.add(new Double(1));
            }
        }

        // Evaluation
        LOGGER.info("variance: " + variance);

        double countUp = 0;
        for (Double value : variance) {
            countUp = countUp + value;
        }
        result = countUp / variance.size();

        return result;
    }

    /**
     * Calculates a similarity value for a specific node based on its difference in several
     * documents. It takes the node out of all documents and compares each node with each
     * other node. The comparison is based on the jaccard similarity over the content of
     * the nodes.
     * 
     * @param list A list of similar documents inclusive the original document.
     * @param xPath The xpath to the node to compare in all documents.
     * @return A value of similarity.
     */
    public static double calculateSimilarityForNode(List<Document> list, String xPath) {
        double result = 0.0;
        List<CountMap<String>> listOfNodeLines = new ArrayList<CountMap<String>>();

        for (Document doc : list) {

            String simNode = HtmlHelper.documentToReadableText(XPathHelper.getXhtmlNode(doc, xPath));

            CountMap<String> nodeLines = CountMap.create();
            StringTokenizer st = new StringTokenizer(simNode, "\n");

            while (st.hasMoreTokens()) {
                String line = st.nextToken();
                nodeLines.add(line);
            }
            listOfNodeLines.add(nodeLines);
        }

        List<Double> allJaccAverage = new ArrayList<Double>();
        for (int i = 0; i < listOfNodeLines.size(); i++) {
            CountMap<String> currentNodeLines = listOfNodeLines.get(i);
            List<Double> jaccArray = new ArrayList<Double>();
            double jaccAverage = 0.0;

            for (int j = 0; j < listOfNodeLines.size(); j++) {
                CountMap<String> compareNodeLines = listOfNodeLines.get(j);

                if (currentNodeLines != compareNodeLines) {
                    Double jacc = MathHelper.computeJaccardSimilarity(currentNodeLines.uniqueItems(),
                            compareNodeLines.uniqueItems());
                    if (jacc.isNaN())
                        jacc = 0.0;
                    jaccArray.add(jacc);
                }
            }

            for (int j = 0; j < jaccArray.size(); j++) {
                jaccAverage = jaccAverage + jaccArray.get(j);
            }
            jaccAverage = jaccAverage / jaccArray.size();
            allJaccAverage.add(jaccAverage);
        }

        for (int j = 0; j < allJaccAverage.size(); j++) {
            result = result + allJaccAverage.get(j);
        }
        result = result / allJaccAverage.size();

        return result;
    }

    /**
     * Calculates similarity values for all conflict nodes of a document.
     * 
     * @param docu The original document.
     * @param conflictNodes A list of its conflict nodes.
     * @param similarFiles A list of similar documents.
     * @return A map of all conflict nodes combined with its similarity values.
     */
    public static Map<String, Double> calculateSimilarityForAllNodes(Document docu, List<String> conflictNodes,
            List<Document> similarFiles) {

        Map<String, Double> similarityOfNodes = new LinkedHashMap<String, Double>();

        List<Document> listOfSimilarDocumentsIncOrg = new ArrayList<Document>(similarFiles);
        listOfSimilarDocumentsIncOrg.add(docu);

        for (int i = 0; i < conflictNodes.size(); i++) {
            String path = conflictNodes.get(i);

            similarityOfNodes.put(path, calculateSimilarityForNode(listOfSimilarDocumentsIncOrg, path));
        }

        return similarityOfNodes;
    }

}
