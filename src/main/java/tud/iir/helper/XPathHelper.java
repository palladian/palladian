package tud.iir.helper;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import tud.iir.web.Crawler;

/**
 * A helper to handle xPath.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Martin Werner
 */
public class XPathHelper {

    /**
     * Check whether document has an xhtml namespace declared.
     * 
     * @param document The document.
     * @return True if the document has an xhtml namespace declared, else false.
     */
    public static boolean hasXMLNS(final Document document) {

        if (document == null) {
            return false;
        }

        Node node = null;
        if (document.getLastChild() != null && document.getLastChild().getAttributes() != null
                && document.getLastChild().getAttributes() != null) {
            node = document.getLastChild().getAttributes().getNamedItem("xmlns");
        }

        if (node != null && node.getTextContent().toLowerCase().indexOf("xhtml") > -1) {
            return true;
        }
        return false;
    }

    /**
     * Add the xhtml namespace to an xPath.
     * 
     * @param document The document.
     * @param xPath The xPath.
     * @return The xPath with the namespace.
     */
    public static String addNameSpaceToXPath(final Document document, final String xPath) {
        String returnValue = xPath;
        if (hasXMLNS(document)) {
            returnValue = addNameSpaceToXPath(xPath);
        }
        return returnValue;
    }

    /**
     * Add the xhtml namespace to an xPath in case it does not have it yet.
     * 
     * TODO this also adds the namespace to XPath functions. For example "//div/text()" is transformed to
     * "//xhtml:div/xhtml:text()".
     * 
     * @param xPath The xPath.
     * @return The xPath with included xhtml namespace.
     */
    public static String addNameSpaceToXPath(final String xPath) {
        if (xPath.toLowerCase(Locale.ENGLISH).indexOf("xhtml:") > -1) {
            return xPath;
        }
        // return xPath.replaceAll("/(?=\\w)","/xhtml:");
        // this is a fix NOT to touch slashes inside quotes,
        // for example in @type='application/rss+xml'
        // RegEx from http://stackoverflow.com/questions/632475/regex-to-pick-commas-outside-of-quotes
        return xPath.replaceAll("(/)(?=\\w(?:[^']|'[^']*')*$)", "/xhtml:");
    }

    /**
     * Gets the nodes.
     * 
     * @param document the document
     * @param xPath the x path
     * @return the nodes
     */
    public static List<Node> getNodes(Document document, String xPath) {
        if (document == null || xPath == null || xPath.length() == 0) {
            return new ArrayList<Node>();
        }
        String modXPath = addNameSpaceToXPath(document, xPath);

        return getNodes(document.getLastChild(), modXPath);
    }

    // TODO uppercase xPath and namespace (xml vs. xhtml!?)
    /**
     * Gets the nodes.
     * 
     * @param node the node
     * @param xPath the x path
     * @return the nodes
     */
    @SuppressWarnings("unchecked")
    public static List<Node> getNodes(final Node node, final String xPath) {

        List<Node> nodes = new ArrayList<Node>();
        try {
            // System.out.println(xPath);
            final DOMXPath xpathObj = new DOMXPath(xPath);
            xpathObj.addNamespace("xhtml", "http://www.w3.org/1999/xhtml");

            nodes = xpathObj.selectNodes(node);
        } catch (JaxenException e) {
            Logger.getRootLogger().error(xPath + ", " + e.getMessage());
        } catch (OutOfMemoryError e) {
            Logger.getRootLogger().error(xPath + ", " + e.getMessage());
        } catch (NullPointerException e) {
            Logger.getRootLogger().error(xPath + ", " + e.getMessage());
        } catch (Error e) {
            Logger.getRootLogger().error(xPath + ", " + e.getMessage());
        }

        return nodes;
    }

    /**
     * Get a node by xPath.
     * 
     * @param node The node where the xPath should be applied to.
     * @param xPath The xPath.
     * @return The node that the xPath points to.
     */
    public static Node getNode(final Node node, final String xPath) {
        if (node == null) {
            return null;
        }
        Node targetNode = null;
        final List<Node> nodeList = getNodes(node, xPath);
        if (nodeList.iterator().hasNext()) {
            targetNode = nodeList.iterator().next();
        }
        return targetNode;
    }

    /**
     * Gets the node.
     * 
     * @param doc the doc
     * @param xPath the x path
     * @return the node
     */
    public static Node getNode(final Document doc, String xPath) {
        if (doc == null) {
            return null;
        }
        Node targetNode = null;
        final List<Node> nodeList = getNodes(doc, xPath);
        if (nodeList.iterator().hasNext()) {
            targetNode = nodeList.iterator().next();
        }
        return targetNode;
    }

    /*
     * public static Node getChildNodeNS(Node node, String xPath) { if (node == null) return null; Node childNode =
     * null; try { //System.out.println(xPath);
     * DOMXPath xpathObj = new DOMXPath(xPath); xpathObj.addNamespace("xhtml", "http://www.w3.org/1999/xhtml");
     * List<Node> nodeList =
     * xpathObj.selectNodes(node); if (nodeList.size() > 0) childNode = nodeList.get(0); } catch (JaxenException e) {
     * Logger.getRootLogger().error(xPath + ", "
     * + e.getMessage()); } catch (OutOfMemoryError e) { Logger.getRootLogger().error(xPath + ", " + e.getMessage()); }
     * catch (NullPointerException e) {
     * Logger.getRootLogger().error(xPath + ", " + e.getMessage()); } catch (Error e) {
     * Logger.getRootLogger().error(xPath + ", " + e.getMessage()); } return
     * childNode; }
     */

