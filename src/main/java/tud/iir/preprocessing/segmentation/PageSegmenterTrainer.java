package tud.iir.preprocessing.segmentation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Document;

import tud.iir.extraction.PageAnalyzer;
import tud.iir.web.Crawler;

/**
 * The PageSegmenterTrainer is needed for the evaluation of the class PageSegmenter.
 * 
 * @author Silvio Rabe
 *
 */
public class PageSegmenterTrainer {

    /**
     * Evaluation help function for the similarity check of documents with specific parameters.
     * Checks the similarity of an given document with a collection of other documents based
     * on specific parameters for q-gram-number and q-gram length.
     * 
     * It creates a detailed xls-files with similarity values per documents and saves it in the
     * folder of given documents.
     * 
     * @param orgURL The URL that needs to be compared.
     * @param place The local folder where the documents to compare can be found.
     * @param numberOfQgrams The number of q-grams to use.
     * @param lengthOfQgrams The length of q-grams to use.
     */
    public static void performDetailedParameterCheckForGivenValues(String orgURL, String place, int numberOfQgrams, int lengthOfQgrams) throws MalformedURLException, IOException {
    	
    	PageSegmenter seg = new PageSegmenter();
    	
    	File files[] = PageSegmenterHelper.readURLsFromDisc(place);
		Map<String, Integer> page1 = seg.createFingerprintForURL(orgURL, numberOfQgrams, lengthOfQgrams);
        
		//FileWriter doc1 = new FileWriter(place+"results.txt");
		BufferedWriter doc1 = new BufferedWriter(new FileWriter(place+"results_"+numberOfQgrams+"_"+lengthOfQgrams+".xls"));
        doc1.write("Original file: "+orgURL);
        doc1.newLine();
        doc1.newLine();
        doc1.write("Similarity\tJaccard\tAverage\tFilename");
        doc1.newLine();
        //doc1.write("----------\t----------\t----------");
        doc1.newLine();

        /*doc1.write("testing\ttesting2"+"\n"+"testing3");
        doc1.newLine();
        doc1.write("t\te");
        doc1.newLine();*/
    	
    	for (int i = 0; i < files.length; i++){
            Map<String, Integer> page2 = seg.createFingerprintForURL(files[i].toString(), numberOfQgrams, lengthOfQgrams);
    		System.out.println(page2);
    		
            Double vari = (Math.round((1-SimilarityCalculator.calculateSimilarity(page1, page2))*100))/100.0;
            Double jacc = (Math.round((SimilarityCalculator.calculateJaccard(page1, page2))*100))/100.0;
            
            Double aver = (Math.round((vari+jacc)/2*100))/100.0;
            
            System.out.println("vari: "+vari+"   jacc: "+jacc+"   aver: "+aver);
            //System.out.println(files[i]+"\n"+place);
            
            doc1.write(vari+"\t"+jacc+"\t"+aver+"\t"+files[i].toString().replace(place, ""));
            doc1.newLine();
            
    	}
    	
 	 	doc1.close();
    }

    /**
     * Evaluation help function for the similarity check of documents with specific parameters.
     * Checks the similarity of an given document with a collection of other documents based
     * on specific parameters for q-gram-number and q-gram length.
     * 
     * It calculates an average value of similarity as result.
     * 
     * @param orgURL The URL that needs to be compared.
     * @param place The local folder where the documents to compare can be found.
     * @param numberOfQgrams The number of q-grams to use.
     * @param lengthOfQgrams The length of q-grams to use.
     * @return An average value of similarity for the given parameters.
     */
    public static Double performAverageParameterCheckForGivenValues(String orgURL, String place, int numberOfQgrams, int lengthOfQgrams) throws MalformedURLException, IOException {
    	
    	PageSegmenter seg = new PageSegmenter();
    	
		Double result = 0.00;
		ArrayList<Double> average = new ArrayList<Double>();
	
    	File files[] = PageSegmenterHelper.readURLsFromDisc(place);
		Map<String, Integer> page1 = seg.createFingerprintForURL(orgURL, numberOfQgrams, lengthOfQgrams);
    	
    	for (int i = 0; i < files.length; i++){
            Map<String, Integer> page2 = seg.createFingerprintForURL(files[i].toString(), numberOfQgrams, lengthOfQgrams);
    		//System.out.println(page2);
    		
            Double vari = (Math.round((1-SimilarityCalculator.calculateSimilarity(page1, page2))*100))/100.0;
            Double jacc = (Math.round((SimilarityCalculator.calculateJaccard(page1, page2))*100))/100.0;
            
            Double aver = (Math.round((vari+jacc)/2*100))/100.0;
            
            //System.out.println("vari: "+vari+"   jacc: "+jacc+"   aver: "+aver);
            //System.out.println(files[i]+"\n"+place);
            
            average.add(aver);
            
    	}
    	
    	Double helper = 0.00;
    	for (int i = 0; i < average.size(); i++){
    		helper = helper + (Double)average.get(i);
    	}
    	result=helper/average.size();
    	
    	return result;
    }

