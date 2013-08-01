package ws.palladian.extraction.entity.tagger;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.ImmutableAnnotation;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * <p>
 * Named Entity Recognizer from Digimap. According to the web page, the NER is focused on historical data, although I
 * cannot find any closer information, what "historic" actually means in this case. From the <a
 * href="http://www.digmap.eu/doku.php">web page</a>: <i>DIGMAP developed solutions for georeferenced digital libraries,
 * especially focused on historical materials and in the promoting of our cultural and scientific heritage.</i>
 * </p>
 * 
 * @see <a href="http://digimap.edina.ac.uk/digimap/home#">Digimap</a>
 * @author Katja Pfeifer
 * @author Philipp Katz
 */
public class DigmapNer extends NamedEntityRecognizer {

    /** The name of this {@link NamedEntityRecognizer}. */
    private static final String NER_NAME = "Digmap NER";

    /** Maximum size of the text chunks to send to the service. */
    private static final int MAXIMUM_TEXT_LENGTH = 10000;

    /** Mapping for the XML namespace. */
    private static final Map<String, String> NAMESPACE_MAPPING;
    static {
        NAMESPACE_MAPPING = CollectionHelper.newHashMap();
        NAMESPACE_MAPPING.put("gp", "http://www.opengis.net/gp");
    }

    /** Template for the XML query document. */
    private static final String XML_REQUEST_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" //
            + "<GetFeature xmlns=\"http://www.opengis.net/gp\" " //
            + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
            + "xmlns:xsi=\"http://www.w3.org/2000/10/XMLSchema-instance\" " //
            + "xsi:schemaLocation=\"http://www.opengis.net/gp ../gp/GetFeatureRequest.xsd http://www.opengis.net/wfs ../wfs/GetFeatureRequest.xsd\" " //
            + "wfs:outputFormat=\"GML2\">" //
            + "<wfs:Query wfs:TypeName=\"PlaceName\" /><wfs:Query wfs:TypeName=\"DateTime\" /><wfs:Query wfs:TypeName=\"People\" /><wfs:Query wfs:TypeName=\"Organizations\" />" //
            + "<Resource mime=\"text/plain\">%s</Resource>" //
            + "</GetFeature>";

    /** The {@link HttpRetriever} is used for performing the POST requests to the API. */
    private final HttpRetriever httpRetriever;

    /** For parsing the XML API response. */
    private final DocumentParser xmlParser;

    /**
     * <p>
     * Create a new Digimap NER.
     * </p>
     */
    public DigmapNer() {
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        xmlParser = ParserFactory.createXmlParser();
    }

    @Override
    public Annotations<Annotation> getAnnotations(String inputText) {

        Annotations<Annotation> annotations = new Annotations<Annotation>();

        // Digmap throws internal error, when text includes "&"
        String replacedInputText = inputText.replace("&", "+");

        List<String> textChunks = NerHelper.createSentenceChunks(replacedInputText, MAXIMUM_TEXT_LENGTH);
        LOGGER.debug("Sending {} text chunks, total text length {}", textChunks.size(), inputText.length());

        for (String textChunk : textChunks) {
            HttpResult httpResult = null;
            try {
                httpResult = getHttpResult(textChunk);
                Document doc = xmlParser.parse(httpResult);
                List<Node> entries = XPathHelper.getNodes(doc, "//gp:EntryCollection/*", NAMESPACE_MAPPING);
                for (Node entry : entries) {
                    String tag = XPathHelper.getNode(entry, "./gp:Label/text()", NAMESPACE_MAPPING).getTextContent();
                    Node startNode = XPathHelper.getNode(entry, "./gp:Ocurrence/gp:Range/@start", NAMESPACE_MAPPING);
                    Node endNode = XPathHelper.getNode(entry, "./gp:Ocurrence/gp:Range/@end", NAMESPACE_MAPPING);
                    int start = Integer.valueOf(startNode.getTextContent());
                    int end = Integer.valueOf(endNode.getTextContent());
                    String entityName = textChunk.substring(start, end);
                    annotations.add(new ImmutableAnnotation(start, entityName, tag));
                }
            } catch (HttpException e) {
                throw new IllegalStateException("Error while performing HTTP request: " + e.getMessage(), e);
            } catch (ParserException e) {
                String resultString = HttpHelper.getStringContent(httpResult);
                throw new IllegalStateException("Error while parsing the result XML: " + e.getMessage()
                        + ", XML content was: " + resultString, e);
            }
        }

        annotations.sort();
        return annotations;
    }

    private HttpResult getHttpResult(String inputText) throws HttpException {
        HttpRequest httpRequest = new HttpRequest(HttpMethod.POST, "http://geoparser.digmap.eu/geoparser-dispatch");
        httpRequest.addHeader("Accept", "application/xml");
        httpRequest.addHeader("Content-type", "application/x-www-form-urlencoded");
        httpRequest.addParameter("request", String.format(XML_REQUEST_TEMPLATE, inputText));
        httpRequest.addParameter("button", "GeoParse");
        return httpRetriever.execute(httpRequest);
    }

    @Override
    public String getName() {
        return NER_NAME;
    }

    public static void main(String[] args) {
        DigmapNer tagger = new DigmapNer();
        String text = "John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone.";
        System.out.println(tagger.tag(text));
    }
}
