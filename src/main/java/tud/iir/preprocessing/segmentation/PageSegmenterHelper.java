package tud.iir.preprocessing.segmentation;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import tud.iir.web.Crawler;

/**
 * The PageSegmenterHelper provides some helper functions to handle the class PageSegmenter.
 * 
 * @author Silvio Rabe
 *
 */
public class PageSegmenterHelper {

	/**
	 * Converts a list of values to a map with list entities as keys to their quantity. 
	 * Therefore it counts the amount of duplicate entities for each entity.
	 * 
	 * @param values A list of values.
	 * @return A map of values combined with their respective quantity.
	 */
    public static Map<String, Integer> convertListToMap(List<String> values) {
    	Map<String, Integer> map = new LinkedHashMap<String, Integer>();

        for (int i = 0; i < values.size(); i++) {
			if (!map.keySet().contains(values.get(i).toString())) {
				map.put(values.get(i).toString(), 1);
				//System.out.println(values.get(i).toString()+" neu eingefügt!");
			} else {
				map.put(values.get(i).toString(), map.get(values.get(i).toString()) + 1);
				//System.out.println(values.get(i).toString()+" schon "+map.get(values.get(i).toString())+"x drin");

			}
		}
        //map=sortMapByValue(map);
        //System.out.println("MAP "+sortByValues(map));    	
    	
    	return map;
    }
    
    /**
     * Sorts a map by its double values.
     * 
     * @param map A map of keys combined with their respective quantities as values. 
     * @return A map sorted by its values.
     */
    public static Map<String, Double> sortMapByDoubleValues(final Map<String, Double> map) {
    	Comparator<String> valueComparator =  new Comparator<String>() {
    	    public int compare(String k1, String k2) {
    	        int compare = map.get(k2).compareTo(map.get(k1));
    	        if (compare == 0) return 1;
    	        else return compare;
    	    }
    	};

    	Map<String, Double> sortedByValues = new TreeMap<String, Double>(valueComparator);
    	sortedByValues.putAll(map);
    	return sortedByValues;
    }
    
    /**
     * Sorts a map by its integer values.
     * 
     * @param map A map of keys combined with their respective quantities as values. 
     * @return A map sorted by its values.
     */
    public static Map<String, Integer> sortMapByIntegerValues(final Map<String, Integer> map) {
    	Comparator<String> valueComparator =  new Comparator<String>() {
    	    public int compare(String k1, String k2) {
    	        int compare = map.get(k2).compareTo(map.get(k1));
    	        if (compare == 0) return 1;
    	        else return compare;
    	    }
    	};

    	Map<String, Integer> sortedByValues = new TreeMap<String, Integer>(valueComparator);
    	sortedByValues.putAll(map);
    	return sortedByValues;
    }

    /**
     * Limits a map in size.
     * 
     * @param map The map to limit.
     * @param number The size limit. 
     * @return A size limited map.
     */
    public static Map<String, Integer> limitMap(Map<String, Integer> map, int number) {
    	
    	Map<String, Integer> result=new TreeMap<String, Integer>();
    	
    	int limit=0;
    	if (map.size()<number) limit=map.size();
    	else limit=number;
    	
        for (int i = 0; i < limit/*map.size()*//*count*/; i++) {
        	//result=result+map.values().toArray()[i]+"x "+map.keySet().toArray()[i]+"\n";
        	result.put((String)map.keySet().toArray()[i], (Integer)map.values().toArray()[i]);
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
    	int level=0;
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
        
        //System.out.println(element.getFirstChild().getNodeName()+"-------------");
        Node dup = doc.importNode(element, true);
        doc.appendChild(dup);
        
        return doc;
    }

    /**
     * Reads the files of a specific folder and returns it as a list of files.
     * 
     * @param place The folder of the files to read.
     * @return A list of files.
     */
    public static File[] readURLsFromDisc(String place) {
    	
    	File maindir = new File(place);
        File files[] = maindir.listFiles(new FilenameFilter(){
            public boolean accept(File dir, String name){
                return name.endsWith(".html");}
            });
        
        System.out.println("files("+files.length+"):----------\n"+files[0]);
    	
        return files;
    }
    
    /**
     * Help function to get the source code of an URL as string.
     * 
     * @param URL The URL to get the source code from.
     * @return The source code of the URL as string. 
     */
    public static String getContentOfURL(String URL){
    	Crawler c = new Crawler();
        Document d = c.getWebDocument(URL);

    	String dText=c.documentToString(d);
    	//System.out.println("content:-------------\n"+dText);
    	
    	return dText;
    }

    /**
     * Gets the label of an URL. The label is assumed as the first separate string after the 
     * domain within the URL.
     * 
     * @param title The URL as string
     * @return The label of the URL
     */
    public static String getLabelOfURL(String title) {
    	String label="";
		Crawler c = new Crawler();
        String domain = c.getDomain(title);
        title=c.getCleanURL(title);
    	//System.out.println("Domain: "+domain+"\nTitle: "+title+"\nLabel: "+label);
    	label=title.replace(c.getCleanURL(domain), "");
    	title=title.replace("/","_");

    	title=title.replaceAll("[[^\\w\\däüöÄÜÖ\\+\\- ]]", "_");
    	label=label.replaceAll("[[^\\w\\däüöÄÜÖ\\+\\- ]]", "_");

    	//System.out.println("Label: "+label);
    	
    	if (label.length()>3 && label.indexOf("_",0)!=label.lastIndexOf("_")) label=label.substring(label.indexOf("_", 0)+1, label.indexOf("_", 2));
    	else label=label.substring(label.indexOf("_", 0)+1, label.length());

    	//System.out.println("Domain: "+c.getCleanURL(domain)+"\nTitle: "+title+"\nLabel: "+label+"\n-----------------------");
    	return label;
    }

	
	
	
}
