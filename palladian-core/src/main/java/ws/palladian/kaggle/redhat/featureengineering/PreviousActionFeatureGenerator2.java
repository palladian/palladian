package ws.palladian.kaggle.redhat.featureengineering;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.AppendedVector;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.FeatureVectorChecker;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDatasetFeatureVectorTransformer;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.value.ImmutableFloatValue;
import ws.palladian.core.value.ImmutableIntegerValue;
import ws.palladian.core.value.NominalValue;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.LruMap;
import ws.palladian.helper.math.FatStats;
import ws.palladian.helper.math.Stats;
import ws.palladian.kaggle.redhat.dataset.value.LocalDateValue;

public class PreviousActionFeatureGenerator2 extends AbstractDatasetFeatureVectorTransformer {

	/** The logger for this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(PreviousActionFeatureGenerator2.class);
	
	private static final boolean DEBUG = true;

	private final Map<String, NavigableMap<Long, Integer>> previousActivities = new HashMap<>();

	private final String groupBy;

	/** Calculating those features is slow. */
	private final LruMap<String, FeatureVector> cache = LruMap.accessOrder(5000);

	public PreviousActionFeatureGenerator2(Dataset dataset) {
		this(dataset, "people_group_1");
	}

	public PreviousActionFeatureGenerator2(Dataset dataset, String groupBy) {
		Objects.requireNonNull(dataset, "dataset must not be null");
		this.groupBy = Objects.requireNonNull(groupBy, "groupBy must not be null");
		
		LOGGER.info("Initializing PreviousActionFeatureGenerator2");
		StopWatch stopWatch = new StopWatch();

		for (Instance instance : dataset) {
			String id = instance.getVector().get(groupBy).toString();
			LocalDate activityDate = ((LocalDateValue) instance.getVector().get("activity_date")).getLocalDate();
			long activityTime = activityDate.toEpochDay();

			NavigableMap<Long, Integer> map = previousActivities.get(id);
			if (map == null) {
				map = new TreeMap<>();
				previousActivities.put(id, map);
			}

			Integer count = map.get(activityTime);
			map.put(activityTime, count == null ? 1 : count + 1);
		}

		LOGGER.info("Initialized PreviousFeatureGenerator2 in {}", stopWatch);
	}

	@Override
	public FeatureVector compute(FeatureVector input) {

		String id = ((NominalValue) input.get(groupBy)).getString();
		LocalDate activityDate = ((LocalDateValue) input.get("activity_date")).getLocalDate();
		String cacheIdentifier = id + "_" + activityDate;

		FeatureVector addedFeatureVector = cache.get(cacheIdentifier);
		if (addedFeatureVector == null) {
			addedFeatureVector = createFeatureVector(id, activityDate);
			cache.put(cacheIdentifier, addedFeatureVector);
		}

		return new AppendedVector(Arrays.asList(input, addedFeatureVector));

	}

