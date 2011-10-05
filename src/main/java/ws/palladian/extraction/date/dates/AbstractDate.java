package ws.palladian.extraction.date.dates;

import java.util.Date;

import ws.palladian.extraction.date.ExtractedDateHelper;

public abstract class AbstractDate {
	
	/**
     * Returns value representing this type of date.<br>
     * Or use getTypeToString of {@link ExtractedDateHelper} to get this type in words.
     * 
     * @return Integer of this type.
     */
    public abstract DateType getType();
    
    /**
     * Returns int value representing this type of date.<br>
     * Returning values are equal to this static TECH_ fields. <br>
     * Or use getTypeToString of {@link ExtractedDateHelper} to get this type in words.
     * 
     * @return Integer of this type.
     */
    public abstract int getTypeInt();

	public abstract String getNormalizedDate(boolean b);

	public abstract int get(int year);

	public abstract Date getNormalizedDate();

	public abstract String getNormalizedDateString();
}
