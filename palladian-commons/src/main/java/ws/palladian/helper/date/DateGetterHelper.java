package ws.palladian.helper.date;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import ws.palladian.helper.DateFormat;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.dates.ContentDate;
import ws.palladian.helper.date.dates.DateParser;
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
    public static ExtractedDate findDate(String dateString, DateFormat[] dateFormats) {
    	ExtractedDate date = null;
    	DateFormat[] regExps = dateFormats;

    	if (regExps == null) {
            regExps = RegExp.getAllRegExp();
        }

        for (int i = 0; i < regExps.length; i++) {
            // FIXME "Mon, 18 Apr 2011 09:16:00 GMT-0700" fails.
            try {
                date = getDateFromString(dateString, regExps[i]);
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
        DateFormat[] regExps = RegExp.getAllRegExp();

        // try to catch numbers that might be year mentions
        if (includeYearOnly) {
            DateFormat yearRegExp = RegExp.DATE_CONTEXT_YYYY;
            List<DateFormat> newArray = new ArrayList<DateFormat>();
            for (DateFormat regExp : regExps) {
                newArray.add(regExp);
            }
            newArray.add(yearRegExp);
            regExps = newArray.toArray(new DateFormat[0]);
        }

        Pattern[] pattern = new Pattern[regExps.length];
        for (int i = 0; i < regExps.length; i++) {
            pattern[i] = Pattern.compile((regExps[i].getRegExp()));
        }
        return findAllDates(text, pattern, regExps);
    }

    /**
     * 
     * @param dateString a date to match.
     * @return The found format, defined in RegExp constants. <br>
     *         If no match is found return <b>null</b>.
     */
    public static List<ContentDate> findAllDates(String text, Pattern[] pattern, DateFormat[] regExps) {
    	String tempText = text;
    	List<ContentDate> dates = new ArrayList<ContentDate>();
    	
    	for(int i = 0; i < pattern.length; i++){
    	    Matcher matcher = pattern[i].matcher(tempText);
    		while(matcher.find()){
				boolean hasPrePostNum = false;
				int start = matcher.start();
				int end = matcher.end();
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
	            	try {
		            	// String dateString = tempText.substring(start, end);
	            	    String dateString = matcher.group();
		            	ExtractedDate temp = DateParser.parse(dateString, regExps[i].getFormat());
		            	ContentDate date = new ContentDate(temp);
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
    public static ExtractedDate findDate(String text, Pattern[] pattern, DateFormat[] regExps) {
//        String tempText = text;
//        ExtractedDate date = null;
//        for (int i = 0; i < pattern.length; i++) {
//            Matcher matcher = pattern[i].matcher(tempText);
//            if (matcher.find()) {
//                boolean hasPrePostNum = false;
//                int start = matcher.start();
//                int end = matcher.end();
//                if (start > 0) {
//                    String temp = tempText.substring(start - 1, start);
//                    try {
//                        Integer.parseInt(temp);
//                        hasPrePostNum = true;
//                    } catch (NumberFormatException e) {
//                    }
//                }
//                if (end < tempText.length()) {
//                    String temp = tempText.substring(end, end + 1);
//                    try {
//                        Integer.parseInt(temp);
//                        hasPrePostNum = true;
//                    } catch (NumberFormatException e) {
//                    }
//                }
//                if (!hasPrePostNum) {
//                    String dateString = tempText.substring(start, end);
//                    date = DateParser.parse(dateString, ((String[])regExps[i])[1]);
//                    break;
//                }
//            }
//        }
//        return date;
        List<ContentDate> dates = findAllDates(text, pattern, regExps);
        if (dates.size() > 0) {
            return dates.get(0);
        }
        return null;
    }


    /**
     * Returns a string of "x"s as long as the parameter string.
     * 
     * @param text
     * @return String of "x"s.
     */
    public static String getXs(String text) {
//        StringBuffer sb = new StringBuffer();
//        for (int i = 0; i < text.length(); i++) {
//            sb.append("x");
//        }
//        return sb.toString();
        return StringUtils.repeat('x', text.length());
    }

    /**
     * Check a string for keywords. Used to look in tag-values for date-keys.
     * 
     * @param text string with possible keywords.
     * @param keys a array of keywords.
     * @return the found keyword.
     */
    public static String hasKeyword(String text, String[] keys) {
        String keyword = null;
        for (int i = 0; i < keys.length; i++) {
            Pattern pattern = Pattern.compile(keys[i].toLowerCase());
            Matcher matcher = pattern.matcher(text.toLowerCase());
            if (matcher.find()) {
                keyword = keys[i];
                break;
            }
        }
        return keyword;
    }


    /**
     * 
     * @param string string, which is to be searched
     * @param regExp regular expression for search
     * @param offsetStart is slider for beginning substring (no negative values) - e.g. substring: "abcd" offsetStart=0:
     *            "abcd" offsetStart=1: "bcd" offsetStart=-1: "abcd"
     * @return found substring or null
     */
    public static ExtractedDate getDateFromString(String dateString, DateFormat dateFormat) {
    	
        String text = StringHelper.removeDoubleWhitespaces(replaceHtmlSymbols(dateString));
        boolean hasPrePostNum = false;
        ExtractedDate date = null;
        Pattern pattern = Pattern.compile(dateFormat.getRegExp());
        Matcher matcher = pattern.matcher(text);
        
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
                //date = new ExtractedDate(text.substring(start, end), regExp[1]);
                date = DateParser.parse(text.substring(start, end), dateFormat.getFormat());
            }

        }
        return date;
    }


    //Monat und Jahr sind nur gerundet.
    public static ExtractedDate findRelativeDate(String text) {

        ExtractedDate date = null;
        Object[] allRegExp = RegExp.getRelativeDates();
        for (int i = 0; i < allRegExp.length; i++) {
            String[] regExp = (String[])allRegExp[i];
            Pattern pattern = Pattern.compile(regExp[0]);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                // int start = matcher.start();
                // int end = matcher.end();
                // String relativeTime = text.substring(start, end);
                String relativeTime = matcher.group();
                long number = Long.valueOf(relativeTime.split(" ")[0]);

                String format = regExp[1];
                GregorianCalendar cal = new GregorianCalendar();
                long actTime = cal.getTimeInMillis();
                long difTime = 0;
                if (format.equalsIgnoreCase("min")) {
                    difTime = number * 60 * 1000;
                } else if (format.equalsIgnoreCase("hour")) {
                    difTime = number * 60 * 60 * 1000;
                } else if (format.equalsIgnoreCase("day")) {
                    difTime = number * 24 * 60 * 60 * 1000;
                } else if (format.equalsIgnoreCase("mon")) {
                    difTime = number * 30 * 24 * 60 * 60 * 1000;
                } else if (format.equalsIgnoreCase("year")) {
                    difTime = number * 365 * 24 * 60 * 60 * 1000;
                }

                long relTime = actTime - difTime;
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
