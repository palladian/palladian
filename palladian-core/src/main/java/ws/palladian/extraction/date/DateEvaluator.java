package ws.palladian.extraction.date;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.extraction.date.rater.ContentDateRater;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.date.dates.ContentDate;

/**
 * This class is responsible for rating dates. <br>
 * Because all searched dates are equal and are neither <i>publish</i> nor <i>modified</i>, it is here to decide which type to use. <br>
 * 
 * In this Kairos Version, only ContentDates will be searched and rated. <br>
 * Therefore only the ContentDateRater will be used, but here is the right place to add more functionality, if there is need. 
 * 
 * @author Martin Gregor
 * 
 */
public class DateEvaluator {

//    private String url;

    private final ContentDateRater contentDateRater;

    /**
     * Standard constructor.
     */
    public DateEvaluator() {
        //setPubMod(PageDateType.publish);
        this(PageDateType.publish);
    }

    /**
     * Standard constructor.
     */
    public DateEvaluator(PageDateType dateType) {
        //setPubMod(pub_mod);
        //this(null, pub_mod);
        contentDateRater = new ContentDateRater(dateType);
    }

//    /**
//     * Constructor setting url.
//     * 
//     * @param url
//     */
//    public DateEvaluator(String url, PageDateType pub_mod) {
//        this.url = url;
//        // setPubMod(pub_mod);
//        cdr = new ContentDateRater(pub_mod);
//    }

//    /**
//     * Use this method to decide between publish and modified dates. 
//     * @param pub_mod
//     */
//    private void setPubMod(PageDateType pub_mod) {
//        cdr = new ContentDateRater(pub_mod);
//    }


    /**
     * Main method of this class. It rates all date and returns them with their confidence.<br>
     * In this Version of Kairos, only the ContentDateRater is used.<br>
     * For future extending add functionality here. 
     * 
     * @param <T>
     * @param extractedDates ArrayList of ExtractedDates.
     * @return HashMap of dates, with rate as value.
     */
    @SuppressWarnings("unchecked")
    public <T extends ExtractedDate> Map<T, Double> rate(List<T> extractedDates) {
        Map<T, Double> evaluatedDates = new HashMap<T, Double>();
        List<T> dates = DateArrayHelper.filter(extractedDates, DateArrayHelper.FILTER_IS_IN_RANGE);
        Map<T, Double> contResult = new HashMap<T, Double>();

        List<ContentDate> contDates = DateArrayHelper.filter(dates, ContentDate.class);
        List<ContentDate> contFullDates = (ArrayList<ContentDate>) DateArrayHelper.filter(contDates,
                DateArrayHelper.FILTER_FULL_DATE);

        contResult.putAll((Map<? extends T, ? extends Double>) contentDateRater.rate(contFullDates));
        evaluatedDates.putAll(contResult);
        DateRaterHelper.writeRateInDate(evaluatedDates);

        return evaluatedDates;
    }

//    /**
//     * Set url.
//     * 
//     * @param url
//     */
//    public void setUrl(String url) {
//        this.url = url;
//    }

//    /**
//     * Getter for url.
//     * 
//     * @return Url as a String.
//     */
//    public String getUrl() {
//        return url;
//    }
}
