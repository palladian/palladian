package tud.iir.daterecognition.technique;

import java.util.ArrayList;
import java.util.HashMap;

import tud.iir.daterecognition.dates.ArchiveDate;

public class ArchiveDateRater extends TechniqueDateRater<ArchiveDate> {

    @Override
    public HashMap<ArchiveDate, Double> rate(ArrayList<ArchiveDate> list) {
        HashMap<ArchiveDate, Double> map = new HashMap<ArchiveDate, Double>();
        map.put(list.get(0), 1.0);
        return map;
    }

}
