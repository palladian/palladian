package ws.palladian.retrieval.ip;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.ThreadHelper;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * IP lookup with <a href="https://freegeoip.net">freegeoip</a> API.
 * 
 * @author Philipp Katz
 * @deprecated freegeoip has <a href="https://github.com/apilayer/freegeoip#readme">changed their API</a> -- this class will no longer be working as of July 1st, 2018
 */
// TODO remove with next version bump
@Deprecated
public final class FreeGeoIpLookup implements IpLookup {
	/** The logger for this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(FreeGeoIpLookup.class);

	/**
	 * Sometimes, the web service sends a 503 error status, but retrying the
	 * request usually yields a success. In this case, this is the maximum # of
	 * attempts being made.
	 */
	private static final int MAX_RETRIES = 5;
	
	/** Wait so many milliseconds before retrying a failed request. */
	private static final long WAIT_BETWEEN_RETRIES = 500;

	@Override
	public IpLookupResult lookup(String ip) throws IpLookupException {
		Validate.notEmpty(ip);
		HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
		String requestUrl = "http://freegeoip.net/json/" + ip;
		for (int attempt = 0;; attempt++) {
			HttpResult httpResult;
			try {
				httpResult = retriever.httpGet(requestUrl);
			} catch (HttpException e) {
				if (attempt == MAX_RETRIES) {
					throw new IpLookupException("HTTP exception", e);
				} else {
					LOGGER.debug("Attempt {} failed with HTTP exception, retrying in {} ms", attempt,
							attempt * WAIT_BETWEEN_RETRIES);
					ThreadHelper.deepSleep(attempt * WAIT_BETWEEN_RETRIES);
					continue;
				}
			}
			if (httpResult.errorStatus()) {
				if (httpResult.getStatusCode() == 404) {
					return null;
				}
				if (httpResult.getStatusCode() != 503 || attempt == MAX_RETRIES) {
					throw new IpLookupException("HTTP status " + httpResult.getStatusCode() + " for " + requestUrl);
				} else {
					LOGGER.debug("Attempt {} failed with HTTP status {}, retrying in {} ms", attempt,
							httpResult.getStatusCode(), attempt * WAIT_BETWEEN_RETRIES);
					ThreadHelper.deepSleep(attempt * WAIT_BETWEEN_RETRIES);
					continue;
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
				builder.setZipCode(jsonResult.getString("zip_code"));
				builder.setTimeZone(jsonResult.getString("time_zone"));
				double lat = jsonResult.getDouble("latitude");
				double lng = jsonResult.getDouble("longitude");
				builder.setCoordinate(new ImmutableGeoCoordinate(lat, lng));
				builder.setMetroCode(jsonResult.getString("metro_code"));
			} catch (JsonException e) {
				throw new IpLookupException("JSON parse error for result \"" + stringResult + "\".", e);
			}
			return builder.create();
		}
	}

}
