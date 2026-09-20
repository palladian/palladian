package ws.palladian.extraction.location.geocoder;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetrieverFactory;

public final class OpenCageGeocoder implements Geocoder, ReverseGeocoder {

    private final String apiKey;

    public OpenCageGeocoder(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API Key is missing.");
        }
        this.apiKey = apiKey;
    }

    @Override
    public GeoCoordinate geoCode(String addressValue) throws GeocoderException {
        var url = String.format("https://api.opencagedata.com/geocode/v1/json?q=%s&key=%s",
                UrlHelper.encodeParameter(addressValue), apiKey);
        var result = performRequest(url);
        var place = parseJson(result.getStringContent());
        return place != null ? place.getCoordinate() : null;
    }

    @Override
    public Place reverseGeoCode(GeoCoordinate coordinate) throws GeocoderException {
        var url = String.format("https://api.opencagedata.com/geocode/v1/json?q=%s%%2C%s&key=%s",
                coordinate.getLatitude(), coordinate.getLongitude(), apiKey);
        var result = performRequest(url);
        return parseJson(result.getStringContent());
    }

    /* Visible for testing */
    static Place parseJson(String jsonString) throws GeocoderException {
        try {
            var jsonObject = new JsonObject(jsonString);
            var resultsArray = jsonObject.getJsonArray("results");
            if (resultsArray.isEmpty()) {
                return null;
            }
            var resultsObject = resultsArray.getJsonObject(0);

            var componentsObject = resultsObject.getJsonObject("components");
            var placeBuilder = new ImmutablePlace.Builder();

            // https://opencagedata.com/faq#formatted
            placeBuilder.setLabel(resultsObject.tryGetString("formatted"));
            placeBuilder.setCity(componentsObject.tryGetString("_normalized_city"));
            placeBuilder.setCityDistrict(componentsObject.tryGetString("suburb"));
            placeBuilder.setCitySubdistrict(componentsObject.tryGetString("city_district"));
            placeBuilder.setContinent(componentsObject.tryGetString("continent"));
            placeBuilder.setCountry(componentsObject.tryGetString("country"));
            placeBuilder.setCounty(componentsObject.tryGetString("county"));
            placeBuilder.setHouseNumber(componentsObject.tryGetString("house_number"));
            placeBuilder.setMunicipality(componentsObject.tryGetString("municipality"));
            placeBuilder.setNeighbourhood(componentsObject.tryGetString("neighbourhood"));
            placeBuilder.setPoliticalUnion(componentsObject.tryGetString("political_union"));
            placeBuilder.setPostalcode(componentsObject.tryGetString("postcode"));
            // placeBuilder.setProvince(componentsObject.tryGetString("province"));
            placeBuilder.setRegion(componentsObject.tryGetString("region"));
            placeBuilder.setStreet(componentsObject.tryGetString("road"));
            placeBuilder.setState(componentsObject.tryGetString("state"));
            placeBuilder.setStateDistrict(componentsObject.tryGetString("state_district"));

            var geometryObject = resultsObject.getJsonObject("geometry");
            var lat = geometryObject.getDouble("lat");
            var lng = geometryObject.getDouble("lng");
            placeBuilder.setCooordinate(GeoCoordinate.from(lat, lng));

            return placeBuilder.create();
        } catch (JsonException e) {
            throw new GeocoderException(e);
        }
    }

    private static HttpResult performRequest(String url) throws GeocoderException {
        HttpResult httpResult;
        try {
            httpResult = HttpRetrieverFactory.getHttpRetriever().httpGet(url);
        } catch (HttpException e) {
            throw new GeocoderException("Encountered HTTP exception for \"" + url + "\".", e);
        }
        if (httpResult.errorStatus()) {
            throw new GeocoderException("Received HTTP status code " + httpResult.getStatusCode());
        }
        return httpResult;
    }

}
