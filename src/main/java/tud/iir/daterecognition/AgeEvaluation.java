package tud.iir.daterecognition;

import java.util.ArrayList;
import java.util.HashMap;

import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.helper.DateArrayHelper;
import tud.iir.helper.DateComparator;

public class AgeEvaluation {

    DateGetter dg = new DateGetter();
    DateRater dr = new DateRater();

    private String url;

    public void setUrl(String url) {
        this.url = url;
    }

    public ExtractedDate getOldestBestRatedDate() {
        ArrayList<ExtractedDate> dates = new ArrayList<ExtractedDate>();
        HashMap<ExtractedDate, Double> ratedDates = new HashMap<ExtractedDate, Double>();

        dg.setURL(url);
        dg.setTechReference(false);
        dates = dg.getDate();

        ratedDates = dr.rate(dates);
        double rate = DateArrayHelper.getHighestRate(ratedDates);
        dates = DateArrayHelper.getRatedDates(ratedDates, rate);
        ExtractedDate date = getOldestDate(dates);

        return date;
    }

    public ArrayList<ExtractedDate> getAllBestRatedDate() {
        ArrayList<ExtractedDate> dates = new ArrayList<ExtractedDate>();
        HashMap<ExtractedDate, Double> ratedDates = new HashMap<ExtractedDate, Double>();

        dg.setURL(url);
        dg.setTechReference(false);
        dates = dg.getDate();
        ratedDates = dr.rate(dates);
        double rate = DateArrayHelper.getHighestRate(ratedDates);
        dates = DateArrayHelper.getRatedDates(ratedDates, rate);
        return dates;
    }

    private ExtractedDate getOldestDate(ArrayList<ExtractedDate> list) {
        DateComparator dc = new DateComparator();
        return dc.getOldestDate(list);
    }

}
