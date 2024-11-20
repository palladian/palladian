package ws.palladian.extraction.location.geocoder;

import java.util.Objects;

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
        var httpResult = executeRequest(url);
        var place = parseJson(httpResult.getStringContent());
        return place != null ? place.getCoordinate() : null;
    }

    @Override
    public Place reverseGeoCode(GeoCoordinate coordinate) throws GeocoderException {
        // https://www.here.com/docs/bundle/geocoding-and-search-api-developer-guide/page/topics-api/code-revgeocode-multiple.html
        var url = String.format("https://revgeocode.search.hereapi.com/v1/revgeocode?at=%s%%2C%s&limit=1&apiKey=%s",
                coordinate.getLatitude(), coordinate.getLongitude(), apiKey);
        var httpResult = executeRequest(url);
        return parseJson(httpResult.getStringContent());
    }

    /* Visible for testing */
    static Place parseJson(String stringContent) throws GeocoderException {
        try {
            var jsonObject = new JsonObject(stringContent);
            var items = jsonObject.getJsonArray("items");
            if (items.isEmpty()) {
                return null;
            }

            var item = items.getJsonObject(0);

            // @formatter:off

            // https://www.here.com/docs/bundle/geocoding-and-search-api-v7-api-reference/page/index.html#/paths/~1geocode/get

            // label - Assembled address value built out of the address components according to the regional postal rules. These are the same rules for all endpoints. It may not include all the input terms. For example: "Schulstraße 4, 32547 Bad Oeynhausen, Germany"
            // countryCode - A three-letter country code. For example: "DEU"
            // countryName - The localised country name. For example: "Deutschland"
            // stateCode - A state code or state name abbreviation – country specific. For example, in the United States it is the two letter state abbreviation: "CA" for California.
            // state - The state division of a country. For example: "North Rhine-Westphalia"
            // countyCode - A county code or county name abbreviation – country specific. For example, for Italy it is the province abbreviation: "RM" for Rome.
            // county - A division of a state; typically, a secondary-level administrative division of a country or equivalent.
            // city - The name of the primary locality of the place. For example: "Bad Oyenhausen"
            // district - A division of city; typically an administrative unit within a larger city or a customary name of a city's neighborhood. For example: "Bad Oyenhausen"
            // subdistrict - A subdivision of a district. For example: "Minden-Lübbecke"
            // street - Name of street. For example: "Schulstrasse"
            // streets - Names of streets in case of intersection result. For example: ["Friedrichstraße","Unter den Linden"]
            // block - Name of block.
            // subblock - Name of sub-block.
            // postalCode - An alphanumeric string included in a postal address to facilitate mail sorting, such as post code, postcode, or ZIP code. For example: "32547"
            // houseNumber - House number. For example: "4"
            // building - Name of building.
            // unit - Secondary unit information. It may include building, floor (level), and suite (unit) details. This field is returned by Geocode and Lookup endpoints only.

            // @formatter:on

            var address = item.getJsonObject("address");

            var placeBuilder = new ImmutablePlace.Builder();
            placeBuilder.setLabel(address.tryGetString("label"));
            // currently unmapped: countryCode
            placeBuilder.setCountry(address.tryGetString("countryName"));
            // currently unmapped: stateCode
            placeBuilder.setState(address.tryGetString("state"));
            // currently unmapped: countyCode
            placeBuilder.setCounty(address.tryGetString("county"));
            placeBuilder.setCity(address.tryGetString("city"));
            placeBuilder.setCityDistrict(address.tryGetString("district"));
            placeBuilder.setCitySubdistrict(address.tryGetString("subdistrict"));
            placeBuilder.setStreet(address.tryGetString("street"));
            // currently unmapped: streets
            // currently unmapped: block
            // currently unmapped: subblock
            placeBuilder.setPostalcode(address.tryGetString("postalCode"));
            placeBuilder.setHouseNumber(address.tryGetString("houseNumber"));
            // currently unmapped: building
            // currently unmapped: unit

            var position = item.getJsonObject("position");
            var lat = position.getDouble("lat");
            var lng = position.getDouble("lng");
            placeBuilder.setCooordinate(GeoCoordinate.from(lat, lng));

            return placeBuilder.create();
        } catch (JsonException e) {
            throw new GeocoderException(e);
        }
    }

    private static HttpResult executeRequest(String url) throws GeocoderException {
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
