package tud.iir.extraction.snippet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import tud.iir.extraction.ExtractionProcessManager;
import tud.iir.helper.DateHelper;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Snippet;
import tud.iir.web.Crawler;
import tud.iir.web.SourceRetriever;
import tud.iir.web.WebResult;

/**
 * The EntitySnippetExtractionThread extracts snippets for one given entity. Extracting snippets can be parallelized on
 * the entity level.
 * 
 * @author David Urbansky
 */
public class EntitySnippetExtractionThread extends Thread {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(EntitySnippetExtractionThread.class);

    /** The entity for which snippets should be extracted. */
    private Entity entity = null;

    public EntitySnippetExtractionThread(ThreadGroup threadGroup, String name, Entity entity) {
        super(threadGroup, name);
        this.entity = entity;
    }

    @Override
    public void run() {
        SnippetExtractor.getInstance().increaseThreadCount();
        long t1 = System.currentTimeMillis();

        // collect web sources for snippet extraction
        List<WebResult> results = new ArrayList<WebResult>();

        SourceRetriever sr = new SourceRetriever();
        sr.setResultCount(SnippetExtractor.RESULTS_PER_SNIPPET);
        sr.setSource(ExtractionProcessManager.getSourceRetrievalSite());

        // create queries for snippet URL retrieval
        SnippetQuery sq = SnippetQueryFactory.getInstance().createEntityQuery(entity);

        String[] querySet = sq.getQuerySet();
        for (String query : querySet) {
            if (interrupted()) {
                break;
            }

            List<WebResult> webResults = sr.getWebResults(query, false);
            results.addAll(webResults);
        }

        // extract snippet candidates from URLs for the given entity
        SnippetBuilder sb = new SnippetBuilder();
        List<Snippet> snippetCandidates = new ArrayList<Snippet>();
        for (WebResult webresult : results) {

            if (interrupted()) {
                break;
            }

            List<Snippet> extractedSnippets = sb.extractSnippets(entity, webresult);
            if (extractedSnippets != null) {
                snippetCandidates.addAll(extractedSnippets);
            }
        }

        // discard duplicates
        SnippetDuplicateDetection.removeDuplicates(snippetCandidates);

        // save snippet candidates in entity
        entity.addSnippets(snippetCandidates);

        LOGGER.info("Thread finished in " + DateHelper.getRuntime(t1) + "s, snippets for \"" + entity.getName()
                + "\" were sought.");

        Set<String> sources = new HashSet<String>();
        for (Snippet snippet : snippetCandidates) {
            sources.add(Crawler.getCleanURL(snippet.getWebResult().getSource().getUrl()));
        }

        LOGGER.info("Thread for \"" + entity.getName() + "\" " + "(" + entity.getID() + ") " + "finished in "
                + DateHelper.getRuntime(t1) + "s, " + snippetCandidates.size() + " snippets, from " + sources.size()
                + " sources were extracted");

        SnippetExtractor.getInstance().decreaseThreadCount();
    }
}