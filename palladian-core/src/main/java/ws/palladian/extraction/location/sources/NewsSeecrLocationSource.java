package ws.palladian.extraction.location.sources;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.JPathHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;

/**
 * FIXME:
 * 52feznh45ezmjxgfzorrk6ooagyadg
 * 
 * @author Sky
 * 
 */
public class NewsSeecrLocationSource implements LocationSource {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NewsSeecrLocationSource.class);

    private static final String SEARCHER_NAME = "NewsSeecr";

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private static final String BASE_URL = "https://qqilihq-newsseecr.p.mashape.com/locations";

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    private final String mashapePublicKey;

    private final String mashapePrivateKey;

    /** Configuraiton key for the Mashape public key. */
    public static final String CONFIG_MASHAPE_PUBLIC_KEY = "api.newsseecr.mashapePublicKey";
    /** Configuration key for the Mashape private key. */
    public static final String CONFIG_MASHAPE_PRIVATE_KEY = "api.newsseecr.mashapePrivateKey";

    private static final Map<String, LocationType> LOCATION_MAPPING;

    static {
        Map<String, LocationType> temp = CollectionHelper.newHashMap();
        temp.put("Country", LocationType.COUNTRY);
        temp.put("Nation", LocationType.COUNTRY);
        temp.put("County", LocationType.UNIT);
        temp.put("City", LocationType.CITY);
        temp.put("Metropole", LocationType.CITY);
        LOCATION_MAPPING = Collections.unmodifiableMap(temp);
    }

    /**
     * <p>
     * Create a new {@link NewsSeecrLocationSource} with the provided credentials from Mashape.
     * </p>
     * 
     * @param mashapePublicKey The Mashape public key, not empty or <code>null</code>.
     * @param mashapePrivateKey The Mashape private key, not empty or <code>null</code>.
     */
    public NewsSeecrLocationSource(String mashapePublicKey, String mashapePrivateKey) {
        Validate.notEmpty(mashapePublicKey, "mashapePublicKey must not be empty");
        Validate.notEmpty(mashapePrivateKey, "mashapePrivateKey must not be empty");
        this.mashapePublicKey = mashapePublicKey;
        this.mashapePrivateKey = mashapePrivateKey;
    }

    @Override
    public List<Location> retrieveLocations(String locationName) {
        List<Location> locations = CollectionHelper.newArrayList();
        HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

        HttpRequest request = new HttpRequest(HttpMethod.GET, BASE_URL + "/search");
        request.addParameter("locationName", locationName);

        // FIXME this is duplicate code from NewsSeecrSearcher
        String mashapeHeader;
        try {
            mashapeHeader = generateMashapeHeader(mashapePublicKey, mashapePrivateKey);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Error while creating Authorization header: " + e.getMessage(), e);
        }
        LOGGER.debug("Authorization header = " + mashapeHeader);
        request.addHeader("X-Mashape-Authorization", mashapeHeader);

        LOGGER.debug("Performing request: " + request);
        HttpResult result;
        try {
            result = retriever.execute(request);
        } catch (HttpException e) {
            throw new RuntimeException("Encountered HTTP error when executing the request: " + request + ": "
                    + e.getMessage(), e);
        }
        if (result.getStatusCode() != 200) {
            // TODO get message
            throw new RuntimeException("Encountered HTTP status " + result.getStatusCode()
                    + " when executing the request: " + request + ", result: " + HttpHelper.getStringContent(result));
        }

        String jsonString = HttpHelper.getStringContent(result);
        LOGGER.debug("JSON result: " + jsonString);

        try {
            JSONArray resultArray = JPathHelper.get(jsonString, "results", JSONArray.class);
            for (int i = 0; i < resultArray.length(); i++) {
                JSONObject resultObject = resultArray.getJSONObject(i);
                Integer id = JPathHelper.get(resultObject, "id", Integer.class);
                Double latitude = JPathHelper.get(resultObject, "latitude", Double.class);
                Double longitude = JPathHelper.get(resultObject, "longitude", Double.class);
                String primaryName = JPathHelper.get(resultObject, "primaryName", String.class);
                String type = JPathHelper.get(resultObject, "locationType", String.class);
                Long population = JPathHelper.get(resultObject, "population", Long.class);
                List<String> alternativeNames = new ArrayList(Arrays.asList(JPathHelper.get(resultObject,
                        "alternateNames", JSONArray.class)));

                Location location = new Location();
                location.setId(id);
                location.setLatitude(latitude);
                location.setLongitude(longitude);
                location.setPopulation(population);
                location.setPrimaryName(primaryName);
                location.setType(LocationType.valueOf(type));
                location.setAlternativeNames(alternativeNames);

                locations.add(location);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error while parsing the JSON response (" + jsonString + "): " + e.getMessage(), e);
        }

        return locations;
    }

    // https://www.mashape.com/docs/consume/rest
    public static String generateMashapeHeader(String publicKey, String privateKey) throws InvalidKeyException,
    NoSuchAlgorithmException {
        return new String(Base64.encodeBase64(String.format("%s:%s", publicKey, sha1hmac(publicKey, privateKey))
                .getBytes()));
    }

    // Code taken from:
    // https://github.com/Mashape/mashape-java-client-library/blob/master/src/main/java/com/mashape/client/http/utils/CryptUtils.java
    static String sha1hmac(String publicKey, String privateKey) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKey key = new SecretKeySpec(privateKey.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(key);
        byte[] rawHmac = mac.doFinal(publicKey.getBytes());
        BigInteger hash = new BigInteger(1, rawHmac);
        String hmac = hash.toString(16);
        if (hmac.length() % 2 != 0) {
            hmac = "0" + hmac;
        }
        return hmac;
    }

    @Override
    public Location retrieveLocation(int locationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Location> getHierarchy(Location location) {

        List<Location> locations = CollectionHelper.newArrayList();

        HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

        HttpRequest request = new HttpRequest(HttpMethod.GET, BASE_URL + "/getHierarchy");
        request.addParameter("locationId", location.getId());

        // FIXME this is duplicate code from NewsSeecrSearcher
        String mashapeHeader;
        try {
            mashapeHeader = generateMashapeHeader(mashapePublicKey, mashapePrivateKey);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Error while creating Authorization header: " + e.getMessage(), e);
        }
        LOGGER.debug("Authorization header = " + mashapeHeader);
        request.addHeader("X-Mashape-Authorization", mashapeHeader);

        LOGGER.debug("Performing request: " + request);
        HttpResult result;
        try {
            result = retriever.execute(request);
        } catch (HttpException e) {
            throw new RuntimeException("Encountered HTTP error when executing the request: " + request + ": "
                    + e.getMessage(), e);
        }
        if (result.getStatusCode() != 200) {
            // TODO get message
            throw new RuntimeException("Encountered HTTP status " + result.getStatusCode()
                    + " when executing the request: " + request + ", result: " + HttpHelper.getStringContent(result));
        }

        String jsonString = HttpHelper.getStringContent(result);
        System.out.println(jsonString);

        try {
            JSONArray resultArray = JPathHelper.get(jsonString, "results", JSONArray.class);
            for (int i = 0; i < resultArray.length(); i++) {
                JSONObject resultObject = resultArray.getJSONObject(i);
                Integer id = JPathHelper.get(resultObject, "id", Integer.class);
                Double latitude = JPathHelper.get(resultObject, "latitude", Double.class);
                Double longitude = JPathHelper.get(resultObject, "longitude", Double.class);
                String primaryName = JPathHelper.get(resultObject, "primaryName", String.class);
                String type = JPathHelper.get(resultObject, "locationType", String.class);
                Long population = JPathHelper.get(resultObject, "population", Long.class);
                List<String> alternativeNames = new ArrayList(Arrays.asList(JPathHelper.get(resultObject,
                        "alternateNames", JSONArray.class)));

                Location location1 = new Location();
                location1.setId(id);
                location1.setLatitude(latitude);
                location1.setLongitude(longitude);
                location1.setPopulation(population);
                location1.setPrimaryName(primaryName);
                location1.setType(LocationType.valueOf(type));
                location1.setAlternativeNames(alternativeNames);

                locations.add(location1);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while parsing the JSON response (" + jsonString + "): " + e.getMessage(),
                    e);
        }

        return locations;
    }

    public static void main(String[] args) {
        NewsSeecrLocationSource newsSeecrLocationSource = new NewsSeecrLocationSource("52feznh45ezmjxgfzorrk6ooagyadg",
                "iwjiagid3rqhbyu5bwwevrbpyicrk2");
        List<Location> locations = newsSeecrLocationSource.retrieveLocations("Berlin");
        CollectionHelper.print(locations);

        Location loc = locations.get(53);
        System.out.println(loc);

        List<Location> hierarchyLocations = newsSeecrLocationSource.getHierarchy(loc);
        CollectionHelper.print(hierarchyLocations);
    }
}
