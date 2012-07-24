package ws.palladian.helper.date.dates;

public enum DateType {
	ExtractedDate, MetaDate(3), StructureDate(4), ContentDate(2), ArchiveDate, ReferenceDate, UrlDate(1);
	
	private int type;
	
	DateType(int type) {
	    this.type = type;
	}
	
	DateType() {
	    this(0);
	}
	
	public int getIntType() {
	    return type;
	}
}
