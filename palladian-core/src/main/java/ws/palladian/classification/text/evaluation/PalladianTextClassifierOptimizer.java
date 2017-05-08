package ws.palladian.classification.text.evaluation;

import static ws.palladian.classification.text.BayesScorer.Options.COMPLEMENT;
import static ws.palladian.classification.text.BayesScorer.Options.LAPLACE;

import java.util.Objects;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.evaluation.roc.RocCurves;
import ws.palladian.classification.text.BayesScorer;
import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.PalladianTextClassifier.Scorer;
import ws.palladian.classification.text.PruningStrategies;
import ws.palladian.classification.text.evaluation.PalladianTextClassifierOptimizerConfig.Builder;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.helper.NoProgress;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.io.FileHelper;

/**
 * <p>
 * Simple, brute force optimizer for {@link FeatureSetting}s. This mechanism determines the best feature settings in
 * combination with the best classification threshold (this means, that it currently only works for binary data).
 * 
 * @author Philipp Katz
 * 
 */
public final class PalladianTextClassifierOptimizer<R> {
	
	private final PalladianTextClassifierOptimizerConfig<R> config;
    
    public PalladianTextClassifierOptimizer(PalladianTextClassifierOptimizerConfig<R> config) {
		this.config = Objects.requireNonNull(config, "config must not be null");
	}

    /**
     * Run the optimization process.
     * 
     * @param training The training data, not <code>null</code>.
     * @param validation The validation data, not <code>null</code>.
     * @param resultCsv
     * @param progressReporter
     */
    public void runOptimization(Dataset training, Dataset validation, String resultCsv, ProgressReporter progressReporter) {
        Validate.notNull(training, "training must not be null");
        Validate.notNull(validation, "validation must not be null");
        Validate.notEmpty(resultCsv, "resultCsv must not be empty");
        if (progressReporter == null) {
            progressReporter = NoProgress.INSTANCE;
        }
        boolean headerWritten = false;
        progressReporter.startTask("Evaluating feature settings", config.getFeatureSettings().size() * config.getScorers().size());
        for (FeatureSetting featureSetting : config.getFeatureSettings()) {
            DictionaryModel model = new PalladianTextClassifier(featureSetting, config.getDictionaryBuilder()).train(training);
            for (Filter<? super CategoryEntries> pruningStrategy : config.getPruningStrategies()) {
                model = new PruningSimulatedDictionaryModel(model, pruningStrategy);
                for (Scorer scorer : config.getScorers()) {
                    PalladianTextClassifier textClassifier = new PalladianTextClassifier(featureSetting, scorer);
                    R evaluationResult = config.getEvaluator().evaluate(textClassifier, model, validation);
					if (!headerWritten) {
						String header = "featureSetting;scorer;pruningStrategy;"
								+ config.getEvaluator().getCsvHeader(evaluationResult) + ";numTerms;numEntries\n";
						FileHelper.appendFile(resultCsv, header);
						headerWritten = true;
					}
                    StringBuilder resultLine = new StringBuilder();
                    resultLine.append(featureSetting).append(';');
                    resultLine.append(scorer).append(';');
                    resultLine.append(pruningStrategy).append(';');
                    resultLine.append(config.getEvaluator().getCsvLine(evaluationResult)).append(';');
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
        
        Dataset train = new TextDatasetIterator(trainIndex, " ", true);
        Dataset validate = new TextDatasetIterator(testIndex, " ", true);
        
        Builder<RocCurves> configBuilder = PalladianTextClassifierOptimizerConfig.withEvaluator(new RocCurves.RocCurvesEvaluator("true"));

        // create combinations of settings for char-based 5- to 8- and word-based 1- to 2-grams;
        // this will also create settings e.g. for character-6-7-grams, if you do not want that,
        // add the #noCombinations method
        configBuilder.setFeatureSettings(new FeatureSettingGenerator().chars(5, 8).words(1, 3).create());

        // compare no pruning with minimum term count 2
        configBuilder.setPruningStrategies(PruningStrategies.none(), PruningStrategies.termCount(2));

        // evaluate bayes scorer
        configBuilder.setScorers(new BayesScorer(LAPLACE, COMPLEMENT));
        
        PalladianTextClassifierOptimizer<RocCurves> optimizer = configBuilder.create();

        // run optimization with all (feature setting, pruning, scorer) combinations using the
        // given training and validation data, and write results to optimizationResult.csv file
        optimizer.runOptimization(train, validate, "optimizationResult.csv", null);

    }

}
