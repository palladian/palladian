package ws.palladian.extraction.feature;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Locale;

import org.apache.commons.lang.Validate;

import ws.palladian.core.AppendedVector;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDatasetFeatureVectorTransformer;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.value.ImmutableBooleanValue;
import ws.palladian.core.value.ImmutableIntegerValue;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.core.value.LocalDateValue;
import ws.palladian.core.value.Value;

public class DateParticleExtractor extends AbstractDatasetFeatureVectorTransformer {
	
	public static enum Precision {
		YEAR, MONTH, DAY
	}

	private static final TemporalField WEEK_OF_YEAR = WeekFields.of(Locale.US).weekOfYear();
	
	private static final String SUFFIX_WEEKEND = "_weekend";
	private static final String SUFFIX_DAY_OF_WEEK_NUM = "_dayOfWeek_num";
	private static final String SUFFIX_DAY_OF_WEEK_NOM = "_dayOfWeek_nom";
	private static final String SUFFIX_DAY_OF_MONTH_NUM = "_dayOfMonth_num";
	private static final String SUFFIX_DAY_OF_MONTH_NOM = "_dayOfMonth_nom";
	private static final String SUFFIX_DAY_OF_YEAR_NUM = "_dayOfYear_num";
	private static final String SUFFIX_DAY_OF_YEAR_NOM = "_dayOfYear_nom";
	private static final String SUFFIX_WEEK_NUM = "_week_num";
	private static final String SUFFIX_WEEK_NOM = "_week_nom";
	private static final String SUFFIX_MONTH_NUM = "_month_num";
	private static final String SUFFIX_MONTH_NOM = "_month_nom";
	private static final String SUFFIX_QUARTER = "_quarter";
	private static final String SUFFIX_YEAR_NUM = "_year_num";
	private static final String SUFFIX_YEAR_NOM = "_year_nom";

	private static final String SUFFIX_YEAR_QUARTER = "_year-quarter";
	private static final String SUFFIX_YEAR_MONTH = "_year-month";
	private static final String SUFFIX_YEAR_WEEK = "_year-week";

	private final String featureName;

	private final Precision precision;

	public DateParticleExtractor(String featureName, Precision precision) {
		Validate.notEmpty(featureName, "featureName must not be empty");
		Validate.notNull(precision, "precision must not be null");
		this.precision = precision;
		this.featureName = featureName;
	}
	public DateParticleExtractor(String featureName) {
		this(featureName, Precision.DAY);
	}

	@Override
	public FeatureVector apply(FeatureVector featureVector) {
		Value value = featureVector.get(featureName);
		
		if (value.isNull()) {
			return featureVector;
		}
		
		LocalDateValue dateValue = (LocalDateValue) value;
		LocalDate date = dateValue.getLocalDate();
		InstanceBuilder resultBuilder = new InstanceBuilder();
		
		
		resultBuilder.set(featureName + SUFFIX_YEAR_NUM, date.getYear());
		resultBuilder.set(featureName + SUFFIX_YEAR_NOM, "Y" + String.valueOf(date.getYear()));

		
		// month precision
		if (precision == Precision.MONTH || precision == Precision.DAY) {
			resultBuilder.set(featureName + SUFFIX_QUARTER, getSeason(date));

			resultBuilder.set(featureName + SUFFIX_MONTH_NUM, date.getMonthValue());
			resultBuilder.set(featureName + SUFFIX_MONTH_NOM, date.getMonth().toString());

			resultBuilder.set(featureName + SUFFIX_YEAR_QUARTER, date.getYear() + "-" + getSeason(date));
			resultBuilder.set(featureName + SUFFIX_YEAR_MONTH, date.getYear() + "-" + date.getMonthValue());
		}

		// day precision
		if (precision == Precision.DAY) {
			resultBuilder.set(featureName + SUFFIX_WEEK_NUM, getWeekOfYear(date));
			resultBuilder.set(featureName + SUFFIX_WEEK_NOM, "cw_" + String.valueOf(getWeekOfYear(date)));

			resultBuilder.set(featureName + SUFFIX_DAY_OF_YEAR_NUM, date.getDayOfYear());
			resultBuilder.set(featureName + SUFFIX_DAY_OF_YEAR_NOM, "d_" + String.valueOf(date.getDayOfYear()));

			resultBuilder.set(featureName + SUFFIX_DAY_OF_MONTH_NUM, date.getDayOfMonth());
			resultBuilder.set(featureName + SUFFIX_DAY_OF_MONTH_NOM, "dm_" + String.valueOf(date.getDayOfMonth()));

			resultBuilder.set(featureName + SUFFIX_DAY_OF_WEEK_NUM, date.getDayOfWeek().getValue());
			resultBuilder.set(featureName + SUFFIX_DAY_OF_WEEK_NOM, date.getDayOfWeek().toString());

			resultBuilder.set(featureName + SUFFIX_WEEKEND, isWeekend(date));

			resultBuilder.set(featureName + SUFFIX_YEAR_WEEK, date.getYear() + "-" + getWeekOfYear(date));
		}
		return new AppendedVector(featureVector, resultBuilder.create());
	}