    /**
     * Evaluation help function for the similarity check of documents with specific parameters.
     * Checks all combinations of the given parameters and either writes it detailed in several
     * xls-files or prints just the results on the console.
     * 
     * @param orgURL The URL that needs to be compared.
     * @param place The local folder where the documents to compare can be found.
     * @param numberOfQgrams An array of the amounts of q-grams to check.
     * @param lengthOfQgrams An array of the lengths of q-grams to check.
     * @param detailedCheck True if the result should be detailed printed in xls-files. False if
     * 		  it should just print the results on the console.
     */
    public static void performParameterCheck(String orgURL, String place, int[] numberOfQgrams, int[] lengthOfQgrams, Boolean detailedCheck) throws MalformedURLException, IOException {
  	
    	ArrayList<String> averageValues = new ArrayList<String>();    	
    	
    	for (int i = 0; i < numberOfQgrams.length; i++){
    		    		
        	for (int j = 0; j < lengthOfQgrams.length; j++){
        	        		
        		System.out.println("number: "+numberOfQgrams[i]+", length: "+lengthOfQgrams[j]);
        	
                if (detailedCheck) performDetailedParameterCheckForGivenValues(orgURL, place, numberOfQgrams[i], lengthOfQgrams[j]);
                else {
                	Double result=performAverageParameterCheckForGivenValues(orgURL, place, numberOfQgrams[i], lengthOfQgrams[j]);
                    averageValues.add("["+numberOfQgrams[i]+"]["+lengthOfQgrams[j]+"] "+result);
                }
        		
        	}
    		
    	}

    	for (int i = 0; i < averageValues.size(); i++){
    		System.out.println(averageValues.get(i));
    	}

    }
    
    /**
     * Saves the content of an URL to local disc
     * 
     * @param URL The URL of the site to save
     * @param place The filename of the site to save
     */
    public static void saveURLToDisc(String URL, String place) throws TransformerFactoryConfigurationError, TransformerException, IOException{
       
    	URL url = new URL(URL);
        BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
        String line = "";//bufferedreader.readLine();
        BufferedWriter doc1 = new BufferedWriter(new FileWriter("C:\\Users\\Silvio\\Documents\\"+place+".html"));

        
        System.out.println("geht los-----");
        while((line = bufferedreader.readLine()) != null) {
	        //System.out.println(line);
	        doc1.write(line);
	        doc1.newLine();
        
        }
        bufferedreader.close();
        doc1.close();
        
    }
    
    /**
     * Saves all the linked URLs of the domain of the given URL to local disc.
     * It distinguishes between probably similar and probably not similar documents based on the URL.
     * 
     * @param URL The given URL
     * @param limit The limit of URLs to save
     */
    public static void saveAllURLsToDisc(String URL, int limit) throws TransformerFactoryConfigurationError, TransformerException, IOException {
		Crawler c = new Crawler();
        String domain = c.getDomain(URL);
        Document d = c.getWebDocument(domain);

        HashSet<String> te = new HashSet<String>();
        te=c.getLinks(d,true, false,"");
		System.out.println(te.size()+" intern verlinkte URLs gefunden!");
        System.out.println(te);
    	
        Iterator<String> it=te.iterator();
        String currentElement="";
        String place="";
        String label="";
        int count=0;
        
    	String title="";
    	String labelOfURL=PageSegmenterHelper.getLabelOfURL(URL);
        
        while (it.hasNext() && count<limit) {
        	currentElement=(String) it.next();
        	System.out.println(currentElement);
 
        	label=PageSegmenterHelper.getLabelOfURL(currentElement);
        	title=c.getCleanURL(currentElement);
        	
        	title=title.replace("/","_");
        	title=title.replaceAll("[[^\\w\\däüöÄÜÖ\\+\\- ]]", "_");

        	System.out.println(title+"\n"+label);
        	
        	if (labelOfURL.equals(label)
        			/*&& URL.length()>=currentElement.length()-1 && URL.length()<=currentElement.length()+1*/) {
        		place="test\\aehnlich\\"+title;
        		System.out.println("-->ähnlich");
        		//System.out.println(URL+"-------"+currentElement);
        		//System.out.println(URL.length()+" zu "+currentElement.length());
        	}
        	else {
        		place="test\\unaehnlich\\"+title;
        		System.out.println("-->nicht ähnlich");
        	}
        	saveURLToDisc(currentElement, place);
        
        	count++;
        }
    	
    }

    /**
     * Saves defined URLs to local disc.
     */
    public static void saveChosenURLsToDisc() throws TransformerFactoryConfigurationError, TransformerException, IOException {
    	
    	String[] collectionOfURL={
        		"http://www.wer-weiss-was.de",
        		"http://www.wikipedia.de",
        		"http://www.google.de",
        		"http://www.youtube.com",
        		"http://www.wetter.com",
        		"http://www.wissen.de",
        		"http://dict.leo.org",
        		"http://www.juraforum.de",
        		"http://www.tomshardware.de",
        		"http://www.treiber.de",
        		"http://www.pixelquelle.de",
        		"http://www.ebay.de",
        		"http://www.expedia.de",
        		"http://www.expedia.de/last-minute/default.aspx",
        		"http://cgi.ebay.de/ws/eBayISAPI.dll?ViewItem&item=220680636640",
        		"http://www.treiber.de/treiber-download/Anchor-Datacomm-updates",
        		"http://www.amazon.com",
        		"http://www.amazon.com/s/ref=nb_sb_noss?url=search-alias%3Daps&field-keywords=mouse",
        		"http://wissen.de/wde/generator/wissen/ressorts/geschichte/was_geschah_am/index.html?day=13&month=10&year=1900&suchen=Suchen",
        		"http://maps.google.de"        	
    	};
    	
    	for (int i=0; i<collectionOfURL.length; i++) {
    		String title=collectionOfURL[i].substring(7);
    		title=title.replace("/","_");
        	title=title.replaceAll("[[^\\w\\däüöÄÜÖ\\+\\- ]]", "_");
    		
        	saveURLToDisc(collectionOfURL[i], "test_2\\unaehnlich2\\"+title);
        	System.out.println(title+" erfolgreich!");
    	}
    	
    }

	
	
	
	
}
