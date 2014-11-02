package ws.palladian.extraction.location.persistence;

import ws.palladian.extraction.location.PalladianLocationExtractor;
import ws.palladian.extraction.location.disambiguation.HeuristicDisambiguation;
import ws.palladian.extraction.location.disambiguation.LocationDisambiguation;
import ws.palladian.extraction.location.evaluation.LocationExtractionEvaluator;
import ws.palladian.helper.StopWatch;
import ws.palladian.persistence.DatabaseManagerFactory;

public class Benchmark {

    public static void main(String[] args) throws Exception {
        LocationExtractionEvaluator evaluator = new LocationExtractionEvaluator();
        evaluator.addDataset("/Users/pk/Dropbox/Uni/Datasets/TUD-Loc-2013/0-all");
        evaluator.addDataset("/Users/pk/temp/LGL-converted/0-all");
        evaluator.addDataset("/Users/pk/temp/CLUST-converted/0-all");

        // QuickDtModel model = ModelCache.getInstance().getLocationModel();
        // FeatureBasedDisambiguation disambiguation = new FeatureBasedDisambiguation(model);
        LocationDisambiguation disambiguation = new HeuristicDisambiguation();
        // Filter<String> filter = FileHelper.deserialize("/Users/pk/temp/bloomFilter.ser");
        LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        // Directory directory = new MMapDirectory(new File("/Users/pk/temp/luceneLocationDatabase"));

        // default MySQL database, no optimizations
        evaluator.addExtractor(new PalladianLocationExtractor(database, disambiguation));

        // MySQL database + Bloom blocker
        // LocationSource blocker = new BlockingLocationSource(database, filter);
        // evaluator.addExtractor(new PalladianLocationExtractor(blocker, disambiguation));

        // MySQL database + cache
        // LocationSource cache = new CachingLocationSource(database);
        // evaluator.addExtractor(new PalladianLocationExtractor(cache, disambiguation));

        // MySQL database + Bloom blocker + cache
        // LocationSource blockerCache = new CachingLocationSource(database);
        // blockerCache = new BlockingLocationSource(blockerCache, filter);
        // evaluator.addExtractor(new PalladianLocationExtractor(blockerCache, disambiguation));

        // Lucene index
        // LocationSource lucene = new LuceneLocationSource(directory);
        // evaluator.addExtractor(new PalladianLocationExtractor(lucene, disambiguation));

        // Lucene index + Bloom blocker
        // LocationSource luceneBlocker = new LuceneLocationSource(directory);
        // luceneBlocker = new BlockingLocationSource(luceneBlocker, filter);
        // evaluator.addExtractor(new PalladianLocationExtractor(luceneBlocker, disambiguation));

        // Lucene index + cache
        // LocationSource luceneCache = new LuceneLocationSource(directory);
        // luceneCache = new CachingLocationSource(luceneCache);
        // evaluator.addExtractor(new PalladianLocationExtractor(luceneCache, disambiguation));

        // Lucene index + Bloom blocker + cache
        // LocationSource luceneBlockerCache = new LuceneLocationSource(directory);
        // luceneBlockerCache = new CachingLocationSource(luceneBlockerCache);
        // luceneBlockerCache = new BlockingLocationSource(luceneBlockerCache, filter);
        // evaluator.addExtractor(new PalladianLocationExtractor(luceneBlockerCache, disambiguation));

        // in-memory store (requires ~ 2 GB of heap)
        // InMemoryLocationStore inMemoryStore = new InMemoryLocationStore(database);
        // evaluator.addExtractor(new PalladianLocationExtractor(inMemoryStore, disambiguation));

        // in-memory store + Bloom blocker
        // LocationSource memoryBlocker = new BlockingLocationSource(inMemoryStore, filter);
        // evaluator.addExtractor(new PalladianLocationExtractor(memoryBlocker, disambiguation));

        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 3; i++) {
            database.resetForPerformanceCheck();
            evaluator.runAll(false);
        }
        System.out.println("took " + stopWatch);
    }


}
