package ws.palladian.extraction.location.geocoder;

import ws.palladian.helper.geo.GeoCoordinate;

public interface Place {

	String getContinent();

	String getPoliticalUnion();

	String getCountry();

	String getState(); // treat synonymous to province

	// String getStateDistrict();

	// String getProvince();

	// String getRegion();

	String getCounty();

	// String getMunicipality();

	String getCity();

	// String getNeighbourhood();

	String getCityDistrict();

	String getCitySubdistrict();

	String getPostalcode();

	String getStreet();

	String getHouseNumber();

	/** @deprecated No longer in use. */
	@Deprecated
	String getLocality();

	/** Label aka. formatted value */
	String getLabel();
	
	String getName();

	GeoCoordinate getCoordinate();

}
