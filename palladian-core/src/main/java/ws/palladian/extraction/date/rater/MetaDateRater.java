package ws.palladian.extraction.date.rater;

import java.util.List;
import java.util.Map;

import ws.palladian.extraction.date.PageDateType;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.helper.date.dates.MetaDate;

public class MetaDateRater extends TechniqueDateRater<MetaDate> {

	private ExtractedDate actualDate;
	
	public MetaDateRater(PageDateType dateType) {
		super(dateType);
	}

	@Override
	public Map<MetaDate, Double> rate(List<MetaDate> list) {
		HeadDateRater hdr = new HeadDateRater(dateType);
		hdr.setCurrentDate(actualDate);
		return hdr.rate(list);
	}

	public void setActualDate(ExtractedDate actualDate){
    	this.actualDate = actualDate;
    }
}
