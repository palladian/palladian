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
import ws.palladian.extraction.date.helper.DateArrayHelper;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.date.dates.ReferenceDate;
import ws.palladian.helper.html.HtmlHelper;

/**
 * This class tries get dates in lined pages.<br>
 * Therefore it uses all the other techniques.
 * 
 * @author Martin Gregor
 * 
 */
public class ReferenceDateGetter extends TechniqueDateGetter<ReferenceDate> {

    @Override
    public List<ReferenceDate> getDates() {
        return getDates(-1);
    }

    /**
     * Returns a List of found dates.<br>
     * Look up in a max number of links.
     * 
     * @param maxLinks Number after look up links will stop getting dates.
     * @return
     */
    public List<ReferenceDate> getDates(int maxLinks) {
        List<ReferenceDate> result = new ArrayList<ReferenceDate>();
        if (document != null) {
            result = getReferenceDates(document, maxLinks);
        }
        return result;
    }

    /**
     * 
     * A crawler searches links of document.<br>
     * Each linked page will be researched for dates, these will be rated too.
     * 
     * @param document Document with outgoing links.
     * @param maxLinks Number after look up links will stop getting dates.
     * @return
     */
    private static List<ReferenceDate> getReferenceDates(Document document, int maxLinks) {
        List<ReferenceDate> dates = new ArrayList<ReferenceDate>();
        if (document != null) {
            Set<String> linksTo = HtmlHelper.getLinks(document, true, true);
            DateGetter dateGetter = new DateGetter();

            DateComparator dc = new DateComparator();
            DateEvaluator de = new DateEvaluator(PageDateType.publish);
            int i = 0;
            
            for (String link : linksTo) {
                dateGetter.setURL(link);
                List<ExtractedDate> referenceDates = dateGetter.getDate();
                Map<ExtractedDate, Double> evaluatedDates = de.rate(referenceDates);
                double rate = DateArrayHelper.getHighestRate(evaluatedDates);
                referenceDates = DateArrayHelper.getRatedDates(evaluatedDates, rate);
                //ReferenceDate refDate = DateConverter.convert(dc.getOldestDate(referenceDates), DateType.ReferenceDate);
                ReferenceDate refDate = new ReferenceDate(dc.getOldestDate(referenceDates));
                refDate.setRate(rate);
                dates.add(refDate);
                if (i == maxLinks) {
                    break;
                }
                i++;
            }
        }
        return dates;
    }

}
