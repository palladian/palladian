package tud.iir.preprocessing.segmentation;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.content.PageContentExtractorException;
import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.Tokenizer;
import tud.iir.helper.XPathHelper;
import tud.iir.web.Crawler;
import tud.iir.web.URLDownloader;

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
    private ArrayList<Document> similarFiles = null;
    
    // ///////////////////// important values ///////////////////////
    //all can be set in the segmenter.conf

    /** the length of q-grams for the similarity comparisons */
    private static int lengthOfQGrams = 0; 
    
    /** the amount of q-grams for the similarity comparisons */
    private static int amountOfQGrams = 0;
    
    /**   threshold needed to be similar */
    private static double similarityNeed = 0;
    
    /** the  maximal depth in DOM tree */
    private static int maxDepth = 0;
    
    /** the number of similar documents needed */
    private static int numberOfSimilarDocuments = 0;

    // ///////////////////// setter methods ///////////////////////
    
    public void setDocument(Document document) {
        this.document = document;
    }

    public void setDocument(String url) {
        Crawler c = new Crawler();
        this.document = c.getWebDocument(url);
    }
    
    //needs to be an html file
	public void setStoreLocation(String storeLocation) {
		this.storeLocation = storeLocation;
	}
	
	//only needed for evaluation
	public void setSimilarFiles(ArrayList<Document> similarFiles) {
		this.similarFiles = similarFiles;
	}
	
	//only needed for evaluation
	private static ArrayList<String> timeEvaluation = new ArrayList<String>();
	
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
	
	public ArrayList<String> getAllXPaths() {
		ArrayList<String> XPaths = new ArrayList<String>();
		
		for (int i=0; i<segments.size(); i++) {
			XPaths.add(segments.get(i).getXPath());
		}
		
		return XPaths;
	}
	
	/**
	 * Gets the list of similar files
	 * 
	 * @return A list of similar files.
	 */
	public ArrayList<Document> getSimilarFiles() {
		return similarFiles;
	}
	
	/** 
	 * Returns only segments specified by color. 
	 * 
	 * @param color The color of segments to return. E.g. "Segment.Color.RED"
	 * @return A list of Segments.
	 */
	public ArrayList<Segment> getSpecificSegments(Segment.Color color) {
		ArrayList<Segment> allSegs = new ArrayList<Segment>();
		
		for (int i=0; i<segments.size(); i++) {
			Segment seg = segments.get(i);
			//System.out.println(seg.getColor());
			if (seg.getColor()==color) {
				//System.out.println("TREFFER! DAS IST "+color);
				allSegs.add(seg);
			}
		}
		
		return allSegs;
	}
	
	/**
	 * Returns segments in the range from beginValue to endValue
	 * 
	 * @param beginValue The begin value of variability
	 * @param endValue The end value of variability
	 * @return A list of Segments.
	 */
	public ArrayList<Segment> getSpecificSegments(double beginValue, double endValue) {
		ArrayList<Segment> allSegs = new ArrayList<Segment>();
		
		for (int i=0; i<segments.size(); i++) {
			Segment seg = segments.get(i);
			//System.out.println(seg.getSignificance());
			if (seg.getVariability()>=beginValue && seg.getVariability()<=endValue) {
				//System.out.println("TREFFER! "+seg.getSignificance()+" IST ZWISCHEN "+beginValue+" UND "+endValue);
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
    Map<String, Integer> createFingerprintForURL(Document doc, int number, int length) throws MalformedURLException, IOException {
    	Crawler c = new Crawler();
    	
        //String dText=PageSegmenterHelper.getContentOfURL(url);
    	String dText = c.documentToString(doc);
        
        String tagList="";
        Iterator<String> it=HTMLHelper.listTags(dText).iterator();
        while (it.hasNext()) {
        	//System.out.println(it.next());
            tagList=tagList+" "+it.next();
        }
        //System.out.println(tagList);
        //System.out.println(HTMLHelper.listTags(dText));
        
        List<String> listOfTags=new ArrayList<String>(Tokenizer.calculateWordNGramsAsList(tagList, length));          
        
        Map<String, Integer> mapOfTags=PageSegmenterHelper.convertListToMap(listOfTags);
        
        mapOfTags=PageSegmenterHelper.sortMapByIntegerValues(mapOfTags);
        //System.out.println(mapOfTags);
        
        Map<String, Integer> testMap = PageSegmenterHelper.limitMap(mapOfTags, number);
        //System.out.println(testMap);
    	
    	return testMap;
    }

    /**
     * Find similar files for an given URL.
     * 
     * @param document The document to search similar files for.
     * @param qgramNumber The number of q-grams to use.
     * @param qgramLength The length of q-grams to use.
     * @param similarityNeed Defines how much similarity is needed to be similar. Value between 0 and 1, e.g. 0.88)
     * @param limit The maximum of similar files to find.
     * 
     * @return A list of documents similar to the given URL.
     */
    private ArrayList<Document> findSimilarFiles(Document document, int qgramNumber, int qgramLength, double similarityNeed, int limit) throws MalformedURLException, IOException {
    	Map<Document, Double> result=new LinkedHashMap<Document, Double>();

    	Crawler c = new Crawler();
    	Document d = document;
  
        // Start of collect URLs (step 1) ////////////////////////////////////////////////////////        
        
        URLDownloader urlDownloader = new URLDownloader();
        
        HashSet<String> links = new HashSet<String>();
        links.addAll(c.getLinks(d,true, false,""));
        System.out.println("Anzahl Links: "+links.size());
        
        Iterator iter2 = links.iterator();
        int zaehler=0;
        while (iter2.hasNext()) {
        //for (int i=0; i<links.size(); i++) {
        	String newURL = (String) iter2.next();
        	int mod = links.size()/10;
        	if (zaehler%mod==0) {
        		urlDownloader.add(newURL);
        		System.out.println("added1: "+newURL);
        	}
        	zaehler++;
        }
        
        HashSet<String> te3 = new HashSet<String>();
        String newURL2 = document.getDocumentURI();
        Boolean moreSlashs2 = true;
        while (moreSlashs2) {
        	urlDownloader.add(newURL2);
    		System.out.println("added2: "+newURL2);
	        int lastSlash = newURL2.lastIndexOf("/");
	        if (!newURL2.substring(lastSlash-1, lastSlash).equals("/")) {
	        	newURL2=newURL2.substring(0, lastSlash);
	        }
	        else {
	        	moreSlashs2 = false;
	        }
    	}
        
        Set<Document> documents = urlDownloader.start();

        Iterator iter = documents.iterator();
        while (iter.hasNext()) {
        	Document doc = (Document) iter.next();
        	te3.addAll(c.getLinks(doc,true, false,""));
        }
        
//        //delete all duplicates of the URL like ...www.URL.de?something...
//        HashSet<String> te4 = new HashSet<String>();
//        iter = te3.iterator();
//        while (iter.hasNext()) {
//        	String doc = (String) iter.next();
//        	if (doc.contains(URL)) te4.add(doc);
//        }
//        te3.removeAll(te4);

    	te3.remove(document.getDocumentURI());

        // End of collect URLs (step 1) ////////////////////////////////////////////////////////
        
        
        // Start of find similar URSs (step 2) ////////////////////////////////////////////////////////
        
    	//Vorfiltern anhand des URL-Teilstrings
        String label=PageSegmenterHelper.getLabelOfURL(document.getDocumentURI());
        System.out.println("label: "+label);

        List<String> te2 = new ArrayList<String>(te3);
        //System.out.println(te2);
        int size=te2.size();
        int counter=0;
        for (int i=0; i<size; i++) {
        	String currentURL=(String)te2.get(i);
            String currentLabel=PageSegmenterHelper.getLabelOfURL(currentURL);
            //System.out.println("currentLabel: "+currentLabel+" matches??: "+(currentLabel.matches("^[0-9]+$")));
            if (!label.equals(currentLabel) && (label.matches("^[0-9]+$") && !currentLabel.matches("^[0-9]+$"))
            		|| (!label.equals(currentLabel) && (!label.matches("^[0-9]+$"))) ) {
            	te2.remove(i); i--;
            	te2.add(size-1, currentURL);
            	//System.out.println("Nicht gleich, wird entfernt. Größe: "+te2.size());
                //System.out.println(te2);
            }
            counter++;
            if (counter==size) break;
        }
        System.out.println(te2);

        
    	Map<String, Integer> page1 = createFingerprintForURL(d, qgramNumber, qgramLength);
        
    	URLDownloader urlDownloader2 = new URLDownloader();
    	
        Iterator<String> it=te2.iterator();
        String currentElement="";
        while (it.hasNext()) {
        	
        	int count = 0;
        	while (it.hasNext() && count<10) {
        		urlDownloader2.add((String)it.next());
        		count++;
        	}
        	Set<Document> currentDocuments = urlDownloader2.start();
        	
        	Iterator<Document> it2 = currentDocuments.iterator();
        	while (it2.hasNext()) {
	        	Document currentDocument = it2.next();
        		currentElement = currentDocument.getDocumentURI();
	        	
	        	if ((HTMLHelper.htmlToString(d)).equals(HTMLHelper.htmlToString(currentDocument))) {
	        		System.out.println("#####################################################");
	        		continue;
	        	}
	
	        	//System.out.println(getStringDistance(currentElement, URL));
	        	
	            Map<String, Integer> page2 = createFingerprintForURL(currentDocument, qgramNumber, qgramLength);
	            
	            //System.out.println(page1);
	            //System.out.println(page2);
	            
	            Double vari = SimilarityCalculator.calculateSimilarity(page1, page2);
	            Double jacc = SimilarityCalculator.calculateJaccard(page1, page2);
	            
	            String variString=(((Double)((1-vari)*100)).toString());
	            variString=variString.substring(0, Math.min(5,variString.length()));
	            
	            double erg = (1-vari+jacc)/2;
	            //System.out.println(erg);

	            if (erg>=similarityNeed && erg<1.0) {
	            	result.put(currentDocument, erg);
	            	System.out.println("Seiten verwenden wahrscheinlich dasselbe Template. ("+variString+"%, Jaccard="+jacc+")----------"+result.size());
	            	
	            }
	            else System.out.println("Unterschied zu groß. Seiten verwenden wahrscheinlich nicht dasselbe Template. ("+variString+"%, Jaccard="+jacc+")");
	            
	            System.out.println("----------------------------------------------------------------------------------------");
	            
	            
	          
	        	if (result.size()>=limit) {
	        		System.out.println("---Erg.: "+result);
	        		break;
	        	}
        	}
        	if (result.size()>=limit) {
        		System.out.println("---Erg.: "+result);
        		break;
        	}
        }
        
        result=PageSegmenterHelper.sortMapByDoubleValues(result);

        // End of find similar URSs (step 2) ////////////////////////////////////////////////////////
        
		ArrayList<Document> simFiles = new ArrayList<Document>();
		simFiles = new ArrayList<Document>(result.keySet());
        
    	return simFiles;
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
     * @param xPath The xPath generated by recursion.
     * @return A list of two ArrayLists. Conflict-nodes and non-conflict-nodes.
     */
    private ArrayList<ArrayList<String>>[] compareDocuments(Document document1, Document document2, ArrayList<String> conflictNodes, ArrayList<String> nonConflictNodes, int level, String xPath) throws ParserConfigurationException {
        NodeList helpList1 = document1.getFirstChild().getChildNodes();
        NodeList helpList2 = document2.getFirstChild().getChildNodes();
        
        PageAnalyzer pa = new PageAnalyzer();
                
        for (int i=0; i<helpList1.getLength(); i++) {
        	Node n1 = (Node)helpList1.item(i);
	        //Node n2 = (Node)helpList2.item(i);
        	if (n1.getTextContent().length()==0) continue;

        	Node n2 = document1.createElement("newnode");//n1.cloneNode(true);
        	n2.setNodeValue("###");
        	n2.setTextContent("#####");
        	
            if (helpList2.getLength()>(i)) n2=(Node)(helpList2.item(i));
            	        
	        String constructXPath = pa.constructXPath(n1);
	        //System.out.println("const: "+constructXPath);
	        
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
	        
	        //System.out.println("newxp: "+newXPath);
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
		            
		            //System.out.println("--------------"+newXPath);
		            
		            //checkChilden
		            if (level>=0) compareDocuments(doc1, doc2, conflictNodes, nonConflictNodes, level-1, newXPath);
	        	}
	            

	        }
        }
         
    	
    	return new ArrayList[] {conflictNodes, nonConflictNodes};
    }
    
    
    public void colorSegments() {
    	colorSegments(segments, true);
    }
    
    public void colorSegments(Segment.Color color) {
        ArrayList<Segment> coloredSegments = getSpecificSegments(color);
    	colorSegments(coloredSegments, true);
    }

    /**
     * Colors the segments of the document based on a comparison with similar documents.
     * Every conflict node will be evaluated in all documents to get a similarity value. Based
     * on this value a colored border is placed.
     * 
     * @param chosenSegmentsInput A list of segments to color. Either a list of xPaths as string
     * or a list of Segments.
     * @param kindOfColoring Defines the kind of the coloring of segments. true for borders,
     * false for background
     */
    public void colorSegments(ArrayList<?> chosenSegmentsInput, Boolean kindOfColoring) {
    	
    	ArrayList<Segment> chosenSegments = new ArrayList<Segment>();
    	
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
     		if (kindOfColoring) {
     			colorString=colorString+".myPageSegmenterBorder"+i+" { border: 2px solid "+colorScale[i]+"; }\n";
     		}
     		else {
     			colorString=colorString+".myPageSegmenterBorder"+i+" { background-color: "+colorScale[i]+"; }\n";
     		}
     	}
     	
      	Text text = document.createTextNode(colorString);
     	e3.appendChild(text);
     	//System.out.println(e3.getTextContent());
     	
     	if (document.getElementsByTagName("style").getLength()!=0) {    	
     		Node styleNode = document.getElementsByTagName("style").item(0);
     		styleNode.appendChild(text);
     		//System.out.println("gefunden\n"+styleNode.getTextContent());
     	}
     	else {
     		Node styleNode = document.getElementsByTagName("head").item(0);
         	//System.out.println(styleNode.getNodeName()+"-------------------");
         	styleNode.appendChild(e3);     		
         	//System.out.println("nicht gefunden\n"+styleNode.getTextContent());         	
     	}
     	
     	// if input is a list of xPaths, turn it into a list of Segments
 		System.out.println(chosenSegmentsInput.get(0).getClass().getSimpleName());
 		System.out.println(chosenSegmentsInput.get(0));
 		if (chosenSegmentsInput.get(0).getClass().getSimpleName().equals("String")) {
 	 		System.out.println("... War ein String");

     		ArrayList<Segment> chosenSegments2 = new ArrayList<Segment>();
         	for (int i=0; i<segments.size(); i++) {
         		if(chosenSegmentsInput.contains(segments.get(i).getXPath())) {
         			chosenSegments2.add(segments.get(i));
         		}
           	}
         	chosenSegments=chosenSegments2;
     	}
 		
 		if (chosenSegmentsInput.get(0).getClass().getSimpleName().equals("Segment")) {
 			chosenSegments=(ArrayList<Segment>) chosenSegmentsInput;
 		}


     	
     	
     	//checks the similarity of ALL nodes
     	for (int i=0; i<chosenSegments.size(); i++) {
    		Segment testSeg = (Segment) chosenSegments.get(i);
    		System.out.println(testSeg.getVariability()+" "+testSeg.getColor()+" "+testSeg.getXPath());
	
	    	
	      	Element e2 = (Element)  XPathHelper.getNode(document, testSeg.getXPath());
	      	
	      	//if (getNodeLevel(e2)<=10) {
		
		        	String border="";
		        	double value=testSeg.getVariability();
		        	//System.out.println(value);
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
        
        ////////////////////////////////////////////////////////////////////////77
        //fix some problems with closing tags
        Crawler c2 = new Crawler();
        String webpage = c2.download(storeLocation);
        String newStoreLocation = storeLocation.substring(0, storeLocation.length()-5)+"_test.html";
        
        String[] tagsToFix={"SCRIPT", "IFRAME", "TEXTAREA"};
    	for (int i=0; i<tagsToFix.length; i++) {
    		String tag=tagsToFix[i];
	        int start=0;
	        while (start<webpage.length()) {
	        	int index1=0;
	        	int index2=0;
	        	int index3=0;
	
	        	
	        	index1=webpage.indexOf("<"+tag+" ",start);
	        	index2=webpage.indexOf("/>", index1);
	        	index3=webpage.indexOf("</"+tag+">", index1);
	        	//System.out.println("tag: "+tag+"::::: start: "+start+" --- index1: "+index1+" --- index2: "+index2+" --- index3: "+index3);
		        
	        	if (index2==-1) index2=webpage.length();
	        	if (index3==-1) index3=webpage.length();

	        	if (index2<index3 && index1!=-1) webpage=webpage.substring(0, index2)+"></"+tag+"><!--fixedForPageSegmenter-->"+webpage.substring(index2+2,webpage.length());

	        	if (index2<=index3) start=index2;
		        else start=index3;
		        
	        	if (index1==-1) start=webpage.length();

	        }
    	}

        FileOutputStream print;
		try {
			print = new FileOutputStream(newStoreLocation);
	        for (int i=0; i < webpage.length(); i++){
	        	print.write((byte)webpage.charAt(i));
	        }
	        print.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
    }
    
    /**
     * Starts the page segmentation process
     */
    public void startPageSegmentation() throws ParserConfigurationException, IOException {

        // Start of step 1 and 2 of the algorithm ////////////////////////////////////////////////////////        
    	
    	//"if" makes it possible to set similar files; e.g. for the evaluation
    	if (similarFiles == null) {
    		System.out.println("Start findSimilarFiles------------------");
	        similarFiles = findSimilarFiles(document, amountOfQGrams, lengthOfQGrams, similarityNeed, numberOfSimilarDocuments);
    	}
    	
        // End of step 1 and 2 of the algorithm ////////////////////////////////////////////////////////        

        
        // Start of segment URLs by finding the conflicts (step 3) ////////////////////////////////////////////////////////        
        
        Node bodyNode1=document.getElementsByTagName("body").item(0);
        ArrayList<String> conflictNodes=new ArrayList<String>();
        ArrayList<String> nonConflictNodes=new ArrayList<String>();
        
    	for (int i=0; i<similarFiles.size(); i++) {
    		System.out.println((i+1)+".Runde-----------------------------------------");
    		
//            String URL2 = (String) similarFiles.keySet().toArray()[i];
//            Document document2 = crawler.getWebDocument(URL2);
    		Document document2 = similarFiles.get(i);
	        Node bodyNode2=document2.getElementsByTagName("body").item(0);
	        
	        //build new docs from body nodes
	        Document doc1=PageSegmenterHelper.transformNodeToDocument(bodyNode1);
	        Document doc2=PageSegmenterHelper.transformNodeToDocument(bodyNode2);
	        

	        //returns a list of xpaths of all conflict and a second of all non-conflict nodes of the actual compare
	        ArrayList[] allNodes=compareDocuments(doc1, doc2, new ArrayList<String>(), new ArrayList<String>(), maxDepth, "/HTML/BODY");
	        
	        System.out.println(allNodes[0].size()+"-"+conflictNodes.size()+"="+(allNodes[0].size()-conflictNodes.size())+" zu "+(conflictNodes.size()*50/100));
	        if ((allNodes[0].size()-conflictNodes.size())<conflictNodes.size()*50/100 || conflictNodes.size()==0) {
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
	            similarFiles.remove(document2);

	        }
	    }
    	
        //removes all conflictNodes from the list of nonConflictNodes
        for (int i=0; i<conflictNodes.size(); i++) {
        	String n = (String) conflictNodes.get(i);
            for (int i2=0; i2<nonConflictNodes.size(); i2++) {
            	String n2 = (String) nonConflictNodes.get(i2);
	        	
            	if (n.contains(n2)) {
            		//System.out.println("YES\n"+n+"\n"+n2);
	        		nonConflictNodes.remove(n2);
	        		//i2--;
	        		//System.out.println(n+" gelöscht.");
	        		
	        	}
            	//else System.out.println("NO");
            }
        }
        
        // End of segment URLs by finding the conflicts (step 3) ////////////////////////////////////////////////////////        

        // Start of rating the segments (step 4) ////////////////////////////////////////////////////////        
        
        Map<String, Double> conflictNodesIncSim = SimilarityCalculator.calculateSimilarityForAllNodes(document, conflictNodes, similarFiles);
        
        segments = generateListOfSegments(document, conflictNodesIncSim, nonConflictNodes);

        // End of rating the segments (step 4) ////////////////////////////////////////////////////////        

        
        System.out.println("Size conflictNodes: "+conflictNodes.size());
        System.out.println("Size nonConflictNodes: "+nonConflictNodes.size());

        //return conflictNodes;
    }

    /**
     * Generates a list of segments after the segmentation has been done.
     * 
     * @param document The original document.
     * @param conflictNodes The conflict nodes of the document.
     * @param nonConflictNodes The non-conflict nodes of the document.
     * @return A list of segments.
     */
    private ArrayList<Segment> generateListOfSegments(Document document, Map<String, Double> conflictNodes, ArrayList<String> nonConflictNodes) {
    	
    	ArrayList<Segment> allSegments = new ArrayList<Segment>();
    	
    	for (int i=0; i<nonConflictNodes.size(); i++) {
        	String xPath = (String) nonConflictNodes.get(i);    
        	Element node = (Element)  XPathHelper.getNode(document, xPath);
        	if (node==null) continue;
        	//System.out.println(node+" mit "+xPath);
        	Integer depth = PageSegmenterHelper.getNodeLevel(node);
        	//System.out.println("Level="+depth);
        	
        	Segment seg = new Segment(document, xPath, node, depth, 0.0);
        	allSegments.add(seg);
        }
    	
        Iterator it = conflictNodes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();

        	String xPath = (String) pairs.getKey();
        	Double significance = (Double) pairs.getValue();
        	Element node = (Element)  XPathHelper.getNode(document, xPath);
        	if (node==null) continue;
        	Integer depth = PageSegmenterHelper.getNodeLevel(node);
        	
        	Segment seg = new Segment(document, xPath, node, depth, 1-significance);
        	allSegments.add(seg);
        }  	

    	return allSegments;
    }
    
    /**
     * Tries to find a mutual xPath for the given segments and returns all the segments under that path.
     * 
     * @param allSegments A list of segments. Can be prefiltered, like only RED.
     * @param level The number of rounds. 1 is enough.
     * @return A list of segments under the mutual xPath.
     */
    public ArrayList<String> makeMutual (ArrayList<Segment> allSegments, int level) {

     	ArrayList<String> xPathList = new ArrayList<String>();
    	
    	
    	PageAnalyzer pa2 = new PageAnalyzer();


     	
	    HashSet<String> s = new HashSet<String>();
	    for (int i=0; i<allSegments.size(); i++) {
	    	Segment actualSeg = allSegments.get(i);
	    	//System.out.println(actualSeg.getXPath());
	    	//System.out.println(actualSeg.getSignificance());
	    	/*if (actualSeg.getColor()==Segment.Color.RED)*/ s.add(actualSeg.getXPath());
	    }
		    
	    for (int l=0; l<level; l++) {

		    
	        String mutual = pa2.makeMutualXPath(s);
	        System.out.println("mutual: "+mutual);
	        
	        
	        String xp = mutual;
	     	System.out.println(xp.substring(xp.lastIndexOf("/")+1, xp.length()));
	     	if (xp.substring(xp.lastIndexOf("/")+1, xp.length()).equals("TR")) {
	     		System.out.println("WAR EIN TR");
	     		xp=xp+"/TD";
	     	}
	        ArrayList<Node> list = (ArrayList<Node>) XPathHelper.getNodes(document, xp);
	     	System.out.println("--------------\n"+xp+"\nS.size: "+s.size()+"\n---------------");
		    for (int i=0; i<list.size(); i++) {
		    	Node n = (Node) list.get(i);
		        String constructXPath = pa2.constructXPath(n);
		    	System.out.println(constructXPath);
		    	//System.out.println(actualSeg.getSignificance());
		    	xPathList.add(constructXPath);
		    	s.remove(constructXPath);
		    }
	    System.out.println("S.size neu: "+s.size());
	    System.out.println(s);
	    }
	    
	    return xPathList;
    }
    
    /**
     * Finds the main segments.
     * 
     * @param segments A list of segments to find the main segments for. Can be prefiltered, e.g.
     * find only the RED and GREEN main segments.
     * @return A list of main segments.
     */
    public ArrayList<Segment> findMainSegments (ArrayList<Segment> segments) {
    	ArrayList<Segment> biggestSegments = new ArrayList<Segment>();
    	System.out.println("biggest-begin: "+segments.size());
    	
		for (int i1=0; i1<segments.size(); i1++) {
			//System.out.println(i1+".Runde ");
			Segment seg1 = (Segment)segments.get(i1);
			//System.out.println(seg1);
			 
			boolean cancel=false;
			Node node3 = seg1.getNode();
			while (node3.getParentNode() != null && !cancel) {
				node3 = node3.getParentNode();
			 
		    	for (int i2=0; i2<segments.size(); i2++) {
		    		Segment seg2 = (Segment)segments.get(i2);
		
		    		if (seg2.getNode().isSameNode(node3) && !seg2.equals(seg1) && seg2.getColor().equals(seg1.getColor())) {
		    			//System.out.println(seg1.getNode().getTextContent());
		    			//System.out.println("yes\n"+seg1.getXPath()+"\n"+seg2.getXPath());
		    			//biggestSegments.add(seg1);
		    			segments.remove(seg1);
		    			i1--;
		    			cancel=true;
		    			break;
		    		} 
		    	}
			}
		}
		
    	System.out.println("biggest-middle: "+segments.size());
		
		for (int i1=0; i1<segments.size(); i1++) {
			Segment seg1 = (Segment)segments.get(i1);
			if (seg1.getNode().getTextContent().length()<50) {
				segments.remove(seg1);
				i1--;
			}
		}
		
		for (int i1=0; i1<segments.size(); i1++) {
			System.out.println(segments.get(i1).getVariability()+" "+segments.get(i1).getXPath());
		}
		
		
     	
    	System.out.println("biggest-end: "+segments.size());
    	return segments;
    }
 
    
    
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException, PageContentExtractorException {
		Crawler c = new Crawler();
		String URL="http://www.kalender-365.de";
		URL="http://www.amazon.de";
		//URL="http://www.december.com/html/demo/hello.html";
		URL="http://www.informatik-blog.net/category/c/";
		URL="http://www.informatikforum.de/forumdisplay.php?f=98";
		URL="http://www.foren.net/content.html";
		URL="http://blogalm.de/";
		//URL="http://www.pcwelt.de/forum/";
        //URL="C:\\Users\\Silvio\\Documents\\test_2\\x_ausgangsdatei\\informatikforum_de_forumdisplay_php_s_1023ae8d9ac0916d345312192ab1b775_f_98.html";

		//URL="http://blog.meingolfportal.de/";
		
        Document d = c.getWebDocument(URL);
        
        //PageSegmenterTrainer.downladRandomSitesForEvaluation2(URL, 5, 10);
		
		//Counts and lists all internal URLs
//        HashSet<String> te = new HashSet<String>();
//		te=c.getLinks(d,true, false,"");
//		System.out.println(te.size()+" intern verlinkte URLs gefunden!");
//        System.out.println(te);
//        
//        Iterator it = te.iterator();
//        while (it.hasNext()) {
//        	System.out.println(it.next());
//        }
        
//        //Counts and lists all tags within that URL        
//        String dText=c.documentToString(d);
//        System.out.println(HTMLHelper.countTags(dText)+" Tags im Code gefunden!");
//        System.out.println(HTMLHelper.listTags(dText));
        
        
        
        
//        PageSegmenterTrainer.saveURLToDisc("http://www.informatikforum.de/forumdisplay.php?f=98", "C:\\Users\\Silvio\\Documents\\downlad0.html");
//        PageSegmenterTrainer.saveURLToDisc("http://www.informatikforum.de/forumdisplay.php?f=84", "C:\\Users\\Silvio\\Documents\\downlad1.html");
//        PageSegmenterTrainer.saveURLToDisc("http://www.informatikforum.de/forumdisplay.php?f=80", "C:\\Users\\Silvio\\Documents\\downlad2.html");
//        PageSegmenterTrainer.saveURLToDisc("http://www.informatikforum.de/forumdisplay.php?f=88", "C:\\Users\\Silvio\\Documents\\downlad3.html");
//        PageSegmenterTrainer.saveURLToDisc("http://www.informatikforum.de/forumdisplay.php?f=81", "C:\\Users\\Silvio\\Documents\\downlad4.html");
//        PageSegmenterTrainer.saveURLToDisc("http://www.informatikforum.de/forumdisplay.php?f=71", "C:\\Users\\Silvio\\Documents\\downlad5.html");
        
        
        
        
        System.out.println("test: "+lengthOfQGrams);
        PageSegmenter seg = new PageSegmenter();
        System.out.println("test: "+lengthOfQGrams+" "+amountOfQGrams+" "+similarityNeed+" "+maxDepth+" "+numberOfSimilarDocuments);

//        seg.setDocument("data/test/pageSegmenter/forum_temp1.html");
//        
//        Map<String, Double> simMap = new HashMap();
//        simMap.put("data/test/pageSegmenter/forum_temp1_aehnlich1.html", 0.0);
//        simMap.put("data/test/pageSegmenter/forum_temp1_aehnlich2.html", 0.0);
//        simMap.put("data/test/pageSegmenter/forum_temp1_aehnlich3.html", 0.0);
//        simMap.put("data/test/pageSegmenter/forum_temp1_aehnlich4.html", 0.0);
//        simMap.put("data/test/pageSegmenter/forum_temp1_aehnlich5.html", 0.0);
//        seg.setSimilarFiles(simMap);
        
        //seg.setDocument("http://www.informatikforum.de/forumdisplay.php?f=98");
        //seg.setDocument("http://www.informatikforum.de/showthread.php?t=1381");
        //seg.setDocument("http://www.informatikforum.de/showthread.php?t=132508");
        //seg.setDocument("http://sebstein.hpfsc.de/tags/berlin/");
        //seg.setDocument("http://forum.spiegel.de/showthread.php?t=24486");
        //seg.setDocument("http://profootballtalk.nbcsports.com/2010/11/10/roddy-white-questionable-for-thursday-night/");
        //seg.setDocument("http://www.berryreview.com/2010/10/14/tips-tricks-use-the-right-convenience-key-to-focus-camera/");
        //seg.setDocument("http://www.dirks-computerseite.de/category/internet/");
        //seg.setDocument("http://gizmodo.com/5592956/is-3d-already-dying");
        //seg.setDocument("http://www.stern.de/digital/computer/sozialer-browser-rockmelt-wo-facebook-immer-mitsurft-1621849.html");
        //seg.setDocument("http://www.it-blog.net/kategorien/5-Windows");
        //seg.setDocument("http://www.basicthinking.de/blog/2006/10/02/ist-die-zeit-der-mischblogs-vorbei/");
        //---gut---
        //seg.setDocument("http://www.kabelstoerung.de/pixelbildung-beim-digitalen-fernsehen");
        //seg.setDocument("http://www.smavel.com/forum/de/botsuana/761-oko-tourismus.html");
        //seg.setDocument("http://www.jusline.at/index.php?cpid=ba688068a8c8a95352ed951ddb88783e&lawid=62&paid=75&mvpa=92");
        seg.setDocument("http://forum.handycool.de/viewforum.php?id=20");
        seg.setStoreLocation("C:\\Users\\Silvio\\Documents\\doc2.html");
        seg.startPageSegmentation();
        
        ArrayList<Segment> allSegments = seg.getAllSegments();
        //allSegments = seg.findMainSegments(allSegments);

        
        //ArrayList<Segment> chosenSegments = seg.getSpecificSegments(Segment.Color.RED);
        //chosenSegments.addAll(seg.getSpecificSegments(Segment.Color.GREEN));
        //chosenSegments = seg.getSpecificSegments(0.96, 1.00);

        //chosenSegments = seg.findMainSegments(chosenSegments);
        
        //seg.colorSegments(chosenSegments, false);

        //seg.colorSegments(Segment.Color.RED);
        
        seg.colorSegments(allSegments, true);
        
       
        //-->in funktion makeMutual
        //ArrayList<String> mutualXPaths= seg.makeMutual(chosenSegments, 1);
	    //seg.colorSegments(mutualXPaths,true);
        

        
        //eva
        
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "001", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "002", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "003", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "004", false);       
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "005", false);
//
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "006", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "007", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "008", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "009", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "010", false);
//
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "011", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "012", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "013", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "014", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "015", false);
//
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "016", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "017", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "018", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "019", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "020", false);
//
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "021", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "022", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "023", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "024", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Foren", "025", false);
//
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "001", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "002", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "003", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "004", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "005", false);
//        
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "006", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "007", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "008", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "009", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "010", false);
//
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "011", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "012", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "013", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "014", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "015", false);
//
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "016", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "017", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "018", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "019", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "020", false);
//
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "021", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "022", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "023", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "024", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Blogs", "025", false);
//
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Sonstige", "001", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Sonstige", "002", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Sonstige", "003", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Sonstige", "004", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Sonstige", "005", false);
//
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Sonstige", "006", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Sonstige", "007", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Sonstige", "008", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Sonstige", "009", false);
//        PageSegmenterTrainer.cvsTest("C:\\Users\\Silvio\\Documents\\CSV\\", "Sonstige", "010", false);

        
        String[] timeEvaluationURLs={
//        		"http://forum.handycool.de/viewtopic.php?pid=4608",
//        		"http://forum.handycool.de/viewforum.php?id=20",
//        		"http://www.chaotenecke.de/forum/forumdisplay.php?f=248",
//        		"http://www.chaotenecke.de/forum/showthread.php?t=6450",
//        		"http://www.natur-forum.de/forum/viewtopic.php?f=9&p=93967",
//        		"http://www.natur-forum.de/forum/viewforum.php?f=30",
//        		"http://www.autoextrem.de/showthread.php?p=1279888#post1279888",
//        		"http://www.autoextrem.de/forumdisplay,f-7.htm",
//        		"http://www.xpbulletin.de/f357.html",
//        		"http://www.xpbulletin.de/t57396-0.html",
        		///////////////////////////////////////////////////////////////////
//        		"http://blog.meingolfportal.de/golfblog/golfreisen",
//        		"http://blog.meingolfportal.de/allgemein/golf-auf-dem-ipad-mit-der-sky-sport-app#comments",
//        		"http://www.cab-drink.com/blog/?m=200907",
//        		"http://www.cab-drink.com/blog/?cat=8",
//        		"http://www.handy-blog24.de/2009/09/",
//        		"http://www.handy-blog24.de/handy-nachrichten/android-tablets-alternative-zum-ipad/",
//        		"http://www.online-cash.org/kostenloses-miniblog-script-als-wordpress-alternative/",
//        		"http://www.online-cash.org/2010/01/",
//        		"http://www.diaet-abnehmen.de/8-tipps-um-ohne-diaet-erfolgreich-abzunehmen/",
//        		"http://www.diaet-abnehmen.de/category/abnehm-tricks/",
        		///////////////////////////////////////////////////////////////////
//        		"http://www.intercharter.com/IC/charter_card_en.php?id=318",
//        		"http://www.mtv.com/music/artist/king_s_singers/albums.jhtml?albumId=999943",
//        		"http://www.welt.de/wissenschaft/article8304700/So-sollen-Unwetterwarnungen-besser-werden.html",
//        		"http://news.google.de/news/section?pz=1&cf=all&ned=de&topic=m&ict=ln",
//        		"http://www.side-manavgat.de/infos_alpha.php?buchstabe=B",
//        		"http://www.jusline.at/index.php?cpid=ba688068a8c8a95352ed951ddb88783e&lawid=62&paid=75&mvpa=92",
//        		"http://www.thespoiler.co.uk/index.php/2010/08/23/jerome-boateng-sidelined-by-comedy-aeroplane-injury",
//        		"http://www.ferienhausmiete.de/ferienhaus_suche1000.php?region=216",
//        		"http://www.jusos.de/themen/integration-und-inneres/abschaffung-des-%C2%A7%C2%A7-129-a-und-b-stgb",
//        		"http://lightlybuzzed.com/2010/10/joe-jonas-mom-hates-his-girlfriend-ashley-greene/"
        };
        
