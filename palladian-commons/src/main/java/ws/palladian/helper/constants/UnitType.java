package ws.palladian.helper.constants;

import org.apache.commons.lang3.tuple.Pair;
import ws.palladian.helper.collection.StringLengthComparator;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * The type of a unit, e.g. "weight" for units such as "g", "tons" etc.
 * </p>
 *
 * @author David Urbansky
 */
public enum UnitType {
    NONE(null), //
    BANDWIDTH("bit/s"), //
    TIME("s"), //
    DIGITAL("byte"), //
    FREQUENCY("Hz"), //
    ROTATION_SPEED("rpm"), //
    LENGTH("cm"), //
    AREA("m²"), //
    AREA_DENSITY("g/m²"), //
    DENSITY("kg/m³"), //
    VOLUME("cm³"), //
    POWER_RATIO("dB"), //
    WEIGHT("g"), //
    FORCE("N"), //
    SPEED("km/h"), //
    TEMPERATURE(null), //
    PRESSURE("pascal"), //
    ELECTRICAL_RESISTANCE("ohm"), //
    POWER("watt"), //
    VOLTAGE("volt"), //
    ENERGY("kilojoule"), //
    CURRENT("ampere"), //
    ELECTRIC_CHARGE("ampere-hour"), //
    TORQUE("Nm"), //
    PIXEL("pixel"), //
    LUMINANCE("nit"), //
    FLOW_RATE("m³/s"), //
    CURRENCY(null), //
    OTHER(null);

    private final List<Pair<List<String>, Double>> units = new ArrayList<>();
    private List<String> sortedUnitNames = new ArrayList<>();

    private final String baseUnit;

    UnitType(String baseUnit) {
        this.baseUnit = baseUnit;
    }

