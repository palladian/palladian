package ws.palladian.extraction.location.scope;

import java.io.File;

import org.apache.commons.configuration.Configuration;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.extraction.location.scope.KNearestNeighborScopeDetector.NearestNeighborScopeDetectorLearner;
import ws.palladian.extraction.location.scope.KNearestNeighborScopeDetector.NearestNeighborScopeModel;
import ws.palladian.extraction.location.scope.evaluation.ScopeDetectorEvaluator;
import ws.palladian.extraction.location.scope.evaluation.WikipediaLocationScopeIterator;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.math.Stats;
import ws.palladian.integrationtests.ITHelper;

public class KNearestNeighborScopeDetectorIT {

    private static Iterable<LocationDocument> trainingSet;

    private static Iterable<LocationDocument> validationSet;

    @BeforeClass
    public static void readConfiguration() {
        Configuration config = ITHelper.getTestConfig();
        String split1Path = config.getString("dataset.wikipediaScope40k.split1");
        String split2Path = config.getString("dataset.wikipediaScope40k.split2");
        ITHelper.assertDirectory(split1Path, split2Path);
        trainingSet = new WikipediaLocationScopeIterator(new File(split1Path));
        validationSet = new WikipediaLocationScopeIterator(new File(split2Path));
        ITHelper.assertMemory(500, SizeUnit.MEGABYTES);
    }

    @After
    public void cleanup() {
        ITHelper.forceGc();
    }

    @Test
    public void testDictionaryScopeDetector_wikipedia40k() {
        FeatureSetting setting = FeatureSettingBuilder.words(1).create();
        int k = 1;
        Directory directory = new RAMDirectory();
        NearestNeighborScopeDetectorLearner learner = new NearestNeighborScopeDetectorLearner(directory, setting);
        NearestNeighborScopeModel model = learner.train(trainingSet);
        ScopeDetector scopeDetector = new KNearestNeighborScopeDetector(model, k);
        Stats stats = ScopeDetectorEvaluator.evaluateScopeDetection(scopeDetector, validationSet, false);
        // ScopeDetectorEvaluator.printStats(stats);
        ITHelper.assertMax("meanError", 1236, stats.getMean()); // 1235.9197784432267
        ITHelper.assertMax("medianError", 102, stats.getMedian()); // 101.01536106093519
        ITHelper.assertMin("probability1km", 0.01, stats.getCumulativeProbability(1)); // 0.019842247966477693
        ITHelper.assertMin("probability10km", 0.16, stats.getCumulativeProbability(10)); // 0.160956371703229
        ITHelper.assertMin("probability100km", 0.49, stats.getCumulativeProbability(100)); // 0.49864431846191765
        ITHelper.assertMin("probability1000km", 0.79, stats.getCumulativeProbability(1000)); // 0.7936899186591078
    }

}
