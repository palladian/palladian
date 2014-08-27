package ws.palladian.helper.normalization;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.StringLengthComparator;
import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.constants.UnitType;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * The UnitNormalizer normalizes units.
 * </p>
 * 
 * @author David Urbansky
 */
public class UnitNormalizer {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(UnitNormalizer.class);

    private final static List<String> ALL_UNITS = CollectionHelper.newArrayList();

    static {
        for (UnitType unitType : UnitType.values()) {
            ALL_UNITS.addAll(unitType.getUnitNames());
        }
        Collections.sort(ALL_UNITS, new StringLengthComparator());
    }

    private static boolean isTimeUnit(String unit) {
        return UnitType.TIME.contains(unit);
    }

    private static boolean isDigitalUnit(String unit) {
        return UnitType.DIGITAL.contains(unit);
    }

    private static boolean isFrequencyUnit(String unit) {
        return UnitType.FREQUENCY.contains(unit);
    }

    private static boolean isLengthUnit(String unit) {
        return UnitType.LENGTH.contains(unit);
    }

    private static boolean isWeightUnit(String unit) {
        return UnitType.WEIGHT.contains(unit);
    }

    private static boolean isAreaUnit(String unit) {
        return UnitType.AREA.contains(unit);
    }

    private static boolean isVolumeUnit(String unit) {
        return UnitType.VOLUME.contains(unit);
    }

    private static boolean isSoundVolumeUnit(String unit) {
        return UnitType.POWER_RATIO.contains(unit);
    }

    private static boolean isTemperatureUnit(String unit) {
        return UnitType.TEMPERATURE.contains(unit);
    }

    public static String detectUnit(String text) {
        for (String unit : ALL_UNITS) {
            if (Pattern.compile("(?<=\\d|\\s|^)" + unit + "(?=$|-|\\s)").matcher(text).find()) {
                return unit;
            }
        }

        return null;
    }

    public static String detectUnit(String text, UnitType unitType) {
        for (String unit : unitType.getUnitNames()) {
            if (Pattern.compile("(?<=\\d|\\s|^)" + unit + "(?=$|\\s)").matcher(text).find()) {
                return unit;
            }
        }
        return null;
    }

    /**
     * <p>
     * Return a collection of units that are of the same type, e.g. if "cm" is given, all other length units are
     * returned.
     * </p>
     * 
     * @param unit The input unit.
     * @return A collection of units of the same type.
     */
    public static Collection<String> getAllUnitsOfSameType(String unit) {

        if (isDigitalUnit(unit)) {
            return UnitType.DIGITAL.getUnitNames();
        }
        if (isTimeUnit(unit)) {
            return UnitType.TIME.getUnitNames();
        }
        if (isFrequencyUnit(unit)) {
            return UnitType.FREQUENCY.getUnitNames();
        }
        if (isLengthUnit(unit)) {
            return UnitType.LENGTH.getUnitNames();
        }
        if (isWeightUnit(unit)) {
            return UnitType.WEIGHT.getUnitNames();
        }
        if (isAreaUnit(unit)) {
            return UnitType.AREA.getUnitNames();
        }
        if (isVolumeUnit(unit)) {
            return UnitType.VOLUME.getUnitNames();
        }
        if (isSoundVolumeUnit(unit)) {
            return UnitType.POWER_RATIO.getUnitNames();
        }
        if (isTemperatureUnit(unit)) {
            return UnitType.TEMPERATURE.getUnitNames();
        }

        return CollectionHelper.newHashSet();
    }

    /**
     * <p>
     * Returns true if unitB is bigger than units. e.g. hours > minutes and GB > MB
     * </p>
     * 
     * @param unitB The bigger unit.
     * @param unitS The smaller unit.
     * @return True if unitB is bigger than unitS.
     */
    public static boolean isBigger(String unitB, String unitS) {
        return unitLookup(unitB) > unitLookup(unitS);
    }

