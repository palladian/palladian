package ws.palladian.kaggle.restaurants.experiments;

import static ws.palladian.helper.functional.Filters.regex;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;

import ws.palladian.classification.quickml.QuickMlClassifier;
import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.FilteredVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.Model;
import ws.palladian.core.value.Value;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.kaggle.restaurants.dataset.Label;
import ws.palladian.kaggle.restaurants.utils.Config;
import ws.palladian.kaggle.restaurants.utils.DependencyMatrix;
import ws.palladian.kaggle.restaurants.utils.DependencyMatrixBuilder;

/**
 * Use the dependencies between class labels to improve prediction.
 * For each class label, we can set the best classifier/model combination available to us.
 */
public class DependencyResolver {

    /**
     * Label priors on a BUSINESS basis (not photo basis) and dependencies between labels.
     */
    private DependencyMatrix dependencyMatrix;

    private double scoreMin = 0;
    private double scoreStep = 0.1;
    private double scoreMax = 1;
    private double threshold = 0;

    /**
     * Cache for classifier, model pairs.
     */
    private Map<Label, Triple<Classifier, Model, String>> classifierModelCache = new HashMap<>();

    public DependencyResolver(double scoreMin, double scoreStep, double scoreMax) {
        this.scoreMin = scoreMin;
        this.scoreStep = scoreStep;
        this.scoreMax = scoreMax;
        dependencyMatrix = new DependencyMatrixBuilder().buildMatrix();
    }

    /**
     * Try to make sense of the classification probabilities and the label dependencies.
     *
     * @param classifications
     * @return A set of labels that we have decided on.
     */
    public Set<Label> score(Map<Label, CategoryEntries> classifications) {

        Set<Label> labels = new HashSet<>();

        for (Map.Entry<Label, CategoryEntries> labelCategoryEntriesEntry : classifications.entrySet()) {
            Label label = labelCategoryEntriesEntry.getKey();
            CategoryEntries categoryEntries = labelCategoryEntriesEntry.getValue();
            String mostLikelyCategory = categoryEntries.getMostLikelyCategory();
            Double probability = categoryEntries.getProbability(mostLikelyCategory);

            if (mostLikelyCategory.equalsIgnoreCase("true")) {
                labels.add(label);
            }

//            if (mostLikelyCategory.equalsIgnoreCase("false")) {
//                probability = 1 - probability;
//            }
//
//            Double prior = dependencyMatrix.getPrior(labelCategoryEntriesEntry.getKey());
//            Double score = prior * probability;
//            for (Map.Entry<Label, CategoryEntries> categoryEntriesEntry : classifications.entrySet()) {
//                Label otherLabel = categoryEntriesEntry.getKey();
//                if (otherLabel == label) {
//                    continue;
//                }
//                Double otherPrior = dependencyMatrix.getPrior(otherLabel);
//                String otherMostLikelyCategory = categoryEntriesEntry.getValue().getMostLikelyCategory();
//                if (otherMostLikelyCategory.equalsIgnoreCase("true")) {
//                    Double otherProbability = categoryEntriesEntry.getValue().getProbability(otherMostLikelyCategory);
//                    Double dependency = dependencyMatrix.getDependency(otherLabel, label);
//                    score += otherPrior * dependency * otherProbability;
////                    score += dependency * otherProbability;
//                }
//            }
//
//            //score /= Label.values().length;
//            score /= 4;
//
//            if (score > threshold) {
//                labels.add(label);
//            }

            // > .5 only
//            if (mostLikelyCategory.equalsIgnoreCase("true") && probability > 0.5) {
//                labels.add(label);
//            }

//            // > prior only
//            if (mostLikelyCategory.equalsIgnoreCase("true") && probability > dependencyMatrix.getPrior(label))  {
//                labels.add(label);
//            }

//            labels.add(label);
        }

        return labels;
    }