//        for (int et=0; et<timeEvaluationURLs.length; et++) {
//        	PageSegmenter segmenter = new PageSegmenter();
//        	segmenter.setDocument(timeEvaluationURLs[et]);
//        	segmenter.startPageSegmentation();
//        	
//        	int totalTime = Integer.parseInt(timeEvaluation.get(0)) + Integer.parseInt(timeEvaluation.get(1)) 
//        		+ Integer.parseInt(timeEvaluation.get(2)) + Integer.parseInt(timeEvaluation.get(3));
//        	String newLine = timeEvaluationURLs[et]+";"+timeEvaluation.get(0)+";"+timeEvaluation.get(1)+";"
//        		+timeEvaluation.get(2)+";"+timeEvaluation.get(3)+";"+totalTime;
//        	timeEvaluation=new ArrayList<String>();
//        	
//        	String mainFile = "C:\\Users\\Silvio\\Documents\\CSV\\performance2.csv";
//            ArrayList<String> mainListe = (ArrayList<String>) FileHelper.readFileToArray(mainFile);         
//            mainListe.add(newLine);
//            FileHelper.writeToFile(mainFile, mainListe);
//
//        }
        
//        PageSegmenter seg2 = new PageSegmenter();
//        seg2.setDocument("data/test/pageSegmenter/forum_temp1.html");
//        seg2.setStoreLocation("C:\\Users\\Silvio\\Documents\\doc2.html");
//        
//        ArrayList<Document> simList = new ArrayList<Document>();
//        simList.add(c.getWebDocument("data/test/pageSegmenter/forum_temp1_aehnlich1.html"));
//        simList.add(c.getWebDocument("data/test/pageSegmenter/forum_temp1_aehnlich2.html"));
//        simList.add(c.getWebDocument("data/test/pageSegmenter/forum_temp1_aehnlich3.html"));
//        simList.add(c.getWebDocument("data/test/pageSegmenter/forum_temp1_aehnlich4.html"));
//        simList.add(c.getWebDocument("data/test/pageSegmenter/forum_temp1_aehnlich5.html"));
//        seg2.setSimilarFiles(simList);
//    	
//        seg2.startPageSegmentation();
//        seg2.colorSegments();
//        
//        System.out.println("all: "+seg2.getAllSegments().size());
//        System.out.println("RED: "+seg2.getSpecificSegments(Segment.Color.RED).size());
//        System.out.println("LIGHTRED: "+seg2.getSpecificSegments(Segment.Color.LIGHTRED).size());
//        System.out.println("REDYELLOW: "+seg2.getSpecificSegments(Segment.Color.REDYELLOW).size());
//        System.out.println("YELLOW: "+seg2.getSpecificSegments(Segment.Color.YELLOW).size());
//        System.out.println("GREENYELLOW: "+seg2.getSpecificSegments(Segment.Color.GREENYELLOW).size());
//        System.out.println("LIGHTGREEN: "+seg2.getSpecificSegments(Segment.Color.LIGHTGREEN).size());
//        System.out.println("GREEN: "+seg2.getSpecificSegments(Segment.Color.GREEN).size());
//	    
//        System.out.println("0.0-0.3: "+seg2.getSpecificSegments(0.0, 0.3).size());
//        System.out.println("0.3-0.6: "+seg2.getSpecificSegments(0.3, 0.6).size());
//        System.out.println("0.0-0.6: "+seg2.getSpecificSegments(0.0, 0.6).size());
//        System.out.println("0.95-1.0: "+seg2.getSpecificSegments(0.95, 1.0).size());
//
//        System.out.println("Mutual-RED: "+seg2.makeMutual(seg2.getSpecificSegments(Segment.Color.RED),1).size());
//        System.out.println("Mutual-YELLOW: "+seg2.makeMutual(seg2.getSpecificSegments(Segment.Color.YELLOW),1).size());
//        System.out.println("Mutual-0.7-0.8: "+seg2.makeMutual(seg2.getSpecificSegments(0.7, 0.8),1).size());


        
        
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
        
        //List nodeList = XPathHelper.getNodes(d, st1);
        //Node n2 = (Node) nodeList.get(0);
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