	private FeatureVector createFeatureVector(String id, LocalDate activityDate) {

		long activityTime = activityDate.toEpochDay();
		NavigableMap<Long, Integer> timestamps = previousActivities.get(id);

		InstanceBuilder builder = new InstanceBuilder();

		List<Long> prevActivities = getTimestampsUntil(timestamps, activityTime);
		List<Long> prevActivities_1_day = getTimestampsUntil(timestamps, activityTime - 1);
		List<Long> prevActivities_2_days = getTimestampsUntil(timestamps, activityTime - 2);
		List<Long> prevActivities_3_days = getTimestampsUntil(timestamps, activityTime - 3);
		List<Long> prevActivities_7_days = getTimestampsUntil(timestamps, activityTime - 7);
		List<Long> prevActivities_14_days = getTimestampsUntil(timestamps, activityTime - 14);
		List<Long> prevActivities_28_days = getTimestampsUntil(timestamps, activityTime - 28);

		int numPrev = prevActivities.size();
		int numPrev_1_day = numPrev - prevActivities_1_day.size();
		int numPrev_2_days = numPrev - prevActivities_2_days.size();
		int numPrev_3_days = numPrev - prevActivities_3_days.size();
		int numPrev_7_days = numPrev - prevActivities_7_days.size();
		int numPrev_14_days = numPrev - prevActivities_14_days.size();
		int numPrev_28_days = numPrev - prevActivities_28_days.size();
		if (numPrev <= 0) {
			throw new IllegalStateException("numPrev was zero; this should not happen");
		}
		builder.set(groupBy + "_numPrev", numPrev);
		builder.set(groupBy + "_numPrev_1_day", numPrev_1_day);
		builder.set(groupBy + "_numPrev_2_days", numPrev_2_days);
		builder.set(groupBy + "_numPrev_3_days", numPrev_3_days);
		builder.set(groupBy + "_numPrev_7_days", numPrev_7_days);
		builder.set(groupBy + "_numPrev_14_days", numPrev_14_days);
		builder.set(groupBy + "_numPrev_28_days", numPrev_28_days);
		builder.set(groupBy + "_numPrev_1_day_div_numPrev", (float) numPrev_1_day / numPrev);
		builder.set(groupBy + "_numPrev_2_days_div_numPrev", (float) numPrev_2_days / numPrev);
		builder.set(groupBy + "_numPrev_3_days_div_numPrev", (float) numPrev_3_days / numPrev);
		builder.set(groupBy + "_numPrev_7_days_div_numPrev", (float) numPrev_7_days / numPrev);
		builder.set(groupBy + "_numPrev_14_days_div_numPrev", (float) numPrev_14_days / numPrev);
		builder.set(groupBy + "_numPrev_28_days_div_numPrev", (float) numPrev_28_days / numPrev);

		int numPrevDays = new HashSet<>(prevActivities).size();
		if (numPrevDays <= 0) {
			throw new IllegalStateException("numPrevDays was zero; this should not happen");
		}
		int numPrevDays_2_days = numPrevDays - new HashSet<>(prevActivities_2_days).size();
		int numPrevDays_3_days = numPrevDays - new HashSet<>(prevActivities_3_days).size();
		int numPrevDays_7_days = numPrevDays - new HashSet<>(prevActivities_7_days).size();
		int numPrevDays_14_days = numPrevDays - new HashSet<>(prevActivities_14_days).size();
		int numPrevDays_28_days = numPrevDays - new HashSet<>(prevActivities_28_days).size();
		builder.set(groupBy + "_numPrevDays", numPrevDays);
		builder.set(groupBy + "_numPrevDays_2_days", numPrevDays_2_days);
		builder.set(groupBy + "_numPrevDays_3_days", numPrevDays_3_days);
		builder.set(groupBy + "_numPrevDays_7_days", numPrevDays_7_days);
		builder.set(groupBy + "_numPrevDays_14_days", numPrevDays_14_days);
		builder.set(groupBy + "_numPrevDays_28_days", numPrevDays_28_days);
		builder.set(groupBy + "_numPrevDays_2_days_div_numPrevDays", (float) numPrevDays_2_days / numPrevDays);
		builder.set(groupBy + "_numPrevDays_3_days_div_numPrevDays", (float) numPrevDays_3_days / numPrevDays);
		builder.set(groupBy + "_numPrevDays_7_days_div_numPrevDays", (float) numPrevDays_7_days / numPrevDays);
		builder.set(groupBy + "_numPrevDays_14_days_div_numPrevDays", (float) numPrevDays_14_days / numPrevDays);
		builder.set(groupBy + "_numPrevDays_28_days_div_numPrevDays", (float) numPrevDays_28_days / numPrevDays);

		List<Long> intervals = calculateIntervals(prevActivities);
		if (intervals.size() > 0) {
			Stats intervalStats = new FatStats(intervals);
			builder.set(groupBy + "_prevIntervals_mean", (float) intervalStats.getMean());
			builder.set(groupBy + "_prevIntervals_median", (int) intervalStats.getMedian());
			builder.set(groupBy + "_prevIntervals_min", (int) intervalStats.getMin());
			builder.set(groupBy + "_prevIntervals_max", (int) intervalStats.getMax());
			builder.set(groupBy + "_prevIntervals_stdDev", (float) intervalStats.getStandardDeviation());
			builder.set(groupBy + "_prevIntervals_relStdDev", (float) intervalStats.getRelativeStandardDeviation());
			builder.set(groupBy + "_prevIntervals_variance", (float) intervalStats.getVariance());
		} else {
			builder.setNull(groupBy + "_prevIntervals_mean");
			builder.setNull(groupBy + "_prevIntervals_median");
			builder.setNull(groupBy + "_prevIntervals_min");
			builder.setNull(groupBy + "_prevIntervals_max");
			builder.setNull(groupBy + "_prevIntervals_stdDev");
			builder.setNull(groupBy + "_prevIntervals_relStdDev");
			builder.setNull(groupBy + "_prevIntervals_variance");
		}

		List<Long> dayIntervals = calculateIntervals(new ArrayList<>(new LinkedHashSet<>(prevActivities)));
		if (dayIntervals.size() > 0) {
			Stats dayIntervalStats = new FatStats(dayIntervals);
			builder.set(groupBy + "_prevIntervalsDays_mean", (float) dayIntervalStats.getMean());
			builder.set(groupBy + "_prevIntervalsDays_median", (int) dayIntervalStats.getMedian());
			builder.set(groupBy + "_prevIntervalsDays_min", (int) dayIntervalStats.getMin());
			builder.set(groupBy + "_prevIntervalsDays_max", (int) dayIntervalStats.getMax());
			builder.set(groupBy + "_prevIntervalsDays_stdDev", (float) dayIntervalStats.getStandardDeviation());
			builder.set(groupBy + "_prevIntervalsDays_relStdDev", (float) dayIntervalStats.getRelativeStandardDeviation());
			builder.set(groupBy + "_prevIntervalsDays_variance", (float) dayIntervalStats.getVariance());
		} else {
			builder.setNull(groupBy + "_prevIntervalsDays_mean");
			builder.setNull(groupBy + "_prevIntervalsDays_median");
			builder.setNull(groupBy + "_prevIntervalsDays_min");
			builder.setNull(groupBy + "_prevIntervalsDays_max");
			builder.setNull(groupBy + "_prevIntervalsDays_stdDev");
			builder.setNull(groupBy + "_prevIntervalsDays_relStdDev");
			builder.setNull(groupBy + "_prevIntervalsDays_variance");
		}
		
		FeatureVector featureVector = builder.create();

		if (DEBUG && FeatureVectorChecker.checkForNaNOrInfinite(featureVector).size() > 0) {
			throw new IllegalStateException(
					"null or infinite values in feature vector; this should not happen: " + featureVector);
		}

		return featureVector;

	}

