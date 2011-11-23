package ws.palladian.retrieval.search.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.preprocessing.multimedia.ExtractedImage;
import ws.palladian.retrieval.search.WebResult;
import ws.palladian.retrieval.search.WebSearcherManager;

public final class GoogleSearcher extends BaseWebSearcher implements WebSearcher {

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
        int grabCount = (int) Math.ceil(resultCount / 8.0); // 
        // divide by 8 because 8 results will be responded by each query
        // Google returns max. 8 pages/64 results
        // http://code.google.com/intl/de/apis/ajaxsearch/documentation/reference.html#_property_GSearch
        grabCount = Math.min(grabCount, 8);
        // System.out.println(grabSize);
        for (int i = 0; i < grabCount; i++) {

            // rsz=large will respond 8 results
            try {
                JSONObject jsonOBJ = retriever
                        .getJSONDocument("http://ajax.googleapis.com/ajax/services/search/web?v=1.0&start=" + i * 8
                                + "&rsz=large&safe=off&lr=" + languageString + "&q=" + UrlHelper.urlEncode(query));

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
    public int getHitCount(String query) {

        int hitCount = 0;

        try {
            JSONObject jsonOBJ = retriever
                    .getJSONDocument("http://ajax.googleapis.com/ajax/services/search/web?v=1.0&rsz=small&safe=off&q="
                            + UrlHelper.urlEncode(query));

            if (jsonOBJ.getJSONObject("responseData") != null
                    && jsonOBJ.getJSONObject("responseData").getJSONObject("cursor") != null
                    && jsonOBJ.getJSONObject("responseData").getJSONObject("cursor").has("estimatedResultCount")) {
                hitCount = jsonOBJ.getJSONObject("responseData").getJSONObject("cursor").getInt("estimatedResultCount");
            }

        } catch (JSONException e) {
            LOGGER.error(e);
        }

        return hitCount;
    }
    
    public List<ExtractedImage> searchImages(String searchQuery, boolean exact, String[] matchContent) {
        if (exact) {
            searchQuery = "\"" + searchQuery + "\"";
        }

        List<ExtractedImage> images = new ArrayList<ExtractedImage>();

        int urlsCollected = 0;
        int grabSize = (int) Math.ceil(getResultCount() / 8.0); // divide by 8
        // because 8
        // results will
        // be responded
        // by
        // each query

        for (int i = 0; i < grabSize; i++) {

            try {
                // rsz=large will respond 8 results
                JSONObject jsonOBJ = retriever
                        .getJSONDocument("http://ajax.googleapis.com/ajax/services/search/images?v=1.0&start=" + i * 8
                                + "&rsz=large&safe=off&lr=lang_en&q=" + UrlHelper.urlEncode(searchQuery));

                // in the first iteration find the maximum of available pages
                // and limit the search to those
                if (i == 0) {
                    JSONArray pages;
                    if (jsonOBJ.getJSONObject("responseData") != null
                            && jsonOBJ.getJSONObject("responseData").getJSONObject("cursor") != null
                            && jsonOBJ.getJSONObject("responseData").getJSONObject("cursor").getJSONArray("pages") != null) {
                        pages = jsonOBJ.getJSONObject("responseData").getJSONObject("cursor").getJSONArray("pages");
                        int lastStartPage = pages.getJSONObject(pages.length() - 1).getInt("start");
                        if (lastStartPage < grabSize) {
                            grabSize = lastStartPage + 1;
                        }
                    }
                }

                JSONArray results = jsonOBJ.getJSONObject("responseData").getJSONArray("results");
                int resultSize = results.length();
                for (int j = 0; j < resultSize; ++j) {
                    LOGGER.debug("next iteration " + j + "/" + resultSize);
                    if (urlsCollected < getResultCount()) {
                        String currentURL = (String) results.getJSONObject(j).get("unescapedUrl");

                        // only accept jpg and png images
                        if (currentURL.indexOf(".jpg") == -1 && currentURL.indexOf(".png") == -1) {
                            continue;
                        }

                        String imageCaption = (String) results.getJSONObject(j).get("content");
                        LOGGER.debug("google retrieved url " + currentURL);

                        // all match content keywords must appear in the caption
                        // of the image
                        int matchCount = 0;
                        for (String element : matchContent) {
                            if (imageCaption.toLowerCase().indexOf(element.toLowerCase()) > -1) {
                                matchCount++;
                            }
                        }
                        if (matchCount < matchContent.length) {
                            continue;
                        }

                        ExtractedImage image = new ExtractedImage();
                        image.setURL(currentURL);
                        image.setWidth(Integer.valueOf((String) results.getJSONObject(j).get("width")));
                        image.setHeight(Integer.valueOf((String) results.getJSONObject(j).get("height")));
                        image.setRankCount(j * (i + 1) + 1);
                        images.add(image);
                        ++urlsCollected;
                        LOGGER.debug("urls collected: " + urlsCollected);
                    } else {
                        break;
                    }
                }
                LOGGER.debug("grab " + i + "/" + grabSize);

            } catch (JSONException e) {
                LOGGER.error(e.getMessage());
            }
            
            requestCount.incrementAndGet();
            LOGGER.info("google requests: " + requestCount.get());
        }

        return images;
    }

    @Override
    public String getName() {
        return "Google";
    }
}
