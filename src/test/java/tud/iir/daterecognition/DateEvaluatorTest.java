package tud.iir.daterecognition;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Test;

import tud.iir.daterecognition.dates.ExtractedDate;

public class DateEvaluatorTest {

    @Test
    public final void testEvaluate() {
        DateGetter dg = new DateGetter(
                "http://www.basicthinking.de/blog/2010/08/24/the-fridge-so-wie-facebook-nur-mit-echten-freunden/");
        dg.setTechReference(false);
        dg.setTechArchive(false);
        ArrayList<ExtractedDate> dates = dg.getDate();

        DateEvaluator de = new DateEvaluator();
        HashMap<ExtractedDate, Double> dateMap = de.evaluate(dates);
        DateEvaluatorHelper.printMap(dateMap);

    }

    @Test
    public final void testEvaluateGroup() {
    }

    @Test
    public final void testEvaluateStructDate() {
    }

    @Test
    public final void testEvaluateHeadDate() {
    }

    @Test
    public final void testEvaluateHTTPDate() {
    }

    @Test
    public final void testEvaluateURLDate() {
    }

    @Test
    public final void testEvaluateContentDate() {
    }

    @Test
    public final void testEvaluateContentDateByNumber() {
    }

}
