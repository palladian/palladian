package ws.palladian.retrieval.search.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.search.WebResult;
import ws.palladian.retrieval.search.WebSearcher;

public abstract class BaseHakiaSearcher extends BaseWebSearcher implements WebSearcher {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(BaseHakiaSearcher.class);

    private static final AtomicInteger requestCount = new AtomicInteger();

    private final String apiKey;
    
    public BaseHakiaSearcher() {
        super();
        ConfigHolder configHolder = ConfigHolder.getInstance();
        PropertiesConfiguration config = configHolder.getConfig();
        this.apiKey = config.getString("api.hakia.key");
    }

    public BaseHakiaSearcher(String apiKey) {
        super();
        this.apiKey = apiKey;
    }

    @Override
    public List<WebResult> search(String query) {

        List<WebResult> webresults = new ArrayList<WebResult>();
        Document searchResult = null;

        // query hakia for search engine results
        try {

            String url = getEndpoint() + "&search.pid=" + apiKey + "&search.query=" + query
                    + "&search.language=en&search.numberofresult=" + getResultCount();
            searchResult = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url);
            LOGGER.debug("Search Results for " + query + ":" + url);
        } catch (SAXException e1) {
            LOGGER.error("hakia", e1);
        } catch (IOException e1) {
            LOGGER.error("hakia", e1);
        } catch (ParserConfigurationException e1) {
            LOGGER.error("hakia", e1);
        }

        // create an xpath to grab the returned urls
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        XPathExpression expr;

        try {

            LOGGER.debug(searchResult);
            expr = xpath.compile("//Result");

            Object result = expr.evaluate(searchResult, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            LOGGER.debug("URL Nodes: " + nodes.getLength());

            int rank = 1;
            int grabSize = Math.min(nodes.getLength(), getResultCount());

            for (int i = 0; i < grabSize; i++) {
                Node nodeResult = nodes.item(i);

                String title = XPathHelper.getChildNode(nodeResult, "Title").getTextContent();
                String summary = XPathHelper.getChildNode(nodeResult, "Paragraph").getTextContent();
                String date = "";
                Node dateNode = XPathHelper.getChildNode(nodeResult, "Date");
                if (dateNode != null) {
                    date = dateNode.getTextContent();
                }
                String currentURL = XPathHelper.getChildNode(nodeResult, "Url").getTextContent();

                WebResult webresult = new WebResult(currentURL, title, summary, date);
                rank++;

                LOGGER.debug("hakia retrieved url " + currentURL);
                webresults.add(webresult);
            }

        } catch (XPathExpressionException e) {
            LOGGER.error(e);
        } catch (DOMException e) {
            LOGGER.error(e);
        }

        requestCount.incrementAndGet();
        return webresults;
    }

    protected abstract String getEndpoint();

}