    /**
     * Gets the node by id.
     * 
     * @param document the document
     * @param nodeId the id
     * @return the node by id
     */
    public static Node getNodeByID(final Document document, final String nodeId) {

        Node node = null;
        try {
            final List<Node> idNodes = XPathHelper.getNodes(document, "//*[@id='" + nodeId + "']");
            for (int i = 0; i < Math.min(1, idNodes.size()); i++) {
                node = idNodes.get(i).getParentNode();
            }
        } catch (OutOfMemoryError e) {
            Logger.getRootLogger().error(nodeId + ", " + e.getMessage());
        } catch (NullPointerException e) {
            Logger.getRootLogger().error(nodeId + ", " + e.getMessage());
        } catch (Error e) {
            Logger.getRootLogger().error(nodeId + ", " + e.getMessage());
        }

        return node;
    }

    /**
     * Get a child node by xPath.
     * 
     * @param node The parent node under which the sought node must descend.
     * @param xPath The xPath that points to a node.
     * @return A node that matches the xPath and descends from the given node.
     */
    public static Node getChildNode(final Node node, final String xPath) {
        if (node == null) {
            return null;
        }
        Node childNode = null;
        if (getChildNodes(node, xPath).iterator().hasNext()) {
            childNode = getChildNodes(node, xPath).iterator().next();
        }
        return childNode;
    }

    /**
     * Check whether a node is a child (descendant) of another node.
     * 
     * @param childCandidate The node that is checked to be a child.
     * @param parent The node under which the childCandidate must descend.
     * @return True, if the childCandidate really descends from the parent, false otherwise.
     */
    private static boolean isChildOf(Node childCandidate, Node parent) {

        Node modChildCandidate = childCandidate.getParentNode();
        while (modChildCandidate != null) {
            if (modChildCandidate.equals(parent)) {
                return true;
            }
            modChildCandidate = modChildCandidate.getParentNode();
        }

        return false;
    }

    /**
     * Gets the child nodes of a given node that are addressed by the given xPath.
     * 
     * @param node The parent node of the children.
     * @param xPath The xPath that addresses the children.
     * @return The child nodes.
     */
    public static List<Node> getChildNodes(Node node, String xPath) {
        List<Node> childNodes = null;
        List<Node> childNodesMatch = new ArrayList<Node>();

        xPath = addNameSpaceToXPath(xPath);
        childNodes = getNodes(node, xPath);

        System.out.println(" " + childNodes.size());

        for (Node cn : childNodes) {
            if (isChildOf(cn, node)) {
                childNodesMatch.add(cn);
            }
        }

        return childNodesMatch;
    }

    /**
     * Gets the child nodes.
     * 
     * @param node the (parent)node
     * @return the childNodes
     */
    public static List<Node> getChildNodes(final Node node) {

        final List<Node> children = new ArrayList<Node>();

        try {
            final NodeList childNodes = node.getChildNodes();
            if (childNodes != null) {
                for (int x = 0; x < childNodes.getLength(); x++) {
                    if (childNodes.item(x).getNodeName() != null && isChildOf(childNodes.item(x), node)) {
                        children.add(childNodes.item(x));
                    }
                }
            }
        } catch (Exception e) {
            Logger.getRootLogger().error(e.getMessage());
        }

        return children;
    }

    /**
     * Convert a node and his children to string.
     * 
     * @param node the node
     * @return the node as string
     */
    public static String convertNodeToString(final Node node) {
        Transformer trans = null;
        try {
            trans = TransformerFactory.newInstance().newTransformer();
        } catch (TransformerConfigurationException e1) {
            Logger.getRootLogger().error(e1.getMessage());
        } catch (TransformerFactoryConfigurationError e1) {
            Logger.getRootLogger().error(e1.getMessage());
        }

        final StringWriter sWriter = new StringWriter();
        try {
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.transform(new DOMSource(node), new StreamResult(sWriter));
        } catch (TransformerException e) {
            Logger.getRootLogger().error(e.getMessage());
        }
        String result = sWriter.toString();
        result = result.replace(" xmlns=\"http://www.w3.org/1999/xhtml\"", "");
        // result = result.replace("xmlns=\"http://www.w3.org/1999/xhtml\"", "");

        return result;
    }

    /**
     * Gets the previous sibling nodes of a node.
     * 
     * @param node the node
     * @return the previous siblings
     */
    public static List<Node> getPreviousSiblings(final Node node) {

        final Node parentNode = node.getParentNode();

        final List<Node> previousSiblings = new ArrayList<Node>();
        final List<Node> childNodes = XPathHelper.getChildNodes(parentNode);

        for (Node childNode : childNodes) {
            if (childNode.isSameNode(node)) {

                break;
            } else {
                previousSiblings.add(childNode);

            }
        }
        return previousSiblings;
    }

    public static void main(String[] args) {

        // String xPath = "//div/text()";
        // System.out.println(addNameSpaceToXPath(xPath));

        Crawler crawler = new Crawler();
        Document doc = crawler.getWebDocument("data/test/xPathTestcase.html");
        
        // iterate over all TRs
        List<Node> rows = XPathHelper.getNodes(doc, "//TABLE/TR");
        for (Node row : rows) {

            // System.out.println(HTMLHelper.getXmlDump(row));
            
            // iterate over TDs
            List<Node> cells = XPathHelper.getChildNodes(row, "//TD"); // does not work EDIT: now it does
            // List<Node> cells = XPathHelper.getChildNodes(row, "*"); // infinite loop? EDIT: yes, stupid me :) solved.
            for (Node cell : cells) {
                System.out.println(cell.getTextContent());
            }
        }

    }

}