	private static List<Long> calculateIntervals(List<Long> timestamps) {
		List<Long> intervals = new ArrayList<>();
		for (int i = 1; i < timestamps.size(); i++) {
			intervals.add(timestamps.get(i) - timestamps.get(i - 1));
		}
		return intervals;
	}

	private static List<Long> getTimestampsUntil(NavigableMap<Long, Integer> timestamps, long timestamp) {
		List<Long> result = new ArrayList<>();
		for (Entry<Long, Integer> entry : timestamps.headMap(timestamp, true).entrySet()) {
			result.addAll(Collections.nCopies(entry.getValue(), entry.getKey()));
		}
		return result;
	}

	@Override
	public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
		FeatureInformationBuilder builder = new FeatureInformationBuilder().add(featureInformation);

		builder.set(groupBy + "_numPrev", ImmutableIntegerValue.class);
		builder.set(groupBy + "_numPrev_1_day", ImmutableIntegerValue.class);
		builder.set(groupBy + "_numPrev_2_days", ImmutableIntegerValue.class);
		builder.set(groupBy + "_numPrev_3_days", ImmutableIntegerValue.class);
		builder.set(groupBy + "_numPrev_7_days", ImmutableIntegerValue.class);
		builder.set(groupBy + "_numPrev_14_days", ImmutableIntegerValue.class);
		builder.set(groupBy + "_numPrev_28_days", ImmutableIntegerValue.class);
		builder.set(groupBy + "_numPrev_1_day_div_numPrev", ImmutableFloatValue.class);
		builder.set(groupBy + "_numPrev_2_days_div_numPrev", ImmutableFloatValue.class);
		builder.set(groupBy + "_numPrev_3_days_div_numPrev", ImmutableFloatValue.class);
		builder.set(groupBy + "_numPrev_7_days_div_numPrev", ImmutableFloatValue.class);
		builder.set(groupBy + "_numPrev_14_days_div_numPrev", ImmutableFloatValue.class);
		builder.set(groupBy + "_numPrev_28_days_div_numPrev", ImmutableFloatValue.class);

