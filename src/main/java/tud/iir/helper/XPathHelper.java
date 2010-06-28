package tud.iir.helper;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A helper to handle xPath.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class XPathHelper {

    /**
     * Check whether document has a xhtml namespace declared.
     * 
     * @param document The document.
     * @return True if the document has a xhtml namespace declared, else false.
     */
    public static boolean hasXMLNS(Document document) {
        if (document == null) {
            return false;
        }

        Node node = null;
        if (document.getLastChild() != null && document.getLastChild().getAttributes() != null && document.getLastChild().getAttributes() != null) {
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
    public static String addNameSpaceToXPath(Document document, String xPath) {
        if (hasXMLNS(document)) {
            return addNameSpaceToXPath(xPath);
        }
        return xPath;
    }

    /**
     * Add the xhtml namespace to an xPath in case it does not have it yet.
     * 
     * @param xPath The xPath.
     * @return The xPath with included xhtml namespace.
     */
    public static String addNameSpaceToXPath(String xPath) {
        if (xPath.toLowerCase().indexOf("xhtml:") > -1) {
            return xPath;
        }
        // return xPath.replaceAll("/(?=\\w)","/xhtml:");
        // this is a fix NOT to touch slashes inside quotes,
        // for example in @type='application/rss+xml'
        // RegEx from http://stackoverflow.com/questions/632475/regex-to-pick-commas-outside-of-quotes
        return xPath.replaceAll("(/)(?=\\w(?:[^']|'[^']*')*$)", "/xhtml:");
    }

    public static List<Node> getNodes(Document document, String xPath) {
        if (document == null || xPath == null || xPath.length() == 0) {
            return new ArrayList<Node>();
        }
        xPath = addNameSpaceToXPath(document, xPath);

        return getNodes(document.getLastChild(), xPath);
    }

    // TODO uppercase xPath and namespace (xml vs. xhtml!?)
    @SuppressWarnings("unchecked")
    public static List<Node> getNodes(Node node, String xPath) {

        List<Node> nodes = new ArrayList<Node>();
        try {
            // System.out.println(xPath);
            DOMXPath xpathObj = new DOMXPath(xPath);
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
    public static Node getNode(Node node, String xPath) {
        if (node == null) {
            return null;
        }
        Node targetNode = null;
        List<Node> nodeList = getNodes(node, xPath);
        if (nodeList.iterator().hasNext()) {
            targetNode = nodeList.iterator().next();
        }
        return targetNode;
    }

    public static Node getNode(Document doc, String xPath) {
        if (doc == null) {
            return null;
        }
        Node targetNode = null;
        List<Node> nodeList = getNodes(doc, xPath);
        if (nodeList.iterator().hasNext()) {
            targetNode = nodeList.iterator().next();
        }
        return targetNode;
    }

    /*
     * public static Node getChildNodeNS(Node node, String xPath) { if (node == null) return null; Node childNode = null; try { //System.out.println(xPath);
     * DOMXPath xpathObj = new DOMXPath(xPath); xpathObj.addNamespace("xhtml", "http://www.w3.org/1999/xhtml"); List<Node> nodeList =
     * xpathObj.selectNodes(node); if (nodeList.size() > 0) childNode = nodeList.get(0); } catch (JaxenException e) { Logger.getRootLogger().error(xPath + ", "
     * + e.getMessage()); } catch (OutOfMemoryError e) { Logger.getRootLogger().error(xPath + ", " + e.getMessage()); } catch (NullPointerException e) {
     * Logger.getRootLogger().error(xPath + ", " + e.getMessage()); } catch (Error e) { Logger.getRootLogger().error(xPath + ", " + e.getMessage()); } return
     * childNode; }
     */

    public static Node getNodeByID(Document document, String id) {

        Node node = null;
        try {
            List<Node> idNodes = XPathHelper.getNodes(document, "//*[@id='" + id + "']");
            for (int i = 0; i < Math.min(1, idNodes.size()); i++) {
                node = idNodes.get(i).getParentNode();
            }
        } catch (OutOfMemoryError e) {
            Logger.getRootLogger().error(id + ", " + e.getMessage());
        } catch (NullPointerException e) {
            Logger.getRootLogger().error(id + ", " + e.getMessage());
        } catch (Error e) {
            Logger.getRootLogger().error(id + ", " + e.getMessage());
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
    public static Node getChildNode(Node node, String xPath) {
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

        while (childCandidate.getParentNode() != null) {
            childCandidate = childCandidate.getParentNode();
            if (childCandidate.equals(parent)) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public static List<Node> getChildNodes(Node node, String xPath) {
        List<Node> childNodes = null;
        List<Node> childNodesMatch = new ArrayList<Node>();
        try {
            DOMXPath xpathExpression = new DOMXPath(xPath);
            childNodes = xpathExpression.selectNodes(node);

            for (Node cn : childNodes) {
                if (isChildOf(cn, node)) {
                    childNodesMatch.add(cn);
                }
            }

        } catch (JaxenException e) {
            Logger.getRootLogger().error(xPath + ", " + e.getMessage());
        } catch (OutOfMemoryError e) {
            Logger.getRootLogger().error(xPath + ", " + e.getMessage());
        } catch (Exception e) {
            Logger.getRootLogger().error(xPath + ", " + e.getMessage());
        }

        return childNodesMatch;
    }
}