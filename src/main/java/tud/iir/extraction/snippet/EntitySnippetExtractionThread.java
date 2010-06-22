package tud.iir.extraction.snippet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Snippet;
import tud.iir.web.AggregatedResult;
import tud.iir.web.Crawler;
import tud.iir.web.RankAggregation;
import tud.iir.web.SourceAggregator;

/**
 * The EntitySnippetExtractionThread extracts snippets for one given entity. Therefore, extracting snippets can be parallelized on the entity level.
 * 
 * This class is described in detail in "Friedrich, Christopher. WebSnippets - Extracting and Ranking of entity-centric knowledge from the Web. Diploma thesis,
 * Technische Universität Dresden, April 2010".
 * 
 * @author Christopher Friedrich
 */
public class EntitySnippetExtractionThread extends Thread {

    private static final Logger logger = Logger.getLogger(EntitySnippetExtractionThread.class);
    private static final Logger snippets_logger = Logger.getLogger("snippets");

    private Entity entity = null;

    public EntitySnippetExtractionThread(ThreadGroup threadGroup, String name, Entity entity) {
        super(threadGroup, name);
        this.entity = entity;
    }

    @Override
    public void run() {
        SnippetExtractor.getInstance().increaseThreadCount();
        long t1 = System.currentTimeMillis();

        // aggregate webresults for snippet extraction
        SourceAggregator sa = new SourceAggregator();
        List<AggregatedResult> webresults = sa.aggregateWebResults(entity, SourceAggregator.IFM, 100, RankAggregation.RANK_AVERAGE);

        // extract snippet_candidates from webresults for given entity
        SnippetBuilder sb = new SnippetBuilder();
        List<Snippet> snippet_candidates = new ArrayList<Snippet>();
        for (AggregatedResult webresult : webresults) {
            // TODO: implement some logic, that remembers which webresults to process

            List<Snippet> wr_snippets = sb.extractSnippets(entity, webresult, SnippetBuilder.DOCUMENT_SNIPPETS);
            if (wr_snippets != null) {
                snippet_candidates.addAll(wr_snippets);
            }
        }

        // discard (near) duplicates
        SnippetDuplicateDetection.removeDuplicates(snippet_candidates, SnippetDuplicateDetection.PLAIN);

        // extract features from snippets for snippet scoring
        for (Snippet snippet : snippet_candidates) {
            SnippetFeatureExtractor.setFeatures(snippet);
            snippet.classify();
        }

        // finally, save remaining snippet_candidates
        entity.addSnippets(snippet_candidates);

        logger.info("Thread finished in " + DateHelper.getRuntime(t1) + "s, snippets for \"" + entity.getName() + "\" were sought.");

        HashSet<String> sources = new HashSet<String>();
        for (Snippet snippet : snippet_candidates) {
            sources.add(Crawler.getCleanURL(snippet.getAggregatedResult().getSource().getUrl()));
        }

        snippets_logger.info("Thread for \"" + entity.getName() + "\" " + "(" + entity.getID() + ") " + "finished in " + DateHelper.getRuntime(t1) + "s, "
                + snippet_candidates.size() + " snippets, from " + sources.size() + " sources were extracted.");

        SnippetExtractor.getInstance().decreaseThreadCount();
    }
}