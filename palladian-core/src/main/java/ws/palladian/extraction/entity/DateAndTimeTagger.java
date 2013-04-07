package ws.palladian.extraction.entity;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import ws.palladian.helper.collection.CollectionHelper;
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
	
	public List<Annotation> tagDateAndTime(String inputText) {
        return tagDateAndTime(inputText, ALL_DATES_WITH_YEARS);
    }

    public List<Annotation> tagDateAndTime(String inputText, DateFormat[] dateFormats) {

		List<Annotation> annotations = CollectionHelper.newArrayList();

        List<ExtractedDate> allDates = DateParser.findDates(inputText, dateFormats);
		
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
