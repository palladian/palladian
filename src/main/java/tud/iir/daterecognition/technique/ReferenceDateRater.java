package tud.iir.daterecognition.technique;

import java.util.ArrayList;
import java.util.HashMap;

import tud.iir.daterecognition.DateGetter;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.ReferenceDate;
import tud.iir.helper.DateArrayHelper;
import tud.iir.helper.DateComparator;

public class ReferenceDateRater extends TechniqueDateRater<ReferenceDate> {

    private String url;

    @Override
    public HashMap<ReferenceDate, Double> rate(ArrayList<ReferenceDate> list) {
        HashMap<ReferenceDate, Double> evaluatedDates = new HashMap<ReferenceDate, Double>();
        ReferenceDate date = getYoungest(list);
        evaluatedDates.put(date, date.getRate());
        return evaluatedDates;
    }

    public HashMap<ReferenceDate, Double> rate(String url) {
        this.url = url;
        return getRefDates();
    }

    private HashMap<ReferenceDate, Double> getRefDates() {
        DateGetter dg = new DateGetter(url);
        dg.setAllFalse();
        dg.setTechReference(true);

        ReferenceDateGetter rdg = new ReferenceDateGetter();
        rdg.setUrl(url);
        ArrayList<ReferenceDate> newRefDates = rdg.getDates();

        ArrayList<ReferenceDate> refDates = (ArrayList<ReferenceDate>) DateArrayHelper.filter(newRefDates,
                ExtractedDate.TECH_REFERENCE);

        ReferenceDate date = getYoungest(refDates);
        HashMap<ReferenceDate, Double> evaluatedDates = new HashMap<ReferenceDate, Double>();
        evaluatedDates.put(date, date.getRate());
        return evaluatedDates;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    private ReferenceDate getYoungest(ArrayList<ReferenceDate> refDates) {
        DateComparator dc = new DateComparator();
        return dc.getYoungestDate(refDates);
    }

}