    static {
        List<String> unitList;

        // NONE units are normalized to 1
        unitList = new ArrayList<>();
        unitList.add("trillions");
        unitList.add("trillion");
        UnitType.NONE.units.add(Pair.of(unitList, 1000000000000.0));

        unitList = new ArrayList<>();
        unitList.add("billions");
        unitList.add("billion");
        UnitType.NONE.units.add(Pair.of(unitList, 1000000000.0));

        unitList = new ArrayList<>();
        unitList.add("millions");
        unitList.add("million");
        UnitType.NONE.units.add(Pair.of(unitList, 1000000.0));

        unitList = new ArrayList<>();
        unitList.add("thousand");
        unitList.add("k");
        UnitType.NONE.units.add(Pair.of(unitList, 1000.0));

        // BANDWIDTH units are normalized to 1 bit/s
        unitList = new ArrayList<>();
        unitList.add("bit/s");
        unitList.add("Bit/s");
        UnitType.BANDWIDTH.units.add(Pair.of(unitList, 1.0));

        unitList = new ArrayList<>();
        unitList.add("byte/s");
        unitList.add("Byte/s");
        UnitType.BANDWIDTH.units.add(Pair.of(unitList, 8.0));

        unitList = new ArrayList<>();
        unitList.add("kbit/s");
        unitList.add("kbps");
        unitList.add("kBit/s");
        UnitType.BANDWIDTH.units.add(Pair.of(unitList, 1000.0));

        unitList = new ArrayList<>();
        unitList.add("kbyte/s");
        unitList.add("kByte/s");
        UnitType.BANDWIDTH.units.add(Pair.of(unitList, 8000.0));

        unitList = new ArrayList<>();
        unitList.add("mbit/s");
        unitList.add("mbps");
        unitList.add("mBit/s");
        UnitType.BANDWIDTH.units.add(Pair.of(unitList, 1000000.0));

        unitList = new ArrayList<>();
        unitList.add("MB/s");
        unitList.add("mb/s");
        unitList.add("Mbyte/s");
        UnitType.BANDWIDTH.units.add(Pair.of(unitList, 8000000.0));

        unitList = new ArrayList<>();
        unitList.add("gbit/s");
        unitList.add("gbps");
        unitList.add("gBit/s");
        UnitType.BANDWIDTH.units.add(Pair.of(unitList, 1000000000.0));

        // FORCE units are normalized to 1 Newton
        unitList = new ArrayList<>();
        unitList.add("N");
        unitList.add("kg m/s^2");
        unitList.add("kg m/s²");
        unitList.add("newton");
        unitList.add("newtons");
        UnitType.FORCE.units.add(Pair.of(unitList, 1.0));

        unitList = new ArrayList<>();
        unitList.add("kp");
        UnitType.FORCE.units.add(Pair.of(unitList, 9.80665));

        unitList = new ArrayList<>();
        unitList.add("dyn");
        UnitType.FORCE.units.add(Pair.of(unitList, 0.00005));

        unitList = new ArrayList<>();
        unitList.add("lbf");
        UnitType.FORCE.units.add(Pair.of(unitList, 4.448222));

        unitList = new ArrayList<>();
        unitList.add("pdl");
        UnitType.FORCE.units.add(Pair.of(unitList, 0.138255));

        // POWER units are normalized to 1 Watt
        unitList = new ArrayList<>();
        unitList.add("mega watts");
        unitList.add("mega watt");
        unitList.add("megawatts");
        unitList.add("megawatt");
        unitList.add("MW");
        UnitType.POWER.units.add(Pair.of(unitList, 1000000.0));

        unitList = new ArrayList<>();
        unitList.add("kilo watts");
        unitList.add("kilo watt");
        unitList.add("kilowatts");
        unitList.add("kilowatt");
        unitList.add("kw");
        UnitType.POWER.units.add(Pair.of(unitList, 1000.0));

        unitList = new ArrayList<>();
        unitList.add("watts");
        unitList.add("watt");
        unitList.add("w");
        UnitType.POWER.units.add(Pair.of(unitList, 1.0));

        unitList = new ArrayList<>();
        unitList.add("milli watts");
        unitList.add("milli watt");
        unitList.add("milliwatts");
        unitList.add("milliwatt");
        unitList.add("mw");
        unitList.add("mW");
        UnitType.POWER.units.add(Pair.of(unitList, 0.001));

        unitList = new ArrayList<>();
        unitList.add("horsepower");
        unitList.add("horses");
        unitList.add("metric horsepower");
        unitList.add("bhp");
        unitList.add("hp");
        UnitType.POWER.units.add(Pair.of(unitList, 745.699872));

        // ENERGY units are normalized to 1 Joule
        unitList = new ArrayList<>();
        unitList.add("kilo joules");
        unitList.add("kilo joule");
        unitList.add("kilojoules");
        unitList.add("kilojoule");
        unitList.add("kj");
        UnitType.ENERGY.units.add(Pair.of(unitList, 1000.0));

        unitList = new ArrayList<>();
        unitList.add("joules");
        unitList.add("joule");
        unitList.add("j");
        UnitType.ENERGY.units.add(Pair.of(unitList, 1.0));

        unitList = new ArrayList<>();
        unitList.add("kcal");
        unitList.add("kilocalories");
        UnitType.ENERGY.units.add(Pair.of(unitList, 4184.));

        unitList = new ArrayList<>();
        unitList.add("watt hours");
        unitList.add("watt hour");
        unitList.add("watt/h");
        unitList.add("w/h");
        unitList.add("wh");
        UnitType.ENERGY.units.add(Pair.of(unitList, 3600.));

        unitList = new ArrayList<>();
        unitList.add("kilo watt hours");
        unitList.add("kilo watt hour");
        unitList.add("kw/h");
        unitList.add("kwh");
        UnitType.ENERGY.units.add(Pair.of(unitList, 3600000.));

        // CURRENT units are normalized to 1 Ampere
        unitList = new ArrayList<>();
        unitList.add("ampere");
        unitList.add("amp");
        unitList.add("a");
        UnitType.CURRENT.units.add(Pair.of(unitList, 1.));

        unitList = new ArrayList<>();
        unitList.add("milli ampere");
        unitList.add("ma");
        UnitType.CURRENT.units.add(Pair.of(unitList, 0.001));

        // ELECTRIC_CHARGE units are normalized to 1 Ah
        unitList = new ArrayList<>();
        unitList.add("ampere-hour");
        unitList.add("amp-hour");
        unitList.add("ah");
        UnitType.ELECTRIC_CHARGE.units.add(Pair.of(unitList, 1.));

        unitList = new ArrayList<>();
        unitList.add("milli ampere-hour");
        unitList.add("milli amp-hour");
        unitList.add("mah");
        UnitType.ELECTRIC_CHARGE.units.add(Pair.of(unitList, 0.001));

        // TORQUE units are normalized to 1 Newton Meter
        unitList = new ArrayList<>();
        unitList.add("Nm");
        unitList.add("nm");
        unitList.add("newton meters");
        unitList.add("newton meter");
        unitList.add("nm");
        UnitType.TORQUE.units.add(Pair.of(unitList, 1.0));

        unitList = new ArrayList<>();
        unitList.add("mkgf");
        unitList.add("m kgf");
        UnitType.TORQUE.units.add(Pair.of(unitList, 9.80665));

        unitList = new ArrayList<>();
        unitList.add("in lbf");
        unitList.add("inlbf");
        unitList.add("in-lbf");
        UnitType.TORQUE.units.add(Pair.of(unitList, 0.1129848290276167));

        unitList = new ArrayList<>();
        unitList.add("ft pdl");
        unitList.add("ftpdl");
        unitList.add("ft-pdl");
        UnitType.TORQUE.units.add(Pair.of(unitList, 0.0421401100938048));

        unitList = new ArrayList<>();
        unitList.add("ft lbf");
        unitList.add("ftlbf");
        unitList.add("ft-lbf");
        unitList.add("ft-lb");
        unitList.add("ft-lbs");
        unitList.add("foot pounds");
        unitList.add("foot-pounds");
        unitList.add("pound feet");
        unitList.add("pound-feet");
        unitList.add("foot pound");
        unitList.add("foot-pound");
        unitList.add("lbs.-ft");
        unitList.add("lb.-ft");
        unitList.add("lb/ft");
        unitList.add("lb ft");
        unitList.add("lb-ft");
        UnitType.TORQUE.units.add(Pair.of(unitList, 1.3558179483314004));

        // VOLTAGE units are normalized to 1 Volt
        unitList = new ArrayList<>();
        unitList.add("V");
        unitList.add("v");
        unitList.add("Volts");
        unitList.add("Volt");
        unitList.add("volts");
        unitList.add("volt");
        UnitType.VOLTAGE.units.add(Pair.of(unitList, 1.0));

        unitList = new ArrayList<>();
        unitList.add("µV");
        unitList.add("µv");
        UnitType.VOLTAGE.units.add(Pair.of(unitList, 0.000001));

        unitList = new ArrayList<>();
        unitList.add("mV");
        unitList.add("mv");
        UnitType.VOLTAGE.units.add(Pair.of(unitList, 0.001));

        unitList = new ArrayList<>();
        unitList.add("kV");
        unitList.add("kv");
        UnitType.VOLTAGE.units.add(Pair.of(unitList, 1000.));

        unitList = new ArrayList<>();
        unitList.add("MV");
        unitList.add("mv");
        UnitType.VOLTAGE.units.add(Pair.of(unitList, 1000000.));

        // PIXEL units are normalized to 1
        unitList = new ArrayList<>();
        unitList.add("mega pixels");
        unitList.add("megapixels");
        unitList.add("mega pixel");
        unitList.add("megapixel");
        unitList.add("mpix");
        unitList.add("mpixel");
        unitList.add("mpx");
        unitList.add("mp");
        UnitType.PIXEL.units.add(Pair.of(unitList, 1000000.0));

        unitList = new ArrayList<>();
        unitList.add("kilopixels");
        unitList.add("kilo pixel");
        unitList.add("kilopixel");
        unitList.add("kpix");
        unitList.add("k");
        UnitType.PIXEL.units.add(Pair.of(unitList, 1000.0));

        unitList = new ArrayList<>();
        unitList.add("pixel");
        UnitType.PIXEL.units.add(Pair.of(unitList, 1.0));

        // TEMPERATURE units will not be normalized as there are non-linear projections
        for (TemperatureUnit tUnit : TemperatureUnit.values()) {
            unitList = new ArrayList<>(tUnit.getNames());
            UnitType.TEMPERATURE.units.add(Pair.of(unitList, null));
        }

        // TIME units are normalized to seconds
        unitList = new ArrayList<>();
        unitList.add("years");
        unitList.add("year");
        UnitType.TIME.units.add(Pair.of(unitList, 31536000.0));

        unitList = new ArrayList<>();
        unitList.add("months");
        unitList.add("month");
        UnitType.TIME.units.add(Pair.of(unitList, 2592000.0));

        unitList = new ArrayList<>();
        unitList.add("weeks");
        unitList.add("week");
        UnitType.TIME.units.add(Pair.of(unitList, 604800.0));

        unitList = new ArrayList<>();
        unitList.add("day(s)");
        unitList.add("days");
        unitList.add("day");
        UnitType.TIME.units.add(Pair.of(unitList, 86400.0));

        unitList = new ArrayList<>();
        unitList.add("hour(s)");
        unitList.add("hours");
        unitList.add("hour");
        unitList.add("hrs");
        unitList.add("hr");
        unitList.add("h");
        UnitType.TIME.units.add(Pair.of(unitList, 3600.0));

        unitList = new ArrayList<>();
        unitList.add("minute(s)");
        unitList.add("minutes");
        unitList.add("minute");
        unitList.add("mins");
        unitList.add("min");
        UnitType.TIME.units.add(Pair.of(unitList, 60.0));

        unitList = new ArrayList<>();
        unitList.add("second(s)");
        unitList.add("seconds");
        unitList.add("second");
        unitList.add("secs");
        unitList.add("sec");
        unitList.add("s");
        UnitType.TIME.units.add(Pair.of(unitList, 1.0));

        unitList = new ArrayList<>();
        unitList.add("milli seconds");
        unitList.add("milliseconds");
        unitList.add("millis");
        unitList.add("ms");
        UnitType.TIME.units.add(Pair.of(unitList, 0.001));

        // DIGITAL units are normalized to bytes
        unitList = new ArrayList<>();
        unitList.add("tera bytes");
        unitList.add("tera byte");
        unitList.add("terabytes");
        unitList.add("terabyte");
        unitList.add("tb");
        UnitType.DIGITAL.units.add(Pair.of(unitList, 1099511627776.0));

        unitList = new ArrayList<>();
        unitList.add("giga bytes");
        unitList.add("giga byte");
        unitList.add("gigabytes");
        unitList.add("gigabyte");
        unitList.add("gb");
        UnitType.DIGITAL.units.add(Pair.of(unitList, 1073741824.0));

        unitList = new ArrayList<>();
        unitList.add("mega bytes");
        unitList.add("mega byte");
        unitList.add("megabytes");
        unitList.add("megabyte");
        unitList.add("mb");
        UnitType.DIGITAL.units.add(Pair.of(unitList, 1048576.0));

        unitList = new ArrayList<>();
        unitList.add("kilo bytes");
        unitList.add("kilo byte");
        unitList.add("kilobytes");
        unitList.add("kilobyte");
        unitList.add("kb");
        unitList.add("kbyte");
        UnitType.DIGITAL.units.add(Pair.of(unitList, 1024.0));

        unitList = new ArrayList<>();
        unitList.add("byte");
        unitList.add("bytes");
        unitList.add("b");
        UnitType.DIGITAL.units.add(Pair.of(unitList, 1.0));

        unitList = new ArrayList<>();
        unitList.add("bit");
        unitList.add("bits");
        UnitType.DIGITAL.units.add(Pair.of(unitList, 1. / 8));

        // FREQUENCY units are normalized to 1 Hz
        unitList = new ArrayList<>();
        unitList.add("terra hertz");
        unitList.add("terrahertz");
        unitList.add("thz");
        UnitType.FREQUENCY.units.add(Pair.of(unitList, 1000000000000.0));

        unitList = new ArrayList<>();
        unitList.add("giga hertz");
        unitList.add("gigahertz");
        unitList.add("ghz");
        UnitType.FREQUENCY.units.add(Pair.of(unitList, 1000000000.0));

        unitList = new ArrayList<>();
        unitList.add("mega hertz");
        unitList.add("megahertz");
        unitList.add("mhz");
        UnitType.FREQUENCY.units.add(Pair.of(unitList, 1000000.0));

        unitList = new ArrayList<>();
        unitList.add("kilo hertz");
        unitList.add("kilohertz");
        unitList.add("khz");
        UnitType.FREQUENCY.units.add(Pair.of(unitList, 1000.0));

        unitList = new ArrayList<>();
        unitList.add("hertz");
        unitList.add("hz");
        UnitType.FREQUENCY.units.add(Pair.of(unitList, 1.0));

        // ROTATION_SPEED in 1 round per minute
        unitList = new ArrayList<>();
        unitList.add("rpm");
        unitList.add("bpm");
        unitList.add("r/min");
        unitList.add("rounds/minute");
        unitList.add("rounds per minute");
        unitList.add("beats per minute");
        UnitType.ROTATION_SPEED.units.add(Pair.of(unitList, 1.0));

        // ELECTRICAL RESISTANCE in 1 Ohm
        unitList = new ArrayList<>();
        unitList.add("ohms");
        unitList.add("ohm");
        unitList.add("Ω");
        UnitType.ELECTRICAL_RESISTANCE.units.add(Pair.of(unitList, 1.0));

        // PRESSURE units are normalized to pascal
        unitList = new ArrayList<>();
        unitList.add("pascals");
        unitList.add("pascal");
        unitList.add("pa");
        UnitType.PRESSURE.units.add(Pair.of(unitList, 1.0));

        unitList = new ArrayList<>();
        unitList.add("hectopascals");
        unitList.add("hectopascal");
        unitList.add("hPa");
        UnitType.PRESSURE.units.add(Pair.of(unitList, 100.0));

        unitList = new ArrayList<>();
        unitList.add("N/m²");
        unitList.add("N/m^2");
        unitList.add("N/m2");
        unitList.add("n/m²");
        unitList.add("n/m^2");
        unitList.add("n/m2");
        UnitType.PRESSURE.units.add(Pair.of(unitList, 1.0));

        unitList = new ArrayList<>();
        unitList.add("kilobar");
        unitList.add("kilo bar");
        unitList.add("kbar");
        UnitType.PRESSURE.units.add(Pair.of(unitList, 100000000.0));

        unitList = new ArrayList<>();
        unitList.add("bar");
        UnitType.PRESSURE.units.add(Pair.of(unitList, 100000.0));

        unitList = new ArrayList<>();
        unitList.add("millibar");
        unitList.add("mbar");
        UnitType.PRESSURE.units.add(Pair.of(unitList, 100.0));

        unitList = new ArrayList<>();
        unitList.add("kN/m²");
        unitList.add("kN/m^2");
        unitList.add("kN/m2");
        unitList.add("kn/m²");
        unitList.add("kn/m^2");
        unitList.add("kn/m2");
        UnitType.PRESSURE.units.add(Pair.of(unitList, 1000.0));

        // LUMINANCE units are normalized to nits
        unitList = new ArrayList<>();
        unitList.add("cd/m2");
        unitList.add("cd/m²");
        unitList.add("cd/m^2");
        unitList.add("nits");
        unitList.add("nt");
        UnitType.LUMINANCE.units.add(Pair.of(unitList, 1.));

        // LENGTH units are normalized to centimeter
        unitList = new ArrayList<>();
        unitList.add("miles");
        unitList.add("mile");
        unitList.add("mi");
        UnitType.LENGTH.units.add(Pair.of(unitList, 160934.4));

        unitList = new ArrayList<>();
        unitList.add("kilometers");
        unitList.add("kilometres");
        unitList.add("kilometre");
        unitList.add("kilometer");
        unitList.add("kms");
        unitList.add("km");
        UnitType.LENGTH.units.add(Pair.of(unitList, 100000.0));

        unitList = new ArrayList<>();
        unitList.add("meters");
        unitList.add("metres");
        unitList.add("meter");
        unitList.add("metre");
        unitList.add("mtr");
        unitList.add("m");
        UnitType.LENGTH.units.add(Pair.of(unitList, 100.0));

        unitList = new ArrayList<>();
        unitList.add("decimeter");
        unitList.add("decimeters");
        unitList.add("decimetre");
        unitList.add("decimetres");
        unitList.add("dm");
        UnitType.LENGTH.units.add(Pair.of(unitList, 10.0));

        unitList = new ArrayList<>();
        unitList.add("foot");
        unitList.add("feet");
        unitList.add("ft");
        UnitType.LENGTH.units.add(Pair.of(unitList, 30.48));

        unitList = new ArrayList<>();
        unitList.add("inches");
        unitList.add("inch");
        unitList.add("in");
        unitList.add("\"");
        unitList.add("”");
        UnitType.LENGTH.units.add(Pair.of(unitList, 2.54));

        unitList = new ArrayList<>();
        unitList.add("centimeters");
        unitList.add("centimetres");
        unitList.add("centimeter");
        unitList.add("centimetre");
        unitList.add("cm");
        UnitType.LENGTH.units.add(Pair.of(unitList, 1.));

        unitList = new ArrayList<>();
        unitList.add("millimeters");
        unitList.add("millimetres");
        unitList.add("millimeter");
        unitList.add("millimetre");
        unitList.add("mm");
        UnitType.LENGTH.units.add(Pair.of(unitList, 0.1));

        // WEIGHT units are normalized to grams
        unitList = new ArrayList<>();
        unitList.add("tons");
        unitList.add("ton");
        unitList.add("t");
        UnitType.WEIGHT.units.add(Pair.of(unitList, 1000000.));

        unitList = new ArrayList<>();
        unitList.add("kilograms");
        unitList.add("kilogram");
        unitList.add("kg");
        unitList.add("kgs");
        UnitType.WEIGHT.units.add(Pair.of(unitList, 1000.));

        unitList = new ArrayList<>();
        unitList.add("pounds");
        unitList.add("pound");
        unitList.add("lbs");
        unitList.add("lb");
        UnitType.WEIGHT.units.add(Pair.of(unitList, 453.59237));

        unitList = new ArrayList<>();
        unitList.add("ounces");
        unitList.add("ounce");
        unitList.add("ozs");
        unitList.add("oz");
        UnitType.WEIGHT.units.add(Pair.of(unitList, 28.3495231));

        unitList = new ArrayList<>();
        unitList.add("gram");
        unitList.add("grams");
        unitList.add("g");
        unitList.add("gs");
        unitList.add("gr.");
        unitList.add("gr");
        UnitType.WEIGHT.units.add(Pair.of(unitList, 1.));

        // AREA units are normalized to square meter
        unitList = new ArrayList<>();
        unitList.add("square miles");
        unitList.add("square mile");
        unitList.add("sq.miles");
        unitList.add("sq miles");
        unitList.add("sq mi");
        UnitType.AREA.units.add(Pair.of(unitList, 2589988.11));

        unitList = new ArrayList<>();
        unitList.add("square kilometers");
        unitList.add("square kilometer");
        unitList.add("square kilometres");
        unitList.add("square kilometre");
        unitList.add("sq.kilometers");
        unitList.add("sq.kilometres");
        unitList.add("sq kilometers");
        unitList.add("sq kilometres");
        unitList.add("sq kilometre");
        unitList.add("sq.kilometer");
        unitList.add("sq.kilometre");
        unitList.add("sq kilometer");
        unitList.add("square kms");
        unitList.add("square km");
        unitList.add("sq.kms");
        unitList.add("sq.km");
        unitList.add("sq kms");
        unitList.add("sq km");
        unitList.add("km²");
        unitList.add("km2");
        UnitType.AREA.units.add(Pair.of(unitList, 1000000.));

        unitList = new ArrayList<>();
        unitList.add("hectares");
        unitList.add("hectare");
        UnitType.AREA.units.add(Pair.of(unitList, 10000.));

        unitList = new ArrayList<>();
        unitList.add("square foot");
        unitList.add("square feet");
        unitList.add("sq. ft.");
        unitList.add("sq ft");
        unitList.add("square ft");
        unitList.add("sqft");
        unitList.add("ft²");
        UnitType.AREA.units.add(Pair.of(unitList, 0.092903));

        unitList = new ArrayList<>();
        unitList.add("square meters");
        unitList.add("square meter");
        unitList.add("square metres");
        unitList.add("square metre");
        unitList.add("sq.meters");
        unitList.add("sq.metres");
        unitList.add("sq meters");
        unitList.add("sq metres");
        unitList.add("sq metre");
        unitList.add("sq.meter");
        unitList.add("sq.metre");
        unitList.add("sq meter");
        unitList.add("square ms");
        unitList.add("square m");
        unitList.add("sq.ms");
        unitList.add("sq.m");
        unitList.add("sq ms");
        unitList.add("sq m");
        unitList.add("m²");
        unitList.add("m2");
        UnitType.AREA.units.add(Pair.of(unitList, 1.));

        // AREA_DENSITY units are normalized to g/m2
        unitList = new ArrayList<>();
        unitList.add("g/m2");
        unitList.add("g/m²");
        unitList.add("gsm");
        UnitType.AREA_DENSITY.units.add(Pair.of(unitList, 1.));

        unitList = new ArrayList<>();
        unitList.add("ounce per square yard");
        unitList.add("oz/yard²");
        UnitType.AREA_DENSITY.units.add(Pair.of(unitList, 33.9));

        // DENSITY units are normalized to kg/m²
        unitList = new ArrayList<>();
        unitList.add("kg/m3");
        unitList.add("kg/m³");
        unitList.add("kilogram per cubic metre");
        UnitType.DENSITY.units.add(Pair.of(unitList, 1.));

        unitList = new ArrayList<>();
        unitList.add("g/cm3");
        unitList.add("g/cm³");
        unitList.add("cgs");
        UnitType.DENSITY.units.add(Pair.of(unitList, 1000.));

        // VOLUME units are normalized to milliliter
        unitList = new ArrayList<>();
        unitList.add("m³");
        UnitType.VOLUME.units.add(Pair.of(unitList, 1000000.));

        unitList = new ArrayList<>();
        unitList.add("gallons");
        unitList.add("gallon");
        unitList.add("gal");
        UnitType.VOLUME.units.add(Pair.of(unitList, 3785.41));

        unitList = new ArrayList<>();
        unitList.add("liters");
        unitList.add("liter");
        unitList.add("ltrs");
        unitList.add("l");
        unitList.add("cubic decimeter");
        unitList.add("dm3");
        unitList.add("dm³");
        UnitType.VOLUME.units.add(Pair.of(unitList, 1000.));

        unitList = new ArrayList<>();
        unitList.add("quarts");
        unitList.add("quart");
        unitList.add("qts");
        unitList.add("qt");
        UnitType.VOLUME.units.add(Pair.of(unitList, 946.353));

        unitList = new ArrayList<>();
        unitList.add("pints");
        unitList.add("pint");
        unitList.add("pts");
        unitList.add("pt");
        UnitType.VOLUME.units.add(Pair.of(unitList, 473.176));

        unitList = new ArrayList<>();
        unitList.add("cups");
        unitList.add("cup");
        unitList.add("cp");
        unitList.add("c");
        UnitType.VOLUME.units.add(Pair.of(unitList, 236.588));

        unitList = new ArrayList<>();
        unitList.add("deciliters");
        unitList.add("deciliter");
        unitList.add("decilitres");
        unitList.add("decilitre");
        unitList.add("dl");
        UnitType.VOLUME.units.add(Pair.of(unitList, 100.));

        unitList = new ArrayList<>();
        unitList.add("fl. oz");
        unitList.add("fl. oz.");
        unitList.add("fl. ozs");
        unitList.add("fl. ozs.");
        unitList.add("fl. ounce");
        unitList.add("fl. ounces");
        unitList.add("fl oz");
        unitList.add("fl oz.");
        unitList.add("fl ozs");
        unitList.add("fl ozs.");
        unitList.add("fl ounce");
        unitList.add("fl ounces");
        unitList.add("fluid ounce");
        unitList.add("fluid ounces");
        UnitType.VOLUME.units.add(Pair.of(unitList, 29.57));

        unitList = new ArrayList<>();
        unitList.add("tablespoons");
        unitList.add("tablespoon");
        unitList.add("Tbsps");
        unitList.add("tbsps");
        unitList.add("tbsp");
        //        unitList.add("T");
        UnitType.VOLUME.units.add(Pair.of(unitList, 14.7868));

        unitList = new ArrayList<>();
        unitList.add("centiliters");
        unitList.add("centilitres");
        unitList.add("cl");
        UnitType.VOLUME.units.add(Pair.of(unitList, 10.));

        unitList = new ArrayList<>();
        unitList.add("teaspoons");
        unitList.add("teaspoon");
        unitList.add("tsps");
        unitList.add("tsp");
        //        unitList.add("t");
        UnitType.VOLUME.units.add(Pair.of(unitList, 4.92892));

        unitList = new ArrayList<>();
        unitList.add("milliliters");
        unitList.add("millilitres");
        unitList.add("milliliter");
        unitList.add("millilitre");
        unitList.add("mls");
        unitList.add("ml");
        unitList.add("mL");
        unitList.add("cm³");
        UnitType.VOLUME.units.add(Pair.of(unitList, 1.));

        unitList = new ArrayList<>();
        // US 1.5 fl. oz (different depending on country)
        unitList.add("measures");
        unitList.add("measure");
        unitList.add("shots");
        unitList.add("shot");
        UnitType.VOLUME.units.add(Pair.of(unitList, 44.3603));

        // SPEED units are normalized to km/h
        unitList = new ArrayList<>();
        unitList.add("miles per hour");
        unitList.add("mph");
        UnitType.SPEED.units.add(Pair.of(unitList, 1.609344));

        unitList = new ArrayList<>();
        unitList.add("kilometers per hour");
        unitList.add("kmh");
        unitList.add("km/h");
        unitList.add("kph");
        UnitType.SPEED.units.add(Pair.of(unitList, 1.));

        // POWER_RATIO
        unitList = new ArrayList<>();
        unitList.add("db");
        unitList.add("db(a)");
        unitList.add("db(b)");
        unitList.add("db(c)");
        unitList.add("dba");
        unitList.add("dbb");
        unitList.add("dbc");
        unitList.add("decibel");
        UnitType.POWER_RATIO.units.add(Pair.of(unitList, 1.));

        // FLOW RATE
        unitList = new ArrayList<>();
        unitList.add("us gpm");
        unitList.add("us gallons per minute");
        unitList.add("us gallons / minute");
        UnitType.FLOW_RATE.units.add(Pair.of(unitList, 0.0000630902));

        unitList = new ArrayList<>();
        unitList.add("gpm");
        unitList.add("gallons per minute");
        unitList.add("gallons / minute");
        UnitType.FLOW_RATE.units.add(Pair.of(unitList, 0.0000757682));

        unitList = new ArrayList<>();
        unitList.add("m³/s");
        UnitType.FLOW_RATE.units.add(Pair.of(unitList, 1.));

        // CURRENCY units are not normalized
        unitList = new ArrayList<>();
        unitList.add("euros");
        unitList.add("euro");
        unitList.add("eur");
        unitList.add("€");
        unitList.add("dollars");
        unitList.add("dollar");
        unitList.add("$");
        unitList.add("pound sterling");
        unitList.add("£");
        unitList.add("yen");
        unitList.add("¥");
        unitList.add("chf");
        unitList.add("swiss francs");
        UnitType.CURRENCY.units.add(Pair.of(unitList, 1.));

        // sort all unit names
        for (UnitType unitType : values()) {
            unitType.sortedUnitNames = new ArrayList<>();

            for (Pair<List<String>, Double> pair : unitType.getUnits()) {
                unitType.sortedUnitNames.addAll(pair.getLeft());
            }

            unitType.sortedUnitNames.sort(StringLengthComparator.INSTANCE);
        }
    }

    public boolean contains(String unit) {
        for (Pair<List<String>, Double> entry : this.units) {
            for (String unitName : entry.getLeft()) {
                if (unitName.equalsIgnoreCase(unit)) {
                    return true;
                }
            }
        }

        return false;
    }

    public String getBaseUnit() {
        return this.baseUnit;
    }

    public List<Pair<List<String>, Double>> getUnits() {
        return units;
    }

    public List<String> getUnitNames() {
        return sortedUnitNames;
    }
}