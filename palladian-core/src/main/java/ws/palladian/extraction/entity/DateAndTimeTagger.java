package ws.palladian.extraction.entity;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import ws.palladian.helper.constants.DateFormat;
import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Tag dates and times in a text.
 * </p>
 * 
 * @author David Urbansky
 */
public class DateAndTimeTagger {

    /** The tag name for URLs. */
	public static final String DATETIME_TAG_NAME = "DATETIME";
	
	/** All date formats defined by default, plus additionally years in context. */
	private static final DateFormat[] ALL_DATES_WITH_YEARS = ArrayUtils.addAll(RegExp.ALL_DATE_FORMATS, RegExp.DATE_CONTEXT_YYYY);
	
	public Annotations tagDateAndTime(String inputText) {

		Annotations annotations = new Annotations();

        List<ExtractedDate> allDates = DateParser.findDates(inputText, ALL_DATES_WITH_YEARS);
		
		for (ExtractedDate dateTime : allDates) {
			
			// get the offset
			List<Integer> occurrenceIndices = StringHelper.getOccurrenceIndices(inputText, dateTime.getDateString());
			
			for (Integer integer : occurrenceIndices) {
				Annotation annotation = new Annotation(integer,dateTime.getDateString(),DATETIME_TAG_NAME);
				annotations.add(annotation);
			}
			
		}

		return annotations;
	}

}
