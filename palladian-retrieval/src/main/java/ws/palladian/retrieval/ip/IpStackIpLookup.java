package ws.palladian.retrieval.ip;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

import java.nio.charset.StandardCharsets;

/**
 * IP lookup with <a href="https://ipstack.com/">IP Stack</a> API.
 *
 * @author David Urbansky
 */
public final class IpStackIpLookup implements IpLookup {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(IpStackIpLookup.class);

    // limited to 10k requests per month
    private static final String API_KEY = "9bed92b977d5272196d9980b8a823b19";

    @Override
    public IpLookupResult lookup(String ip) throws IpLookupException {
        Validate.notEmpty(ip);
        HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
        String requestUrl = "http://api.ipstack.com/" + ip + "?access_key=" + API_KEY;
        HttpResult httpResult;

        try {
            httpResult = retriever.httpGet(requestUrl);
        } catch (HttpException e) {
            throw new IpLookupException("HTTP exception", e);
        }

        if (httpResult.errorStatus()) {
            if (httpResult.getStatusCode() == 404) {
                return null;
            }
            if (httpResult.getStatusCode() != 503) {
                throw new IpLookupException("HTTP status " + httpResult.getStatusCode() + " for " + requestUrl);
            }
        }

        // result does not specify encoding, use UTF-8 explicitely
        String stringResult = new String(httpResult.getContent(), StandardCharsets.UTF_8);
        IpLookupResult.Builder builder;
        try {
            JsonObject jsonResult = new JsonObject(stringResult);
            builder = new IpLookupResult.Builder();
            builder.setIp(jsonResult.getString("ip"));
            builder.setCountryCode(jsonResult.getString("country_code"));
            builder.setCountryName(jsonResult.getString("country_name"));
            builder.setRegionCode(jsonResult.getString("region_code"));
            builder.setRegionName(jsonResult.getString("region_name"));
            builder.setCity(jsonResult.getString("city"));
            builder.setZipCode(jsonResult.getString("zip"));
            //            builder.setTimeZone(jsonResult.getString("time_zone"));
            double lat = jsonResult.getDouble("latitude");
            double lng = jsonResult.getDouble("longitude");
            builder.setCoordinate(GeoCoordinate.from(lat, lng));
            //            builder.setMetroCode(jsonResult.getString("metro_code"));

        } catch (JsonException e) {
            throw new IpLookupException("JSON parse error for result \"" + stringResult + "\".", e);
        }
        return builder.create();
    }

}
