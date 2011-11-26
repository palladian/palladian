package ws.palladian.retrieval.search;

import java.text.ParseException;
import java.util.List;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.search.services.BingImageSearcher;

public class UsageExamples {
    
    public static void main(String[] args) throws ParseException {
        // DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
        // System.out.println(dateFormat.format(new Date()));
        // Date date = dateFormat.parse("2011-11-24T06:46:00Z");
        // System.out.println(date);
        // System.exit(0);

        // Searcher<WebResult> searcher = new BingNewsSearcher();
        Searcher<WebImageResult> searcher = new BingImageSearcher();
        searcher.setResultCount(10);
        List<WebImageResult> result = searcher.search("apple");
        CollectionHelper.print(result);
        // System.out.println(BaseBingSearcher.requestCount);
        
//        Searcher<WebResult> searcher = new GoogleSearcher();
//        // Searcher<WebImageResult> searcher = new GoogleImageSearcher();
//        List<WebResult> queryResult = searcher.search("apple");
//        CollectionHelper.print(queryResult);
        
//        Searcher<WebResult> searcher = new HakiaNewsSearcher();
//        List<WebResult> result = searcher.search("apple");
//        CollectionHelper.print(result);


    }

}
