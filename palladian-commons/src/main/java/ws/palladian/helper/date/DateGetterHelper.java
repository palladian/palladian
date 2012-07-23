package ws.palladian.helper.date;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;

import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.dates.ContentDate;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.nlp.StringHelper;

/**
 * DateGetterHelper provides the techniques to find dates out of webpages. Also provides different helper methods.
 * 
 * @author Martin Gregor
 * 
 */
public final class DateGetterHelper {

    /**
     * Tries to match a date in a dateformat. The format is given by the regular expressions of RegExp.
     * 
     * @param dateString a date to match.
     * @return The found date, defined in RegExp constants. <br>
     *         If no match is found return <b>null</b>.
     */
    public static ExtractedDate findDate(String dateString) {

        return findDate(dateString, null);
    }

    /**
     * Tries to match a date in a dateformat. The format is given by the regular expressions of RegExp.
     * 
     * @param dateString a date to match.
     * @param regExpArray regular expressions of dates to match. If this is null {@link RegExp}.getAllRegExp will be
     *            called.
     * @return The found date, defined in RegExp constants. <br>
     *         If no match is found return <b>null</b>.
     */
    public static ExtractedDate findDate(final String dateString, Object[] regExpArray) {
    	ExtractedDate date = null;
    	Object[] regExps = regExpArray;

    	if (regExps == null) {
            regExps = RegExp.getAllRegExp();
        }

        for (int i = 0; i < regExps.length; i++) {
            // FIXME "Mon, 18 Apr 2011 09:16:00 GMT-0700" fails.
            try {
            date = getDateFromString(dateString, (String[]) regExps[i]);
            } catch (Throwable th) {
                th.printStackTrace();
            }
            if (date != null) {
                break;
            }
        }
        return date;
    }
    public static List<ContentDate> findAllDates(String text) {
        return findAllDates(text, false);
    }

    public static List<ContentDate> findAllDates(String text, boolean includeYearOnly) {
        Object[] regExps = RegExp.getAllRegExp();

        // try to catch numbers that might be year mentions
        if (includeYearOnly) {
            String[] yearRegExp = RegExp.DATE_CONTEXT_YYYY;
            List<Object> newArray = new ArrayList<Object>();
            for (Object regExp : regExps) {
                newArray.add(regExp);
            }
            newArray.add(yearRegExp);
            regExps = newArray.toArray();
        }

        Pattern[] pattern = new Pattern[regExps.length];
        Matcher[] matcher = new Matcher[regExps.length];
        for (int i = 0; i < regExps.length; i++) {
            pattern[i] = Pattern.compile(((String[])regExps[i])[0]);
            matcher[i] = pattern[i].matcher("");
        }
        return findAllDates(text, matcher, regExps);
    }

    /**
     * 
     * @param dateString a date to match.
     * @return The found format, defined in RegExp constants. <br>
     *         If no match is found return <b>null</b>.
     */
    public static List<ContentDate> findAllDates(String text, Matcher[] matcher, Object[] regExps) {
    	String tempText = text;
    	ArrayList<ContentDate> dates = new ArrayList<ContentDate>();
    	
    	
    	int start;
    	int end;
    	for(int i = 0; i < matcher.length; i++){
    		matcher[i].reset(tempText);
    		while(matcher[i].find()){
				boolean hasPrePostNum = false;
				start = matcher[i].start();
				end = matcher[i].end();
				if (start > 0) {
	                String temp = tempText.substring(start - 1, start);
	                try {
	                    Integer.parseInt(temp);
	                    hasPrePostNum = true;

	                } catch (NumberFormatException e) {
	                	//e.printStackTrace();
	                }
	            }
	            if (end < tempText.length()) {
	                String temp = tempText.substring(end, end + 1);
	                try {
	                    Integer.parseInt(temp);
	                    hasPrePostNum = true;

	                } catch (NumberFormatException e) {
	                	//e.printStackTrace();
	                }
	            }
	            if (!hasPrePostNum) {
	            	try {
		            	String dateString = tempText.substring(start, end);
//                        ContentDate date = DateConverter.convert(new ExtractedDate(dateString,
//                                ((String[])regExps[i])[1]), DateType.ContentDate);
                        
                        ContentDate date = new ContentDate(dateString, ((String[])regExps[i])[1]);
                        
		            	int index = tempText.indexOf(date.getDateString());
		            	date.set(ContentDate.DATEPOS_IN_TAGTEXT, index);
		                String xString = getXs(dateString);
		                tempText = tempText.replaceFirst(dateString, xString);
		                dates.add(date);
	            	} catch (Exception e) {
	            		e.printStackTrace();
	            	}
	            }
    		}
    	}
    	return dates;
    }
  
