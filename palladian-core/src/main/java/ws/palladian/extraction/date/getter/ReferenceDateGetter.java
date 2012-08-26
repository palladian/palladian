package ws.palladian.extraction.date.getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;

import ws.palladian.extraction.date.DateEvaluator;
import ws.palladian.extraction.date.DateGetter;
import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.dates.ReferenceDate;
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.html.HtmlHelper;

/**
 * This class tries get dates in lined pages.<br>
 * Therefore it uses all the other techniques.
 * 
 * 
 * A crawler searches links of document.<br>
 * Each linked page will be researched for dates, these will be rated too.
 * 
 * @author Martin Gregor
 * 
 */
public class ReferenceDateGetter extends TechniqueDateGetter<ReferenceDate> {

    @Override
    public List<ReferenceDate> getDates(Document document) {
        List<ReferenceDate> dates = new ArrayList<ReferenceDate>();
            Set<String> links = HtmlHelper.getLinks(document, true, true);
            DateGetter dateGetter = new DateGetter();

            DateComparator dateComparator = new DateComparator();
            DateEvaluator dateEvaluator = new DateEvaluator(PageDateType.PUBLISH);
            
            for (String link : links) {
                dateGetter.setURL(link);
                List<ExtractedDate> referenceDates = dateGetter.getDate();
                Map<ExtractedDate, Double> evaluatedDates = dateEvaluator.rate(referenceDates);
                double rate = DateArrayHelper.getHighestRate(evaluatedDates);
                referenceDates = DateArrayHelper.getRatedDates(evaluatedDates, rate);
                ReferenceDate refDate = new ReferenceDate(dateComparator.getOldestDate(referenceDates));
                refDate.setRate(rate);
                dates.add(refDate);
            }
        return dates;
    }

}
