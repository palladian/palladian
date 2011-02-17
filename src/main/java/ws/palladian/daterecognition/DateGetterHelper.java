package ws.palladian.daterecognition;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ws.palladian.daterecognition.dates.ContentDate;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.helper.HTMLHelper;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.StringHelper;

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
     * @return The found format, defined in RegExp constants. <br>
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
     * @return The found format, defined in RegExp constants. <br>
     *         If no match is found return <b>null</b>.
     */
    public static ExtractedDate findDate(final String dateString, Object[] regExpArray) {
        Object[] regExps = regExpArray;
        if (regExps == null) {
            regExps = RegExp.getAllRegExp();
        }
        ExtractedDate date = null;

        for (int i = 0; i < regExps.length; i++) {
            date = getDateFromString(dateString, (String[]) regExps[i]);
            if (date != null) {
                break;
            }
        }
        return date;
    }

    /**
     * 
     * @param dateString a date to match.
     * @return The found format, defined in RegExp constants. <br>
     *         If no match is found return <b>null</b>.
     */
    public static ArrayList<ContentDate> findALLDates(String text) {
    	//String dateString = org.apache.commons.lang.StringEscapeUtils.unescapeHtml(text);
    	//dateString = StringHelper.removeDoubleWhitespaces(dateString);
    	String dateString = StringHelper.removeDoubleWhitespaces(HTMLHelper.replaceHTMLSymbols(text));
    	
        ArrayList<ContentDate> contentDates = new ArrayList<ContentDate>();
        ExtractedDate date = null;
        Object[] regExps = RegExp.getAllRegExp();
        for (int i = 0; i < regExps.length; i++) {
            date = getDateFromString(dateString, (String[]) regExps[i]);
            if (date != null) {
                ContentDate cDate = DateConverter.convert(date, DateConverter.TECH_HTML_CONT);
                int index = dateString.indexOf(date.getDateString());
                cDate.set(ContentDate.DATEPOS_IN_TAGTEXT, index);
                contentDates.add(cDate);
                //Bei ReplaceFirst wird der Matcher verwendet, der aber manchaml zu problemen führt: "http://kerneltrap.org/node/1776"
                dateString = dateString.replace(date.getDateString(), getXs(date.getDateString()));
                i--;
            }
        }

        return contentDates;
    }

    /**
     * Returns a string of whitespace as long as the parameter string.
     * 
     * @param text
     * @return String of whitespace.
     */
    public static String getWhitespaces(String text) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            sb.append("x");
            // sb.append(" ");
        }
        return sb.toString();
    }

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

    /**
     * Finds out the separating symbol of date-string
     * 
     * @param date
     * @return
     */
    public static String getSeparator(final ExtractedDate date) {
        final String dateString = date.getDateString();
        return ExtractedDateHelper.getSeparator(dateString);
    }

    /**
     * 
     * @param string string, which is to be searched
     * @param regExp regular expression for search
     * @param offsetStart is slider for beginning substring (no negative values) - e.g. substring: "abcd" offsetStart=0:
     *            "abcd" offsetStart=1: "bcd" offsetStart=-1: "abcd"
     * @return found substring or null
     */
    public static ExtractedDate getDateFromString(final String dateString, final String[] regExp) {
        String text = StringHelper.removeDoubleWhitespaces(HTMLHelper.replaceHTMLSymbols(dateString));
        boolean hasPrePostNum = false;
        ExtractedDate date = null;
        Pattern pattern;
        Matcher matcher;
        pattern = Pattern.compile(regExp[0]);
        matcher = pattern.matcher(text);
        if (matcher.find()) {
            final int start = matcher.start();
            final int end = matcher.end();
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
                try {
                    Integer.parseInt(temp);
                    hasPrePostNum = true;

                } catch (NumberFormatException e) {

                }
            }
            if (!hasPrePostNum) {
                date = new ExtractedDate(text.substring(start, end), regExp[1]);
            }

        }
        return date;
    }

    /**
     * In opposition to <b>findeNodeKeyword</b> also keywords as part of longer String will be found. <br>
     * But with condition that keyword is no part of a word. <br>
     * E.g.: date in timedate will not be found, but there for time-date matches.
     * 
     * @param node HTML-node to be searched.
     * @param keyWords Array of keywords to look for.
     * @return
     */
    public static String findNodeKeywordPart(Node node, String[] keyWords) {
        String keyword = null;
        NamedNodeMap attrMap = node.getAttributes();
        Pattern p = Pattern.compile("\\w");
        Matcher m;
        if (attrMap != null) {
            for (int j = 0; j < keyWords.length; j++) {
                String lookUp = keyWords[j].toLowerCase();
                for (int i = 0; i < attrMap.getLength(); i++) {
                    Node attr = attrMap.item(i);
                    String attrText = attr.getNodeValue().toLowerCase();
                    int index = attrText.indexOf(lookUp);
                    if (index != -1) {
                        boolean letter = false;
                        int start = index;
                        int end = index + lookUp.length();

                        if (start > 0) {

                            String bevor = attrText.substring(start - 1, start);
                            m = p.matcher(bevor);
                            if (m.find()) {
                                letter = true;
                            }
                        }

                        if (attrText.length() > end) {
                            String after = attrText.substring(end, end + 1);
                            m = p.matcher(after);
                            if (m.find()) {
                                letter = true;
                            }
                        }
                        if (!letter) {
                            keyword = lookUp;
                        }
                        break;
                    }
                }
                if (keyword != null) {
                    break;
                }
            }
        }
        return keyword;
    }

    /**
     * Looks up in a node for keywords. <br>
     * Only find keywords if the attribute values are equals to the keyword. <br>
     * Date in pubdate will not be found, also in time-date the keyword will not be found.
     * 
     * @param node HTML-node to be searched.
     * @param keyWords Array of keywords to look for.
     * @return
     */
    public static String findNodeKeyword(Node node, String[] keyWords) {
        String keyword = null;
        NamedNodeMap attrMap = node.getAttributes();
        if (attrMap != null) {
            for (int j = 0; j < keyWords.length; j++) {
                String lookUp = keyWords[j];
                for (int i = 0; i < attrMap.getLength(); i++) {
                    Node attr = attrMap.item(i);
                    if (lookUp.equalsIgnoreCase(attr.getNodeValue())) {
                        keyword = lookUp;
                        break;
                    }
                }
                if (keyword != null) {
                    break;
                }
            }
        }
        return keyword;
    }

    /**
     * For date in a string the nearest keyword will be found, independently from kind of keyword or position in keyword
     * array.<br>
     * If a keyword is found, it will be set to the date that will be returned.
     * 
     * @param textString String within the date.
     * @param date Date found in the textString.
     * @param keys
     */
    public static void setNearestTextkeyword(String textString, ContentDate date, String[] keys) {
        String text = StringHelper.removeDoubleWhitespaces(HTMLHelper.replaceHTMLSymbols(textString));
        String keyword = null;
        String dateString = date.getDateString();
        int dateBegin = text.indexOf(dateString);
        int dateEnd = dateBegin + dateString.length();
        int distance = 9999;
        int keyBegin;
        int keyEnd;
        int temp;

        Pattern p = Pattern.compile("\\w");
        Matcher m;

        for (int i = 0; i < keys.length; i++) {
        	
            keyBegin = text.toLowerCase(Locale.ENGLISH).indexOf(keys[i].toLowerCase(Locale.ENGLISH));
            if (keyBegin != -1) {
            	
                keyEnd = keyBegin + keys[i].length();
                
                if (keyBegin > 0) {
                    m = p.matcher(text.substring(keyBegin - 1, keyBegin));
                    if (m.find()) {
                        continue;
                    }
                }
                
                if (keyEnd < text.length()) {

                    m = p.matcher(text.substring(keyEnd, keyEnd + 1));
                    if (m.find()) {
                        continue;
                    }
                }
                
                int subBegin = Math.min(dateBegin, keyBegin);
                int subende = Math.max(dateEnd, keyEnd);
                String subText = text.substring(subBegin, subende);
                
                temp = StringHelper.countWhitespaces(subText);
                if (temp < distance) {
                    distance = temp;
                    keyword = keys[i];
                }
                
            }
            
        }

        if (keyword != null && distance < 20) {
            date.setKeyword(keyword);
            date.set(ContentDate.DISTANCE_DATE_KEYWORD, distance);
            date.set(ContentDate.KEYWORDLOCATION, ContentDate.KEY_LOC_CONTENT);
        }
    }
    
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
}
