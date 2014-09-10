package ws.palladian.extraction.entity.tagger;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * <p>
 * FISE: <i>FISE is the Furtwangen IKS Semantic Engine, created during the IKS Semantic Engine Hackathon in March 2010.
 * It implements a simple OSGi-based RESTful engine that can enhance textual content, using pluggable enhancement
 * engines. </i>
 * </p>
 * 
 * @see <a href="http://wiki.iks-project.eu/index.php/FISE">FISE project</a>
 * @see <a href="http://opensaga.org:8888/engines">Used FISE server</a>
 * @author Katja Pfeifer
 * @author Philipp Katz
 */
public class FiseNer extends NamedEntityRecognizer {
    
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FiseNer.class);

    /** The name of this {@link NamedEntityRecognizer}. */
    private static final String NER_NAME = "FISE NER";

    /** The maximum number of characters allowed to send per request (actually 100,000). */
    private final int MAXIMUM_TEXT_LENGTH = 90000;

    /** The {@link HttpRetriever} is used for performing the POST requests to the API. */
    private final HttpRetriever httpRetriever;

    /**
     * <p>
     * Create a new FISE NER.
     * </p>
     */
    public FiseNer() {
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
    }

    @Override
    public List<Annotation> getAnnotations(String inputText) {
        Annotations<Annotation> annotations = new Annotations<Annotation>();
        List<String> textChunks = NerHelper.createSentenceChunks(inputText, MAXIMUM_TEXT_LENGTH);
        LOGGER.debug("Sending {} text chunks, total text length {}", textChunks.size(), inputText.length());

        for (String textChunk : textChunks) {
            HttpResult httpResult = null;
            try {
                httpResult = getHttpResult(textChunk.toString());
                List<Annotation> annotationsChunk = parseJson(inputText, httpResult.getStringContent());
                annotations.addAll(annotationsChunk);
            } catch (HttpException e) {
                throw new IllegalStateException("Error while performing HTTP request: " + e.getMessage(), e);
            } catch (JsonException e) {
                throw new IllegalStateException("Error while parsing the result JSON: " + e.getMessage()
                        + ", JSON content was: " + httpResult.getStringContent(), e);
            }
        }
        annotations.sort();
        return annotations;
    }

    /** package-private for unit-testing. */
    static List<Annotation> parseJson(String text, String json) throws JsonException {
        Annotations<Annotation> annotations = new Annotations<Annotation>();
        JsonObject jsonObject = new JsonObject(json);

        for (String key : jsonObject.keySet()) {
            JsonObject current = jsonObject.getJsonObject(key);

            if (current.get("http://purl.org/dc/terms/type") == null) {
                continue;
            }

            String entityName = getValue(current, "http://fise.iks-project.eu/ontology/selected-text");
            String type = getValue(current, "http://purl.org/dc/terms/type");
            // double confidence = Double.valueOf(getValue(current, "http://fise.iks-project.eu/ontology/confidence"));

            // the service provides no occurrence indices, so we have to look for the annotations in the text
//            String escapedEntity = StringHelper.escapeForRegularExpression(entityName.replace(" .", "."));
//            Pattern pattern = Pattern.compile("(?<=\\s)" + escapedEntity + "(?![0-9A-Za-z])|(?<![0-9A-Za-z])"
//                    + escapedEntity + "(?=\\s)", Pattern.DOTALL);
//
//            Matcher matcher = pattern.matcher(text);
//            boolean found = false;
//            while (matcher.find()) {
//                annotations.add(new Annotation(matcher.start(), entityName, type));
//                found = true;
//            }
//            if (!found) {
//                LOGGER.warn("Could not find position for entity {} in text", entityName);
//            }
            
            List<Integer> entityOffsets = NerHelper.getEntityOffsets(text, entityName);
            if (entityOffsets.isEmpty()) {
                LOGGER.warn("Could not find position for entity {} in text", entityName);
            }
            for (Integer offset : entityOffsets) {
                annotations.add(new ImmutableAnnotation(offset, entityName, type));
            }
        }
        annotations.removeNested();
        return annotations;
    }

    private static String getValue(JsonObject json, String key) throws JsonException {
        return json.getJsonArray(key).getJsonObject(0).getString("value");
    }

    private HttpResult getHttpResult(String inputText) throws HttpException {
        HttpRequest request = new HttpRequest(HttpMethod.POST, "http://opensaga.org:8888/engines/");
        request.addHeader("Accept", "application/rdf+json");
        request.addHeader("Content-type", "text/plain");
        request.addParameter("data", inputText);
        return httpRetriever.execute(request);
    }

    @Override
    public String getName() {
        return NER_NAME;
    }

    public static void main(String[] args) {
        FiseNer tagger = new FiseNer();
        String text = "John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone.";
        System.out.println(tagger.tag(text));
    }
}
