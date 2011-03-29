package ws.palladian.daterecognition.technique;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.w3c.dom.Document;

import ws.palladian.daterecognition.DateConverter;
import ws.palladian.daterecognition.DateEvaluator;
import ws.palladian.daterecognition.DateGetter;
import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.dates.ReferenceDate;
import ws.palladian.extraction.PageAnalyzer;
import ws.palladian.helper.date.DateArrayHelper;
import ws.palladian.helper.date.DateComparator;
import ws.palladian.web.DocumentRetriever;

/**
 * This class tries get dates in lined pages.<br>
 * Therefore it uses all the other techniques.
 * 
 * @author Martin Gregor
 * 
 */
public class ReferenceDateGetter extends TechniqueDateGetter {

    @Override
    public ArrayList<ReferenceDate> getDates() {
        return getDates(-1);
    }

    /**
     * Returns a List of found dates.<br>
     * Look up in a max number of links.
     * 
     * @param maxLinks Number after look up links will stop getting dates.
     * @return
     */
    public ArrayList<ReferenceDate> getDates(int maxLinks) {
        ArrayList<ReferenceDate> result = new ArrayList<ReferenceDate>();
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
    private static ArrayList<ReferenceDate> getReferenceDates(Document document, int maxLinks) {
        ArrayList<ReferenceDate> dates = new ArrayList<ReferenceDate>();
        if (document != null) {
            DocumentRetriever c = new DocumentRetriever();
            Iterator<String> linksTo = PageAnalyzer.getLinks(document, true, true).iterator();
            DateGetter dateGetter = new DateGetter();
            dateGetter.setTechReference(false);
            dateGetter.setTechArchive(false);

            DateComparator dc = new DateComparator();
            DateEvaluator de = new DateEvaluator(PageDateType.publish);
            int i = 0;
            while (linksTo.hasNext()) {
                String link = linksTo.next();
                dateGetter.setURL(link);
                ArrayList<ExtractedDate> referenceDates = dateGetter.getDate();
                HashMap<ExtractedDate, Double> evaluatedDates = de.rate(referenceDates);
                double rate = DateArrayHelper.getHighestRate(evaluatedDates);
                referenceDates = DateArrayHelper.getRatedDates(evaluatedDates, rate);
                ReferenceDate refDate = DateConverter.convert((ExtractedDate) dc.getOldestDate(referenceDates),
                        DateConverter.TECH_REFERENCE);
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
