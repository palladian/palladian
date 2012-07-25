package ws.palladian.extraction.date.rater;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.extraction.date.DateRaterHelper;
import ws.palladian.extraction.date.KeyWords;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.date.dates.ContentDate;
import ws.palladian.helper.html.HtmlHelper;

/**
 *This class evaluates content-dates. <br>
 *Doing this by dividing dates in three parts: Keyword in attribute, in text and no keyword.<br>
 * Each part will be rate different.<br>
 * Part one by keyword classes, see {@link KeyWords#getKeywordPriority(String)} and age.
 * Part two by distance of keyword an date, keyword classes and age.
 * Part three by age.
 * 
 * @author Martin Gregor
 * 
 */
public class ContentDateRater_old extends TechniqueDateRater<ContentDate> {


    private byte hightPriority;
    private byte middlePriority;
    private byte lowPriority;
	
    public ContentDateRater_old(PageDateType dateType) {
		super(dateType);
		switch(this.dateType){
		case publish: 
			hightPriority = KeyWords.PUBLISH_KEYWORD;
			middlePriority = KeyWords.MODIFIED_KEYWORD;
			lowPriority = KeyWords.OTHER_KEYWORD;
			break;
		case last_modified:
			hightPriority = KeyWords.MODIFIED_KEYWORD;
			middlePriority = KeyWords.PUBLISH_KEYWORD;
			lowPriority = KeyWords.OTHER_KEYWORD;
			break;
		}
	}

	@Override
    public Map<ContentDate, Double> rate(List<ContentDate> list) {
    	Map<ContentDate, Double> returnDates = evaluateContentDate(list);
    	this.ratedDates = returnDates;
        return returnDates;
    }

    /**
     * Evaluates content dates.<br>
     * Divide all dates in one of three parts: keyword in attribute, in text an no keyword.<br>
     * Evaluate each part. <br>
     * Put all parts together.
     * 
     * @param dates
     * @return
     */
    private Map<ContentDate, Double> evaluateContentDate(List<ContentDate> dates) {
        Map<ContentDate, Double> result = new HashMap<ContentDate, Double>();

        List<ContentDate> attrDates = DateArrayHelper.filter(dates, DateArrayHelper.FILTER_KEYLOC_ATTR);
        List<ContentDate> contDates = DateArrayHelper.filter(dates, DateArrayHelper.FILTER_KEYLOC_CONT);
        List<ContentDate> nokeywordDates = DateArrayHelper.filter(dates, DateArrayHelper.FILTER_KEYLOC_NO);

        Map<ContentDate, Double> attrResult = evaluateKeyLocAttr(attrDates);
        Map<ContentDate, Double> contResult = evaluateKeyLocCont(contDates);
        Map<ContentDate, Double> nokeywordResult = new HashMap<ContentDate, Double>();

        for (int i = 0; i < nokeywordDates.size(); i++) {
            ContentDate date = nokeywordDates.get(i);
            String tag = date.getTag();
            String[] keys = KeyWords.ALL_KEYWORDS;

            // Run through dates without keyword.
            double newRate = 0;
            for (int j = 0; j < keys.length; j++) {
                if (tag.equalsIgnoreCase(keys[j])) {
                    newRate = 1.0 / dates.size();
                    break;
                }
            }
            nokeywordResult.put(date, Math.round(newRate * 10000) / 10000.0);
        }

        // increase rate, if tag is a headline tag. (h1..h6)
        attrResult = evaluateTag(attrResult);
        contResult = evaluateTag(contResult);
        nokeywordResult = evaluateTag(nokeywordResult);

        result.putAll(attrResult);
        result.putAll(contResult);
        result.putAll(nokeywordResult);

        // evaluatePosInDoc(result);

        return result;
    }

    /**
     * Calculates the rate of dates with keywords within text (content).<br>
     * Factors are keyword-class, distance of keyword and date as well as age.
     * 
     * @param contDates
     * @return
     */
    private Map<ContentDate, Double> evaluateKeyLocCont(List<ContentDate> contDates) {
        HashMap<ContentDate, Double> contResult = new HashMap<ContentDate, Double>();
        double factor_keyword;
        for (int i = 0; i < contDates.size(); i++) {
            ContentDate date = contDates.get(i);
            double factorContent = calcContDateContent(date);
            contResult.put(date, factorContent);
        }
        List<ContentDate> rate1dates = DateArrayHelper.getRatedDates(contResult, 1.0);

        List<ContentDate> rateRestDates = DateArrayHelper.getRatedDates(contResult, 1.0, false);


        for (int i = 0; i < rate1dates.size(); i++) {
            // anz der dates mit gleichen werten / anz aller dates
            ContentDate key = rate1dates.get(i);

            factor_keyword = calcContDateAttr(key);
            int countSame = DateArrayHelper.countDates(key, rate1dates, -1) + 1;
            double newRate = 1.0 * countSame / rate1dates.size();
            contResult.put(key, Math.round(newRate * factor_keyword * 10000) / 10000.0);
        }

        for (int i = 0; i < rateRestDates.size(); i++) {
            ContentDate key = rateRestDates.get(i);
            factor_keyword = calcContDateAttr(key);
            int countSame = DateArrayHelper.countDates(key, rateRestDates, -1) + 1;
            double newRate = 1.0 * contResult.get(key) * countSame / contDates.size();
            contResult.put(key, Math.round(newRate * factor_keyword * 10000) / 10000.0);
        }
        return contResult;
    }

