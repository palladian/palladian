package ws.palladian.retrieval.search.services;

import java.util.List;

import ws.palladian.retrieval.search.WebResult;

public interface WebSearcher {

    List<WebResult> search(String query);
    
}