    public void test() throws IOException {

        StringBuilder summary = new StringBuilder();
        summary.append("threshold;overall\n");
        for (threshold = scoreMin; threshold <= scoreMax; threshold += scoreStep) {

            System.out.println("start evaluating with threshold " + threshold);

            CsvDatasetReaderConfig.Builder csvConfigBuilder = CsvDatasetReaderConfig.filePath(new File(Config.CONFIG.getString("dataset.yelp.restaurants.root") + "/yelp_features_small_test.csv"));
            Iterable<Instance> testingInstances = csvConfigBuilder.create();

            // label => TP, FP, FN
            Map<Label, Integer[]> evaluation = new HashMap<>();
            Map<Label, ConfusionMatrix> evaluationConfusionMatrices = new HashMap<>();
            for (Label label : Label.values()) {
                evaluation.put(label, new Integer[]{0, 0, 0});
                evaluationConfusionMatrices.put(label, new ConfusionMatrix());
            }

            //Iterable<Instance> experimentTesting = filterFeaturesIterable(testingInstances, regex("main_color-.*|.*_(max|mean|min|range|stdDev|sum|count|percentile)"));

            // iterate through test data
            ProgressMonitor pm = new ProgressMonitor(2000, .1, "Evaluate");
            for (Instance testInstance : testingInstances) {

                // for each label, keep the classification results for later processing
                Map<Label, CategoryEntries> map = new LinkedHashMap<>();

                Set<Label> correctLabels = new HashSet<>();

                // classify each photo with each label classifier
                for (Label label : Label.values()) {
                    Value category = testInstance.getVector().get(label.toString());
                    if (category == null) {
                        throw new IllegalArgumentException("No feature with name \"" + label.toString() + "\".");
                    }
                    if (category.toString().equals("true")) {
                        correctLabels.add(label);
                    }
                    Instance newTestInstance = new InstanceBuilder().add(testInstance.getVector()).create(category.toString());

                    Triple<Classifier, Model, String> classifierAndModel = getClassifierAndModel(label);
                    Classifier labelClassifier = classifierAndModel.getLeft();
                    Model model = classifierAndModel.getMiddle();
                    String regex = classifierAndModel.getRight();

//                    FeatureVector filteredTestInstance = filterFeatures(newTestInstance.getVector(), regex("main_color-.*|.*_(max|mean|min|range|stdDev|sum|count|percentile)"));
                    FeatureVector filteredTestInstance = new FilteredVector(newTestInstance.getVector(), regex(regex));

                    CategoryEntries categoryEntries = labelClassifier.classify(filteredTestInstance, model);
                    map.put(label, categoryEntries);
                }

                Set<Label> acceptedLabels = score(map);
                for (Label label : Label.values()) {

                    boolean isCorrect = correctLabels.contains(label);
                    boolean isClassified = acceptedLabels.contains(label);

                    evaluationConfusionMatrices.get(label).add(isCorrect + "", isClassified + "");

                    if (isCorrect) {
                        if (isClassified) {
                            // true positive
                            evaluation.get(label)[0]++;
                        } else {
                            // false negative
                            evaluation.get(label)[2]++;
                        }
                    } else {
                        if (isClassified) {
                            // false  positive
                            evaluation.get(label)[1]++;
                        }
                    }
                }

                pm.incrementAndPrintProgress();
            }

            // write results
            StringBuilder results = new StringBuilder();
            double overall = 1.;
            for (Map.Entry<Label, Integer[]> labelEntry : evaluation.entrySet()) {
                results.append("=================== Label: ").append(labelEntry.getKey().toString()).append("===================\n");

                int tp = labelEntry.getValue()[0];
                int fp = labelEntry.getValue()[1];
                int fn = labelEntry.getValue()[2];

                double precision = (double) tp / (fp + tp);
                double recall = (double) tp / (tp + fn);
                double f1 = 2 * precision * recall / (precision + recall);
                Double prior = dependencyMatrix.getPrior(labelEntry.getKey());

                results.append("Precision: ").append(precision).append("\n");
                results.append("Recall: ").append(recall).append("\n");
                results.append("F1: ").append(f1).append("\n");
                results.append("Prior: ").append(prior).append("\n");

                results.append(evaluationConfusionMatrices.get(labelEntry.getKey()).toString());
                results.append("\n");

                overall *= prior * f1;

                results.append("\n");
            }
            StringBuilder finalBuilder = new StringBuilder();
            finalBuilder.append("Overall: ").append(100000 * overall).append("\n\n");
            finalBuilder.append(results);

            System.out.println(results);
//            FileHelper.writeToFile(Config.CONFIG.getString("dataset.yelp.restaurants.results") + "/dependency-results-" + DateHelper.getCurrentDatetime() + ".txt", finalBuilder);
            FileHelper.writeToFile(Config.CONFIG.getString("dataset.yelp.restaurants.results") + "/dependency-results-" + MathHelper.round(threshold,3) + ".txt", finalBuilder);

            summary.append(threshold).append(";").append(overall).append("\n");
        }
        FileHelper.writeToFile(Config.CONFIG.getString("dataset.yelp.restaurants.results") + "/dependency-results-summary-" + DateHelper.getCurrentDatetime() + ".txt", summary);
    }

