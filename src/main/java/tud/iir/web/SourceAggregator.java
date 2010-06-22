package tud.iir.web;

import java.util.ArrayList;
import java.util.List;

import tud.iir.control.Controller;
import tud.iir.extraction.snippet.SnippetQuery;
import tud.iir.extraction.snippet.SnippetQueryFactory;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;

/**
 * A collection of source aggregation algorithms. All algorithms take an entity as input and return an list of AggregatedResults as output, given the provided
 * aggregation technique and rank aggregation technique.
 * 
 * This class is described in detail in "Friedrich, Christopher. WebSnippets - Extracting and Ranking of entity-centric knowledge from the Web. Diploma thesis,
 * Technische Universität Dresden, April 2010".
 * 
 * @author Christopher Friedrich
 */
public class SourceAggregator {

    public static final int IFM = 0;

    public SourceAggregator() {
    }

    public int[] getIndices(Entity currentEntity) {
        int[] indices = { SourceRetrieverManager.YAHOO, SourceRetrieverManager.GOOGLE,
        // SourceRetrieverManager.MICROSOFT,
                // SourceRetrieverManager.HAKIA,
                // SourceRetrieverManager.YAHOO_BOSS,
                SourceRetrieverManager.BING, SourceRetrieverManager.TWITTER, SourceRetrieverManager.GOOGLE_BLOGS,
        // SourceRetrieverManager.TEXTRUNNER,
        };

        return indices;
    }

    /**
     * The main function to access the algorithms implemented in this class. It takes an entity as input and provides a list of AggregatedResults as output.
     * 
     * @param currentEntity - The entity to retrieve AggregatedResults for
     * @param method - The source aggregation technique to use
     * @param maxResults - The maximum lenght of the results list
     * @param rankAggregationMethod - the rank aggregation method to use
     * @return The list of AggregatedResults
     */
    public List<AggregatedResult> aggregateWebResults(Entity currentEntity, int method, int maxResults, int rankAggregationMethod) {
        int[] indices = this.getIndices(currentEntity);
        return this.aggregateWebResults(currentEntity, method, indices, maxResults, rankAggregationMethod);
    }

    public List<AggregatedResult> aggregateWebResults(Entity currentEntity, int method, int[] indices, int maxResults, int rankAggregationMethod) {

        List<List<WebResult>> results = null;

        // TODO: index (search engine) selection -> some configuration for
        // TODO: select which INDEX to query how FREQUENT with wich QUERIES

        switch (method) {
            case IFM:
                results = aggregateWebResultsIFM(currentEntity, indices, maxResults);
                break;
        }

        // TODO: optimize and don't re-process the same sources over and over again
        // TODO: remove sources for that entity, that have already been processed

        // TODO: optimize this approach for dynamic content pages

        return RankAggregation.aggregate(results, rankAggregationMethod, maxResults);
    }

    /**
     * This technique is based on IFM described in "Searching with context", R.Kraft et al., WWW 2006
     */
    private List<List<WebResult>> aggregateWebResultsIFM(Entity currentEntity, int[] indices, int maxResults) {

        List<List<WebResult>> results = new ArrayList<List<WebResult>>();

        SnippetQuery sq = SnippetQueryFactory.getInstance().createEntityQuery(currentEntity);

        // per search engine, retrieve top-k webresults

        SourceRetriever sr = new SourceRetriever();
        for (int index : indices) {

            sr.setSource(index);
            sr.setResultCount(maxResults);

            String[] querySet = sq.getQuerySet();
            for (int i = 0; i < querySet.length; ++i) {
                ArrayList<WebResult> webresults = sr.getWebResults(querySet[i], index, false);
                results.add(webresults);
            }
        }

        return results;
    }

    public static void main(String[] abc) {
        Controller.getInstance();

        Entity entity = new Entity("iPhone 3GS", new Concept("Mobile Phone"));

        // some default indices to query
        int[] indices = { SourceRetrieverManager.YAHOO, SourceRetrieverManager.GOOGLE,
        // SourceRetrieverManager.HAKIA,
                // SourceRetrieverManager.YAHOO_BOSS,
                SourceRetrieverManager.BING, SourceRetrieverManager.GOOGLE_BLOGS,
        // SourceRetrieverManager.MICROSOFT,
        // SourceRetrieverManager.TWITTER,
        // SourceRetrieverManager.TEXTRUNNER,
        };

        int maxResults = 20;
        int aggregationMethod = SourceAggregator.IFM;

        SourceAggregator sa = new SourceAggregator();
        List<AggregatedResult> awrs = sa.aggregateWebResults(entity, aggregationMethod, indices, maxResults, RankAggregation.RANK_AVERAGE);
        for (AggregatedResult ar : awrs) {
            System.out.println(ar.getAggregatedRank() + ": (" + ar.getWebresults().size() + ") " + ar.getSource().getUrl());
        }
    }
}
