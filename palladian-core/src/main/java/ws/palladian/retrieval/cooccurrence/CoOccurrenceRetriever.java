package ws.palladian.retrieval.cooccurrence;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.search.SearcherException;
import ws.palladian.retrieval.search.web.GoogleSearcher;
import ws.palladian.retrieval.search.web.TwitterSearcher;
import ws.palladian.retrieval.search.web.WebResult;
import ws.palladian.retrieval.search.web.WebSearcher;

/**
 * <p>
 * The Co-Occurrence Retriever takes two terms as input and finds how often they occur together in
 * documents/sentences/phrases that can be found in the given searchers.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class CoOccurrenceRetriever {
    
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CoOccurrenceRetriever.class);

    /** Specify how far or close the terms must be to count the co-occurrence. */
    private CoOccurrenceContext coOccurrenceContext;

    /** The number of results to analyze per searcher. */
    private int numberOfResults = 10;
    
    private Language language;

    public CoOccurrenceRetriever(CoOccurrenceContext coOccurrenceContext, int numberOfResults,
            Language language) {
        this.coOccurrenceContext = coOccurrenceContext;
        this.numberOfResults = numberOfResults;
        this.language = language;
    }

    public CoOccurrenceStatistics getCoOccurrenceStatistics(String term1, String term2,
            Collection<WebSearcher<WebResult>> searchers, boolean caseInsensitive) {

        return getCoOccurrenceStatistics(term1, term2, new HashSet<String>(), searchers, caseInsensitive);

    }

    public CoOccurrenceStatistics getCoOccurrenceStatistics(String term1, String term2,
            Collection<String> contextTerms, Collection<WebSearcher<WebResult>> searchers, boolean caseInsensitive) {

        CoOccurrenceStatistics coOccurrenceStatistics = new CoOccurrenceStatistics(term1, term2);

        DocumentRetriever documentRetriever = new DocumentRetriever();

        String query = buildQuery(term1, term2, contextTerms);
        
        for (WebSearcher<WebResult> searcher : searchers) {
            
            try {
                List<WebResult> webResults = searcher.search(query, numberOfResults, language);
                
                for (WebResult webResult : webResults) {

                    String pageText = webResult.getSummary();

                    if (webResult.getUrl() != null) {
                        pageText = documentRetriever.getText(webResult.getUrl());
                    }

                    if (pageText != null) {
                        findCoOccurrences(pageText, coOccurrenceStatistics, searcher, caseInsensitive);
                    }

                }
            } catch (SearcherException e) {
                LOGGER.error("Searcher exception while searching {}", query, e);
            }
        }

        return coOccurrenceStatistics;
    }

    private String buildQuery(String term1, String term2, Collection<String> contextTerms) {
        String query = "\"" + term1 + "\" \"" + term2 + "\"";

        for (String term : contextTerms) {
            query += " \"" + term + "\"";
        }

        return query;
    }

    private void findCoOccurrences(String pageText, CoOccurrenceStatistics stats, WebSearcher<WebResult> searcher,
            boolean caseInsensitive) {
        
        String term1 = stats.getTerm1();
        String term2 = stats.getTerm2();

        if (caseInsensitive) {
            pageText = pageText.toLowerCase();
            term1 = term1.toLowerCase();
            term2 = term2.toLowerCase();
        }

        if (coOccurrenceContext.equals(CoOccurrenceContext.DOCUMENT)) {
            
            if (pageText.contains(term1) && pageText.contains(term2)) {
                stats.addCoOccurrence(searcher.getName(), pageText);
            }
            
        } else if (coOccurrenceContext.equals(CoOccurrenceContext.SENTENCE)) {
            
            pageText = StringHelper.clean(pageText);
            
            List<String> sentences = Tokenizer.getSentences(pageText);
            for (String sentence : sentences) {

                if (sentence.contains(term1) && sentence.contains(term2)) {
                    stats.addCoOccurrence(searcher.getName(), sentence);
                }

            }
            

        } else if (coOccurrenceContext.equals(CoOccurrenceContext.CONTEXT_200_CHARS)) {
            
            pageText = StringHelper.clean(pageText);

            String regexp = term1 + ".{0,200}" + term2;

            List<String> matches = StringHelper.getRegexpMatches(regexp, pageText);

            if (!stats.getTerm1().equals(stats.getTerm2())) {
                regexp = term2 + ".{0,200}" + term1;
                matches.addAll(StringHelper.getRegexpMatches(regexp, pageText));
            }

            for (String match : matches) {
                stats.addCoOccurrence(searcher.getName(), match);
            }
        
        }
        
    }

    /**
     * <p>
     * Example of how to use the CoOccurrence Retriever.
     * </p>
     */
    public static void main(String[] args) {
        CoOccurrenceRetriever coOccurrenceRetriever = new CoOccurrenceRetriever(CoOccurrenceContext.CONTEXT_200_CHARS,
                10, Language.GERMAN);
        
        Collection<WebSearcher<WebResult>> searchers = new HashSet<WebSearcher<WebResult>>();
        searchers.add(new GoogleSearcher());
        searchers.add(new TwitterSearcher());
        
        // CoOccurrenceStatistics stats = coOccurrenceRetriever.getCoOccurrenceStatistics("Hugo Cabret", "oscar",
        // searchers, true);

        CoOccurrenceStatistics stats = coOccurrenceRetriever.getCoOccurrenceStatistics("financial meltdown", "2008",
                searchers, true);

        // document: [term1=Hugo Cabret, term2=oscar, coOccurrences={Google=10, Twitter=7}]
        // sentence: [term1=Hugo Cabret, term2=oscar, coOccurrences={Google=33, Twitter=2}]
        // char 200: [term1=Hugo Cabret, term2=oscar, coOccurrences={Google=64, Twitter=7}]
        System.out.println(stats);
    }

}
