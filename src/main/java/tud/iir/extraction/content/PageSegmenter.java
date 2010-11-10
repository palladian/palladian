package tud.iir.extraction.content;

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
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import tud.iir.extraction.PageAnalyzer;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.Tokenizer;
import tud.iir.helper.XPathHelper;
import tud.iir.web.Crawler;

/**
 * The PageSegmenter segments a given URL into independent parts and rates the importance for each part.
 * 
 * @author Silvio Rabe
 *
 */
public class PageSegmenter {
	
    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(PageSegmenter.class);
    
    /** configs for the crawler can be set in config/segmengter.conf */
    private PropertiesConfiguration config = null;

    /** the document to use */
    private Document document = null;

    /** the location to store the colored result */
    private String storeLocation = "";
    
    /** a list of all segments */
    private ArrayList<Segment> segments = null;
    
    /** a map of similar files */
    private Map<String, Double> similarFiles = null;
    
    // ///////////////////// important values ///////////////////////

    private static int lengthOfQGrams = 0; 
    
    private static int amountOfQGrams = 0;
    
    private static double similarityNeed = 0;
    
    private static int maxDepth = 0;
    
    private static int numberOfSimilarDocuments = 0;

    // ///////////////////// setter methods ///////////////////////
    
    public void setDocument(Document document) {
        this.document = document;
    }

    public void setDocument(String url) {
        Crawler c = new Crawler();
        this.document = c.getWebDocument(url);
    }
    
	public void setStoreLocation(String storeLocation) {
		this.storeLocation = storeLocation;
	}
	
    // ///////////////////// constructors ///////////////////////
	
    public PageSegmenter() {
        initialize("config/segmenter.conf");
    }
    
	public PageSegmenter(Document document) {
		this(document, "");
	}
	
	public PageSegmenter(Document document, String storeLocation){
        initialize("config/segmenter.conf");
		this.document = document;
		this.storeLocation = storeLocation;
	}
	

   private void initialize(String configPath) {
        loadConfig(configPath);
    }

