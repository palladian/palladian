package ws.palladian.helper.normalization;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.javatuples.Pair;
import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.StringLengthComparator;

/**
 * Test cases for the normalization.
 * 
 * @author David Urbansky
 */
public class NormalizationTest {

    @Test
    public void testSmartNormalization() {

        Pair<Double, List<String>> transformed = null;

        transformed = UnitNormalizer.smartTransform(0.5, UnitType.LENGTH);
        assertEquals("5.0mm", transformed.getValue0() + getShortest(transformed.getValue1()));

        transformed = UnitNormalizer.smartTransform(5000000., UnitType.WEIGHT);
        assertEquals("5.0ton", transformed.getValue0() + getShortest(transformed.getValue1()));

    }

    private String getShortest(List<String> list) {
        Collections.sort(list, new StringLengthComparator());
        return CollectionHelper.getLast(list);
    }

    @Test
    public void testNormalizeNumber() {
        assertEquals(StringNormalizer.normalizeNumber("30,000,000.00"), "30000000");
        assertEquals(StringNormalizer.normalizeNumber("30,000,000.10"), "30000000.1");
        assertEquals(StringNormalizer.normalizeNumber("30,000,000?"), "30000000?");
        assertEquals(StringNormalizer.normalizeNumber("30,000,000!"), "30000000!");
        assertEquals(StringNormalizer.normalizeNumber("30,000,000.004500"), "30000000.0045");
        assertEquals(StringNormalizer.normalizeNumber("30,234523000"), "30.234523");
        assertEquals(StringNormalizer.normalizeNumber("4,07000"), "4.07");
        assertEquals(StringNormalizer.normalizeNumber("4.4560000"), "4.456");
        assertEquals(StringNormalizer.normalizeNumber("7,500,000"), "7500000");
        assertEquals(StringNormalizer.normalizeNumber("7,500,400"), "7500400");
        assertEquals(StringNormalizer.normalizeNumber("1990"), "1990");
    }

    @Test
    public void testGetUnitType() {

        assertEquals(UnitType.WEIGHT, UnitNormalizer.getUnitType("g"));
        assertEquals(UnitType.VOLUME, UnitNormalizer.getUnitType("tablespoon"));

    }

    @Test
    public void testDetectUnit() {

        assertEquals("ghz", UnitNormalizer.detectUnit("8 in ghz"));
        assertEquals("hz", UnitNormalizer.detectUnit("8 hz"));
        assertEquals("mhz", UnitNormalizer.detectUnit("mhz"));
        assertEquals("cm", UnitNormalizer.detectUnit("2cm- up to 8"));
        assertEquals("kilobytes", UnitNormalizer.detectUnit("how much are 100 kilobytes"));
        assertEquals("kilometers", UnitNormalizer.detectUnit("kilometers"));
        assertEquals("miles", UnitNormalizer.detectUnit("1.5miles"));
        assertEquals("liters", UnitNormalizer.detectUnit("2 liters of milk"));
        assertEquals("g", UnitNormalizer.detectUnit("2g"));
    }

