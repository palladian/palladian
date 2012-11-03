package ws.palladian.extraction.date.comparators;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.date.DateExactness;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;

public class DateComparatorTest {
    
    private ExtractedDate date1;
    private ExtractedDate date2;
    private ExtractedDate date3;
    private ExtractedDate date4;
    private ExtractedDate date5;
    private ExtractedDate date6;
    private ExtractedDate date7;
    private ExtractedDate date8;
    private ExtractedDate date9;
    private List<ExtractedDate> dates;

    @Before
    public void setUp() {
        date1 = DateParser.parseDate("2010-09-01", RegExp.DATE_ISO8601_YMD);
        date2 = DateParser.parseDate("2005-09-01", RegExp.DATE_ISO8601_YMD);
        date3 = DateParser.parseDate("2010-07-21", RegExp.DATE_ISO8601_YMD);
        date4 = DateParser.parseDate("2010-07", RegExp.DATE_ISO8601_YM);
        date5 = DateParser.parseDate("2010-09-01", RegExp.DATE_ISO8601_YMD);
        date6 = DateParser.parseDate("2010-09-03", RegExp.DATE_ISO8601_YMD);
        date7 = DateParser.parseDate("2010-09-01T20:14:00", RegExp.DATE_ISO8601_YMD_T);
        date8 = DateParser.parseDate("2010-09-01T19:12:00", RegExp.DATE_ISO8601_YMD_T);
        date9 = DateParser.parseDate("2010-09-01T20:12:00", RegExp.DATE_ISO8601_YMD_T);
        dates = new ArrayList<ExtractedDate>();
        dates.add(date1);
        dates.add(date2);
        dates.add(date8);
        dates.add(date4);
        dates.add(date5);
        dates.add(date6);
        dates.add(date7);
        dates.add(date3);
        dates.add(date9);
        dates.add(date2);
    }

    @Test
    public void testDateComparator1() {
        Collections.sort(dates, new DateComparator());
        assertEquals(date6.getDateString(), dates.get(0).getDateString());
        assertEquals(date7.getDateString(), dates.get(1).getDateString());
        assertEquals(date9.getDateString(), dates.get(2).getDateString());
        assertEquals(date8.getDateString(), dates.get(3).getDateString());
        assertEquals(date1.getDateString(), dates.get(4).getDateString());
        assertEquals(date5.getDateString(), dates.get(5).getDateString());
        assertEquals(date3.getDateString(), dates.get(6).getDateString());
        assertEquals(date4.getDateString(), dates.get(7).getDateString());
        assertEquals(date2.getDateString(), dates.get(8).getDateString());
        assertEquals(date2.getDateString(), dates.get(9).getDateString());
    }
    
    @Test
    public void testDateComparator2() {
        DateComparator comparator = new DateComparator(DateExactness.DAY);
        assertEquals(0, comparator.compare(date7, date8));
        assertEquals(-1, comparator.compare(date6, date7));
        
        comparator = new DateComparator(DateExactness.HOUR);
        assertEquals(0, comparator.compare(date7, date9));
        assertEquals(-1, comparator.compare(date7, date8));
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
