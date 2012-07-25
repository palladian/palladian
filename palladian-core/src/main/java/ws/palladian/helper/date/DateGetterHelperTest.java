package ws.palladian.helper.date;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.helper.date.dates.DateParser;

public class DateGetterHelperTest {
    
    @Test
    public void testFindAllDates() {
        assertEquals("2010-01", DateGetterHelper.findAllDates("Januar 2010").get(0).getNormalizedDateString());
        assertEquals("2010-02", DateGetterHelper.findAllDates("Februar 2010").get(0).getNormalizedDateString());
        assertEquals("2010-03", DateGetterHelper.findAllDates("März 2010").get(0).getNormalizedDateString());
        assertEquals("2010-04", DateGetterHelper.findAllDates("April 2010").get(0).getNormalizedDateString());
        assertEquals("2010-05", DateGetterHelper.findAllDates("Mai 2010").get(0).getNormalizedDateString());
        assertEquals("2010-06", DateGetterHelper.findAllDates("Juni 2010").get(0).getNormalizedDateString());
        assertEquals("2010-07", DateGetterHelper.findAllDates("Juli 2010").get(0).getNormalizedDateString());
        assertEquals("2010-08", DateGetterHelper.findAllDates("August 2010").get(0).getNormalizedDateString());
        assertEquals("MMMM YYYY", DateGetterHelper.findAllDates("August 2010").get(0).getFormat());
        assertEquals("2010-09", DateGetterHelper.findAllDates("September 2010").get(0).getNormalizedDateString());
        assertEquals("2010-10", DateGetterHelper.findAllDates("Oktober 2010").get(0).getNormalizedDateString());
        assertEquals("2010-11", DateGetterHelper.findAllDates("November 2010").get(0).getNormalizedDateString());
        assertEquals("2010-12", DateGetterHelper.findAllDates("Dezember 2010").get(0).getNormalizedDateString());
        String date = DateGetterHelper.findAllDates("SEPTEMBER 1, 2010").get(0).getNormalizedDateString();
        assertEquals("2010-09-01", date);
        date = DateGetterHelper.findAllDates(", 17/09/06 03:51:53").get(0).getNormalizedDateString();
        assertEquals("2006-09-17 03:51:53", date);
        date = DateGetterHelper.findAllDates("30.09.2010").get(0).getNormalizedDateString();
        assertEquals("2010-09-30", date);
        date = DateGetterHelper.findAllDates(", 08. Februar 2010, 17:15").get(0).getNormalizedDateString();
        assertEquals("2010-02-08", date);
        date = DateGetterHelper.findAllDates("Sept. 3, 2010").get(0).getNormalizedDateString();
        assertEquals("2010-09-03", date);
        date = DateGetterHelper.findAllDates("Last Modified: Wednesday, 11-Aug-2010 14:41:10 EDT").get(0)
                .getNormalizedDateString();
        assertEquals("2010-08-11 14:41:10", date);
        date = DateGetterHelper.findAllDates("JUNE 1, 2010").get(0).getNormalizedDateString();
        assertEquals("2010-06-01", date);
        assertEquals("2010-01", DateGetterHelper.findAllDates("jan. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-02", DateGetterHelper.findAllDates("Feb. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-03", DateGetterHelper.findAllDates("Mär. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-04", DateGetterHelper.findAllDates("Apr. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-05", DateGetterHelper.findAllDates("Mai. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-06", DateGetterHelper.findAllDates("Jun. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-07", DateGetterHelper.findAllDates("Jul. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-08", DateGetterHelper.findAllDates("Aug. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-09", DateGetterHelper.findAllDates("Sep. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-10", DateGetterHelper.findAllDates("Okt. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-11", DateGetterHelper.findAllDates("nov. 2010").get(0).getNormalizedDateString());
        assertEquals("2010-12", DateGetterHelper.findAllDates("Dez. 2010").get(0).getNormalizedDateString());
        assertEquals("2007-12-06 17:37:45", DateGetterHelper.findAllDates("2007-12-06T17:37:45Z").get(0)
                .getNormalizedDateString());
        assertEquals("2008-12-06 17:37:45",
                DateGetterHelper.findAllDates("2007-12-06T17:37:45Z 2008-12-06T17:37:45Z").get(1)
                        .getNormalizedDateString());
        assertEquals("2008-09-20", DateGetterHelper.findAllDates("Saturday, September 20, 2008").get(0)
                .getNormalizedDateString());
        assertEquals("2011-04-18 16:16:00", DateParser.findDate("Mon, 18 Apr 2011 09:16:00 GMT-0700").getNormalizedDateString());
        assertEquals("2011-05-03", (DateGetterHelper.findAllDates("Dienstag, 03. Mai 2011 um 05:13")).get(0)
                .getNormalizedDateString());
    }

}
