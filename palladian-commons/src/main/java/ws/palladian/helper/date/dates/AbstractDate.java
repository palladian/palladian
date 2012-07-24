package ws.palladian.helper.date.dates;

import java.util.Date;

public interface AbstractDate {

    static final int YEAR = 1;
    static final int MONTH = 2;
    static final int DAY = 3;
    static final int HOUR = 4;
    static final int MINUTE = 5;
    static final int SECOND = 6;
    static final int EXACTENESS = 7;

    /**
     * Constructs a normalized datestring in a format from YYYY-MM-DD HH:MM:SS to YYYY-MM depending of given values
     * 
     * @param time <code>true</code> to include time.
     * @return
     */
    String getNormalizedDate(boolean time);

    /**
     * Returns date values. <br>
     * To get a value use static date fields of this class.<br>
     * <br>
     * Only for date properties. For date-technique use getType(). <br>
     * Use this static fields to define a property.
     * 
     * @param field
     * @return
     */
    int get(int field);

    /**
     * Converts this extracted-date in a {@link Date}. <br>
     * Be careful, if a datepart is not given, it will be set to 0. (Except day will be set to 1). <br>
     * 
     * @return
     */
    Date getNormalizedDate();

    /**
     * Constructs a normalized datestring in a format from YYYY-MM-DD HH:MM:SS to YYYY-MM depending of given values
     * 
     * @param dateParts
     * @return
     */
    String getNormalizedDateString();
}
