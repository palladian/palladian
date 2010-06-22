package tud.iir.web.datasetcrawler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import tud.iir.helper.FileHelper;
import tud.iir.helper.XPathHelper;
import tud.iir.web.Crawler;

/**
 * The LinkedDataStatisticsCrawler creates a <a href="http://graphml.graphdrawing.org/">GraphML</a> representation of the linked data on the web.
 * 
 * @author David Urbansky
 * 
 */
public class LinkedDataStatisticsCrawler {

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(LinkedDataStatisticsCrawler.class);
    private Crawler crawler;

    public LinkedDataStatisticsCrawler() {
        crawler = new Crawler();
    }

    /**
     * Create the graph XML.
     */
    public void createGraphML() {
        // scrape data sets
        HashSet<DataSet> dataSets = getDataSets();

        // find links between data sets
        findLinks(dataSets);

        HashSet<String> dataSetsWithIncomingLinks = new HashSet<String>();
        for (DataSet dataSet : dataSets) {
            for (Entry<DataSet, Double> entry : dataSet.getLinks().entrySet()) {
                dataSetsWithIncomingLinks.add(entry.getKey().getName());
            }
        }

        // create the graph ml document
        StringBuilder graphML = new StringBuilder();
        graphML.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        graphML.append("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\">\n");
        graphML.append("  <graph edgedefault=\"undirected\">\n");
        graphML.append("\n    <!-- data schema -->\n");
        graphML.append("    <key id=\"name\" for=\"node\" attr.name=\"name\" attr.type=\"string\"/>\n");
        graphML.append("    <key id=\"triples\" for=\"node\" attr.name=\"triples\" attr.type=\"double\"/>\n");
        graphML.append("    <key id=\"linkCount\" for=\"edge\" attr.name=\"linkCount\" attr.type=\"double\"/>\n");

        graphML.append("\n    <!-- nodes -->\n");

        for (DataSet dataSet : dataSets) {
            // skip data sets without links
            if (dataSet.getLinks().size() == 0 && !dataSetsWithIncomingLinks.contains(dataSet.getName())) {
                continue;
            }

            graphML.append("   <node id=\"").append(dataSet.hashCode()).append("\">\n");
            graphML.append("      <data key=\"name\">").append(dataSet.getName()).append("</data>\n");
            graphML.append("      <data key=\"triples\">").append(dataSet.getNumberOfTriples()).append("</data>\n");
            graphML.append("   </node>\n");
        }

        graphML.append("\n    <!-- edges -->\n");
        for (DataSet dataSet : dataSets) {
            for (Entry<DataSet, Double> entry : dataSet.getLinks().entrySet()) {
                graphML.append("   <edge directed=\"true\" source=\"").append(dataSet.hashCode()).append("\" target=\"").append(entry.getKey().hashCode())
                        .append("\">\n");
                graphML.append("     <data key=\"linkCount\">").append(entry.getValue()).append("</data>\n");
                graphML.append("   </edge>\n");
            }
        }

        graphML.append("  </graph>\n");
        graphML.append("</graphml>");

        FileHelper.writeToFile("data/temp/ldweb.xml", graphML);

        LOGGER.info("graph ml created and saved");
    }

    /**
     * Find data sets with names and number of triples.
     * 
     * @return A set of data sets.
     */
    private HashSet<DataSet> getDataSets() {

        HashSet<DataSet> dataSets = new HashSet<DataSet>();

        Document document = crawler.getWebDocument("http://esw.w3.org/TaskForces/CommunityProjects/LinkingOpenData/DataSets/Statistics");

        List<Node> nameNodes = XPathHelper.getNodes(document, "//div/div/table/tr/td[1]/a".toUpperCase());
        List<Node> valueNodes = XPathHelper.getNodes(document, "//div/div/table/tr/td[2]/code".toUpperCase());

        double totalSize = 0;
        for (int i = 0; i < nameNodes.size(); i++) {

            try {
                String sizeString = valueNodes.get(i).getTextContent();

                sizeString = sizeString.replaceAll(">", "").replaceAll(",", "").replaceAll("\\+", "").trim();
                double size = Double.valueOf(sizeString);
                totalSize += size;
                DataSet dataSet = new DataSet(nameNodes.get(i).getTextContent(), size);
                dataSets.add(dataSet);

                // System.out.println(valueNodes.get(i).getTextContent());

            } catch (NumberFormatException e) {
                LOGGER.error(e.getMessage());
            }
        }

        LOGGER.info(dataSets.size() + " data sets found, " + totalSize + " triples in total");

        return dataSets;
    }

