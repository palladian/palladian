package ws.palladian.extraction.location.geocoder;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
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
        HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
        // https://api.opencagedata.com/geocode/v1/json?key=YOUR-API-KEY&q=52.3877830%2C+9.7334394&pretty=1&no_annotations=1
        // https://api.opencagedata.com/geocode/v1/json?q=URI-ENCODED-PLACENAME&key=YOUR-API-KEY
        try {
            String url = String.format("https://api.opencagedata.com/geocode/v1/json?q=%s&key=%s",
                    UrlHelper.encodeParameter(addressValue), apiKey);
            HttpResult result = retriever.httpGet(url);
            JsonObject jsonObject = new JsonObject(result.getStringContent());
            JsonArray resultsArray = jsonObject.getJsonArray("results");
            JsonObject resultObject = resultsArray.getJsonObject(0);
            JsonObject geometryObject = resultObject.getJsonObject("geometry");
            double lat = geometryObject.getDouble("lat");
            double lng = geometryObject.getDouble("lng");
            return GeoCoordinate.from(lat, lng);
        } catch (HttpException e) {
            throw new GeocoderException(e);
        } catch (JsonException e) {
            throw new GeocoderException(e);
        }
    }

    @Override
    public Place reverseGeoCode(GeoCoordinate coordinate) throws GeocoderException {
        try {
            // https://api.opencagedata.com/geocode/v1/json?q=52.3877830%2C9.7334394&key=YOUR-API-KEY
            var url = String.format("https://api.opencagedata.com/geocode/v1/json?q=%s%%2C%s&key=%s",
                    coordinate.getLatitude(), coordinate.getLongitude(), apiKey);
            var retriever = HttpRetrieverFactory.getHttpRetriever();
            var result = retriever.httpGet(url);
            var jsonObject = new JsonObject(result.getStringContent());
            var resultsArray = jsonObject.getJsonArray("results");
            var resultsObject = resultsArray.getJsonObject(0);
            var componentsObject = resultsObject.getJsonObject("components");
            var placeBuilder = new ImmutablePlace.Builder();
            placeBuilder.setHouseNumber(componentsObject.getString("house_number"));
            placeBuilder.setStreet(componentsObject.getString("road"));
            placeBuilder.setPostalcode(componentsObject.getString("postcode"));
            placeBuilder.setCountry(componentsObject.getString("country"));
            placeBuilder.setRegion(componentsObject.getString("region"));
            placeBuilder.setCounty(componentsObject.getString("county"));
            // locality, neighbourhood, label
            return placeBuilder.create();
        } catch (HttpException e) {
            throw new GeocoderException(e);
        } catch (JsonException e) {
            throw new GeocoderException(e);
        }
    }

}
