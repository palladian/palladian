package tud.iir.extraction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import tud.iir.helper.CollectionHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.StringHelper;
import tud.iir.helper.XPathHelper;
import tud.iir.web.Crawler;

/**
 * The PageAnalyzer's responsibility is it to perform generic tasks on the DOM tree.
 * 
 * @author David Urbansky
 */
public class PageAnalyzer {

    public static final Logger LOGGER = Logger.getLogger(PageAnalyzer.class);

    private Document document = null;

    // TODO general namespace handling, not only xhtml
    public PageAnalyzer() {
    }

    private Document getDocument() {
        return this.document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public void setDocument(String url) {
        Crawler c = new Crawler();
        this.document = c.getWebDocument(url);
    }

    /**
     * Find and return the content of the <title> tag of the web page.
     * 
     * @return The title of the web page.
     */
    @SuppressWarnings("unchecked")
    public String getTitle() {
        String title = "";

        String xPath = "//TITLE";
        try {
            xPath = XPathHelper.addNameSpaceToXPath(document, xPath);

            // TODO no attribute xpath working "/@href"
            DOMXPath xpathObj = new DOMXPath(xPath);

            List<Node> results = xpathObj.selectNodes(document.getLastChild());
            Iterator<Node> nodeIterator = results.iterator();
            while (nodeIterator.hasNext()) {
                Node node = nodeIterator.next();
                title += node.getTextContent();
            }

        } catch (JaxenException e) {
            Logger.getRootLogger().error(xPath, e);
            title = "#error#";
        } catch (DOMException e) {
            Logger.getRootLogger().error(xPath, e);
            title = "#error#";
        } catch (Exception e) {
            Logger.getRootLogger().error(xPath, e);
            title = "#error#";
        }

        return title;
    }


    /**
     * Try to find a table with at least 4 facts.
     * 
     * @return A string array with 0: the xpath to the table row, 1: the first td index and 2: the number of rows.
     */
    public String[] detectFactTable() {
        String[] tableParameters = { "", "", "" };

        XPathSet xPaths = getXPathSet();

        tableParameters[0] = xPaths.getHighestCountXPath(4);

        // number of rows is expected to be half of td count since table must have at least two columns
        tableParameters[2] = String.valueOf((int) Math.ceil((xPaths.getCountOfXPath(tableParameters[0]) / (double) 2)));

        // check whether there is one more xPath that ends on "th" instead of "td" with the same count
        if (tableParameters[0].length() > 0) {
            int thCount = xPaths
            .getCountOfXPath(tableParameters[0].substring(0, tableParameters[0].length() - 1) + "H");
            if (thCount == xPaths.getCountOfXPath(tableParameters[0])) {
                tableParameters[0] = tableParameters[0].substring(0, tableParameters[0].length() - 1) + "H";

                // if th is found each xPath count points to one row
                tableParameters[2] = String.valueOf(xPaths.getCountOfXPath(tableParameters[0]));
            }
        }

        tableParameters[1] = "0";
        // tableParameters[2] = String.valueOf((int)(xPaths.getCountOfXPath(tableParameters[0])/2));
        // tableParameters[2] = String.valueOf(getNumberOfTableRows(tableParameters[0]));

        return tableParameters;
    }

    /**
     * Get a set of xPaths.
     * 
     * @return A set of xPaths.
     */
    private XPathSet getXPathSet() {
        String[] listElements = { "//TD", "//TH" };
        XPathSet xPathSet = new XPathSet();

        for (String currentXPath : listElements) {

            List<Node> results = XPathHelper.getNodes(document, currentXPath);
            if (results == null) {
                continue;
            }
            for (Node currentNode : results) {
                String[] rcElements = { "TABLE" };
                String xPath = removeXPathIndicesNot(constructXPath(currentNode), rcElements);
                // System.out.println(pa.constructXPath(currentNode)+" / "+xPath);
                xPathSet.add(xPath);
            }
        }

        return xPathSet;
    }

    /**
     * Get all xPaths to the specified keyword in the specified document. The function does not return duplicates.
     * 
     * @param document The document.
     * @param keyword The keyword.
     * @return
     */
    public LinkedHashSet<String> constructAllXPaths(String keyword) {
        return constructAllXPaths(document, keyword, false, false);
    }

    public LinkedHashSet<String> constructAllXPaths(Document document, String keyword) {
        return constructAllXPaths(document, keyword, false, false);
    }

    public LinkedHashSet<String> constructAllXPaths(String keyword, boolean deleteAllIndices, boolean wordMatch) {
        return constructAllXPaths(document, keyword, deleteAllIndices, wordMatch);
    }

    public LinkedHashSet<String> constructAllXPaths(Document document, String keyword, boolean deleteAllIndices,
            boolean wordMatch) {
        LinkedHashSet<String> xpaths = new LinkedHashSet<String>();

        if (document == null) {
            Logger.getRootLogger().warn("document was null when constructing xpaths");
            return xpaths;
        }
        // System.out.println("print the DOM from last child");
        // printDOM(document.getLastChild(),"_");

        // xpaths = visit(document.getLastChild(),0,keyword,xpaths);
        try {
            xpaths = visit(document.getLastChild(), keyword, wordMatch, xpaths);
        } catch (StackOverflowError e) {
            Logger.getRootLogger().error(document.getDocumentURI(), e);
        } catch (Exception e) {
            Logger.getRootLogger().error(document.getDocumentURI(), e);
        }

        // add namespace if necessary TODO delete, do that only when applying the xpath (XPathHelper.getNodesNS)
        LinkedHashSet<String> nsxpaths = new LinkedHashSet<String>();
        Iterator<String> xpathIterator = xpaths.iterator();
        while (xpathIterator.hasNext()) {
            String currentXpath = xpathIterator.next();
            currentXpath = XPathHelper.addNameSpaceToXPath(document, currentXpath);

            if (deleteAllIndices) {
                currentXpath = removeXPathIndices(currentXpath);
            }

            nsxpaths.add(currentXpath);
        }

        return nsxpaths;
    }

    /**
     * Keep only xPaths that point to one of the specified elements. For example: [/HTML, /HTML/BODY/P] and [P] =>
     * [/HTML/BODY/P]
     * 
     * @param xPaths
     * @param targetNodes
     * @return A set of xPaths that all point to one of the specified elements.
     */
    public static LinkedHashSet<String> keepXPathPointingTo(LinkedHashSet<String> xPaths, String[] targetNodes) {
        LinkedHashSet<String> filteredXPaths = new LinkedHashSet<String>();

        Set<String> targetNodeSet = new HashSet<String>();
        for (String targetNode : targetNodes) {
            targetNodeSet.add(targetNode.toLowerCase());
        }
        for (String xPath : xPaths) {
            String[] elements = removeXPathIndices(xPath).split("/");
            String lastElement = elements[elements.length - 1].toLowerCase();
            lastElement = lastElement.replaceAll("xhtml:", "");
            if (targetNodeSet.contains(lastElement)) {
                filteredXPaths.add(xPath);
            }
        }

        return filteredXPaths;
    }

    /**
     * Find a single xPath that is generalized and works for many xPaths from the xPathSet. If several generalized
     * xPaths are found, take the one with the
     * highest count.
     * 
     * @param xPathSet A set of xPaths.
     * @return A string representing the mutual xPath.
     */
    public String makeMutualXPath(Set<String> xPathSet) {

        if (xPathSet.isEmpty()) {
            return "";
        }

        XPathSet xps = new XPathSet();
        Iterator<String> xPathIterator = xPathSet.iterator();
        while (xPathIterator.hasNext()) {
            xps.add(removeXPathIndices(xPathIterator.next()));
        }
        String highestCountXPath = xps.getHighestCountXPath();
        String[] highestCountXPathElements = highestCountXPath.split("/");

        // find the xpath with index that belongs to the group of a highest count xpath
        String mutualXPath = "";
        xPathIterator = xPathSet.iterator();
        while (xPathIterator.hasNext()) {
            String currentXPath = xPathIterator.next();
            boolean match = true;
            String[] xPathElements = removeXPathIndices(currentXPath).split("/");
            for (int i = 0; i < Math.min(xPathElements.length, highestCountXPathElements.length); i++) {
                if (!xPathElements[i].equals(highestCountXPathElements[i])) {
                    match = false;
                    break;
                }
            }
            if (match) {
                mutualXPath = currentXPath;
                break;
            }
        }

        String[] pathArray = mutualXPath.split("/");

        Integer[] indices = new Integer[pathArray.length];
        // 1 means keep index (no change), 0 means no index
        for (int i = 0; i < indices.length; i++) {
            indices[i] = 1;
        }

        Iterator<String> xPathIterator2 = xPathSet.iterator();
        while (xPathIterator2.hasNext()) {
            String xPath2 = xPathIterator2.next();
            String[] xPath2Array = xPath2.split("/");

            for (int i = 0; i < Math.min(pathArray.length, xPath2Array.length); i++) {
                int indexPosition1 = pathArray[i].indexOf("[");
                if (indexPosition1 == -1) {
                    continue;
                }

                int indexPosition2 = xPath2Array[i].indexOf("[");
                if (indexPosition2 == -1) {
                    continue;
                }

                int index1 = Integer.valueOf(pathArray[i].substring(indexPosition1 + 1, pathArray[i].length() - 1));
                int index2 = Integer.valueOf(xPath2Array[i].substring(indexPosition2 + 1, xPath2Array[i].length() - 1));

                if (!pathArray[i].substring(0, indexPosition1).equals(xPath2Array[i].substring(0, indexPosition2))) {
                    continue;
                }
                if (index1 != index2) {
                    indices[i] = 0;
                }
            }
        }

        // delete indices where necessary
        for (int i = 0; i < pathArray.length; i++) {
            int indexPosition = pathArray[i].indexOf("[");

            // there was no index, no change
            if (indexPosition == -1) {
                continue;
            }

            // no change between the different xpath found
            if (indices[i] == 1) {
                continue;
            }

            // change found, delete index
            pathArray[i] = pathArray[i].substring(0, indexPosition);
        }

        // rebuild the xpath
        mutualXPath = "";
        for (String element : pathArray) {
            mutualXPath += element + "/";
        }
        mutualXPath = mutualXPath.substring(0, mutualXPath.length() - 1);

        return mutualXPath;
    }

    /**
     * Recursive visit all nodes and check for a given keyword and create add the xPath if found.
     * 
     * @param node The start node.
     * @param keyword The keyword that needs to be found.
     * @param wordMatch If true a whole word has to match the keyword.
     * @param xpaths A set of xPath satisfying the conditions.
     */
    private LinkedHashSet<String> visit(Node node, String keyword, boolean wordMatch, LinkedHashSet<String> xpaths) {
        // System.out.println(indent+node.getNodeName());

        try {
            Node child = node.getFirstChild();
            while (child != null) {
                // System.out.println(" "+node.getNodeName()+" "+child.getNodeName());
                // if (child.getNodeType() == 3) System.out.println(child.getTextContent());

                // check whether the keyword appears in the node text, do not consider comment nodes (type 8)
                // TODO do not take if attribute is part of another word like CAPITALism

                // if (child.getNodeValue() != null)
                // System.out.println("found "+child.getNodeType()+","+child.getNodeName()+","+child.getNodeValue());

                if (child.getNodeValue() != null && child.getNodeType() != 8
                        && child.getNodeValue().toLowerCase().indexOf(keyword.toLowerCase()) > -1) {
                    // System.out.println("found "+child.getNodeType()+child.getNodeName()+child.getNodeValue());

                    if (wordMatch) {
                        Pattern pattern = Pattern
                        .compile("(?<![A-Za-z_-])" + StringHelper.escapeForRegularExpression(keyword)
                                + "(?![A-Za-z_-])", Pattern.CASE_INSENSITIVE);
                        Matcher m = pattern.matcher(child.getNodeValue());
                        if (m.find()) {
                            String xpath = constructXPath(child);
                            if (xpath.length() > 0) {
                                xpaths.add(xpath);
                            }
                        }
                    } else {
                        String xpath = constructXPath(child);
                        if (xpath.length() > 0) {
                            xpaths.add(xpath);
                        }
                    }

                    // makePattern(t);
                    // System.out.println(t);
                    // System.out.println("found "+nl.item(i).getNodeType()+nl.item(i).getNodeName()+nl.item(i).getNodeValue());
                    // break;
                }

                // if (depth == 200) return xpaths;
                xpaths = visit(child, keyword, wordMatch, xpaths);
                child = child.getNextSibling();
            }
        } catch (Exception e) {
            Logger.getRootLogger().error(e.getMessage());
        }

        return xpaths;
    }

    /**
     * Construct a simple xPath from the root to the specified node.
     * 
     * @param node The start node.
     * @return The string of the constructed xPath.
     */
    public String constructXPath(Node node) {
        String xpath = "";

        while (true) {
            // count numbers of previous siblings to determine the index
            int psCount = 0;
            String currentNodeName = node.getNodeName();
            Node ps = node.getPreviousSibling();
            while (ps != null) {
                if (ps.getNodeName().equalsIgnoreCase(currentNodeName)) {
                    ++psCount;
                }
                ps = ps.getPreviousSibling();
            }
            psCount++; // xpath is based on 1

            // if (node.getNextSibling() != null &&
            // node.getNextSibling().getNodeName().equalsIgnoreCase(currentNodeName) && psCount == 0) psCount = 1;
            // TODO allow th to have [] but change getNextSibling then!
            // TODO adding [1] also if no other elements exist could improve performance (XWI)
            String currentNode = node.getNodeName();
            if ((node.getNextSibling() != null || psCount > 1) && !node.getNodeName().equalsIgnoreCase("html")
                    && !node.getNodeName().equalsIgnoreCase("th")) {
                currentNode = node.getNodeName() + "[" + psCount + "]";
            }
            // System.out.println(node.getNodeName()+" "+node.getNodeType()+" "+node.getNodeValue());
            xpath = currentNode + "/" + xpath;

            // remove "#text" from xpath
            int textNodeIndex = xpath.indexOf("/#text");
            if (textNodeIndex > -1) {
                xpath = xpath.substring(0, textNodeIndex);
            }

            node = node.getParentNode();
            if (node == null) {
                break;
            }
        }

        // normalize (add tbody)
        // System.out.println("before normalization: "+xpath);
        xpath = xpath.substring(9, xpath.length());
        // System.out.println("after normalization: "+xpath);

        if (xpath.toLowerCase().indexOf("/script") > -1 || xpath.toLowerCase().indexOf("/html:script") > -1) {
            return "";
        }

        if (xpath.endsWith("/")) {
            xpath = xpath.substring(0, xpath.length() - 1);
        }

        // Logger.getInstance().log("constructed xpath: "+xpath.toLowerCase(),false);
        return xpath;
    }

    /**
     * Find out whether the node specified by the xPath is in a table (in a td cell).
     * 
     * @param xPath The xpath string pointing to the node.
     * @param lookBack How many parent nodes should be taken into account, e.g. with a lookBack of 3 the xpath
     *            /div/table/tr/td/div/span/a/b is not considered
     *            in a table because there is too much structure in the cell (more than 3 parents of the last node are
     *            not table structures).
     * @return True if given xpath points to a node in a table, else false.
     */
    public boolean nodeInTable(String xPath, int lookBack) {

        boolean inTable = false;
        String[] nodes = xPath.split("/");
        for (int nl = nodes.length, i = nl - 1; i > Math.max(0, nl - lookBack - 1); --i) {
            if (nodes[i].toLowerCase().indexOf("td") == 0 || nodes[i].toLowerCase().indexOf("xhtml:td") == 0
                    || nodes[i].toLowerCase().indexOf("th") == 0 || nodes[i].toLowerCase().indexOf("xhtml:th") == 0) {
                inTable = true;
                break;
            }
        }
        return inTable;
    }

    /**
     * Get the xPath to the table cell where the given xPath is pointing to. e.g. /div/p/table/tr/td/a[5]/b =>
     * /div/p/table/tr/td
     * 
     * @param xPath The xPath.
     * @return The string representation of an xPath.
     */
    public String getTableCellPath(String xPath) {

        String[] nodes = xPath.split("/");
        int index = nodes.length;
        for (int nl = nodes.length, i = nl - 1; i > 0; --i) {
            // System.out.println(i+" "+index+" "+nodes.length+" "+nodes[i]);
            if (nodes[i].toLowerCase().indexOf("td") == 0 || nodes[i].toLowerCase().indexOf("xhtml:td") == 0
                    || nodes[i].toLowerCase().indexOf("th") == 0 || nodes[i].toLowerCase().indexOf("xhtml:th") == 0) {
                index = i + 1;
                break;
            }
        }

        // rebuild xpath
        StringBuilder shortenedXPath = new StringBuilder();
        for (int i = 1; i < index; ++i) {
            shortenedXPath.append("/").append(nodes[i]);
        }

        return shortenedXPath.toString();
    }

    /**
     * Get the name of the node the given xPath is pointing to. e.g. /html/body/div/table[5]/tr/td[3]/p/a[4] => a
     * 
     * @param xpath The xPath.
     * @return The string representation of an xPath.
     */
    public String getTargetNode(String xpath) {
        int lastNodeIndex = xpath.lastIndexOf("/");
        String pointingNode = "";

        if (lastNodeIndex > -1) {
            pointingNode = xpath.substring(xpath.lastIndexOf("/") + 1);
            pointingNode = pointingNode.toLowerCase().replace("xhtml:", "").replaceAll("\\[(\\d)+\\]", "");
        }

        return pointingNode;
    }

    /**
     * Check whether a node is in a box. A box is the "p" and the "div" tag.
     * 
     * @param xPath The xPath.
     * @param lookBack How many parent nodes should be considered.
     * @return True if the specified xPath is in a box, else false.
     */
    public boolean nodeInBox(String xPath, int lookBack) {

        boolean inBox = false;
        String[] nodes = xPath.split("/");
        for (int nl = nodes.length, i = nl - 1; i > Math.max(0, nl - lookBack - 1); --i) {
            if (nodes[i].toLowerCase().indexOf("p") == 0 || nodes[i].toLowerCase().indexOf("xhtml:p") == 0
                    || nodes[i].toLowerCase().indexOf("div") == 0 || nodes[i].toLowerCase().indexOf("xhtml:div") == 0) {
                inBox = true;
                break;
            }
        }
        return inBox;
    }

    /**
     * Find the last box section ("p", "div", "td" or "th") of the given xPath. This is helpful as a certain term might
     * be in a too deep structure and searched
     * elements are around it. e.g. /table/tr/td/div[4]/span/b/a => /table/tr/td/div[4]
     * 
     * @param xPath The xPath.
     * @return The potentially shortened xPath if found, else the input xPath.
     */
    public String findLastBoxSection(String xPath) {

        String[] nodes = xPath.split("/");
        int index = nodes.length;
        for (int nl = nodes.length, i = nl - 1; i > 0; --i) {
            // System.out.println(i+" "+index+" "+nodes.length+" "+nodes[i]);
            if (nodes[i].toLowerCase().indexOf("p") == 0 || nodes[i].toLowerCase().indexOf("xhtml:p") == 0
                    || nodes[i].toLowerCase().indexOf("div") == 0 || nodes[i].toLowerCase().indexOf("xhtml:div") == 0
                    || nodes[i].toLowerCase().indexOf("td") == 0 || nodes[i].toLowerCase().indexOf("xhtml:td") == 0
                    || nodes[i].toLowerCase().indexOf("th") == 0 || nodes[i].toLowerCase().indexOf("xhtml:th") == 0) {
                index = i + 1;
                break;
            }
        }

        // rebuild xpath
        StringBuilder shortenedXPath = new StringBuilder();
        for (int i = 1; i < index; ++i) {
            shortenedXPath.append("/").append(nodes[i]);
        }

        return shortenedXPath.toString();
    }

    public String getNextSibling(String xPath) {
        return getNextSibling(xPath, false);
    }

    /**
     * Create an xpath that points to the next sibling of the node specified by the given xPath. e.g.
     * /div/p/table[4]/tr[6]/td[1] => /div/p/table[4]/tr[6]/td[2]
     * /div/p/table[4]/tr[6]/td[1]/div[4] => /div/p/table[4]/tr[6]/td[1]/div[5] /div/p/table[4]/tr[6]/th/b/a =>
     * /div/p/table[4]/tr[6]/td[1]/b/a
     * /div/p/table[4]/tr[6]/td => /div/p/table[4]/tr[7]/td ----- with tableCellSibling = true -----
     * /div/p/table[4]/tr[6]/td[1]/div[4] =>
     * /div/p/table[4]/tr[6]/td[2]/div[4] (compare with above) /div/p/table[4]/tr[6]/th/div[4] =>
     * /div/p/table[4]/tr[6]/td[1]/div[4] TODO sometimes a spacer
     * cell is between attribute and value: http://www.smartone-vodafone.com/jsp/phone/english/detail_v3.jsp?id=662
     * 
     * @param xPath The xPath
     * @param tableCellSibling If true, only siblings of table cells (td,th) are searched.
     * @return The xpath pointing to the sibling.
     */
    public String getNextSibling(String xPath, boolean tableCellSibling) {
        int lastOpeningBrackets;
        int lastClosingBrackets;

        // System.out.println("xpath in: "+xPath);

        if (tableCellSibling) {
            lastOpeningBrackets = Math.max(xPath.lastIndexOf("td["), xPath.lastIndexOf("TD[")) + 2;
            lastClosingBrackets = xPath.indexOf("]", lastOpeningBrackets);
        } else {
            lastOpeningBrackets = xPath.lastIndexOf("[");
            lastClosingBrackets = xPath.lastIndexOf("]");
        }

        // next sibling of "td" is "td[1]" and next sibling of "th" is "td[1]"
        int tdIndex = Math.max(xPath.toLowerCase().lastIndexOf("/td"), xPath.toLowerCase().lastIndexOf("/xhtml:td"));
        int thIndex = Math.max(xPath.toLowerCase().lastIndexOf("/th"), xPath.toLowerCase().lastIndexOf("/xhtml:th"));

        if (tdIndex > lastClosingBrackets && tdIndex > thIndex) {
            String firstPart = xPath.substring(0, tdIndex);
            String lastPart = xPath.substring(tdIndex).replace("/td", "/td[1]").replace("/TD", "/TD[1]")
            .replace("/xhtml:td", "/xhtml:td[1]").replace("/xhtml:TD", "/xhtml:TD[1]");
            xPath = firstPart + lastPart;
            return xPath;
        } else if (thIndex > lastClosingBrackets && thIndex > tdIndex) {
            String firstPart = xPath.substring(0, thIndex);
            String lastPart = xPath.substring(thIndex).replace("/th", "/td[1]").replace("/TH", "/TD[1]")
            .replace("/xhtml:th", "/xhtml:td[1]").replace("/xhtml:TH", "/xhtml:TD[1]");
            xPath = firstPart + lastPart;
            return xPath;
        }

        // if not found any brackets return the given xpath
        if (lastClosingBrackets <= lastOpeningBrackets || lastOpeningBrackets == 1) {
            return xPath;
        }

        // update counter and return the updated xpath (no th was found after the last brackets)
        int currentIndex = Integer.valueOf(xPath.substring(lastOpeningBrackets + 1, lastClosingBrackets));
        return xPath.substring(0, lastOpeningBrackets + 1) + String.valueOf(++currentIndex)
        + xPath.substring(lastClosingBrackets);
    }

    public String getNextTableCell(String xPath) {
        return getNextSibling(xPath, true);
    }

    /**
     * Point xPath to first table cell. For example: //TABLE/TR/TD => //TABLE/TR/TD[1] //TABLE/TR/TD[1] =>
     * //TABLE/TR/TD[1] //TABLE/TR/TH => //TABLE/TR/TH
     * 
     * @param xPath The xPath.
     * @return The xPath pointing to the first table cell of the deepest table.
     */
    public String getFirstTableCell(String xPath) {
        int lastOpeningBrackets;
        int lastClosingBrackets;

        lastOpeningBrackets = Math.max(xPath.lastIndexOf("td["), xPath.lastIndexOf("TD[")) + 2;
        lastClosingBrackets = xPath.indexOf("]", lastOpeningBrackets);

        // first cell of "td" is "td[1]" and first cell of "th" is "th"
        int tdIndex = Math.max(xPath.toLowerCase().lastIndexOf("/td"), xPath.toLowerCase().lastIndexOf("/xhtml:td"));
        int thIndex = Math.max(xPath.toLowerCase().lastIndexOf("/th"), xPath.toLowerCase().lastIndexOf("/xhtml:th"));

        if (tdIndex > lastClosingBrackets && tdIndex > thIndex) {
            String firstPart = xPath.substring(0, tdIndex);
            String lastPart = xPath.substring(tdIndex).replace("/td", "/td[1]").replace("/TD", "/TD[1]")
            .replace("/xhtml:td", "/xhtml:td[1]").replace("/xhtml:TD", "/xhtml:TD[1]");
            xPath = firstPart + lastPart;
            return xPath;
        }

        return xPath;
    }

    /**
     * Get number of table rows.
     * 
     * @param attributeXPath This path should point to one attribute cell.
     * @return The number of table rows.
     */
    public int getNumberOfTableRows(String attributeXPath) {
        return getTableRows(getDocument(), attributeXPath, getNextSibling(attributeXPath, true)).size();
    }

    /**
     * Get rows of a table.
     * 
     * @param attributeXPath This path should point to one attribute cell.
     * @return An array of table row xPaths.
     */
    public ArrayList<String[]> getTableRows(String attributeXPath) {
        return getTableRows(getDocument(), attributeXPath, getNextSibling(attributeXPath, true));
    }

    /**
     * Get rows of a table.
     * 
     * @param attributeXPath This path should point to one attribute cell.
     * @param siblingXPath This path should point to the fact value cell of the attribute.
     * @return An array of table row xPaths.
     */
    public ArrayList<String[]> getTableRows(String attributeXPath, String siblingXPath) {
        return getTableRows(getDocument(), attributeXPath, siblingXPath);
    }

    /**
     * Get rows of a table.
     * 
     * @param document The document.
     * @param attributeXPath This path should point to one attribute cell.
     * @param siblingXPath This path should point to the fact value cell of the attribute.
     * @return An array of table row xPaths.
     */
    public ArrayList<String[]> getTableRows(Document document, String attributeXPath, String siblingXPath) {
        ArrayList<String[]> tableRowsXPaths = new ArrayList<String[]>();

        int lastOpeningBrackets = Math.max(attributeXPath.lastIndexOf("tr["), attributeXPath.lastIndexOf("TR[")) + 2;
        int lastClosingBrackets = attributeXPath.indexOf("]", lastOpeningBrackets);

        // if to specific tr is given, the first one is taken
        if (lastClosingBrackets <= lastOpeningBrackets || lastOpeningBrackets == 1) {
            attributeXPath = getNextTableRow(attributeXPath);
            siblingXPath = getNextTableRow(siblingXPath);
            lastOpeningBrackets = Math.max(attributeXPath.lastIndexOf("tr["), attributeXPath.lastIndexOf("TR[")) + 2;
            lastClosingBrackets = attributeXPath.indexOf("]", lastOpeningBrackets);
        }

        // if not found any brackets return an empty array
        if (lastClosingBrackets <= lastOpeningBrackets || lastOpeningBrackets == 1) {
            return tableRowsXPaths;
        }

        // find out how many rows the table has
        String tableXPath = getParentNode(attributeXPath.substring(0, lastOpeningBrackets));
        List<Node> tableNodes = XPathHelper.getNodes(document, tableXPath);
        if (tableNodes.size() == 0) {
            return tableRowsXPaths;
        }

        int rowCount = 0;
        NodeList childNodes = tableNodes.get(0).getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeName().toLowerCase().equals("tr")) {
                rowCount++;
            }
        }
        // int rowCount = tableNodes.get(0).getChildNodes().getLength();

