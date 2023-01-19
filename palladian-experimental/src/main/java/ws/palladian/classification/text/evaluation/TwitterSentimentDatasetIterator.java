package ws.palladian.classification.text.evaluation;

import org.apache.commons.lang3.Validate;
import ws.palladian.classification.evaluation.roc.RocCurves;
import ws.palladian.classification.text.BayesScorer;
import ws.palladian.classification.text.BayesScorer.Options;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.classification.text.PalladianTextClassifier.Scorer;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.dataset.split.RandomSplit;
import ws.palladian.core.value.TextValue;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.*;
import ws.palladian.helper.nlp.StringHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static ws.palladian.classification.text.PalladianTextClassifier.VECTOR_TEXT_IDENTIFIER;
import static ws.palladian.helper.constants.Language.ENGLISH;

public final class TwitterSentimentDatasetIterator extends AbstractDataset {

    /**
     * The normalization options as described in "<a href="http://s3.eddieoz.com/docs/sentiment_analysis/
     * Twitter_Sentiment_Classification_using_Distant_Supervision
     * .pdf">Twitter Sentiment Classification using Distant Supervision</a>"; Alec Go;
     * Richa Bhayani; Lei Huang; 2009.
     */
    public enum NormalizationOptions {
        /** Replace query terms by 'QUERY_TERM'. */
        QUERY_TERM,
        /** Replace user names by 'USERNAME'. */
        USER_NAMES,
        /** Replace links by 'URL'. */
        LINKS,
        /** Normalize repeated letters in words (e.g. 'huuuuuuungry' becomes 'huungry'). */
        REPEATED_LETTERS
    }

    private final File datasetFile;
    private final Set<NormalizationOptions> options;
    private final int numLines;

    public TwitterSentimentDatasetIterator(File datasetFile, NormalizationOptions... options) {
        Validate.notNull(datasetFile, "datasetFile must not be null");
        this.datasetFile = datasetFile;
        this.options = CollectionHelper.newHashSet(options);
        this.numLines = FileHelper.getNumberOfLines(datasetFile);
    }

    public TwitterSentimentDatasetIterator(File datasetFile) {
        this(datasetFile, NormalizationOptions.values());
    }

    @Override
    public CloseableIterator<Instance> iterator() {
        LineIterator lineIterator = new LineIterator(datasetFile);
        final ProgressMonitor monitor = new ProgressMonitor();
        monitor.startTask(getClass().getSimpleName(), numLines);
        Function<String, Instance> converter = new Function<String, Instance>() {
            @Override
            public Instance apply(String input) {
                List<String> split = DelimitedStringHelper.splitLine(input, ',', '"');
                if (split.size() != 6) {
                    throw new IllegalStateException("Expected six columns, got " + split.size() + " in '" + input + "'");
                }
                String category = split.get(0);
                String text = split.get(5);
                if (options.contains(NormalizationOptions.QUERY_TERM)) {
                    String queryTerm = split.get(3);
                    text = StringHelper.replaceWord(queryTerm, "QUERY_TERM", text);
                }
                if (options.contains(NormalizationOptions.USER_NAMES)) {
                    text = text.replaceAll("@[^\\s]+", "USERNAME");
                }
                if (options.contains(NormalizationOptions.LINKS)) {
                    text = text.replaceAll("https?://[^\\s]+", "URL");
                }
                if (options.contains(NormalizationOptions.REPEATED_LETTERS)) {
                    text = text.replaceAll("(\\w)\\1{3,}", "$1$1");
                }
                monitor.increment();
                return new InstanceBuilder().setText(text).create(category);
            }
        };
        return new CloseableIteratorAdapter<>(CollectionHelper.convert(lineIterator, converter));
    }

    @Override
    public FeatureInformation getFeatureInformation() {
        return new FeatureInformationBuilder().set(VECTOR_TEXT_IDENTIFIER, TextValue.class).create();
    }

    @Override
    public long size() {
        return this.numLines;
    }

    public static void main(String[] args) {
        File trainData = new File("/Users/pk/Desktop/training.1600000.processed.noemoticon.csv");
        // File testData = new File("/Users/pk/Downloads/trainingandtestdata/testdata.manual.2009.06.14.csv");
        ws.palladian.core.dataset.Dataset dataset = new TwitterSentimentDatasetIterator(trainData);
        dataset = dataset.buffer();
        RandomSplit split = new RandomSplit(dataset, 0.5);

        PalladianTextClassifierOptimizerConfig.Builder<RocCurves> builder = PalladianTextClassifierOptimizerConfig.withEvaluator(new RocCurves.RocCurvesEvaluator("4"));
        List<FeatureSetting> featureSettings = new ArrayList<>();
        featureSettings.addAll(new FeatureSettingGenerator().words(1, 3).create());
        featureSettings.addAll(new FeatureSettingGenerator().chars(3, 10).create());
        featureSettings.add(FeatureSettingBuilder.words().language(ENGLISH).removeStopwords().create());
        featureSettings.add(FeatureSettingBuilder.words().language(ENGLISH).removeStopwords().stem().create());
        featureSettings.add(FeatureSettingBuilder.words().language(ENGLISH).stem().create());
        featureSettings.add(FeatureSettingBuilder.words(1, 2).language(ENGLISH).removeStopwords().create());
        featureSettings.add(FeatureSettingBuilder.words(1, 2).language(ENGLISH).removeStopwords().stem().create());
        featureSettings.add(FeatureSettingBuilder.words(1, 2).language(ENGLISH).stem().create());
        // featureSettings.add(FeatureSettingBuilder.words(1,3).language(ENGLISH).removeStopwords().create());
        // featureSettings.add(FeatureSettingBuilder.words(1,3).language(ENGLISH).removeStopwords().stem().create());
        // featureSettings.add(FeatureSettingBuilder.words(1,3).language(ENGLISH).stem().create());
        // featureSettings.add(FeatureSettingBuilder.words(1,3).language(ENGLISH).removeStopwords().createSkipGrams().create());
        // featureSettings.add(FeatureSettingBuilder.words(1,3).language(ENGLISH).removeStopwords().createSkipGrams().stem().create());
        // featureSettings.add(FeatureSettingBuilder.words(1,3).language(ENGLISH).stem().createSkipGrams().create());

        List<Scorer> scorers = new ArrayList<>();
        // scorers.add(PalladianTextClassifier.DEFAULT_SCORER);
        // scorers.add(new BayesScorer());
        // scorers.add(new BayesScorer(Options.FREQUENCIES));
        scorers.add(new BayesScorer(Options.LAPLACE));
        // scorers.add(new BayesScorer(Options.PRIORS));
        scorers.add(new BayesScorer(Options.FREQUENCIES, Options.LAPLACE));
        // scorers.add(new BayesScorer(Options.FREQUENCIES,Options.PRIORS));
        scorers.add(new BayesScorer(Options.LAPLACE, Options.PRIORS));
        scorers.add(new BayesScorer(Options.FREQUENCIES, Options.LAPLACE, Options.PRIORS));

        builder.setScorers(scorers);
        builder.setFeatureSettings(featureSettings);
        PalladianTextClassifierOptimizer<RocCurves> optimizer = builder.create();
        optimizer.runOptimization(split.getTrain(), split.getTest(), "/Users/pk/Desktop/evaluation-result.csv", new ProgressMonitor());

    }

}
