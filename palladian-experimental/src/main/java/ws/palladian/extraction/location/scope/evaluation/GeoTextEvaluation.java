package ws.palladian.extraction.location.scope.evaluation;

import static ws.palladian.extraction.location.scope.evaluation.ScopeDetectorEvaluator.evaluateScopeDetection;

import java.io.File;

import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.PalladianTextClassifier.Scorer;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.extraction.location.scope.DictionaryScopeDetector.DictionaryScopeDetectorLearner;
import ws.palladian.extraction.location.scope.DictionaryScopeDetector.DictionaryScopeModel;
import ws.palladian.extraction.location.scope.MultiStepDictionaryScopeDetector;
import ws.palladian.extraction.location.scope.ScopeDetector;
import ws.palladian.extraction.location.scope.evaluation.GeoTextDatasetReader.Combination;
import ws.palladian.extraction.location.scope.evaluation.GeoTextDatasetReader.SubSet;

@SuppressWarnings("unused")
public class GeoTextEvaluation {

    public static void main(String[] args) {

        String file = "/Users/pk/Desktop/GeoText.2010-10-12/full_text.txt";
        Iterable<LocationDocument> train = new GeoTextDatasetReader(new File(file), SubSet.TRAIN, Combination.USER);
        Iterable<LocationDocument> dev = new GeoTextDatasetReader(new File(file), SubSet.DEV, Combination.USER);

        double defaultGrid = 0.1;
        Scorer defaultScorer = new PalladianTextClassifier.DefaultScorer();

//        Set<FeatureSetting> featureSettings = new FeatureSettingGenerator().words(1, 5).chars(1, 10).create();
//        for (FeatureSetting setting : featureSettings) {
//            Directory directory = new RAMDirectory();
//            NearestNeighborScopeModel model = new NearestNeighborScopeDetectorLearner(directory, setting).train(train);
//            evaluateScopeDetection(new KNearestNeighborScopeDetector(model, 1, MORE_LIKE_THIS_QUERY_CREATOR), dev, false);
//        }
//
//        for (FeatureSetting setting : featureSettings) {
//            DictionaryScopeModel model = new DictionaryScopeDetectorLearner(setting, defaultGrid).train(train);
//            evaluateScopeDetection(new DictionaryScopeDetector(model, defaultScorer), dev, false);
//        }
        
        FeatureSetting featureSetting = FeatureSettingBuilder.words(1).create();
//        DictionaryScopeModel model = new DictionaryScopeDetectorLearner(featureSetting, defaultGrid).train(train);
//        evaluateScopeDetection(new DictionaryScopeDetector(model, new PalladianTextClassifier.DefaultScorer()), dev, false);
//        evaluateScopeDetection(new DictionaryScopeDetector(model, new BayesScorer(LAPLACE)), dev, false);
//        evaluateScopeDetection(new DictionaryScopeDetector(model, new BayesScorer(PRIORS)), dev, false);
//        evaluateScopeDetection(new DictionaryScopeDetector(model, new BayesScorer(FREQUENCIES)), dev, false);
//        evaluateScopeDetection(new DictionaryScopeDetector(model, new BayesScorer(COMPLEMENT)), dev, false);
//        evaluateScopeDetection(new DictionaryScopeDetector(model, new BayesScorer(LAPLACE, PRIORS)), dev, false);
//        evaluateScopeDetection(new DictionaryScopeDetector(model, new BayesScorer(LAPLACE, FREQUENCIES)), dev, false);
//        evaluateScopeDetection(new DictionaryScopeDetector(model, new BayesScorer(LAPLACE, COMPLEMENT)), dev, false);
//        evaluateScopeDetection(new DictionaryScopeDetector(model, new BayesScorer(PRIORS, FREQUENCIES)), dev, false);
//        evaluateScopeDetection(new DictionaryScopeDetector(model, new BayesScorer(PRIORS, COMPLEMENT)), dev, false);
//        evaluateScopeDetection(new DictionaryScopeDetector(model, new BayesScorer(FREQUENCIES, COMPLEMENT)), dev, false);
//        evaluateScopeDetection(new DictionaryScopeDetector(model, new BayesScorer(LAPLACE, PRIORS, FREQUENCIES)), dev, false);
//        evaluateScopeDetection(new DictionaryScopeDetector(model, new BayesScorer(LAPLACE, PRIORS, COMPLEMENT)), dev, false);
//        evaluateScopeDetection(new DictionaryScopeDetector(model, new BayesScorer(PRIORS, FREQUENCIES, COMPLEMENT)), dev, false);
//        evaluateScopeDetection(new DictionaryScopeDetector(model, new BayesScorer(LAPLACE, PRIORS, FREQUENCIES, COMPLEMENT)), dev, false);
        
//         BayesScorer scorer = new BayesScorer(LAPLACE, COMPLEMENT);
//         // ScopeDetector detector = new MultiStepDictionaryScopeDetector(model, scorer, 6.4, 3.2, 1.6, .8, .4, .2);
//         // ScopeDetector detector = new MultiStepDictionaryScopeDetector(model, scorer, 6.4, 1.6, .4);
//         ScopeDetector detector = new MultiStepDictionaryScopeDetector(model, scorer, 6.4, .8);
//         evaluateScopeDetection(detector, dev, false);
//         detector = new MultiStepDictionaryScopeDetector(model, scorer, 3.2);
//         evaluateScopeDetection(detector, dev, false);
//         detector = new MultiStepDictionaryScopeDetector(model, scorer, 1.6);
//         evaluateScopeDetection(detector, dev, false);
        
//        double[] gridSizes = TextBasedScopeDetectorEvaluation.createEvaluationGridSize();
//        for (double fineGrid : gridSizes) {
//            DictionaryScopeModel model = new DictionaryScopeDetectorLearner(featureSetting, fineGrid).train(train);
//            for (double coarseGrid : gridSizes) {
//                if (coarseGrid > fineGrid) {
//                    ScopeDetector detector = new MultiStepDictionaryScopeDetector(model, defaultScorer, coarseGrid);
//                    evaluateScopeDetection(detector, dev, false);
//                }
//            }
//        }

    }

}
