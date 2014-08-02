package ws.palladian.extraction.entity;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.core.Tagger;
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
public class DateAndTimeTagger implements Tagger {

    /** The tag name for URLs. */
    public static final String DATETIME_TAG_NAME = "DATETIME";

    /** All date formats defined by default, plus additionally years in context. */
    private static final DateFormat[] ALL_DATES_WITH_YEARS = ArrayUtils.addAll(RegExp.ALL_DATE_FORMATS,
            RegExp.DATE_CONTEXT_YYYY);

    private final DateFormat[] dateFormats;

    public DateAndTimeTagger(DateFormat... dateFormats) {
        this.dateFormats = dateFormats;
    }

    public DateAndTimeTagger() {
        this(ALL_DATES_WITH_YEARS);
    }

    @Override
    public List<Annotation> getAnnotations(String text) {
        List<Annotation> annotations = CollectionHelper.newArrayList();

        List<ExtractedDate> allDates = DateParser.findDates(text, dateFormats);

        for (ExtractedDate dateTime : allDates) {

            // get the offset
            List<Integer> occurrenceIndices = StringHelper.getOccurrenceIndices(text, dateTime.getDateString());

            for (Integer index : occurrenceIndices) {
                annotations.add(new ImmutableAnnotation(index, dateTime.getDateString(), DATETIME_TAG_NAME));
            }
        }

        return annotations;
    }

}
