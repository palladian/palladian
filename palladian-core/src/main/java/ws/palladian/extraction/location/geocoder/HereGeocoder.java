package ws.palladian.extraction.location.geocoder;

import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetrieverFactory;

public final class HereGeocoder implements Geocoder, ReverseGeocoder {

    private final String apiKey;

    public HereGeocoder(String apiKey) {
        Objects.requireNonNull(apiKey, "apiKey must not be null");
        this.apiKey = apiKey;
    }

    @Override
    public GeoCoordinate geoCode(String addressValue) throws GeocoderException {
        // https://www.here.com/docs/bundle/geocoding-and-search-api-developer-guide/page/topics-api/code-geocode-address.html
        var url = String.format("https://geocode.search.hereapi.com/v1/geocode?q=%s&apiKey=%s",
                UrlHelper.encodeParameter(addressValue), apiKey);
        HttpResult httpResult;
        try {
            httpResult = HttpRetrieverFactory.getHttpRetriever().httpGet(url);
        } catch (HttpException e) {
            throw new GeocoderException("Encountered HTTP exception for \"" + url + "\".", e);
        }
        if (httpResult.errorStatus()) {
            throw new GeocoderException("Received HTTP status code " + httpResult.getStatusCode());
        }
        var parsed = parseGeoCodeResult(httpResult.getStringContent());
        return parsed.getLeft();
    }

    static Pair<GeoCoordinate, Place> parseGeoCodeResult(String stringContent) throws GeocoderException {
        try {
            var jsonObject = new JsonObject(stringContent);
            var items = jsonObject.getJsonArray("items");
            if (items.size() > 0) {
                var item = items.getJsonObject(0);
                // coordinate
                var position = item.getJsonObject("position");
                var lat = position.getDouble("lat");
                var lng = position.getDouble("lng");
                var coord = GeoCoordinate.from(lat, lng);
                // place
                var address = item.getJsonObject("address");
                var placeBuilder = new ImmutablePlace.Builder();
                placeBuilder.setHouseNumber(address.getString("houseNumber"));
                placeBuilder.setStreet(address.getString("street"));
                placeBuilder.setPostalcode(address.getString("postalCode"));
                placeBuilder.setCountry(address.getString("countryName"));
                // region
                placeBuilder.setCounty(address.getString("county"));
                // locality
                // neighbourhood
                placeBuilder.setLabel(address.getString("label"));
                return Pair.of(coord, placeBuilder.create());
            } else {
                return null;
            }
        } catch (JsonException e) {
            throw new GeocoderException(e);
        }
    }

    @Override
    public Place reverseGeoCode(GeoCoordinate coordinate) throws GeocoderException {
        // https://www.here.com/docs/bundle/geocoding-and-search-api-developer-guide/page/topics-api/code-revgeocode-multiple.html
        var url = String.format("https://revgeocode.search.hereapi.com/v1/revgeocode?at=%s%%2C%s&limit=1&apiKey=%s",
                coordinate.getLatitude(), coordinate.getLongitude(), apiKey);
        HttpResult httpResult;
        try {
            httpResult = HttpRetrieverFactory.getHttpRetriever().httpGet(url);
        } catch (HttpException e) {
            throw new GeocoderException("Encountered HTTP exception for \"" + url + "\".", e);
        }
        if (httpResult.errorStatus()) {
            throw new GeocoderException("Received HTTP status code " + httpResult.getStatusCode());
        }
        var parsed = parseGeoCodeResult(httpResult.getStringContent());
        return parsed.getRight();
    }

}
