package ws.palladian.extraction.date.dates;

import ws.palladian.helper.date.ExtractedDate;

public final class MetaDate extends KeywordDate {

    /** Name of the tag, where this {@link MetaDate} was found. */
    private final String tag;

    public MetaDate(ExtractedDate date, String keyword, String tag) {
        super(date, keyword);
        this.tag = tag;
    }

    public MetaDate(ExtractedDate date, String keyword) {
        this(date, keyword, null);
    }

    public MetaDate(ExtractedDate date) {
        this(date, null);
    }

    /**
     * Get the name of the tag, where this {@link MetaDate} was found.
     * 
     * @return Name of the tag, where this {@link MetaDate} was found.
     */
    public String getTag() {
        return tag;
    }

}
