package ws.palladian.extraction.location.geocoder;

import ws.palladian.helper.geo.GeoCoordinate;

public interface ReverseGeocoder {

    Place reverseGeoCode(GeoCoordinate coordinate) throws GeocoderException;

}