    /**
     * <p>
     * Returns true if units are the same unit type (time,distance etc.). e.g. MB and GB are digital size, hours and
     * minutes are time units.
     * </p>
     * 
     * @param unit1 The first unit.
     * @param unit2 The second unit.
     * @return True if both units are the same type.
     */
    public static boolean unitsSameType(String unit1, String unit2) {

        unit1 = unit1.toLowerCase().trim();
        unit2 = unit2.toLowerCase().trim();

        // check time units
        if (isTimeUnit(unit1) && isTimeUnit(unit2)) {
            return true;
        }

        // digital units
        if (isDigitalUnit(unit1) && isDigitalUnit(unit2)) {
            return true;
        }

        // frequency units
        if (isFrequencyUnit(unit1) && isFrequencyUnit(unit2)) {
            return true;
        }

        // distances
        if (isLengthUnit(unit1) && isLengthUnit(unit2)) {
            return true;
        }

        // weight
        if (isWeightUnit(unit1) && isWeightUnit(unit2)) {
            return true;
        }

        // area
        if (isAreaUnit(unit1) && isAreaUnit(unit2)) {
            return true;
        }

        // volume
        if (isVolumeUnit(unit1) && isVolumeUnit(unit2)) {
            return true;
        }

        // volume
        if (isSoundVolumeUnit(unit1) && isSoundVolumeUnit(unit2)) {
            return true;
        }

        // temperature
        if (isTemperatureUnit(unit1) && isTemperatureUnit(unit2)) {
            return true;
        }

        return false;
    }

    /**
     * <p>
     * </p>
     * 
     * @param unit The source unit.
     * @return The unit to which all values are normalized to, e.g. "second" for time units.
     */
    public static String findBaseUnit(String unit) {

        unit = unit.toLowerCase();
        Collection<String> allUnitsOfSameType = getAllUnitsOfSameType(unit);
        for (String unitType : allUnitsOfSameType) {
            double multiplier = unitLookup(unitType);
            if (multiplier == 1.) {
                return unitType;
            }
        }

        return null;
    }

    public static double unitLookup(String unit) {

        unit = unit.toLowerCase().trim();
        if (unit.endsWith(".")) {
            unit = unit.substring(0, unit.length() - 1);
        }

        // -1 means no multiplier found (hint for other function that a shorter sequence for the unit string might bring
        // a result)
        double multiplier = -1.0;

        ol: for (UnitType unitType : UnitType.values()) {
            for (Pair<List<String>, Double> pair : unitType.getUnits()) {
                for (String unitTypeUnit : pair.getValue0()) {
                    if (unit.equals(unitTypeUnit)) {
                        if (pair.getValue1() == null) {
                            multiplier = -1.0;
                        } else {
                            multiplier = pair.getValue1();
                        }
                        break ol;
                    }
                }
            }

        }

        return multiplier;
    }

