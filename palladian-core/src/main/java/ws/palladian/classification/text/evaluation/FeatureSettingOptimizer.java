package ws.palladian.classification.text.evaluation;

import static ws.palladian.classification.text.BayesScorer.Options.COMPLEMENT;
import static ws.palladian.classification.text.BayesScorer.Options.FREQUENCIES;
import static ws.palladian.classification.text.BayesScorer.Options.LAPLACE;
import static ws.palladian.classification.text.BayesScorer.Options.PRIORS;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.classification.text.BayesScorer;
import ws.palladian.classification.text.BayesScorer.Options;
import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.PalladianTextClassifier.Scorer;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Instance;
import ws.palladian.helper.NoProgress;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Filters;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;

/**
 * <p>
 * Simple, brute force optimizer for {@link FeatureSetting}s. This mechanism determines the best feature settings in
 * combination with the best classification threshold (this means, that it currently only works for binary data).
 * 
 * @author pk
 * 
 */
public final class FeatureSettingOptimizer {

    /**
     * Factory for different feature setting combinations.
     * 
     * @author pk
     */
    public static final class FeatureSettingGenerator implements Factory<Set<FeatureSetting>> {

        private int minCharLength;
        private int maxCharLength;
        private int minWordLength;
        private int maxWordLength;
        private boolean withCombinations = true;

        /**
         * Evaluate character features in the given range [min,max].
         * 
         * @param min The minimum character n-gram length.
         * @param max The maximum character n-gram length.
         * @return The instance, builder pattern.
         */
        public FeatureSettingGenerator chars(int min, int max) {
            Validate.isTrue(min > 0, "min must be greater zero");
            Validate.isTrue(max >= min, "max must be greater/equal min");
            this.minCharLength = min;
            this.maxCharLength = max;
            return this;
        }

        /**
         * Evaluate word features in the given range [min,max].
         * 
         * @param min The minimum word n-gram length.
         * @param max The maximum word n-gram length.
         * @return The instance, builder pattern.
         */
        public FeatureSettingGenerator words(int min, int max) {
            Validate.isTrue(min > 0, "min must be greater zero");
            Validate.isTrue(max >= min, "max must be greater/equal min");
            this.minWordLength = min;
            this.maxWordLength = max;
            return this;
        }

        /**
         * Indicate, that no combinations (e.g. [2,3]-grams) should be generated.
         * 
         * @return The instance, builder pattern.
         */
        public FeatureSettingGenerator noCombinations() {
            this.withCombinations = false;
            return this;
        }

        @Override
        public Set<FeatureSetting> create() {
            Set<FeatureSetting> settings = CollectionHelper.newLinkedHashSet();
            if (minCharLength > 0) {
                for (int min = minCharLength; min <= maxCharLength; min++) {
                    for (int max = min; max <= maxCharLength; max++) {
                        if (min == max || withCombinations) {
                            settings.add(FeatureSettingBuilder.chars(min, max).create());
                        }
                    }
                }
            }
            if (minWordLength > 0) {
                for (int min = minWordLength; min <= maxWordLength; min++) {
                    for (int max = min; max <= maxWordLength; max++) {
                        if (min == max || withCombinations) {
                            settings.add(FeatureSettingBuilder.words(min, max).create());
                        }
                    }
                }
            }
            return settings;
        }

    }

    /**
     * Evaluate {@link FeatureSetting}s and/or PruningStrategies and/or {@link Scorer}s.
     * 
     * @param featureSettings The feature settings to evaluate, not <code>null</code> or empty.
     * @param pruningStrategies The pruning strategies to evalutate, <code>null</code> means no pruning.
     * @param scorers The scorers to evaluate, not <code>null</code> or empty.
     * @param training The training data, not <code>null</code>.
     * @param validation The validation data, not <code>null</code>.
     * @param progressReporter For reporting progress, or <code>null</code> when no progress should be reported.
     */
    public static void evaluateFeatureSettings( //
            Set<FeatureSetting> featureSettings, //
            Collection<? extends Filter<? super CategoryEntries>> pruningStrategies, //
            Collection<? extends Scorer> scorers, //
            Iterable<? extends Instance> training, //
            Iterable<? extends Instance> validation, //
            ProgressReporter progressReporter) {
        Validate.notEmpty(featureSettings, "featureSettings must not be null");
        Validate.notNull(training, "training must not be null");
        Validate.notNull(validation, "validation must not be null");
        Validate.notEmpty(scorers, "scorers must not be empty");
        if (pruningStrategies == null || pruningStrategies.isEmpty()) {
            pruningStrategies = Collections.singleton(Filters.ALL);
        }
        if (progressReporter == null) {
            progressReporter = NoProgress.INSTANCE;
        }
        File resultCsv = new File("TextClassifierFeatureOptimization_" + System.currentTimeMillis() + ".csv");
        boolean headerWritten = false;
        progressReporter.startTask("Evaluating feature settings", featureSettings.size() * scorers.size());
        for (FeatureSetting featureSetting : featureSettings) {
            DictionaryModel model = new PalladianTextClassifier(featureSetting).train(training);
            for (Filter<? super CategoryEntries> pruningStrategy : pruningStrategies) {
                model = new PruningSimulatedDictionaryModel(model, pruningStrategy);
                for (Scorer scorer : scorers) {
                    PalladianTextClassifier textClassifier = new PalladianTextClassifier(featureSetting, scorer);
                    ConfusionMatrix confusionMatrix = ClassifierEvaluation.evaluate(textClassifier, validation, model);
                    // XXX code below should be moved to ConfusionMatrix#toCsv or something alike
                    TreeSet<String> categoryNames = new TreeSet<String>(confusionMatrix.getCategories());
                    if (!headerWritten) {
                        StringBuilder header = new StringBuilder("featureSetting;scorer;avgPr;avgRc;avgF1;accuracy;");
                        for (String categoryName : categoryNames) {
                            header.append("pr-").append(categoryName).append(";");
                            header.append("rc-").append(categoryName).append(";");
                            header.append("f1-").append(categoryName).append(";");
                        }
                        header.append("modelSize\n");
                        FileHelper.appendFile(resultCsv.getPath(), header);
                        headerWritten = true;
                    }
                    StringBuilder resultLine = new StringBuilder();
                    resultLine.append(featureSetting).append(';');
                    resultLine.append(scorer).append(';');
                    resultLine.append(confusionMatrix.getAveragePrecision(true)).append(';');
                    resultLine.append(confusionMatrix.getAverageRecall(true)).append(';');
                    resultLine.append(confusionMatrix.getAverageF(1, true)).append(';');
                    resultLine.append(confusionMatrix.getAverageAccuracy(true)).append(';');
                    // precision, recall, f1 for each individual classes
                    for (String categoryName : categoryNames) {
                        double pr = confusionMatrix.getPrecision(categoryName);
                        double f1 = confusionMatrix.getF(1, categoryName);
                        resultLine.append(Double.isNaN(pr) ? StringUtils.EMPTY : pr).append(';');
                        resultLine.append(confusionMatrix.getRecall(categoryName)).append(';');
                        resultLine.append(Double.isNaN(f1) ? StringUtils.EMPTY : f1).append(';');
                    }
                    resultLine.append(model.getNumUniqTerms()).append('\n');
                    FileHelper.appendFile(resultCsv.getPath(), resultLine);
                    progressReporter.increment();
                }
            }
        }
        progressReporter.finishTask();
    }