	private static int getWeekOfYear(LocalDate date) {
		return date.get(WEEK_OF_YEAR);
	}

	private static boolean isWeekend(LocalDate date) {
		switch (date.getDayOfWeek()) {
		case SATURDAY:
		case SUNDAY:
			return true;
		default:
			return false;
		}
	}

	private static String getSeason(LocalDate date) {
		Month month = date.getMonth();
		switch (month) {
		case JANUARY:
		case FEBRUARY:
		case MARCH:
			return "WINTER";
		case APRIL:
		case MAY:
		case JUNE:
			return "SPRING";
		case JULY:
		case AUGUST:
		case SEPTEMBER:
			return "SUMMER";
		case OCTOBER:
		case NOVEMBER:
		case DECEMBER:
			return "AUTUMN";
		}
		throw new IllegalArgumentException();
	}

	@Override
	public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
		FeatureInformationBuilder resultBuilder = new FeatureInformationBuilder();
		resultBuilder.add(featureInformation);

		resultBuilder.set(featureName + SUFFIX_YEAR_NUM, ImmutableIntegerValue.class);
		resultBuilder.set(featureName + SUFFIX_YEAR_NOM, ImmutableStringValue.class);
		
		
		if (precision == Precision.MONTH || precision == Precision.DAY) {

			resultBuilder.set(featureName + SUFFIX_QUARTER, ImmutableStringValue.class);

			resultBuilder.set(featureName + SUFFIX_MONTH_NUM, ImmutableIntegerValue.class);
			resultBuilder.set(featureName + SUFFIX_MONTH_NOM, ImmutableStringValue.class);

			resultBuilder.set(featureName + SUFFIX_YEAR_QUARTER, ImmutableStringValue.class);
			resultBuilder.set(featureName + SUFFIX_YEAR_MONTH, ImmutableStringValue.class);

		}

		if (precision == Precision.DAY) {
			resultBuilder.set(featureName + SUFFIX_WEEK_NUM, ImmutableIntegerValue.class);
			resultBuilder.set(featureName + SUFFIX_WEEK_NOM, ImmutableStringValue.class);

			resultBuilder.set(featureName + SUFFIX_DAY_OF_YEAR_NUM, ImmutableIntegerValue.class);
			resultBuilder.set(featureName + SUFFIX_DAY_OF_YEAR_NOM, ImmutableStringValue.class);

			resultBuilder.set(featureName + SUFFIX_DAY_OF_MONTH_NUM, ImmutableIntegerValue.class);
			resultBuilder.set(featureName + SUFFIX_DAY_OF_MONTH_NOM, ImmutableStringValue.class);

			resultBuilder.set(featureName + SUFFIX_DAY_OF_WEEK_NUM, ImmutableIntegerValue.class);
			resultBuilder.set(featureName + SUFFIX_DAY_OF_WEEK_NOM, ImmutableStringValue.class);

			resultBuilder.set(featureName + SUFFIX_WEEKEND, ImmutableBooleanValue.class);

			resultBuilder.set(featureName + SUFFIX_YEAR_WEEK, ImmutableStringValue.class);
		}


		return resultBuilder.create();
	}

//	public static void main(String[] args) {
//
//		Dataset trainAndValidationSet = Config.getJoinedTrain();
//
//		TrainTestSplit split = new PeopleIdSplit(trainAndValidationSet);
//		Dataset trainSet = split.getTrain();
//		Dataset validationSet = split.getTest();
//
//		trainSet = trainSet.transform(new DateParticleExtractor("people_date"));
//		trainSet = trainSet.transform(new DateParticleExtractor("activity_date"));
//		
//		validationSet = validationSet.transform(new DateParticleExtractor("people_date"));
//		validationSet = validationSet.transform(new DateParticleExtractor("activity_date"));
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
