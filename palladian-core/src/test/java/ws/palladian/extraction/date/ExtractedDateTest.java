package ws.palladian.extraction.date;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.extraction.date.comparators.DateComparator;
import ws.palladian.helper.date.dates.DateExactness;
import ws.palladian.helper.date.dates.ExtractedDate;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;

public class ExtractedDateTest {

    private ExtractedDate date1;
    private ExtractedDate date2;
    private ExtractedDate date3;
    private ExtractedDate date4;
    private ExtractedDate date5;
    private ExtractedDate date6;
    private ExtractedDate date7;
    private ExtractedDate date8;
    private ExtractedDate date9;
    private ExtractedDate date10;
    private ExtractedDate date11;
    private ExtractedDate date12;
    private ExtractedDate date13;
    private ExtractedDate date14;
    private ExtractedDate date15;
    private ExtractedDate date16;
    private ExtractedDate date17;
    private ExtractedDate date18;

    @Before
    public void setUp() throws Exception {
        date1 = new ExtractedDate("2010-06-12", "yyyy-mm-dd");
        date2 = new ExtractedDate("10-06-07", "yyyy-mm-dd");
        date3 = new ExtractedDate("07.06.2010", "dd.mm.yyyy");
        date4 = new ExtractedDate("07.06.10", "dd.mm.yyyy");
        date5 = new ExtractedDate("06/07/2010", "mm/dd/yyyy");
        date6 = new ExtractedDate("06/07/10", "mm/dd/yyyy");
        date7 = new ExtractedDate("07. June 2010", "dd. mmmm yyyy");
        date8 = new ExtractedDate("June 07, 2010", "mmmm dd, yyyy");
        date9 = new ExtractedDate("07. June '10", "dd. mmmm yyyy");
        date10 = new ExtractedDate("2010_06_07", "yyyy_mm_dd");
        date11 = new ExtractedDate("2010.06.07", "yyyy_mm_dd");
        date12 = new ExtractedDate("2010/06/07", "yyyy_mm_dd");
        date13 = new ExtractedDate("june 10", "MMMM YYYY");
        date14 = new ExtractedDate("june 2010", "MMMM YYYY");
        date15 = new ExtractedDate("june '10", "MMMM YYYY");
        date16 = new ExtractedDate("mon, 07 jun 2010 07:06:05 GMT", "WD, DD MMM YYYY HH:MM:SS TZ");
        date17 = new ExtractedDate("Mondy, 07-jun-10 07:06:05 GMT", "WWD, DD-MMM-YY HH:MM:SS TZ");
        date18 = new ExtractedDate("mon jun 7 07:06:05 2010", "WD MMM DD_1 HH:MM:SS YYYY");
    }

    @Test
    public void testGetNormalizedDateString() throws Exception {
        assertEquals(date1.getDateString(), "2010-06-12", date1.getNormalizedDateString());
        assertEquals(date2.getDateString(), "2010-06-07", date2.getNormalizedDateString());
        assertEquals(date3.getDateString(), "2010-06-07", date3.getNormalizedDateString());
        assertEquals(date4.getDateString(), "2010-06-07", date4.getNormalizedDateString());
        assertEquals(date5.getDateString(), "2010-06-07", date5.getNormalizedDateString());
        assertEquals(date6.getDateString(), "2010-06-07", date6.getNormalizedDateString());
        assertEquals(date7.getDateString(), "2010-06-07", date7.getNormalizedDateString());
        assertEquals(date8.getDateString(), "2010-06-07", date8.getNormalizedDateString());
        assertEquals(date9.getDateString(), "2010-06-07", date9.getNormalizedDateString());
        assertEquals(date10.getDateString(), "2010-06-07", date10.getNormalizedDateString());
        assertEquals(date11.getDateString(), "2010-06-07", date11.getNormalizedDateString());
        assertEquals(date12.getDateString(), "2010-06-07", date12.getNormalizedDateString());
        assertEquals(date13.getDateString(), "2010-06", date13.getNormalizedDateString());
        assertEquals(date14.getDateString(), "2010-06", date14.getNormalizedDateString());
        assertEquals(date15.getDateString(), "2010-06", date15.getNormalizedDateString());
        assertEquals(date16.getDateString(), "2010-06-07 07:06:05", date16.getNormalizedDateString());
        assertEquals(date17.getDateString(), "2010-06-07 07:06:05", date17.getNormalizedDateString());
        assertEquals(date18.getDateString(), "2010-06-07 07:06:05", date18.getNormalizedDateString());
    }

