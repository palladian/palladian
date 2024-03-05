package ws.palladian.extraction.date;

import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.extraction.entity.DateAndTimeTagger;
import ws.palladian.helper.date.ExtractedDate;

/**
 * A date annotation.
 *
 * @author David Urbansky
 */
public class DateAnnotation extends ImmutableAnnotation {
    private final ExtractedDate extractedDate;

    public DateAnnotation(int startPosition, String value, ExtractedDate extractedDate) {
        super(startPosition, value, DateAndTimeTagger.DATETIME_TAG_NAME);
        this.extractedDate = extractedDate;
    }

    public ExtractedDate getExtractedDate() {
        return extractedDate;
    }
}
