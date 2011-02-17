package ws.palladian.preprocessing.segmentation;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import ws.palladian.extraction.PageAnalyzer;
import ws.palladian.extraction.content.PageContentExtractorException;
import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.HTMLHelper;
import ws.palladian.helper.Tokenizer;
import ws.palladian.helper.XPathHelper;
import ws.palladian.web.Crawler;
import ws.palladian.web.URLDownloader;

/**
 * The PageSegmenter segments a given URL into independent parts and rates the importance for each part.
 * 
 * @author Silvio Rabe
 * @author David Urbansky
 * 
 */
public class PageSegmenter {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(PageSegmenter.class);

    /** the document to use */
    private Document document = null;

    /** the location to store the colored result */
    private String storeLocation = "";

    /** a list of all segments */
    private ArrayList<Segment> segments = null;

    /** a map of similar files */
    private ArrayList<Document> similarFiles = null;

    // ///////////////////// important values ///////////////////////
    // all can be set in the segmenter.conf

    /** the length of q-grams for the similarity comparisons */
    private static int lengthOfQGrams = 0;

    /** the amount of q-grams for the similarity comparisons */
    private static int amountOfQGrams = 0;

    /** threshold needed to be similar */
    private static double similarityNeed = 0;

    /** the maximal depth in DOM tree */
    private static int maxDepth = 0;

    /** the number of similar documents needed */
    private static int numberOfSimilarDocuments = 0;

    // ///////////////////// setter methods ///////////////////////

    public void setDocument(Document document) {
        this.document = document;
    }

    public void setDocument(String url) {
        Crawler c = new Crawler();
        this.document = c.getWebDocument(url);
    }

    // needs to be an html file
    public void setStoreLocation(String storeLocation) {
        this.storeLocation = storeLocation;
    }

    // only needed for evaluation
    public void setSimilarFiles(ArrayList<Document> similarFiles) {
        this.similarFiles = similarFiles;
    }

    // ///////////////////// constructors ///////////////////////

    public PageSegmenter() {
        loadConfig();
    }

    public PageSegmenter(Document document) {
        this(document, "");
    }

    public PageSegmenter(Document document, String storeLocation) {
        loadConfig();
        this.document = document;
        this.storeLocation = storeLocation;
    }

    /**
     * Load the configuration and set the variables accordingly.
     */
    public final void loadConfig() {

        ConfigHolder configHolder = ConfigHolder.getInstance();

        PropertiesConfiguration config = configHolder.getConfig();

        PageSegmenter.lengthOfQGrams = config.getInt("pageSegmentation.lengthOfQGrams");
        PageSegmenter.amountOfQGrams = config.getInt("pageSegmentation.amountOfQGrams");
        PageSegmenter.similarityNeed = config.getDouble("pageSegmentation.similarityNeed");
        PageSegmenter.maxDepth = config.getInt("pageSegmentation.maxDepth");
        PageSegmenter.numberOfSimilarDocuments = config.getInt("pageSegmentation.numberOfSimilarDocuments");

    }

    /**
     * Returns all segments. startPageSegmentation has to be used first.
     * 
     * @return A list of Segments.
     */
    public ArrayList<Segment> getAllSegments() {
        return this.segments;
    }

    /**
     * Returns only the xPaths of the segments.
     * 
     * @return A list of XPaths.
     */
    public ArrayList<String> getAllXPaths() {
        ArrayList<String> XPaths = new ArrayList<String>();

        for (int i = 0; i < segments.size(); i++) {
            XPaths.add(segments.get(i).getXPath());
        }

        return XPaths;
    }

    /**
     * Returns the list of similar files
     * 
     * @return A list of similar files.
     */
    public ArrayList<Document> getSimilarFiles() {
        return similarFiles;
    }

    /**
     * Returns only segments specified by color.
     * 
     * @param color The color of segments to return. E.g. "Segment.Color.RED"
     * @return A list of Segments.
     */
    public ArrayList<Segment> getSpecificSegments(Segment.Color color) {
        ArrayList<Segment> allSegs = new ArrayList<Segment>();

        for (int i = 0; i < segments.size(); i++) {
            Segment seg = segments.get(i);
            if (seg.getColor() == color) {
                allSegs.add(seg);
            }
        }

        return allSegs;
    }