    @Test
    public void testGetNormalizedDate() throws Exception {
    	Calendar c = new GregorianCalendar();
    	c.set(2010, 5 ,12, 0, 0, 0);
        assertEquals(date1.getDateString(), c.getTime().toString(), date1.getNormalizedDate().toString());
        
        c.set(2010, 5 ,7, 0, 0, 0);
        assertEquals(date2.getDateString(), c.getTime().toString(), date2.getNormalizedDate().toString());
        assertEquals(date3.getDateString(), c.getTime().toString(), date3.getNormalizedDate().toString());
        assertEquals(date4.getDateString(), c.getTime().toString(), date4.getNormalizedDate().toString());
        assertEquals(date5.getDateString(), c.getTime().toString(), date5.getNormalizedDate().toString());
        assertEquals(date6.getDateString(), c.getTime().toString(), date6.getNormalizedDate().toString());
        assertEquals(date7.getDateString(), c.getTime().toString(), date7.getNormalizedDate().toString());
        assertEquals(date8.getDateString(), c.getTime().toString(), date8.getNormalizedDate().toString());
        assertEquals(date9.getDateString(), c.getTime().toString(), date9.getNormalizedDate().toString());
        assertEquals(date10.getDateString(), c.getTime().toString(), date10.getNormalizedDate().toString());
        assertEquals(date11.getDateString(), c.getTime().toString(), date11.getNormalizedDate().toString());
        assertEquals(date12.getDateString(), c.getTime().toString(), date12.getNormalizedDate().toString());
        
        c.set(2010, 5 ,1, 0, 0, 0);
        assertEquals(date13.getDateString(), c.getTime().toString(), date13.getNormalizedDate().toString());
        assertEquals(date14.getDateString(), c.getTime().toString(), date14.getNormalizedDate().toString());
        assertEquals(date15.getDateString(), c.getTime().toString(), date15.getNormalizedDate().toString());
        
        c.set(2010, 5 ,7, 7, 6, 5);
        assertEquals(date16.getDateString(), c.getTime().toString(), date16.getNormalizedDate().toString());
        assertEquals(date17.getDateString(), c.getTime().toString(), date17.getNormalizedDate().toString());
        assertEquals(date18.getDateString(), c.getTime().toString(), date18.getNormalizedDate().toString());
    }

    
    @Test
    public void testSetDateParts() {
        assertEquals(2010, date1.get(ExtractedDate.YEAR));
        assertEquals(6, date1.get(ExtractedDate.MONTH));
        assertEquals(12, date1.get(ExtractedDate.DAY));
        assertEquals(-1, date15.get(ExtractedDate.DAY));
        assertEquals(7, date16.get(ExtractedDate.HOUR));
        assertEquals(6, date16.get(ExtractedDate.MINUTE));
        assertEquals(5, date16.get(ExtractedDate.SECOND));
        assertEquals(7, date17.get(ExtractedDate.HOUR));
        assertEquals(6, date17.get(ExtractedDate.MINUTE));
        assertEquals(5, date17.get(ExtractedDate.SECOND));
        assertEquals(7, date18.get(ExtractedDate.HOUR));
        assertEquals(6, date18.get(ExtractedDate.MINUTE));
        assertEquals(5, date18.get(ExtractedDate.SECOND));
    }
    
    @Test
    public void testGetExactness() {
        assertEquals(DateExactness.DAY, date1.getExactness());
        assertEquals(DateExactness.DAY, date2.getExactness());
        assertEquals(DateExactness.DAY, date3.getExactness());
        assertEquals(DateExactness.DAY, date4.getExactness());
        assertEquals(DateExactness.DAY, date5.getExactness());
        assertEquals(DateExactness.DAY, date6.getExactness());
        assertEquals(DateExactness.DAY, date7.getExactness());
        assertEquals(DateExactness.DAY, date8.getExactness());
        assertEquals(DateExactness.DAY, date9.getExactness());
        assertEquals(DateExactness.DAY, date10.getExactness());
        assertEquals(DateExactness.DAY, date11.getExactness());
        assertEquals(DateExactness.DAY, date12.getExactness());
        assertEquals(DateExactness.MONTH, date13.getExactness());
        assertEquals(DateExactness.MONTH, date14.getExactness());
        assertEquals(DateExactness.MONTH, date15.getExactness());
        assertEquals(DateExactness.SECOND, date16.getExactness());
        assertEquals(DateExactness.SECOND, date17.getExactness());
        assertEquals(DateExactness.SECOND, date18.getExactness());
    }
    
    @Test
    public void testGetDifference() {
        assertEquals(432000, date1.getDifference(date2, TimeUnit.SECONDS), 0);
        assertEquals(7200, date1.getDifference(date2, TimeUnit.MINUTES), 0);
        assertEquals(120, date1.getDifference(date2, TimeUnit.HOURS), 0);
        assertEquals(5, date1.getDifference(date2, TimeUnit.DAYS), 0);
    }
    
}
