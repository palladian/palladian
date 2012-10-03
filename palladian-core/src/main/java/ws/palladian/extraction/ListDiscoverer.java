package ws.palladian.extraction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.PageAnalyzer;
import ws.palladian.retrieval.XPathSet;

/**
 * <p>
 * The ListDiscoverer tries to find a list (with entities) on a web page. If a "good" list is found the xPath for one or
 * all entries in the list is returned. Features of a "good" list are as follows.
 * </p>
 * <ul>
 * <li>the list has at least 10 entries</li>
 * <li>it is the only long list on the web page, not just one of many (path lengths distribution)</li>
 * <li>it has uniform entries, that is, entries are in almost the same format</li>
 * <li>the list is specific for the web page, it should not be a navigation list that can be found on another page of
 * the website</li>
 * </ul>
 * <p>
 * If no good list is found, an empty string is returned.
 * </p>
 * 
 * @author David Urbansky
 */
public class ListDiscoverer {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ListDiscoverer.class);

    /** Set of URLs that were found in the pagination of the given page */
    private Set<String> paginationURLs;

    /** The XPath that points to the pagination of the given page */
    private String paginationXPath = "";

    private String url = "";
    private Document document = null;

    private DocumentRetriever crawler = null;

    public ListDiscoverer() {
        paginationURLs = new HashSet<String>();
        crawler = new DocumentRetriever();
    }

    public Set<String> findPaginationURLs(Document document) {
        if (document == null) {
            return paginationURLs;
        }
        this.document = document;
        this.url = document.getDocumentURI();
        return findPaginationURLs();
    }

    public Set<String> findPaginationURLs(String url) {
        if (document == null || !this.url.equalsIgnoreCase(url)) {
            document = crawler.getWebDocument(url);
            this.url = url;
            paginationXPath = "";
        }
        return findPaginationURLs();
    }

    /**
     * <p>
     * Find the URLs that point to pages that were used in the pagination.
     * </p>
     * TODO also find "next" and "previous" button if no other pagination is given
     * TODO find pagination in drop down boxes
     * 
     * @return A set of URLs.
     */
    public Set<String> findPaginationURLs() {

        if (paginationXPath.length() == 0) {

            // reset previously found URLs
            paginationURLs = new HashSet<String>();

            XPathSet paginationPaths = new XPathSet();
            PageAnalyzer pa = new PageAnalyzer();
            String[] removeCountElements = {"a", "tr", "td", "p", "span", "li"};
            List<Node> paginationCandidates = XPathHelper.getXhtmlNodes(document, "//a");
            if (paginationCandidates == null) {
                return paginationURLs;
            }

            for (int j = 0; j < paginationCandidates.size(); j++) {
                Node currentNode = paginationCandidates.get(j);
                String nodeText = StringHelper.trim(currentNode.getTextContent());
                nodeText = nodeText.replaceAll("\\[", "").replaceAll("\\]", "");
                // System.out.println(nodeText+" | "+currentNode.getTextContent());
                if (nodeText.length() > 0
                        && (nodeText.length() <= 3 && StringHelper.isNumber(nodeText) || nodeText.length() == 1
                                && StringHelper.isCompletelyUppercase(nodeText) || nodeText.toLowerCase().indexOf(
                                "next") > -1
                                && nodeText.length() < 8)) {
                    paginationPaths.add(PageAnalyzer.removeXPathIndices(pa.constructXPath(currentNode),
                            removeCountElements));
                }
            }

            LinkedHashMap<String, Integer> xPathMap = paginationPaths.getXPathMap();
            if (xPathMap.entrySet().size() > 0) {
                LinkedHashMap<String, Double> xPathsBySimilarity = new LinkedHashMap<String, Double>();
                // QGramsDistance stringDistanceMetric = new QGramsDistance();
                JaroWinkler stringDistanceMetric = new JaroWinkler();
                Iterator<Map.Entry<String, Integer>> xPathMapIterator = xPathMap.entrySet().iterator();
                while (xPathMapIterator.hasNext()) {
                    Map.Entry<String, Integer> entry = xPathMapIterator.next();
                    double similaritySum = 0.0;
                    int comparisons = 0;

                    List<Node> hrefNodes = XPathHelper.getXhtmlNodes(document, entry.getKey() + "/@href");

                    // remove duplicates
                    int samePageLinks = 0;
                    HashSet<String> hrefTexts = new HashSet<String>();
                    for (int i = 0; i < hrefNodes.size(); i++) {
                        String hrefText = hrefNodes.get(i).getTextContent().replaceAll("#.*", "");
                        if (hrefText.length() == 0) {
                            samePageLinks++;
                            continue;
                        }
                        hrefTexts.add(hrefText);
                    }

                    if ((double)samePageLinks / (double)hrefNodes.size() > 0.5) {
                        paginationXPath = "";
                        return paginationURLs;
                    }

                    // there must be at least two distinct links
                    if (hrefTexts.size() < 2) {
                        continue;
                    }

                    String hrefText1 = "";
                    String hrefText2 = "";
                    int i = 0;
                    Iterator<String> hrefTextIterator = hrefTexts.iterator();
                    while (hrefTextIterator.hasNext()) {
                        String currentHrefText = hrefTextIterator.next();
                        if (i % 2 == 0) {
                            hrefText1 = currentHrefText;
                        } else {
                            hrefText2 = currentHrefText;
                            float hrefSimilarity = stringDistanceMetric.getSimilarity(hrefText1, hrefText2);
                            similaritySum += hrefSimilarity;
                            comparisons++;
                        }
                        i++;
                    }

                    double averageLinkSimilarity = similaritySum / comparisons;
                    if (averageLinkSimilarity > 0.8) {
                        xPathsBySimilarity.put(entry.getKey(), averageLinkSimilarity);
                    }
                }
                xPathsBySimilarity = CollectionHelper.sortByValue(xPathsBySimilarity, CollectionHelper.DESCENDING);

                if (!xPathsBySimilarity.isEmpty()) {
                    paginationXPath = xPathsBySimilarity.entrySet().iterator().next().getKey();
                } else {
                    paginationXPath = paginationPaths.getHighestCountXPath();
                }
                int count = paginationPaths.getCountOfXPath(paginationXPath);
                // System.out.println("one element: "+pa.getTextByXpath(document, paginationXPath));
                if (count == 1/*
                               * || (count == 2 && !StringHelper.trim(pa.getTextByXpath(document,
                               * paginationXPath)).equals("1,2"))
                               */) {
                    String pathText = StringHelper.trim(pa.getTextByXPath(document, paginationXPath));
                    if (pathText.toLowerCase().indexOf("next") == -1 && !pathText.equals("1")) {
                        paginationXPath = "";
                        return paginationURLs;
                    }

                }/*
                  * else if (count > 1 && count < 20) { String pathText = StringHelper.trim(pa.getTextByXpath(document,
                  * paginationXPath)); String[] entries =
                  * pathText.split(","); int numericCount = 0; for (int i = 0; i < entries.length; i++) { if
                  * (StringHelper.isNumericExpression(entries[i]))
                  * numericCount++; } if ((double) numericCount / (double) entries.length < 0.4 &&
                  * (pathText.indexOf("A") == -1 || pathText.indexOf("B") == -1
                  * || pathText.indexOf("C") == -1)) { paginationXPath = ""; } }
                  */

            } else {
                // TODO link similarity check: http://openwetware.org/wiki/OpenWetWare:Feature_list/Lab_notebook
                // TODO link similarity check: http://www.infoplease.com/countries.html
                // TODO link similarity check: more urls (look in comments in test class)
                paginationXPath = paginationPaths.getHighestCountXPath(3);
            }

            if (paginationXPath.length() == 0) {
                return paginationURLs;
            }

            Set<Integer> pageNumbers = new TreeSet<Integer>();
            List<Node> linkNodes = XPathHelper.getXhtmlNodes(document, paginationXPath);
            for (int i = 0; i < linkNodes.size(); i++) {
                String nodeText = StringHelper.trim(linkNodes.get(i).getTextContent());
                nodeText = nodeText.replaceAll("\\[", "").replaceAll("\\]", "");
                if (StringHelper.isNumber(nodeText)) {
                    try {
                        pageNumbers.add(Integer.valueOf(nodeText));
                    } catch (NumberFormatException e) {
                        LOGGER.error(nodeText + "," + e.getMessage());
                    }
                }
            }

            // majority of numbers must form a sequence (but not all because some pages number this way: 1|2|3...511
            // next>
            if (pageNumbers.size() > 0 && pageNumbers.size() < 2) {
                paginationXPath = "";
                return paginationURLs;
            }
            int longestSequence = 0;
            int currentSequence = 0;
            int lastNumber = -1;
            Iterator<Integer> pageNumberIterator = pageNumbers.iterator();
            while (pageNumberIterator.hasNext()) {
                int pageNumber = pageNumberIterator.next();
                if (lastNumber > -1) {
                    if (pageNumber == lastNumber + 1) {
                        currentSequence++;
                        if (currentSequence > longestSequence) {
                            longestSequence = currentSequence;
                        }
                    } else {
                        currentSequence = 0;
                    }
                }
                lastNumber = pageNumber;
            }
            if (longestSequence < pageNumbers.size() / 2) {
                paginationXPath = "";
                return paginationURLs;
            }

            if (paginationXPath.length() > 0) {
                paginationXPath += "/@href";
            }
        }

        if (paginationXPath.length() == 0) {
            return paginationURLs;
        }

        paginationXPath = removeHtmlBody(paginationXPath);

        List<Node> linkNodes = XPathHelper.getXhtmlNodes(document, paginationXPath);
        for (int i = 0; i < linkNodes.size(); i++) {
            String linkURL = linkNodes.get(i).getTextContent();
            linkURL = UrlHelper.makeFullUrl(url, linkURL);
            if (linkURL.length() > 0) {
                paginationURLs.add(linkURL);
            }
        }

        filterPaginationUrls();

        return paginationURLs;
    }

    /**
     * <p>
     * Check the pagination URLs and remove the ones that might not be correct. For example, if we have a list of URLs
     * "http.../A", "http.../B", "http.../somethingelse", the last one should be filtered out.
     * </p>
     */
    private void filterPaginationUrls() {
        CountMap<Integer> countMap = CountMap.create();
        for (String url : paginationURLs) {
            countMap.add(url.length());
        }

        if (countMap.uniqueSize() == 0) {
            return;
        }

        int mostLikelyLength = (Integer)countMap.getSortedMapDescending().entrySet().iterator().next().getKey();

        Set<String> filteredUrls = new HashSet<String>();

        for (String url : paginationURLs) {
            if (MathHelper.isWithinRange(url.length(), mostLikelyLength, 1)) {
                filteredUrls.add(url);
            }
        }

        paginationURLs = filteredUrls;
    }

    public static String removeHtmlBody(String paginationXPath) {
        return paginationXPath.replace("/html/body/", "//").replace("/xhtml:html/xhtml:body/", "//");
    }

    public Set<String> getPaginationURLs() {
        return paginationURLs;
    }

    /**
     * Get a set of xPaths.
     * 
     * @param document The document the xPaths are constructed for.
     * @return A set of xPaths.
     */
    public XPathSet getXPathSet(Document document) {
        String[] listElements = {"//ul/li", "//ol/li", "//td", "//h2", "//h3", "//h4", "//h5", "//h6", "//a", "//i",
                "//div", "//strong", "//span"};
        PageAnalyzer pa = new PageAnalyzer();
        XPathSet xPathSet = new XPathSet();

        // pa.printDOM(document.getLastChild()," ");

        for (String currentXPath : listElements) {
            List<Node> results = XPathHelper.getXhtmlNodes(document, currentXPath);
            if (results == null) {
                continue;
            }
            for (int j = 0; j < results.size(); j++) {
                Node currentNode = results.get(j);
                // String xPath = PageAnalyzer.removeCounts(pa.constructXPath(currentNode));
                // 1/1 better results
                // String[] rcElements = {"DIV","TABLE","P","A"};
                String[] rcElements = {"table"}; // TODO! get more than one table?:
                // http://www.blu-ray.com/movies/movies.php?genre=action AND
                // http://www.nytimes.com/ref/movies/1000best.html
                String xPath = PageAnalyzer.removeXPathIndicesNot(pa.constructXPath(currentNode), rcElements);
                // System.out.println(pa.constructXPath(currentNode)+" / "+xPath);
                xPathSet.add(xPath);
            }
        }

        return xPathSet;
    }

    // TODO check content of sibling paths
    // TODO detect horizontal lists
    // TODO recurring structure test (compare website7.html and website35.html)
    // TODO check structure in box elements, only those with few child tags are considered
    // TODO distribution
    // TODO keyword must appear on site (best if in heading)
    // TODO website55 not enough /html/body/div/div/table/tr/td/table/tbody/tr/td
    // TODO get text content with whitespace if <br /> (website33.html)
    public String discoverEntityXPath(String url) {
        DocumentRetriever c = new DocumentRetriever();
        this.url = url;
        document = c.getWebDocument(url);
        return discoverEntityXPath(document);
    }

    public String discoverEntityXPath(Document document) {
        String entityXPath = "";
        PageAnalyzer pa = new PageAnalyzer();

        this.url = document.getDocumentURI();
        this.document = document;

        XPathSet xPathSet = getXPathSet(document);

        // remove paths from xpath set that can be found on a sibling page
        xPathSet = removeSiblingPagePaths(xPathSet, url, document);

        // if no page specific list has been found (one that does not also appear on the sibling page) return empty
        // string
        if (xPathSet.getXPathMap().size() == 0) {
            return "";
        }

        // get list path
        entityXPath = xPathSet.getHighestCountXPath(); // TODO test without that
        entityXPath = xPathSet.getLongestHighCountXPath(document);
        entityXPath = removeHtmlBody(entityXPath);

        // if xPath ends on td, the correct column has to be found
        if (pa.nodeInTable(entityXPath, 6)) {
            int column = findEntityColumn(document, entityXPath);

            // no column is uniform, return empty xPath
            if (column == -1) {
                return "";
            }
            entityXPath = setIndex(entityXPath, "td", column);

            /*
             * // if list is in block node (p or div), the list is rejected if block has many children } else if
             * (pa.nodeInBox(entityXPath, 6)) { String
             * boxXPath = pa.findLastBoxSection(entityXPath); String boxText = pa.getTextByXpath(document, boxXPath);
             * String a = setIndex(entityXPath,
             * pa.getTargetNode(entityXPath).toUpperCase(), 1); String firstNodeText = pa.getTextByXpath(document, a);
             * //if (boxText.length() > 4 *
             * firstNodeText.length()) return ""; }
             */
        }
        // if (entityXPath.indexOf("LI") == -1 && entityXPath.indexOf("TABLE") == -1) return "";

        // uniformity check
        List<Node> listNodes = XPathHelper.getXhtmlNodes(document, entityXPath);
        List<String> entityCandidateList = new ArrayList<String>();
        for (int j = 0; j < listNodes.size(); j++) {
            entityCandidateList.add(listNodes.get(j).getTextContent());
        }
        if (!entriesUniform(entityCandidateList, false)) {
            return "";
        }

        // list must have at least 10 entries
        if (entityCandidateList.size() < 10) {
            return "";
        }

        return entityXPath;
    }

    // TODO check similar page if path leads to same list (website37.html) only if same content!
    public XPathSet removeSiblingPagePaths(XPathSet xPathSet, String url, Document document) {
        XPathSet reducedXPathSet = new XPathSet();

        PageAnalyzer pa = new PageAnalyzer();
        String siblingURL = pa.getSiblingPage(document);
        if (siblingURL.length() == 0) {
            return xPathSet;
        }

        Document siblingDocument = crawler.getWebDocument(siblingURL);
        if (siblingDocument == null) {
            return xPathSet;
        }

        XPathSet siblingXPathSet = getXPathSet(siblingDocument);
        LinkedHashMap<String, Integer> siblingXPathSetMap = siblingXPathSet.getXPathMap();

        int samePathContent = 0;
        Iterator<Map.Entry<String, Integer>> xPathSetIterator = xPathSet.getXPathMap().entrySet().iterator();
        while (xPathSetIterator.hasNext()) {
            Map.Entry<String, Integer> entry = xPathSetIterator.next();

            if (!siblingXPathSetMap.containsKey(entry.getKey())) {
                reducedXPathSet.addEntry(entry);
            } else {
                // check whether content is also the same
                // String text1 = pa.getTextByXpath(document, entry.getKey());
                // String text2 = pa.getTextByXpath(siblingDocument, entry.getKey());
                //
                // System.out.println(entry.getKey()+":"+text1.length()+" "+text2.length());
                // if (!text1.equalsIgnoreCase(text2)) {
                // System.out.println("enter");
                // reducedXPathSet.addEntry(entry);
                // }
                // TODO nodes can be shifted so that they do not match on similar pages:
                // http://www.btc.bg/en/business/devices/catalogue/filter/1//?page=2
                // use another string similarity function
                // int sameCount = 0;
                // List<Node> nodeList1 = XPathHelper.getNodesNS(document, entry.getKey());
                // List<Node> nodeList2 = XPathHelper.getNodesNS(siblingDocument, entry.getKey());
                // for (int i = 0; i < Math.min(nodeList1.size(), nodeList2.size()); i++) {
                // String text1 = nodeList1.get(i).getTextContent();
                // String text2 = nodeList2.get(i).getTextContent();
                // //System.out.println(text1+" =? "+text2);
                // if (text1.equalsIgnoreCase(text2)) {
                // sameCount++;
                // }
                // }
                // if ((double)sameCount / (double)Math.min(nodeList1.size(), nodeList2.size()) < 0.5) {
                // reducedXPathSet.addEntry(entry);
                // }

                String text1 = pa.getTextByXPath(document, entry.getKey());
                text1 = text1.substring(0, Math.min(200, text1.length()));
                String text2 = pa.getTextByXPath(siblingDocument, entry.getKey());
                text2 = text2.substring(0, Math.min(200, text2.length()));
                // OverlapCoefficient oc = new OverlapCoefficient();
                QGramsDistance qg = new QGramsDistance();
                float sim = qg.getSimilarity(text1, text2);
                // System.out.println("estimated time: "+oc.getSimilarityTimingEstimated(text1, text2));
                // float sim = oc.getSimilarity(text1, text2);
                // System.out.println("similarity: "+sim+" ("+entry.getKey().toLowerCase()+")");
                if (sim < 0.7) {
                    reducedXPathSet.addEntry(entry);
                } else if (sim > 0.98) {
                    samePathContent++;
                }
            }
        }

        // if almost everything is the same, it indicates that it is the same page but with another url (e.g. different
        // sorting etc.)
        if ((double)samePathContent / (double)xPathSet.getXPathMap().entrySet().size() >= 0.9) {
            Logger.getRootLogger().info("sibling url was probably the same as source url");
            return xPathSet;
        }

        return reducedXPathSet;
    }

    public int findEntityColumn(Document document, String entityXPath) {
        int entityColumn = 0; // 0 means all columns are uniform and can be taken for extraction

        PageAnalyzer pa = new PageAnalyzer();

        // find out how many columns the table has
        int columnCount = pa.getNumberOfTableColumns(document, entityXPath);

        List<Integer> uniformColumns = new ArrayList<Integer>();

        // for each column, get all entries in an array and check whether they look similar
        // List<Node> trNodes = XPathHelper.getNodesNS(document,pa.getParentNode(entityXPath));
        // Node firstTR = trNodes.get(0);
        for (int i = 1; i <= columnCount; i++) {
            List<String> columnEntries = new ArrayList<String>();
            List<Node> columnNodes = XPathHelper.getXhtmlNodes(document, setIndex(entityXPath, "td", i));
            List<Node> pureColumnNodes = XPathHelper.getXhtmlNodes(document,
                    pa.getTableCellPath(setIndex(entityXPath, "td", i)));
            for (int j = 0; j < columnNodes.size(); j++) {
                columnEntries.add(columnNodes.get(j).getTextContent());
            }
            if (entriesUniform(columnEntries, true) && columnEntries.size() > 0 || pureColumnNodes.size() <= 1) {
                uniformColumns.add(i);
                LOGGER.info("Column " + i + "/" + columnCount + " is uniform");
            } else {
                LOGGER.info("Column " + i + "/" + columnCount + " is not uniform");
            }
        }

        // no column is uniform, table should not be used for extraction
        if (uniformColumns.size() == 0) {
            LOGGER.info("No uniform columns found");
            return -1;
        }

        // all columns are uniform, no index required
        if (uniformColumns.size() == columnCount) {
            LOGGER.info("All columns are uniform");
            return 0;
        }

        // take the first uniform column, since the sought entity possibly appear in one of the first columns
        entityColumn = uniformColumns.get(0);

        return entityColumn;
    }

    private String setIndex(String xPath, String element, int index) {
        String updatedXPath = xPath;
        int elementIndex = xPath.lastIndexOf(element);
        if (elementIndex == -1) {
            return xPath;
        }
        updatedXPath = xPath.substring(0, elementIndex);
        if (updatedXPath.matches(element + "\\[")) {
            if (index == 0) {
                updatedXPath += xPath.substring(elementIndex).replaceAll(element + "\\[(\\d)+\\]", element);
            } else {
                updatedXPath += xPath.substring(elementIndex).replaceAll(element + "\\[(\\d)+\\]",
                        element + "[" + index + "]");
            }
        } else if (index > 0) {
            updatedXPath += xPath.substring(elementIndex).replaceAll(element, element + "[" + index + "]");
        } else {
            return xPath;
        }

        return updatedXPath;
    }

    /**
     * Check whether a list of entries is likely to be a list of entities. The list is rejected if:
     * <ul>
     * <li>more than 10% of them are just numbers</li>
     * <li>more than 50% are only capitalized, e.g. CATEGORIES</li> TODO does it make a difference?
     * <li>the average string length is more than 12 words</li>
     * <li>there are not more than 10% entries that have duplicates</li>
     * <li>there are not more than 10% entries missing</li>
     * </ul>
     * 
     * @param entries
     * @return True if the list entries are uniform, else false.
     */
    public static boolean entriesUniform(List<String> entries, boolean tableDuplicateCheck) {

        int totalEntries = entries.size();
        int numericEntries = 0;
        int completelyCapitalized = 0;
        int totalWordLength = 0;
        int missingEntries = 0;

        Set<String> duplicateCountSet = new HashSet<String>();
        Set<String> duplicateWordCountSet = new HashSet<String>();
        int duplicateCount = 0;
        int duplicateWordCount = 0;

        for (String entry : entries) {

            entry = StringHelper.trim(entry);

            totalWordLength += entry.split(" ").length;

            if (entry.length() > 200) {
                continue;
            }

            try {
                if (StringHelper.isNumericExpression(entry) || StringHelper.isTimeExpression(entry)) {
                    numericEntries++;
                }
            } catch (NumberFormatException e) {
                LOGGER.error(entry, e);
            } catch (OutOfMemoryError e) {
                LOGGER.error(entry, e);
            }

            if (StringHelper.isCompletelyUppercase(entry)) {
                completelyCapitalized++;
            }

            if (entry.length() == 0) {
                missingEntries++;
            } else if (!duplicateCountSet.add(entry)) {
                duplicateCount++;
                if (duplicateWordCountSet.add(entry)) {
                    duplicateWordCount++;
                }
            }
        }

        if ((double)numericEntries / (double)totalEntries > 0.15) {
            LOGGER.info("entries not uniform because too many numeric entries");
            return false;
        }
        if ((double)completelyCapitalized / (double)totalEntries > 0.5) {
            LOGGER.info("entries not uniform because too many entirely capitalized entries");
            return false;
        }
        if ((double)totalWordLength / (double)totalEntries > 12) {
            LOGGER.info("entries not uniform because average word length too long");
            return false;
        }
        if (tableDuplicateCheck && (double)duplicateCount / (double)totalEntries > 0.1) {
            LOGGER.info("entries not uniform because too many duplicates");
            return false;
        } else if (!tableDuplicateCheck && (double)duplicateWordCount / (double)duplicateCountSet.size() > 0.6) {
            LOGGER.info("entries not uniform because too many duplicate words");
            return false;
        }
        // if ((double) missingEntries / (double) totalEntries > 0.1) {
        // LOGGER.info("entries not uniform because too many entries are missing");
        // return false;
        // }

        return true;
    }

    public String getPaginationXPath() {
        return paginationXPath;
    }

    public void setPaginationXPath(String paginationXPath) {
        this.paginationXPath = paginationXPath;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // TODO save pages where entities are extracted and that have attributes on them
    /**
     * @param args
     */
    public static void main(String[] args) {
        ListDiscoverer ld = new ListDiscoverer();
        String url = "";

        url = "data/benchmarkSelection/entities/google8/website39.html";
        url = "http://en.wikipedia.org/wiki/100_Years...100_Movies"; // TODO why is the page not retrieved correctly?
        url = "http://www.filmcrave.com/list_top_movie.php";
        url = "http://www.georgia.gov/00/topic_index_channel/0,2092,4802_5081,00.html";

        url = "http://www.stars-portraits.com/en/stars-cinema.php";
        url = "http://en.wikipedia.org/wiki/List_of_countries_by_population";

        // XPathWrapperInductor xwpi = new XPathWrapperInductor();
        // xwpi.setEntityXPath(path);
        // xwpi.extract("http://movies.yahoo.com/trailers/archive/0-9", true);
        // CollectionHelper.print(xwpi.getExtractions());

        String path = "";
        path = ld.discoverEntityXPath(url);
        if (path.length() == 0) {
            System.out.println("no path found");
            // return;
        } else {
            System.out.println("path: " + path.toLowerCase());
            System.out.println("path: " + path);
        }

        PageAnalyzer pa = new PageAnalyzer();
        DocumentRetriever crawler = new DocumentRetriever();
        Document document = crawler.getWebDocument(url);
        System.out.println(pa.getTextByXPath(document, path));

        List<Node> nodes = XPathHelper.getXhtmlNodes(document, path);
        for (Node n : nodes) {
            System.out.println("kbEntities.put(\"" + StringHelper.trim(n.getTextContent()) + "\",ct1);");
        }

        ld.findPaginationURLs(url);
        System.out.println(pa.getTextByXPath(document, ld.getPaginationXPath().replaceAll("/@href", "")));
        System.out.println("pagination xpath: " + ld.getPaginationXPath().toLowerCase());
    }
}