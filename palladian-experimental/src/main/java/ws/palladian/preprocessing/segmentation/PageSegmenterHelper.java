package ws.palladian.preprocessing.segmentation;

import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;

/**
 * The PageSegmenterHelper provides some helper functions to handle the class PageSegmenter.
 * 
 * @author Silvio Rabe
 * 
 */
public class PageSegmenterHelper {

    /**
     * Limits a map in size.
     * 
     * @param map The map to limit.
     * @param number The size limit.
     * @return A size limited map.
     */
    public static Map<String, Integer> limitMap(Map<String, Integer> map, int number) {

        Map<String, Integer> result = new TreeMap<String, Integer>();

        int limit = 0;
        if (map.size() < number) {
            limit = map.size();
        } else {
            limit = number;
        }

        for (int i = 0; i < limit; i++) {
            result.put((String) map.keySet().toArray()[i], (Integer) map.values().toArray()[i]);
        }
        return result;
    }

    /**
     * Returns the depth of a specific node in a dom tree.
     * 
     * @param node The node to check.
     * @return The depth of the node in its dom tree.
     */
    public static int getNodeLevel(Node node) {
        int level = 0;
        if (node == null) {
            return 0;
        }
        while (node.getParentNode() != null) {
            node = node.getParentNode();
            level++;
        }
        return level;
    }

    /**
     * Transforms a single node(and its subtree) to document type.
     * 
     * @param node The node to transform.
     * @return The node as document.
     */
    public static Document transformNodeToDocument(Node node) throws ParserConfigurationException {
        Element element = (Element) node;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Node dup = doc.importNode(element, true);
        doc.appendChild(dup);

        return doc;
    }

    /**
     * Gets the label of an URL. The label is assumed as the first separate string after the
     * domain within the URL.
     * 
     * @param title The URL as string
     * @return The label of the URL
     */
    public static String getLabelOfURL(String title) {
        String label = "";
        String domain = UrlHelper.getDomain(title);
        title = UrlHelper.getCleanUrl(title);
        label = title.replace(UrlHelper.getCleanUrl(domain), "");
        title = title.replace("/", "_");

        title = title.replaceAll("[[^\\w\\däüöÄÜÖ\\+\\- ]]", "_");
        label = label.replaceAll("[[^\\w\\däüöÄÜÖ\\+\\- ]]", "_");

        if (label.length() > 3 && label.indexOf("_", 0) != label.lastIndexOf("_")) {
            label = label.substring(label.indexOf("_", 0) + 1, label.indexOf("_", 2));
        } else {
            label = label.substring(label.indexOf("_", 0) + 1, label.length());
        }

        return label;
    }

}
