package tud.iir.daterecognition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.helper.DateArrayHelper;
import tud.iir.helper.RatedDateComparator;

/**
 * Use this class to rate a webpage. <br>
 * Set an url and use evaluate to get and rate all dates of a webpage. <br>
 * Different methods return found dates.
 * 
 * @author Martin Gregor
 * 
 */
public class WebPageDateEvaluator {
    /** Standard DateGetter. */
    DateGetter dg = new DateGetter();

    /** Standard DateRater. */
    DateEvaluator dr = new DateEvaluator();

    ArrayList<ExtractedDate> list = new ArrayList<ExtractedDate>();

    private String url;
    private boolean reference = false;

    /**
     * Set url for webpage to be searched.
     * 
     * @param url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Look up for all dates of webpage and rate them.<br>
     * Writes results in list, get it by getter-methods.
     * 
     * @param url for webpage.
     */
    public void evaluate(String url) {
        this.url = url;
        evaluate();
    }

    /**
     * Look up for all dates of webpage and rate them.<br>
     * Writes results in list, get it by getter-methods.<br>
     * <br>
     * Be sure than an url is set. Otherwise use <b>evaluate(String url)</b>.
     */
    public void evaluate() {
        if (this.url != null) {
            ArrayList<ExtractedDate> dates = new ArrayList<ExtractedDate>();
            HashMap<ExtractedDate, Double> ratedDates = new HashMap<ExtractedDate, Double>();
            ExtractedDate date = new ExtractedDate();

            dg.setURL(url);
            dg.setTechReference(reference);
            dates = dg.getDate();

            ratedDates = dr.rate(dates);
            this.list = DateArrayHelper.hashMapToArrayList(ratedDates);
        }
    }

    /**
     * Found the beste rated date in list and returns it.<br>
     * If more then one date have same best rate, the first date of an ordered list will be returned. <br>
     * The list will be sort with {@link RatedDateComparator}.
     * ExtractedDate is only super class, date can be cast to original type. <br>
     * Get orginal type by using getType() of date.
     * 
     * @return Best rated date.
     */
    public ExtractedDate getBestRatedDate() {
        ExtractedDate date = new ExtractedDate();
        if (list != null && list.size() > 0) {
            ArrayList<ExtractedDate> orderedList = list;
            Collections.sort(orderedList, new RatedDateComparator<ExtractedDate>());
            date = orderedList.get(0);
        }
        return date;
    }

    /**
     * Found the best rate and returns all dates with this rate. <br>
     * 
     * @return
     *         ArraylList of dates.
     */
    public ArrayList<ExtractedDate> getAllBestRatedDate() {
        return getAllBestRatedDate(false);
    }

    /**
     * Found the best rate and returns all dates with this rate.
     * 
     * @param onlyFullDates True for look at only dates with minimum year, month and day. <br>
     *            False for look at all dates.
     * @return
     *         ArraylList of dates.
     */
    public ArrayList<ExtractedDate> getAllBestRatedDate(boolean onlyFullDates) {
        ArrayList<ExtractedDate> dates = list;
        if (onlyFullDates) {
            dates = DateArrayHelper.filter(dates, DateArrayHelper.FILTER_FULL_DATE);
        }
        double rate = DateArrayHelper.getHighestRate(dates);
        dates = DateArrayHelper.getRatedDates(dates, rate);
        Collections.sort(dates, new RatedDateComparator<ExtractedDate>());
        return dates;
    }

    /**
     * Returns all found and rated dates.
     * 
     * @return all dates.
     */
    public ArrayList<ExtractedDate> getAllDates() {
        ArrayList<ExtractedDate> sorted = list;
        Collections.sort(sorted, new RatedDateComparator<ExtractedDate>());
        return sorted;
    }

    /**
     * By default the technique reference is turned off. <br>
     * Use this to active it.
     */
    public void activateReference() {
        this.reference = true;
    }
}