    /**
     * Returns segments in the range from beginValue to endValue
     * 
     * @param beginValue The begin value of variability
     * @param endValue The end value of variability
     * @return A list of Segments.
     */
    public ArrayList<Segment> getSpecificSegments(double beginValue, double endValue) {
        ArrayList<Segment> allSegs = new ArrayList<Segment>();

        for (int i = 0; i < segments.size(); i++) {
            Segment seg = segments.get(i);
            if (seg.getVariability() >= beginValue && seg.getVariability() <= endValue) {
                allSegs.add(seg);
            }
        }

        return allSegs;
    }

    /**
     * Creates a fingerprint for a given URL. The fingerprint is a map of tag-q-grams
     * combined with their quantity.
     * 
     * @param url The URL of the document.
     * @param number The limit of q-grams.
     * @param length The length of the q-grams.
     * @return A map of q-grams and their quantity.
     */
    Map<String, Integer> createFingerprintForURL(Document doc, int number, int length) throws MalformedURLException,
            IOException {

        String dText = Crawler.documentToString(doc);

        String tagList = "";
        Iterator<String> it = HTMLHelper.listTags(dText).iterator();
        while (it.hasNext()) {
            tagList = tagList + " " + it.next();
        }

        List<String> listOfTags = new ArrayList<String>(Tokenizer.calculateWordNGramsAsList(tagList, length));

        Map<String, Integer> mapOfTags = PageSegmenterHelper.convertListToMap(listOfTags);

        mapOfTags = PageSegmenterHelper.sortMapByIntegerValues(mapOfTags);

        Map<String, Integer> testMap = PageSegmenterHelper.limitMap(mapOfTags, number);

        return testMap;
    }

