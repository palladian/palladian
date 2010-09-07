package tud.iir.helper;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Ignore;
import org.junit.Test;

import tud.iir.control.AllTests;
import tud.iir.daterecognition.DateGetter;
import tud.iir.daterecognition.dates.ExtractedDate;

public class DateArrayHelperTest {

    @Test
    public void testFilter() {
        final String url = "data/test/webPages/dateExtraction/zeit1.htm";
        if (!AllTests.ALL_TESTS) {
            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setAllTrue();
            dateGetter.setTechArchive(false);
            dateGetter.setTechReference(false);
            ArrayList<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            ArrayList<ExtractedDate> filter = DateArrayHelper.filter(date, ExtractedDate.TECH_HTML_CONT);
            assertEquals(6, filter.size());
            filter = DateArrayHelper.filter(date, ExtractedDate.TECH_HTML_STRUC);
            assertEquals(4, filter.size());
        }
    }

    @Test
    public void testArrangeByDate() {
        ArrayList<ExtractedDate> array = new ArrayList<ExtractedDate>();
        ExtractedDate date = new ExtractedDate("2010-08-01", "YYYY-MM-DD");
        array.add(date);
        date = new ExtractedDate("2010-08-02", "YYYY-MM-DD");
        array.add(date);
        date = new ExtractedDate("2010-08-03", "YYYY-MM-DD");
        array.add(date);
        date = new ExtractedDate("2010-08-04", "YYYY-MM-DD");
        array.add(date);
        date = new ExtractedDate("2010-08-05", "YYYY-MM-DD");
        array.add(date);
        date = new ExtractedDate("2010-08-03", "YYYY-MM-DD");
        array.add(date);
        date = new ExtractedDate("2010-08-04", "YYYY-MM-DD");
        array.add(date);
        date = new ExtractedDate("2010-08-05", "YYYY-MM-DD");
        array.add(date);
        date = new ExtractedDate("2010-08-05", "YYYY-MM-DD");
        array.add(date);
        date = new ExtractedDate("2010-08-05", "YYYY-MM-DD");
        array.add(date);
        date = new ExtractedDate("2010-08-04", "YYYY-MM-DD");
        array.add(date);
        date = new ExtractedDate("2010-08-03", "YYYY-MM-DD");
        array.add(date);
        date = new ExtractedDate("2010-08-05", "YYYY-MM-DD");
        array.add(date);
        date = new ExtractedDate("2010-08-02", "YYYY-MM-DD");
        array.add(date);
        date = new ExtractedDate("2010-08-04", "YYYY-MM-DD");
        array.add(date);
        ArrayList<ArrayList<ExtractedDate>> arrangedArray = DateArrayHelper.arrangeByDate(array);
        DateComparator dc = new DateComparator();
        for (int i = 0; i < 5; i++) {
            // ExtractedDateHelper.printDateArray(arrangedArray.get(i));
            // one time 2010-08-01, two time 2010-08-02, three time 2010-08-03
            assertEquals(i + 1, arrangedArray.get(i).size());

            for (int j = 0; j < arrangedArray.get(i).size() - 1; j++) {
                int compare = dc.compare(arrangedArray.get(i).get(j), arrangedArray.get(i).get(j + 1));
                assertEquals(0, compare);
            }
        }
    }

    @Ignore
    @Test
    public void testArrangeByDate2() {
        final String url = "data/test/webPages/dateExtraction/kullin.htm";
        if (!AllTests.ALL_TESTS) {
            ArrayList<ExtractedDate> date = new ArrayList<ExtractedDate>();
            DateGetter dateGetter = new DateGetter(url);
            dateGetter.setAllTrue();
            dateGetter.setTechArchive(false);
            dateGetter.setTechReference(false);
            ArrayList<ExtractedDate> dates = dateGetter.getDate();
            date.addAll(dates);
            ArrayList<ArrayList<ExtractedDate>> arrangedArray = DateArrayHelper.arrangeByDate(date);
            for (int i = 0; i < arrangedArray.size(); i++) {
                System.out.println("==============================================================================");
                DateArrayHelper.printDateArray(arrangedArray.get(i));
            }
        }
    }

}
