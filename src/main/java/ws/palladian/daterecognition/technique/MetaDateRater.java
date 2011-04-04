package ws.palladian.daterecognition.technique;

import java.util.ArrayList;
import java.util.HashMap;

import ws.palladian.daterecognition.dates.ExtractedDate;
import ws.palladian.daterecognition.dates.MetaDate;

public class MetaDateRater extends TechniqueDateRater<MetaDate> {

	private ExtractedDate actualDate;
	
	public MetaDateRater(PageDateType dateType) {
		super(dateType);
		// TODO Auto-generated constructor stub
	}

	@Override
	public HashMap<MetaDate, Double> rate(ArrayList<MetaDate> list) {
		HeadDateRater hdr = new HeadDateRater(dateType);
		hdr.setActualDate(actualDate);
		return hdr.rate(list);
	}

	public void setActualDate(ExtractedDate actualDate){
    	this.actualDate = actualDate;
    }
}
