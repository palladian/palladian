package ws.palladian.extraction.location.geocoder;

import ws.palladian.helper.geo.GeoCoordinate;

/**
 * A Geocoder transforms an address, e.g.
 * "1600 Amphitheatre Parkway, Mountain View, CA" to a geographic coordinates,
 * such as <tt>(37.423021, -122.083739)</tt>.
 * 
 * @author Philipp Katz
 */
public interface Geocoder {

	GeoCoordinate geoCode(String addressValue) throws GeocoderException;

}