    /**
     * Find similar files for an given URL.
     * 
     * @param document The document to search similar files for.
     * @param qgramNumber The number of q-grams to use.
     * @param qgramLength The length of q-grams to use.
     * @param similarityNeed Defines how much similarity is needed to be similar. Value between 0 and 1, e.g. 0.88)
     * @param limit The maximum of similar files to find.
     * 
     * @return A list of documents similar to the given URL.
     */
    private ArrayList<Document> findSimilarFiles(Document document, int qgramNumber, int qgramLength,
            double similarityNeed, int limit) throws MalformedURLException, IOException {
        Map<Document, Double> result = new LinkedHashMap<Document, Double>();

        Crawler c = new Crawler();
        Document d = document;

        // Start of collect URLs (step 1) ////////////////////////////////////////////////////////

        URLDownloader urlDownloader = new URLDownloader();

        Set<String> links = new HashSet<String>();
        links.addAll(c.getLinks(d, true, false, ""));
        System.out.println("Anzahl Links: " + links.size());

        Iterator<String> iter2 = links.iterator();
        int zaehler = 0;
        while (iter2.hasNext()) {
            String newURL = iter2.next();
            int mod = links.size() / 10;
            if (zaehler % mod == 0) {
                urlDownloader.add(newURL);
                System.out.println("added1: " + newURL);
            }
            zaehler++;
        }

        HashSet<String> te3 = new HashSet<String>();
        String newURL2 = document.getDocumentURI();
        Boolean moreSlashs2 = true;
        while (moreSlashs2) {
            urlDownloader.add(newURL2);
            System.out.println("added2: " + newURL2);
            int lastSlash = newURL2.lastIndexOf("/");
            if (!newURL2.substring(lastSlash - 1, lastSlash).equals("/")) {
                newURL2 = newURL2.substring(0, lastSlash);
            } else {
                moreSlashs2 = false;
            }
        }

        Set<Document> documents = urlDownloader.start();

        Iterator<Document> iter = documents.iterator();
        while (iter.hasNext()) {
            Document doc = iter.next();
            te3.addAll(c.getLinks(doc, true, false, ""));
        }

        // //delete all duplicates of the URL like ...www.URL.de?something... = duplicate content
        // //problem if it is www.URL.de?page=2 = not duplicate content
        // HashSet<String> te4 = new HashSet<String>();
        // iter = te3.iterator();
        // while (iter.hasNext()) {
        // String doc = (String) iter.next();
        // if (doc.contains(URL)) te4.add(doc);
        // }
        // te3.removeAll(te4);

        te3.remove(document.getDocumentURI());

        // End of collect URLs (step 1) ////////////////////////////////////////////////////////

        // Start of find similar URSs (step 2) ////////////////////////////////////////////////////////

        // Vorfiltern anhand des URL-Teilstrings
        String label = PageSegmenterHelper.getLabelOfURL(document.getDocumentURI());
        System.out.println("label: " + label);

        List<String> te2 = new ArrayList<String>(te3);
        int size = te2.size();
        int counter = 0;
        for (int i = 0; i < size; i++) {
            String currentURL = te2.get(i);
            String currentLabel = PageSegmenterHelper.getLabelOfURL(currentURL);
            if (!label.equals(currentLabel) && label.matches("^[0-9]+$") && !currentLabel.matches("^[0-9]+$")
                    || !label.equals(currentLabel) && !label.matches("^[0-9]+$")) {
                te2.remove(i);
                i--;
                te2.add(size - 1, currentURL);
            }
            counter++;
            if (counter == size) {
                break;
            }
        }

        Map<String, Integer> page1 = createFingerprintForURL(d, qgramNumber, qgramLength);

        URLDownloader urlDownloader2 = new URLDownloader();

        Iterator<String> it = te2.iterator();
        while (it.hasNext()) {

            int count = 0;
            while (it.hasNext() && count < 10) {
                urlDownloader2.add(it.next());
                count++;
            }
            Set<Document> currentDocuments = urlDownloader2.start();

            Iterator<Document> it2 = currentDocuments.iterator();
            while (it2.hasNext()) {
                Document currentDocument = it2.next();

                if (HTMLHelper.documentToReadableText(d).equals(HTMLHelper.documentToReadableText(currentDocument))) {
                    System.out.println("#####################################################");
                    continue;
                }

                Map<String, Integer> page2 = createFingerprintForURL(currentDocument, qgramNumber, qgramLength);

                Double vari = SimilarityCalculator.calculateSimilarity(page1, page2);
                Double jacc = SimilarityCalculator.calculateJaccard(page1, page2);

                String variString = ((Double) ((1 - vari) * 100)).toString();
                variString = variString.substring(0, Math.min(5, variString.length()));

                double erg = (1 - vari + jacc) / 2;

                if (erg >= similarityNeed && erg < 1.0) {
                    result.put(currentDocument, erg);
                    System.out.println("Seiten verwenden wahrscheinlich dasselbe Template. (" + variString
                            + "%, Jaccard=" + jacc + ")----------" + result.size());

                } else {
                    System.out
                            .println("Unterschied zu groß. Seiten verwenden wahrscheinlich nicht dasselbe Template. ("
                                    + variString + "%, Jaccard=" + jacc + ")");
                }

                System.out
                        .println("----------------------------------------------------------------------------------------");

                if (result.size() >= limit) {
                    break;
                }
            }
            if (result.size() >= limit) {
                System.out.println("---Erg.: " + result);
                break;
            }
        }

        result = PageSegmenterHelper.sortMapByDoubleValues(result);

        // End of find similar URSs (step 2) ////////////////////////////////////////////////////////

        ArrayList<Document> simFiles = new ArrayList<Document>();
        simFiles = new ArrayList<Document>(result.keySet());

        return simFiles;
    }