    /**
     * <p>
     * Find special formats for combined values (well formed as "1 min 4 sec" are handled by getNormalizedNumber).
     * </p>
     * 
     * <pre>
     * 1m20s => 80s
     * 1h2m20s => 3740s (1m:20s => 80s)
     * 00:01:20 => 80s
     * 1:20 => 80s
     * 5'9" => 175.26cm
     * 5'9'' => 175.26cm
     * </pre>
     * 
     * @param number The number.
     * @param unitText The text after the unit.
     * @return The combined value or -1 if number is not part of special format.
     */
    public static double handleSpecialFormat(double number, String unitText, int decimals) {
        double combinedValue = -1.0;

        try {

            Pattern pattern;
            Matcher matcher;

            // 1m20s type
            pattern = Pattern.compile("\\Am(\\s)?(\\d)+s");
            matcher = pattern.matcher(unitText);
            if (matcher.find()) {
                combinedValue = number * 60; // minutes to seconds
                combinedValue += Double.valueOf(matcher.group().substring(1, matcher.end() - 1));
                return MathHelper.round(combinedValue, decimals);
            }

            // 1h2m20s, 1h2m type
            pattern = Pattern.compile("\\Ah(\\s)?(\\d)+m(\\s)?((\\d)+s)?");
            matcher = pattern.matcher(unitText);
            if (matcher.find()) {
                combinedValue = number * 3600; // hours to seconds
                int minutesIndex = unitText.indexOf("m");
                combinedValue += Double.valueOf(matcher.group().substring(1, minutesIndex)) * 60; // minutes to seconds
                int secondsIndex = unitText.indexOf("s");
                if (secondsIndex > -1) {
                    combinedValue += Double.valueOf(matcher.group().substring(minutesIndex + 1, secondsIndex));
                }
                return MathHelper.round(combinedValue, decimals);
            }

            // 01:01:20 type
            pattern = Pattern.compile("\\A:(\\d)+:(\\d)+");
            matcher = pattern.matcher(unitText);
            if (matcher.find()) {
                combinedValue = number * 3600; // hours to seconds
                int lastColonIndex = matcher.group().lastIndexOf(":");
                combinedValue += Double.valueOf(matcher.group().substring(1, lastColonIndex)) * 60; // minutes to
                // seconds
                combinedValue += Double.valueOf(matcher.group().substring(lastColonIndex + 1, matcher.end()));
                return MathHelper.round(combinedValue, decimals);
            }

            // 01:20 type
            pattern = Pattern.compile("\\A:(\\d)+");
            matcher = pattern.matcher(unitText);
            if (matcher.find()) {
                combinedValue = number * 60; // minutes to seconds
                combinedValue += Double.valueOf(matcher.group().substring(1, matcher.end()));
                return MathHelper.round(combinedValue, decimals);
            }

            // 5'9" / 5' 9" type
            pattern = Pattern.compile("\\A'(\\s)?(\\d)+\"");
            matcher = pattern.matcher(unitText);
            if (matcher.find()) {
                combinedValue = number * unitLookup("ft"); // feet to centimeters
                combinedValue += Double.valueOf(matcher.group().substring(1, matcher.end() - 1).trim())
                        * unitLookup("in"); // inches to centimeters
                return MathHelper.round(combinedValue, decimals);
            }

            // 5'9'' / 5'9'' type
            pattern = Pattern.compile("\\A'(\\s)?(\\d)+''");
            matcher = pattern.matcher(unitText);
            if (matcher.find()) {
                combinedValue = number * unitLookup("ft"); // feet to centimeters
                combinedValue += Double.valueOf(matcher.group().substring(1, matcher.end() - 2).trim())
                        * unitLookup("in"); // inches to centimeters
                return MathHelper.round(combinedValue, decimals);
            }

            // per thousand, per 1000 type
            pattern = Pattern.compile("(\\Aper thousand)|(\\Aper 1000)");
            matcher = pattern.matcher(unitText);
            if (matcher.find()) {
                combinedValue = number / 10; // to percent
                return MathHelper.round(combinedValue, decimals);
            }
        } catch (StringIndexOutOfBoundsException e) {
            LOGGER.error(unitText, e);
        }

        return -1.0;
    }

    /**
     * <p>
     * Transforms a normalized value to the target unit.
     * </p>
     * 
     * @param unitTo The unit to transform.
     * @param value The value to transform.
     * @return The transformed value.
     */
    public static double transorm(String unitTo, double value) {
        double divider = unitLookup(unitTo);
        if (divider != -1) {
            return value / divider;
        } else {
            return value;
        }
    }

    public static double transorm(String unitTo, String value) {
        return transorm(unitTo, Double.valueOf(value));
    }

    public static UnitType getUnitType(String string) {
        String words[] = string.split(" ");
        UnitType unitType = UnitType.NONE;

        for (String word2 : words) {
            String word = word2.toLowerCase();
            if (isTimeUnit(word)) {
                unitType = UnitType.TIME;
            }
            if (isDigitalUnit(word)) {
                unitType = UnitType.DIGITAL;
            }
            if (isFrequencyUnit(word)) {
                unitType = UnitType.FREQUENCY;
            }
            if (isLengthUnit(word)) {
                unitType = UnitType.LENGTH;
            }
            if (isWeightUnit(word)) {
                unitType = UnitType.WEIGHT;
            }
            if (isVolumeUnit(word)) {
                unitType = UnitType.VOLUME;
            }
            if (isTemperatureUnit(word)) {
                unitType = UnitType.TEMPERATURE;
            }
            if (unitType != UnitType.NONE) {
                break; // we found a unit
            }
        }
        return unitType;
    }

