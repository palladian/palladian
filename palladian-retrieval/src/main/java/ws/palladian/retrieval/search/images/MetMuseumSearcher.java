package ws.palladian.retrieval.search.images;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.SearcherException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 * Search for free images on <a href="http://www.metmuseum.org/">The Met Museum</a>.
 * </p>
 *
 * @author David Urbansky
 * @see <a href="https://metmuseum.github.io/#search">API Docs</a>
 */
public class MetMuseumSearcher extends AbstractSearcher<WebImage> {
    /** The name of this searcher. */
    private static final String SEARCHER_NAME = "MetMuseum";
    private final DocumentRetriever documentRetriever;
    private static IntArrayList allObjectIds = null;

    /**
     * <p>
     * Creates a new MetMuseum searcher.
     * </p>
     */
    public MetMuseumSearcher() {
        documentRetriever = new DocumentRetriever();
    }

    public MetMuseumSearcher(int defaultResultCount) {
        this.defaultResultCount = defaultResultCount;
        documentRetriever = new DocumentRetriever();
    }

    public WebImage getRandom(Orientation orientation) {
        if (allObjectIds == null) {
            JsonObject allObjects = new DocumentRetriever().tryGetJsonObject("https://collectionapi.metmuseum.org/public/collection/v1/objects");
            JsonArray objectIDs = allObjects.tryGetJsonArray("objectIDs");
            allObjectIds = new IntArrayList(objectIDs.size());
            for (int i = 0; i < objectIDs.size(); i++) {
                allObjectIds.add(objectIDs.tryGetInt(i));
            }
        }

        WebImage webImage = null;
        for (int i = 0; i < 100; i++) {
            Integer randomObjectId = MathHelper.getRandomEntry(allObjectIds);
            JsonObject objJson = getObject(randomObjectId);

            if (orientation != null) {
                Orientation imageOrientation = getOrientation(objJson);
                if (imageOrientation == null || imageOrientation != orientation) {
                    continue;
                }
            }
            webImage = buildImage(objJson);
            if (webImage == null) {
                continue;
            }
            break;
        }

        return webImage;
    }

    public List<WebImage> search(String query, int resultCount, Orientation orientation) throws SearcherException {
        return search(query, resultCount, Language.ENGLISH, orientation);
    }

    @Override
    /**
     * @param language Supported languages are English.
     */ public List<WebImage> search(String query, int resultCount, Language language) throws SearcherException {
        return search(query, resultCount, language, null);
    }

    public List<WebImage> search(String query, int resultCount, Language language, Orientation orientation) throws SearcherException {
        List<WebImage> results = new ArrayList<>();

        resultCount = defaultResultCount == null ? resultCount : defaultResultCount;

        String requestUrl = buildRequest(query);
        try {
            JsonObject jsonResponse = documentRetriever.getJsonObject(requestUrl);
            if (jsonResponse == null) {
                throw new SearcherException("Failed to get JSON from " + requestUrl);
            }

            JsonObject json = new JsonObject(jsonResponse);
            JsonArray jsonArray = json.getJsonArray("objectIDs");
            for (int i = 0; i < jsonArray.size(); i++) {
                int objectId = jsonArray.tryGetInt(i);

                JsonObject objJson = getObject(objectId);

                if (orientation != null) {
                    Orientation imageOrientation = getOrientation(objJson);
                    if (imageOrientation != orientation) {
                        continue;
                    }
                }
                String primaryImage = objJson.tryGetString("primaryImage");
                if (primaryImage == null || primaryImage.isEmpty()) {
                    continue;
                }

                results.add(buildImage(objJson));
                if (results.size() >= resultCount) {
                    break;
                }
            }
        } catch (JsonException e) {
            throw new SearcherException(e.getMessage());
        }

        return results;
    }

    public JsonObject getObject(int objectId) {
        return documentRetriever.tryGetJsonObject("https://collectionapi.metmuseum.org/public/collection/v1/objects/" + objectId);
    }

    private Orientation getOrientation(JsonObject obj) {
        JsonArray measurements = obj.tryGetJsonArray("measurements");
        if (measurements != null && !measurements.isEmpty()) {
            JsonObject measure = measurements.tryGetJsonObject(0);
            double height = Optional.ofNullable(measure.tryQueryDouble("elementMeasurements/Height")).orElse(1.);
            double width = Optional.ofNullable(measure.tryQueryDouble("elementMeasurements/Width")).orElse(1.);
            double ratio = width / height;
            if (ratio > 1) {
                return Orientation.LANDSCAPE;
            } else if (ratio == 1) {
                return Orientation.SQUARE;
            }
            return Orientation.PORTRAIT;
        }
        return null;
    }

    private WebImage buildImage(JsonObject objJson) {
        String primaryImage = objJson.tryGetString("primaryImage");
        if (primaryImage == null || primaryImage.isEmpty()) {
            return null;
        }
        BasicWebImage.Builder builder = new BasicWebImage.Builder();
        builder.setIdentifier(objJson.tryGetString("objectID"));
        builder.setAdditionalData("id", objJson.tryGetString("objectID"));
        builder.setUrl(objJson.tryGetString("objectURL"));
        builder.setImageUrl(primaryImage);
        builder.setTitle(objJson.tryGetString("title"));
        builder.setImageType(ImageType.PHOTO);
        builder.setThumbnailUrl(objJson.tryGetString("primaryImageSmall"));
        builder.setLicense(objJson.tryGetBoolean("isPublicDomain", false) ? License.PUBLIC_DOMAIN : License.FREE);
        builder.setAdditionalData("artist-name", objJson.tryGetString("artistDisplayName"));
        builder.setAdditionalData("artist-bio", objJson.tryGetString("artistDisplayBio"));
        builder.setAdditionalData("object-date", objJson.tryGetString("objectDate"));

        return builder.create();
    }

    private String buildRequest(String searchTerms) {
        return String.format("https://collectionapi.metmuseum.org/public/collection/v1/search?hasImages=true&q=%s", UrlHelper.encodeParameter(searchTerms));
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    public static void main(String[] args) throws SearcherException {
        MetMuseumSearcher searcher = new MetMuseumSearcher();
        List<WebImage> results = searcher.search("sunflower", 5, Orientation.LANDSCAPE);
        CollectionHelper.print(results);

        WebImage random = searcher.getRandom(Orientation.PORTRAIT);
        System.out.println(random);
        random = searcher.getRandom(Orientation.PORTRAIT);
        System.out.println(random);
    }
}
