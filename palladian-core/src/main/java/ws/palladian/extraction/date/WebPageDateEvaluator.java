package ws.palladian.extraction.date;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ws.palladian.extraction.date.comparators.RatedDateComparator;
import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.extraction.date.getter.ContentDateGetter;
import ws.palladian.extraction.date.helper.DateExtractionHelper;
import ws.palladian.extraction.date.rater.ContentDateRater;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.XPathHelper;
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

//    private static final double THRESHOLD_GROUP_1 = 0.15;
//    private static final double THRESHOLD_GROUP_2 = 0.24;
//    private static final double THRESHOLD_GROUP_3 = 0.18;
//    private static final double THRESHOLD_GROUP_4 = 0.16;
//    private static final double THRESHOLD_GROUP_5 = 0.14;
//    private static final double THRESHOLD_GROUP_6 = 0.13;
//    private static final double THRESHOLD_GROUP_7 = 0.17;
//    private static final double THRESHOLD_GROUP_8 = 0.26;

    private WebPageDateEvaluator() {
        // helper class, prevent instantiation.
    }

    public static List<RatedDate<ExtractedDate>> getDates(Document document, PageDateType type) {
        Validate.notNull(document, "document must not be null");
        Validate.notNull(type, "type must not be null");
        
        ContentDateGetter contentDateGetter = new ContentDateGetter();
        List<ContentDate> dates = contentDateGetter.getDates(document);

        List<RatedDate<ExtractedDate>> ratedDates = rate(dates, type);
        Collections.sort(ratedDates, new RatedDateComparator());

        return ratedDates;
    }

    public static RatedDate<ExtractedDate> getBestDate(Document document, PageDateType type) {
        Validate.notNull(document, "document must not be null");
        Validate.notNull(type, "type must not be null");

        List<RatedDate<ExtractedDate>> dates = getDates(document, type);
        if (dates.size() > 0) {
            RatedDate<ExtractedDate> bestRatedDate = dates.get(0);
            return bestRatedDate;
            
//            ExtractedDate bestDate = bestRatedDate.getDate();
//            if (bestDate instanceof ContentDate) {
//                ContentDate bestContentDate = (ContentDate)bestDate;
//                double size = 1 / bestContentDate.getRelSize();
//                double limit = 0;
//
//                // XXX are those strange thresholds really necessary?
//
//                if (0 < size && size <= 1) {
//                    limit = THRESHOLD_GROUP_1;
//                } else if (1 < size && size <= 2) {
//                    limit = THRESHOLD_GROUP_2;
//                } else if (2 < size && size <= 3) {
//                    limit = THRESHOLD_GROUP_3;
//                } else if (3 < size && size <= 5) {
//                    limit = THRESHOLD_GROUP_4;
//                } else if (5 < size && size <= 10) {
//                    limit = THRESHOLD_GROUP_5;
//                } else if (10 < size && size <= 20) {
//                    limit = THRESHOLD_GROUP_6;
//                } else if (20 < size && size <= 50) {
//                    limit = THRESHOLD_GROUP_7;
//                } else if (50 < size) {
//                    limit = THRESHOLD_GROUP_8;
//                }
//                if (bestRatedDate.getRate() >= limit) {
//                    return bestRatedDate;
//                }
//            }
        }
        return null;
    }
    
    /**
     * <p>
     * Quick and dirty method to get the publication date from an HTML5 webpage, by looking for <code>article</code>
     * nodes with <code>datetime</code> nodes.
     * </p>
     * 
     * @param document The HTML5 document from which to extract the publication date, not <code>null</code>.
     * @return The publictation date, if it could be extracted from the document, or <code>null</code>.
     */
    public static ExtractedDate getBestPubDateHtml5(Document document) {
        Validate.notNull(document, "document must not be null");

        List<Node> articleNodes = XPathHelper.getXhtmlNodes(document, "//article");
        
        // determine the longest article node.
        Node nodeToCheck = null;
        int longest = -1;
        for (Node articleNode : articleNodes) {
            int articleLength = HtmlHelper.getInnerXml(articleNode).length();
            if (articleLength > longest) {
                nodeToCheck = articleNode;
                longest = articleLength;
            }
        }
        
        // we couldn't identify a document node, consider the whole document
        if (nodeToCheck == null) {
            nodeToCheck = document;
        }

        if (nodeToCheck != null) {
            List<Node> timeNodes = XPathHelper.getXhtmlNodes(nodeToCheck, ".//time");
            for (Node timeNode : timeNodes) {
                NamedNodeMap attributes = timeNode.getAttributes();
                if (attributes.getNamedItem("pubdate") != null) {
                    Node dateTime = attributes.getNamedItem("datetime");
                    if (dateTime != null) {
                        return DateParser.findDate(dateTime.getTextContent());
                    }
                }
            }
        }
        return null;
    }

    public static RatedDate<ExtractedDate> getBestDate(String url, PageDateType type) {
        Validate.notEmpty(url, "url must not be empty");
        Validate.notNull(type, "type must not be null");
        
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
     * <p>
     * Rate the specified {@link ExtractedDate}s and return a {@link List} with {@link RatedDate}s signifying their
     * rates. Currently, only {@link ContentDate}s are considered, but this might be extended in the future.
     * </p>
     * 
     * @param extractedDates List with {@link ExtractedDate}s, not <code>null</code>.
     * @param type The {@link PageDateType} specifying whether to extract publish or creation dates.
     * @return A List of {@link RatedDate}s.
     */
    @SuppressWarnings("unchecked")
    public static List<RatedDate<ExtractedDate>> rate(List<? extends ExtractedDate> extractedDates, PageDateType type) {
        Validate.notNull(extractedDates, "extractedDates must not be null");
        Validate.notNull(type, "type must not be null");
        
        List<RatedDate<? extends ExtractedDate>> result = CollectionHelper.newArrayList();

        ContentDateRater contentDateRater = new ContentDateRater(type);

        List<? extends ExtractedDate> filtered = DateExtractionHelper.filterByRange(extractedDates);
        
        // currently, only ContentDates are considered
        List<ContentDate> contentDates = DateExtractionHelper.filter(filtered, ContentDate.class);
        List<ContentDate> fullContentDates = DateExtractionHelper.filterFullDate(contentDates);
        List<RatedDate<ContentDate>> ratedContentDates = contentDateRater.rate(fullContentDates);
        result.addAll(ratedContentDates);
        
        return (List<RatedDate<ExtractedDate>>)((List<?>)result);
    }
}
