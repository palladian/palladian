package ws.palladian.extraction.feature;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Locale;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDatasetFeatureVectorTransformer;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.value.ImmutableIntegerValue;
import ws.palladian.core.value.LocalDateValue;

public class DateDifferenceCalculator extends AbstractDatasetFeatureVectorTransformer {

	private final String featureName1;
	private final String featureName2;

	public DateDifferenceCalculator(String featureName1, String featureName2) {
		this.featureName1 = featureName1;
		this.featureName2 = featureName2;
	}

	@Override
	public FeatureVector compute(FeatureVector featureVector) {
		LocalDate date1 = ((LocalDateValue) featureVector.get(featureName1)).getLocalDate();
		LocalDate date2 = ((LocalDateValue) featureVector.get(featureName2)).getLocalDate();

		int differenceDays = (int) ChronoUnit.DAYS.between(date2, date1);
		int differenceWeeks = weeksDifference(date2, date1);
		int differenceWorkDays = workDaysDifference(date2, date1);

		InstanceBuilder builder = new InstanceBuilder().add(featureVector);
		builder.set(featureName1 + "_minus_" + featureName2 + "_days", differenceDays);
		builder.set(featureName1 + "_minus_" + featureName2 + "_weeks", differenceWeeks);
		builder.set(featureName1 + "_minus_" + featureName2 + "_businessDays", differenceWorkDays);
		return builder.create();
	}

	@Override
	public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
		FeatureInformationBuilder builder = new FeatureInformationBuilder().add(featureInformation);
		builder.set(featureName1 + "_minus_" + featureName2 + "_days", ImmutableIntegerValue.class);
		builder.set(featureName1 + "_minus_" + featureName2 + "_weeks", ImmutableIntegerValue.class);
		builder.set(featureName1 + "_minus_" + featureName2 + "_businessDays", ImmutableIntegerValue.class);
		return builder.create();
	}

	static int workDaysDifference(LocalDate d1, LocalDate d2) {
		int workDays = 0;
		if (d1.isBefore(d2)) {
			LocalDate temp = d1;
			d1 = d2;
			d2 = temp;
		}

		while (d2.isBefore(d1)) {
			d2 = d2.plusDays(1);
			if (d2.getDayOfWeek() != DayOfWeek.SATURDAY && d2.getDayOfWeek() != DayOfWeek.SUNDAY) {
				workDays++;
			}
		}
		return workDays;

	}

	static int weeksDifference(LocalDate d1, LocalDate d2) {
		if (d1.isBefore(d2)) {
			LocalDate temp = d1;
			d1 = d2;
			d2 = temp;
		}
		TemporalField weekOfYear = WeekFields.of(Locale.US).weekOfYear();

		int weekOfYear1 = d1.get(weekOfYear);
		int year1 = d1.getYear();

		int weekOfYear2 = d2.get(weekOfYear);
		int year2 = d2.getYear();

		if (weekOfYear1 == weekOfYear2 && year1 == year2) {
			return 0;
		}

		int weeks = 0;
		while (d2.isBefore(d1)) {
			d2 = d2.plus(1, ChronoUnit.WEEKS);
			weeks++;
		}
		return weeks;

	}

}
