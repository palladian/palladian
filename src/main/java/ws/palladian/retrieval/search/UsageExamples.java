package ws.palladian.retrieval.search;

import java.util.List;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.search.web.GoogleImageSearcher;
import ws.palladian.retrieval.search.web.GoogleSearcher;
import ws.palladian.retrieval.search.web.WebImageResult;
import ws.palladian.retrieval.search.web.WebResult;
import ws.palladian.retrieval.search.web.WebSearcher;
import ws.palladian.retrieval.search.web.WebSearcherLanguage;

public class UsageExamples {

    public static void main(String[] args) {

        // create a web searcher for the Bing search engine
        WebSearcher<WebResult> searcher = new GoogleSearcher();
        // set maximum number of desired results
        searcher.setResultCount(50);
        // set search result language to English
        searcher.setLanguage(WebSearcherLanguage.ENGLISH);
        // search for "Jim Carrey"
        List<WebResult> webResults = searcher.search("Jim Carrey");
        // print the results
        CollectionHelper.print(webResults);
        
        // create a web searcher to search for images on Google
        WebSearcher<WebImageResult> imageSearcher = new GoogleImageSearcher();
        // search for images with "Jim Carrey"
        List<WebImageResult> imageResults = imageSearcher.search("Jim Carrey");
        // print the results
        CollectionHelper.print(imageResults);

    }

}
