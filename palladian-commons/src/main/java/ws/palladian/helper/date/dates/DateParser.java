package ws.palladian.helper.date.dates;

import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.helper.DateFormat;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.ExtractedDateHelper;
import ws.palladian.helper.nlp.StringHelper;

public class DateParser {

    public static ExtractedDate parse(String dateString, String format) {
        DateParserLogic parseLogic = new DateParserLogic();
        parseLogic.parse(dateString, format);
        return new ExtractedDate(parseLogic.year, parseLogic.month, parseLogic.day, parseLogic.hour, parseLogic.minute,
                parseLogic.second, dateString, format);
    }
    
    /**
     * Tries to match a date in a dateformat. The format is given by the regular expressions of RegExp.
     * 
     * @param dateString a date to match.
     * @return The found date, defined in RegExp constants. <br>
     *         If no match is found return <b>null</b>.
     */
    public static ExtractedDate findDate(String dateString) {
        return findDate(dateString, RegExp.ALL_DATE_FORMATS);
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
        
        for (DateFormat dateFormat : dateFormats) {
            // FIXME "Mon, 18 Apr 2011 09:16:00 GMT-0700" fails.
            try {
                date = getDateFromString(dateString, dateFormat);
            } catch (Throwable th) {
                th.printStackTrace();
            }
            if (date != null) {
                break;
            }
        }
        return date;
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
        
        String text = StringHelper.removeDoubleWhitespaces(ExtractedDateHelper.replaceHtmlSymbols(dateString)); // FIXME is this necessary?
        boolean hasPrePostNum = false;
        ExtractedDate date = null;
        //Pattern pattern = Pattern.compile(dateFormat.getRegex());
        Pattern pattern = dateFormat.getPattern();
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (start > 0) {
                String temp = text.substring(start - 1, start);
//                try {
//                    Integer.parseInt(temp);
//                    hasPrePostNum = true;
//                } catch (NumberFormatException e) {
//                    e.printStackTrace(); // FIXME
//                }
                hasPrePostNum = temp.matches("\\d");
            }
            if (end < text.length()) {
                String temp = text.substring(end, end + 1);
                //If last character is "/" no check for number is needed.
                if(!text.substring(end-1, end).equals("/")){
//                  try {
//                      Integer.parseInt(temp);
//                      hasPrePostNum = true;
//                  } catch (NumberFormatException e) {
//                      e.printStackTrace(); // FIXME
//                  }
                    hasPrePostNum = temp.matches("\\d");
                }
            }
            if (!hasPrePostNum) {
                // date = DateParser.parse(text.substring(start, end), dateFormat.getFormat());
                date = parse(matcher.group(), dateFormat.getFormat());
            }

        }
        return date;
    }
    
    //Monat und Jahr sind nur gerundet.
    public static ExtractedDate findRelativeDate(String text) {

        ExtractedDate date = null;
        DateFormat[] relativeDateFormats = RegExp.RELATIVE_DATES;
        for (DateFormat dateFormat : relativeDateFormats) {
            Pattern pattern = dateFormat.getPattern();
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String relativeTime = matcher.group();
                long number = Long.valueOf(relativeTime.split(" ")[0]);

                String format = dateFormat.getFormat();
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

    
//    /**
//     * Convert month-name in a number; January is 01..
//     * TODO somewhat duplicate to {@link DateHelper#monthNameToNumber(String)}
//     * 
//     * @param month
//     * @return month-number as string
//     */
//    public static String getMonthNumber(String monthString) {
//        String month = monthString;
//        month = month.replaceAll(",", "");
//        month = month.replaceAll("\\.", "");
//        month = month.replaceAll(" ", "");
//        month = month.toLowerCase();
//        String monthNumber = null;
//        if (month.equals("january") || month.equals("januar") || month.equals("jan")) {
//            monthNumber = "01";
//        } else if (month.equals("february") || month.equals("februar") || month.equals("feb")) {
//            monthNumber = "02";
//        } else if (month.equals("march") || month.equals("märz") || month.equals("mär") || month.equals("mar")) {
//            monthNumber = "03";
//        } else if (month.equals("april") || month.equals("apr")) {
//            monthNumber = "04";
//        } else if (month.equals("may") || month.equals("mai") || month.equals("may")) {
//            monthNumber = "05";
//        } else if (month.equals("june") || month.equals("juni") || month.equals("jun")) {
//            monthNumber = "06";
//        } else if (month.equals("july") || month.equals("juli") || month.equals("jul")) {
//            monthNumber = "07";
//        } else if (month.equals("august") || month.equals("aug")) {
//            monthNumber = "08";
//        } else if (month.equals("september") || month.equals("sep") || month.equals("sept")) {
//            monthNumber = "09";
//        } else if (month.equals("october") || month.equals("oktober") || month.equals("oct") || month.equals("okt")) {
//            monthNumber = "10";
//        } else if (month.equals("november") || month.equals("nov")) {
//            monthNumber = "11";
//        } else if (month.equals("december") || month.equals("dezember") || month.equals("dec") || month.equals("dez")) {
//            monthNumber = "12";
//        }
//        return monthNumber;
//    }

}
