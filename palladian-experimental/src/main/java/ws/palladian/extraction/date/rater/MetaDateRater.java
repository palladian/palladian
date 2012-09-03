package ws.palladian.extraction.date.rater;

import java.util.List;
import java.util.Map;

import ws.palladian.extraction.date.PageDateType;
import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.extraction.date.dates.RatedDate;
import ws.palladian.helper.date.ExtractedDate;

public class MetaDateRater extends TechniqueDateRater<MetaDate> {

    private final HeadDateRater hdr;
	private ExtractedDate actualDate;
	
	public MetaDateRater(PageDateType dateType) {
		super(dateType);
		hdr = new HeadDateRater(dateType);
	}

	@Override
	public List<RatedDate<MetaDate>> rate(List<MetaDate> list) {
		hdr.setCurrentDate(actualDate);
		return hdr.rate(list);
	}

	public void setActualDate(ExtractedDate actualDate){
    	this.actualDate = actualDate;
    }
}