    public static double getNormalizedNumber(String unitText) throws NumberFormatException, NullPointerException {

        // add space in case it's missing "2.4Ghz" => "2.4 Ghz"
        unitText = unitText.replaceAll("(\\d)([A-Za-z\"])", "$1 $2").trim();
        String words[] = unitText.split(" ");

        if (words.length == 0) {
            words = unitText.trim().split("(?<=[0-9])(?=\\w)");
        }

        double number = Double.parseDouble(words[0]);

        String newUnitText = "";
        for (int i = 1; i < words.length; i++) {
            newUnitText += words[i] + " ";
        }
        return getNormalizedNumber(number, newUnitText.trim(), 3, "");
    }

    public static double getNormalizedNumber(double number, String unitText) {
        return getNormalizedNumber(number, unitText, 3, "");
    }

    private static double getNormalizedNumber(double number, String unitText, String combinedSearchPreviousUnit) {
        return getNormalizedNumber(number, unitText, 3, combinedSearchPreviousUnit);
    }

    public static double getNormalizedNumber(double number, String unitText, int decimals,
            String combinedSearchPreviousUnit) {

        boolean combinedSearch = false;
        if (combinedSearchPreviousUnit.length() > 0) {
            combinedSearch = true;
        }

        // test first whether number is part of a special format
        double specialFormatOutcome = handleSpecialFormat(number, StringHelper.trim(unitText, ":'\""), decimals);
        if (specialFormatOutcome != -1.0) {
            return MathHelper.round(specialFormatOutcome, decimals);
        }

        // trim again, delete also ":" this time
        if (!unitText.equals("\"") && !unitText.equals("''")) {
            unitText = StringHelper.trim(unitText);
        }

        // some units are presented in optional plural form e.g. 5 hour(s) but some values are in brackets e.g.
        // (2.26GHz), decide here whether to delete closing
        // bracket or not
        if (!unitText.endsWith("(s)") && unitText.endsWith(")")) {
            unitText = unitText.substring(0, unitText.length() - 1);
        }

        String words[] = unitText.split(" ");

        int l = words.length;
        double multiplier = 1.0;
        String restWordSequence = ""; // keep the rest word sequence to check for combined values
        String wordSequence = "";
        while (l > 0) {
            wordSequence = "";
            restWordSequence = "";
            for (int i = 0; i < l; i++) {
                if (words[i].equals("\"") || words[i].equals("''")) {
                    wordSequence += " " + words[i];
                } else {
                    wordSequence += " " + StringHelper.trim(words[i]);
                }
            }
            for (int i = l; i < words.length; ++i) {
                // System.out.println("add ");
                if (words[i].equals("\"") || words[i].equals("''")) {
                    restWordSequence += " " + words[i];
                } else {
                    restWordSequence += " " + StringHelper.trim(words[i]);
                }
            }
            // System.out.println("current word sequence "+wordSequence);
            // Logger.getInstance().log("current word sequence "+wordSequence, false);
            multiplier = unitLookup(wordSequence);
            if (multiplier != -1.0) {
                // when a subsequent unit is searched is has to be smaller than the previous one
                // e.g. 1 hour 23 minutes (minutes < hour) otherwise 2GB 80GB causes problems
                if (combinedSearch
                        && !(unitsSameType(combinedSearchPreviousUnit, wordSequence) && isBigger(
                                combinedSearchPreviousUnit, wordSequence))) {
                    return 0.0;
                }
                break;
            }
            l--;
        }

        if (multiplier < 0 && !combinedSearch) {
            // no unit found, do not change value of number
            multiplier = 1.0;
        } else if (multiplier < 0) {
            multiplier = 0.0;
        }

        number *= multiplier;

        // keep searching in unit text for combined values as:
        // 1 hour 52 minutes
        // 1 min 20 sec
        // 5 ft 9 in
        // because of trimming RAM: 2GB - 80GB HDD becomes 2GB 80GB
        // second unit must be same type (time, distance etc.) and smaller
        restWordSequence = restWordSequence.trim();
        Pattern pat = Pattern.compile("\\A" + RegExp.NUMBER);
        Matcher m = pat.matcher(restWordSequence);

        m.region(0, restWordSequence.length());

        try {
            if (m.find()) {
                number += getNormalizedNumber(Double.valueOf(StringNormalizer.normalizeNumber(m.group())),
                        restWordSequence.substring(m.end()), wordSequence);
            }
        } catch (NumberFormatException e) {
            LOGGER.error(m.group(), e);
        }

        return MathHelper.round(number, decimals);
    }