    private Triple<Classifier, Model, String> getClassifierAndModel(Label label) throws IOException {
        Triple<Classifier, Model, String> combo;

//        combo = classifierModelCache.get(label);
//        if (combo == null) {
//            Model model = FileHelper.deserialize(Config.CONFIG.getString("dataset.yelp.restaurants.root") + "/models/" + label.toString() + ".gz");
////            combo = Pair.with(new QuickMlClassifier(), model);
//            combo = Pair.with(new NaiveBayesClassifier(), model);
////            combo = Pair.with(new KnnClassifier(), model);
//        }
//
//        classifierModelCache.put(label, combo);

        String regex = ".*";

        combo = classifierModelCache.get(label);
        if (combo == null) {
            Model model = FileHelper.deserialize(Config.CONFIG.getString("dataset.yelp.restaurants.root") + "/models/" + label.toString() + ".gz");

            // any other classifier ever better than random forest?
            Classifier classifier = new QuickMlClassifier();
            switch (label) {
                case GOOD_FOR_LUNCH:
                    classifier = new QuickMlClassifier();
                    regex = ".*_(max|mean|min|range|stdDev|sum|count|percentile)";
                    break;
                case GOOD_FOR_DINNER:
                    classifier = new QuickMlClassifier();
                    regex = "(width|height|ratio || main_color.* || .*_(max|mean|min|range|stdDev|sum|count|percentile) || symmetry-.* || .*_region.* || frequency-.* || 4x4-similarity_.*)";
                    break;
                case TAKES_RESERVATIONS:
                    classifier = new QuickMlClassifier();
                    regex = "(width|height|ratio || main_color.* || .*_(max|mean|min|range|stdDev|sum|count|percentile) || symmetry-.* || .*_region.* || frequency-.* || 4x4-similarity_.*)";
                    break;
                case OUTDOOR_SEATING:
                    classifier = new QuickMlClassifier();
                    regex = ".*_(max|mean|min|range|stdDev|sum|count|percentile)";
                    break;
                case RESTAURANT_IS_EXPENSIVE:
                    classifier = new QuickMlClassifier();
                    regex = "(SURF.* || SIFT.* || (width|height|ratio || main_color.* || .*_(max|mean|min|range|stdDev|sum|count|percentile) || symmetry-.* || .*_region.* || frequency-.* || 4x4-similarity_.*))";
                    break;
                case HAS_ALCOHOL:
                    classifier = new QuickMlClassifier();
                    regex = "SIFT.*";
                    break;
                case HAS_TABLE_SERVICE:
                    classifier = new QuickMlClassifier();
                    regex = "(SURF.* || SIFT.* || (width|height|ratio || main_color.* || .*_(max|mean|min|range|stdDev|sum|count|percentile) || symmetry-.* || .*_region.* || frequency-.* || 4x4-similarity_.*))";
                    break;
                case AMBIENCE_IS_CLASSY:
                    classifier = new QuickMlClassifier();
                    regex = "(SURF.* || SIFT.* || (width|height|ratio || main_color.* || .*_(max|mean|min|range|stdDev|sum|count|percentile) || symmetry-.* || .*_region.* || frequency-.* || 4x4-similarity_.*))";
                    break;
                case GOOD_FOR_KIDS:
                    classifier = new QuickMlClassifier();
                    regex = "(SURF.* || SIFT.* || (width|height|ratio || main_color.* || .*_(max|mean|min|range|stdDev|sum|count|percentile) || symmetry-.* || .*_region.* || frequency-.* || 4x4-similarity_.*))";
                    break;
            }
            combo = Triple.of(classifier, model, regex);
        }

        classifierModelCache.put(label, combo);

        return combo;
    }

    public static void main(String[] args) throws IOException {
//        new DependencyResolver(0, 0.001, 0.15).test();
        new DependencyResolver(0, 1, 0).test();
    }

}
