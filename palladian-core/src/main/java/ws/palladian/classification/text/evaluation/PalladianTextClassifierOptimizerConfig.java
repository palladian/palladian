package ws.palladian.classification.text.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.evaluation.ClassificationEvaluator;
import ws.palladian.classification.evaluation.roc.RocCurves;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.FeatureSettingBuilder;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.PalladianTextClassifier.Scorer;
import ws.palladian.core.CategoryEntries;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Filters;

/**
 * Configuration for the {@link PalladianTextClassifierOptimizer}. Create using
 * the {@link #withEvaluator(ClassificationEvaluator)} method.
 * 
 * @author pk
 * @param <R>
 *            Type of the evaluation result (e.g. {@link RocCurves}).
 */
public class PalladianTextClassifierOptimizerConfig<R> {

	public static final class Builder<R> implements Factory<PalladianTextClassifierOptimizer<R>> {

		private static final Collection<FeatureSetting> DEFAULT_FEATURE_SETTINGS = Collections
				.singleton(FeatureSettingBuilder.words().create());

		private static final Set<Filter<Object>> DEFAULT_PRUNING_STRATEGIES = Collections.singleton(Filters.ALL);

		private static final Collection<? extends Scorer> DEFAULT_SCORERS = Collections
				.singleton(PalladianTextClassifier.DEFAULT_SCORER);

		private final ClassificationEvaluator<R> evaluator;

		private Collection<FeatureSetting> featureSettings = DEFAULT_FEATURE_SETTINGS;

		private Collection<? extends Filter<? super CategoryEntries>> pruningStrategies = DEFAULT_PRUNING_STRATEGIES;

		private Collection<? extends Scorer> scorers = DEFAULT_SCORERS;

		private Builder(ClassificationEvaluator<R> evaluator) {
			this.evaluator = evaluator;
		}

		/**
		 * Specify the {@link FeatureSetting}s to evaluate. Use
		 * {@link FeatureSettingGenerator} to create a combinations
		 * conveniently.
		 * 
		 * @param featureSettings
		 *            The feature settings to evaluate, not <code>null</code> or
		 *            empty.
		 * @return
		 */
		public Builder<R> setFeatureSettings(Collection<FeatureSetting> featureSettings) {
			Validate.notEmpty(featureSettings, "featureSettings must not be null");
			this.featureSettings = featureSettings;
			return this;
		}

		/**
		 * Specify the pruning strategies to evaluate.
		 * 
		 * @param pruningStrategies
		 *            The pruning strategies to evaluate, <code>null</code>
		 *            means no pruning.
		 * @return
		 */
		public Builder<R> setPruningStrategies(
				Collection<? extends Filter<? super CategoryEntries>> pruningStrategies) {
			this.pruningStrategies = pruningStrategies;
			return this;
		}

		/**
		 * Specify the pruning strategies to evaluate.
		 * 
		 * @param pruningStrategies
		 *            The pruning strategies to evaluate, <code>null</code>
		 *            means no pruning.
		 * @return
		 */
		@SafeVarargs
		public final Builder<R> setPruningStrategies(Filter<CategoryEntries>... pruningStrategies) {
			return setPruningStrategies(Arrays.asList(pruningStrategies));
		}

		/**
		 * Specify the scorers to evaluate.
		 * 
		 * @param scorers
		 *            The scorers to evaluate, not <code>null</code> or empty.
		 * @return
		 */
		public Builder<R> setScorers(Collection<? extends Scorer> scorers) {
			Validate.notEmpty(scorers, "scorers must not be empty");
			this.scorers = scorers;
			return this;
		}

		/**
		 * Specify the scorers to evaluate.
		 * 
		 * @param scorers
		 *            The scorers to evaluate, not <code>null</code> or empty.
		 * @return
		 */
		public Builder<R> setScorers(Scorer... scorers) {
			return setScorers(Arrays.asList(scorers));
		}

		@Override
		public PalladianTextClassifierOptimizer<R> create() {
			return new PalladianTextClassifierOptimizer<>(new PalladianTextClassifierOptimizerConfig<R>(this));
		}

	}

	public static <R> Builder<R> withEvaluator(ClassificationEvaluator<R> evaluator) {
		return new Builder<R>(evaluator);
	}

	private final ClassificationEvaluator<R> evaluator;

	private final Collection<FeatureSetting> featureSettings;

	private final Collection<? extends Filter<? super CategoryEntries>> pruningStrategies;

	private final Collection<? extends Scorer> scorers;

	public PalladianTextClassifierOptimizerConfig(Builder<R> builder) {
		this.evaluator = builder.evaluator;
		this.featureSettings = new ArrayList<>(builder.featureSettings);
		this.pruningStrategies = new ArrayList<>(builder.pruningStrategies);
		this.scorers = new ArrayList<>(builder.scorers);
	}

	Collection<FeatureSetting> getFeatureSettings() {
		return featureSettings;
	}

	Collection<? extends Scorer> getScorers() {
		return scorers;
	}

	Collection<? extends Filter<? super CategoryEntries>> getPruningStrategies() {
		return pruningStrategies;
	}

	ClassificationEvaluator<R> getEvaluator() {
		return evaluator;
	}

}