		builder.set(groupBy + "_numPrevDays", ImmutableIntegerValue.class);
		builder.set(groupBy + "_numPrevDays_2_days", ImmutableIntegerValue.class);
		builder.set(groupBy + "_numPrevDays_3_days", ImmutableIntegerValue.class);
		builder.set(groupBy + "_numPrevDays_7_days", ImmutableIntegerValue.class);
		builder.set(groupBy + "_numPrevDays_14_days", ImmutableIntegerValue.class);
		builder.set(groupBy + "_numPrevDays_28_days", ImmutableIntegerValue.class);
		builder.set(groupBy + "_numPrevDays_2_days_div_numPrevDays", ImmutableFloatValue.class);
		builder.set(groupBy + "_numPrevDays_3_days_div_numPrevDays", ImmutableFloatValue.class);
		builder.set(groupBy + "_numPrevDays_7_days_div_numPrevDays", ImmutableFloatValue.class);
		builder.set(groupBy + "_numPrevDays_14_days_div_numPrevDays", ImmutableFloatValue.class);
		builder.set(groupBy + "_numPrevDays_28_days_div_numPrevDays", ImmutableFloatValue.class);

		builder.set(groupBy + "_prevIntervals_mean", ImmutableFloatValue.class);
		builder.set(groupBy + "_prevIntervals_median", ImmutableIntegerValue.class);
		builder.set(groupBy + "_prevIntervals_min", ImmutableIntegerValue.class);
		builder.set(groupBy + "_prevIntervals_max", ImmutableIntegerValue.class);
		builder.set(groupBy + "_prevIntervals_stdDev", ImmutableFloatValue.class);
		builder.set(groupBy + "_prevIntervals_relStdDev", ImmutableFloatValue.class);
		builder.set(groupBy + "_prevIntervals_variance", ImmutableFloatValue.class);

		builder.set(groupBy + "_prevIntervalsDays_mean", ImmutableFloatValue.class);
		builder.set(groupBy + "_prevIntervalsDays_median", ImmutableIntegerValue.class);
		builder.set(groupBy + "_prevIntervalsDays_min", ImmutableIntegerValue.class);
		builder.set(groupBy + "_prevIntervalsDays_max", ImmutableIntegerValue.class);
		builder.set(groupBy + "_prevIntervalsDays_stdDev", ImmutableFloatValue.class);
		builder.set(groupBy + "_prevIntervalsDays_relStdDev", ImmutableFloatValue.class);
		builder.set(groupBy + "_prevIntervalsDays_variance", ImmutableFloatValue.class);

		return builder.create();
	}

//	public static void main(String[] args) {
//
//		Dataset trainAndValidationSet = Config.getJoinedTrain();
//
//		TrainTestSplit split = new PeopleIdSplit(trainAndValidationSet);
//		Dataset trainSet = split.getTrain();
//		Dataset validationSet = split.getTest();
//
//		trainSet = trainSet.transform(new PreviousActionFeatureGenerator2(trainSet, "people_group_1"));
//		trainSet = trainSet.transform(new PreviousActionFeatureGenerator2(trainSet, "people_id"));
//		
//		validationSet = validationSet.transform(new PreviousActionFeatureGenerator2(validationSet, "people_group_1"));
//		validationSet = validationSet.transform(new PreviousActionFeatureGenerator2(validationSet, "people_id"));
//
//		trainSet = trainSet.buffer();
//		validationSet = validationSet.buffer();
//
//		Learner<NaiveBayesModel> learner = new NaiveBayesLearner();
//		Classifier<NaiveBayesModel> classifier = new NaiveBayesClassifier();
//		ClassificationEvaluator<RocCurves> evaluator = new RocCurves.RocCurvesEvaluator("1");
//		Function<RocCurves, Double> mapper = new Function<RocCurves, Double>() {
//			@Override
//			public Double compute(RocCurves input) {
//				return input.getAreaUnderCurve();
//			}
//		};
//
//		FeatureRanker ranker = new SingleFeatureClassification(learner, classifier, evaluator, mapper);
//		FeatureRanking result = ranker.rankFeatures(trainSet, validationSet, new ProgressMonitor(0.1));
//		CollectionHelper.print(result.getAll());
//
//	}

}
