package tud.iir.daterecognition.technique;

import java.util.ArrayList;
import java.util.HashMap;

import tud.iir.daterecognition.DateRaterHelper;
import tud.iir.daterecognition.dates.StructureDate;
import tud.iir.helper.DateArrayHelper;
import tud.iir.helper.DateComparator;
import tud.iir.knowledge.KeyWords;

/**
 * This class rates structure dates by keywords.
 * 
 * @author Martin Gregor
 * 
 */
public class StructureDateRater extends TechniqueDateRater<StructureDate> {

    @Override
    public HashMap<StructureDate, Double> rate(ArrayList<StructureDate> list) {
        return evaluateStructDate(list);
    }

    /**
     * Evaluates the structure-dates.<br>
     * There for keywords are distinct into classes. See {@link KeyWords#getKeywordPriority(String)}.<br>
     * Only best class will be rated, all other get value of 0. <br>
     * Also a weight in dependency of number of dates will be set.
     * 
     * @param structDates
     * @return
     */
    private HashMap<StructureDate, Double> evaluateStructDate(ArrayList<StructureDate> structDates) {
        HashMap<StructureDate, Double> result = new HashMap<StructureDate, Double>();
        double rate;
        for (int i = 0; i < structDates.size(); i++) {
            StructureDate date = structDates.get(i);
            byte keywordPriority = DateRaterHelper.getKeywordPriority(date);
            if (keywordPriority == KeyWords.FIRST_PRIORITY) {
                rate = 1;
            } else if (keywordPriority == KeyWords.SECOND_PRIORITY) {
                rate = -1; // TODO: rate bestimmen.

            } else if (keywordPriority == KeyWords.THIRD_PRIORITY) {
                rate = -2; // TODO: rate bestimmen.

            } else {
                rate = 0;
            }
            result.put(date, rate);
        }

        ArrayList<StructureDate> highRatedDates = DateArrayHelper.getRatedDates(result, 1);
        ArrayList<StructureDate> middleRatedDates = DateArrayHelper.getRatedDates(result, -1);
        ArrayList<StructureDate> lowRatedDates = DateArrayHelper.getRatedDates(result, -2);
        if (highRatedDates.size() > 0) {
            DateRaterHelper.setRateWhightedByGroups(highRatedDates, result, DateComparator.STOP_MINUTE);

            DateRaterHelper.setRateToZero(middleRatedDates, result);
            DateRaterHelper.setRateToZero(lowRatedDates, result);
        } else if (middleRatedDates.size() > 0) {
            DateRaterHelper.setRateWhightedByGroups(middleRatedDates, result, DateComparator.STOP_MINUTE);

            DateRaterHelper.setRateToZero(lowRatedDates, result);
        } else {
            DateRaterHelper.setRateWhightedByGroups(lowRatedDates, result, DateComparator.STOP_MINUTE);
        }
        return result;
    }

}
