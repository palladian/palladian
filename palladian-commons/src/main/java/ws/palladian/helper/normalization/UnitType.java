package ws.palladian.helper.normalization;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.StringLengthComparator;
import ws.palladian.helper.constants.TemperatureUnit;

/**
 * <p>
 * The type of a unit, e.g. "weight" for units such as "g", "tons" etc.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public enum UnitType {

    NONE, //
    TIME, //
    DIGITAL, //
    FREQUENCY, //
    LENGTH, //
    AREA, //
    VOLUME, //
    POWER_RATIO, //
    WEIGHT, //
    SPEED, //
    TEMPERATURE, //
    PIXEL, //
    OTHER;

    private List<Pair<List<String>, Double>> units = CollectionHelper.newArrayList();
    private List<String> sortedUnitNames = CollectionHelper.newArrayList();

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

    static {

        List<String> unitList = CollectionHelper.newArrayList();

        // NONE units are normalized to 1
        unitList = CollectionHelper.newArrayList();
        unitList.add("trillions");
        unitList.add("trillion");
        UnitType.NONE.units.add(Pair.of(unitList, 1000000000000.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("billions");
        unitList.add("billion");
        UnitType.NONE.units.add(Pair.of(unitList, 1000000000.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("millions");
        unitList.add("million");
        UnitType.NONE.units.add(Pair.of(unitList, 1000000.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("thousand");
        unitList.add("k");
        UnitType.NONE.units.add(Pair.of(unitList, 1000.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("percents");
        unitList.add("per cent");
        unitList.add("percent");
        unitList.add("perc");
        unitList.add("%");
        UnitType.NONE.units.add(Pair.of(unitList, 0.01));

        // PIXEL units are normalized to 1
        unitList = CollectionHelper.newArrayList();
        unitList.add("mega pixels");
        unitList.add("megapixels");
        unitList.add("mega pixel");
        unitList.add("megapixel");
        unitList.add("mpix");
        unitList.add("mpixel");
        unitList.add("mpx");
        unitList.add("mp");
        UnitType.PIXEL.units.add(Pair.of(unitList, 1000000.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("kilopixels");
        unitList.add("kilo pixel");
        unitList.add("kilopixel");
        unitList.add("kpix");
        unitList.add("k");
        UnitType.PIXEL.units.add(Pair.of(unitList, 1000.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("pixel");
        UnitType.PIXEL.units.add(Pair.of(unitList, 1.0));

        // TEMPERATURE units will not be normalized as there are non-linear projections
        for (TemperatureUnit tUnit : TemperatureUnit.values()) {
            unitList = CollectionHelper.newArrayList();
            for (String name : tUnit.getNames()) {
                unitList.add(name);
            }
            UnitType.TEMPERATURE.units.add(Pair.<List<String>, Double> of(unitList, null));
        }

        // TIME units are normalized to seconds
        unitList = CollectionHelper.newArrayList();
        unitList.add("years");
        unitList.add("year");
        UnitType.TIME.units.add(Pair.of(unitList, 31536000.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("months");
        unitList.add("month");
        UnitType.TIME.units.add(Pair.of(unitList, 2592000.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("weeks");
        unitList.add("week");
        UnitType.TIME.units.add(Pair.of(unitList, 604800.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("day(s)");
        unitList.add("days");
        unitList.add("day");
        UnitType.TIME.units.add(Pair.of(unitList, 86400.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("hour(s)");
        unitList.add("hours");
        unitList.add("hour");
        unitList.add("hrs");
        unitList.add("hr");
        unitList.add("h");
        UnitType.TIME.units.add(Pair.of(unitList, 3600.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("minute(s)");
        unitList.add("minutes");
        unitList.add("minute");
        unitList.add("min");
        UnitType.TIME.units.add(Pair.of(unitList, 60.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("second(s)");
        unitList.add("seconds");
        unitList.add("second");
        unitList.add("sec");
        unitList.add("s");
        UnitType.TIME.units.add(Pair.of(unitList, 1.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("milli seconds");
        unitList.add("milliseconds");
        unitList.add("ms");
        UnitType.TIME.units.add(Pair.of(unitList, 0.001));

        // DIGITAL units are normalized to bytes
        unitList = CollectionHelper.newArrayList();
        unitList.add("tera bytes");
        unitList.add("terabytes");
        unitList.add("tb");
        UnitType.DIGITAL.units.add(Pair.of(unitList, 1099511627776.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("giga byte");
        unitList.add("gigabytes");
        unitList.add("gb");
        UnitType.DIGITAL.units.add(Pair.of(unitList, 1073741824.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("mega byte");
        unitList.add("megabytes");
        unitList.add("mb");
        UnitType.DIGITAL.units.add(Pair.of(unitList, 1048576.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("kilo byte");
        unitList.add("kilobyte");
        unitList.add("kilobytes");
        unitList.add("kb");
        unitList.add("kbyte");
        UnitType.DIGITAL.units.add(Pair.of(unitList, 1024.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("byte");
        unitList.add("bytes");
        unitList.add("b");
        UnitType.DIGITAL.units.add(Pair.of(unitList, 1.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("bit");
        unitList.add("bits");
        UnitType.DIGITAL.units.add(Pair.of(unitList, 1. / 8));

        // FREQUENCY units are normalized to 1
        unitList = CollectionHelper.newArrayList();
        unitList.add("terra hertz");
        unitList.add("terrahertz");
        unitList.add("thz");
        UnitType.FREQUENCY.units.add(Pair.of(unitList, 1000000000000.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("giga hertz");
        unitList.add("gigahertz");
        unitList.add("ghz");
        UnitType.FREQUENCY.units.add(Pair.of(unitList, 1000000000.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("mega hertz");
        unitList.add("megahertz");
        unitList.add("mhz");
        UnitType.FREQUENCY.units.add(Pair.of(unitList, 1000000.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("kilo hertz");
        unitList.add("kilohertz");
        unitList.add("khz");
        UnitType.FREQUENCY.units.add(Pair.of(unitList, 1000.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("hertz");
        unitList.add("hz");
        UnitType.FREQUENCY.units.add(Pair.of(unitList, 1.0));

        // LENGTH units are normalized to centimeter
        unitList = CollectionHelper.newArrayList();
        unitList.add("miles");
        unitList.add("mile");
        unitList.add("mi");
        UnitType.LENGTH.units.add(Pair.of(unitList, 160934.4));

        unitList = CollectionHelper.newArrayList();
        unitList.add("kilometers");
        unitList.add("kilometres");
        unitList.add("kilometre");
        unitList.add("kilometer");
        unitList.add("kms");
        unitList.add("km");
        UnitType.LENGTH.units.add(Pair.of(unitList, 100000.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("meters");
        unitList.add("metres");
        unitList.add("meter");
        unitList.add("metre");
        unitList.add("m");
        UnitType.LENGTH.units.add(Pair.of(unitList, 100.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("decimeter");
        unitList.add("decimeters");
        unitList.add("decimetre");
        unitList.add("decimetres");
        unitList.add("dm");
        UnitType.LENGTH.units.add(Pair.of(unitList, 10.0));

        unitList = CollectionHelper.newArrayList();
        unitList.add("foot");
        unitList.add("feet");
        unitList.add("ft");
        UnitType.LENGTH.units.add(Pair.of(unitList, 30.48));

        unitList = CollectionHelper.newArrayList();
        unitList.add("inches");
        unitList.add("inch");
        unitList.add("in");
        unitList.add("\"");
        UnitType.LENGTH.units.add(Pair.of(unitList, 2.54));

        unitList = CollectionHelper.newArrayList();
        unitList.add("centimeters");
        unitList.add("centimetres");
        unitList.add("centimeter");
        unitList.add("centimetre");
        unitList.add("cm");
        UnitType.LENGTH.units.add(Pair.of(unitList, 1.));

        unitList = CollectionHelper.newArrayList();
        unitList.add("millimeters");
        unitList.add("millimetres");
        unitList.add("millimeter");
        unitList.add("millimetre");
        unitList.add("mm");
        UnitType.LENGTH.units.add(Pair.of(unitList, 0.1));

        // WEIGHT units are normalized to grams
        unitList = CollectionHelper.newArrayList();
        unitList.add("tons");
        unitList.add("ton");
        UnitType.WEIGHT.units.add(Pair.of(unitList, 1000000.));

        unitList = CollectionHelper.newArrayList();
        unitList.add("kilograms");
        unitList.add("kilogram");
        unitList.add("kg");
        unitList.add("kgs");
        UnitType.WEIGHT.units.add(Pair.of(unitList, 1000.));

        unitList = CollectionHelper.newArrayList();
        unitList.add("pounds");
        unitList.add("pound");
        unitList.add("lbs");
        unitList.add("lb");
        UnitType.WEIGHT.units.add(Pair.of(unitList, 453.59237));

        unitList = CollectionHelper.newArrayList();
        unitList.add("ounces");
        unitList.add("ounce");
        unitList.add("ozs");
        unitList.add("oz");
        UnitType.WEIGHT.units.add(Pair.of(unitList, 28.3495231));

        unitList = CollectionHelper.newArrayList();
        unitList.add("gram");
        unitList.add("grams");
        unitList.add("g");
        unitList.add("gs");
        unitList.add("gr");
        UnitType.WEIGHT.units.add(Pair.of(unitList, 1.));

        // AREA units are normalized to square meter
        unitList = CollectionHelper.newArrayList();
        unitList.add("square miles");
        unitList.add("square mile");
        unitList.add("sq.miles");
        unitList.add("sq miles");
        unitList.add("sq mi");
        UnitType.AREA.units.add(Pair.of(unitList, 2589988.11));

        unitList = CollectionHelper.newArrayList();
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

        unitList = CollectionHelper.newArrayList();
        unitList.add("hectares");
        unitList.add("hectare");
        UnitType.AREA.units.add(Pair.of(unitList, 10000.));

        unitList = CollectionHelper.newArrayList();
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

        // VOLUME units are normalized to milliliter
        unitList = CollectionHelper.newArrayList();
        unitList.add("m³");
        UnitType.VOLUME.units.add(Pair.of(unitList, 1000000.));

        unitList = CollectionHelper.newArrayList();
        unitList.add("gallons");
        unitList.add("gallon");
        unitList.add("gal");
        UnitType.VOLUME.units.add(Pair.of(unitList, 3785.41));

        unitList = CollectionHelper.newArrayList();
        unitList.add("liters");
        unitList.add("liter");
        unitList.add("l");
        UnitType.VOLUME.units.add(Pair.of(unitList, 1000.));

        unitList = CollectionHelper.newArrayList();
        unitList.add("quarts");
        unitList.add("quart");
        unitList.add("qts");
        unitList.add("qt");
        UnitType.VOLUME.units.add(Pair.of(unitList, 946.353));

        unitList = CollectionHelper.newArrayList();
        unitList.add("pints");
        unitList.add("pint");
        unitList.add("pts");
        unitList.add("pt");
        UnitType.VOLUME.units.add(Pair.of(unitList, 473.176));

        unitList = CollectionHelper.newArrayList();
        unitList.add("cups");
        unitList.add("cup");
        unitList.add("cp");
        unitList.add("c");
        UnitType.VOLUME.units.add(Pair.of(unitList, 236.588));

        unitList = CollectionHelper.newArrayList();
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

        unitList = CollectionHelper.newArrayList();
        unitList.add("tablespoons");
        unitList.add("tablespoon");
        unitList.add("Tbsps");
        unitList.add("tbsps");
        unitList.add("tbsp");
        unitList.add("T");
        UnitType.VOLUME.units.add(Pair.of(unitList, 14.7868));

        unitList = CollectionHelper.newArrayList();
        unitList.add("centiliters");
        unitList.add("centilitres");
        unitList.add("cl");
        UnitType.VOLUME.units.add(Pair.of(unitList, 10.));

        unitList = CollectionHelper.newArrayList();
        unitList.add("teaspoons");
        unitList.add("teaspoon");
        unitList.add("tsps");
        unitList.add("tsp");
        unitList.add("t");
        UnitType.VOLUME.units.add(Pair.of(unitList, 4.92892));

        unitList = CollectionHelper.newArrayList();
        unitList.add("milliliters");
        unitList.add("millilitres");
        unitList.add("milliliter");
        unitList.add("millilitre");
        unitList.add("mls");
        unitList.add("ml");
        unitList.add("mL");
        unitList.add("cm³");
        UnitType.VOLUME.units.add(Pair.of(unitList, 1.));

        unitList = CollectionHelper.newArrayList();
        // US 1.5 fl. oz (different depending on country)
        unitList.add("measures");
        unitList.add("measure");
        unitList.add("shots");
        unitList.add("shot");
        UnitType.VOLUME.units.add(Pair.of(unitList, 44.3603));

        // SPEED units are normalized to km/h
        unitList = CollectionHelper.newArrayList();
        unitList.add("miles per hour");
        unitList.add("mph");
        UnitType.SPEED.units.add(Pair.of(unitList, 1.609344));

        unitList = CollectionHelper.newArrayList();
        unitList.add("kilometers per hour");
        unitList.add("kmh");
        unitList.add("kph");
        UnitType.SPEED.units.add(Pair.of(unitList, 1.));

        // POWER_RATIO
        unitList = CollectionHelper.newArrayList();
        unitList.add("db");
        UnitType.POWER_RATIO.units.add(Pair.of(unitList, 1.));

        // OTHER units are normalized to different values
        unitList = CollectionHelper.newArrayList();
        unitList.add("kilowatts");
        unitList.add("kilowatt");
        unitList.add("kw");
        UnitType.OTHER.units.add(Pair.of(unitList, 1.3410));

        unitList = CollectionHelper.newArrayList();
        unitList.add("horsepower");
        unitList.add("horses");
        unitList.add("metric horsepower");
        unitList.add("bhp");
        unitList.add("hp");
        UnitType.OTHER.units.add(Pair.of(unitList, 1.));

        unitList = CollectionHelper.newArrayList();
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
        unitList.add("ft-lb");
        unitList.add("ft-lbs");
        UnitType.OTHER.units.add(Pair.of(unitList, 1.355817952));

        unitList = CollectionHelper.newArrayList();
        unitList.add("newton meters");
        unitList.add("newton meter");
        unitList.add("nm");
        UnitType.OTHER.units.add(Pair.of(unitList, 1.));

        // sort all unit names
        for (UnitType unitType : values()) {
            unitType.sortedUnitNames = CollectionHelper.newArrayList();

            for (Pair<List<String>, Double> pair : unitType.getUnits()) {
                for (String unit : pair.getLeft()) {
                    unitType.sortedUnitNames.add(unit);
                }
            }

            Collections.sort(unitType.sortedUnitNames, new StringLengthComparator());
        }

    }

    public List<Pair<List<String>, Double>> getUnits() {
        return units;
    }

    public List<String> getUnitNames() {
        return sortedUnitNames;
    }

}
