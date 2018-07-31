package ws.palladian.classification.featureselection;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.Validate;

import ws.palladian.classification.evaluation.ClassificationEvaluator;
import ws.palladian.classification.evaluation.ConfusionMatrixEvaluator;
import ws.palladian.classification.evaluation.roc.RocCurves;
import ws.palladian.core.Classifier;
import ws.palladian.core.Learner;
import ws.palladian.core.Model;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.helper.functional.Factories;
import ws.palladian.helper.functional.Factory;
import java.util.function.Function;
import java.util.function.Predicate;
import ws.palladian.helper.math.ConfusionMatrix;

public class FeatureSelectorConfig {
	static final class EvaluationConfig<M extends Model, R> {
		private final Factory<? extends Learner<M>> learnerFactory;
		private final Factory<? extends Classifier<M>> classifierFactory;
		private final ClassificationEvaluator<R> evaluator;
		private final Function<R, Double> mapper;
		private EvaluationConfig(Factory<? extends Learner<M>> learnerFactory, Factory<? extends Classifier<M>> classifierFactory, ClassificationEvaluator<R> evaluator, Function<R, Double> mapper) {
			this.learnerFactory = learnerFactory;
			this.classifierFactory = classifierFactory;
			this.evaluator = evaluator;
			this.mapper = mapper;
		}
		public double score(Dataset trainData, Dataset testData) {
			return mapper.apply(evaluator.evaluate(learnerFactory.create(), classifierFactory.create(), trainData, testData));
		}
	}
	public static final class Builder<M extends Model> implements Factory<FeatureSelector>{
		private final Factory<? extends Learner<M>> learnerFactory;
		private final Factory<? extends Classifier<M>> classifierFactory;
		private EvaluationConfig<M, ?> evaluator;
		private int numThreads = 1;
		private Collection<Predicate<? super String>> featureGroups = new HashSet<>();
		private boolean backward = true;
		private Builder(Learner<M> learner, Classifier<M> classifier) {
			this(Factories.constant(learner), Factories.constant(classifier));
		}
		@SuppressWarnings("deprecation")
		private Builder(Factory<? extends Learner<M>> learnerFactory, Factory<? extends Classifier<M>> classifierFactory) {
			this.learnerFactory = learnerFactory;
			this.classifierFactory = classifierFactory;
			this.evaluator = new EvaluationConfig<>(learnerFactory, classifierFactory, new ConfusionMatrixEvaluator(), FeatureSelector.ACCURACY_SCORER);
		}
//		public Builder<M> learner(Learner<M> learner) {
//			Validate.notNull(learner, "learner must not be null");
//			learner(Factories.constant(learner));
//			return this;
//		}
//		public Builder<M> learner(Factory<? extends Learner<M>> learnerFactory) {
//			Validate.notNull(learnerFactory, "learnerFactory must not be null");
//			this.learnerFactory = learnerFactory;
//			return this;
//		}
//		public Builder<M> classifier(Classifier<M> classifier) {
//			Validate.notNull(classifier, "classifier must not be null");
//			classifier(Factories.constant(classifier));
//			return this;
//		}
//		public Builder<M> classifier(Factory<? extends Classifier<M>> classifierFactory) {
//			Validate.notNull(classifierFactory, "classifierFactory must not be null");
//			this.classifierFactory = classifierFactory;
//			return this;
//		}
		@Deprecated
		public Builder<M> scorer(Function<ConfusionMatrix, Double> scorer) {
			Validate.notNull(scorer, "scorer must not be null");
			evaluator(new ConfusionMatrixEvaluator(), scorer);
			return this;
		}
		@SuppressWarnings("deprecation")
		public Builder<M> scoreAccuracy() {
			scorer(FeatureSelector.ACCURACY_SCORER);
			return this;
		}
		@SuppressWarnings("deprecation")
		public Builder<M> scoreF1(String className) {
			scorer(new FeatureSelector.FMeasureScorer(className));
			return this;
		}
		public Builder<M> scoreAuc(String className) {
			Validate.notNull(className, "className must not be null");
			evaluator(new RocCurves.RocCurvesEvaluator(className), new Function<RocCurves, Double>() {
				@Override
				public Double apply(RocCurves input) {
					return input.getAreaUnderCurve();
				}
			});
			return this;
		}
		public <R> Builder<M> evaluator(ClassificationEvaluator<R> evaluator, Function<R, Double> mapper) {
			this.evaluator = new EvaluationConfig<M, R>(learnerFactory, classifierFactory, evaluator, mapper);
			return this;
		}
		public Builder<M> numThreads(int numThreads) {
			Validate.isTrue(numThreads > 0, "numThreads must be greater zero");
			this.numThreads = numThreads;
			return this;
		}
		public Builder<M> featureGroups(Collection<? extends Predicate<? super String>> featureGroups) {
			Validate.notNull(featureGroups, "featureGroups must not be null");
			this.featureGroups = new HashSet<>(featureGroups);
			return this;
		}
		public Builder<M> addFeatureGroup(Predicate<? super String> featureGroup) {
			Validate.notNull(featureGroup, "featureGroup must not be null");
			this.featureGroups.add(featureGroup);
			return this;
		}
		/**
		 * Perform a forward feature construction, i.e. start with zero features
		 * and add the best performing feature in each iteration.
		 * 
		 * @return The builder.
		 */
		public Builder<M> forward() {
			this.backward = false;
			return this;
		}
		/**
		 * Perform a backward feature elimination, i.e. start with all features
		 * and remove the feature, which has the smallest impact on the
		 * classification quality.
		 * 
		 * @return The builder.
		 */
		public Builder<M> backward() {
			this.backward = true;
			return this;
		}
		@Override
		public FeatureSelector create() {
			return new FeatureSelector(createConfig());
		}
		public FeatureSelectorConfig createConfig() {
			if (learnerFactory == null) {
				throw new IllegalArgumentException("no learner specified");
			}
			if (classifierFactory == null) {
				throw new IllegalArgumentException("no classifier specified");
			}
			return new FeatureSelectorConfig(this);
		}
	}
	public static <M extends Model> Builder<M> with(Learner<M> learner, Classifier<M> classifier) {
		return new Builder<M>(learner, classifier);
	}
	public static <M extends Model> Builder<M> with(Factory<? extends Learner<M>> learnerFactory, Factory<? extends Classifier<M>> classifierFactory) {
		return new Builder<M>(learnerFactory, classifierFactory);
	}
//	private final Factory<? extends Learner<M>> learnerFactory;
//	private final Factory<? extends Classifier<M>> classifierFactory;
	private final EvaluationConfig<?, ?> evaluator;
	private final int numThreads;
	private final Collection<? extends Predicate<? super String>> featureGroups;
	private final boolean backward;
	protected FeatureSelectorConfig(Builder<?> builder) {
//		learnerFactory = builder.learnerFactory;
//		classifierFactory = builder.classifierFactory;
		evaluator = builder.evaluator;
		numThreads = builder.numThreads;
		featureGroups = builder.featureGroups;
		backward = builder.backward;
	}
//	public Learner<M> createLearner() {
//		return learnerFactory.create();
//	}
//	public Classifier<M> createClassifier() {
//		return classifierFactory.create();
//	}
	public EvaluationConfig<?, ?> evaluator() {
		return evaluator;
	}
	public int numThreads() {
		return numThreads;
	}
	public Collection<? extends Predicate<? super String>> featureGroups() {
		return featureGroups;
	}
	public boolean isBackward() {
		return backward;
	}
}