    /**
     * Compares two documents based on their dom trees. It returns a list of conflict-nodes
     * and a list of non-conflict-nodes. A conflict is described as a node(and its subtree)
     * that is not equal in both documents. A maximum tree depth to check can be defined.
     * 
     * @param document1 The document that needs to be segmented.
     * @param document2 A document with high similarity to document1.
     * @param conflictNodes A list of all xpaths to conflict-nodes. Recursively built.
     * @param nonConflictNodes A list of all xpaths to non-conflict-nodes. Recursively built.
     * @param level The level(depth)of the tree to check.
     * @param xPath The xPath generated by recursion.
     * @return A list of two ArrayLists. Conflict-nodes and non-conflict-nodes.
     */
    private ArrayList<ArrayList<String>>[] compareDocuments(Document document1, Document document2,
            ArrayList<String> conflictNodes, ArrayList<String> nonConflictNodes, int level, String xPath)
            throws ParserConfigurationException {
        NodeList helpList1 = document1.getFirstChild().getChildNodes();
        NodeList helpList2 = document2.getFirstChild().getChildNodes();

        PageAnalyzer pa = new PageAnalyzer();

        for (int i = 0; i < helpList1.getLength(); i++) {
            Node n1 = helpList1.item(i);
            if (n1.getTextContent().length() == 0) {
                continue;
            }

            Node n2 = document1.createElement("newnode");// n1.cloneNode(true);
            n2.setNodeValue("###");
            n2.setTextContent("#####");

            if (helpList2.getLength() > i) {
                n2 = helpList2.item(i);
            }

            String constructXPath = pa.constructXPath(n1);

            // delete nodename of itself from the beginning of the path
            if (constructXPath.contains("/")) {
                constructXPath = constructXPath.substring(constructXPath.indexOf("/") + 1, constructXPath.length());
            }
            if (!constructXPath.contains("/")) {
                constructXPath = "";
            }
            if (constructXPath.contains("/")) {
                constructXPath = constructXPath.substring(constructXPath.indexOf("/"), constructXPath.length());
            }
            // doesn't work for #comment #ect
            if (constructXPath.contains("#")) {
                constructXPath = "";
            }

            String newXPath = xPath + constructXPath;

            if (n1.getTextContent().equals(n2.getTextContent())) {
                if (!nonConflictNodes.contains(newXPath) && !conflictNodes.contains(newXPath)) {
                    nonConflictNodes.add(newXPath);
                }
            } else {
                if (!conflictNodes.contains(newXPath)) {
                    conflictNodes.add(newXPath);
                    nonConflictNodes.remove(newXPath);
                }

                if (n1.hasChildNodes() && n2.hasChildNodes()) {
                    // build new documents from nodes
                    Document doc1 = PageSegmenterHelper.transformNodeToDocument(n1);
                    Document doc2 = PageSegmenterHelper.transformNodeToDocument(n2);

                    // System.out.println("--------------"+newXPath);

                    // checkChilden
                    if (level >= 0) {
                        compareDocuments(doc1, doc2, conflictNodes, nonConflictNodes, level - 1, newXPath);
                    }
                }

            }
        }

        return new ArrayList[] { conflictNodes, nonConflictNodes };
    }

    public void colorSegments() {
        colorSegments(segments, true);
    }

    public void colorSegments(Segment.Color color) {
        ArrayList<Segment> coloredSegments = getSpecificSegments(color);
        colorSegments(coloredSegments, true);
    }