    /**
     * Find the number of links between the given data sets.
     * 
     * @param dataSets A set of data sets.
     */
    private void findLinks(HashSet<DataSet> dataSets) {

        // index data sets to access them by name
        HashMap<String, DataSet> dataSetIndex = new HashMap<String, DataSet>();
        for (DataSet ds : dataSets) {
            dataSetIndex.put(ds.getName().replaceAll("\\s", "").toLowerCase(), ds);
        }

        // get link information
        Document document = crawler.getWebDocument("http://esw.w3.org/TaskForces/CommunityProjects/LinkingOpenData/DataSets/LinkStatistics");

        List<Node> sourceNodes = XPathHelper.getNodes(document, "//div/div/table/tr/td[1]/a".toUpperCase());
        List<Node> targetNodes = XPathHelper.getNodes(document, "//div/div/table/tr/td[3]/a".toUpperCase());
        List<Node> valueNodes = XPathHelper.getNodes(document, "//div/div/table/tr/td[4]/code".toUpperCase());

        double totalSize = 0;
        for (int i = 0; i < sourceNodes.size(); i++) {

            try {
                String sizeString = valueNodes.get(i).getTextContent();

                sizeString = sizeString.replaceAll(">", "").replaceAll(",", "").replaceAll("\\+", "").trim();
                double linkCount = Double.valueOf(sizeString);
                totalSize += linkCount;

                DataSet sourceDataSet = dataSetIndex.get(sourceNodes.get(i).getTextContent().replaceAll("\\s", "").toLowerCase());
                DataSet targetDataSet = dataSetIndex.get(targetNodes.get(i).getTextContent().replaceAll("\\s", "").toLowerCase());

                if (sourceDataSet == null || targetDataSet == null) {
                    LOGGER.warn("data set was not found, continuing");
                    continue;
                }

                sourceDataSet.addLink(targetDataSet, linkCount);

                LOGGER.info(sourceDataSet.getName() + " to " + targetDataSet.getName() + " with " + linkCount + " links");

            } catch (NumberFormatException e) {
                LOGGER.error(e.getMessage());
            }
        }

        LOGGER.info(totalSize + " links in total");

    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        LinkedDataStatisticsCrawler c = new LinkedDataStatisticsCrawler();
        c.createGraphML();
    }

}

class DataSet {

    private String name = "";
    private double numberOfTriples = 0;

    // links to other data sets with the number of links
    private HashMap<DataSet, Double> links = new HashMap<DataSet, Double>();

    public DataSet(String name, double numberOfTriples) {
        setName(name);
        setNumberOfTriples(numberOfTriples);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.replaceAll("é", "e");
    }

    public double getNumberOfTriples() {
        return numberOfTriples;
    }

    public void setNumberOfTriples(double numberOfTriples) {
        this.numberOfTriples = numberOfTriples;
    }

    public HashMap<DataSet, Double> getLinks() {
        return links;
    }

    public void setLinks(HashMap<DataSet, Double> links) {
        this.links = links;
    }

    public void addLink(DataSet dataSet, double linkCount) {
        this.links.put(dataSet, linkCount);
    }

    @Override
    public String toString() {
        return "DataSet [name=" + name + ", numberOfTriples=" + numberOfTriples + "]";
    }
}