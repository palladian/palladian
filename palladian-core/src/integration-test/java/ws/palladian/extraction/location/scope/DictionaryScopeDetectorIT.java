package ws.palladian.extraction.location.scope;

import java.io.File;

import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.PalladianTextClassifier.Scorer;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.extraction.location.scope.DictionaryScopeDetector.DictionaryScopeDetectorLearner;
import ws.palladian.extraction.location.scope.DictionaryScopeDetector.DictionaryScopeModel;
import ws.palladian.extraction.location.scope.evaluation.ScopeDetectorEvaluator;
import ws.palladian.extraction.location.scope.evaluation.WikipediaLocationScopeIterator;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.math.Stats;
import ws.palladian.integrationtests.ITHelper;

public class DictionaryScopeDetectorIT {

    private static final Scorer scorer = new PalladianTextClassifier.DefaultScorer();

    private static final double fineGridSize = 0.7;

    private static final double coarseGridSize = 5.63;

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
        ITHelper.assertMemory(750, SizeUnit.MEGABYTES);
    }

    @After
    public void cleanup() {
        ITHelper.forceGc();
    }

    @Test
    public void testDictionaryScopeDetector_wikipedia40k() {
        FeatureSetting setting = FeatureSettingBuilder.chars(6, 9).create();
        DictionaryScopeDetectorLearner learner = new DictionaryScopeDetectorLearner(setting, coarseGridSize);
        DictionaryScopeModel model = learner.train(trainingSet);
        ScopeDetector scopeDetector = new DictionaryScopeDetector(model, scorer);
        Stats stats = ScopeDetectorEvaluator.evaluateScopeDetection(scopeDetector, validationSet, false);
        // ScopeDetectorEvaluator.printStats(stats);
        ITHelper.assertMax("meanError", 1027, stats.getMean()); // 1026.048326852913
        ITHelper.assertMax("medianError", 199, stats.getMedian()); // 198.47593580609546
        ITHelper.assertMin("probability1km", 0, stats.getCumulativeProbability(1)); // 7.394626571358146E-4
        ITHelper.assertMin("probability10km", 0.02, stats.getCumulativeProbability(10)); // 0.021074685728370717
        ITHelper.assertMin("probability100km", 0.23, stats.getCumulativeProbability(100)); // 0.232684249445403
        ITHelper.assertMin("probability1000km", 0.86, stats.getCumulativeProbability(1000)); // 0.8682524032536357
    }

    @Test
    public void testTwoStepScopeDetector_wikipedia40k() {
        FeatureSetting setting = FeatureSettingBuilder.chars(6, 9).create();
        DictionaryScopeDetectorLearner learner = new DictionaryScopeDetectorLearner(setting, fineGridSize);
        DictionaryScopeModel model = learner.train(trainingSet);
        ScopeDetector scopeDetector = new MultiStepDictionaryScopeDetector(model, scorer, coarseGridSize);
        Stats stats = ScopeDetectorEvaluator.evaluateScopeDetection(scopeDetector, validationSet, false);
        // ScopeDetectorEvaluator.printStats(stats);
        ITHelper.assertMax("meanError", 930, stats.getMean()); // 929.5262702219871
        ITHelper.assertMax("medianError", 57, stats.getMedian()); // 56.23283449739908
        ITHelper.assertMin("probability1km", 0, stats.getCumulativeProbability(1)); // 0.009120039438008381
        ITHelper.assertMin("probability10km", 0.13, stats.getCumulativeProbability(10)); // 0.13273354695587872
        ITHelper.assertMin("probability100km", 0.60, stats.getCumulativeProbability(100)); // 0.6006901651466601
        ITHelper.assertMin("probability1000km", 0.87, stats.getCumulativeProbability(1000)); // 0.8715799852107469

    }

}
