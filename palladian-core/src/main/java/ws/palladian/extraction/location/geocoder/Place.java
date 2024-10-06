package ws.palladian.extraction.location.geocoder;

import ws.palladian.helper.geo.GeoCoordinate;

public interface Place {
    String getHouseNumber();

    String getStreet();

    String getPostalcode();

    String getCountry();

    String getRegion();

    String getCounty();

    /** @deprecated No longer in use. */
    @Deprecated
    String getLocality();

    String getNeighbourhood();

    /** Label aka. formatted value */
    String getLabel();

    String getProvince();

    String getStateDistrict();

    String getPoliticalUnion();

    String getMunicipality();

    String getCitySubdistrict();

    String getContinent();

    String getCityDistrict();

    String getCity();

    String getState();

    GeoCoordinate getCoordinate();

}
