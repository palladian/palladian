package tud.iir.extraction.content;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.w3c.dom.Document;

import tud.iir.helper.HTMLHelper;
import tud.iir.helper.XPathHelper;
import tud.iir.web.Crawler;

/**
 * The SimilarityCalculator provides functions to calculate the similarity between texts, DOM-nodes 
 * and whole documents.
 * 
 * @author Silvio Rabe
 *
 */
public class SimilarityCalculator {

    /**
     * Calculates the similarity between two documents by counting their tag-q-grams. 
     * 
     * @param page1 Map of q-grams of the given document.
     * @param page2 Map of q-grams of the document to compare.
     * @return The similarity value between 0 and 1.
     */
    public static double calculateSimilarity(Map<String, Integer> page1, Map<String, Integer> page2) {
    
    /*
     * schauen ob key vorhanden
     * value vergleichen, nur abweichung speichern
     * 		wenn gleich = Abweichung ist 0
     * 		ansonsten prozentual: 10 zu 9 abweichung 10%; 600 zu 300 abweichung 50%
     * 		durchschnitt aller abweichungen?
     * grenzen für die abweichung festlegen (bis 20% noch gleich?)
     * 
     */
    	double result=0;
    	List<Double> variance=new ArrayList<Double>();
    	String key="";
    	
        Iterator<String> it=page1.keySet().iterator();
        while (it.hasNext()) {
            key=(String) it.next();
        	//System.out.println(key);

        	//If both maps contain same key, exermine if there is a difference in the value
        	if (page2.keySet().contains(key)) {
        		//Calculate the difference in the value
            	if (page2.get(key)==page1.get(key)) {
            		//System.out.println("------------------gleich oft");
            		variance.add(new Double(0));
            	}
            	else {
            		//int value=((Integer) page1.get(key)).intValue();
            		//int value2=((Integer) page2.get(key2)).intValue();
            		Integer value=((Integer) page1.get(key));//*tags1;
            		Integer value2=((Integer) page2.get(key));//*tags2;
            		//System.out.print(value+" "+value2+" ");
            		
            		double d=0;
            		if (value>value2) d = (double) value2/value;
            		if (value<value2) d = (double) value/value2;
            		d=1-d;
            		//System.out.println(d);
            		variance.add(d);
            	}
            }
        	//if both maps do not contain the same key
        	else {
        		//System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++nicht gleich");
        		variance.add(new Double(1));
        	}        	
        }

    	//Evaluation
    	//System.out.println("---------------------------");
    	System.out.println(variance);
    	
    	double countUp=0;
    	Iterator<Double> it3=variance.iterator();
        while (it3.hasNext()) {
        	countUp=countUp+((Double)it3.next());
        }
        result=countUp/variance.size();
    	//System.out.println("Durchschnittlich: "+result);

    	return result;
    }
    
    /**
     * Calculates the Jaccard value for two documents by comparing their tags.
     * 
     * @param page1 The given document.
     * @param page2 The document to compare.
     * @return The Jaccard value between 0 and 1.
     */
    public static double calculateJaccard(Map<String, Integer> page1, Map<String, Integer> page2) {
    	double result=0;
    	
    	Map<String, Integer> helperMap=new HashMap<String, Integer>(page1);
    	
    	//System.out.println("1:"+helperMap.size());
    	helperMap.keySet().retainAll(page2.keySet());
    	//System.out.println("2:"+helperMap.size());
    	
    	int z1=helperMap.size();
    	
    	//helperMap=page1;
    	
    	Set<String> s1=new HashSet<String>(page1.keySet());
    	Set<String> s2=new HashSet<String>(page2.keySet());
    	//System.out.println(s1+"\n"+s2);

    	s1.addAll(s2);

    	int z2=s1.size();
    	
    	result=(double) z1/z2;
    	
    	//System.out.println("z1="+z1+" z2="+z2+" Jaccard="+result);
    	
    	return result;
    }