    /**
     * <p>
     * Transforms a given <b>normalized</b> value and transforms it to the most readable unit for its unit type. E.g.
     * "0.5" with LENGTH will become "5mm".
     * </p>
     * 
     * @param normalizedValue The value, normalized to its base value in its unit type.
     * @param unitType The unit type of the normalized value.
     * @return A pair with the transformed value and the used unit.
     */
    public static Pair<Double, List<String>> smartTransform(Double normalizedValue, UnitType unitType) {

        double smallestReadableValue = normalizedValue;
        Pair<List<String>, Double> bestMatchingTransformation = null;
        for (Pair<List<String>, Double> entry : unitType.getUnits()) {

            double transformed = normalizedValue / entry.getValue1();
            if ((transformed < smallestReadableValue && transformed > 1)
                    || (transformed > smallestReadableValue && smallestReadableValue < 1)
                    || bestMatchingTransformation == null) {
                bestMatchingTransformation = entry;
                smallestReadableValue = transformed;
            }

        }

        Pair<Double, List<String>> smartTransformationResult = new Pair<Double, List<String>>(smallestReadableValue,
                bestMatchingTransformation.getValue0());

        return smartTransformationResult;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        System.out.println(getNormalizedNumber(6, "ft 1.5 in 187 cm"));

        System.out.println(getUnitType("2.26 GHz"));
        // System.out.println(getNormalizedNumber("a 2.26 GHz"));
        System.out.println(getUnitType("21.4 million"));
        System.out.println(getNormalizedNumber("21.4 million"));
        System.out.println(getUnitType("2 hr. 32 min"));
        System.out.println(getNormalizedNumber("2 hr. 32 min"));
        System.out.println(getUnitType("3 ft 9 inches"));
        System.out.println(getNormalizedNumber("3 ft 9 inches"));

        System.out.println(getNormalizedNumber(2.26, "GHz)"));
        System.out.println(getNormalizedNumber(21.4, " million.[1]"));
        System.out.println(getNormalizedNumber(13, " per thousand asdf asdfisdf "));
        System.out.println(getNormalizedNumber(13, " per thousand. asdf asdfisdf "));
        System.out.println(getNormalizedNumber(13, " per 1000 asdf asdfisdf "));
        System.out.println(getNormalizedNumber(1.6, " GHz, 1024MB RAM"));
        System.out.println(getNormalizedNumber(1.6, "GHz, 1066MHz Front side bus"));

        // test combined search
        System.out.println(getNormalizedNumber(80, "'GB'))"));
        System.out.println(getNormalizedNumber(2, " hr. 32 min."));
        System.out.println(getNormalizedNumber(13.3, "\" adf fs"));
        System.out.println(getNormalizedNumber(6, "' 2''"));
        System.out.println(getNormalizedNumber(6, "'2\""));
        System.out.println(getNormalizedNumber(5, "hours 4 minutes 6seconds"));
        System.out.println(getNormalizedNumber(6, " h 30 min"));
        System.out.println(getNormalizedNumber(5, "ft 9 inches"));
        System.out.println(getNormalizedNumber(5, "\""));
        System.out.println(getNormalizedNumber(2, "mb 4 GB"));
        System.out.println(getNormalizedNumber(2, "mb 2mb"));
        System.out.println(getNormalizedNumber(2, "mb 100kb"));

        // types and sizes
        System.out.println(unitsSameType("gb", "mb"));
        System.out.println(unitsSameType("minute", "mb"));
        System.out.println(isBigger("minute", "second"));

        // test special format
        System.out.println(String.valueOf(getNormalizedNumber(Double.valueOf("6"), "' 2'',")));
        System.out.println(handleSpecialFormat(6.0, "' 2'',", 3));
        System.out.println(handleSpecialFormat(5, "' 9''", 3));
        System.out.println(handleSpecialFormat(5, "'9''", 3));
        System.out.println(handleSpecialFormat(5, "' 9\"", 3));
        System.out.println(handleSpecialFormat(5, "'9\"", 3));
        System.out.println(handleSpecialFormat(0, ":59", 3));
        System.out.println(handleSpecialFormat(2, ":44", 3));
        System.out.println(handleSpecialFormat(4, ":2:40", 3));
        System.out.println(handleSpecialFormat(4, ":02:40", 3));
        System.out.println(handleSpecialFormat(4, ":20:40", 3));
        System.out.println(handleSpecialFormat(1, "h 20m 40s", 3));
        System.out.println(handleSpecialFormat(1, "h 20m", 3));
        System.out.println(handleSpecialFormat(2, "m 40s", 3));
        System.out.println(handleSpecialFormat(1, "h20m40s", 3));
        System.out.println(handleSpecialFormat(1, "h20m", 3));
        System.out.println(handleSpecialFormat(2, "m40s", 3));

        // test round
        System.out.println(MathHelper.round(0.2344223, 4));

        // test unit normalization
        System.out.println(getNormalizedNumber(5, "mpixel"));
        System.out.println(getNormalizedNumber(2, "megapixels"));
        System.out.println(getNormalizedNumber(30, "miles per hour is really fast"));
        System.out.println(getNormalizedNumber(20, "m kilometers"));
        System.out.println(getNormalizedNumber(53.4, "million, compared to"));
        System.out.println(getNormalizedNumber(125, "ft-lbs torque!!!"));
        System.out.println(getNormalizedNumber(125, "lb-ft torque, and power speed"));
        System.out.println(getNormalizedNumber(125, ""));
        System.out.println(getNormalizedNumber(1, "min 20s 23sdf sdf a__:"));
        System.out.println(getNormalizedNumber(1, "hour 30 minutes 20sdf"));
        System.out.println(getNormalizedNumber(5, "ft 9 in 20sdf"));
        System.out.println(getNormalizedNumber(1, "m20s 23sdf sdf a__:"));
        System.out.println(getNormalizedNumber(1, ":20 23sdf sdf a__:"));
        System.out.println(getNormalizedNumber(1, ":20 23sdf sdf a__:"));

        System.out.println(getNormalizedNumber(20, "inch"));
        System.out.println(transorm("inch", getNormalizedNumber(20, "inch")));

        // System.out.println(Double.valueOf("8.589934592E9")/100000);

        // Locale.setDefault(Locale.ENGLISH);
        // DecimalFormat formatter = new DecimalFormat("#.###");
        // System.out.println(formatter.format(Double.valueOf("8.589934592E3")));
        // System.out.println(formatter.format(Double.valueOf("8.589934592E12")));
        // System.out.println(formatter.format(Double.valueOf("8.589934592")));
        //
        //
        // String factString = "16";
        // String unitText = "GB sadf asdf";
        // // normalize units when given
        // if (factString.length() > 0) {
        // try {
        // factString = String.valueOf(MathHelper.getNormalizedNumber(Double.valueOf(factString),unitText));
        // System.out.println(factString);
        // // make it a normalized string again (no .0)
        // factString = StringHelper.normalizeNumber(factString);
        // System.out.println("number after unit normalization "+factString);
        //
        // } catch (NumberFormatException e) {
        // e.printStackTrace();
        // }
        // }
    }

}
