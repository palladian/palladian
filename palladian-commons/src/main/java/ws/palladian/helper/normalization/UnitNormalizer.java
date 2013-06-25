package ws.palladian.helper.normalization;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.constants.RegExp;
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

    public static final int UNIT_UNITLESS = 0;
    public static final int UNIT_TIME = 1;
    public static final int UNIT_DIGITAL = 2;
    public static final int UNIT_FREQUENCY = 3;
    public static final int UNIT_LENGTH = 4;
    public static final int UNIT_WEIGHT = 5;

    private static boolean isTimeUnit(String unit) {
        HashSet<String> timeUnits = new HashSet<String>();
        timeUnits.add("year");
        timeUnits.add("years");
        timeUnits.add("month");
        timeUnits.add("months");
        timeUnits.add("week");
        timeUnits.add("weeks");
        timeUnits.add("day");
        timeUnits.add("days");
        timeUnits.add("day(s)");
        timeUnits.add("hour");
        timeUnits.add("hours");
        timeUnits.add("hour(s)");
        timeUnits.add("hrs");
        timeUnits.add("hr");
        timeUnits.add("h");
        timeUnits.add("minute");
        timeUnits.add("minutes");
        timeUnits.add("min");
        timeUnits.add("second");
        timeUnits.add("seconds");
        timeUnits.add("secs");
        timeUnits.add("sec");
        timeUnits.add("s");
        timeUnits.add("milli seconds");
        timeUnits.add("milliseconds");
        timeUnits.add("ms");
        timeUnits.add("years");

        return timeUnits.contains(unit);
    }

    private static boolean isDigitalUnit(String unit) {
        HashSet<String> digitalUnits = new HashSet<String>();
        digitalUnits.add("terra bytes");
        digitalUnits.add("terra byte");
        digitalUnits.add("tb");
        digitalUnits.add("giga bytes");
        digitalUnits.add("giga byte");
        digitalUnits.add("gb");
        digitalUnits.add("mega bytes");
        digitalUnits.add("mega byte");
        digitalUnits.add("mb");
        digitalUnits.add("kilo bytes");
        digitalUnits.add("kilo byte");
        digitalUnits.add("kilobyte");
        digitalUnits.add("kb");
        digitalUnits.add("kbytes");
        digitalUnits.add("kbyte");
        digitalUnits.add("bytes");
        digitalUnits.add("byte");
        digitalUnits.add("b");
        digitalUnits.add("bit");
        digitalUnits.add("bits");

        return digitalUnits.contains(unit);
    }

    private static boolean isFrequencyUnit(String unit) {
        HashSet<String> frequencyUnits = new HashSet<String>();
        frequencyUnits.add("terrahertz");
        frequencyUnits.add("thz");
        frequencyUnits.add("terra hertz");
        frequencyUnits.add("gigahertz");
        frequencyUnits.add("ghz");
        frequencyUnits.add("giga hertz");
        frequencyUnits.add("megahertz");
        frequencyUnits.add("mhz");
        frequencyUnits.add("mega hertz");
        frequencyUnits.add("kilohertz");
        frequencyUnits.add("khz");
        frequencyUnits.add("kilo hertz");
        frequencyUnits.add("hertz");
        frequencyUnits.add("hz");

        return frequencyUnits.contains(unit);
    }

    private static boolean isLengthUnit(String unit) {
        HashSet<String> lengthUnits = new HashSet<String>();
        lengthUnits.add("km");
        lengthUnits.add("kms");
        lengthUnits.add("kilometer");
        lengthUnits.add("kilometers");
        lengthUnits.add("kilometre");
        lengthUnits.add("kilometres");
        lengthUnits.add("mile");
        lengthUnits.add("miles");
        lengthUnits.add("mi");
        lengthUnits.add("meter");
        lengthUnits.add("meters");
        lengthUnits.add("metre");
        lengthUnits.add("metres");
        lengthUnits.add("m");
        lengthUnits.add("decimeter");
        lengthUnits.add("decimeters");
        lengthUnits.add("decimetre");
        lengthUnits.add("decimetres");
        lengthUnits.add("dm");
        lengthUnits.add("foot");
        lengthUnits.add("feet");
        lengthUnits.add("ft");
        lengthUnits.add("in");
        lengthUnits.add("inch");
        lengthUnits.add("inches");
        lengthUnits.add("\"");
        lengthUnits.add("centimeter");
        lengthUnits.add("centimeters");
        lengthUnits.add("centimetre");
        lengthUnits.add("centimetres");
        lengthUnits.add("cm");
        lengthUnits.add("millimeter");
        lengthUnits.add("millimeters");
        lengthUnits.add("millimetre");
        lengthUnits.add("millimetres");
        lengthUnits.add("mm");

        return lengthUnits.contains(unit);
    }

    private static boolean isWeightUnit(String unit) {
        HashSet<String> weightUnits = new HashSet<String>();
        weightUnits.add("ton");
        weightUnits.add("tons");
        weightUnits.add("kilograms");
        weightUnits.add("kilogram");
        weightUnits.add("kg");
        weightUnits.add("kgs");
        weightUnits.add("pound");
        weightUnits.add("pounds");
        weightUnits.add("lbs");
        weightUnits.add("ounce");
        weightUnits.add("ounces");
        weightUnits.add("oz");
        weightUnits.add("ozs");
        weightUnits.add("gram");
        weightUnits.add("grams");
        weightUnits.add("g");
        weightUnits.add("gr");

        return weightUnits.contains(unit);
    }

    private static boolean isVolumeUnit(String unit) {
        HashSet<String> volumeUnits = new HashSet<String>();
        volumeUnits.add("gal");
        volumeUnits.add("gallon");
        volumeUnits.add("gallons");
        volumeUnits.add("pint");
        volumeUnits.add("pints");
        volumeUnits.add("cups");
        volumeUnits.add("cup");
        volumeUnits.add("cp");
        volumeUnits.add("c");
        volumeUnits.add("teaspoons");
        volumeUnits.add("teaspoon");
        volumeUnits.add("tsps");
        volumeUnits.add("tsp");
        volumeUnits.add("t");
        volumeUnits.add("tablespoons");
        volumeUnits.add("tablespoon");
        volumeUnits.add("tbsps");
        volumeUnits.add("tbsp");
        volumeUnits.add("T");
        volumeUnits.add("quart");
        volumeUnits.add("quarts");
        volumeUnits.add("qt");
        volumeUnits.add("qts");
        volumeUnits.add("liter");
        volumeUnits.add("liters");
        volumeUnits.add("l");
        volumeUnits.add("milliliter");
        volumeUnits.add("milliliters");
        volumeUnits.add("ml");
        volumeUnits.add("fl oz");
        volumeUnits.add("fl oz.");
        volumeUnits.add("fl ozs");
        volumeUnits.add("fl ozs.");
        volumeUnits.add("fl ounce");
        volumeUnits.add("fl ounces");

        return volumeUnits.contains(unit);
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

        // volume
        if (isVolumeUnit(unit1) && isVolumeUnit(unit2)) {
            return true;
        }

        return false;
        // TODO more
    }

    public static double unitLookup(String unit) {

        String origUnit = unit;
        unit = unit.toLowerCase().trim();
        if (unit.endsWith(".")) {
            unit = unit.substring(0, unit.length() - 1);
        }

        // means no multiplier found (hint for other function that a shorter sequence for the unit string might bring a
        // result)
        double multiplier = -1.0;

        // TODO more units (Mhz, Ghz..., volumes)

        // time units, all to seconds
        if (unit.equals("year") || unit.equals("years")) {
            multiplier = 31536000.0;
        } else if (unit.equals("month") || unit.equals("months")) {
            multiplier = 2592000.0;
        } else if (unit.equals("week") || unit.equals("weeks")) {
            multiplier = 604800.0;
        } else if (unit.equals("day") || unit.equals("days") || unit.equals("day(s)")) {
            multiplier = 86400.0;
        } else if (unit.equals("hour") || unit.equals("hours") || unit.equals("hour(s)") || unit.equals("hrs")
                || unit.equals("hr") || unit.equals("h")) {
            multiplier = 3600.0;
        } else if (unit.equals("minute") || unit.equals("minutes") || unit.equals("min")) {
            multiplier = 60.0;
        } else if (unit.equals("second") || unit.equals("seconds") || unit.equals("sec") || unit.equals("s")) {
            multiplier = 1.0;
        } else if (unit.equals("milli seconds") || unit.equals("milliseconds") || unit.equals("ms")) {
            multiplier = 0.001;

            // binary size, all to byte
        } else if (unit.equals("tera byte") || unit.equals("terabytes") || unit.equals("tb")) {
            multiplier = 1099511627776.0;
        } else if (unit.equals("giga byte") || unit.equals("gigabytes") || unit.equals("gb")) {
            multiplier = 1073741824.0;
        } else if (unit.equals("mega byte") || unit.equals("megabytes") || unit.equals("mb")) {
            multiplier = 1048576.0;
        } else if (unit.equals("kilo byte") || unit.equals("kilobyte") || unit.equals("kilobytes") || unit.equals("kb")
                || unit.equals("kbyte")) {
            multiplier = 1024.0;
        } else if (unit.equals("byte") || unit.equals("bytes") || unit.equals("b")) {
            multiplier = 1.0;
        } else if (unit.equals("bit") || unit.equals("bits")) {
            multiplier = 0.125;

            // frequencies, all to Hz
        } else if (unit.equals("terrahertz") || unit.equals("thz") || unit.equals("terra hertz")) {
            multiplier = 1000000000000.0;
        } else if (unit.equals("gigahertz") || unit.equals("ghz") || unit.equals("giga hertz")) {
            multiplier = 1000000000.0;
        } else if (unit.equals("megahertz") || unit.equals("mhz") || unit.equals("mega hertz")) {
            multiplier = 1000000.0;
        } else if (unit.equals("kilohertz") || unit.equals("khz") || unit.equals("kilo hertz")) {
            multiplier = 1000.0;
        } else if (unit.equals("hertz") || unit.equals("hz")) {
            multiplier = 1.0;

            // weight, all to grams
        } else if (unit.equals("ton") || unit.equals("tons")) {
            multiplier = 1000000.0;
        } else if (unit.equals("kilograms") || unit.equals("kilogram") || unit.equals("kg") || unit.equals("kgs")) {
            multiplier = 1000.0;
        } else if (unit.equals("pound") || unit.equals("pounds") || unit.equals("lb") || unit.equals("lbs")) {
            multiplier = 453.59237;
        } else if (unit.equals("ounce") || unit.equals("ounces") || unit.equals("oz") || unit.equals("ozs")) {
            multiplier = 28.3495231;
        } else if (unit.equals("gram") || unit.equals("grams") || unit.equals("g") || unit.equals("gr")) {
            multiplier = 1.0;

            // length, all to centimeter
        } else if (unit.equals("km") || unit.equals("kms") || unit.equals("kilometer") || unit.equals("kilometre")
                || unit.equals("kilometers") || unit.equals("kilometres")) {
            multiplier = 100000.0;
        } else if (unit.equals("mile") || unit.equals("miles") || unit.equals("mi")) {
            multiplier = 160934.4;
        } else if (unit.equals("meter") || unit.equals("meters") || unit.equals("metre") || unit.equals("metres")
                || unit.equals("m")) {
            multiplier = 100.0;
        } else if (unit.equals("decimeter") || unit.equals("decimeters") || unit.equals("decimetre")
                || unit.equals("decimetres") || unit.equals("dm")) {
            multiplier = 10.0;
        } else if (unit.equals("foot") || unit.equals("feet") || unit.equals("ft")) {
            multiplier = 30.48;
        } else if (unit.equals("in") || unit.equals("inch") || unit.equals("inches") || unit.equals("\"")) {
            multiplier = 2.54;
        } else if (unit.equals("centimeter") || unit.equals("centimeters") || unit.equals("centimetre")
                || unit.equals("centimetres") || unit.equals("cm")) {
            multiplier = 1.0;
        } else if (unit.equals("millimeter") || unit.equals("millimeters") || unit.equals("millimetre")
                || unit.equals("millimetres") || unit.equals("mm")) {
            multiplier = 0.1;

            // areas, all to square meter
        } else if (unit.equals("sq.miles") || unit.equals("sq miles") || unit.equals("square mile")
                || unit.equals("square miles") || unit.equals("sq mi")) {
            multiplier = 2589988.11;
        } else if (unit.equals("thousand square miles")) {
            multiplier = 2589988110.0;
        } else if (unit.equals("million sqare miles")) {
            multiplier = 2589988110000.0;
        } else if (unit.equals("sq.kilometer") || unit.equals("sq kilometer") || unit.equals("km²")
                || unit.equals("km 2") || unit.equals("km2") || unit.equals("sq km") || unit.equals("sq.km")
                || unit.equals("square kilometer") || unit.equals("square kilometers") || unit.equals("square km")
                || unit.equals("sq.kilometre") || unit.equals("sq kilometre") || unit.equals("square kilometre")
                || unit.equals("square kilometres")) {
            multiplier = 1000000.0;
        } else if (unit.equals("million square kilometers")) {
            multiplier = 1000000000000.0;
        } else if (unit.equals("hectare") || unit.equals("hectares")) {
            multiplier = 10000.0;
        } else if (unit.equals("sq m") || unit.equals("sq meter") || unit.equals("sq meters")
                || unit.equals("square meter") || unit.equals("square meters") || unit.equals("m²")) {
            multiplier = 1.0;

            // volume (density of water) all to milliliter
        } else if (unit.equals("teaspoon") || unit.equals("teaspoons") || origUnit.equals("t") || unit.equals("tsp")
                || unit.equals("tsps")) {
            multiplier = 4.92892;
        } else if (unit.equals("tablespoon") || unit.equals("tablespoons") || origUnit.equals("T")
                || unit.equals("tbsp") || unit.equals("tbsps")) {
            multiplier = 14.7868;
        } else if (unit.equals("fl oz") || unit.equals("fl ozs") || origUnit.equals("fl ounce")
                || unit.equals("fl ounces") || unit.equals("fl. oz") || unit.equals("fl. oz.")
                || unit.equals("fl. ozs") || unit.equals("fl. ozs.")) {
            multiplier = 29.57;
        } else if (unit.equals("liters") || unit.equals("liter") || unit.equals("l")) {
            multiplier = 1000.;
        } else if (unit.equals("gallons") || unit.equals("gallon") || unit.equals("gal")) {
            multiplier = 3785.41;
        } else if (unit.equals("quart") || unit.equals("quarts") || unit.equals("qt") || unit.equals("qts")) {
            multiplier = 946.353;
        } else if (unit.equals("pint") || unit.equals("pints")) {
            multiplier = 473.176;
        } else if (unit.equals("cups") || unit.equals("cup") || unit.equals("c") || unit.equals("cp")) {
            multiplier = 236.588;
        } else if (unit.equals("milli liters") || unit.equals("milliliters") || unit.equals("ml")) {
            multiplier = 1.;

            // technical, hp and kw to hp, mile per hour to kilometer per hour, lb-ft to Nm
        } else if (unit.equals("hp") || unit.equals("horsepower") || unit.equals("horses") || unit.equals("bhp")
                || unit.equals("metric horsepower")) {
            multiplier = 1.;
        } else if (unit.equals("kw") || unit.equals("kilowatt") || unit.equals("kilowatts")) {
            multiplier = 1.3410;
        } else if (unit.equals("mph") || unit.equals("miles per hour")) {
            multiplier = 1.609344;
        } else if (unit.equals("kmh") || unit.equals("kph") || unit.equals("kilometers per hour")
                || unit.equals("km/h")) {
            multiplier = 1.0;
        } else if (unit.equals("lb/ft") || unit.equals("lbs.-ft") || unit.equals("lb ft") || unit.equals("lb-ft")
                || unit.equals("lb.-ft") || unit.equals("pound feet") || unit.equals("pound-feet")
                || unit.equals("foot pound") || unit.equals("foot-pound") || unit.equals("foot pounds")
                || unit.equals("foot-pounds") || unit.equals("ft-lb") || unit.equals("ft-lbs")) {
            multiplier = 1.355817952;
        } else if (unit.equals("nm") || unit.equals("newton meter") || unit.equals("newton meters")) {
            multiplier = 1.0;

            // unitless
        } else if (unit.equals("thousand") || unit.equals("k")) {
            multiplier = 1000.0;
        } else if (unit.equals("million") || unit.equals("mio") || unit.equals("millions")) {
            multiplier = 1000000.0;
        } else if (unit.equals("billion") || unit.equals("billions")) {
            multiplier = 1000000000.0;
        } else if (unit.equals("trillion") || unit.equals("trillions")) {
            multiplier = 1000000000000.0;
        } else if (unit.equals("mega pixel") || unit.equals("mega pixels") || unit.equals("megapixel")
                || unit.equals("megapixels") || unit.equals("mpix") || unit.equals("mpixel") || unit.equals("mp")
                || unit.equals("mpx")) {
            multiplier = 1000000.0;
        } else if (unit.equals("%")) {
            multiplier = 0.01;
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
     * transforms a normalized value to the target unit
     * @param unitTo unit to transform to
     * @param value value to transorm
     * @return transformed value
     */
    public static double transorm(String unitTo, double value) {
        double divider = unitLookup(unitTo);
        if(divider != -1){
            return value/divider;
        }else{
            return value;
        }
    }

    public static double transorm(String unitTo, String value) {
        return transorm(unitTo, Double.valueOf(value));
    }

    public static String getUnitTypeName(String string) {
        int unitType = getUnitType(string);
        if (unitType == UNIT_TIME) {
            return "sec";
        } else if (unitType == UNIT_DIGITAL) {
            return "bytes";
        } else if (unitType == UNIT_FREQUENCY) {
            return "Hz";
        } else if (unitType == UNIT_LENGTH) {
            return "cm";
        } else if (unitType == UNIT_WEIGHT) {
            return "g";
        } else {
            return "";
        }
    }

    public static int getUnitType(String string) {
        String words[] = string.split(" ");
        int unitType = UNIT_UNITLESS;

        for (String word2 : words) {
            String word = word2.toLowerCase();
            if (isTimeUnit(word)) {
                unitType = UNIT_TIME;
            }
            if (isDigitalUnit(word)) {
                unitType = UNIT_DIGITAL;
            }
            if (isFrequencyUnit(word)) {
                unitType = UNIT_FREQUENCY;
            }
            if (isLengthUnit(word)) {
                unitType = UNIT_LENGTH;
            }
            if (isWeightUnit(word)) {
                unitType = UNIT_WEIGHT;
            }
            if (unitType != UNIT_UNITLESS) {
                break; // we found a unit
            }
        }
        return unitType;
    }

    public static double getNormalizedNumber(String unitText) throws NumberFormatException, NullPointerException {

        // add space in case it's missing "2.4Ghz" => "2.4 Ghz"
        unitText = unitText.replaceAll("(\\d)([A-Za-z])", "$1 $2").trim();
        String words[] = unitText.split(" ");

        if (words.length == 0) {
            words = unitText.trim().split("(?<=[0-9])(?=\\w)");
        }

        double number = Double.parseDouble(words[0]);

        String newUnitText = "";
        for (int i = 1; i < words.length; i++) {
            newUnitText += words[i] + " ";
        }
        return getNormalizedNumber(number, newUnitText, 3, "");
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


        System.out.println(getNormalizedNumber(20,"inch"));
        System.out.println(transorm("inch", getNormalizedNumber(20,"inch")));

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
