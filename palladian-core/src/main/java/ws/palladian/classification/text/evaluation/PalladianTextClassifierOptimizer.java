package ws.palladian.classification.text.evaluation;

import static ws.palladian.classification.text.BayesScorer.Options.COMPLEMENT;
import static ws.palladian.classification.text.BayesScorer.Options.LAPLACE;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.classification.text.BayesScorer;
import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.PalladianTextClassifier.Scorer;
import ws.palladian.classification.text.PruningStrategies;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Instance;
import ws.palladian.helper.NoProgress;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Filters;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;

/**
 * <p>
 * Simple, brute force optimizer for {@link FeatureSetting}s. This mechanism determines the best feature settings in
 * combination with the best classification threshold (this means, that it currently only works for binary data).
 * 
 * @author Philipp Katz
 * 
 */
public final class PalladianTextClassifierOptimizer {

    private Collection<FeatureSetting> featureSettings;

    private Collection<? extends Filter<? super CategoryEntries>> pruningStrategies;

    private Collection<? extends Scorer> scorers;

    private ProgressReporter progressReporter;

    /**
     * Specify the {@link FeatureSetting}s to evaluate. Use {@link FeatureSettingGenerator} to create a combinations
     * conveniently.
     * 
     * @param featureSettings The feature settings to evaluate, not <code>null</code> or empty.
     * @return
     */
    public PalladianTextClassifierOptimizer setFeatureSettings(Collection<FeatureSetting> featureSettings) {
        Validate.notEmpty(featureSettings, "featureSettings must not be null");
        this.featureSettings = featureSettings;
        return this;
    }

    /**
     * Specify the pruning strategies to evaluate.
     * 
     * @param pruningStrategies The pruning strategies to evaluate, <code>null</code> means no pruning.
     * @return
     */
    public PalladianTextClassifierOptimizer setPruningStrategies(
            Collection<? extends Filter<? super CategoryEntries>> pruningStrategies) {
        this.pruningStrategies = pruningStrategies;
        return this;
    }

    /**
     * Specify the pruning strategies to evaluate.
     * 
     * @param pruningStrategies The pruning strategies to evaluate, <code>null</code> means no pruning.
     * @return
     */
    @SafeVarargs
    public final PalladianTextClassifierOptimizer setPruningStrategies(Filter<CategoryEntries>... pruningStrategies) {
        return setPruningStrategies(Arrays.asList(pruningStrategies));
    }

    /**
     * Specify the scorers to evaluate.
     * 
     * @param scorers The scorers to evaluate, not <code>null</code> or empty.
     * @return
     */
    public PalladianTextClassifierOptimizer setScorers(Collection<? extends Scorer> scorers) {
        Validate.notEmpty(scorers, "scorers must not be empty");
        this.scorers = scorers;
        return this;
    }

    /**
     * Specify the scorers to evaluate.
     * 
     * @param scorers The scorers to evaluate, not <code>null</code> or empty.
     * @return
     */
    public PalladianTextClassifierOptimizer setScorers(Scorer... scorers) {
        return setScorers(Arrays.asList(scorers));
    }

    /**
     * Specify a {@link ProgressReporter}.
     * 
     * @param progressReporter For reporting progress, or <code>null</code> when no progress should be reported.
     * @return
     */
    public PalladianTextClassifierOptimizer setProgressReporter(ProgressReporter progressReporter) {
        this.progressReporter = progressReporter;
        return this;
    }

    /**
     * Run the optimization process.
     * 
     * @param training The training data, not <code>null</code>.
     * @param validation The validation data, not <code>null</code>.
     * @param resultCsv
     */
    public void runOptimization(Iterable<Instance> training, Iterable<Instance> validation, String resultCsv) {
        Validate.notNull(training, "training must not be null");
        Validate.notNull(validation, "validation must not be null");
        Validate.notEmpty(resultCsv, "resultCsv must not be empty");
        if (pruningStrategies == null || pruningStrategies.isEmpty()) {
            pruningStrategies = Collections.singleton(Filters.ALL);
        }
        if (progressReporter == null) {
            progressReporter = NoProgress.INSTANCE;
        }
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
                        StringBuilder header = new StringBuilder(
                                "featureSetting;pruningStrategy;scorer;avgPr;avgRc;avgF1;accuracy;");
                        for (String categoryName : categoryNames) {
                            header.append("pr-").append(categoryName).append(';');
                            header.append("rc-").append(categoryName).append(';');
                            header.append("f1-").append(categoryName).append(';');
                            header.append("acc-").append(categoryName).append(';');
                        }
                        header.append("numTerms;");
                        header.append("numEntries\n");
                        FileHelper.appendFile(resultCsv, header);
                        headerWritten = true;
                    }
                    StringBuilder resultLine = new StringBuilder();
                    resultLine.append(featureSetting).append(';');
                    resultLine.append(scorer).append(';');
                    resultLine.append(pruningStrategy).append(';');
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
                        resultLine.append(confusionMatrix.getAccuracy(categoryName)).append(';');
                    }
                    resultLine.append(model.getNumUniqTerms()).append(';');
                    resultLine.append(model.getNumEntries()).append('\n');
                    FileHelper.appendFile(resultCsv, resultLine);
                    progressReporter.increment();
                }
            }
        }
        progressReporter.finishTask();
    }

    public static void main(String[] args) {
        String trainIndex = "/Users/pk/Dropbox/Uni/Datasets/20newsgroups-18828/index_split1.txt";
        String testIndex = "/Users/pk/Dropbox/Uni/Datasets/20newsgroups-18828/index_split2.txt";
        
        Iterable<Instance> train = new TextDatasetIterator(trainIndex, " ", true);
        Iterable<Instance> validate = new TextDatasetIterator(testIndex, " ", true);

        PalladianTextClassifierOptimizer optimizer = new PalladianTextClassifierOptimizer();

        // create combinations of settings for char-based 5- to 8- and word-based 1- to 2-grams;
        // this will also create settings e.g. for character-6-7-grams, if you do not want that,
        // add the #noCombinations method
        optimizer.setFeatureSettings(new FeatureSettingGenerator().chars(5, 8).words(1, 3).create());

        // compare no pruning with minimum term count 2
        optimizer.setPruningStrategies(PruningStrategies.none(), PruningStrategies.termCount(2));

        // evaluate bayes scorer
        optimizer.setScorers(new BayesScorer(LAPLACE, COMPLEMENT));

        // run optimization with all (feature setting, pruning, scorer) combinations using the
        // given training and validation data, and write results to optimizationResult.csv file
        optimizer.runOptimization(train, validate, "optimizationResult.csv");

    }

}
