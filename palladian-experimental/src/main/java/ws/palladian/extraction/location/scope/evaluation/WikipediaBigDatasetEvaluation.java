package ws.palladian.extraction.location.scope.evaluation;

import java.io.File;
import java.io.IOException;

import org.apache.commons.collections.bag.SynchronizedSortedBag;
import org.apache.commons.lang3.Validate;

import ws.palladian.classification.text.BayesScorer;
import ws.palladian.classification.text.DictionaryBuilder;
import ws.palladian.classification.text.DictionaryTrieModel;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.classification.text.HashedDictionaryMapModel;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.PruningStrategies;
import ws.palladian.classification.text.TermSelector;
import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.evaluation.ImmutableLocationDocument;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.extraction.location.scope.DictionaryScopeDetector;
import ws.palladian.extraction.location.scope.DictionaryScopeDetector.DictionaryScopeDetectorLearner;
import ws.palladian.extraction.location.scope.DictionaryScopeDetector.DictionaryScopeModel;
import ws.palladian.extraction.location.scope.KNearestNeighborScopeDetector;
import ws.palladian.extraction.location.scope.KNearestNeighborScopeDetector.NearestNeighborScopeDetectorLearner;
import ws.palladian.extraction.location.scope.KNearestNeighborScopeDetector.NearestNeighborScopeModel;
import ws.palladian.extraction.location.scope.MultiStepDictionaryScopeDetector;
import ws.palladian.helper.collection.CollectionHelper;

import java.util.Iterator;
import java.util.function.Consumer;
import ws.palladian.helper.functional.ConsumerIteratorAdapter;
import java.util.function.Function;
import java.util.function.Predicate;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.wiki.MarkupCoordinate;
import ws.palladian.retrieval.wiki.MediaWikiUtil;
import ws.palladian.retrieval.wiki.WikiPage;

/**
 * Evaluation script for the text-based scope detection using the big Wikipedia dump.
 * 
 * @author Philipp Katz
 */
@SuppressWarnings("unused")
public class WikipediaBigDatasetEvaluation {

    // private static final File WIKI_DUMP = new File("/Users/pk/Desktop/enwiki-20140614-pages-articles.xml.bz2");
    // private static final File WIKI_DUMP = new File("/Volumes/LaCie500/enwiki-latest-pages-articles.xml.bz2");
    // private static final File WIKI_DUMP = new File("/Volumes/iMac HD/temp/enwiki-20130503-pages-articles.xml.bz2");
    // private static final File WIKI_DUMP = new File("/Volumes/iMac SSD 2/Location_Lab_Revisited/enwiki-20220501-pages-articles-multistream.xml.bz2");
    private static final File WIKI_DUMP = new File("/Volumes/Archiv/Location_Lab_Revisited/enwiki-20220501-pages-articles-multistream.xml.bz2");

    private static final Function<WikiPage, LocationDocument> CONVERTER = new Function<WikiPage, LocationDocument>() {
        private static final String UNDETERMINED = "undetermined";

        @Override
        public LocationDocument apply(WikiPage input) {
            // copy MarkupCoordinate, as it contains lots of junk taking memory
            MarkupCoordinate tmp = input.getCoordinate();
            GeoCoordinate scope = new ImmutableGeoCoordinate(tmp.getLatitude(), tmp.getLongitude());
            Location scopeLocation = new ImmutableLocation(-1, UNDETERMINED, LocationType.UNDETERMINED, scope, null);
            return new ImmutableLocationDocument(input.getTitle(), input.getCleanText(), null, scopeLocation);
        }
    };

    /** Split by modulo. */
    private static final class ModSplitter implements Predicate<WikiPage> {

        private final int mod;
        private final int min;
        private final int max;

        /** 1:1 split in odd/even IDs. */
        @SuppressWarnings("unused")
        public ModSplitter(boolean even) {
            this(2, even ? 0 : 1, even ? 0 : 1);
        }

