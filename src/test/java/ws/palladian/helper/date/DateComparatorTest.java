package ws.palladian.helper.date;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

import ws.palladian.extraction.date.dates.ExtractedDate;
import ws.palladian.helper.RegExp;
import ws.palladian.helper.date.DateArrayHelper;
import ws.palladian.helper.date.DateComparator;

public class DateComparatorTest {

    @Test
    public void testOrderDates() {
        ExtractedDate date1 = new ExtractedDate("2010-09-01", RegExp.DATE_ISO8601_YMD[1]);
        ExtractedDate date2 = new ExtractedDate("2005-09-01", RegExp.DATE_ISO8601_YMD[1]);
        ExtractedDate date3 = new ExtractedDate("2010-07-21", RegExp.DATE_ISO8601_YMD[1]);
        ExtractedDate date4 = new ExtractedDate("2010-07", RegExp.DATE_ISO8601_YM[1]);
        ExtractedDate date5 = new ExtractedDate("2010-09-01", RegExp.DATE_ISO8601_YMD[1]);
        ExtractedDate date6 = new ExtractedDate("2010-09-03", RegExp.DATE_ISO8601_YMD[1]);
        ExtractedDate date7 = new ExtractedDate("2010-09-01T20:14:00", RegExp.DATE_ISO8601_YMD_T[1]);
        ExtractedDate date8 = new ExtractedDate("2010-09-01T19:12:00", RegExp.DATE_ISO8601_YMD_T[1]);
        ExtractedDate date9 = new ExtractedDate("2010-09-01T20:12:00", RegExp.DATE_ISO8601_YMD_T[1]);

        ArrayList<ExtractedDate> unorderd = new ArrayList<ExtractedDate>();
        unorderd.add(date1);// 1
        unorderd.add(date2);// 2
        unorderd.add(date8);// 3
        unorderd.add(date4);// 4
        unorderd.add(date5);// 5
        unorderd.add(date6);// 6
        unorderd.add(date7);// 7
        unorderd.add(date3);// 8
        unorderd.add(date9);// 9
        unorderd.add(date2);// 10

        ArrayList<ExtractedDate> orderd = new ArrayList<ExtractedDate>();
        DateComparator dc = new DateComparator();
        orderd = dc.orderDates(unorderd, true);
        DateArrayHelper.printDateArray(unorderd);
        System.out.println("===============================================================================");
        DateArrayHelper.printDateArray(orderd);
        assertEquals(orderd.get(0).getDateString(), unorderd.get(5).getDateString());
        assertEquals(orderd.get(1).getDateString(), unorderd.get(6).getDateString());
        assertEquals(orderd.get(2).getDateString(), unorderd.get(8).getDateString());
        assertEquals(orderd.get(3).getDateString(), unorderd.get(2).getDateString());
        assertEquals(orderd.get(4).getDateString(), unorderd.get(0).getDateString());
        assertEquals(orderd.get(5).getDateString(), unorderd.get(4).getDateString());
        assertEquals(orderd.get(6).getDateString(), unorderd.get(7).getDateString());
        assertEquals(orderd.get(7).getDateString(), unorderd.get(3).getDateString());
        assertEquals(orderd.get(8).getDateString(), unorderd.get(1).getDateString());
        assertEquals(orderd.get(9).getDateString(), unorderd.get(9).getDateString());

    }
}
