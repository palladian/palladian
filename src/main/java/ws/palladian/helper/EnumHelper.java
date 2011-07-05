package ws.palladian.helper;

import org.apache.log4j.Logger;

public class EnumHelper {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(EnumHelper.class);

    /**
     * A common method to get an enum from string, e.g. when loaded from database.
     * Works for all enums since they can't have another base class
     * taken from http://stackoverflow.com/questions/604424/java-enum-converting-string-to-enum
     * 
     * <p>
     * usage in MyEnum:<br />
     * public static MyEnum fromString(String name) {<br />
     * return getEnumFromString(MyEnum.class, name);<br />
     * }
     * </p>
     * 
     * @param <T> Enum type
     * @param theClass enum type. All enums must be all capitalized!
     * @param string case insensitive
     * @return corresponding enum, or <code>null</code> if string was <code>null</code> or not representing a valid enum
     *         of this class.
     */
    public static <T extends Enum<T>> T getEnumFromString(Class<T> theClass, String string) {
        if (theClass != null && string != null) {
            try {
                return Enum.valueOf(theClass, string.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                LOGGER.error("Could not get enum " + theClass + " from string " + string);
            }
        }
        return null;
    }

}
