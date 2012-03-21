package ws.palladian.extraction.entity;

import java.util.List;

import ws.palladian.extraction.date.DateGetterHelper;
import ws.palladian.extraction.date.dates.ContentDate;
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
	
	public Annotations tagDateAndTime(String inputText) {

		Annotations annotations = new Annotations();

        List<ContentDate> allDates = DateGetterHelper.findAllDates(inputText, true);
		
		for (ContentDate dateTime : allDates) {
			
			// get the offset
			List<Integer> occurrenceIndices = StringHelper.getOccurrenceIndices(inputText, dateTime.getDateString());
			
			for (Integer integer : occurrenceIndices) {
				Annotation annotation = new Annotation(integer,dateTime.getDateString(),DATETIME_TAG_NAME,annotations);
				annotations.add(annotation);
			}
			
		}

		return annotations;
	}

}
