package ws.palladian.extraction.date.comparators;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.DateExactness;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;

public class DateComparatorTest {

    @Test
    public void testOrderDates() {
        ExtractedDate date1 = DateParser.parse("2010-09-01", RegExp.DATE_ISO8601_YMD.getFormat());
        ExtractedDate date2 = DateParser.parse("2005-09-01", RegExp.DATE_ISO8601_YMD.getFormat());
        ExtractedDate date3 = DateParser.parse("2010-07-21", RegExp.DATE_ISO8601_YMD.getFormat());
        ExtractedDate date4 = DateParser.parse("2010-07", RegExp.DATE_ISO8601_YM.getFormat());
        ExtractedDate date5 = DateParser.parse("2010-09-01", RegExp.DATE_ISO8601_YMD.getFormat());
        ExtractedDate date6 = DateParser.parse("2010-09-03", RegExp.DATE_ISO8601_YMD.getFormat());
        ExtractedDate date7 = DateParser.parse("2010-09-01T20:14:00", RegExp.DATE_ISO8601_YMD_T.getFormat());
        ExtractedDate date8 = DateParser.parse("2010-09-01T19:12:00", RegExp.DATE_ISO8601_YMD_T.getFormat());
        ExtractedDate date9 = DateParser.parse("2010-09-01T20:12:00", RegExp.DATE_ISO8601_YMD_T.getFormat());

        List<ExtractedDate> unordered = new ArrayList<ExtractedDate>();
        unordered.add(date1);
        unordered.add(date2);
        unordered.add(date8);
        unordered.add(date4);
        unordered.add(date5);
        unordered.add(date6);
        unordered.add(date7);
        unordered.add(date3);
        unordered.add(date9);
        unordered.add(date2);

        DateComparator comparator = new DateComparator();
        List<ExtractedDate> ordered = comparator.orderDates(unordered, true);

        // DateArrayHelper.printDateArray(unordered);
        // System.out.println("===============================================================================");
        // DateArrayHelper.printDateArray(ordered);

        assertEquals(date6.getDateString(), ordered.get(0).getDateString());
        assertEquals(date7.getDateString(), ordered.get(1).getDateString());
        assertEquals(date9.getDateString(), ordered.get(2).getDateString());
        assertEquals(date8.getDateString(), ordered.get(3).getDateString());
        assertEquals(date1.getDateString(), ordered.get(4).getDateString());
        assertEquals(date5.getDateString(), ordered.get(5).getDateString());
        assertEquals(date3.getDateString(), ordered.get(6).getDateString());
        assertEquals(date4.getDateString(), ordered.get(7).getDateString());
        assertEquals(date2.getDateString(), ordered.get(8).getDateString());
        assertEquals(date2.getDateString(), ordered.get(9).getDateString());
    }

//    /**
//     * <p>
//     * Trying to reproduce Comparator problem with Java 1.7
//     * </p>
//     * 
//     * @see https://bitbucket.org/palladian/palladian/issue/3/error-in-testgetdatetime
//     */
//    @Test
//    public void testComparisonContract() {
//        List<int[]> data = new ArrayList<int[]>();
//        data.add(new int[] {-1, 2, 2, -1, -1, -1});
//        data.add(new int[] {1950, 6, -1, -1, -1, -1});
//        data.add(new int[] {1951, 6, -1, -1, -1, -1});
//        data.add(new int[] {1951, 6, -1, -1, -1, -1});
//        data.add(new int[] {1951, 6, -1, -1, -1, -1});
//        data.add(new int[] {1951, 6, -1, -1, -1, -1});
//        data.add(new int[] {1952, 6, -1, -1, -1, -1});
//        data.add(new int[] {1952, 6, -1, -1, -1, -1});
//        data.add(new int[] {1953, 6, -1, -1, -1, -1});
//        data.add(new int[] {1953, 6, -1, -1, -1, -1});
//        data.add(new int[] {1954, 6, -1, -1, -1, -1});
//        data.add(new int[] {1955, 6, -1, -1, -1, -1});
//        data.add(new int[] {1955, 6, -1, -1, -1, -1});
//        data.add(new int[] {1947, 6, -1, -1, -1, -1});
//        data.add(new int[] {1956, 6, -1, -1, -1, -1});
//        data.add(new int[] {1956, 6, -1, -1, -1, -1});
//        data.add(new int[] {1956, 6, -1, -1, -1, -1});
//        data.add(new int[] {1957, 6, -1, -1, -1, -1});
//        data.add(new int[] {1958, 6, -1, -1, -1, -1});
//        data.add(new int[] {1957, 6, -1, -1, -1, -1});
//        data.add(new int[] {1958, 6, -1, -1, -1, -1});
//        data.add(new int[] {1959, 6, -1, -1, -1, -1});
//        data.add(new int[] {1960, 6, -1, -1, -1, -1});
//        data.add(new int[] {1959, 6, -1, -1, -1, -1});
//        data.add(new int[] {-1, 5, 14, -1, -1, -1});
//        data.add(new int[] {-1, 8, 1, -1, -1, -1});
//        data.add(new int[] {1959, 6, -1, -1, -1, -1});
//        data.add(new int[] {1959, 6, -1, -1, -1, -1});
//        data.add(new int[] {1934, 6, -1, -1, -1, -1});
//        data.add(new int[] {1950, 6, -1, -1, -1, -1});
//        data.add(new int[] {1934, 6, -1, -1, -1, -1});
//        data.add(new int[] {1932, 5, -1, -1, -1, -1});
//        data.add(new int[] {1970, 11, -1, -1, -1, -1});
//        data.add(new int[] {1960, 6, -1, -1, -1, -1});
//        data.add(new int[] {1970, 11, -1, -1, -1, -1});
//        data.add(new int[] {1970, 11, -1, -1, -1, -1});
//        data.add(new int[] {1934, 6, -1, -1, -1, -1});
//        data.add(new int[] {1950, 6, -1, -1, -1, -1});
//        data.add(new int[] {1960, 6, -1, -1, -1, -1});
//        data.add(new int[] {1995, 5, -1, -1, -1, -1});
//        data.add(new int[] {1995, 5, -1, -1, -1, -1});
//        data.add(new int[] {1995, 5, -1, -1, -1, -1});
//        data.add(new int[] {1958, 3, -1, -1, -1, -1});
//        data.add(new int[] {1961, 6, -1, -1, -1, -1});
//        data.add(new int[] {1961, 6, -1, -1, -1, -1});
//        data.add(new int[] {1962, 6, -1, -1, -1, -1});
//        data.add(new int[] {1962, 6, -1, -1, -1, -1});
//        data.add(new int[] {1963, 6, -1, -1, -1, -1});
//        data.add(new int[] {1963, 6, -1, -1, -1, -1});
//        data.add(new int[] {1964, 6, -1, -1, -1, -1});
//        data.add(new int[] {1964, 6, -1, -1, -1, -1});
//        data.add(new int[] {1964, 6, -1, -1, -1, -1});
//        data.add(new int[] {1965, 6, -1, -1, -1, -1});
//        data.add(new int[] {1965, 6, -1, -1, -1, -1});
//        data.add(new int[] {1963, 6, -1, -1, -1, -1});
//        data.add(new int[] {1967, 3, -1, -1, -1, -1});
//        data.add(new int[] {1928, 3, -1, -1, -1, -1});
//        data.add(new int[] {1928, 3, -1, -1, -1, -1});
//        data.add(new int[] {1952, 3, -1, -1, -1, -1});
//        data.add(new int[] {1967, 3, -1, -1, -1, -1});
//        data.add(new int[] {1930, 5, 21, -1, -1, -1});
//        data.add(new int[] {-1, 4, 1, -1, -1, -1});
//        data.add(new int[] {-1, 1, 7, -1, -1, -1});
//        data.add(new int[] {-1, 1, 7, -1, -1, -1});
//        data.add(new int[] {-1, 1, 8, -1, -1, -1});
//        List<ExtractedDate> extractedDates = new ArrayList<ExtractedDate>();
//        for (int[] d : data) {
//            ExtractedDate extractedDate = new ExtractedDate();
//            extractedDate.set(AbstractDate.YEAR, d[0]);
//            extractedDate.set(AbstractDate.MONTH, d[1]);
//            extractedDate.set(AbstractDate.DAY, d[2]);
//            extractedDate.set(AbstractDate.HOUR, d[3]);
//            extractedDate.set(AbstractDate.MINUTE, d[4]);
//            extractedDate.set(AbstractDate.SECOND, d[5]);
//            extractedDates.add(extractedDate);
//        }
//        Collections.sort(extractedDates, new DateComparator());
//    }
    
    @Test
    public void testCompareDepth() {
        DateExactness depth1 = DateExactness.YEAR;
        DateExactness depth2 = DateExactness.HOUR;
        DateExactness minDepth = DateExactness.getCommonExactness(depth1, depth2);
        assertEquals(DateExactness.YEAR, minDepth);
    }
}
