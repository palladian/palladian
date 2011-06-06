package ws.palladian.helper.html;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jaxen.JaxenException;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A helper class for handling XPath queries, depending on Jaxen XPath library.
 * 
 * The methods {@link #getNodes(Node, String, Map)} and {@link #getNode(Node, String, Map)} allow processing of
 * Documents with namespaces. If your XPath expression contains namespace prefixes, like <code>//atom:entry</code>, you
 * must supply the corresponding mapping from prefix to URI as parameter, for
 * example:
 * 
 * <pre>
 * Map&lt;String, String&gt; mapping = new HashMap&lt;String, String&gt;();
 * mapping.put(&quot;atom&quot;, &quot;http://www.w3.org/2005/Atom&quot;);
 * List&lt;Node&gt; nodes = XPathHelper.getNodes(doc, &quot;//atom:entry&quot;, mapping);
 * </pre>
 * 
 * The methods with <i>xhtml</i> in their names, like {@link #getXhtmlNodes(Document, String)}, serve as convenience
 * methods for processing XHTML documents. The prefix for XHTML namespace is inserted automatically, simplifying XHTML
 * XPath queries.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Martin Werner
 * 
 * @see http://jaxen.codehaus.org/
 */
public class XPathHelper {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(XPathHelper.class);

    /**
     * Get a list of {@link Node}s matching the given XPath expression.
     * 
     * @param node The Node or Document to consider, not <code>null</code>.
     * @param xPath The XPath expression, not <code>null</code>.
     * @param namespaces (Optional) Map with namespaces, necessary to bind prefixes in XPath expression to namespaces.
     * @return Matching nodes for the given xPath expression, or an empty List if no nodes match or an error occured.
     */
    @SuppressWarnings("unchecked")
    public static List<Node> getNodes(Node node, String xPath, Map<String, String> namespaces) {
        notNull(node, xPath);

        List<Node> nodes = new ArrayList<Node>();

        try {

            DOMXPath xpathObj = new DOMXPath(xPath);
            SimpleNamespaceContext namespaceContext = new SimpleNamespaceContext();
            namespaceContext.addNamespace("xhtml", "http://www.w3.org/1999/xhtml");
            if (namespaces != null) {
                for (Entry<String, String> entry : namespaces.entrySet()) {
                    String prefix = entry.getKey();
                    String URI = entry.getValue();
                    namespaceContext.addNamespace(prefix, URI);
                }
            }
            xpathObj.setNamespaceContext(namespaceContext);

            nodes = xpathObj.selectNodes(node);

        } catch (JaxenException e) {
            LOGGER.error(xPath + ", " + e.getMessage());
        }

        return nodes;
    }

    /**
     * Get a list of {@link Node}s from matching the given XPath expression.
     * 
     * @param node The Node or Document to consider, not <code>null</code>.
     * @param xPath The XPath expression, not <code>null</code>.
     * @return Matching nodes for the given xPath expression, or an empty List if no nodes match or an error occured.
     */
    public static List<Node> getNodes(Node node, String xPath) {
        notNull(node, xPath);
        return getNodes(node, xPath, null);
    }

    /**
     * Get a {@link Node} matching the given XPath expression.
     * 
     * @param node The Node or Document to consider, not <code>null</code>.
     * @param xPath The XPath expression, not <code>null</code>.
     * @param namespaces (Optional) Map with namespaces, necessary to bind prefixes in XPath expression to namespaces.
     * @return Matching node for the given xPath expression, or <code>null</code> if no matching node or an error
     *         occured.
     */
    public static Node getNode(Node node, String xPath, Map<String, String> namespaces) {
        notNull(node, xPath);

        Node targetNode = null;
        List<Node> nodeList = getNodes(node, xPath, namespaces);
        if (nodeList.iterator().hasNext()) {
            targetNode = nodeList.iterator().next();
        }
        return targetNode;
    }

    /**
     * Get a {@link Node} matching the given XPath expression.
     * 
     * @param node The Node or Document to consider, not <code>null</code>.
     * @param xPath The XPath expression, not <code>null</code>.
     * @return Matching node for the given xPath expression, or <code>null</code> if no matching node or an error
     *         occured.
     */
    public static Node getNode(Node node, String xPath) {
        notNull(node, xPath);
        return getNode(node, xPath, null);
    }

    /**
     * Get the {@link Node} with the specified ID.
     * 
     * @param node The Node or Document to consider, not <code>null</code>.
     * @param nodeId The ID of the Node to return, not <code>null</code>.
     * @return Matching node with the given ID, or <code>null</code> if no matching node.
     */
    public static Node getNodeByID(Node node, String nodeId) {
        notNull(node, nodeId);

        Node result = null;
        List<Node> idNodes = XPathHelper.getNodes(node, "//*[@id='" + nodeId + "']");
        for (int i = 0; i < Math.min(1, idNodes.size()); i++) {
            result = idNodes.get(i);
        }

        return result;
    }

    /**
     * Get the parent of the {@link Node} with the specified ID.
     * 
     * @param node The Node or Document to consider, not <code>null</code>.
     * @param nodeId The ID of the Node to return, not <code>null</code>.
     * @return Parent of the node with the given ID, or <code>null</code> if no matching node.
     */
    public static Node getParentNodeByID(Node node, String nodeId) {
        notNull(node, nodeId);

        Node result = null;
        Node childNode = getNodeByID(node, nodeId);
        if (childNode != null) {
            result = childNode.getParentNode();
        }
        return result;
    }

    /**
     * Gets the child {@link Node}s of a given node that are addressed by the given XPath.
     * 
     * @param node The parent node of the children, not <code>null</code>
     * @param xPath The XPath that addresses the children, not <code>null</code>.
     * @return The child nodes, or an empty List if no matching child nodes.
     */
    public static List<Node> getChildNodes(Node node, String xPath) {
        notNull(node, xPath);

        List<Node> childNodesMatch = new ArrayList<Node>();
        List<Node> childNodes = getNodes(node, xPath);

        for (Node cn : childNodes) {
            if (isChildOf(cn, node)) {
                childNodesMatch.add(cn);
            }
        }

        return childNodesMatch;
    }

    /**
     * Gets the child nodes of a given node.
     * 
     * @param node The parent of the children, not <code>null</code>.
     * @return The child nodes, or an empty List if no matching child nodes.
     */
    public static List<Node> getChildNodes(Node node) {
        notNull(node);

        List<Node> children = new ArrayList<Node>();

        NodeList childNodes = node.getChildNodes();
        if (childNodes != null) {
            for (int x = 0; x < childNodes.getLength(); x++) {
                if (childNodes.item(x).getNodeName() != null && isChildOf(childNodes.item(x), node)) {
                    children.add(childNodes.item(x));
                }
            }
        }

        return children;
    }

    /**
     * Get a child node from the {@link Node} matching the given XPath.
     * 
     * @param node The parent node under which the sought node must descend, not <code>null</code>
     * @param xPath The XPath that points to a node, not <code>null</code>.
     * @return A node that matches the XPath and descends from the given node.
     */
    public static Node getChildNode(Node node, String xPath) {
        notNull(node, xPath);

        List<Node> childNodes = getChildNodes(node, xPath);
        Node childNode = null;
        if (childNodes.iterator().hasNext()) {
            childNode = childNodes.iterator().next();
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
     * Get the text content of a child node with the given XPath expression.
     * 
     * @param node The node whose children are considered, not <code>null</code>.
     * @param xPath The XPath expression addressing the node with the sought text content, not <code>null</code>.
     * @return The Node's text content, or an empty String if node does not exist.
     */
    public static String getNodeTextContent(Node node, String xPath) {
        notNull(node, xPath);

        String textContent = "";
        Node textNode = getNode(node, xPath);
        if (textNode != null) {
            textContent = textNode.getTextContent();
        }

        return textContent;
    }

    /**
     * Get the xPath that points to the parent element of the given xPath.<br>
     * For example: /DIV/P/A => /DIV/P
     * 
     * @param xPath The xPath for which the parent xPath should be found, not <code>null</code>.
     * @return The xPath that points to the parent node of the given xPath.
     */
    public static String getParentXPath(String xPath) {
        notNull(xPath);

        String parentXPath = xPath;

        int i = xPath.lastIndexOf("/");
        if (i > -1) {
            parentXPath = xPath.substring(0, i);
        }

        return parentXPath;
    }

    /**
     * Gets the previous sibling nodes of a node.
     * 
     * @param node the node
     * @return the previous siblings
     */
    public static List<Node> getPreviousSiblings(Node node) {
        notNull(node);

        Node parentNode = node.getParentNode();
        List<Node> previousSiblings = new ArrayList<Node>();
        List<Node> childNodes = XPathHelper.getChildNodes(parentNode);

        for (Node childNode : childNodes) {
            if (childNode.isSameNode(node)) {
                break;
            } else {
                previousSiblings.add(childNode);
            }
        }
        return previousSiblings;
    }

    // //////////////////////////////////////////////////
    // convenience methods for XHTML XPaths
    // TODO auto-uppercase?
    // //////////////////////////////////////////////////

    public static List<Node> getXhtmlNodes(Document document, String xPath) {
        notNull(document, xPath);
        return getNodes(document.getLastChild(), addXhtmlNsToXPath(document, xPath));

    }

    public static Node getXhtmlNode(Document doc, String xPath) {
        notNull(doc, xPath);
        return getNode(doc, addXhtmlNsToXPath(xPath));
    }

    public static List<Node> getXhtmlChildNodes(Node node, String xPath) {
        notNull(node, xPath);
        return getChildNodes(node, addXhtmlNsToXPath(xPath));

    }

    public static Node getXhtmlChildNode(Node node, String xPath) {
        notNull(node, xPath);

        List<Node> childNodes = getXhtmlChildNodes(node, xPath);
        Node childNode = null;
        Iterator<Node> iterator = childNodes.iterator();
        if (iterator.hasNext()) {
            childNode = iterator.next();
        }
        return childNode;
    }

    /**
     * Check whether document has an xhtml namespace declared.
     * 
     * @param document The document.
     * @return True if the document has an xhtml namespace declared, else false.
     */
    public static boolean hasXhtmlNs(Document document) {
        notNull(document);

        boolean result = false;
        Node node = null;
        if (document.getLastChild() != null && document.getLastChild().getAttributes() != null) {
            node = document.getLastChild().getAttributes().getNamedItem("xmlns");
        }

        if (node != null && node.getTextContent().toLowerCase().indexOf("xhtml") > -1) {
            result = true;
        }
        return result;
    }

    /**
     * Add the xhtml namespace to an xPath.
     * 
     * @param document The document.
     * @param xPath The xPath.
     * @return The xPath with the namespace.
     */
    public static String addXhtmlNsToXPath(Document document, String xPath) {
        notNull(document, xPath);

        String returnValue = xPath;
        if (hasXhtmlNs(document)) {
            returnValue = addXhtmlNsToXPath(xPath);
        }
        return returnValue;
    }

    /**
     * Add the xhtml namespace to an xPath in case it does not have it yet.
     * 
     * 
     * @param xPath The xPath.
     * @return The xPath with included xhtml namespace.
     */
    public static String addXhtmlNsToXPath(String xPath) {
        notNull(xPath);

        if (xPath.toLowerCase(Locale.ENGLISH).indexOf("xhtml:") > -1) {
            return xPath;
        }
        // return xPath.replaceAll("/(?=\\w)","/xhtml:");
        // this is a fix NOT to touch slashes inside quotes,
        // for example in @type='application/rss+xml'
        // RegEx from http://stackoverflow.com/questions/632475/regex-to-pick-commas-outside-of-quotes
        // return xPath.replaceAll("(/)(?=\\w(?:[^']|'[^']*')*$)", "/xhtml:");
        return xPath.replaceAll("(/)(?=\\w+(\\[|\\/|$)(?:[^']|'[^']*')*$)", "/xhtml:");

    }

    /**
     * Ensure that the given arguments are not <code>null</code>, otherwise throw a {@link NullPointerException}.
     * 
     * @param args
     */
    private static void notNull(Object... args) {
        for (Object arg : args) {
            if (arg == null) {
                throw new NullPointerException("parameter must not be null");
            }
        }
    }

}