package ws.palladian.retrieval;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CollectionHelper.Order;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * A set of xPaths.
 * </p>
 * 
 * @author David Urbansky
 */
public class XPathSet {
    
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(XPathSet.class);

    private Map<String, Integer> xPathMap = null;

    public XPathSet() {
        xPathMap = new LinkedHashMap<String, Integer>();
    }

    public Map<String, Integer> getXPathMap() {
        return xPathMap;
    }

    public void add(Set<String> xPaths) {
        for (String xPath : xPaths) {
            add(xPath);
        }
    }

    public void remove(String xPath) {
        xPathMap.remove(xPath);
    }
    public void add(String xPath) {
        if (xPathMap.containsKey(xPath)) {
            int count = xPathMap.get(xPath);
            xPathMap.put(xPath, count + 1);
        } else {
            xPathMap.put(xPath, 1);
        }
    }

    public void addEntry(Map.Entry<String, Integer> entry) {
        xPathMap.put(entry.getKey(), entry.getValue());
    }

    public int getCountOfXPath(String xPath) {
        int count = 0;
        if (xPathMap.containsKey(xPath)) {
            count = xPathMap.get(xPath);
        }
        return count;
    }

    public String getHighestCountXPath() {
        return getHighestCountXPath(0);
    }

    public String getHighestCountXPath(int minCount) {
        xPathMap = CollectionHelper.sortByValue(xPathMap, Order.DESCENDING);

        // Iterator<Map.Entry<String, Integer>> it = xPathMap.entrySet().iterator();
        // while (it.hasNext()) {
        // Map.Entry<String, Integer> entry = it.next();
        // // System.out.println(entry.getKey().toLowerCase()+" "+entry.getValue());
        // }

        if (xPathMap.entrySet().iterator().hasNext()) {
            Map.Entry<String, Integer> entry = xPathMap.entrySet().iterator().next();
            if (entry.getValue() >= minCount) {
                return entry.getKey();
            }
        }

        return "";
    }

    public String getLongestXPath() {
        String longestPath = "";

        for (String xPath : xPathMap.keySet()) {
            if (xPath.length() > longestPath.length()) {
                longestPath = xPath;
            }
        }

        return longestPath;
    }

    /**
     * Return the longest (or highest priority) path that contains the highest count path as a substring. TODO b/a = a/b
     * (website1.html)
     * 
     * @return The longest xPath with the highest count.
     */
    public String getLongestHighCountXPath(Document document) {
        String longestHighCountXPath = "";

        xPathMap = CollectionHelper.sortByValue(xPathMap, Order.DESCENDING);

        Iterator<Map.Entry<String, Integer>> it = xPathMap.entrySet().iterator();
        String highestHitCountXPath = "";
        int highestHitCount = 0;
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            String xPath = entry.getKey();

            if (longestHighCountXPath.length() == 0) {
                longestHighCountXPath = xPath;
                highestHitCountXPath = xPath;
                highestHitCount = entry.getValue();
                continue;
            }

            if (xPath.indexOf(highestHitCountXPath) > -1 && xPath.length() > longestHighCountXPath.length()
                    && entry.getValue() > highestHitCount / 6) {
                longestHighCountXPath = xPath;
            } else {
                break;
            }
        }

        // child stages after highest count xPath
        String[] stagesArray = longestHighCountXPath.replace(highestHitCountXPath, "").split("/");
        int stages = stagesArray.length - 1;

        LOGGER.debug(
                "longest high count: {} stages: {}", longestHighCountXPath.toLowerCase(), stages);

        // check whether there is text content at the specified path, otherwise move stages up until
        // text content is found or the highest count xpath is reached
        while (stages > 0) {
            // System.out.println(longestHighCountXPath+": "+pa.getTextByXpath(document, longestHighCountXPath));
            if (StringHelper.trim(PageAnalyzer.getTextByXPath(document, longestHighCountXPath)).length() > 0) {
                break;
            }
            longestHighCountXPath = PageAnalyzer.getParentNode(longestHighCountXPath);
            stages--;
        }

        LOGGER.debug("node with content: {}", longestHighCountXPath.toLowerCase());

        return longestHighCountXPath;
    }

    public boolean isEmpty() {
        return xPathMap.isEmpty();
    }
}