    /**
     * Calculates a similarity value for a specific node based on its difference in several
     * documents. It takes the node out of all documents and compares each node with each
     * other node. The comparison is based on the jaccard similarity over the content of
     * the nodes. 
     * 
     * @param list A list of similar documents inclusive the original document.
     * @param xPath The xpath to the node to compare in all documents.
     * @return A value of similarity.
     */
	public static double calculateSimilarityForNode(ArrayList<Document> list, String xPath) {
		double result=0.0;
		ArrayList<Map<String, Integer>> listOfNodeLines = new ArrayList<Map<String, Integer>>();
		
        //System.out.println(list.size()+" documents in list.");
        Iterator<Document> it = list.iterator();
        while (it.hasNext()) {
            Document doc = (Document)it.next();
            //System.out.println();
            
            String simNode = HTMLHelper.htmlToString(XPathHelper.getNode(doc,xPath));
            
            Map<String, Integer> nodeLines = new LinkedHashMap<String, Integer>();
            StringTokenizer st = new StringTokenizer(simNode, "\n");

            while ( st.hasMoreTokens() ) {String line = st.nextToken(); nodeLines.put(line, 0);}
            listOfNodeLines.add(nodeLines);       
        }
        
        ArrayList<Double> allJaccAverage = new ArrayList<Double>();
        for (int i=0; i<listOfNodeLines.size(); i++) {
        	Map<String, Integer> currentNodeLines=(Map<String, Integer>) listOfNodeLines.get(i);
        	//System.out.println("1-"+currentNodeLines);
        	ArrayList<Double> jaccArray = new ArrayList<Double>();
        	double jaccAverage=0.0;
        	
            for (int j=0; j<listOfNodeLines.size(); j++) {
            	Map<String, Integer> compareNodeLines=(Map<String, Integer>) listOfNodeLines.get(j);
            	//System.out.println("2------"+compareNodeLines);
            	
	            if (currentNodeLines!=compareNodeLines) {
			        Double jacc = calculateJaccard(currentNodeLines, compareNodeLines);
			        if (jacc.isNaN()) jacc=0.0;
			        //System.out.println("Jacc="+jacc);
			        jaccArray.add(jacc);
			        //System.out.println(jaccArray);
	            }
            }
            
            for (int j=0; j<jaccArray.size(); j++) {
            	jaccAverage=jaccAverage+(Double)jaccArray.get(j);
            }
            jaccAverage=jaccAverage/jaccArray.size();
            //System.out.println("Durchschnittswert="+jaccAverage);
            allJaccAverage.add(jaccAverage);
        }
		
        for (int j=0; j<allJaccAverage.size(); j++) {
        	result=result+(Double)allJaccAverage.get(j);
        }
        result=result/allJaccAverage.size();
        
		return result;
	}

	/**
	 * Calculates similarity values for all conflict nodes of a document. 
	 * 
	 * @param docu The original document.
	 * @param conflictNodes A list of its conflict nodes.
	 * @param similarFiles A list of similar documents.
	 * @return A map of all conflict nodes combined with its similarity values.
	 */
	public static Map<String, Double> calculateSimilarityForAllNodes(Document docu, ArrayList<String> conflictNodes, Map<String, Double> similarFiles) throws MalformedURLException, IOException {
		
		Crawler c = new Crawler();

        Map<String, Double> similarityOfNodes = new LinkedHashMap<String, Double>();
        Map<String, Double> testMap = new LinkedHashMap<String, Double>(); 
        
        /*testMap.put("http://www.informatikforum.de/forumdisplay.php?f=39", "0.9485261961871161");
        testMap.put("http://www.informatikforum.de/forumdisplay.php?f=31", "0.9698535597228519");
        testMap.put("http://www.informatikforum.de/forumdisplay.php?f=27", "0.9693909965972903");
        testMap.put("http://www.informatikforum.de/forumdisplay.php?f=98&daysprune=-1&order=desc&sort=replycount", "0.9629184384605889");
        testMap.put("http://www.informatikforum.de/forumdisplay.php?f=98&daysprune=-1&order=desc&sort=voteavg", "0.9573928892042225");
        */
        
        //testMap = findSimilarFiles(docu.getDocumentURI(), 5000, 9, 0.689, 5);
        testMap = similarFiles;
        System.out.println("--------------------URI:"+docu.getDocumentURI());
        
        
        //ArrayList conflictNodes = markLeafs3("http://www.informatikforum.de/forumdisplay.php?f=98", "http://www.informatikforum.de/forumdisplay.php?f=31", "C:\\Users\\Silvio\\Documents\\doc2.html", 30);
        //System.out.println(conflictNodes.size());
        

        ArrayList<Document> listOfSimilarDocuments = new ArrayList<Document>();
        Iterator it = testMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            Document doc = c.getWebDocument((String) pairs.getKey());
            listOfSimilarDocuments.add(doc);
        }
        
        ArrayList<Document> listOfSimilarDocumentsIncOrg = new ArrayList<Document>(listOfSimilarDocuments);
        listOfSimilarDocumentsIncOrg.add(docu);
        
        
        for (int i=0; i<conflictNodes.size(); i++) {
	        String path = (String) conflictNodes.get(i);
      	        
            similarityOfNodes.put(path, calculateSimilarityForNode(listOfSimilarDocumentsIncOrg, path));
            //System.out.println("Durchschnittswert multi="+similarityOfNodes);
	        
	        //System.out.println("--------------");

        }
        
        System.out.println("FERTIG");
        
        it = similarityOfNodes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            System.out.println(pairs.getKey()+" = "+pairs.getValue());
        }

		return similarityOfNodes;
	}

	
	
	
}
