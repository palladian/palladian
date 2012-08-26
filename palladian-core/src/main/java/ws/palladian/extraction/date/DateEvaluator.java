package ws.palladian.extraction.date;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.extraction.date.rater.ContentDateRater;
import ws.palladian.helper.date.ExtractedDate;

/**
 * This class is responsible for rating dates. <br>
 * Because all searched dates are equal and are neither <i>publish</i> nor <i>modified</i>, it is here to decide which
 * type to use. <br>
 * 
 * In this Kairos Version, only ContentDates will be searched and rated. <br>
 * Therefore only the ContentDateRater will be used, but here is the right place to add more functionality, if there is
 * need.
 * 
 * @author Martin Gregor
 * 
 */
public class DateEvaluator {

    private final ContentDateRater contentDateRater;

    /**
     * Standard constructor.
     */
    public DateEvaluator() {
        this(PageDateType.PUBLISH);
    }

    /**
     * Standard constructor.
     */
    public DateEvaluator(PageDateType dateType) {
        contentDateRater = new ContentDateRater(dateType);
    }

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
        List<T> dates = DateArrayHelper.filterByRange(extractedDates);
        Map<T, Double> contResult = new HashMap<T, Double>();

        List<ContentDate> contDates = DateArrayHelper.filter(dates, ContentDate.class);
        List<ContentDate> contFullDates = DateArrayHelper.filterFullDate(contDates);

        contResult.putAll((Map<? extends T, ? extends Double>)contentDateRater.rate(contFullDates));
        evaluatedDates.putAll(contResult);
        DateRaterHelper.writeRateInDate(evaluatedDates);

        return evaluatedDates;
    }

}
