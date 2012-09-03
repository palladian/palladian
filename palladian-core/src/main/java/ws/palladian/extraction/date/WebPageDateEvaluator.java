package ws.palladian.extraction.date;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;

import ws.palladian.extraction.date.comparators.RatedDateComparator;
import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.extraction.date.getter.ContentDateGetter;
import ws.palladian.extraction.date.helper.DateExtractionHelper;
import ws.palladian.extraction.date.rater.ContentDateRater;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * Use this class to rate a webpage. <br>
 * Set an url and use evaluate to get and rate all dates of a webpage. <br>
 * Different methods return found dates.
 * 
 * @author Martin Gregor
 * @author Philipp Katz
 */
public final class WebPageDateEvaluator {

    private static final double THRESHOLD_GROUP_1 = 0.15;
    private static final double THRESHOLD_GROUP_2 = 0.24;
    private static final double THRESHOLD_GROUP_3 = 0.18;
    private static final double THRESHOLD_GROUP_4 = 0.16;
    private static final double THRESHOLD_GROUP_5 = 0.14;
    private static final double THRESHOLD_GROUP_6 = 0.13;
    private static final double THRESHOLD_GROUP_7 = 0.17;
    private static final double THRESHOLD_GROUP_8 = 0.26;

    private WebPageDateEvaluator() {
        // helper class, prevent instantiation.
    }

    public static List<RatedDate<? extends ExtractedDate>> getDates(Document document, PageDateType type) {
        ContentDateGetter contentDateGetter = new ContentDateGetter();
        List<ContentDate> dates = contentDateGetter.getDates(document);

        List<RatedDate<? extends ExtractedDate>> ratedDates = rate(dates, type);
        Collections.sort(ratedDates, new RatedDateComparator());

        return ratedDates;
    }

    public static RatedDate<? extends ExtractedDate> getBestDate(Document document, PageDateType type) {
        RatedDate<? extends ExtractedDate> result = null;

        List<RatedDate<? extends ExtractedDate>> dates = getDates(document, type);
        if (dates.size() > 0) {
            RatedDate<? extends ExtractedDate> bestRatedDate = dates.get(0);
            ExtractedDate bestDate = bestRatedDate.getDate();
            if (bestDate instanceof ContentDate) {
                ContentDate bestContentDate = (ContentDate)bestDate;
                double size = 1 / bestContentDate.getRelSize();
                double limit = 0;
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
                if (bestRatedDate.getRate() >= limit) {
                    result = bestRatedDate;
                }
            }
        }
        return result;
    }
    
    public static RatedDate<? extends ExtractedDate> getBestDate(String url, PageDateType type) {
        try {
            HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
            HttpResult httpResult = httpRetriever.httpGet(url);
            DocumentParser htmlParser = ParserFactory.createHtmlParser();
            Document document = htmlParser.parse(httpResult);
            return getBestDate(document, type);
        } catch (HttpException e) {
            return null;
        } catch (ParserException e) {
            return null;
        }
    }
    
    /**
     * It rates all date and returns them with their confidence.<br>
     * In this Version of Kairos, only the ContentDateRater is used.<br>
     * For future extending add functionality here.
     * 
     * @param <T>
     * @param extractedDates ArrayList of ExtractedDates.
     * @return HashMap of dates, with rate as value.
     */
    public static List<RatedDate<? extends ExtractedDate>> rate(List<? extends ExtractedDate> extractedDates, PageDateType dateType) {
        List<RatedDate<? extends ExtractedDate>> result = new ArrayList<RatedDate<? extends ExtractedDate>>();
        
        ContentDateRater contentDateRater = new ContentDateRater(dateType);

        List<? extends ExtractedDate> dates = DateExtractionHelper.filterByRange(extractedDates);
        List<ContentDate> contDates = DateExtractionHelper.filter(dates, ContentDate.class);
        List<ContentDate> contFullDates = DateExtractionHelper.filterFullDate(contDates);
        
        List<RatedDate<ContentDate>> ratedContentDates = contentDateRater.rate(contFullDates);
        result.addAll(ratedContentDates);
        
        return result;
    }

}
