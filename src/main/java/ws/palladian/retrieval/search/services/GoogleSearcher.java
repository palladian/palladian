package ws.palladian.retrieval.search.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.retrieval.search.WebResult;
import ws.palladian.retrieval.search.WebSearcherManager;

public class GoogleSearcher extends BaseWebSearcher implements WebSearcher {
    
    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(GoogleSearcher.class);
    
    private static final AtomicInteger requestCount = new AtomicInteger();
    
    public GoogleSearcher() {
        super();
    }

    @Override
    public List<WebResult> search(String query, int resultCount, WebSearcherLanguage language) {
        
        List<WebResult> webresults = new ArrayList<WebResult>();

        // set the preferred language
        // (http://www.google.com/cse/docs/resultsxml.html#languageCollections)
        String languageString = "lang_en";
        if (language == WebSearcherLanguage.GERMAN) {
            languageString = "lang_de";
        }

        int rank = 1;
        int urlsCollected = 0;
        int grabCount = (int) Math.ceil(resultCount / 8.0); // divide by 8
        // because 8
        // results will
        // be responded
        // by
        // each query
        // Google returns max. 8 pages/64 results --
        // http://code.google.com/intl/de/apis/ajaxsearch/documentation/reference.html#_property_GSearch
        grabCount = Math.min(grabCount, 8);
        // System.out.println(grabSize);
        for (int i = 0; i < grabCount; i++) {

            // rsz=large will respond 8 results
            try {
                JSONObject jsonOBJ = retriever
                        .getJSONDocument("http://ajax.googleapis.com/ajax/services/search/web?v=1.0&start=" + i * 8
                                + "&rsz=large&safe=off&lr=" + languageString + "&q=" + query);

                // System.out.println(jsonOBJ.toString(1));
                // in the first iteration find the maximum of available pages
                // and limit the search to those
                if (i == 0) {
                    JSONArray pages;
                    if (jsonOBJ.getJSONObject("responseData") != null
                            && jsonOBJ.getJSONObject("responseData").getJSONObject("cursor") != null
                            && jsonOBJ.getJSONObject("responseData").getJSONObject("cursor").getJSONArray("pages") != null) {
                        pages = jsonOBJ.getJSONObject("responseData").getJSONObject("cursor").getJSONArray("pages");
                        int lastStartPage = pages.getJSONObject(pages.length() - 1).getInt("start");
                        if (lastStartPage < grabCount) {
                            grabCount = lastStartPage + 1;
                        }
                    }
                }

                JSONArray results = jsonOBJ.getJSONObject("responseData").getJSONArray("results");
                int resultSize = results.length();
                for (int j = 0; j < resultSize; ++j) {
                    if (urlsCollected < resultCount) {
                        JSONObject jsonObject = results.getJSONObject(j);
                        String title = jsonObject.getString("titleNoFormatting");
                        String summary = jsonObject.getString("content");
                        String currentURL = jsonObject.getString("unescapedUrl");

                        WebResult webresult = new WebResult(WebSearcherManager.GOOGLE, rank, currentURL, title, summary);
                        rank++;

                        LOGGER.debug("google retrieved url " + currentURL);
                        webresults.add(webresult);

                        ++urlsCollected;
                    } else {
                        break;
                    }
                }

            } catch (JSONException e) {
                LOGGER.error(e.getMessage());
            }
            
            requestCount.incrementAndGet();
            LOGGER.debug("google requests: " + requestCount.get());
        }

        return webresults;
    }

    @Override
    public String getName() {
        return "Google";
    }
}
