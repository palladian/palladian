package tud.iir.daterecognition.technique;

import java.util.ArrayList;
import java.util.HashMap;

import tud.iir.daterecognition.DateRaterHelper;
import tud.iir.daterecognition.ExtractedDateHelper;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.daterecognition.dates.HeadDate;
import tud.iir.helper.DateArrayHelper;
import tud.iir.helper.DateComparator;
import tud.iir.knowledge.KeyWords;

public class HeadDateRater extends TechniqueDateRater<HeadDate> {

    @Override
    public HashMap<HeadDate, Double> rate(ArrayList<HeadDate> list) {
        // TODO Auto-generated method stub
        return evaluateHeadDate(list);
    }

    // zuerst high prio keywors bewerten - darumter das ältestte datum wählen rest abwerten
    // mittlere prio nur bewerten, wenn keine high prio -älteste datum auf 1, rest abwerten
    // rest daten nur wenn andere nicht vorhanden - bewertung 1/anz.

    /**
     * Evaluates the head-dates.
     * 
     */
    private HashMap<HeadDate, Double> evaluateHeadDate(ArrayList<HeadDate> headDates) {
        HashMap<HeadDate, Double> result = new HashMap<HeadDate, Double>();
        double rate;
        HeadDate date;
        for (int i = 0; i < headDates.size(); i++) {
            date = headDates.get(i);
            byte keywordPriority = DateRaterHelper.getKeywordPriority(date);
            if (keywordPriority == KeyWords.FIRST_PRIORITY) {
                rate = 1;
            } else if (keywordPriority == KeyWords.SECOND_PRIORITY) {
                rate = -1;
            } else {
                rate = -2;
            }
            result.put(date, rate);
        }
        ArrayList<HeadDate> highRatedDates = DateArrayHelper.getRatedDates(result, 1, true);
        ArrayList<HeadDate> middleRatedDates = DateArrayHelper.getRatedDates(result, -1, true);
        ArrayList<HeadDate> lowRatedDates = DateArrayHelper.getRatedDates(result, -2, true);
        if (highRatedDates.size() > 0) {
            DateRaterHelper.setRateToZero(middleRatedDates, result);
            DateRaterHelper.setRateToZero(lowRatedDates, result);

        } else if (middleRatedDates.size() > 0) {
            DateRaterHelper.setRat(middleRatedDates, result, 1.0);
            DateRaterHelper.setRateToZero(lowRatedDates, result);

        } else {

            ExtractedDate actualDate = ExtractedDateHelper.createActualDate();
            DateComparator dc = new DateComparator();
            for (int i = 0; i < lowRatedDates.size(); i++) {
                rate = 0.75;
                if (dc.getDifference(actualDate, lowRatedDates.get(i), DateComparator.MEASURE_HOUR) < 3) {
                    rate = 0.0;
                }
                result.put(lowRatedDates.get(i), rate);
            }
        }

        DateComparator dc = new DateComparator();
        ArrayList<HeadDate> dates = dc.orderDates(result);
        HeadDate oldest = dc.getOldestDate(DateArrayHelper.getExactestMap(result));

        double diff;
        double oldRate;
        double newRate;

        for (int i = 0; i < dates.size(); i++) {
            diff = dc.getDifference(oldest, dates.get(i), dates.get(i).getExactness());
            if (diff > 24) {
                diff = 24;
            }
            date = dates.get(i);
            oldRate = result.get(date);
            newRate = oldRate - (oldRate * (diff / 24.0));
            result.put(date, Math.round(newRate * 10000) / 10000.0);
        }

        return result;
    }

}