    /**
     * Calculates rate of dates with keyword within attribute.<br>
     * Factors are keyword-class and age.
     * 
     * @param attrDates
     * @return
     */
    private Map<ContentDate, Double> evaluateKeyLocAttr(List<ContentDate> attrDates) {
        HashMap<ContentDate, Double> attrResult = new HashMap<ContentDate, Double>();
        for (int i = 0; i < attrDates.size(); i++) {
            ContentDate date = attrDates.get(i);
            double rate = calcContDateAttr(date);
            attrResult.put(date, rate);
        }

        List<ContentDate> rate1Dates = DateArrayHelper.getRatedDates(attrResult, 1);
        List<ContentDate> middleRatedDates = DateArrayHelper.getRatedDates(attrResult, 0.7);
        List<ContentDate> lowRatedDates = DateArrayHelper.getRatedDates(attrResult, 0.5);

        if (rate1Dates.size() > 0) {
            attrResult.putAll(setRateWhightedByGroups(rate1Dates, attrDates));

            DateRaterHelper.setRateToZero(middleRatedDates, attrResult);
            DateRaterHelper.setRateToZero(lowRatedDates, attrResult);
        } else if (middleRatedDates.size() > 0) {
            attrResult.putAll(setRateWhightedByGroups(middleRatedDates, attrDates));

            DateRaterHelper.setRateToZero(lowRatedDates, attrResult);
        } else {
            attrResult.putAll(setRateWhightedByGroups(lowRatedDates, attrDates));
        }
        return attrResult;

    }
    /**
     * Sets the factor for keyword-classes.
     * 
     * @param date
     * @return
     */
    private double calcContDateAttr(ContentDate date) {
        String key = date.getKeyword();
        double factor = 0;
        byte keywordPriority = DateRaterHelper.getKeywordPriority(date);
        if (key != null) {
            if (keywordPriority == hightPriority) {
                factor = 1;
            } else if (keywordPriority == middlePriority) {
                factor = 0.7;
            } else if (keywordPriority == lowPriority) {
                factor = 0.5;
            } else {
                factor = 0.0;
            }
        }
        return factor;
    }

    /**
     * Sets the factor for distance of keyword and date.
     * 
     * @param date
     * @return
     */
    private double calcContDateContent(ContentDate date) {
        int distance = date.get(ContentDate.DISTANCE_DATE_KEYWORD);
        // f(x) = -1/17*x+20/17
        double factor = -1.0 / 17.0 * distance + 20.0 / 17.0;
        factor = Math.max(0, Math.min(1.0, factor));
        return Math.round(factor * 10000) / 10000.0;
    }
    
    /**
     * Increase the rate by 10 percent, if date surrounding tag is a headline-tag.
     * 
     * @param contentDates
     * @return
     */
    public static Map<ContentDate, Double> evaluateTag(Map<ContentDate, Double> contentDates) {
        Map<ContentDate, Double> result = contentDates;
        for (Entry<ContentDate, Double> e : contentDates.entrySet()) {
            if (HtmlHelper.isHeadlineTag(e.getKey().getTag())) {
                double newRate = (1 - e.getValue()) * 0.1 + e.getValue();
                result.put(e.getKey(), Math.round(newRate * 10000) / 10000.0);
            }
        }
        return result;
    }
    
    /**
     * Calculates the rate for dates.<br>
     * NewRate = CountOfSameDatesToSet / CountOfDatesToSet. <br>
     * Example: datesToSet.size()=5; 3/5 and 2/5.
     * 
     * @param <T>
     * @param datesToSet
     * @param dates
     */
    public static <T extends ExtractedDate> Map<T, Double> setRateWhightedByGroups(List<T> datesToSet, List<T> dates) {
        Map<T, Double> resultDates = new HashMap<T, Double>();
        for (int k = 0; k < datesToSet.size(); k++) {
            int contSame = DateArrayHelper.countDates(datesToSet.get(k), dates, -1) + 1;
            double newRate = 1.0 * contSame / dates.size();

            resultDates.put(datesToSet.get(k), Math.round(newRate * 10000) / 10000.0);

        }
        return resultDates;
    }

}
