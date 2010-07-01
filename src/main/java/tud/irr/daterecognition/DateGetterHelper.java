package tud.irr.daterecognition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;

import tud.iir.helper.DateHelper;
import tud.iir.knowledge.RegExp;
import tud.iir.web.Crawler;
import tud.iir.web.CrawlerCallback;

public class DateGetterHelper {

	
	public static String[] getURLDate(String url){
		
		String[] returnValue= {"0", "false"};
		
		Boolean setDay=false;
		
		String date;
		String[] dateArray;
		
		//ISO8601
		date=getDateFromString(url, "/" + RegExp.DATE_ISO8601_YMD, 1);
		if(date==null){
			date=getDateFromString(url, "/" + RegExp.DATE_ISO8601_YM, 1);
		if(date==null){
			date=getDateFromString(url, "/" + RegExp.DATE_ISO8601_YWD, 1);
		if(date==null){
			date=getDateFromString(url, "/" + RegExp.DATE_ISO8601_YW, 1);
		if(date==null){
			date=getDateFromString(url, "/" + RegExp.DATE_ISO8601_YD, 1);
		if(date==null){
			date=getDateFromString(url, "/" + RegExp.URL_DATE_D, 1);
		if(date==null){
			date=getDateFromString(url, "/" + RegExp.URL_DATE, 1);
		}}}}}}
		
		System.out.println(url);
		if(date!=null){
			String separator = getSeparator(date);
			if(separator==null){
				System.out.println(date);
				System.out.println("no separator found");
				return null;
			}
			dateArray = date.split(separator);
			if(dateArray.length<2 || dateArray.length>3){
				System.out.println("wrong array");
				return null;
			}
			else{
				dateArray[0]= getLongYear(dateArray[0]);
				if(dateArray.length==2){
					System.out.println("short year");
					dateArray=setDayToDate(dateArray);
					setDay=true;
				}
				returnValue[0] =(getISO8601(dateArray[0], dateArray[1], dateArray[2]));
				returnValue[1] = setDay.toString();
				return returnValue;
			}
		}
		System.out.println("no regular expression found");
		return returnValue;
		
		

	}
	
	/**
	 * 
	 * @param string string, which is to be searched
	 * @param regExp regular expression for search
	 * @param offsetStart is slider for beginning substring (no negative values) - e.g. substring: "abcd" offsetStart=0: "abcd" offsetStart=1: "bcd" offsetStart=-1: "abcd"  
	 * @return found substring or null
	 */
	public static String getDateFromString(String string, String regExp, int offsetStart){
		Pattern pattern;
		Matcher matcher;
		
		pattern = Pattern.compile(regExp);
        matcher = pattern.matcher(string);
        
        System.out.println(regExp);
        
        if (matcher.find()) {
			System.out.println("true");
        	int start = matcher.start();
        	int end = matcher.end();
        	if(offsetStart<0 || start+offsetStart>=end)
        		offsetStart=0;
        	return string.substring(start+offsetStart, end);
        }
		return null;
	}
	
	/**
	 *  Every number between 100 and 1992 returns null
	 * Every number over 1992 returns the parameter _
	 * Every number under 93 returns 2000-2092 _
	 * Every number between 93 and 99 return 1993-1999 _
	 * @param year 
	 * @return a year with 4 positions between 1993 and 2092
	 *
	 */
	public static String getLongYear(String year){
		int yearInt = Integer.parseInt(year);
		if(yearInt<93)
			return "20" + yearInt;
		else if(yearInt<100)
			return "19" + yearInt;
		else if (yearInt>1992)
			return String.valueOf(yearInt);
		else 
			return null;
	}
	/**
	 * 
	 * @param month
	 * @return a string month with two positions
	 */
	public static String getLongMonth(String month){
		int monthInt = Integer.parseInt(month);
		if(monthInt>12 || monthInt<1)
			return null;
		else if(monthInt<10)
			return "0" + monthInt;
		else
			return String.valueOf(monthInt);
	}
	
	/**
	 * 
	 * @param day
	 * @return day with two positions
	 */
	public static String getLongDay(String dayString) {
		int day = Integer.parseInt(dayString);
		if (day<1 || day>31)
			return null;
		else if(day<10)
			return "0" + day;
		else
			return String.valueOf(day);
	}
	
	public static String getISO8601(String year, String month, String day){
		return year + "-" + month + "-" + day;
	}
	
	public static String getISO8601(int year, int month, int day){
		return getLongYear(String.valueOf(year)) + "-" + getLongMonth(String.valueOf(month)) + "-" + getLongDay(String.valueOf(day));
	}
	
	/**
	 * 
	 * @param text a date, where year, month and day are separated by . / or _
	 * @return the separating symbol
	 */
	public static String getSeparator(String text){
		String separator = "";

        int index = text.indexOf(".");
        if (index == -1) {
            index = text.indexOf("/");
            if (index == -1) {
                index = text.indexOf("_");
                if (index == -1) {
                	index=text.indexOf("-");
                	if(index==-1){
                		return null;
                	}
                	else{
                		separator = "-";
                	}
                } else {
                    separator = "_";
                }
            } else {
                separator = "/";
            }
        } else {
            separator = "\\.";
        }
        return separator;
	}
	
	public static String[] setDayToDate(String[] dateArray){
		String[] help = new String[3];
		help[0]=dateArray[0];
		help[1]=dateArray[1];
		help[2]="01";
		return help;
	}
	
	/**
	 * tries to find out what value is the year, month and day
	 * @param array a array with three values unordered year, month and day
	 * @return ordered array with year, month and day
	 * 
	 */
	public static String[] getDateparts(String[] array){
		
		return null;
	}	
}
