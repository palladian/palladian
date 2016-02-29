package ws.palladian.classification.featureselection.shuffling;

import static ws.palladian.classification.featureselection.BackwardFeatureElimination.ACCURACY_SCORER;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.Validate;

import ws.palladian.classification.dt.QuickDtClassifier;
import ws.palladian.classification.dt.QuickDtLearner;
import ws.palladian.classification.dt.QuickDtModel;
import ws.palladian.classification.featureselection.AbstractFeatureRanker;
import ws.palladian.classification.featureselection.FeatureRanking;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.classification.utils.ClassifierEvaluation;
import ws.palladian.classification.utils.CsvDatasetReader;
import ws.palladian.classification.utils.CsvDatasetReaderConfig;
import ws.palladian.classification.utils.CsvDatasetReaderConfig.Builder;
import ws.palladian.core.Classifier;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.Model;
import ws.palladian.core.value.Value;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Function;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.FatStats;
import ws.palladian.helper.math.Stats;

public class ShufflingFeatureRanker<MODEL extends Model> extends AbstractFeatureRanker {

	private final Classifier<MODEL> classifier;

	private final MODEL model;

	private final Function<ConfusionMatrix, Double> scorer;

	private final Random random = new Random();

	private final int repetitions;

	public ShufflingFeatureRanker(Classifier<MODEL> classifier, MODEL model,
			Function<ConfusionMatrix, Double> scorer, int repetitions) {
		Validate.notNull(classifier, "classifier must not be null");
		Validate.notNull(model, "model must not be null");
		Validate.notNull(scorer, "scorer must not be null");
		Validate.isTrue(repetitions > 0, "repetitions must be greater zero");
		this.classifier = classifier;
		this.model = model;
		this.scorer = scorer;
		this.repetitions = repetitions;
	}

	@Override
	public FeatureRanking rankFeatures(Collection<? extends Instance> dataset, ProgressReporter progress) {
		Validate.notNull(dataset, "dataset must not be null");
		Validate.notNull(progress, "progress must not be null");
		FeatureRanking ranking = new FeatureRanking();
		Set<String> featureNames = ClassificationUtils.getFeatureNames(ClassificationUtils.unwrapInstances(dataset));
		progress.startTask("Target Shuffling", featureNames.size() * repetitions);
		for (String featureName : featureNames) {
			Stats scoreStats = new FatStats();
			for (int repetition = 0; repetition < repetitions; repetition++) {
				Iterable<Instance> featureShuffledData = createFeatureShuffledSet(dataset, featureName);
				@SuppressWarnings("unchecked")
				ConfusionMatrix confusionMatrix = ClassifierEvaluation.evaluate(classifier, featureShuffledData, model);
				scoreStats.add(scorer.compute(confusionMatrix));
				progress.increment();
			}
			ranking.add(featureName, scoreStats.getStandardDeviation());
		}
		return ranking;
	}

	private Iterable<Instance> createFeatureShuffledSet(Collection<? extends Instance> dataset, String featureName) {
		List<Value> featureValues = new ArrayList<>();
		for (Instance instance : dataset) {
			featureValues.add(instance.getVector().get(featureName));
		}
		Collections.shuffle(featureValues, random);
		// create new instances with shuffled values for current feature
		List<Instance> featureShuffledInstances = new ArrayList<>();
		Iterator<Value> valueIterator = featureValues.iterator();
		for (Instance instance : dataset) {
			InstanceBuilder builder = new InstanceBuilder().add(instance.getVector());
			builder.set(featureName, valueIterator.next());
			featureShuffledInstances.add(builder.create(instance.getCategory()));
		}
		return featureShuffledInstances;
	}

	public static void main(String[] args) {
//		String dataFile = "/Users/pk/Code/palladian/palladian-core/src/test/resources/classifier/adultData.txt";
//		String dataFile = "/Users/pk/Code/palladian/palladian-core/src/test/resources/classifier/carData.txt";
		String dataFile = "/Users/pk/Code/palladian/palladian-core/src/test/resources/classifier/diabetes2.csv";
		Builder builder = CsvDatasetReaderConfig.filePath(new File(dataFile));
		builder.readHeader(true);
		CsvDatasetReader reader = builder.create();
		List<Instance> data = reader.readAll();
		List<Instance> train = data.subList(0, data.size() / 2);
		List<Instance> test = data.subList(data.size() / 2 + 1, data.size() - 1);
		QuickDtLearner learner = QuickDtLearner.randomForest(100);
		QuickDtModel model = learner.train(train);
		ShufflingFeatureRanker<QuickDtModel> ranker = new ShufflingFeatureRanker<>(new QuickDtClassifier(),
				model, ACCURACY_SCORER, 50);
		FeatureRanking ranking = ranker.rankFeatures(test, new ProgressMonitor());
		CollectionHelper.print(ranking.getAll());
	}

}