    /**
     * Find the first date in text. <br>
     * @param text
     * @param matcher
     * @param regExps
     * @return
     */
    public static ExtractedDate findDate(String text, Matcher[] matcher, Object[] regExps) {
    	String tempText = text;
    	ExtractedDate date = null;
    	int start;
    	int end;
    	for(int i = 0; i < matcher.length; i++){
    		matcher[i].reset(tempText);
    		if(matcher[i].find()){
				boolean hasPrePostNum = false;
				start = matcher[i].start();
				end = matcher[i].end();
				if (start > 0) {
	                String temp = tempText.substring(start - 1, start);
	                try {
	                    Integer.parseInt(temp);
	                    hasPrePostNum = true;
	                } catch (NumberFormatException e) {
	                }
	            }
	            if (end < tempText.length()) {
	                String temp = tempText.substring(end, end + 1);
	                try {
	                    Integer.parseInt(temp);
	                    hasPrePostNum = true;
	                } catch (NumberFormatException e) {
	                }
	            }
	            if (!hasPrePostNum) {
	            	String dateString = tempText.substring(start, end);
	            	date = new ExtractedDate(dateString,((String[])regExps[i])[1]);
	                break;
	            }
	            
    		}
    	}
    	return date;
    }
    
//    /**
//     * Returns a string of whitespace as long as the parameter string.
//     * 
//     * @param text
//     * @return String of whitespace.
//     */
//    public static String getWhitespaces(String text) {
//        StringBuffer sb = new StringBuffer();
//        for (int i = 0; i < text.length(); i++) {
//            sb.append("x");
//            // sb.append(" ");
//        }
//        return sb.toString();
//    }

    /**
     * Returns a string of "x"s as long as the parameter string.
     * 
     * @param text
     * @return String of "x"s.
     */
    public static String getXs(String text) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            sb.append("x");
        }
        return sb.toString();
    }

    /**
     * Check a string for keywords. Used to look in tag-values for date-keys.
     * 
     * @param text string with possible keywords.
     * @param keys a array of keywords.
     * @return the found keyword.
     */
    public static String hasKeyword(final String text, final String[] keys) {
        String keyword = null;
        Pattern pattern;
        Matcher matcher;

        for (int i = 0; i < keys.length; i++) {
            pattern = Pattern.compile(keys[i].toLowerCase());
            matcher = pattern.matcher(text.toLowerCase());
            if (matcher.find()) {
                keyword = keys[i];
                break;
            }
        }
        return keyword;
    }

//    /**
//     * Finds out the separating symbol of date-string
//     * 
//     * @param date
//     * @return
//     */
//    public static String getSeparator(final ExtractedDate date) {
//        final String dateString = date.getDateString();
//        return ExtractedDateHelper.getSeparator(dateString);
//    }

    /**
     * 
     * @param string string, which is to be searched
     * @param regExp regular expression for search
     * @param offsetStart is slider for beginning substring (no negative values) - e.g. substring: "abcd" offsetStart=0:
     *            "abcd" offsetStart=1: "bcd" offsetStart=-1: "abcd"
     * @return found substring or null
     */
    public static ExtractedDate getDateFromString(final String dateString, final String[] regExp) {
    	
        String text = StringHelper.removeDoubleWhitespaces(DateGetterHelper.replaceHtmlSymbols(dateString));
        boolean hasPrePostNum = false;
        ExtractedDate date = null;
        Pattern pattern;
        Matcher matcher;
        
        pattern = Pattern.compile(regExp[0]);
        matcher = pattern.matcher(text);

        if (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (start > 0) {
                String temp = text.substring(start - 1, start);
                try {
                    Integer.parseInt(temp);
                    hasPrePostNum = true;
                } catch (NumberFormatException e) {
                }
            }
            if (end < text.length()) {
            	String temp = text.substring(end, end + 1);
            	//If last character is "/" no check for number is needed.
            	if(!text.substring(end-1, end).equals("/")){
            		try {
	                    Integer.parseInt(temp);
	                    hasPrePostNum = true;
	                } catch (NumberFormatException e) {
	                }
            	}
            }
            if (!hasPrePostNum) {
                date = new ExtractedDate(text.substring(start, end), regExp[1]);
            }

        }
        //System.out.println("getDateFromString: " + (double)(endTime - startTime)/1000.0);
        return date;
    }

    
    
