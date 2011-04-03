package ws.palladian.daterecognition.technique;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import ws.palladian.daterecognition.DateConverter;
import ws.palladian.daterecognition.DateGetterHelper;
import ws.palladian.daterecognition.KeyWords;
import ws.palladian.daterecognition.dates.ContentDate;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.dates.StructureDate;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.html.HTMLHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * This class extracts all dates out of the content of webpages. <br>
 * For more than one use, use {@link ContentDateGetter#reset()} to avoid errors and mistakes. <br>
 * Otherwise list will not be cleared and you get {@link OutOfMemoryError}.
 * @author Martin Gregor
 * 
 */
public class ContentDateGetter extends TechniqueDateGetter<ContentDate> {

	
	
	/**
	 * Stores all keywords with index, that are within document's text . 
	 */
	private HashMap<Integer, String> keyContentMap = new HashMap<Integer, String>();
	/**
	 * Stores a node and its keyword.
	 */
	private HashMap<Node, String> keyAttrMap = new HashMap<Node, String>();
	/**
	 * Stores the position after in a document found text. <br>
	 * Used to restart search after an already found text. 
	 */
	private HashMap<String, Integer> nodeIndexMap = new HashMap<String, Integer>();
	private String doc; 
	
	
	private HashMap<Node, StructureDate> structDateMap = new HashMap<Node, StructureDate>();
	private HashMap<Node, Boolean> lookedUpNodeMap = new HashMap<Node, Boolean>(); 
	
    @Override
    public ArrayList<ContentDate> getDates() {
        ArrayList<ContentDate> result = new ArrayList<ContentDate>();
        if (document != null) {
            result = getContentDates(this.document);
            //DataSetHandler.writeDateFactors(result, url, doc);
        }
        return result;
    }
   
    /**
     * Get dates of text-nodes of body part of document.
     * 
     * @param document Document to be searched.
     * @return List of dates.
     */
    private ArrayList<ContentDate> getContentDates(Document document) {
    	ArrayList<ContentDate> dates = new ArrayList<ContentDate>();
    	List<Node> nodeList = XPathHelper.getNodes(document, "//text()");
    	if(!nodeList.isEmpty()){
    		NodeList body = document.getElementsByTagName("body");
    		
    		//Get webpage as text (for finding position).
    		this.doc = StringHelper.removeDoubleWhitespaces(
    				HTMLHelper.replaceHTMLSymbols(
    						HTMLHelper.documentToReadableText(body.item(0))));
    		
    		/*
    		 * Prepare Pattern for faster matching.
    		 * Only regExps.length. Not (regExps.length)*(nodeList.size) [n < n*m] 
    		 */
    		Object[] regExps = RegExp.getAllRegExp();
        	Pattern[] pattern = new Pattern[regExps.length];
        	Matcher[] matcher = new Matcher[regExps.length];
        	for(int i = 0; i < regExps.length; i++){
        		pattern[i] = Pattern.compile(((String[])regExps[i])[0]);
        		matcher[i] = pattern[i].matcher("");
        	}
        	
        	setDocKeywords();
        	
    		for(int i = 0; i< nodeList.size(); i++){
    			if (nodeList.get(i).getNodeType() == Node.TEXT_NODE) {
    				Node node = nodeList.get(i);
    				Node parent = node.getParentNode();
    				if(parent.getNodeType() != Node.COMMENT_NODE 
    						&& !parent.getNodeName().equalsIgnoreCase("script") 
    						&& !parent.getNodeName().equalsIgnoreCase("style")){
    					dates.addAll(checkTextnode((Text) node ,matcher, regExps));
    				}
    	        }
    		}
    	}
    	
        
        return dates;
    }
   
   /**
     * Find a date in text of node.<br>
     * Node as to be a {@link Text}.
     * 
     * @param node Text-node to be searched.
     * @param doc Whole human readable document (displayed content) as string to get position of found dates.
     * @param depth Depth of node in document structure.
     * @return
     */
    private ArrayList<ContentDate> checkTextnode(Text node, Matcher[] matcher, Object[] regExps) {
    	
    	
    	
        String text = StringHelper.removeDoubleWhitespaces(HTMLHelper.replaceHTMLSymbols(node.getNodeValue()));
        int index = -1;
        Node parent = node.getParentNode();
        Node tag = parent;
        
        while (HTMLHelper.isSimpleElement(parent)) {
            parent = parent.getParentNode();
        }
        
        ArrayList<ContentDate> returnDates = new ArrayList<ContentDate>();
        ArrayList<ContentDate> dateList = DateGetterHelper.findALLDates(text, matcher, regExps);
        
       if (dateList.size() > 0) {
        	Integer beginIndex = nodeIndexMap.get(text);
        	if(beginIndex == null){
        		beginIndex = -1;
        	}
            index = this.doc.indexOf(text, beginIndex);
            if(index != -1){
            	nodeIndexMap.put(text, index + text.length());
            }
            //System.out.println(index);
        }
        
       for(ContentDate date : dateList){
    	  
    	   
    	   date.setStructureDate(getStructureDate(tag, matcher, regExps));
    	   
    	   if(date.getStructureDate() == null && tag != parent){
    		   date.setStructureDate(getStructureDate(parent, matcher, regExps));
    	   }
    	   
    	   boolean keyword3Class = true;
			
    	   date.setTagNode(parent.toString());
    	   date.setTag(tag.getNodeName());
    	   date.setNode(tag);
			
    	   if (index != -1) {
    		   date.set(ContentDate.DATEPOS_IN_DOC, index + date.get(ContentDate.DATEPOS_IN_TAGTEXT));
    	   }
			
    	   //String keyword = DateGetterHelper.findNodeKeywordPart(tag, KeyWords.BODY_CONTENT_KEYWORDS_FIRST);
    	   String keyword = getNodeKeyword(tag);
			
    	   if(keyword.equals("") && tag != parent){
    		   keyword = getNodeKeyword(parent);
    	   }
			
    	   if(!keyword.equals("")){
				
    		   keyword3Class  = KeyWords.getKeywordPriority(keyword) == KeyWords.OTHER_KEYWORD ;
    		   date.set(ContentDate.KEYWORDLOCATION, ContentDate.KEY_LOC_ATTR);
    	   }
			
    	   if(keyword.equals("") ||  keyword3Class ){
    		   setClosestKeyword(date);
    		   if(date.getKeyword() != null){
    			   date.set(ContentDate.KEYWORDLOCATION, ContentDate.KEY_LOC_CONTENT);
    			   keyword = date.getKeyword();
    		   }
    	   }
			
    	   if(!keyword.equals("")){
    		   date.setKeyword(keyword);
    	   }
    	   returnDates.add(date);
       }
       return returnDates;
    }
    
    
    /**
     * Finds all content-keywords in a text. <br>
     * Returns a hashmap with keyword-index as key an keyword as value.
     * 
     * @param doc Text to be searched.
     * @return Hashmap with indexes and keywords.
     */
    private void setDocKeywords(){
    	if(this.doc != null){
    		this.keyContentMap = new HashMap<Integer, String>();
    		String text = this.doc.toLowerCase();
	    	String[] keywords = KeyWords.BODY_CONTENT_KEYWORDS_ALL;
	    	int index;
	    	for (int i=0; i< keywords.length; i++){
	    		String key = keywords[i];
	    		index = text.indexOf(key);
	    		if(index != -1){
	    			this.keyContentMap.put(index, key);
	    			text = text.replaceFirst(key, DateGetterHelper.getXs(key));
	    			i--;
	    		}	
	    	}
    	}
    }
    
    private void setClosestKeyword(ContentDate date){
    	String keyword = null;
    	String keywordBefore;
    	String keywordAfter;
    	int indexBefore; 
    	int indexAfter;
    	int subStart = 0;
    	int subEnd = 0;
    	int datePos = date.get(ContentDate.DATEPOS_IN_DOC);
    	
    	if(datePos >= 0){
    		
    		for(int i=1; i < 151; i++){
    			indexBefore = datePos - i;
    			indexAfter = datePos + i;
    			
				keywordBefore = this.keyContentMap.get(indexBefore);
				if(keywordBefore != null){
					keyword = keywordBefore;
					subStart = indexBefore + keywordBefore.length();
					subEnd = datePos;
					break;
    			}
    			
    			keywordAfter = this.keyContentMap.get(indexAfter);
    			if(keywordAfter != null){
    				keyword = keywordAfter;
    				subStart = datePos + date.getDateString().length();
					subEnd = indexAfter;
    				break;
    			}
    			
    		}
    		if(keyword != null){
    			date.setKeyword(keyword);
    			int diff = StringHelper.countWhitespaces(this.doc.substring(subStart, subEnd));
    			date.set(ContentDate.DISTANCE_DATE_KEYWORD, diff);
    		}
    	}
    }
    
    private String getNodeKeyword(Node node){
    	String keyword = this.keyAttrMap.get(node);
    	if(keyword == null){
    		keyword = findNodeKeyword(node);
    		if(keyword ==  null){
    			keyword = "";
    		}
    		this.keyAttrMap.put(node, keyword);
    	}
    	return keyword;
    }
    
    private String findNodeKeyword(Node node){
    	String returnValue = null;
    	Node tempNode = node.cloneNode(false);
    	String nodeText = HTMLHelper.getXmlDump(tempNode);
    	String[] keywords = KeyWords.BODY_CONTENT_KEYWORDS_ALL;
    	for(int i=0; i< keywords.length; i++){
    		String keyword = keywords[i];
    		if(nodeText.indexOf(keyword) != -1){
    			returnValue = keyword;
    			break;
    		}
    		
    	}
    	return returnValue;    
    }
    @Override
    public void reset(){
    	this.doc = null;
    	this.document = null;
    	this.keyAttrMap = new HashMap<Node, String>();
    	this.keyContentMap = new HashMap<Integer, String>();
    	this.nodeIndexMap = new HashMap<String, Integer>();
    	this.lookedUpNodeMap = new HashMap<Node, Boolean>();
    	this.structDateMap = new HashMap<Node, StructureDate>();
    }
    
    
    private StructureDate getStructureDate(Node node, Matcher[] matcher, Object[] regExps){
    	Boolean hasDate = lookedUpNodeMap.get(node);
    	StructureDate date;
    	
    	if(hasDate == null){
    		date = findStructureDate(node, matcher, regExps);
    		lookedUpNodeMap.put(node, true);
    		structDateMap.put(node, date);
    	}else{
    		date = structDateMap.get(node);
    	}
    	
    	return date;
    }
    
    private StructureDate findStructureDate(Node node, Matcher[] matcher, Object[] regExps){
    	StructureDate structDate = null;
    	ExtractedDate date = null;
    	    	
    	NamedNodeMap attributes = node.getAttributes();
    	for(int i=0; i<attributes.getLength(); i++){
    		Node attr = attributes.item(i);
    		if(!attr.getNodeName().equalsIgnoreCase("href")){
    			date = DateGetterHelper.findDate(attr.getNodeValue(), matcher, regExps);
    			if(date != null){
    				
    				break;
    			}
    		}
    	}
    	
    	if(date != null){
	    	String keyword = getNodeKeyword(node);
	    	structDate = DateConverter.convert(date, DateConverter.TECH_HTML_STRUC);
	    	structDate.setKeyword(keyword);
	    	structDate.setNode(node);
    	}
    	return structDate;
    }
    
    public String getDoc(){
    	return this.doc;
    }
    
    public HashMap<Node, Boolean> getLookUpNodeMap(){
    	return this.lookedUpNodeMap;
    }
    public HashMap<Node, StructureDate> getStructDateMap(){
    	return this.structDateMap;
    }
    public HashMap<Node, String> getKeyAttrMap(){
    	return this.keyAttrMap;
    }
}


