package ws.palladian.classification.featureselection;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.Validate;

import ws.palladian.core.Classifier;
import ws.palladian.core.Learner;
import ws.palladian.core.Model;
import ws.palladian.helper.functional.Factories;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Function;
import ws.palladian.helper.math.ConfusionMatrix;

public final class BackwardFeatureEliminationConfig<M extends Model> {
	public static final class Builder<M extends Model> implements Factory<BackwardFeatureElimination<M>>{
		private Factory<? extends Learner<M>> learnerFactory;
		private Factory<? extends Classifier<M>> classifierFactory;
		private Function<ConfusionMatrix, Double> scorer = BackwardFeatureElimination.ACCURACY_SCORER;
		private int numThreads = 1;
		private Collection<Filter<? super String>> featureGroups = new HashSet<>();
		public Builder<M> learner(Learner<M> learner) {
			Validate.notNull(learner, "learner must not be null");
			learner(Factories.constant(learner));
			return this;
		}
		public Builder<M> learner(Factory<? extends Learner<M>> learnerFactory) {
			Validate.notNull(learnerFactory, "learnerFactory must not be null");
			this.learnerFactory = learnerFactory;
			return this;
		}
		public Builder<M> classifier(Classifier<M> classifier) {
			Validate.notNull(classifier, "classifier must not be null");
			classifier(Factories.constant(classifier));
			return this;
		}
		public Builder<M> classifier(Factory<? extends Classifier<M>> classifierFactory) {
			Validate.notNull(classifierFactory, "classifierFactory must not be null");
			this.classifierFactory = classifierFactory;
			return this;
		}
		public Builder<M> scorer(Function<ConfusionMatrix, Double> scorer) {
			Validate.notNull(scorer, "scorer must not be null");
			this.scorer = scorer;
			return this;
		}
		public Builder<M> scoreAccuracy() {
			this.scorer = BackwardFeatureElimination.ACCURACY_SCORER;
			return this;
		}
		public Builder<M> scoreF1(String className) {
			this.scorer = new BackwardFeatureElimination.FMeasureScorer(className);
			return this;
		}
		public Builder<M> numThreads(int numThreads) {
			Validate.isTrue(numThreads > 0, "numThreads must be greater zero");
			this.numThreads = numThreads;
			return this;
		}
		public Builder<M> featureGroups(Collection<? extends Filter<? super String>> featureGroups) {
			Validate.notNull(featureGroups, "featureGroups must not be null");
			this.featureGroups = new HashSet<>(featureGroups);
			return this;
		}
		public Builder<M> addFeatureGroup(Filter<? super String> featureGroup) {
			Validate.notNull(featureGroup, "featureGroup must not be null");
			this.featureGroups.add(featureGroup);
			return this;
		}
		@Override
		public BackwardFeatureElimination<M> create() {
			return new BackwardFeatureElimination<>(createConfig());
		}
		public BackwardFeatureEliminationConfig<M> createConfig() {
			if (learnerFactory == null) {
				throw new IllegalArgumentException("no learner specified");
			}
			if (classifierFactory == null) {
				throw new IllegalArgumentException("no classifier specified");
			}
			return new BackwardFeatureEliminationConfig<>(this);
		}
	}
	private final Factory<? extends Learner<M>> learnerFactory;
	private final Factory<? extends Classifier<M>> classifierFactory;
	private final Function<ConfusionMatrix, Double> scorer;
	private final int numThreads;
	private final Collection<? extends Filter<? super String>> featureGroups;
	private BackwardFeatureEliminationConfig(Builder<M> builder) {
		learnerFactory = builder.learnerFactory;
		classifierFactory = builder.classifierFactory;
		scorer = builder.scorer;
		numThreads = builder.numThreads;
		featureGroups = builder.featureGroups;
	}
	public Learner<M> createLearner() {
		return learnerFactory.create();
	}
	public Classifier<M> createClassifier() {
		return classifierFactory.create();
	}
	public Function<ConfusionMatrix, Double> scorer() {
		return scorer;
	}
	public int numThreads() {
		return numThreads;
	}
	public Collection<? extends Filter<? super String>> featureGroups() {
		return featureGroups;
	}
}
