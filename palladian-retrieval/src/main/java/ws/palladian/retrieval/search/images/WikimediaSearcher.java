package ws.palladian.retrieval.search.images;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.SearcherException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Searching wikimedia.
 *
 * @see https://www.mediawiki.org/wiki/Extension:PageImages
 */
public class WikimediaSearcher extends AbstractSearcher<WebImage> {
    private static final String SEARCHER_NAME = "WikimediaSearcher";

    public static final class WikimediaSearcherMetaInfo implements SearcherMetaInfo<WikimediaSearcher, WebImage> {
        @Override
        public String getSearcherName() {
            return SEARCHER_NAME;
        }

        @Override
        public String getSearcherId() {
            return "wikimedia";
        }

        @Override
        public Class<WebImage> getResultType() {
            return WebImage.class;
        }

        @Override
        public List<ConfigurationOption<?>> getConfigurationOptions() {
            return Collections.emptyList();
        }

        @Override
        public WikimediaSearcher create(Map<ConfigurationOption<?>, ?> config) {
            return new WikimediaSearcher();
        }
    }

    public WikimediaSearcher() {

    }

    public WikimediaSearcher(int defaultResultCount) {
        super();
        this.defaultResultCount = defaultResultCount;
    }

    @Override
    public List<WebImage> search(String query, int resultCount, Language language) throws SearcherException {
        List<WebImage> webImages = new ArrayList<>();

        //        resultCount = defaultResultCount == null ? resultCount : defaultResultCount;
        String url = "https://en.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&piprop=original&titles=" + UrlHelper.encodeParameter(
                StringHelper.upperCaseFirstLetters(query));
        JsonObject response = new DocumentRetriever().tryGetJsonObject(url);
        try {
            JsonObject pagesJso = response.tryQueryJsonObject("query/pages");
            String firstKey = CollectionHelper.getFirst(pagesJso.keySet());
            String imageUrl = pagesJso.tryQueryString(firstKey + "/original/source");

            BasicWebImage.Builder builder = new BasicWebImage.Builder();
            builder.setAdditionalData("id", firstKey);
            builder.setUrl(imageUrl);
            builder.setImageUrl(imageUrl);
            builder.setTitle(pagesJso.tryQueryString(firstKey + "/title"));
            builder.setWidth(pagesJso.tryQueryInt(firstKey + "/original/width"));
            builder.setHeight(pagesJso.tryQueryInt(firstKey + "/original/height"));
            builder.setImageType(ImageType.UNKNOWN);
            builder.setLicense(License.FREE);
            webImages.add(builder.create());
        } catch (Exception e) {
            throw new SearcherException(e);
        }
        return webImages;
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    public static void main(String[] args) throws SearcherException {
        List<WebImage> results = new WikimediaSearcher().search("socrates", 10, Language.ENGLISH);
        CollectionHelper.print(results);
    }
}
