package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Web searcher uses an unofficial Javascript call to get DuckDuckGo search results.
 * </p>
 * 
 * @author David Urbansky
 */
public final class DuckDuckGoSearcher extends WebSearcher<WebResult> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DuckDuckGoSearcher.class);

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    public DuckDuckGoSearcher() {
        super();
    }

    @Override
    public List<WebResult> search(String query, int resultCount, Language language) {

        Set<String> urls = new HashSet<String>();
        List<WebResult> result = new ArrayList<WebResult>();
            
        try {

            int entriesPerPage = 10;

            paging: for (int page = 0; page <= 999; page++) {

                String requestUrl = "http://duckduckgo.com/d.js?l=us-en&p=1&s="
                        + entriesPerPage * page + "&q=" + UrlHelper.urlEncode(query);
                
                HttpResult httpResult = retriever.httpGet(requestUrl);
                String content = new String(httpResult.getContent());
                content = content.replace("if (nrn) nrn('d',", "");
                content = content.replace("}]);","}])");
                JSONArray jsonArray = new JSONArray(content);
                
                TOTAL_REQUEST_COUNT.incrementAndGet();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject object = jsonArray.getJSONObject(i);
                    
                    if (!urls.add(object.getString("u"))) {
                        continue;
                    }
                    String summary = object.getString("a");
                    summary = StringEscapeUtils.unescapeHtml(summary);
                    summary = HtmlHelper.stripHtmlTags(summary);
                    
                    String title = object.getString("t");
                    title = StringEscapeUtils.unescapeHtml(title);
                    title = HtmlHelper.stripHtmlTags(title);
                    
                    WebResult webResult = new WebResult(object.getString("u"), title, summary, getName());
                    result.add(webResult);

                    if (result.size() >= resultCount) {
                        break paging;
                    }
                }

            }

        } catch (HttpException e) {
            LOGGER.error(e);
        } catch (JSONException e) {
            LOGGER.error(e);
        }

        return result;
    }

    @Override
    public String getName() {
        return "DuckDuckGo";
    }

    /**
     * Gets the number of HTTP requests sent to Scroogle.
     * 
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.get();
    }

    public static void main(String[] args) throws SearcherException  {        
        DuckDuckGoSearcher ddg = new DuckDuckGoSearcher();
        List<String> urls = ddg.searchUrls("cinefreaks", 5);
        CollectionHelper.print(urls);
        
        List<WebResult> webResults = ddg.search("cinefreaks", 5);
        CollectionHelper.print(webResults);
    }
}
