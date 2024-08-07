package ws.palladian.retrieval.search.web;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.persistence.ParserException;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.SearcherException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Searcher for <a href="http://www.etools.ch">ETools</a> meta search engine. The partner ID is taken from the Carrot2
 * workbench, not for public usage therefore.
 * </p>
 *
 * @author Philipp Katz
 */
public final class EToolsSearcher extends AbstractSearcher<WebContent> {

    public static final class EToolsSearcherMetaInfo implements SearcherMetaInfo<EToolsSearcher, WebContent> {
        @Override
        public String getSearcherName() {
            return SEARCHER_NAME;
        }

        @Override
        public String getSearcherId() {
            return "etools";
        }

        @Override
        public Class<WebContent> getResultType() {
            return WebContent.class;
        }

        @Override
        public List<ConfigurationOption<?>> getConfigurationOptions() {
            return Collections.emptyList();
        }

        @Override
        public EToolsSearcher create(Map<ConfigurationOption<?>, ?> config) {
            return new EToolsSearcher();
        }

        @Override
        public boolean isDeprecated() {
            return true;
        }

        @Override
        public String getSearcherDocumentationUrl() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getSearcherDescription() {
            // TODO Auto-generated method stub
            return null;
        }
    }

    private static final String SEARCHER_NAME = "ETools";

    private static final DocumentParser XML_PARSER = ParserFactory.createXmlParser();

    private static final String PARTNER_ID = "Carrot2";

    private final HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @Override
    public List<WebContent> search(String query, int resultCount, Language language) throws SearcherException {
        String requestUrl = buildUrl(query, resultCount, language);
        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(requestUrl);
        } catch (HttpException e) {
            throw new SearcherException(e);
        }

        Document document;
        try {
            document = XML_PARSER.parse(httpResult);
        } catch (ParserException e) {
            throw new SearcherException(e);
        }

        List<Node> records = XPathHelper.getNodes(document, "/result/records/record");
        List<WebContent> results = new ArrayList<>();

        for (Node record : records) {
            BasicWebContent.Builder builder = new BasicWebContent.Builder();
            builder.setTitle(XPathHelper.getNode(record, "./title").getTextContent());
            builder.setSummary(XPathHelper.getNode(record, "./text").getTextContent());
            builder.setUrl(XPathHelper.getNode(record, "./url").getTextContent());
            results.add(builder.create());
        }
        return results;
    }

    private String buildUrl(String query, int numResults, Language language) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("https://www.etools.ch/partnerSearch.do");
        stringBuilder.append("?partner=").append(PARTNER_ID);
        stringBuilder.append("&query=").append(UrlHelper.encodeParameter(query));
        stringBuilder.append("&maxRecords=").append(numResults);

        if (language != null) {
            stringBuilder.append("&language=");
            switch (language) {
                case GERMAN:
                    stringBuilder.append("DE");
                    break;
                case ENGLISH:
                    stringBuilder.append("EN");
                    break;
                default:
                    stringBuilder.append("web");
                    break;
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean isDeprecated() {
        return true;
    }

}
