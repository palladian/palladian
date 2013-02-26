package ws.palladian.extraction.entity.tagger;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.helper.collection.MapBuilder;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * <p>
 * Named Entity Recognizer based on <a href="http://wikimeta.com">Wikimeta</a>. Supported entity types can be found <a
 * href="http://wikimeta.com/forum/viewtopic.php?f=9&t=2&sid=6f949629baf4149f69b12308d3975a86">here</a>.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://wikimeta.com/api.html">API documentation</a>
 */
public final class WikimetaNer extends NamedEntityRecognizer {

    /** The API key for accessing the service. */
    private final String apiKey;

    /** For contacting the web service. */
    private final HttpRetriever httpRetriever;

    /** For parsing the XML API response. */
    private final DocumentParser xmlParser;

    /**
     * <p>
     * Initialize a new Wikimedia Named Entity Recognizer with the specified API key.
     * </p>
     * 
     * @param apiKey The required API key for accessing the service, not <code>null</code> or empty.
     */
    public WikimetaNer(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be provided.");

        this.apiKey = apiKey;
        this.httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        this.xmlParser = ParserFactory.createXmlParser();
    }

    /** Package-private. Intended for unit-testing only. */
    WikimetaNer() {
        this.apiKey = null;
        this.httpRetriever = null;
        this.xmlParser = ParserFactory.createXmlParser();
    }

    @Override
    public Annotations getAnnotations(String inputText) {
        Annotations annotations;
        try {
            HttpResult httpResult = performRequest(inputText);
            String resultString = HttpHelper.getStringContent(httpResult);
            if (resultString.contains("<error msg=")) {
                throw new IllegalStateException("Error from the web service: " + resultString);
            }
            annotations = parseXml(new InputSource(new ByteArrayInputStream(httpResult.getContent())), inputText);
        } catch (HttpException e) {
            throw new IllegalStateException("Encountered HttpException: " + e.getMessage(), e);
        } catch (ParserException e) {
            throw new IllegalStateException("Encountered ParseException: " + e.getMessage(), e);
        }
        return annotations;
    }

    private HttpResult performRequest(String inputText) throws HttpException {
        Map<String, String> headers = new MapBuilder<String, String>().add("Accept", "application/xml");
        Map<String, String> content = new MapBuilder<String, String>().add("contenu", inputText).add("api", apiKey)
                .add("semtag", "0").add("lng", "EN"); // hard coded English language for now
        return httpRetriever.httpPost("http://www.wikimeta.com/wapi/service", headers, content);
    }

    /** Package-private for unit-testing. */
    Annotations parseXml(InputSource inputSource, String inputText) throws ParserException {

        Annotations annotations = new Annotations();
        Document doc = xmlParser.parse(inputSource);

        List<String> tokens = getCdataContent(doc);

        // keep token positions (i.e. character index in whole text)
        List<Integer> tokenPositions = new ArrayList<Integer>();

        // the nodes from the result XML keeping the extracted NE phrases (i.e. one/more tokens)
        List<Node> extractedNodes = XPathHelper.getNodes(doc, "/wikimeta/extraction");

        // determine the starting index of the tokens
        int characterPosition = 0;
        for (String tokenLine : tokens) {
            String[] split = tokenLine.split("\t");
            if (split.length < 3) {
                throw new IllegalStateException(
                        "Error parsing the CDATA response, each line should at least contain three tab-separated items.");
            }
            String tokenValue = split[0];
            characterPosition = inputText.indexOf(tokenValue, characterPosition);
            tokenPositions.add(characterPosition);
        }

        for (Node node : extractedNodes) {
            Node neNode = XPathHelper.getNode(node, "NE");
            Node typeNode = XPathHelper.getNode(node, "type");
            Node positionNode = XPathHelper.getNode(node, "position");
            if (neNode == null || typeNode == null || positionNode == null) {
                throw new IllegalStateException(
                        "Error parsing XML. NE, type and/or position element withing extraction element was missing.");
            }
            String value = neNode.getTextContent();
            String type = typeNode.getTextContent();
            // the position value in XML is the line in the token index
            Integer tokenIndex = Integer.valueOf(positionNode.getTextContent());
            Integer tokenCharIndex = tokenPositions.get(tokenIndex);
            // the actual character index might be later
            tokenCharIndex = inputText.indexOf(value, tokenCharIndex);
            annotations.add(new Annotation(tokenCharIndex, value, type, annotations));
        }

        return annotations;
    }

    /**
     * <p>
     * Getting the content from the CDATA section in the response. This contains all tokens from the text and their
     * tags. As the CDATA is directly in the {@link Document}s root, I didn't figure out how to extract this content
     * using XPath, therefore just transform the {@link Document} to String and parse line by line.
     * </p>
     * 
     * @param document The result {@link Document} from the API.
     * @return Lines within the CDATA section.
     */
    private List<String> getCdataContent(Document document) {
        String stringRepresentation = HtmlHelper.xmlToString(document);
        LOGGER.debug("xml data:\n" + stringRepresentation);
        String[] lines = stringRepresentation.split("\n");
        List<String> items = new ArrayList<String>();
        int index;
        // we are interested in lines between "<![CDATA[" and "]]>"
        for (index = 0; index < lines.length; index++) {
            if (lines[index].startsWith("<![CDATA[")) {
                break;
            }
        }
        for (index++; index < lines.length; index++) {
            if (lines[index].startsWith("]]>")) {
                break;
            }
            items.add(lines[index]);
        }
        return items;
    }

    /** Overridden, intended for unit-testing only. */
    @Override
    protected String tagText(String inputText, Annotations annotations) {
        return super.tagText(inputText, annotations);
    }

    @Override
    public String getName() {
        return "Wikimeta NER";
    }

    public static void main(String[] args) {
        WikimetaNer ner = new WikimetaNer("useYourOwn!");
        String text = FileHelper.readFileToString("src/test/resources/NewsSampleText.txt");
        System.out.println(ner.tag(text));
    }

}
