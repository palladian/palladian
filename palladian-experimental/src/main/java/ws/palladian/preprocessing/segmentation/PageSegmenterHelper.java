package ws.palladian.preprocessing.segmentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * @author Philipp Katz
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
    public static Document transformNodeToDocument(Node node) {
        Element element = (Element) node;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Encountered ParserConfigurationException: " + e.getMessage(), e);
        }
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
    
  /**
  * <p>
  * Lists all tags. Deletes arguments within the tags, if there are any.
  * </p>
  *
  * @param htmlText The html text.
  * @return A list of tags.
  */
 public static List<String> listTags(String htmlText) {
     List<String> tags = new ArrayList<String>();

     List<String> lev = new ArrayList<String>();
     String currentTag = "";

     Pattern pattern = Pattern.compile("(\\<.*?>)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
     Matcher matcher = pattern.matcher(htmlText);

     while (matcher.find()) {
         currentTag = matcher.group();

         // Delete arguments within the tags
         if (currentTag.contains(" ")) {
             currentTag = currentTag.substring(0, currentTag.indexOf(" ")) + ">";

             // System.out.print("+++++++++++++++++++"+currentTag);

             if (currentTag.contains("<!") || currentTag.contains("<html") || currentTag.contains("<head")
                     || currentTag.contains("<title") || currentTag.contains("<body") /*
                                                                                       * ||
                                                                                       * currentTag.contains("meta_name"
                                                                                       * )
                                                                                       */) {
                 continue;
             }

             // if (currentTag.contains("http") || currentTag.contains("span") || currentTag.contains("href")) {
             // currentTag=currentTag.substring(0, currentTag.indexOf(" "))+">";
             // }
             //
             // if (currentTag.contains("id=")) {
             // currentTag=currentTag.substring(0, currentTag.indexOf("id=")-1).concat(
             // currentTag.substring(currentTag.indexOf("\"",currentTag.indexOf("id=")+4)+1,
             // currentTag.indexOf(">")+1));
             // }
             //
             // if (currentTag.contains("name=")) {
             // currentTag=currentTag.substring(0, currentTag.indexOf("name=")-1).concat(
             // currentTag.substring(currentTag.indexOf("\"",currentTag.indexOf("name=")+6)+1,
             // currentTag.indexOf(">")+1));
             // }
             //
             //
             //
             // if (currentTag.substring(0,2).equals("<i")) currentTag="<img>";
             // if (currentTag.substring(0,2).equals("<a")) currentTag="<a>";
             // if (currentTag.contains("<div class")) currentTag="<div>";
             // if (currentTag.contains("<meta ")) currentTag="<meta>";
             //
             //
             //
             // //System.out.println(" ersetzt zu "+currentTag);
             //
             //
             // currentTag=currentTag.replaceAll(" ", "_");
         }

         /*
          * Versuch die aktuelle Ebene einzubeziehen - fehlgeschlagen, nicht brauchbar
          * if (!currentTag.contains("/")) level++;
          * tags.add(level+currentTag);
          * if (currentTag.contains("/")) level--;
          * tags.add(level+currentTag);
          */

         if (!lev.contains(currentTag)) {
             // System.out.println(currentTag+"..."+lev);

             lev.add(currentTag);
             // lev2.add("1"+"o"+currentTag);
             // currentTag="1"+"o"+currentTag;

         }

         tags.add(currentTag);
     }

     return tags;
 }

}
