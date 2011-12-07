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
        // search for "Jim Carrey", 50 results, English language
        List<WebResult> webResults = searcher.search("Jim Carrey", 50, WebSearcherLanguage.ENGLISH);
        // print the results
        CollectionHelper.print(webResults);
        
        // create a web searcher to search for images on Google
        WebSearcher<WebImageResult> imageSearcher = new GoogleImageSearcher();
        // search for ten images with "Jim Carrey"
        List<WebImageResult> imageResults = imageSearcher.search("Jim Carrey", 10);
        // print the results
        CollectionHelper.print(imageResults);

    }

}