        public ModSplitter(int mod, int min, int max) {
            Validate.isTrue(mod > 1, "mod must be greater 1");
            Validate.isTrue(min >= 0, "min must be greater/equal 0");
            Validate.isTrue(max >= min, "max must be greater/equal min");
            this.mod = mod;
            this.min = min;
            this.max = max;
        }

        @Override
        public boolean test(WikiPage item) {
            int tmp = item.getId() % mod;
            return min <= tmp && tmp <= max;
        }

    }

    public static void main(String[] args) throws Exception {
        new ConsumerIteratorAdapter<WikiPage>() {
            @Override
            protected void produce(Consumer<WikiPage> action) throws Exception {
                MediaWikiUtil.parseDump(WIKI_DUMP, action);
            }

            @Override
            protected void consume(Iterable<WikiPage> iterable) {
//                Iterator<WikiPage> temp = CollectionHelper.limit(iterable, 10000).iterator();
//                int count = 0;
//                while (temp.hasNext()) {
//                    WikiPage page = temp.next();
//                    if (page.getCoordinate() != null) {
//                        System.out.println(page.getTitle());
//                        count++;
//                    }
//                }
//                System.out.println("# pages with coordinates: " + count);
//                // # pages with coordinates: 671
//                // # before: # pages with coordinates: 600
//                System.exit(0);

                iterable = CollectionHelper.filter(iterable, (Predicate<WikiPage>) item -> {
                    if (item.getNamespaceId() != WikiPage.MAIN_NAMESPACE) {
                        return false;
                    }
                    if (item.getTitle().toLowerCase().startsWith("list of")) {
                        return false;
                    }
                    if (item.getCoordinate() == null) {
                        return false;
                    }
                    return true;
                });

                // make a 90:10 training:test split
                Iterable<WikiPage> trainingPages = CollectionHelper.filter(iterable, new ModSplitter(10, 0, 8));
                 Iterable<WikiPage> testingPages = CollectionHelper.filter(iterable, new ModSplitter(10, 9, 9));
                Iterable<LocationDocument> trainingLocations = CollectionHelper.convert(trainingPages, CONVERTER);
                 Iterable<LocationDocument> testingLocations = CollectionHelper.convert(testingPages, CONVERTER);
                 
                 // limit to just 100 for now
                 testingLocations = CollectionHelper.limit(testingLocations, 100);
//                 testingLocations = CollectionHelper.limit(testingLocations, 1000);
//                 testingLocations = CollectionHelper.limit(testingLocations, 10000);
                 
                 
//                 File indexFile = new File("/Users/pk/Desktop/Location_Lab_Revisited/knn-scope-model-wikipedia-90-train-v2");
//                 NearestNeighborScopeModel knnIndex = KNearestNeighborScopeDetector.NearestNeighborScopeModel.fromIndex(indexFile);
//                 ScopeDetectorEvaluator.evaluateScopeDetection(new KNearestNeighborScopeDetector(knnIndex, 5), testingLocations, true );
//                 System.exit(0);
                 
//                 DictionaryScopeModel model = FileHelper.tryDeserialize("/Users/pk/Desktop/Location_Lab_Revisited/enwiki-20220501-locationDictionary-1-word-0.3515625.ser.gz");
//                 DictionaryScopeModel model = FileHelper.tryDeserialize("/Users/pk/Repositories/palladian/palladian-experimental/enwiki-20220501-locationDictionary-1-word-500-frequency-0.3515625-v2-hashed.ser.gz");
                 DictionaryScopeModel model = FileHelper.tryDeserialize("/Users/pk/Desktop/Location_Lab_Revisited/enwiki-20220501-locationDictionary-2-word-0.3515625.ser.gz");
//                 PalladianTextClassifier.PARALLELIZED = false;
//                 ScopeDetectorEvaluator.evaluateScopeDetection(new MultiStepDictionaryScopeDetector(model, 5.625, 2.8125, 1.40625, 0.703125), testingLocations, true);
                  DictionaryScopeDetector scopeDetector = new DictionaryScopeDetector(model);
                  System.out.println(scopeDetector.getScope("St. Petersburg is near Miami."));
                // ScopeDetectorEvaluator.evaluateScopeDetection(scopeDetector, testingLocations, true);
//                 PalladianTextClassifier.PARALLELIZED = true;
//                 ScopeDetectorEvaluator.evaluateScopeDetection(new DictionaryScopeDetector(model, new BayesScorer()), testingLocations, true);
//                 PalladianTextClassifier.PARALLELIZED = false;
//                 ScopeDetectorEvaluator.evaluateScopeDetection(new MultiStepDictionaryScopeDetector(model, new BayesScorer(), 5.625, 2.8125, 1.40625, 0.703125), testingLocations, true);
//                 System.exit(0);

//////                 FeatureSetting setting = FeatureSettingBuilder.chars(6, 9).create();
//                FeatureSetting setting = FeatureSettingBuilder.words(1).maxTerms(500, TermSelector.FREQUENCY).create();
////                DictionaryBuilder builder = new DictionaryTrieModel.Builder();
//                DictionaryBuilder builder = new HashedDictionaryMapModel.Builder();
////                builder.setPruningStrategy(new PruningStrategies.TermCountPruningStrategy(2));
//                DictionaryScopeDetectorLearner learner = new DictionaryScopeDetectorLearner(setting, builder, 0.3515625);
//                DictionaryScopeModel model = learner.train(trainingLocations);
//                FileHelper.trySerialize(model, "enwiki-20220501-locationDictionary-1-word-500-frequency-0.3515625-v2-hashed.ser.gz");
                
//                File indexFile = new File("/Users/pk/temp/knn-scope-model-wikipedia-90-train-v2");
//                FeatureSetting featureSetting = FeatureSettingBuilder.words(1).create();
//                NearestNeighborScopeDetectorLearner learner = new NearestNeighborScopeDetectorLearner(indexFile, featureSetting);
//                learner.train(trainingLocations);
                
                
                // trainingLocations = CollectionHelper.limit(trainingLocations, 20000);
                // testingLocations = CollectionHelper.limit(testingLocations, 10);
                
                // DictionaryScopeModel model = FileHelper.tryDeserialize("/Volumes/iMac HD/temp/wikipediaLocationGridModel_0.1.ser.gz");
                
                // double fineGridSize = 0.1;
                // double coarseGridSize = 2.5;
                // ScopeDetector detector = new SimulatedTwoLevelTextClassifierScopeDetector(model, coarseGridSize);
                // evaluateScopeDetection(detector, testingLocations, true);
                
                // Scorer scorer = new BayesScorer(LAPLACE, PRIORS, FREQUENCIES, COMPLEMENT);
                
                // way too slow with the BayesScorer
                // DictionaryScopeDetector scopeDetector = new DictionaryScopeDetector(model, scorer);
                
                // ScopeDetector scopeDetector = new MultiStepDictionaryScopeDetector(model, scorer, 10.0, 5.0, 2.5, 1.0, 0.5);
                // evaluateScopeDetection(scopeDetector, testingLocations, true);
                
                // MultiStepDictionaryScopeDetector scopeDetector = new MultiStepDictionaryScopeDetector(model, scorer, 2.5);
                // evaluateScopeDetection(scopeDetector, testingLocations, true);
                
                
//                FeatureSetting featureSetting = FeatureSettingBuilder.chars(6, 9).create();
//                DictionaryScopeModel model = DictionaryScopeDetector.train(trainingLocations, featureSetting, fineGridSize);
//                
//                // write the huge model, for later, just in case :)
//                try {
//                    FileHelper.serialize(model, "/Volumes/iMac HD/temp/wikipediaLocationGridModel_0.1.ser.gz");
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        };

    }

}
