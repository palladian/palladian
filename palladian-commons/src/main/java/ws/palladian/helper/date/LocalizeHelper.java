/**
 * 
 */
package ws.palladian.helper.date;

import java.util.Locale;
import java.util.TimeZone;

/**
 * This class helps to (temporarily) change the {@link Locale} or {@link TimeZone}.
 * FIXME No word about wtf this is necessary?
 * 
 * @author Sandro Reichert
 * 
 */
public class LocalizeHelper {

    /** Store default {@link Locale} to temporarily change it. */
    private static Locale vmLocale = Locale.getDefault();

    /** Store default {@link TimeZone} to temporarily change it. */
    private static TimeZone vmTimeZone = TimeZone.getDefault();

    /**
     * Make a backup of current {@link TimeZone} and set to {@code TimeZone.getTimeZone("Etc/UTC")}.
     * 
     * @see {@link #restoreTimeZone()} to restore to the default values.
     */
    public static void setTimeZoneUTC() {
        vmTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Etc/UTC"));
    }

    /**
     * Make a backup of current {@link Locale} and set to {@link Locale#ENGLISH}
     * 
     * @see {@link #restoreLocale()} to restore to the default values.
     */
    public static void setLocaleEnglish() {
        vmLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
    }

    /**
     * Backup current values and set UTC {@link TimeZone} and English {@link Locale}.
     * 
     * @see {@link #setLocaleEnglish()}
     * @see {@link #setTimeZoneUTC()}
     */
    public static void setUTCandEnglish() {
        setTimeZoneUTC();
        setLocaleEnglish();
    }

    /**
     * Restores the {@link TimeZone} to default value.
     * 
     * @see {@link #setTimeZoneUTC()} to make a backup of default value.
     */
    public static void restoreTimeZone() {
        TimeZone.setDefault(vmTimeZone);
    }

    /**
     * Restores the {@link Locale} to default value from backup.
     * 
     * @see {@link #setLocaleEnglish()} to make a backup of default value.
     */
    public static void restoreLocale() {
        Locale.setDefault(vmLocale);
    }

    /**
     * Restores the {@link Locale} and {@link TimeZone} to default values.
     * 
     * @see {@link #setUTCandEnglish()} to make a backup of default values.
     * @see {@link #restoreTimeZone()}
     * @see {@link #restoreLocale()}
     */
    public static void restoreTimeZoneAndLocale() {
        restoreTimeZone();
        restoreLocale();
    }

}
