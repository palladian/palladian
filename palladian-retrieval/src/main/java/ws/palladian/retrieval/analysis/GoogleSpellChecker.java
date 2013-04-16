package ws.palladian.retrieval.analysis;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>Spell checks and auto-corrects text using the Google spell checker.</p>
 * @author David Urbansky
 * 
 */
public class GoogleSpellChecker {

	/**
	 * <p>Check if a text contains errors.</p>
	 * @param text The text to check for errors.
	 * @return True if errors were found, false otherwise.
	 */
	public boolean containsErrors(String text) {
		return !getCorrectionSuggestions(text).isEmpty();
	}
	
	/**
	 * <p>Automatically detect and correct spelling mistakes.</p>
	 * @param text The text to check for errors.
	 * @return The auto-corrected text.
	 */
	public String autoCorrect(String text) {
		Map<String, String> correctionSuggestions = getCorrectionSuggestions(text);
		for (Entry<String, String> suggestion : correctionSuggestions.entrySet()) {
			text = text.replace(suggestion.getKey(), suggestion.getValue());
		}
		return text;
	}
	
	/**
	 * <p>Return suggestions for correction.</p>
	 * @param text The text to check for errors.
	 * @return The suggestions.
	 */
	public Map<String, String> getCorrectionSuggestions(String text) {
		Map<String,String> correctionMap = new HashMap<String, String>();
		
		Document document = getDocument(text);
		
		Set<String> errorWords = new HashSet<String>();
        List<Node> correctionNodes = XPathHelper.getNodes(document, "//c");
        for (Node node : correctionNodes) {
            double confidence = Double.parseDouble(node.getAttributes().getNamedItem("s").getTextContent());
            String correctionString = node.getTextContent();
            String bestCorrection = correctionString.split("\t")[0];
			if (confidence > 0.99 && !errorWords.contains(correctionString)) {
                errorWords.add(correctionString);
//                System.out.println(correctionString + " => " + node.getAttributes().getNamedItem("s").getTextContent());
                int offset = Integer.valueOf(node.getAttributes().getNamedItem("o").getTextContent());
                int length = Integer.valueOf(node.getAttributes().getNamedItem("l").getTextContent());
				String wrongWord = text.substring(offset, offset+length);
//				System.out.println(wrongWord + " => " + bestCorrection);
                
                correctionMap.put(wrongWord, bestCorrection);
            }
        }
        
//        CollectionHelper.print(correctionMap.entrySet());
		
		return correctionMap;
	}
	
	
	private Document getDocument(String text) {
		Document document = null;
		
		OutputStreamWriter out = null;
        InputStream in = null;
        try {
            // don't do spell checking on uppercase words (mostly entities)
            text = StringEscapeUtils.unescapeHtml(text);
            text = text.replace("’", "'");
            text = text.replace("”", "");
            text = text.replace("“", "");
            text = text.replace("&", "");

            // format the XML that needs to be send to Google Spell Checker
            StringBuilder requestXML = new StringBuilder();
            requestXML.append("<spellrequest textalreadyclipped=\"0\"" + " ignoredups=\"1\""
                    + " ignoredigits=\"1\" ignoreallcaps=\"1\"><text>");
            requestXML.append("<![CDATA[");
            requestXML.append(text);
            requestXML.append("]]>");
            requestXML.append("</text></spellrequest>");

            // the Google Spell Checker URL
            URL url = new URL("https://www.google.com/tbproxy/spell?lang=en&hl=en");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);

            out = new OutputStreamWriter(conn.getOutputStream());
            out.write(requestXML.toString());
            out.close();

            // get the result from Google Spell Checker
            in = conn.getInputStream();

            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
            String resultString = HtmlHelper.xmlToString(document, true);
            System.out.println(resultString);
            if (resultString.contains("error=\"1")) {
                throw new RuntimeException("got an error from Google's spell checker");
            }

        } catch (Exception e) {
        	throw new RuntimeException(e.getMessage());
        } finally {
            FileHelper.close(in, out);
        }
        
        return document;
	}
	
    public static void main(String[] args) {
//    	System.out.println(new GoogleSpellChecker().autoCorrect("this is aobout a laamp with lightt and muchc mor"));
//        System.out.println(new GoogleSpellChecker().autoCorrect("apocalypsexx"));
        System.out.println(new GoogleSpellChecker().autoCorrect("Thas is hoow the etxt is sopossed to be"));
    }
}
