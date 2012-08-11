package ws.palladian.extraction.date;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.w3c.dom.Document;

import ws.palladian.extraction.date.comparators.RatedDateComparator;
import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.date.ExtractedDate;

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

	private List<ExtractedDate> list = new ArrayList<ExtractedDate>();

	private String url;
	private Document document;
	
	public static final double DEFAULT_THRESHOLD_GROUP_1 = 0.15;
	public static final double DEFAULT_THRESHOLD_GROUP_2 = 0.24;
	public static final double DEFAULT_THRESHOLD_GROUP_3 = 0.18;
	public static final double DEFAULT_THRESHOLD_GROUP_4 = 0.16;
	public static final double DEFAULT_THRESHOLD_GROUP_5 = 0.14;
	public static final double DEFAULT_THRESHOLD_GROUP_6 = 0.13;
	public static final double DEFAULT_THRESHOLD_GROUP_7 = 0.17;
	public static final double DEFAULT_THRESHOLD_GROUP_8 = 0.26;

	public void setPubMod(PageDateType pub_mod) {
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

	public void setDocument(Document document) {
	    this.url = document.getDocumentURI();
		this.document = document;
	}

//	/**
//	 * Look up for all dates of webpage and rate them.<br>
//	 * Writes results in list, get it by getter-methods.
//	 * 
//	 * @param url
//	 *            for webpage.
//	 */
//	public void evaluate(String url) {
//		this.url = url;
//		evaluate();
//	}

//	/**
//	 * Look up for all dates of webpage and rate them.<br>
//	 * Writes results in list, get it by getter-methods.<br><br>
//	 * Uses url of document. <br>
//	 * If you like to use different urls and documents use setters.
//	 * 
//	 * @param doc
//	 *            for webpage.
//	 */
//	public void evaluate(Document doc) {
//		this.url = doc.getDocumentURI();
//		this.document = doc;
//		evaluate();
//	}

	/**
	 * Look up for all dates of webpage and rate them.<br>
	 * Writes results in list, get it by getter-methods.<br>
	 * <br>
	 * Be sure an url is set. Otherwise use <b>evaluate(String url)</b>.
	 */
	public void evaluate() {
		if (this.url != null) {
			if (document != null) {
				dg.setDocument(document);
			}
			dg.setURL(url);
			List<ExtractedDate> dates = dg.getDate();
			Map<ExtractedDate, Double> ratedDates = dr.rate(dates);
			this.list = new ArrayList<ExtractedDate>(ratedDates.keySet());
		}
	}

	/**
	 * Found the beste rated date in list and returns it.<br>
	 * If more then one date have same best rate, the first date of an ordered
	 * list will be returned. <br>
	 * The list will be sort with {@link RatedDateComparator}. ExtractedDate is
	 * only super class, date can be cast to original type. <br>
	 * Get orginal type by using getType() of date.
	 * 
	 * @return Best rated date.
	 */
	public ExtractedDate getBestRatedDate() {
		return getBestRatedDate(-1);
	}

	/**
	 * Returns the best rated date that is over a limit.
	 * @param limit Minimum confidence of best rated date. <br>
	 * Use '-1' for a fix set limits, depending on number of dates of a webpage.
	 * @return The best rated date. <br>
	 *  Returns <b>null</b> if there is no date or rate of best date is below the limit.
	 */
	private ExtractedDate getBestRatedDate(double limit) {
		ExtractedDate result = null;
		if (list != null && list.size() > 0) {
			//List<ExtractedDate> orderedList = list;
//			Collections.sort(orderedList,
//					new RatedDateComparator());
//		    ExtractedDate date = orderedList.get(0);
		    Collections.sort(list, new RatedDateComparator());
		    ExtractedDate date = list.get(0);
			if (limit < 0) {
				//DateType dateType = date.getType();
				//if (dateType.equals(DateType.ContentDate)) {
			    if (date instanceof ContentDate) {
					ConfigHolder configHolder = ConfigHolder.getInstance();
					PropertiesConfiguration config = configHolder.getConfig();
					double size = 1 / ((ContentDate) date).getRelSize();
					if (0 < size && size <= 1) {
						limit = config.getDouble("threshold.group1", DEFAULT_THRESHOLD_GROUP_1);
					} else if (1 < size && size <= 2) {
						limit = config.getDouble("threshold.group2", DEFAULT_THRESHOLD_GROUP_2);
					} else if (2 < size && size <= 3) {
						limit = config.getDouble("threshold.group3", DEFAULT_THRESHOLD_GROUP_3);
					} else if (3 < size && size <= 5) {
						limit = config.getDouble("threshold.group4", DEFAULT_THRESHOLD_GROUP_4);
					} else if (5 < size && size <= 10) {
						limit = config.getDouble("threshold.group5", DEFAULT_THRESHOLD_GROUP_5);
					} else if (10 < size && size <= 20) {
						limit = config.getDouble("threshold.group6", DEFAULT_THRESHOLD_GROUP_6);
					} else if (20 < size && size <= 50) {
						limit = config.getDouble("threshold.group7", DEFAULT_THRESHOLD_GROUP_7);
					} else if (50 < size) {
						limit = config.getDouble("threshold.group8", DEFAULT_THRESHOLD_GROUP_8);
					}
				} else {
					limit = 0;
				}
			}
			if (date.getRate() >= limit) {
				result = date;
			}
		}
		return result;
	}

	/**
	 * Found the best rate and returns all dates with this rate. <br>
	 * 
	 * @return ArraylList of dates.
	 */
	List<ExtractedDate> getAllBestRatedDate() {
		return getAllBestRatedDate(false);
	}

	/**
	 * Found the best rate and returns all dates with this rate.
	 * 
	 * @param onlyFullDates
	 *            True for look at only dates with minimum year, month and day. <br>
	 *            False for look at all dates.
	 * @return ArraylList of dates.
	 */
	private List<ExtractedDate> getAllBestRatedDate(boolean onlyFullDates) {
		List<ExtractedDate> dates = list;
		if (onlyFullDates) {
			dates = DateArrayHelper.filter(dates,
					DateArrayHelper.FILTER_FULL_DATE);
		}
		double rate = DateArrayHelper.getHighestRate(dates);
		dates = DateArrayHelper.getRatedDates(dates, rate);
		Collections.sort(dates, new RatedDateComparator());
		return dates;
	}

	/**
	 * Returns all found and rated dates.
	 * 
	 * @return all dates.
	 */
	List<ExtractedDate> getAllDates() {
		List<ExtractedDate> sorted = list;
		Collections.sort(sorted, new RatedDateComparator());
		return sorted;
	}
}
