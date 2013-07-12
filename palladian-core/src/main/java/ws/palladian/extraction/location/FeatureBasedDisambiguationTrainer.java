package ws.palladian.extraction.location;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import ws.palladian.classification.dt.BaggedDecisionTreeClassifier;
import ws.palladian.classification.dt.BaggedDecisionTreeModel;
import ws.palladian.classification.featureselection.AverageMergingStrategy;
import ws.palladian.classification.featureselection.BackwardFeatureElimination;
import ws.palladian.classification.featureselection.ChiSquaredFeatureRanker;
import ws.palladian.classification.featureselection.FeatureRanker;
import ws.palladian.classification.featureselection.FeatureRanking;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.extraction.location.LocationExtractorUtils.LocationDocument;
import ws.palladian.helper.collection.Function;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.processing.Trainable;

public class FeatureBasedDisambiguationTrainer {

    public static void main(String[] args) {
        File datasetDirectory = new File("/Users/pk/Desktop/TUD-Loc-2013/TUD-Loc-2013_V2/1-training");
        Iterator<LocationDocument> iterator = LocationExtractorUtils.iterateDataset(datasetDirectory);
        while (iterator.hasNext()) {
            LocationDocument document = iterator.next();
            System.out.println(document.getAnnotations());
        }
        System.exit(0);
        // performBackwardElimination();
        // System.exit(0);
        // String csvFilePath = "data/temp/location_disambiguation_1373234035433.csv";
        // List<Trainable> dataset = ClassificationUtils.readCsv(csvFilePath, true);
        // dataset = ClassificationUtils.filterFeatures(dataset, InverseFilter.create(new RegexFilter("marker=.*")));
        // performFeatureSelection(dataset);
        // performBackwardElimination(dataset);
        // System.exit(0);
    }
    static void performFeatureSelection(List<Trainable> dataset) {
        // FeatureRanker ranker = new InformationGainFeatureRanker();
        FeatureRanker ranker = new ChiSquaredFeatureRanker(new AverageMergingStrategy());
        FeatureRanking featureRanking = ranker.rankFeatures(dataset);
        System.out.println(featureRanking);
    }
    
    static void performBackwardElimination() {
        String trainFilePath = "data/temp/ld_features_training_1373470997471.csv";
        String validationFilePath = "data/temp/ld_features_validation_1373479728807.csv";
        List<Trainable> trainSet = ClassificationUtils.readCsv(trainFilePath, true);
        List<Trainable> validationSet = ClassificationUtils.readCsv(validationFilePath, true);

        BaggedDecisionTreeClassifier classifier = new BaggedDecisionTreeClassifier();
        Function<ConfusionMatrix, Double> scorer = new Function<ConfusionMatrix, Double>() {
            @Override
            public Double compute(ConfusionMatrix input) {
                return input.getF(1.0, "true");
            }
        };

        BackwardFeatureElimination<BaggedDecisionTreeModel> elimination = new BackwardFeatureElimination<BaggedDecisionTreeModel>(
                classifier, classifier, scorer);
        FeatureRanking featureRanking = elimination.rankFeatures(trainSet, validationSet);
        System.out.println(featureRanking);
    }

}
