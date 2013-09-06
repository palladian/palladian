package ws.palladian.extraction.date.getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;

import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.extraction.date.comparators.RatedDateComparator;
import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.extraction.date.dates.ReferenceDate;
import ws.palladian.extraction.date.rater.ContentDateRater;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.HtmlHelper;

/**
 * This class tries get dates in linked pages.<br>
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

    private final ContentDateGetter contentDateGetter;
    private final ContentDateRater contentDateRater;

    public ReferenceDateGetter() {
        contentDateGetter = new ContentDateGetter();
        contentDateRater = new ContentDateRater(PageDateType.PUBLISH);
    }

    @Override
    public List<ReferenceDate> getDates(Document document) {
        List<ReferenceDate> result = new ArrayList<ReferenceDate>();
        Set<String> links = HtmlHelper.getLinks(document, true, true);

        for (String link : links) {
            List<ContentDate> contentDates = contentDateGetter.getDates(link);
            List<RatedDate<ContentDate>> ratedContentDates = contentDateRater.rate(contentDates);
            Collections.sort(ratedContentDates, new RatedDateComparator());

            // keep all with highest rate
            // TODO test this
            List<ContentDate> highRatedContentDates = CollectionHelper.newArrayList();
            double highestRate = 0.0;
            for (RatedDate<ContentDate> ratedDate : ratedContentDates) {
                if (ratedDate.getRate() > highestRate) {
                    highestRate = ratedDate.getRate();
                    highRatedContentDates.clear();
                    highRatedContentDates.add(ratedDate.getDate());
                } else if (ratedDate.getRate() == highestRate) {
                    highRatedContentDates.add(ratedDate.getDate());
                }
            }

            // take the oldest date
            Collections.sort(highRatedContentDates, new DateComparator());

            if (highRatedContentDates.size() > 0) {
                result.add(new ReferenceDate(highRatedContentDates.get(0)));
            }

        }
        return result;
    }
}
