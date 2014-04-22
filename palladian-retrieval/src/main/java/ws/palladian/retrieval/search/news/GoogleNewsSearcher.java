package ws.palladian.retrieval.search.news;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.BaseGoogleSearcher;

/**
 * <p>
 * Google news search.
 * </p>
 * 
 * @author Philipp Katz
 */
@Deprecated
public final class GoogleNewsSearcher extends BaseGoogleSearcher<WebContent> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleNewsSearcher.class);

    private static final String DATE_PATTERN = "E, dd MMM yyyy HH:mm:ss Z";

    @Override
    protected String getBaseUrl() {
        return "http://ajax.googleapis.com/ajax/services/search/news";
    }

    @Override
    protected WebContent parseResult(JsonObject resultData) throws JsonException {
        BasicWebContent.Builder builder = new BasicWebContent.Builder();
        builder.setTitle(resultData.getString("titleNoFormatting"));
        builder.setSummary(resultData.getString("content"));
        builder.setUrl(resultData.getString("unescapedUrl"));
        builder.setPublished(parseDate(resultData.getString("publishedDate")));
        return builder.create();
    }

    @Override
    public String getName() {
        return "Google News";
    }

    static Date parseDate(String dateString) {
        try {
            return new SimpleDateFormat(DATE_PATTERN, Locale.US).parse(dateString);
        } catch (ParseException e) {
            LOGGER.warn("Could not parse date string '" + dateString + "'.");
            return null;
        }
    }

}
