package ws.palladian.extraction.location.geocoder;

public interface Place {
	String getHouseNumber();
	String getStreet();
	String getPostalcode();
	String getCountry();
	String getRegion();
	String getCounty();
	String getLocality();
	String getNeighbourhood();
	String getLabel();
}