    /**
     * Colors the segments of the document based on a comparison with similar documents.
     * Every conflict node will be evaluated in all documents to get a similarity value. Based
     * on this value a colored border is placed.
     * 
     * @param chosenSegmentsInput A list of segments to color. Either a list of xPaths as string
     *            or a list of Segments.
     * @param kindOfColoring Defines the kind of the coloring of segments. true for borders,
     *            false for background
     */
    public void colorSegments(ArrayList<?> chosenSegmentsInput, Boolean kindOfColoring) {

        ArrayList<Segment> chosenSegments = new ArrayList<Segment>();

        // set colorscale
        String[] colorScale = { "#ff0000", "#ff9600", "#ffc800", "#ffff00", "#e6ff00", "#c8ff00", "green"// "#00ff00"
        };

        // insert style attributes in head of the html document
        Element e3 = document.createElement("style");
        e3.setAttribute("type", "text/css");

        String colorString = "\n.myPageSegmenterBorder_dummy_NOTINUSE { border: 2px solid blue; }\n";
        for (int i = 0; i < colorScale.length; i++) {
            if (kindOfColoring) {
                colorString = colorString + ".myPageSegmenterBorder" + i + " { border: 2px solid " + colorScale[i]
                        + "; }\n";
            } else {
                colorString = colorString + ".myPageSegmenterBorder" + i + " { background-color: " + colorScale[i]
                        + "; }\n";
            }
        }

        Text text = document.createTextNode(colorString);
        e3.appendChild(text);

        if (document.getElementsByTagName("style").getLength() != 0) {
            Node styleNode = document.getElementsByTagName("style").item(0);
            styleNode.appendChild(text);
        } else {
            Node styleNode = document.getElementsByTagName("head").item(0);
            styleNode.appendChild(e3);
        }

        // if input is a list of xPaths, turn it into a list of Segments
        System.out.println(chosenSegmentsInput.get(0).getClass().getSimpleName());
        System.out.println(chosenSegmentsInput.get(0));
        if (chosenSegmentsInput.get(0).getClass().getSimpleName().equals("String")) {
            System.out.println("... War ein String");

            ArrayList<Segment> chosenSegments2 = new ArrayList<Segment>();
            for (int i = 0; i < segments.size(); i++) {
                if (chosenSegmentsInput.contains(segments.get(i).getXPath())) {
                    chosenSegments2.add(segments.get(i));
                }
            }
            chosenSegments = chosenSegments2;
        }

        if (chosenSegmentsInput.get(0).getClass().getSimpleName().equals("Segment")) {
            chosenSegments = (ArrayList<Segment>) chosenSegmentsInput;
        }

        // checks the similarity of ALL nodes
        for (int i = 0; i < chosenSegments.size(); i++) {
            Segment testSeg = chosenSegments.get(i);
            System.out.println(testSeg.getVariability() + " " + testSeg.getColor() + " " + testSeg.getXPath());

            Element e2 = (Element) XPathHelper.getXhtmlNode(document, testSeg.getXPath());

            String border = "";
            double value = testSeg.getVariability();
            Segment.Color color = testSeg.getColor();

            if (color == Segment.Color.RED) {
                border = "myPageSegmenterBorder0";
            }
            if (color == Segment.Color.LIGHTRED) {
                border = "myPageSegmenterBorder1";
            }
            if (color == Segment.Color.REDYELLOW) {
                border = "myPageSegmenterBorder2";
            }
            if (color == Segment.Color.YELLOW) {
                border = "myPageSegmenterBorder3";
            }
            if (color == Segment.Color.GREENYELLOW) {
                border = "myPageSegmenterBorder4";
            }
            if (color == Segment.Color.LIGHTGREEN) {
                border = "myPageSegmenterBorder5";
            }
            if (color == Segment.Color.GREEN) {
                border = "myPageSegmenterBorder6";
            }

            String old = e2.getAttribute("class");

            e2.setAttribute("class", border + " " + old);
        }

        if (storeLocation != "") {
            // writes the segmented result-document on local disc
            try {
                OutputStream os = new FileOutputStream(storeLocation);
                OutputFormat format = new OutputFormat(document);
                XMLSerializer serializer = new XMLSerializer(os, format);
                serializer.serialize(document);
            } catch (IOException e) {
                LOGGER.error("could not write to local file, " + e.getMessage());
            }
        }

        // ////////////////////////////////////////////////////////////////////////
        // fix some problems with closing tags and saves it to storeLocation+"_test.html"
        Crawler c2 = new Crawler();
        String webpage = c2.download(storeLocation);
        String newStoreLocation = storeLocation.substring(0, storeLocation.length() - 5) + "_test.html";

        String[] tagsToFix = { "SCRIPT", "IFRAME", "TEXTAREA" };
        for (String tag : tagsToFix) {
            int start = 0;
            while (start < webpage.length()) {
                int index1 = 0;
                int index2 = 0;
                int index3 = 0;

                index1 = webpage.indexOf("<" + tag + " ", start);
                index2 = webpage.indexOf("/>", index1);
                index3 = webpage.indexOf("</" + tag + ">", index1);

                if (index2 == -1) {
                    index2 = webpage.length();
                }
                if (index3 == -1) {
                    index3 = webpage.length();
                }

                if (index2 < index3 && index1 != -1) {
                    webpage = webpage.substring(0, index2) + "></" + tag + "><!--fixedForPageSegmenter-->"
                            + webpage.substring(index2 + 2, webpage.length());
                }

                if (index2 <= index3) {
                    start = index2;
                } else {
                    start = index3;
                }

                if (index1 == -1) {
                    start = webpage.length();
                }

            }
        }

        FileOutputStream print;
        try {
            print = new FileOutputStream(newStoreLocation);
            for (int i = 0; i < webpage.length(); i++) {
                print.write((byte) webpage.charAt(i));
            }
            print.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Starts the page segmentation process
     */
    public void startPageSegmentation() throws ParserConfigurationException, IOException {

        // Start of step 1 and 2 of the algorithm ////////////////////////////////////////////////////////

        // "if" makes it possible to set similar files; e.g. for the evaluation
        if (similarFiles == null) {
            System.out.println("Start findSimilarFiles------------------");
            similarFiles = findSimilarFiles(document, amountOfQGrams, lengthOfQGrams, similarityNeed,
                    numberOfSimilarDocuments);
        }

        // End of step 1 and 2 of the algorithm ////////////////////////////////////////////////////////

        // Start of segment URLs by finding the conflicts (step 3)
        // ////////////////////////////////////////////////////////

        Node bodyNode1 = document.getElementsByTagName("body").item(0);
        ArrayList<String> conflictNodes = new ArrayList<String>();
        ArrayList<String> nonConflictNodes = new ArrayList<String>();

        for (int i = 0; i < similarFiles.size(); i++) {
            System.out.println(i + 1 + ".Runde-----------------------------------------");

            Document document2 = similarFiles.get(i);
            Node bodyNode2 = document2.getElementsByTagName("body").item(0);

            // build new docs from body nodes
            Document doc1 = PageSegmenterHelper.transformNodeToDocument(bodyNode1);
            Document doc2 = PageSegmenterHelper.transformNodeToDocument(bodyNode2);

            // returns a list of xpaths of all conflict and a second of all non-conflict nodes of the actual compare
            ArrayList[] allNodes = compareDocuments(doc1, doc2, new ArrayList<String>(), new ArrayList<String>(),
                    maxDepth, "/HTML/BODY");

            System.out.println(allNodes[0].size() + "-" + conflictNodes.size() + "="
                    + (allNodes[0].size() - conflictNodes.size()) + " zu " + conflictNodes.size() * 50 / 100);
            if (allNodes[0].size() - conflictNodes.size() < conflictNodes.size() * 50 / 100
                    || conflictNodes.size() == 0) {
                // adds the new conflict nodes to the list of all conflict nodes
                for (int j = 0; j < allNodes[0].size(); j++) {
                    if (!conflictNodes.contains(allNodes[0].get(j))) {
                        conflictNodes.add((String) allNodes[0].get(j));
                    }
                }
                // adds the new non-conflict nodes to the list of all non-conflict nodes
                for (int j = 0; j < allNodes[1].size(); j++) {
                    if (!nonConflictNodes.contains(allNodes[1].get(j))) {
                        nonConflictNodes.add((String) allNodes[1].get(j));
                    }
                }
                System.out.println("Size conflictNodes: " + conflictNodes.size());
                System.out.println("Size nonConflictNodes: " + nonConflictNodes.size());
            } else {
                System.out.println("Zu viele neue Konflikte. Wahrscheinlich Inkompatibel.");
                similarFiles.remove(document2);

            }
        }

        // removes all conflictNodes from the list of nonConflictNodes
        for (int i = 0; i < conflictNodes.size(); i++) {
            String n = conflictNodes.get(i);
            for (int i2 = 0; i2 < nonConflictNodes.size(); i2++) {
                String n2 = nonConflictNodes.get(i2);

                if (n.contains(n2)) {
                    nonConflictNodes.remove(n2);
                }
            }
        }

        // End of segment URLs by finding the conflicts (step 3)
        // ////////////////////////////////////////////////////////

        // Start of rating the segments (step 4) ////////////////////////////////////////////////////////

        Map<String, Double> conflictNodesIncSim = SimilarityCalculator.calculateSimilarityForAllNodes(document,
                conflictNodes, similarFiles);

        segments = generateListOfSegments(document, conflictNodesIncSim, nonConflictNodes);

        // End of rating the segments (step 4) ////////////////////////////////////////////////////////

        System.out.println("Size conflictNodes: " + conflictNodes.size());
        System.out.println("Size nonConflictNodes: " + nonConflictNodes.size());

    }

    /**
     * Generates a list of segments after the segmentation has been done.
     * 
     * @param document The original document.
     * @param conflictNodes The conflict nodes of the document.
     * @param nonConflictNodes The non-conflict nodes of the document.
     * @return A list of segments.
     */
    private ArrayList<Segment> generateListOfSegments(Document document, Map<String, Double> conflictNodes,
            ArrayList<String> nonConflictNodes) {

        ArrayList<Segment> allSegments = new ArrayList<Segment>();

        for (int i = 0; i < nonConflictNodes.size(); i++) {
            String xPath = nonConflictNodes.get(i);
            Element node = (Element) XPathHelper.getXhtmlNode(document, xPath);
            if (node == null) {
                continue;
            }
            Integer depth = PageSegmenterHelper.getNodeLevel(node);

            Segment seg = new Segment(document, xPath, node, depth, 0.0);
            allSegments.add(seg);
        }

        Iterator<?> it = conflictNodes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();

            String xPath = (String) pairs.getKey();
            Double significance = (Double) pairs.getValue();
            Element node = (Element) XPathHelper.getXhtmlNode(document, xPath);
            if (node == null) {
                continue;
            }
            Integer depth = PageSegmenterHelper.getNodeLevel(node);

            Segment seg = new Segment(document, xPath, node, depth, 1 - significance);
            allSegments.add(seg);
        }

        return allSegments;
    }

    /**
     * Tries to find a mutual xPath for the given segments and returns all the segments under that path.
     * 
     * @param allSegments A list of segments. Can be prefiltered, like only RED.
     * @param level The number of rounds. 1 is enough.
     * @return A list of segments under the mutual xPath.
     */
    public ArrayList<String> makeMutual(ArrayList<Segment> allSegments, int level) {

        ArrayList<String> xPathList = new ArrayList<String>();
        PageAnalyzer pa2 = new PageAnalyzer();

        HashSet<String> s = new HashSet<String>();
        for (int i = 0; i < allSegments.size(); i++) {
            Segment actualSeg = allSegments.get(i);
            s.add(actualSeg.getXPath());
        }

        for (int l = 0; l < level; l++) {

            String mutual = pa2.makeMutualXPath(s);
            System.out.println("mutual: " + mutual);

            String xp = mutual;
            System.out.println(xp.substring(xp.lastIndexOf("/") + 1, xp.length()));
            if (xp.substring(xp.lastIndexOf("/") + 1, xp.length()).equals("TR")) {
                xp = xp + "/TD";
            }
            ArrayList<Node> list = (ArrayList<Node>) XPathHelper.getXhtmlNodes(document, xp);
            System.out.println("--------------\n" + xp + "\nS.size: " + s.size() + "\n---------------");
            for (int i = 0; i < list.size(); i++) {
                Node n = list.get(i);
                String constructXPath = pa2.constructXPath(n);
                System.out.println(constructXPath);
                xPathList.add(constructXPath);
                s.remove(constructXPath);
            }
            System.out.println("S.size neu: " + s.size());
            System.out.println(s);
        }

        return xPathList;
    }

    /**
     * Finds the main segments.
     * 
     * @param segments A list of segments to find the main segments for. Can be prefiltered, e.g.
     *            find only the RED and GREEN main segments.
     * @return A list of main segments.
     */
    public ArrayList<Segment> findMainSegments(ArrayList<Segment> segments) {
        System.out.println("biggest-begin: " + segments.size());

        for (int i1 = 0; i1 < segments.size(); i1++) {
            Segment seg1 = segments.get(i1);

            boolean cancel = false;
            Node node3 = seg1.getNode();
            while (node3.getParentNode() != null && !cancel) {
                node3 = node3.getParentNode();

                for (int i2 = 0; i2 < segments.size(); i2++) {
                    Segment seg2 = segments.get(i2);

                    if (seg2.getNode().isSameNode(node3) && !seg2.equals(seg1)
                            && seg2.getColor().equals(seg1.getColor())) {
                        segments.remove(seg1);
                        i1--;
                        cancel = true;
                        break;
                    }
                }
            }
        }

        for (int i1 = 0; i1 < segments.size(); i1++) {
            Segment seg1 = segments.get(i1);
            if (seg1.getNode().getTextContent().length() < 50) {
                segments.remove(seg1);
                i1--;
            }
        }

        for (int i1 = 0; i1 < segments.size(); i1++) {
            System.out.println(segments.get(i1).getVariability() + " " + segments.get(i1).getXPath());
        }

        System.out.println("biggest-end: " + segments.size());
        return segments;
    }

    /**
     * Main function to test PageSegmenter
     */
    public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException,
            TransformerFactoryConfigurationError, TransformerException, PageContentExtractorException {

        // Crawler c = new Crawler();
        // String URL="http://blogalm.de/";
        // Document d = c.getWebDocument(URL);

        System.out.println("test: " + lengthOfQGrams);
        PageSegmenter seg = new PageSegmenter();
        System.out.println("test: " + lengthOfQGrams + " " + amountOfQGrams + " " + similarityNeed + " " + maxDepth
                + " " + numberOfSimilarDocuments);

        // seg.setDocument("http://www.informatikforum.de/forumdisplay.php?f=98");
        // seg.setDocument("http://www.informatikforum.de/showthread.php?t=1381");
        // seg.setDocument("http://www.informatikforum.de/showthread.php?t=132508");
        // seg.setDocument("http://sebstein.hpfsc.de/tags/berlin/");
        // seg.setDocument("http://forum.spiegel.de/showthread.php?t=24486");
        // seg.setDocument("http://profootballtalk.nbcsports.com/2010/11/10/roddy-white-questionable-for-thursday-night/");
        // seg.setDocument("http://www.berryreview.com/2010/10/14/tips-tricks-use-the-right-convenience-key-to-focus-camera/");
        // seg.setDocument("http://www.dirks-computerseite.de/category/internet/");
        // seg.setDocument("http://gizmodo.com/5592956/is-3d-already-dying");
        // seg.setDocument("http://www.stern.de/digital/computer/sozialer-browser-rockmelt-wo-facebook-immer-mitsurft-1621849.html");
        // seg.setDocument("http://www.it-blog.net/kategorien/5-Windows");
        // seg.setDocument("http://www.basicthinking.de/blog/2006/10/02/ist-die-zeit-der-mischblogs-vorbei/");
        // ---gut---
        // seg.setDocument("http://www.kabelstoerung.de/pixelbildung-beim-digitalen-fernsehen");
        // seg.setDocument("http://www.smavel.com/forum/de/botsuana/761-oko-tourismus.html");
        // seg.setDocument("http://www.jusline.at/index.php?cpid=ba688068a8c8a95352ed951ddb88783e&lawid=62&paid=75&mvpa=92");
        seg.setDocument("http://forum.handycool.de/viewforum.php?id=20");

        // seg.setStoreLocation("C:\\Users\\Silvio\\Documents\\doc2.html");

        seg.startPageSegmentation();
        // ArrayList<Segment> allSegments = seg.getAllSegments();
        // allSegments = seg.findMainSegments(allSegments);

        // ArrayList<Segment> chosenSegments = seg.getSpecificSegments(Segment.Color.RED);
        // chosenSegments.addAll(seg.getSpecificSegments(Segment.Color.GREEN));
        // chosenSegments = seg.getSpecificSegments(0.96, 1.00);

        // chosenSegments = seg.findMainSegments(chosenSegments);

        // seg.colorSegments(chosenSegments, false);
        // seg.colorSegments(Segment.Color.RED);
        // seg.colorSegments(allSegments, true);

        // ArrayList<String> mutualXPaths= seg.makeMutual(chosenSegments, 1);
        // seg.colorSegments(mutualXPaths,true);

    }

}