    @Test
    public void testGetNormalizedNumber() {

        assertEquals(8.89, UnitNormalizer.getNormalizedNumber("3.5\""), 0.1);
        assertEquals(20.0, UnitNormalizer.getNormalizedNumber("2cl"), 2);
        assertEquals(1600000000.0, UnitNormalizer.getNormalizedNumber(1.6, " GHz, 1024MB RAM"), 2);
        assertEquals(14.785, UnitNormalizer.getNormalizedNumber(0.5, "fluid ounce"), 0.1);
        assertEquals(UnitNormalizer.getNormalizedNumber(1, "measure"), 44.3603, 0.1);
        assertEquals(UnitNormalizer.getNormalizedNumber(2.5, "shots"), 110.9, 0.1);

        assertEquals(214, UnitNormalizer.getNormalizedNumber("214 pixel [1]"), 1);
        assertEquals(214, UnitNormalizer.getNormalizedNumber("214pixel [1]"), 1);

        // System.out.println(MathHelper.getNormalizedNumber(3800,"thousand square miles"));
        assertEquals(UnitNormalizer.getNormalizedNumber(21.4, " million. [1]"), 21400000.0, 1);
        assertEquals(UnitNormalizer.getNormalizedNumber(13, " per thousand asdf asdfisdf "), 1.3, 1);
        assertEquals(UnitNormalizer.getNormalizedNumber(13, " per thousand. asdf asdfisdf "), 1.3, 1);
        assertEquals(UnitNormalizer.getNormalizedNumber(13, " per 1000 asdf asdfisdf "), 1.3, 1);
        // TODO?
        // assertEquals(UnitNormalizer.getNormalizedNumber(80,"'GB'))"),85899345920.0);
        assertEquals(UnitNormalizer.getNormalizedNumber(2, " hr. 32 min."), 9120.0, 0);
        // assertEquals(UnitNormalizer.getNormalizedNumber(13.3, "\" adf fs"), 33.782);
        assertEquals(UnitNormalizer.getNormalizedNumber(6, "' 2''"), 187.96, 2);
        assertEquals(UnitNormalizer.getNormalizedNumber(6, "'2\""), 187.96, 2);
        assertEquals(UnitNormalizer.getNormalizedNumber(7.5, "\""), 18.75, 2);
        assertEquals(UnitNormalizer.getNormalizedNumber(5, "hours 4 minutes 6seconds"), 18246.0, 0);
        assertEquals(UnitNormalizer.getNormalizedNumber(6, " h 30 min"), 23400.0, 0);
        assertEquals(UnitNormalizer.getNormalizedNumber(5, "ft 9 inches"), 175.26, 2);
        assertEquals(UnitNormalizer.getNormalizedNumber(5, "\""), 12.7, 0);
        assertEquals(UnitNormalizer.getNormalizedNumber(2, "mb 4 GB"), 2097152.0, 0);
        assertEquals(UnitNormalizer.getNormalizedNumber(2, "mb 2mb"), 2097152.0, 0);
        assertEquals(UnitNormalizer.getNormalizedNumber(2, "mb 100kb"), 2199552.0, 0);
        assertEquals(UnitNormalizer.getNormalizedNumber(5, "mpixel"), 5000000.0, 0);
        assertEquals(UnitNormalizer.getNormalizedNumber(30, "miles per hour is really fast"), 48.28, 20);
        assertEquals(UnitNormalizer.getNormalizedNumber(20, "m kilometers"), 2000.0, 0);
        assertEquals(UnitNormalizer.getNormalizedNumber(53.4, "million, compared to"), 53400000.0, 0);
        assertEquals(UnitNormalizer.getNormalizedNumber(125, "ft-lbs torque!!!"), 169.477, 3);
        assertEquals(UnitNormalizer.getNormalizedNumber(125, "lb-ft torque, and power speed"), 169.477, 3);
        assertEquals(UnitNormalizer.getNormalizedNumber(125, ""), 125.0, 0);
        assertEquals(UnitNormalizer.getNormalizedNumber(1, "min 20s 23sdf sdf a__:"), 80.0, 0);
        assertEquals(UnitNormalizer.getNormalizedNumber(1, "hour 30 minutes 20sdf"), 5400.0, 0);
        assertEquals(UnitNormalizer.getNormalizedNumber(5, "ft 9 in 20sdf"), 175.26, 0);
        assertEquals(UnitNormalizer.getNormalizedNumber(1, "m20s 23sdf sdf a__:"), 80.0, 0);
        assertEquals(UnitNormalizer.getNormalizedNumber(1, ":20 23sdf sdf a__:"), 80.0, 0);
        // assertEquals(UnitNormalizer.getNormalizedNumber(3800, "thousand square miles"), 9841954818000.0, 0);
        assertEquals(UnitNormalizer.getNormalizedNumber(46, "% (2008)"), 0.46, 0);

    }

    @Test
    public void testHandleSpecialFormat() {
        assertEquals(UnitNormalizer.handleSpecialFormat(6.0, "' 2'',", 3), 187.96, 0);
        assertEquals(UnitNormalizer.handleSpecialFormat(5, "' 9''", 3), 175.26, 0);
        assertEquals(UnitNormalizer.handleSpecialFormat(5, "'9''", 3), 175.26, 0);
        assertEquals(UnitNormalizer.handleSpecialFormat(5, "' 9\"", 3), 175.26, 0);
        assertEquals(UnitNormalizer.handleSpecialFormat(5, "'9\"", 3), 175.26, 0);
        assertEquals(UnitNormalizer.handleSpecialFormat(0, ":59", 3), 59.0, 0);
        assertEquals(UnitNormalizer.handleSpecialFormat(2, ":44", 3), 164.0, 0);
        assertEquals(UnitNormalizer.handleSpecialFormat(4, ":2:40", 3), 14560.0, 0);
        assertEquals(UnitNormalizer.handleSpecialFormat(4, ":02:40", 3), 14560.0, 0);
        assertEquals(UnitNormalizer.handleSpecialFormat(4, ":20:40", 3), 15640.0, 0);
        assertEquals(UnitNormalizer.handleSpecialFormat(1, "h 20m 40s", 3), 4840.0, 0);
        assertEquals(UnitNormalizer.handleSpecialFormat(2, "m 40s", 3), 160.0, 0);
        assertEquals(UnitNormalizer.handleSpecialFormat(1, "h20m40s", 3), 4840.0, 0);
        assertEquals(UnitNormalizer.handleSpecialFormat(1, "h20m", 3), 4800.0, 0);
        assertEquals(UnitNormalizer.handleSpecialFormat(2, "m40s", 3), 160.0, 0);
    }

    @Test
    public void testUnitSameAndBigger() {
        assertEquals(UnitNormalizer.unitsSameType("gb", "mb"), true);
        assertEquals(UnitNormalizer.unitsSameType("minute", "mb"), false);
        assertEquals(UnitNormalizer.isBigger("minute", "second"), true);
    }

}