    private FeatureSettingOptimizer() {
        // not for instantiation.
    }

    public static void main(String[] args) {
        Set<FeatureSetting> featureSettings = new FeatureSettingGenerator().chars(3, 8).words(1, 3).create();
        CollectionHelper.print(featureSettings);
        System.exit(0);
        // String datasetFile = "/Users/pk/Desktop/amplicateDataset20k_random10000.txt";
        // TextDatasetIterator iterator = new TextDatasetIterator(datasetFile, "<###>", false);
        // List<Instance> temp = CollectionHelper.newArrayList(iterator);
        // List<Instance> training = temp.subList(0, temp.size() / 2);
        // List<Instance> validation = temp.subList(temp.size() / 2 + 1, temp.size());
        
        // Iterable<Instance> training = new TextDatasetIterator("/Users/pk/Dropbox/Uni/Datasets/20newsgroups-18828/index_split1.txt", " ", true);
        // Iterable<Instance> validation = new TextDatasetIterator("/Users/pk/Dropbox/Uni/Datasets/20newsgroups-18828/index_split2.txt", " ", true);
        
        // Iterable<Instance> training = new TextDatasetIterator("/Users/pk/Dropbox/Uni/Datasets/Wikipedia76Languages/languageDocumentIndex_random1000_train.txt", " ", true);
        // Iterable<Instance> validation = new TextDatasetIterator("/Users/pk/Dropbox/Uni/Datasets/Wikipedia76Languages/languageDocumentIndex_random1000_test.txt", " ", true);
        
        // Iterable<Instance> training = new TextDatasetIterator("/Users/pk/Dropbox/Uni/Datasets/imdb1/index_train.tsv", " ", true);
        // Iterable<Instance> validation = new TextDatasetIterator("/Users/pk/Dropbox/Uni/Datasets/imdb1/index_test.tsv", " ", true);
        
        Iterable<Instance> training = new TextDatasetIterator("/Users/pk/Dropbox/Uni/Datasets/Spamassassin/index_shuf_split-1-1.tsv", " ", true);
        Iterable<Instance> validation = new TextDatasetIterator("/Users/pk/Dropbox/Uni/Datasets/Spamassassin/index_shuf_split-1-2.tsv", " ", true);
        
        // load them in memory
        training = CollectionHelper.newArrayList(training);
        validation = CollectionHelper.newArrayList(validation);
        
        List<Scorer> scorers = CollectionHelper.newArrayList();
        scorers.add(new PalladianTextClassifier.DefaultScorer());
        scorers.add(new BayesScorer(new Options[0]));
        scorers.add(new BayesScorer(LAPLACE));
        scorers.add(new BayesScorer(PRIORS));
        scorers.add(new BayesScorer(FREQUENCIES));
        scorers.add(new BayesScorer(COMPLEMENT));
        scorers.add(new BayesScorer(LAPLACE, PRIORS));
        scorers.add(new BayesScorer(LAPLACE, FREQUENCIES));
        scorers.add(new BayesScorer(LAPLACE, COMPLEMENT));
        scorers.add(new BayesScorer(PRIORS, FREQUENCIES));
        scorers.add(new BayesScorer(PRIORS, COMPLEMENT));
        scorers.add(new BayesScorer(FREQUENCIES, COMPLEMENT));
        scorers.add(new BayesScorer(LAPLACE, PRIORS, FREQUENCIES));
        scorers.add(new BayesScorer(LAPLACE, PRIORS, COMPLEMENT));
        scorers.add(new BayesScorer(PRIORS, FREQUENCIES, COMPLEMENT));
        scorers.add(new BayesScorer(LAPLACE, PRIORS, FREQUENCIES, COMPLEMENT));
        evaluateFeatureSettings(featureSettings, null, scorers, training, validation, new ProgressMonitor());
    }

}