//    /**
//     * In opposition to <b>findeNodeKeyword</b> also keywords as part of longer String will be found. <br>
//     * But with condition that keyword is no part of a word. <br>
//     * E.g.: date in timedate will not be found, but there for time-date matches. <br>
//     * (Underscores won't match, e.g. time_date is wrong.)
//     * 
//     * @param node HTML-node to be searched.
//     * @param keyWords Array of keywords to look for.
//     * @return
//     */
//    public static String findNodeKeywordPart(Node node, String[] keyWords) {
//        String keyword = null;
//        //Node tempNode = XPathHelper.removeAllCildren(node);
//        Node tempNode = node.cloneNode(false);
//        String nodeDump =  HtmlHelper.getXmlDump(tempNode);
//        boolean hasKeyword = false;
//        for (int j = 0; j < keyWords.length; j++) {
//        	int index = nodeDump.indexOf(keyWords[j]);
//        	if(index != -1){
//        		hasKeyword = true;
//        	}
//        }
//        NamedNodeMap attrMap = node.getAttributes();
//       
//        if (attrMap != null && hasKeyword) {
//            for (int j = 0; j < keyWords.length; j++) {
//                String lookUp = keyWords[j].toLowerCase();
//                for (int i = 0; i < attrMap.getLength(); i++) {
//                    Node attr = attrMap.item(i);
//                    String attrText = attr.getNodeValue().toLowerCase();
//                    int index = attrText.indexOf(lookUp);
//                    if (index != -1) {
//                        boolean letter = false;
//                        int start = index;
//                        int end = index + lookUp.length();
//
//                        if (start > 0) {
//                        	String sub = attrText.substring(start - 1, start);
//                        	// Check, if char after keyword is [a-zA-Z0-9_]. If so, result is 0, else 1.
//                        	if(sub.split("\\w").length == 0){
//                        		letter = true;;
//                        	}
//                        }
//
//                        if (attrText.length() > end) {
//                        	String sub = attrText.substring(end, end + 1);
//                        	// Check, if char after keyword is [a-zA-Z0-9_]. If so, result is 0, else 1.
//                        	if(sub.split("\\w").length == 0){
//                        		letter = true;;
//                        	}
//                        }
//                        if (!letter) {
//                            keyword = lookUp;
//                        }
//                        break;
//                    }
//                }
//                if (keyword != null) {
//                    break;
//                }
//            }
//        }
//        return keyword;
//    }

//    /**
//     * Looks up in a node for keywords. <br>
//     * Only find keywords if the attribute values are equals to the keyword. <br>
//     * Date in pubdate will not be found, also in time-date the keyword will not be found.
//     * 
//     * @param node HTML-node to be searched.
//     * @param keyWords Array of keywords to look for.
//     * @return
//     */
//    public static String findNodeKeyword(Node node, String[] keyWords) {
//        String keyword = null;
//        NamedNodeMap attrMap = node.getAttributes();
//        if (attrMap != null) {
//            for (int j = 0; j < keyWords.length; j++) {
//                String lookUp = keyWords[j];
//                for (int i = 0; i < attrMap.getLength(); i++) {
//                    Node attr = attrMap.item(i);
//                    if (lookUp.equalsIgnoreCase(attr.getNodeValue())) {
//                        keyword = lookUp;
//                        break;
//                    }
//                }
//                if (keyword != null) {
//                    break;
//                }
//            }
//        }
//        return keyword;
//    }

    //Monat und Jahr sind nur gerundet.
    public static ExtractedDate findRelativeDate(String text){
    	
    	ExtractedDate date = null;
    	Object[] allRegExp = RegExp.getRealtiveDates();
    	Pattern pattern;
		Matcher matcher;
		
		for(int i=0; i<allRegExp.length; i++){
			String[] regExp = (String[]) allRegExp[i];
			pattern = Pattern.compile(regExp[0]);
			matcher = pattern.matcher(text);
			if (matcher.find()) {
			    int start = matcher.start();
			    int end = matcher.end();
			    String relativeTime = text.substring(start, end);
			    long number = Long.valueOf(relativeTime.split(" ")[0]);
			    
			    String format = regExp[1];
			    GregorianCalendar cal = new GregorianCalendar();
			    long actTime = cal.getTimeInMillis();
			    long difTime = 0;
			    long relTime;
			    if(format.equalsIgnoreCase("min")){
			    	difTime = number * 60 * 1000;
			    }else if(format.equalsIgnoreCase("hour")){
			    	difTime = number * 60 * 60 * 1000;
			    }else if(format.equalsIgnoreCase("day")){
			    	difTime = number * 24 * 60 * 60 * 1000;
			    }else if(format.equalsIgnoreCase("mon")){
			    	difTime = number * 30 * 24 * 60 * 60 * 1000;
			    }else if(format.equalsIgnoreCase("year")){
			    	difTime = number * 365 * 24 * 60 * 60 * 1000;
			    }
			    
			    relTime = actTime - difTime;
			    date = ExtractedDateHelper.createDate(relTime);
			    break;
			}
		}
		return date;
    }

    /**
     * <p>
     * Sometimes texts in webpages have special code for character. E.g. <i>&ampuuml;</i> or whitespace. To evaluate
     * this text reasonably you need to convert this code.
     * </p>
     * 
     * @param text
     * @return
     */
    public static String replaceHtmlSymbols(String text) {
    
        String result = StringEscapeUtils.unescapeHtml(text);
        result = StringHelper.replaceProtectedSpace(result);
    
        // remove undesired characters
        result = result.replace("&#8203;", " "); // empty whitespace
        result = result.replace("\n", " ");
        result = result.replace("&#09;", " "); // html tabulator
        result = result.replace("\t", " ");
        result = result.replace(" ,", " ");
    
        return result;
    }
}
