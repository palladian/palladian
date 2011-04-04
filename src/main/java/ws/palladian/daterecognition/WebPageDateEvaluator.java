package ws.palladian.daterecognition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.w3c.dom.Document;

import ws.palladian.daterecognition.dates.AbstractDate;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.technique.PageDateType;
import ws.palladian.helper.date.DateArrayHelper;
import ws.palladian.helper.date.RatedDateComparator;

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
    private DateGetter dg = new DateGetter();

    /** Standard DateRater. */
    private DateEvaluator dr = new DateEvaluator(PageDateType.publish);

    private  ArrayList<AbstractDate> list = new ArrayList<AbstractDate>();

    private String url;
    private Document document;
    private boolean reference = false;
    private boolean archive = false;


    public void setPubMod(PageDateType pub_mod){
    	this.dr = new DateEvaluator(pub_mod);
    }
    
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
     * Writes results in list, get it by getter-methods.
     * 
     * @param url for webpage.
     */
    public void evaluate(Document doc) {
        this.url = doc.getDocumentURI();
        this.document = doc;
        evaluate();
    }

    /**
     * Look up for all dates of webpage and rate them.<br>
     * Writes results in list, get it by getter-methods.<br>
     * <br>
     * Be sure than an url is set. Otherwise use <b>evaluate(String url)</b>.
     */
    public  void evaluate() {
    	
        if (this.url != null) {
            if(document != null){
            	dg.setDocument(document);
            }
            dg.setURL(url);
            dg.setTechReference(reference);
            dg.setTechArchive(archive);
            ArrayList<AbstractDate> dates = dg.getDate();
            HashMap<AbstractDate, Double> ratedDates = dr.rate(dates);
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
    public AbstractDate getBestRatedDate() {
    	AbstractDate date = new ExtractedDate();
        if (list != null && list.size() > 0) {
            ArrayList<AbstractDate> orderedList = list;
            Collections.sort(orderedList, new RatedDateComparator<AbstractDate>());
            date = orderedList.get(0);
        }
        return date;
    }
    
   /**
    * 
    * @param url
    * @param externalSearch
    * @return
    */
    public static ExtractedDate getBestRatedDate(String url,boolean externalSearch) {
    	return getBestRatedDate(url, externalSearch, PageDateType.publish);
    }
    
    /**
     * 
     * @param url
     * @param externalSearch
     * @return
     */
     public static ExtractedDate getBestRatedDate(String url, boolean externalSearch, PageDateType pub_mod) {
     	ArrayList<ExtractedDate> list = new ArrayList<ExtractedDate>();
     	DateGetter dg = new DateGetter();
     	DateEvaluator dr = new DateEvaluator(pub_mod);
     	if (url != null) {
             ArrayList<ExtractedDate> dates = new ArrayList<ExtractedDate>();
             HashMap<ExtractedDate, Double> ratedDates;
             
             dg.setURL(url);
             dg.setTechReference(externalSearch);
             dg.setTechArchive(externalSearch);
             dates = dg.getDate();

             ratedDates = dr.rate(dates);
             list = DateArrayHelper.hashMapToArrayList(ratedDates);
         }
         ExtractedDate date = new ExtractedDate();
         if (list != null && list.size() > 0) {
             ArrayList<ExtractedDate> orderedList = list;
             Collections.sort(orderedList, new RatedDateComparator<ExtractedDate>());
             date = orderedList.get(0);
         }
         return date;
     }
    
    /**
     * 
     * @param url
     * @param externalSearch
     * @return
     */
     public static ExtractedDate getBestRatedDate(Document document,boolean externalSearch, PageDateType pub_mod) {
     	ArrayList<ExtractedDate> list = new ArrayList<ExtractedDate>();
     	DateGetter dg = new DateGetter();
     	DateEvaluator dr = new DateEvaluator(pub_mod);
     	if (document != null) {
             ArrayList<ExtractedDate> dates = new ArrayList<ExtractedDate>();
             HashMap<ExtractedDate, Double> ratedDates;
             
             dg.setDocument(document);
             dg.setURL(document.getDocumentURI());
             dg.setTechReference(externalSearch);
             dg.setTechArchive(externalSearch);
             dates = dg.getDate();

             ratedDates = dr.rate(dates);
             list = DateArrayHelper.hashMapToArrayList(ratedDates);
         }
         ExtractedDate date = new ExtractedDate();
         if (list != null && list.size() > 0) {
             ArrayList<ExtractedDate> orderedList = list;
             Collections.sort(orderedList, new RatedDateComparator<ExtractedDate>());
             date = orderedList.get(0);
         }
         return date;
     }
     
     /**
      * 
      * @param url
      * @param externalSearch
      * @return
      */
      public static ExtractedDate getBestRatedDate(Document document,boolean externalSearch) {
          return getBestRatedDate(document,externalSearch, PageDateType.publish);
      }

    /**
     * Found the best rate and returns all dates with this rate. <br>
     * 
     * @return
     *         ArraylList of dates.
     */
    public ArrayList<AbstractDate> getAllBestRatedDate() {
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
    public ArrayList<AbstractDate>  getAllBestRatedDate(boolean onlyFullDates) {
        ArrayList<AbstractDate> dates = list;
        if (onlyFullDates) {
            dates = DateArrayHelper.filter(dates, DateArrayHelper.FILTER_FULL_DATE);
        }
        double rate = DateArrayHelper.getHighestRate(dates);
        dates = DateArrayHelper.getRatedDates(dates, rate);
        Collections.sort(dates, new RatedDateComparator<AbstractDate>());
        return dates;
    }

    /**
     * Returns all found and rated dates.
     * 
     * @return all dates.
     */
    public ArrayList<AbstractDate> getAllDates() {
        ArrayList<AbstractDate> sorted = list;
        Collections.sort(sorted, new RatedDateComparator<AbstractDate>());
        return sorted;
    }

    /**
     * By default the technique reference is turned off. <br>
     * Use this to active it.
     */
    public void activateReference() {
        this.reference = true;
    }
    /**
     * By default the technique archive is turned off. <br>
     * Use this to active it.
     */
    public void activateArchive() {
        this.archive = true;
    }
}
