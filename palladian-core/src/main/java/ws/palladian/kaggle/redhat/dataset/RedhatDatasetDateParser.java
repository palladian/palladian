package ws.palladian.kaggle.redhat.dataset;

import static java.util.Arrays.asList;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import ws.palladian.core.value.ImmutableDateValue;
import ws.palladian.core.value.Value;
import ws.palladian.core.value.io.AbstractValueParser;
import ws.palladian.core.value.io.ValueParserException;
import ws.palladian.kaggle.redhat.dataset.value.ImmutableLocalDateValue;
import ws.palladian.kaggle.redhat.dataset.value.LocalDateValue;

/**
 * Dedicated parser for reconstructing the (supposedly) correct dates.
 * 
 * @author pk
 */
public final class RedhatDatasetDateParser extends AbstractValueParser {

	/**
	 * UTC seems reasonable, as we're looking at logged data.
	 */
	public static final TimeZone TIME_ZONE = SimpleTimeZone.getTimeZone("UTC");

	/**
	 * Determined by looking at the data set, how to map back the from years
	 * 2022 and 2023 to reasonable values. Possible would be e.g. 2004, 2010,
	 * 2021, 2032, 2038, but it does not make sense to go to the future, so we
	 * go for 2010, which is an offset of -12.
	 */
	private static final int OFFSET = -12;

	public static final RedhatDatasetDateParser INSTANCE = new RedhatDatasetDateParser();

	private RedhatDatasetDateParser() {
		super(ImmutableDateValue.class);
	}

	@Override
	public Value parse(String input) throws ValueParserException {
		String[] split = input.split("-");
		int year = Integer.parseInt(split[0]) + OFFSET;
		int month = Integer.parseInt(split[1]);
		int day = Integer.parseInt(split[2]);
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTimeZone(TIME_ZONE);
		calendar.set(year, month - 1, day);
		LocalDateValue date = new ImmutableLocalDateValue(LocalDate.of(year, month, day));
		// System.out.println(date);
		return date;
	}
	
	
	/////////////
	
//	public static void main(String[] args) {
//		Dataset dataset = Config.getJoinedTrain();
//
//		Bag<String> datesCount = new Bag<>();
//		SortedSet<String> dates = new TreeSet<>();
//
//		for (Instance instance : dataset) {
//			String activityDate = instance.getVector().get("activity_date").toString();
//			dates.add(activityDate);
//			datesCount.add(activityDate);
//		}
//
//		// group them by modulus
//
//		int idx = 0;
//		Bag<Integer> byDay = new Bag<>();
//		for (String date : dates) {
//			System.out.println(idx + "\t" + datesCount.count(date) + "\t" + date);
//			byDay.add(idx, datesCount.count(date));
//			idx = (idx + 1) % 7;
//		}
//
//		CollectionHelper.print(byDay.toMap());
//
//	}
	
	static void tryDates() {
		List<String> datesS = asList("2022-07-18", "2022-09-19", "2022-10-10", "2022-10-31", "2023-02-20", "2023-04-17", "2023-07-31", "2023-08-21");
		// from studying the dataset, these should all map to sundays
		for (int offset = -20; offset < +20; offset++) {
			boolean result = test(datesS, offset);
			if (result) System.out.println("offset = " + offset + " year=" + (2022+offset));
		}
	}
	
	
	private static boolean test(List<String> strings, int offset) {
		for (String string : strings) {
			if (!test(string, offset)) {
				return false;
			}
		}
		return true;
	}

	private static boolean test(String string, int offset) {
		String[] split = string.split("-");
		int year = Integer.parseInt(split[0]) + offset;
		int month = Integer.parseInt(split[1]);
		int day = Integer.parseInt(split[2]);
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTimeZone(SimpleTimeZone.getTimeZone("UTC"));
		calendar.set(year, month - 1, day);
		return calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
	}

}
