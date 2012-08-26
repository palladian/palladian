package ws.palladian.extraction.date;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import ws.palladian.extraction.date.comparators.RatedDateComparator;
import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.extraction.date.helper.DateArrayHelper;
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
    
    private static final double THRESHOLD_GROUP_1 = 0.15;
    private static final double THRESHOLD_GROUP_2 = 0.24;
    private static final double THRESHOLD_GROUP_3 = 0.18;
    private static final double THRESHOLD_GROUP_4 = 0.16;
    private static final double THRESHOLD_GROUP_5 = 0.14;
    private static final double THRESHOLD_GROUP_6 = 0.13;
    private static final double THRESHOLD_GROUP_7 = 0.17;
    private static final double THRESHOLD_GROUP_8 = 0.26;
    
	private final DateGetter dateGetter;

	private final DateEvaluator dateEvaluator;

	private List<ExtractedDate> extractedDates = new ArrayList<ExtractedDate>();

	private String url;
	private Document document;
	
	public WebPageDateEvaluator(PageDateType dateType) {
	    dateEvaluator = new DateEvaluator(dateType);
	    dateGetter = new DateGetter();
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

	/**
	 * Look up for all dates of webpage and rate them.<br>
	 * Writes results in list, get it by getter-methods.<br>
	 * <br>
	 */
	public void evaluate() {
		if (this.url != null) {
			if (document != null) {
				dateGetter.setDocument(document);
			}
			dateGetter.setURL(url);
			List<ExtractedDate> dates = dateGetter.getDate();
			Map<ExtractedDate, Double> ratedDates = dateEvaluator.rate(dates);
			this.extractedDates = new ArrayList<ExtractedDate>(ratedDates.keySet());
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
		if (extractedDates != null && extractedDates.size() > 0) {
		    Collections.sort(extractedDates, new RatedDateComparator());
		    ExtractedDate date = extractedDates.get(0);
			if (limit < 0) {
			    if (date instanceof ContentDate) {
					double size = 1 / ((ContentDate) date).getRelSize();
					if (0 < size && size <= 1) {
						limit = THRESHOLD_GROUP_1;
					} else if (1 < size && size <= 2) {
						limit = THRESHOLD_GROUP_2;
					} else if (2 < size && size <= 3) {
						limit = THRESHOLD_GROUP_3;
					} else if (3 < size && size <= 5) {
						limit = THRESHOLD_GROUP_4;
					} else if (5 < size && size <= 10) {
						limit = THRESHOLD_GROUP_5;
					} else if (10 < size && size <= 20) {
						limit = THRESHOLD_GROUP_6;
					} else if (20 < size && size <= 50) {
						limit = THRESHOLD_GROUP_7;
					} else if (50 < size) {
						limit = THRESHOLD_GROUP_8;
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
		List<ExtractedDate> dates = extractedDates;
		if (onlyFullDates) {
			dates = DateArrayHelper.filterFullDate(dates);
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
		Collections.sort(extractedDates, new RatedDateComparator());
		return extractedDates;
	}
}