        // create xPaths for each row
        for (int i = 1; i <= rowCount; i++) {
            String[] rowXPaths = new String[2];
            rowXPaths[0] = attributeXPath.substring(0, lastOpeningBrackets + 1) + String.valueOf(i)
            + attributeXPath.substring(lastClosingBrackets);
            rowXPaths[1] = siblingXPath.substring(0, lastOpeningBrackets + 1) + String.valueOf(i)
            + siblingXPath.substring(lastClosingBrackets);

            tableRowsXPaths.add(rowXPaths);
        }

        return tableRowsXPaths;
    }

    /**
     * Find the next table row for a given xPath. For example: //TABLE/TR[1]/TD[2] => //TABLE/TR[2]/TD[2]
     * //TABLE/TR/TD[2] => //TABLE/TR[1]/TD[2]
     * 
     * @param xPath
     * @return
     */
    public String getNextTableRow(String xPath) {

        int trIndex = xPath.toLowerCase().lastIndexOf("tr");

        if (trIndex == -1) {
            return xPath;
        }

        // check whether tr has index already
        if (xPath.substring(trIndex + 2, trIndex + 3).equals("[")) {
            int currentIndex = Integer.valueOf(xPath.substring(trIndex + 3, xPath.indexOf("]", trIndex + 3)));
            xPath = xPath.substring(0, trIndex + 3) + String.valueOf(currentIndex + 1)
            + xPath.substring(xPath.indexOf("]", trIndex + 3));
            return xPath;
        } else {
            xPath = xPath.substring(0, trIndex + 2) + "[1]" + xPath.substring(trIndex + 2);
            return xPath;
        }

    }

    /**
     * Move one tag up in the DOM, e.g. /div/span/a => /div/span.
     * 
     * @param xPath The xPath.
     * @return The parent node.
     */
    public static String getParentNode(String xPath) {
        return xPath.substring(0, xPath.lastIndexOf("/"));
    }

    /**
     * Count the number of columns in a table.
     * 
     * @param document The document.
     * @param tableTDXPath The xPath to the table data tag.
     * @return The number of columns.
     */
    public int getNumberOfTableColumns(Document document, String tableTDXPath) {
        int numberOfColumns = 0;

        // System.out.println(getParentNode(getTableCellPath(tableTDXPath)));
        // printDOM(XPathHelper.getNodes(document, getParentNode(getTableCellPath(tableTDXPath))).item(0), " ");
        List<Node> nodeList = XPathHelper.getNodes(document, getParentNode(getTableCellPath(tableTDXPath)));
        LinkedHashMap<Integer, Integer> tdCountMap = new LinkedHashMap<Integer, Integer>();

        for (int i = 0; i < nodeList.size(); i++) {
            Node trNode = nodeList.get(i);
            NodeList nl = trNode.getChildNodes();

            numberOfColumns = 0;
            for (int j = 0; j < nl.getLength(); j++) {
                // System.out.println(nl.item(j).getNodeName());
                // nl.item(i).getAttributes();
                if (nl.item(j).getNodeName().equalsIgnoreCase("td") || nl.item(j).getNodeName().equalsIgnoreCase("th")) {
                    // check whether column is colspan
                    NamedNodeMap attributes = nl.item(j).getAttributes();
                    for (int k = 0; k < attributes.getLength(); k++) {
                        if (attributes.item(k).getNodeName().equalsIgnoreCase("colspan")) {
                            numberOfColumns += Integer.valueOf(attributes.item(k).getNodeValue()) - 1;
                            break;
                        }
                    }
                    numberOfColumns++;
                }
            }

            if (tdCountMap.containsKey(numberOfColumns)) {
                int count = tdCountMap.get(numberOfColumns);
                tdCountMap.put(numberOfColumns, count + 1);
            } else {
                tdCountMap.put(numberOfColumns, 1);
            }
        }

        if (tdCountMap.entrySet().size() == 0) {
            return 0;
        }

        tdCountMap = CollectionHelper.sortByValue(tdCountMap.entrySet(), CollectionHelper.DESCENDING);
        numberOfColumns = tdCountMap.entrySet().iterator().next().getKey();

        // rowspan might have led to zero columns
        if (numberOfColumns == 0) {
            numberOfColumns = 1;
        }

        return numberOfColumns;
    }

    @SuppressWarnings("unchecked")
    public String getHTMLTextByXPath(String xPath) {
        StringBuilder sb = new StringBuilder();

        DOMXPath xpathObj;
        try {
            xpathObj = new DOMXPath(xPath);

            xpathObj.addNamespace("xhtml", "http://www.w3.org/1999/xhtml");

            List<Node> results = xpathObj.selectNodes(document.getLastChild());

            Iterator<Node> nodeIterator = results.iterator();
            while (nodeIterator.hasNext()) {
                Node node = nodeIterator.next();
                // String a = node.getNodeName();
                // String b = node.getNodeValue();
                // String c = node.getTextContent();
                // String d = node.getChildNodes().toString();
                // sb.append(nodeIterator.next());

                sb.append(getChildHTMLContents(node, new StringBuilder()));
            }
        } catch (JaxenException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public String getTextByXPath(String xPath) {
        return getTextByXPath(document, xPath);
    }

    public String getTextByXPath(Document document, String xpath) {

        if (document == null || xpath.length() == 0) {
            Logger.getRootLogger().warn("document is NULL or xpath is empty");
            return "";
        }

        StringBuilder sb = new StringBuilder();

        try {
            // TODO next line, DOMXPath instead of XPath and document.getLastChild changed (might lead to different
            // evaluation results)
            // xpath = XPathHelper.addNameSpaceToXPath(document, xpath);
            //
            // // TODO no attribute xpath working "/@href"
            // DOMXPath xpathObj = new DOMXPath(xpath);
            // xpathObj.addNamespace("xhtml", "http://www.w3.org/1999/xhtml");
            //
            // List<Node> results = xpathObj.selectNodes(document.getLastChild());

            List<Node> results = XPathHelper.getNodes(document, xpath);

            Iterator<Node> nodeIterator = results.iterator();
            while (nodeIterator.hasNext()) {

                // get all text nodes
                Node node = nodeIterator.next();
                sb.append(getSeparatedTextContents(node, new StringBuilder(""))).append(" ");
                // sb.append(nodeIterator.next().getTextContent()).append(" "); // texts from different nodes stick
                // together
            }

        } catch (DOMException e) {
            Logger.getRootLogger().error(xpath + " " + e.getMessage());
            return "#error#";
        } catch (Exception e) {
            Logger.getRootLogger().error(xpath + " " + e.getMessage());
            return "#error#";
        } catch (OutOfMemoryError e) {
            Logger.getRootLogger().error(xpath + " " + e.getMessage());
            return "#error#";
        }

        return sb.toString();
    }

    private StringBuilder getSeparatedTextContents(Node node, StringBuilder currentString) throws OutOfMemoryError {
        Node child = node.getFirstChild();

        int maximumTags = 50;
        int tagCount = 0;
        while (child != null && tagCount < maximumTags) {

            // do not consider comment nodes (type 8) TODO only take text nodes
            if (child.getNodeValue() != null && child.getNodeType() == 3) {
                String nodeValue = StringHelper.trim(child.getNodeValue(), ":?!");
                if (nodeValue.length() > 0) {
                    currentString.append(nodeValue).append(" ");// + ", "; TODO changed without testing!
                }
                // System.out.println(child.getNodeType());
            }

            currentString = getSeparatedTextContents(child, currentString);
            child = child.getNextSibling();
            tagCount++;
        }

        // if (currentString.endsWith(", ")) currentString = currentString.substring(0,currentString.length()-2);

        return currentString;
    }

    private StringBuilder getChildHTMLContents(Node node, StringBuilder currentString) {
        Node child = node.getFirstChild();

        int maximumTags = 50;
        int tagCount = 0;
        while (child != null && tagCount < maximumTags) {
            // System.out.println(child.getNodeType() + " " + child.getNodeName());
            // do not consider comment nodes (type 8)
            if (child.getNodeType() == 3 || child.getNodeType() == 1) {
                if (child.getNodeValue() != null && child.getNodeName().startsWith("#")) {
                    if (!child.getNodeName().startsWith("#")) {
                        currentString.append("<");
                        currentString.append(child.getNodeName());
                        currentString.append(">");
                        currentString.append(child.getNodeValue());
                        currentString.append("</");
                        currentString.append(child.getNodeName());
                        currentString.append(">");
                    } else {
                        currentString.append(child.getNodeValue());
                    }
                } else {

                    currentString.append("<").append(child.getNodeName()).append(" ");

                    // add attributes
                    NamedNodeMap nnm = child.getAttributes();
                    for (int i = 0; i < nnm.getLength(); i++) {
                        Node attributeNode = nnm.item(i);
                        currentString.append(attributeNode.getNodeName());
                        currentString.append("=\"");
                        currentString.append(attributeNode.getTextContent()).append("\" ");
                    }

                    currentString.append("/>");
                }
            }

            currentString = getChildHTMLContents(child, currentString);
            child = child.getNextSibling();
            tagCount++;
        }

        // if (currentString.endsWith(", ")) currentString = currentString.substring(0,currentString.length()-2);

        return currentString;
    }

    /**
     * If an xPath points to several (sibling) nodes, get the text of each node and add it to a list.
     * 
     * @param xPath The xPath.
     * @return A list of contents from the nodes that were targeted with the xPath.
     */
    public ArrayList<String> getTextsByXPath(String xPath) {
        return getTextsByXpath(document, xPath);
    }

    @SuppressWarnings("unchecked")
    public ArrayList<String> getTextsByXpath(Document document, String xpath) {

        ArrayList<String> texts = new ArrayList<String>();

        if (document == null) {
            return texts;
        }


        try {
            // TODO next line, DOMXPath instead of XPath and document.getLastChild changed (might lead to different
            // evaluation results)
            xpath = XPathHelper.addNameSpaceToXPath(document, xpath);

            // xpath = xpath.replaceAll("/xhtml:TBODY", "/");

            // TODO no attribute xpath working "/@href"
            DOMXPath xpathObj = new DOMXPath(xpath);
            xpathObj.addNamespace("xhtml", "http://www.w3.org/1999/xhtml");

            List<Node> results = xpathObj.selectNodes(document.getLastChild());

            Iterator<Node> nodeIterator = results.iterator();
            while (nodeIterator.hasNext()) {
                // get all text nodes
                Node node = nodeIterator.next();
                texts.add(node.getTextContent());
            }

        } catch (JaxenException e) {
            Logger.getRootLogger().error(xpath, e);
        } catch (DOMException e) {
            Logger.getRootLogger().error(xpath, e);
        } catch (Exception e) {
            Logger.getRootLogger().error(xpath, e);
        }

        return texts;
    }

    public static String removeXPathIndices(String xPath) {
        return xPath.replaceAll("\\[(\\d)+\\]", "");
    }

    /**
     * For example: /html/div/p[3]/small => /html/div/p/small
     * 
     * @param xPath
     * @return
     */
    public static String removeXPathIndicesFromLastCountNode(String xPath) {

        String xPathR = StringHelper.reverseString(xPath);
        xPathR = xPathR.replaceFirst("\\](\\d)+\\[", "");

        return StringHelper.reverseString(xPathR);
        // int i = xPath.lastIndexOf("]");
        // String xPath1 = xPath.substring(0, i);
        // String xPath2 = xPath.substring(i);
        // xPath2 = removeXPathIndices(xPath2);
        // return xPath1 + xPath2;
    }

    public static String removeXPathIndices(String xPath, String[] removeCountElements) {
        for (String removeCountElement : removeCountElements) {
            xPath = xPath.replaceAll(removeCountElement + "\\[(\\d)+\\]", removeCountElement);
        }

        return xPath;
    }

    public static String removeXPathIndicesNot(String xPath, String[] notRemoveCountElements) {
        for (String notRemoveCountElement : notRemoveCountElements) {
            xPath = xPath
            .replaceAll(notRemoveCountElement + "\\[(\\d)+\\]", notRemoveCountElement + "\\{$1\\}");
        }

        xPath = xPath.replaceAll("\\[(\\d)+\\]", "");

        for (String notRemoveCountElement : notRemoveCountElements) {
            xPath = xPath
            .replaceAll(notRemoveCountElement + "\\{(\\d)+\\}", notRemoveCountElement + "\\[$1\\]");
        }
        return xPath;
    }

    public static void main(String[] args) {

        String url = "http://www.cinefreaks.com/downloads";
        url = "data/test/webPages/faqExtraction2.html";
        Crawler c = new Crawler();
        Document document = c.getWebDocument(url);

        System.out.println(HTMLHelper.getXmlDump(document));
        System.exit(1);

        // String t = PageAnalyzer.getDocumentTextDump(document);
        String t = HTMLHelper.documentToHTMLString(document);

        System.out.println(t.getBytes().length);
        System.out.println(t);
        // System.out.println(t.indexOf("https://sec1.woopra.com"));

        System.exit(0);

        PageAnalyzer pa = new PageAnalyzer();
        pa.setDocument("data/benchmarkSelection/qa/training/webpage32.html");

        // LinkedHashSet lhs = pa.constructAllXPaths(pa.getDocument(), "?", false, false);
        Set<String> lhs = pa.constructAllXPaths("vitamins B1, B2, B3, B5 and C.  Actions");
        CollectionHelper.print(lhs);
    }
    /**
     * @param args
     */
    /*
     * public static void main(String[] args) throws Exception { DOMParser parser = new DOMParser(); // Crawler c = new
     * Crawler(); // String pageString =
     * c.download("http://www.mobileburn.com/review.jsp?Id=4993"); // StringReader stringReader = new
     * StringReader(pageString); // InputSource is = new
     * InputSource(stringReader); // parser.parse(is); PageAnalyzer pa = new PageAnalyzer(); InputSource is = new
     * InputSource(new BufferedInputStream(new
     * FileInputStream("data/test/reviewPage.html")));
     * parser.parse(is);//"http://www.mobileburn.com/review.jsp?Id=4993"); Document document =
     * parser.getDocument(); Node startNode = document.getLastChild().getChildNodes().item(1);
     * //System.out.println(startNode.getNodeName()+" "+document.getNodeName()); //pa.constructXPath(startNode);
     * HashSet<String> xpath =
     * pa.constructAllXPaths(document, "video"); System.out.println("found "+xpath.size()+" xpaths"); Iterator<String>
     * xpathIterator = xpath.iterator(); while
     * (xpathIterator.hasNext()) { String test = xpathIterator.next(); System.out.println("test xpath: "+test); // test
     * the xpaths org.jaxen.XPath xpath2 = new
     * DOMXPath(test); xpath2.addNamespace("xhtml", "http://www.w3.org/1999/xhtml"); List results =
     * xpath2.selectNodes(startNode);
     * //System.out.println(results.size()); Iterator<Node> nodeIterator = results.iterator(); while
     * (nodeIterator.hasNext()) { Node node = nodeIterator.next();
     * System.out.println("result: "+node.getNodeName()+" with "+" "+node.getTextContent()); } } // test getTextByXPath
     * System.out.println(pa.getTextByXpath(document,
     * "/xhtml:HTML/xhtml:BODY/xhtml:DIV[1]/xhtml:DIV[1]/xhtml:DIV[4]/xhtml:DIV[1]/xhtml:DIV[2]/xhtml:DIV[1]/xhtml:DIV[1]/xhtml:DIV[1]/xhtml:DIV[1]/xhtml:DIV[1]/xhtml:DIV[1]/xhtml:DIV[2]/xhtml:TABLE[3]/xhtml:TR[11]/xhtml:TD[1]"
     * )); String t = pa.getTextByXpath(document,
     * "/xhtml:HTML/xhtml:BODY/xhtml:DIV/xhtml:DIV[1]/xhtml:DIV[4]/xhtml:DIV[1]/xhtml:DIV[1]/xhtml:DIV[1]/xhtml:DIV"); t
     * = StringHelper.trim(t);
     * System.out.println(t); }
     */
}