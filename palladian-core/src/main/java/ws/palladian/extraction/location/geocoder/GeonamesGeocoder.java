package ws.palladian.extraction.location.geocoder;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;

public final class GeonamesGeocoder implements Geocoder {

    private final String username;

    public GeonamesGeocoder(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username is missing.");
        }
        this.username = username;
    }

    @Override
    public GeoCoordinate geoCode(String addressValue) throws GeocoderException {
        HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
        try {
            // http://api.geonames.org/geoCodeAddressJSON?q=Museumplein+6+amsterdam&username=qqilihq
            String url = String.format("http://api.geonames.org/geoCodeAddressJSON?q=%s&username=%s",
                    UrlHelper.encodeParameter(addressValue), username);
            HttpResult result = retriever.httpGet(url);
            // {
            // "address": {
            // "adminCode2": "0363",
            // "sourceId": "0363010012084818",
            // "adminCode3": "",
            // "adminCode1": "07",
            // "lng": "4.88132",
            // "houseNumber": "6",
            // "locality": "Amsterdam",
            // "adminCode4": "",
            // "adminName2": "Gemeente Amsterdam",
            // "street": "Museumplein",
            // "postalcode": "1071 DJ",
            // "countryCode": "NL",
            // "adminName1": "North Holland",
            // "lat": "52.35792"
            // }
            // }
            JsonObject jsonObject = new JsonObject(result.getStringContent());
            JsonObject jsonAddress = jsonObject.getJsonObject("address");
            if (jsonAddress == null) {
                return null;
            }
            double lat = jsonAddress.getDouble("lat");
            double lng = jsonAddress.getDouble("lng");
            return GeoCoordinate.from(lat, lng);
        } catch (HttpException e) {
            throw new GeocoderException(e);
        } catch (JsonException e) {
            throw new GeocoderException(e);
        }
    }

}
