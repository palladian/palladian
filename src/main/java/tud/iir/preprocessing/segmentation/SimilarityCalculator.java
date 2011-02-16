package tud.iir.preprocessing.segmentation;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.w3c.dom.Document;

import tud.iir.helper.HTMLHelper;
import tud.iir.helper.XPathHelper;

/**
 * The SimilarityCalculator provides functions to calculate the similarity between texts, DOM-nodes
 * and whole documents.
 * 
 * @author Silvio Rabe
 * 
 */
public class SimilarityCalculator {

    /**
     * Calculates the similarity between two documents by counting their tag-q-grams.
     * 
     * @param page1 Map of q-grams of the given document.
     * @param page2 Map of q-grams of the document to compare.
     * @return The similarity value between 0 and 1.
     */
    public static double calculateSimilarity(Map<String, Integer> page1, Map<String, Integer> page2) {

        double result = 0;
        List<Double> variance = new ArrayList<Double>();
        String key = "";

        Iterator<String> it = page1.keySet().iterator();
        while (it.hasNext()) {
            key = (String) it.next();

            // If both maps contain same key, exermine if there is a difference in the value
            if (page2.keySet().contains(key)) {
                // Calculate the difference in the value
                if (page2.get(key) == page1.get(key)) {
                    variance.add(new Double(0));
                } else {
                    Integer value = ((Integer) page1.get(key));
                    Integer value2 = ((Integer) page2.get(key));

                    double d = 0;
                    if (value > value2)
                        d = (double) value2 / value;
                    if (value < value2)
                        d = (double) value / value2;
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
        System.out.println(variance);

        double countUp = 0;
        Iterator<Double> it3 = variance.iterator();
        while (it3.hasNext()) {
            countUp = countUp + ((Double) it3.next());
        }
        result = countUp / variance.size();

        return result;
    }

    /**
     * Calculates the Jaccard value for two documents by comparing their tags.
     * 
     * @param page1 The given document.
     * @param page2 The document to compare.
     * @return The Jaccard value between 0 and 1.
     */
    public static double calculateJaccard(Map<String, Integer> page1, Map<String, Integer> page2) {
        double result = 0;

        Map<String, Integer> helperMap = new HashMap<String, Integer>(page1);

        helperMap.keySet().retainAll(page2.keySet());

        int z1 = helperMap.size();

        Set<String> s1 = new HashSet<String>(page1.keySet());
        Set<String> s2 = new HashSet<String>(page2.keySet());

        s1.addAll(s2);

        int z2 = s1.size();

        result = (double) z1 / z2;

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
    public static double calculateSimilarityForNode(ArrayList<Document> list, String xPath) {
        double result = 0.0;
        ArrayList<Map<String, Integer>> listOfNodeLines = new ArrayList<Map<String, Integer>>();

        Iterator<Document> it = list.iterator();
        while (it.hasNext()) {
            Document doc = (Document) it.next();

            String simNode = HTMLHelper.htmlToReadableText(XPathHelper.getNode(doc, xPath));

            Map<String, Integer> nodeLines = new LinkedHashMap<String, Integer>();
            StringTokenizer st = new StringTokenizer(simNode, "\n");

            while (st.hasMoreTokens()) {
                String line = st.nextToken();
                nodeLines.put(line, 0);
            }
            listOfNodeLines.add(nodeLines);
        }

        ArrayList<Double> allJaccAverage = new ArrayList<Double>();
        for (int i = 0; i < listOfNodeLines.size(); i++) {
            Map<String, Integer> currentNodeLines = (Map<String, Integer>) listOfNodeLines.get(i);
            ArrayList<Double> jaccArray = new ArrayList<Double>();
            double jaccAverage = 0.0;

            for (int j = 0; j < listOfNodeLines.size(); j++) {
                Map<String, Integer> compareNodeLines = (Map<String, Integer>) listOfNodeLines.get(j);

                if (currentNodeLines != compareNodeLines) {
                    Double jacc = calculateJaccard(currentNodeLines, compareNodeLines);
                    if (jacc.isNaN())
                        jacc = 0.0;
                    jaccArray.add(jacc);
                }
            }

            for (int j = 0; j < jaccArray.size(); j++) {
                jaccAverage = jaccAverage + (Double) jaccArray.get(j);
            }
            jaccAverage = jaccAverage / jaccArray.size();
            allJaccAverage.add(jaccAverage);
        }

        for (int j = 0; j < allJaccAverage.size(); j++) {
            result = result + (Double) allJaccAverage.get(j);
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
    public static Map<String, Double> calculateSimilarityForAllNodes(Document docu, ArrayList<String> conflictNodes,
            ArrayList<Document> similarFiles) throws MalformedURLException, IOException {

        Map<String, Double> similarityOfNodes = new LinkedHashMap<String, Double>();

        ArrayList<Document> listOfSimilarDocuments = new ArrayList<Document>();
        listOfSimilarDocuments = similarFiles;

        ArrayList<Document> listOfSimilarDocumentsIncOrg = new ArrayList<Document>(listOfSimilarDocuments);
        listOfSimilarDocumentsIncOrg.add(docu);

        for (int i = 0; i < conflictNodes.size(); i++) {
            String path = (String) conflictNodes.get(i);

            similarityOfNodes.put(path, calculateSimilarityForNode(listOfSimilarDocumentsIncOrg, path));
        }

        return similarityOfNodes;
    }

}
