package ws.palladian.extraction.date.helper;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ws.palladian.helper.date.DateExactness;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.date.ExtractedDateHelper;
import ws.palladian.helper.date.dates.ArchiveDate;
import ws.palladian.helper.date.dates.ContentDate;
import ws.palladian.helper.date.dates.KeywordDate;
import ws.palladian.helper.date.dates.StructureDate;
import ws.palladian.helper.date.dates.UrlDate;

public class DateArrayHelperTest {

    @Test
    public void testFilter() throws FileNotFoundException {
        List<ExtractedDate> dates = new ArrayList<ExtractedDate>();
        
        ExtractedDate now = ExtractedDateHelper.getCurrentDate();
        dates.add(now);
        dates.add(new ContentDate(now));
        dates.add(new UrlDate(now, "http://www.example.com"));
        dates.add(new ContentDate(now));
        dates.add(new ContentDate(now));
        dates.add(new ArchiveDate(now));
        dates.add(new StructureDate(now));
        
        assertEquals(7, dates.size());
        assertEquals(3, DateArrayHelper.filter(dates, ContentDate.class).size());
        assertEquals(1, DateArrayHelper.filter(dates, ArchiveDate.class).size());
        assertEquals(4, DateArrayHelper.filter(dates, KeywordDate.class).size());
        assertEquals(7, DateArrayHelper.filter(dates, ExtractedDate.class).size());
    }

    @Test
    public void testCluster() {
        List<ExtractedDate> dates = new ArrayList<ExtractedDate>();
        ExtractedDate date1 = DateParser.parse("2010-08-01", "YYYY-MM-DD");
        dates.add(date1);
        ExtractedDate date2 = DateParser.parse("2010-08-02", "YYYY-MM-DD");
        dates.add(date2);
        ExtractedDate date3 = DateParser.parse("2010-08-03", "YYYY-MM-DD");
        dates.add(date3);
        ExtractedDate date4 = DateParser.parse("2010-08-04", "YYYY-MM-DD");
        dates.add(date4);
        ExtractedDate date5 = DateParser.parse("2010-08-05", "YYYY-MM-DD");
        dates.add(date5);
        ExtractedDate date6 = DateParser.parse("2010-08-03", "YYYY-MM-DD");
        dates.add(date6);
        ExtractedDate date7 = DateParser.parse("2010-08-04", "YYYY-MM-DD");
        dates.add(date7);
        ExtractedDate date8 = DateParser.parse("2010-08-05", "YYYY-MM-DD");
        dates.add(date8);
        ExtractedDate date9 = DateParser.parse("2010-08-05", "YYYY-MM-DD");
        dates.add(date9);
        ExtractedDate date10 = DateParser.parse("2010-08-05", "YYYY-MM-DD");
        dates.add(date10);
        ExtractedDate date11 = DateParser.parse("2010-08-04", "YYYY-MM-DD");
        dates.add(date11);
        ExtractedDate date12 = DateParser.parse("2010-08-03", "YYYY-MM-DD");
        dates.add(date12);
        ExtractedDate date13 = DateParser.parse("2010-08-05", "YYYY-MM-DD");
        dates.add(date13);
        ExtractedDate date14 = DateParser.parse("2010-08-02", "YYYY-MM-DD");
        dates.add(date14);
        ExtractedDate date15 = DateParser.parse("2010-08-04", "YYYY-MM-DD");
        dates.add(date15);
        
        List<List<ExtractedDate>> clusters = DateArrayHelper.cluster(dates, DateExactness.DAY);
        
        assertEquals(5, clusters.size());
        
        // 1st cluster, contains 1 x "2010-08-01"
        assertEquals(1, clusters.get(0).size());
        assertEquals(date1, clusters.get(0).get(0));
        
        // 2nd cluster, contains 2 x "2010-08-02"
        assertEquals(2, clusters.get(1).size());
        assertEquals(date2, clusters.get(1).get(0));
        assertEquals(date14, clusters.get(1).get(1));
        
        // 3rd cluster, contains 3 x "2010-08-03"
        assertEquals(3, clusters.get(2).size());
        assertEquals(date3, clusters.get(2).get(0));
        assertEquals(date6, clusters.get(2).get(1));
        assertEquals(date12, clusters.get(2).get(2));
        
        // 4th cluster, contains 4 x "2010-08-04"
        assertEquals(4, clusters.get(3).size());
        assertEquals(date4, clusters.get(3).get(0));
        assertEquals(date7, clusters.get(3).get(1));
        assertEquals(date11, clusters.get(3).get(2));
        assertEquals(date15, clusters.get(3).get(3));
        
        // 5th cluster, contains 5 x "2010-08-05"
        assertEquals(5, clusters.get(4).size());
        assertEquals(date5, clusters.get(4).get(0));
        assertEquals(date8, clusters.get(4).get(1));
        assertEquals(date9, clusters.get(4).get(2));
        assertEquals(date10, clusters.get(4).get(3));
        assertEquals(date13, clusters.get(4).get(4));
    }

}