    /**
     * Load the configuration file from the specified location and set the variables accordingly.
     * 
     * @param configPath The location of the configuration file.
     */
    public final void loadConfig(String configPath) {
        try {
            config = new PropertiesConfiguration(configPath);
            
            this.lengthOfQGrams = config.getInt("lengthOfQGrams");
            this.amountOfQGrams = config.getInt("amountOfQGrams");
            this.similarityNeed = config.getDouble("similarityNeed");
            this.maxDepth = config.getInt("maxDepth");
            this.numberOfSimilarDocuments = config.getInt("numberOfSimilarDocuments");

        } catch (ConfigurationException e) {
            LOGGER.warn("PageSegmenter configuration under " + configPath + " could not be loaded completely: "
                    + e.getMessage());
        }
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
	 * Returns only segments specified by color. 
	 * 
	 * @param color The color of segments to return. E.g. "Segment.Color.RED"
	 * @return A lsit of Segments.
	 */
	public ArrayList<Segment> getSpecificSegments(Segment.Color color) {
		ArrayList<Segment> allSegs = new ArrayList<Segment>();
		
		for (int i=0; i<segments.size(); i++) {
			Segment seg = segments.get(i);
			System.out.println(seg.getColor());
			if (seg.getColor()==color) {
				System.out.println("TREFFER! DAS IST "+color);
				allSegs.add(seg);
			}
		}
		
		return allSegs;
	}
	
	public ArrayList<Segment> getSpecificSegments(double beginValue, double endValue) {
		ArrayList<Segment> allSegs = new ArrayList<Segment>();
		
		for (int i=0; i<segments.size(); i++) {
			Segment seg = segments.get(i);
			System.out.println(seg.getSignificance());
			if (seg.getSignificance()>=beginValue && seg.getSignificance()<=endValue) {
				System.out.println("TREFFER! "+seg.getSignificance()+" IST ZWISCHEN "+beginValue+" UND "+endValue);
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
    Map<String, Integer> createFingerprintForURL(String url, int number, int length) throws MalformedURLException, IOException {
    	Map<String, Integer> result=new TreeMap<String, Integer>();
    	
        String dText=PageSegmenterHelper.getContentOfURL(url);
        
        String tagList="";
        Iterator<String> it=HTMLHelper.listTags(dText).iterator();
        while (it.hasNext()) {
        	//System.out.println(it.next());
            tagList=tagList+" "+it.next();
        }
        //System.out.println(tagList);
        
        String testString=tagList;
        
        List<String> setOfTags=new ArrayList<String>(Tokenizer.calculateWordNGrams(testString, length));
        List<String> listOfTags=new ArrayList<String>(Tokenizer.calculateWordNGramsAsList(testString, length));          
        List<Integer> numberOfTags=new ArrayList<Integer>();

        
        //System.out.println(setOfTags);
        //System.out.println(listOfTags);
        
        //count number of same n-grams
        List<String> helperList = new ArrayList<String>(listOfTags);
        it=setOfTags.iterator();
        String currentElement="";
        int count=0;
        while (it.hasNext()) {
        	currentElement=it.next().toString();
        	while (helperList.contains(currentElement)) {
        		count++;
        		helperList.remove(currentElement);
        	}
        	numberOfTags.add(count);
        	count=0;
        }
        //System.out.println(numberOfTags);
        
        /*for (int i = 0; i < setOfTags.size(); i++) {
        	System.out.println(numberOfTags.get(i)+"x "+setOfTags.get(i));
        }*/
        
        Map<String, Integer> mapOfTags=PageSegmenterHelper.convertListToMap(listOfTags);
        mapOfTags=PageSegmenterHelper.sortMapByIntegerValues(mapOfTags);
        //System.out.println(mapOfTags);
        
        Map<String, Integer> testMap = PageSegmenterHelper.limitMap(mapOfTags, number);
        //System.out.println(testMap);

    	
    	
    	result=testMap;
    	
    	return result;
    }

    /**
     * Find similar files for an given URL.
     * 
     * @param URL The URL to search similar files for.
     * @param similarityNeed Defines how much similarity is needed to be similar. Value between 0 and 1, e.g. 0.88)
     * @param limit The maximum of similar files to find.
     * 
     * @return A list of files similar to the given URL.
     */
    private Map<String, Double> findSimilarFiles(String URL, int qgramNumber, int qgramLength, double similarityNeed, int limit) throws MalformedURLException, IOException {
    	Map<String, Double> result=new LinkedHashMap<String, Double>();
    	
        /*
         * Funktion, die aus den gesammelten Links eine nach der anderen herausnimmt
         * mit Ausgangsdokument vergleicht
         * und bei großer Übereinstimmung in einer Liste speichert (z.B. 5 Stück)
         * 
         */
    	
    	System.out.println("Ausgangs-URL: "+URL);
    	
    	Crawler c = new Crawler();
    	Document d = c.getWebDocument(URL);
    	//System.out.println("1111 "+HTMLHelper.htmlToString(d));
    	String domain = c.getDomain(URL);
    	Document d2 = c.getWebDocument(domain);
    	
    	//Counts and lists all internal URLs
        HashSet<String> te = new HashSet<String>();
		te.addAll(c.getLinks(d,true, false,""));
		System.out.println(te.size()+" intern verlinkte URLs gefunden!");
        System.out.println(te);
        te.addAll(c.getLinks(d2,true, false,""));
		System.out.println(te.size()+" insgesamt (mit Links der Domain("+c.getLinks(d2,true, false,"").size()+"))");
        System.out.println(te);
        te.remove(URL);

    	/**TODO Vorfiltern anhand des URL-Teilstrings */
        String label=PageSegmenterHelper.getLabelOfURL(URL);
        System.out.println("label: "+label);

        List<String> te2 = new ArrayList<String>(te);
        //System.out.println(te2);
        int size=te2.size();
        int counter=0;
        for (int i=0; i<size; i++) {
        	String currentURL=(String)te2.get(i);
            String currentLabel=PageSegmenterHelper.getLabelOfURL(currentURL);
            //System.out.println("currentLabel: "+currentLabel);
            if (!label.equals(currentLabel)) {
            	te2.remove(i); i--;
            	te2.add(size-1, currentURL);
            	//System.out.println("Nicht gleich, wird entfernt. Größe: "+te2.size());
                //System.out.println(te2);
            }
            counter++;
            if (counter==size) break;
        }
        //System.out.println(te2);

        
    	Map<String, Integer> page1 = createFingerprintForURL(URL, qgramNumber, qgramLength);
        
        Iterator<String> it=te2.iterator();
        String currentElement="";
        while (it.hasNext()) {
        	currentElement=(String) it.next();
        	System.out.println(currentElement);
        	
        	Document currentDocument = c.getWebDocument(currentElement);
        	//System.out.println("1111 "+d.getFirstChild().getTextContent());
        	//System.out.println("2222 "+currentDocument.getFirstChild().getTextContent());
        	if ((HTMLHelper.htmlToString(d)).equals(HTMLHelper.htmlToString(currentDocument))) {
        		System.out.println("#####################################################");
        		continue;
        	}

        	//System.out.println(getStringDistance(currentElement, URL));
        	
            Map<String, Integer> page2 = createFingerprintForURL(currentElement, qgramNumber, qgramLength);
            
            //System.out.println(page1);
            //System.out.println(page2);
            
            Double vari = SimilarityCalculator.calculateSimilarity(page1, page2);
            //vari=0.9999999999;
            Double jacc = SimilarityCalculator.calculateJaccard(page1, page2);

            
            String variString=(((Double)((1-vari)*100)).toString());
            variString=variString.substring(0, Math.min(5,variString.length()));
            
            double erg = (1-vari+jacc)/2;
            System.out.println(erg);
            
//            //test without function calculateSimilarity
//            Double jacc = calculateJaccard(page1, page2);
//            Double erg=jacc;
//            String variString="";
            
            //similarityNeed=0.82;
            if (erg>=similarityNeed && erg<1.0) {
            	result.put(currentElement, erg);
            	System.out.println("Seiten verwenden wahrscheinlich dasselbe Template. ("+variString+"%, Jaccard="+jacc+")----------"+result.size());
            	
            }
            else System.out.println("Unterschied zu groß. Seiten verwenden wahrscheinlich nicht dasselbe Template. ("+variString+"%, Jaccard="+jacc+")");
            
            System.out.println("----------------------------------------------------------------------------------------");
            
            
          
        	if (result.size()>=limit) {
        		System.out.println("---Erg.: "+result);
        		break;
        	}
        }
        
        /**TODO delete*/
        //result.put("http://www.informatikforum.de/forumdisplay.php?f=51",0.999);
        
        result=PageSegmenterHelper.sortMapByDoubleValues(result);
        //System.out.println("Map1: "+result);
        //Map m = CollectionHelper.sortByValue(result, true ) ;
        //System.out.println("Map2: "+m);
        
//        Iterator it2 = result.entrySet().iterator();
//        while (it2.hasNext()) {
//            Map.Entry pairs = (Map.Entry)it2.next();
//            System.out.println(pairs.getKey()+" = "+pairs.getValue());
//        }

    	return result;
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
     * @return A list of two ArrayLists. Conflict-nodes and non-conflict-nodes.
     */
    private ArrayList<ArrayList<String>>[] compareDocuments(Document document1, Document document2, ArrayList<String> conflictNodes, ArrayList<String> nonConflictNodes, int level, String xPath) throws ParserConfigurationException {
        NodeList helpList1 = document1.getFirstChild().getChildNodes();
        NodeList helpList2 = document2.getFirstChild().getChildNodes();
        
        PageAnalyzer pa = new PageAnalyzer();
                
        for (int i=0; i<helpList1.getLength(); i++) {
        	Node n1 = (Node)helpList1.item(i);
	        //Node n2 = (Node)helpList2.item(i);

        	Node n2 = n1.cloneNode(true);
        	n2.setNodeValue("###");
        	n2.setTextContent("#####");
        	
//            for (int i2=0; i2<3; i2++) {
//            	if (helpList2.getLength()>(i+i2)) { 
//            		n2=(Node)(helpList2.item(i+i2));
//            		if (n1.getNodeType()==n2.getNodeType()) break;
//            	}
//            }
            if (helpList2.getLength()>(i)) n2=(Node)(helpList2.item(i));
            
	        //System.out.println(n1.getNodeName()+"-"+n2.getNodeName());
	        
	        String constructXPath = pa.constructXPath(n1);
	        //System.out.println(constructXPath);
	        
	        //delete nodename of itself from the beginning of the path
	        if (constructXPath.contains("/")) {
	        	constructXPath = constructXPath.substring(constructXPath.indexOf("/")+1,constructXPath.length());
	        }
	        if (!constructXPath.contains("/")) constructXPath="";
	        if (constructXPath.contains("/")) {
	        	constructXPath = constructXPath.substring(constructXPath.indexOf("/"),constructXPath.length());
	        }
	        //doesn't work for #comment #ect
	        if (constructXPath.contains("#")) constructXPath="";
	        
	        String newXPath = xPath + constructXPath;
	        
	        //System.out.println(newXPath);
	        //System.out.println(n1.getTextContent());
	        
	        if (n1.getTextContent().equals(n2.getTextContent())) {
	        	//System.out.println("gleicher Inhalt");
	        	
	        	if (!nonConflictNodes.contains(newXPath) && !conflictNodes.contains(newXPath)) {	
	        		nonConflictNodes.add(newXPath);
	        	}
	        }
	        else {
	        	//System.out.println("kein gleicher Inhalt");
	        	
	        	if (!conflictNodes.contains(newXPath)) {
	        		conflictNodes.add(newXPath);
	        		nonConflictNodes.remove(newXPath);
	        	}
	        	
	        	if (n1.hasChildNodes() && n2.hasChildNodes()) {
		        	//build new documents from nodes
		            Document doc1=PageSegmenterHelper.transformNodeToDocument(n1);
		            Document doc2=PageSegmenterHelper.transformNodeToDocument(n2);
		            
		            //checkChilden
		            if (level>=0) compareDocuments(doc1, doc2, conflictNodes, nonConflictNodes, level-1, newXPath);
	        	}
	            

	        }
        }
         
    	
    	return new ArrayList[] {conflictNodes, nonConflictNodes};
    }
    
    
    public void colorSegments() {
    	colorSegments(segments);
    }
    
    public void colorSegments(Segment.Color color) {
        ArrayList<Segment> coloredSegments = getSpecificSegments(color);
    	colorSegments(coloredSegments);
    }

    /**
     * Colors the segments of the document based on a comparition with similar documents.
     * Every conflict node will be evaluated in all documents to get a simiarity value. Based
     * on this value a colored border is placed.
     * 
     * @param chosenSegments a list of segments to color.
     */
    public void colorSegments(ArrayList<Segment> chosenSegments) {
    	
        //set colorscale
        String[] colorScale={
        		"#ff0000",
        		"#ff9600",
        		"#ffc800",
        		"#ffff00",
        		"#e6ff00",
        		"#c8ff00",
        		"green"//"#00ff00"
        };
    	
        //insert style attributes in head of the html document
        Element e3 = document.createElement("style");
     	e3.setAttribute("type","text/css");
     	
     	String colorString="\n.myPageSegmenterBorder_dummy_NOTINUSE { border: 2px solid blue; }\n";
     	for (int i=0; i<colorScale.length; i++) {
     		colorString=colorString+".myPageSegmenterBorder"+i+" { border: 2px solid "+colorScale[i]+"; }\n";
     	}
     	
      	Text text = document.createTextNode(colorString);
     	e3.appendChild(text);
     	System.out.println(e3.getTextContent());
     	
     	if (document.getElementsByTagName("style").getLength()!=0) {    	
     		Node styleNode = document.getElementsByTagName("style").item(0);
     		styleNode.appendChild(text);
     		System.out.println("gefunden\n"+styleNode.getTextContent());
     	}
     	else {
     		Node styleNode = document.getElementsByTagName("head").item(0);
         	//System.out.println(styleNode.getNodeName()+"-------------------");
         	styleNode.appendChild(e3);     		
         	System.out.println("nicht gefunden\n"+styleNode.getTextContent());         	
     	}

//     	//color all non-conflict nodes green
//        for (int i=0; i<nonConflictNodes.size(); i++) {
//        	String nonConflictNode = (String) nonConflictNodes.get(i);    
//        	Element e2 = (Element)  XPathHelper.getNode(document, nonConflictNode);
//        	//if (getNodeLevel(e2)<=10) {
//		        String old = e2.getAttribute("class");
//	        	//System.out.println(e2.getNodeType());
//	        	e2.setAttribute("class", "myPageSegmenterBorder6 "+old);
//        	//}
//        	
//        	continue;
//        }
//        
//    	//checks the similarity of all conflict nodes
//        Map<String, Double> similarityOfNodes = SimilarityCalculator.calculateSimilarityForAllNodes(document, conflictNodes, similarFiles);
//        
//        //color all conflict nodes based on their similarity value
//        Iterator it = similarityOfNodes.entrySet().iterator();
//        while (it.hasNext()) {
//            Map.Entry pairs = (Map.Entry)it.next();
//            //System.out.println(pairs.getKey()+" = "+pairs.getValue());
//        	//String conflictNode = (String) conflictNodes.get(i);       	
//        	Element e2 = (Element)  XPathHelper.getNode(document, (String) pairs.getKey());
//	        
//        	//if (getNodeLevel(e2)<=10) {
//	
//	        	String border="";
//	        	double value=(Double) pairs.getValue();
//	        	System.out.println(value);
//	        	
//	        	//if (value<=0.42) {
//	        	
//		        	if (value>=0) border="myPageSegmenterBorder0";
//		        	if (value>0.14) border="myPageSegmenterBorder1";
//		        	if (value>0.28) border="myPageSegmenterBorder2";
//		        	if (value>0.42) border="myPageSegmenterBorder3";
//		        	if (value>0.58) border="myPageSegmenterBorder4";
//		        	if (value>0.72) border="myPageSegmenterBorder5";
//		        	if (value>0.86) border="myPageSegmenterBorder6";
//		        	
//			        String old = e2.getAttribute("class");
//		
//		        	e2.setAttribute("class", border+" "+old);
//	        	//}
//        	//}
//        }
     	
     	
     	//checks the similarity of ALL nodes
     	for (int i=0; i<chosenSegments.size(); i++) {
    		Segment testSeg = chosenSegments.get(i);
	
	    	
	      	Element e2 = (Element)  XPathHelper.getNode(document, testSeg.getXPath());
		        
	      	//if (getNodeLevel(e2)<=10) {
		
		        	String border="";
		        	double value=testSeg.getSignificance();
		        	System.out.println(value);
		        	Segment.Color color = testSeg.getColor();
		        	
		        	//if (value<=0.42) {
		        	
			        	
			        	if (color==Segment.Color.RED) border="myPageSegmenterBorder0";
			        	if (color==Segment.Color.LIGHTRED) border="myPageSegmenterBorder1";
			        	if (color==Segment.Color.REDYELLOW) border="myPageSegmenterBorder2";
			        	if (color==Segment.Color.YELLOW) border="myPageSegmenterBorder3";
			        	if (color==Segment.Color.GREENYELLOW) border="myPageSegmenterBorder4";
			        	if (color==Segment.Color.LIGHTGREEN) border="myPageSegmenterBorder5";
			        	if (color==Segment.Color.GREEN) border="myPageSegmenterBorder6";
			        	
				        String old = e2.getAttribute("class");
			
			        	e2.setAttribute("class", border+" "+old);
		        	//}
	      	//}
     	}
     	
     	/**TODO check if storeLocation is valid*/
        if (storeLocation!="") {
	        //writes the segmented result-document on local disc
	        try {
		        OutputStream os = new FileOutputStream(storeLocation);      
		        OutputFormat format = new OutputFormat(document);
		        XMLSerializer serializer = new XMLSerializer(os, format);
				serializer.serialize(document);
			} catch (IOException e) {
	            LOGGER.error("could not write to local file, " + e.getMessage());
			}        	
        }
        LOGGER.info("------------------------info logger-----------------------");
        LOGGER.warn("------------------------warn logger-----------------------");
        LOGGER.error("------------------------error logger-----------------------");
     	
    }
    
    /**
     * Tries to segment a given URL in conected areas. The result document will be saved
     * on disc.
     * 
     * It is based on the need of another URL with high similarity to the first URL.
     * 
     * @param URL1 The URL of the document to segment.
     * @param URL2 The URL of the helper document with high similarity to URL1.
     * @param outputFile The path where to save the result document.
     * @param level The level(depth)of the tree to check.
     */
    public void startPageSegmentation() throws ParserConfigurationException, IOException {
    	Crawler crawler = new Crawler();
    	
        //Document document1 = crawler.getWebDocument(URL1);

        /**TODO What to do if there are less than 5 similar documents? It would just abort at the moment.*/
        similarFiles = findSimilarFiles(document.getDocumentURI(), amountOfQGrams, lengthOfQGrams, similarityNeed, numberOfSimilarDocuments);
  
        Node bodyNode1=document.getElementsByTagName("body").item(0);
        ArrayList<String> conflictNodes=new ArrayList<String>();
        ArrayList<String> nonConflictNodes=new ArrayList<String>();
        
    	for (int i=0; i<similarFiles.size(); i++) {
    		System.out.println((i+1)+".Runde-----------------------------------------");
    		
            String URL2 = (String) similarFiles.keySet().toArray()[i];
            Document document2 = crawler.getWebDocument(URL2);
	        Node bodyNode2=document2.getElementsByTagName("body").item(0);
	        
	        //build new docs from body nodes
	        Document doc1=PageSegmenterHelper.transformNodeToDocument(bodyNode1);
	        Document doc2=PageSegmenterHelper.transformNodeToDocument(bodyNode2);
	        

	        //returns a list of xpaths of all conflict and a second of all non-conflict nodes of the actual compare
	        ArrayList[] allNodes=compareDocuments(doc1, doc2, new ArrayList<String>(), new ArrayList<String>(), maxDepth, "/HTML/BODY");
	        
	        System.out.println(allNodes[0].size()+"-"+conflictNodes.size()+"="+(allNodes[0].size()-conflictNodes.size())+" zu "+(conflictNodes.size()/100)*35);
	        if ((allNodes[0].size()-conflictNodes.size())<conflictNodes.size()/100*35 || conflictNodes.size()==0) {
		        //adds the new conflict nodes to the list of all conflict nodes
		        for (int j=0; j<allNodes[0].size(); j++){
		        	if (!conflictNodes.contains(allNodes[0].get(j))) conflictNodes.add((String)allNodes[0].get(j));
		        }
		        //adds the new non-conflict nodes to the list of all non-conflict nodes
		        for (int j=0; j<allNodes[1].size(); j++){
		        	if (!nonConflictNodes.contains(allNodes[1].get(j))) nonConflictNodes.add((String)allNodes[1].get(j));
		        }    	
		        System.out.println("Size conflictNodes: "+conflictNodes.size());
		        System.out.println("Size nonConflictNodes: "+nonConflictNodes.size());
	        }
	        else {
	        	System.out.println("Zu viele neue Konflikte. Wahrscheinlich Inkompatibel.");
	            similarFiles.remove(URL2);

	        }
	    }
    	

        //removes all conflictNodes from the list of nonConflictNodes
        for (int i=0; i<conflictNodes.size(); i++) {
        	String n = (String) conflictNodes.get(i);
        	if (nonConflictNodes.contains(n)) {
        		nonConflictNodes.remove(n);
        		System.out.println(n+" gelöscht.");
        	}
        }  
        
        Map<String, Double> conflictNodesIncSim = SimilarityCalculator.calculateSimilarityForAllNodes(document, conflictNodes, similarFiles);
        
        segments = generateListOfSegments(document, conflictNodesIncSim, nonConflictNodes);
           
        //colors the segments
        //colorSegments(document, conflictNodes, nonConflictNodes, similarFiles);
       
        
        System.out.println("Size conflictNodes: "+conflictNodes.size());
        System.out.println("Size nonConflictNodes: "+nonConflictNodes.size());

        //return conflictNodes;
    }

    private ArrayList<Segment> generateListOfSegments(Document document, Map<String, Double> conflictNodes, ArrayList<String> nonConflictNodes) {
    	
    	ArrayList<Segment> allSegments = new ArrayList<Segment>();
    	
    	for (int i=0; i<nonConflictNodes.size(); i++) {
        	String xPath = (String) nonConflictNodes.get(i);    
        	Element node = (Element)  XPathHelper.getNode(document, xPath);
        	Integer depth = PageSegmenterHelper.getNodeLevel(node);
        	
        	Segment seg = new Segment(document, xPath, node, depth, 1.0);
        	allSegments.add(seg);
        }
    	
        Iterator it = conflictNodes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();

        	String xPath = (String) pairs.getKey();
        	Double significance = (Double) pairs.getValue();
        	Element node = (Element)  XPathHelper.getNode(document, xPath);
        	Integer depth = PageSegmenterHelper.getNodeLevel(node);
        	
        	Segment seg = new Segment(document, xPath, node, depth, significance);
        	allSegments.add(seg);
        }
    	
    	//test
//		System.out.println("--------Start ListOfSegments-----------");
//
//    	for (int i=0; i<allSegments.size(); i++) {
//    		Segment testSeg = allSegments.get(i);
//    		System.out.println(testSeg.getNode().getNodeName());
//    		System.out.println(testSeg.getXPath());
//    		System.out.println(testSeg.getSignificance());
//    		System.out.println(testSeg.getDepth());
//    		System.out.println("-------------------------");
//    	}

    	return allSegments;
    }
    
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException, PageContentExtractorException {
		Crawler c = new Crawler();
		String URL="http://www.kalender-365.de";
		URL="http://www.amazon.de";
		//URL="http://www.december.com/html/demo/hello.html";
		URL="http://www.informatik-blog.net/category/c/";
		URL="http://www.informatikforum.de/forumdisplay.php?f=98";
        //URL="C:\\Users\\Silvio\\Documents\\test_2\\x_ausgangsdatei\\informatikforum_de_forumdisplay_php_s_1023ae8d9ac0916d345312192ab1b775_f_98.html";

		
        Document d = c.getWebDocument(URL);
		
		//Counts and lists all internal URLs
        HashSet<String> te = new HashSet<String>();
		te=c.getLinks(d,true, false,"");
		System.out.println(te.size()+" intern verlinkte URLs gefunden!");
        System.out.println(te);
        
        //Counts and lists all tags within that URL        
        String dText=c.documentToString(d);
        System.out.println(HTMLHelper.countTags(dText)+" Tags im Code gefunden!");
        System.out.println(HTMLHelper.listTags(dText));
        
        
        System.out.println("test: "+lengthOfQGrams);
        PageSegmenter seg = new PageSegmenter();
        System.out.println("test: "+lengthOfQGrams+" "+amountOfQGrams+" "+similarityNeed+" "+maxDepth+" "+numberOfSimilarDocuments);

        seg.setDocument(d);
        seg.setStoreLocation("C:\\Users\\Silvio\\Documents\\doc2.html");
        seg.startPageSegmentation();
        
        ArrayList<Segment> allSegments = seg.getAllSegments();
//        for (int i=0; i<10; i++) {
//        	Segment actualSeg = chosenSegments.get(i);
//        	System.out.println(actualSeg.getXPath());
//        	System.out.println(actualSeg.getSignificance());
//        }
        
        ArrayList<Segment> chosenSegments = seg.getSpecificSegments(Segment.Color.LIGHTGREEN);
//        for (int i=0; i<10; i++) {
//        	Segment actualSeg = chosenSegments.get(i);
//        	System.out.println(actualSeg.getXPath());
//        	System.out.println(actualSeg.getSignificance());
//        }
        
        //chosenSegments = seg.getSpecificSegments(0.00, 0.50);
        //seg.colorSegments(chosenSegments);

        //seg.colorSegments(Segment.Color.RED);
        
        seg.colorSegments();
        
        
        
        
        //Gets a node-tree and tries to find the leafs
        //markLeafs(URL, "doc2");
        
        //markLeafs2("http://www.informatik-blog.net/category/c/", "http://www.informatik-blog.net/category/codierung/", "doc2");
        //markLeafs2("http://www.informatikforum.de/forumdisplay.php?f=98", "http://www.informatikforum.de/forumdisplay.php?f=68", "doc2");        
        //startPageSegmenter("http://www.informatik-blog.net/category/c/", "http://www.informatik-blog.net/category/codierung/", "C:\\Users\\Silvio\\Documents\\doc2.html", 3);
        //startPageSegmenter("http://www.informatikforum.de/forumdisplay.php?f=98", "http://www.informatikforum.de/forumdisplay.php?f=68", "C:\\Users\\Silvio\\Documents\\doc2.html", 50);
        //startPageSegmenter("http://www.informatikforum.de/showthread.php?t=163305", "http://www.informatikforum.de/showthread.php?t=163234", "C:\\Users\\Silvio\\Documents\\doc2.html", 30);
        //startPageSegmenter("http://www.amazon.de/Pegasus-Spiele-17186G-Munchkin-Kuhthulus/dp/3939794627/ref=pd_bxgy_toy_img_a", "http://www.amazon.de/Heidelberger-Spieleverlag-HEI00058-Herr-Meister/dp/B000EWPC84/ref=pd_sim_toy_56", "C:\\Users\\Silvio\\Documents\\doc2.html", 13);
        //startPageSegmenter("http://www.dirks-computerseite.de/category/internet/", "http://www.dirks-computerseite.de/category/netzwerk/", "C:\\Users\\Silvio\\Documents\\doc2.html", 30);
        //startPageSegmenter("http://www.informatikforum.de/showthread.php?t=159299", "http://www.informatikforum.de/showthread.php?t=159299", "C:\\Users\\Silvio\\Documents\\doc2.html", 7);
 
        
        
        String domain = c.getDomain(URL);
        //System.out.println("Domain: "+domain);
        
        
        //Document d2 = c.getWebDocument("C:\\Users\\Silvio\\Documents\\doc2.html");

        Node n = d.getElementsByTagName("tr").item(0);
        PageAnalyzer pa = new PageAnalyzer();
        String st1 = pa.constructXPath(n);
        String st2 = "";//getXPath(n);
        //System.out.println(st1+"\n"+st2);
        String ta1 = pa.getTargetNode(st1);
        String ta2 = pa.getTargetNode(st2);
        //System.out.println(ta1+"\n"+ta2);
        
        XPathFactory factory=XPathFactory.newInstance();
        XPath xPath=factory.newXPath();
        
        //File xmlDocument = new File("C:\\Users\\Silvio\\Documents\\doc2.html");
        InputSource inputSource = new InputSource(URL);//new FileInputStream(xmlDocument));
        
        //try {xPath.evaluate("/HTML/BODY", inputSource, XPathConstants.NODESET);} 
        //catch (XPathExpressionException e) {e.printStackTrace();}
        
        //Element noElem = DomUtil.getElement(n, "no");
        
        //Node n2 = XPathAPI.selectSingleNode(d.getFirstChild(), st1);
        
        List nodeList = XPathHelper.getNodes(d, st1);
        Node n2 = (Node) nodeList.get(0);
        //System.out.println(n2.getFirstChild().getNodeName());
        
        
        //saveURLToDisc(URL, "testing");
        //saveAllURLsToDisc(URL, te.size());
        //saveChosenURLsToDisc();
        
        //File files[] = readURLsFromDisc("C:\\Users\\Silvio\\Documents\\test\\aehnlich");
        //getContentOfURL(files[0].toString());
        
        //performParameterCheckForGivenValues(URL, "C:\\Users\\Silvio\\Documents\\test\\aehnlich\\", 50, 3);
        //performParameterCheck(URL, "C:\\Users\\Silvio\\Documents\\test_2\\unaehnlich\\", new int[]{50,100,500,5000}, new int[]{8,9,10}, false);
        //performParameterCheck(URL, "C:\\Users\\Silvio\\Documents\\test_2\\aehnlich2\\", new int[]{10000}, new int[]{2,3,4,5,6,7,8,9,10}, false);
        

	    System.out.println("-------------------------------");
        //System.out.println(createFingerprintForURL(URL));
        
        String testURL1="http://www.informatikforum.de/forumdisplay.php?f=39";
        String testURL2="http://www.informatikforum.de/forumdisplay.php?f=98";
        testURL1="http://www.amazon.de";
        testURL2="http://www.google.de";
        //testURL1="http://www.amazon.de/Pegasus-Spiele-17186G-Munchkin-Kuhthulus/dp/3939794627/ref=pd_bxgy_toy_img_a";
        //testURL2="http://www.amazon.de/Shadows-Teenage-Fanclub/dp/B003IXAOHK/ref=pd_sim_m_22";
        
        String[] collectionOfURL={
        		/**-----Forum--------------------------------------------------------------------------------------------*/
        		/*0		Startseite*/			"http://www.informatikforum.de/",
        		/*1		Unterforum normal*/		"http://www.informatikforum.de/forumdisplay.php?f=98",
        		/*2		Unterforum normal*/		"http://www.informatikforum.de/forumdisplay.php?f=68",
        		/*3		Unterforum kurz*/		"http://www.informatikforum.de/forumdisplay.php?f=87",
        		/*4		Thread normal*/			"http://www.informatikforum.de/showthread.php?t=159299",
        		/*5		Thread normal*/			"http://www.informatikforum.de/showthread.php?t=159992",
        		/*6		Thread lang*/			"http://www.informatikforum.de/showthread.php?t=1381",
        		/*7		Thread kurz*/			"http://www.informatikforum.de/showthread.php?t=132508",
        		
        		/**-----Händler-Seite------------------------------------------------------------------------------------*/
        		/*8		Startseite*/			"http://www.amazon.de/",
        		/*9		Produkt Musik*/			"http://www.amazon.de/Suburbs-Artikel-unterschiedlichen-Covervarianten-ausgeliefert/dp/B003V0EWJQ/ref=pd_sim_m_4",
        		/*10	Produkt Musik*/			"http://www.amazon.de/American-Slang-Gaslight-Anthem/dp/B003FK8V7G/ref=pd_bxgy_m_img_c",
        		/*11	Produkt Musik*/			"http://www.amazon.de/Shadows-Teenage-Fanclub/dp/B003IXAOHK/ref=pd_sim_m_22",
        		/*12	Produkt Spiel*/			"http://www.amazon.de/Pegasus-Spiele-17186G-Munchkin-Kuhthulus/dp/3939794627/ref=pd_bxgy_toy_img_a",
        		/*13	Produkt Spiel*/			"http://www.amazon.de/Heidelberger-Spieleverlag-HEI00058-Herr-Meister/dp/B000EWPC84/ref=pd_sim_toy_56",
        		/*14	Suchseite Liste lang*/	"http://www.amazon.de/s/ref=nb_sb_noss?__mk_de_DE=%C5M%C5Z%D5%D1&url=search-alias%3Daps&field-keywords=drucker&x=0&y=0",
        		/*15	Suchseite Liste kurz*/	"http://www.amazon.de/s/ref=nb_sb_ss_i_8_6?__mk_de_DE=%C5M%C5Z%D5%D1&url=search-alias%3Daps&field-keywords=pickelhaube+deutschland&sprefix=pickel",
        		/*16	Suchseite Kachel lang*/	"http://www.amazon.de/s/ref=nb_sb_noss?__mk_de_DE=%C5M%C5Z%D5%D1&url=search-alias%3Ddiy&field-keywords=stuhl",
        		/*17	Suchseite Kachel lang*/	"http://www.amazon.de/s/ref=nb_sb_ss_i_0_3?__mk_de_DE=%C5M%C5Z%D5%D1&url=search-alias%3Ddiy&field-keywords=s%E4ge&sprefix=s%E4g",
        		/*18	Suchseite Kachel kurz*/	"http://www.amazon.de/s/ref=nb_sb_noss?__mk_de_DE=%C5M%C5Z%D5%D1&url=search-alias%3Ddiy&field-keywords=absatz",
        };
        
        testURL1=collectionOfURL[0];
        testURL2=collectionOfURL[7];
        
        //testURL1="http://www.spiegel.de";
        //testURL2="http://www.otto.de";
        
        //find similar files to given URL
        URL=collectionOfURL[1]; 
        //URL="http://www.dirks-computerseite.de/category/internet/";
        //findSimilarFiles(URL, 5000, 9, 0.689, 5);
        
        
        
//        Map similarityOfNodes = new LinkedHashMap();
//        Map testMap = new LinkedHashMap(); 
//        //testMap = findSimilarFiles(URL, 5000, 9, 0.689, 5);
//        
//        testMap.put("http://www.informatikforum.de/forumdisplay.php?f=39", "0.9485261961871161");
//        testMap.put("http://www.informatikforum.de/forumdisplay.php?f=31", "0.9698535597228519");
//        testMap.put("http://www.informatikforum.de/forumdisplay.php?f=27", "0.9693909965972903");
//        testMap.put("http://www.informatikforum.de/forumdisplay.php?f=98&daysprune=-1&order=desc&sort=replycount", "0.9629184384605889");
//        testMap.put("http://www.informatikforum.de/forumdisplay.php?f=98&daysprune=-1&order=desc&sort=voteavg", "0.9573928892042225");
//        
//        ArrayList conflictNodes = markLeafs3("http://www.informatikforum.de/forumdisplay.php?f=98", "http://www.informatikforum.de/forumdisplay.php?f=31", "C:\\Users\\Silvio\\Documents\\doc2.html", 30);
//        System.out.println(conflictNodes.size());
//        
          Document docu = c.getWebDocument(URL);
//
//        ArrayList listOfSimilarDocuments = new ArrayList();
//        Iterator it = testMap.entrySet().iterator();
//        while (it.hasNext()) {
//            Map.Entry pairs = (Map.Entry)it.next();
//            Document doc = c.getWebDocument((String) pairs.getKey());
//            listOfSimilarDocuments.add(doc);
//        }
//        
//        ArrayList listOfSimilarDocumentsIncOrg = new ArrayList(listOfSimilarDocuments);
//        listOfSimilarDocumentsIncOrg.add(docu);
//        
//        
//        for (int i=0; i<conflictNodes.size(); i++) {
//	        String path = (String) conflictNodes.get(i);
////        	System.out.println(path);
////            String orgNode = HTMLHelper.htmlToString(XPathHelper.getNode(docu,path));
////            //System.out.println(orgNode);
////            
////            ArrayList jaccArray = new ArrayList();
////            Double jaccAverage = 0.0;
////            
////            /*String[] orgNodeZeilen;
////            orgNodeZeilen=orgNode.split("\n");
////            System.out.println(orgNodeZeilen);
////            
////            for (int j=0; j<orgNodeZeilen.length; j++) {
////            	System.out.println("Zeile"+j+": "+orgNodeZeilen[j]);
////            }*/
////            
////            Map orgNodeZeilen = new LinkedHashMap();
////            StringTokenizer st = new StringTokenizer(orgNode, "\n");
////
////            while ( st.hasMoreTokens() ) {
////            	String line = st.nextToken();
////            	//System.out.println("Zeile: "+line);
////            	orgNodeZeilen.put(line, 0);
////            }
////
////	        it = listOfSimilarDocuments.iterator();
////	        while (it.hasNext()) {
////	            Document doc = (Document)it.next();
////	            //System.out.println(pairs.getKey() + " = " + pairs.getValue());
////	            
////	            
////	            String simNode = HTMLHelper.htmlToString(XPathHelper.getNode(doc,path));
////	            
////	            Map simNodeZeilen = new LinkedHashMap();
////	            st = new StringTokenizer(simNode, "\n");
////
////	            while ( st.hasMoreTokens() ) {
////	            	String line = st.nextToken();
////	            	//System.out.println("Zeile: "+line);
////	            	simNodeZeilen.put(line, 0);
////	            }
////	            
////	            Double jacc = calculateJaccard(orgNodeZeilen, simNodeZeilen);
////	            if (jacc.isNaN()) jacc=0.0;
////	            System.out.println("Jacc="+jacc);
////	            jaccArray.add(jacc);
////	        }
////	        
////            for (int j=0; j<jaccArray.size(); j++) {
////            	jaccAverage=jaccAverage+(Double)jaccArray.get(j);
////            }
////            jaccAverage=jaccAverage/jaccArray.size();
////            System.out.println("Durchschnittswert single="+jaccAverage);
//	        	        
//            similarityOfNodes.put(path, calculateSimilarityForNode(listOfSimilarDocumentsIncOrg, path));
//            //System.out.println("Durchschnittswert multi="+similarityOfNodes);
//	        
//	        //System.out.println("--------------");
//
//        }
//        
//        System.out.println("FERTIG");
//        
//        it = similarityOfNodes.entrySet().iterator();
//        while (it.hasNext()) {
//            Map.Entry pairs = (Map.Entry)it.next();
//            System.out.println(pairs.getKey()+" = "+pairs.getValue());
//        }
        
     //   calculateSimilarityForAllNodes(docu);
        
        //testURL1="http://www.informatikforum.de/forumdisplay.php?f=98";
        //testURL2="http://www.informatikforum.de/forumdisplay.php?f=27";
        
        //testURL1="C:\\Users\\Silvio\\Documents\\test_2\\aehnlich\\informatikforum_de_forumdisplay_php_s_1023ae8d9ac0916d345312192ab1b775_f_98.html";
        //testURL2="C:\\Users\\Silvio\\Documents\\test_2\\aehnlich\\informatikforum_de_forumdisplay_php_s_1023ae8d9ac0916d345312192ab1b775_f_27.html";

        

        
        
        String s1 = PageSegmenterHelper.getContentOfURL(testURL1);
        String s2 = PageSegmenterHelper.getContentOfURL(testURL2);
                

        //System.out.println(s1.length()+" zu "+s2.length());
        
        //for (int i=s2.length(); i<s1.length(); i++) {s2=s2+"#";}
        //System.out.println(s1.length()+" zu "+s2.length());
       
        double dis=0.0;
        //dis=getStringDistance2(s1.substring(0, 1000),s2.substring(0, 1000));
        //dis=getStringDistance2(s1, s2);
        //System.out.println(dis);

        double sim=1-(dis/s1.length());
        //System.out.println(sim);

        /*int teiler=(s1.length())/10;
        System.out.println("teiler="+teiler);
        
        int ges=0;
        for (int i=1; i<=10; i++) {
        	int actual=teiler*i;
        	int dis=getStringDistance(s1.substring(actual-teiler, actual),s2.substring(actual-teiler, actual));
        	System.out.println("Distanz"+i+"="+dis);
        	ges=ges+dis;
        }
        
        System.out.println("Gesamtdistanz="+ges);*/
        
        
        testURL1="http://www.dirks-computerseite.de/category/internet/";
        testURL2="http://www.dirks-computerseite.de/category/netzwerk/";
        
        testURL1=collectionOfURL[1];
        testURL2="http://www.informatikforum.de/forumdisplay.php?s=c3a050e3de04955726811f7b8deec070&f=98&daysprune=-1&order=desc&sort=voteavg";

/*        Map page1 = createFingerprintForURL(testURL1, 5000, 3);
        Map page2 = createFingerprintForURL(testURL2, 5000, 3);
              
        //System.out.println(page1);
        //System.out.println(page2);
        
        Double vari = calculateSimilarity(page1, page2);
        
        String variString=(((Double)((1-vari)*100)).toString()).substring(0, 5);
        
        //if (vari<0.20) System.out.println("Seiten verwenden wahrscheinlich dasselbe Template. ("+variString+"%)");
        //else System.out.println("Unterschied zu groß. Seiten verwenden wahrscheinlich nicht dasselbe Template. ("+variString+"%)");
        
        
        Double jacc=calculateJaccard(page1, page2);
        //calculateJaccard2(page1, page2);

        System.out.println("vari="+(1-vari)+" jacc="+jacc);

        Double erg=(1-vari+jacc)/2;
        System.out.println(erg);
      
        markLeafs3(testURL1, testURL2, "C:\\Users\\Silvio\\Documents\\doc2.html", 30);
		
		
	*/	
		
		
	}

	
	
	
}
