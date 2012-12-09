package ws.palladian.extraction.date.dates;

import ws.palladian.helper.date.ExtractedDate;

public final class MetaDate extends KeywordDate {

    public MetaDate(ExtractedDate date, String keyword) {
        super(date, keyword);
    }

    public MetaDate(ExtractedDate date) {
        this(date, null);
    }

}
