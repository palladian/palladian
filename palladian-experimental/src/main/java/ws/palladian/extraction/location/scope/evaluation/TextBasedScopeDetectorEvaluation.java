package ws.palladian.extraction.location.scope.evaluation;

import static ws.palladian.extraction.location.scope.evaluation.ScopeDetectorEvaluator.evaluateScopeDetection;

import java.io.File;

import ws.palladian.classification.text.BayesScorer;
import ws.palladian.classification.text.DictionaryTrieModel;
import ws.palladian.classification.text.ExperimentalScorers;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.extraction.location.scope.DictionaryScopeDetector;
import ws.palladian.extraction.location.scope.DictionaryScopeDetector.DictionaryScopeModel;
import ws.palladian.extraction.location.scope.MultiStepDictionaryScopeDetector;
import ws.palladian.extraction.location.scope.ScopeDetector;
import ws.palladian.helper.io.FileHelper;

/**
 * Evaluation script for text-based scope detectors; runs different feature settings to determine the optimal config.
 * 
 * @author Philipp Katz
 */
@SuppressWarnings("unused")
public final class TextBasedScopeDetectorEvaluation {

    private TextBasedScopeDetectorEvaluation() {
        // utility
    }
    
    public static void main(String[] args) throws Exception {
        File trainingDirectory = new File("/Users/pk/Desktop/Location_Lab_Revisited/WikipediaScopeDataset-2014/split-1");
        File validationDirectory = new File("/Users/pk/Desktop/Location_Lab_Revisited/WikipediaScopeDataset-2014/split-2");
        File testDirectory = new File("/Users/pk/Desktop/Location_Lab_Revisited/WikipediaScopeDataset-2014/split-3");
        Iterable<LocationDocument> trainingSet = new WikipediaLocationScopeIterator(trainingDirectory, true);
        Iterable<LocationDocument> validationSet = new WikipediaLocationScopeIterator(validationDirectory);
        Iterable<LocationDocument> testSet = new WikipediaLocationScopeIterator(testDirectory);

        // FeatureSetting setting = FeatureSettingBuilder.words(1).create();
        // NearestNeighborScopeModel model = new NearestNeighborScopeDetectorLearner(new RAMDirectory(), setting).train(trainingSet);
        // ScopeDetector detector = new KNearestNeighborScopeDetector(model, 5, MORE_LIKE_THIS_QUERY_CREATOR);
        // evaluateScopeDetection(detector, testSet, false);
        // System.exit(0);

//        FeatureSetting setting = FeatureSettingBuilder.chars(6, 9).maxTerms(1500).create();
//        FeatureSetting setting = FeatureSettingBuilder.words(1).maxTerms(1500).create(); // 5.7 mb
//        FeatureSetting setting = FeatureSettingBuilder.words(1).maxTerms(5000).create(); // dto.
        FeatureSetting setting = FeatureSettingBuilder.words(1).maxTerms(Integer.MAX_VALUE).create();
//        DictionaryScopeModel model = new DictionaryScopeDetector.DictionaryScopeDetectorLearner(setting, 0.703125).train(trainingSet);
        DictionaryScopeModel model = new DictionaryScopeDetector.DictionaryScopeDetectorLearner(setting, new DictionaryTrieModel.Builder(), 0.703125).train(trainingSet);
//         DictionaryScopeModel model = new DictionaryScopeDetector.DictionaryScopeDetectorLearner(setting, new DictionaryMapModel.Builder(), 0.703125).train(trainingSet);
        FileHelper.trySerialize(model, "evaluationModel-0.703125-6-9-char-ngrams.ser.gz");
        // DictionaryScopeModel model = FileHelper.deserialize("evaluationModel-0.703125.ser.gz");

         // ScopeDetector detector = new DictionaryScopeDetector(model, new ExperimentalScorers.LogCountScorer());
         // evaluateScopeDetection(detector, testSet, false);
         
//         ScopeDetector detector2 = new MultiStepDictionaryScopeDetector(model, new ExperimentalScorers.LogCountScorer(), 11.25, 5.625, 2.8125, 1.40625);
//         evaluateScopeDetection(detector2, testSet, false);
//
//         ScopeDetector detector3 = new MultiStepDictionaryScopeDetector(model, PalladianTextClassifier.DEFAULT_SCORER, 11.25, 5.625, 2.8125, 1.40625);
//         evaluateScopeDetection(detector3, testSet, false);

//         ScopeDetector detector4 = new MultiStepDictionaryScopeDetector(model, new BayesScorer(), 11.25, 5.625, 2.8125, 1.40625);
//         evaluateScopeDetection(detector4, testSet, false);

//         ScopeDetector detector5 = new MultiStepDictionaryScopeDetector(model, new BayesScorer(), 11.25, 2.8125);
//         evaluateScopeDetection(detector5, testSet, false);

        ScopeDetector detector6 = new MultiStepDictionaryScopeDetector(model, new BayesScorer(), 2.8125);
         evaluateScopeDetection(detector6, testSet, false);

         ScopeDetector detector7 = new MultiStepDictionaryScopeDetector(model, new BayesScorer(), 11.25);
         evaluateScopeDetection(detector7, testSet, false);

        // System.exit(0);

//        final double[] gridSizes = createEvaluationGridSize();
//        final double defaultGridSize = 5.0;
//        final Scorer scorer = new PalladianTextClassifier.DefaultScorer();
//        
//        // DictionaryScopeDetector ///////////////////////////////////////////////////////////////////////////
//        
//        // determine feature setting
//        Set<FeatureSetting> featureSettings = new FeatureSettingGenerator().words(1, 5).chars(1, 10).create();
//        for (FeatureSetting featureSetting : featureSettings) {
//            DictionaryScopeDetectorLearner learner = new DictionaryScopeDetectorLearner(featureSetting, defaultGridSize);
//            DictionaryScopeModel model = learner.train(trainingSet);
//            evaluateScopeDetection(new DictionaryScopeDetector(model), validationSet, true);
//        }

        // determine grid size, using the best feature setting from above
//        FeatureSetting featureSetting = FeatureSettingBuilder.chars(6, 9).create();
//        for (double gridSize : gridSizes) {
//            DictionaryScopeModel model = DictionaryScopeDetector.train(trainingSet, featureSetting, gridSize);
//            evaluateScopeDetection(new DictionaryScopeDetector(model, scorer), validationSet, true);
//        }

        // create learning curves, using the best feature setting + default grid size; 
        // perform ten runs with shuffled training data
//        for (int r = 0; r < 10; r++) {
//            
//            // reload shuffled training set for current fold
//            trainingSet = new WikipediaLocationScopeIterator(trainingDirectory, true);
//            
//            for (int i = 0; i <= 14; i++) {
//                int trainingSize = (int)Math.pow(2, i);
//                FileHelper.appendFile(RESULT_CSV_FILE.getPath(), "### training documents " + trainingSize + "\n");
//                Iterable<LocationDocument> limitedTrain = CollectionHelper.limit(trainingSet, trainingSize);
//                DictionaryScopeModel model = DictionaryScopeDetector.train(limitedTrain, featureSetting, defaultGridSize);
//                evaluateScopeDetection(new DictionaryScopeDetector(model, scorer), validationSet, false);
//            }
//        }

        // TwoLevelTextClassifierScopeDetector //////////////////////////////////////////////////////////////////

//        // determine optimal coarse/fine grid combination
//        FeatureSetting featureSetting = FeatureSettingBuilder.chars(6, 9).create();
//        for (double fineGrid : gridSizes) {
//            DictionaryScopeModel model = DictionaryScopeDetector.train(trainingSet, featureSetting, fineGrid);
//            for (double coarseGrid : gridSizes) {
//                if (coarseGrid > fineGrid) {
//                    ScopeDetector detector = new MultiStepDictionaryScopeDetector(model, scorer, coarseGrid);
//                    evaluateScopeDetection(detector, validationSet, false);
//                }
//            }
//        }
        
        // create learning curves, using the best feature setting + default grid size
//        double coarseGridSize = 2.5;
//        double fineGridSize = 0.5;
//        for (int trainSize = 500; trainSize <= 16000; trainSize += 500) {
//            FileHelper.appendFile(RESULT_CSV_FILE.getPath(), "### training documents " + trainSize + "\n");
//            Iterable<LocationDocument> limitedTrain = CollectionHelper.limit(trainingSet, trainSize);
//            TwoLevelTextClassifierScopeModel model = TwoLevelTextClassifierScopeDetector.train(limitedTrain, featureSetting, coarseGridSize, fineGridSize);
//            evaluateScopeDetection(new TwoLevelTextClassifierScopeDetector(model), validationSet, true);
//        }

        // KNearestNeighborScopeDetector /////////////////////////////////////////////////////////////////////////
        
        // determine feature setting
//        Set<FeatureSetting> featureSettings = new FeatureSettingOptimizer.FeatureSettingGenerator().words(1, 5)/*.chars(1, 10)*/.create();
//        QueryCreator queryCreator = new KNearestNeighborScopeDetector.MoreLikeThisQueryCreator();
//        int k = 1;
//        for (FeatureSetting featureSetting : featureSettings) {
//            NearestNeighborScopeModel model = KNearestNeighborScopeDetector.train(trainingSet, featureSetting);
//            evaluateScopeDetection(new KNearestNeighborScopeDetector(model, k, queryCreator), validationSet, true);
//        }
        
        // determine optimal k, using the best feature setting from above
//        int[] ks = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 25, 50, 100};
//        FeatureSetting featureSetting = FeatureSettingBuilder.words(1).create();
//        QueryCreator queryCreator = new KNearestNeighborScopeDetector.MoreLikeThisQueryCreator();
//        for (int k : ks) {
//            NearestNeighborScopeModel model = KNearestNeighborScopeDetector.train(trainingSet, featureSetting);
//            evaluateScopeDetection(new KNearestNeighborScopeDetector(model, k, queryCreator), validationSet, true);
//        }
        
        // create learning curves, using the best feature setting and optimal k
//        FeatureSetting featureSetting = FeatureSettingBuilder.words(1).create();
//        QueryCreator queryCreator = KNearestNeighborScopeDetector.MORE_LIKE_THIS_QUERY_CREATOR;
//        int k = 5;
//        for (int i = 0; i <= 14; i++) {
//            int trainingSize = (int)Math.pow(2, i);
//            FileHelper.appendFile(RESULT_CSV_FILE.getPath(), "### training documents " + trainingSize + "\n");
//            Iterable<LocationDocument> limitedTrain = CollectionHelper.limit(trainingSet, trainingSize);
//            NearestNeighborScopeModel model = KNearestNeighborScopeDetector.train(limitedTrain, featureSetting);
//            evaluateScopeDetection(new KNearestNeighborScopeDetector(model, k, queryCreator), validationSet, false);
//        }

    }

    public static double[] createEvaluationGridSize() {
        final double[] gridSizes = new double[15];
        for (int i = 0; i < gridSizes.length; i++) {
            gridSizes[i] = (double)180 / (Math.pow(2, i));
        }
        return gridSizes;
    }

}
