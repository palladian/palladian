package ws.palladian.helper.date;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.helper.DateFormat;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.dates.ContentDate;

/**
 * DateGetterHelper provides the techniques to find dates out of webpages. Also provides different helper methods.
 * 
 * @author Martin Gregor
 * 
 */
public final class DateGetterHelper {


    public static List<ContentDate> findAllDates(String text) {
        return findAllDates(text, RegExp.ALL_DATE_FORMATS);
    }

//    public static List<ContentDate> findAllDates(String text, boolean includeYearOnly) {
////        DateFormat[] regExps = RegExp.ALL_DATE_FORMATS;
////
////        // try to catch numbers that might be year mentions
////        if (includeYearOnly) {
////            DateFormat yearRegExp = RegExp.DATE_CONTEXT_YYYY;
////            List<DateFormat> newArray = new ArrayList<DateFormat>();
////            for (DateFormat regExp : regExps) {
////                newArray.add(regExp);
////            }
////            newArray.add(yearRegExp);
////            regExps = newArray.toArray(new DateFormat[0]);
////        }
////        return findAllDates(text, regExps);
//        
//        if (includeYearOnly) {
//            return findAllDates(text, RegExp.ALL_DATE_FORMATS);
//        } else {
//            return findAllDates(text, ArrayUtils.addAll(RegExp.ALL_DATE_FORMATS, RegExp.DATE_CONTEXT_YYYY));
//        }
//    }

    /**
     * 
     * @param dateString a date to match.
     * @return The found format, defined in RegExp constants. <br>
     *         If no match is found return <b>null</b>.
     */
    public static List<ContentDate> findAllDates(String text, DateFormat... formats) {
    	String tempText = text;
    	List<ContentDate> dates = new ArrayList<ContentDate>();
    	
    	for (DateFormat format : formats) {
    	    Matcher matcher = format.getPattern().matcher(tempText);
    		while(matcher.find()){
				boolean hasPrePostNum = false;
				int start = matcher.start();
				int end = matcher.end();
				if (start > 0) {
	                String temp = tempText.substring(start - 1, start);
	                hasPrePostNum = temp.matches("\\d");
	            }
	            if (end < tempText.length()) {
	                String temp = tempText.substring(end, end + 1);
	                hasPrePostNum = temp.matches("\\d");
	            }
	            if (!hasPrePostNum) {
	            	try {
	            	    String dateString = matcher.group();
		            	ExtractedDate temp = DateParser.parseDate(dateString, format);
		            	ContentDate date = new ContentDate(temp);
		            	int index = tempText.indexOf(date.getDateString());
		            	date.set(ContentDate.DATEPOS_IN_TAGTEXT, index);
		                String xString = StringUtils.repeat('x', dateString.length());
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

}
