package ws.palladian.extraction.entity.tagger;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * <p>
 * The Open Calais service for Named Entity Recognition. This class uses the Open Calais API and therefore requires the
 * application to have access to the Internet.
 * </p>
 * 
 * <p>
 * Open Calais can recognize the following entities:<br>
 * <ul>
 * <li>Anniversary</li>
 * <li>City</li>
 * <li>Company</li>
 * <li>Continent</li>
 * <li>Country</li>
 * <li>Currency</li>
 * <li>EmailAddress</li>
 * <li>EntertainmentAwardEvent</li>
 * <li>Facility</li>
 * <li>FaxNumber</li>
 * <li>Holiday</li>
 * <li>IndustryTerm</li>
 * <li>MarketIndex</li>
 * <li>MedicalCondition</li>
 * <li>MedicalTreatment</li>
 * <li>Movie</li>
 * <li>MusicAlbum</li>
 * <li>MusicGroup</li>
 * <li>NaturalFeature</li>
 * <li>OperatingSystem</li>
 * <li>Organization</li>
 * <li>Person</li>
 * <li>PhoneNumber</li>
 * <li>PoliticalEvent</li>
 * <li>Position</li>
 * <li>Product</li>
 * <li>ProgrammingLanguage</li>
 * <li>ProvinceOrState</li>
 * <li>PublishedMedium</li>
 * <li>RadioProgram</li>
 * <li>RadioStation</li>
 * <li>Region</li>
 * <li>SportsEvent</li>
 * <li>SportsGame</li>
 * <li>SportsLeague</li>
 * <li>Technology</li>
 * <li>TVShow</li>
 * <li>TVStation</li>
 * <li>URL</li>
 * </ul>
 * </p>
 * 
 * @see <a
 *      href="http://www.opencalais.com/documentation/calais-web-service-api/api-metadata/entity-index-and-definitions">English
 *      Semantic Metadata: Entity/Fact/Event Definitions and Descriptions</a>
 * @author David Urbansky
 */
public class OpenCalaisNer extends NamedEntityRecognizer {
    
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenCalaisNer.class);

    /** Identifier for the API key when supplied via {@link Configuration}. */
    public static final String CONFIG_API_KEY = "api.opencalais.key";

    /** The API key for the Open Calais service. */
    private final String apiKey;

    /** The maximum number of characters allowed to send per request (actually 100,000). */
    private final int MAXIMUM_TEXT_LENGTH = 90000;

    /** The {@link HttpRetriever} is used for performing the POST requests to the API. */
    private final HttpRetriever httpRetriever;

    /**
     * <p>
     * Create a new {@link OpenCalaisNer} with an API key provided by the supplied {@link Configuration} instance.
     * </p>
     * 
     * @param configuration The configuration providing the API key via {@value #CONFIG_API_KEY}, not <code>null</code>.
     */
    public OpenCalaisNer(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    /**
     * <p>
     * Create a new {@link OpenCalaisNer} with the specified API key.
     * </p>
     * 
     * @param apiKey API key to use for connecting with OpenCalais, not <code>null</code> or empty.
     */
    public OpenCalaisNer(String apiKey) {
        Validate.notEmpty(apiKey, "API key must be given.");
        this.apiKey = apiKey;
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
    }

    @Override
    public List<Annotation> getAnnotations(String inputText) {

        Annotations<Annotation> annotations = new Annotations<Annotation>();

        List<String> textChunks = NerHelper.createSentenceChunks(inputText, MAXIMUM_TEXT_LENGTH);

        LOGGER.debug("sending " + textChunks.size() + " text chunks, total text length " + inputText.length());

        // since the offset is per chunk we need to add the offset for each new chunk to get the real position of the
        // entity in the original text
        int cumulatedOffset = 0;
        for (String textChunk : textChunks) {

            String response = null;

            try {

                HttpResult httpResult = getHttpResult(textChunk.toString());
                response = httpResult.getStringContent();

                JsonObject json = new JsonObject(response);

                for (String key : json.keySet()) {

                    JsonObject obj = json.getJsonObject(key);
                    if (obj.get("_typeGroup") != null && obj.getString("_typeGroup").equalsIgnoreCase("entities")) {

                        String entityName = obj.getString("name");
                        String entityTag = obj.getString("_type");

                        if (obj.get("instances") != null) {
                            JsonArray instances = obj.getJsonArray("instances");

                            for (int i = 0; i < instances.size(); i++) {
                                JsonObject instance = instances.getJsonObject(i);

                                // take only instances that are as long as the entity name, this way we discard
                                // co-reference resolution instances
                                if (instance.getInt("length") == entityName.length()) {
                                    int offset = instance.getInt("offset");
                                    annotations.add(new ImmutableAnnotation(cumulatedOffset + offset, entityName,
                                            entityTag));
                                }
                            }
                        }
                    }
                }

            } catch (HttpException e) {
                LOGGER.error("Error performing HTTP POST: {}", e.getMessage());
            } catch (JsonException e) {
                LOGGER.error("Could not parse the JSON response: {}, exception: {}", response, e.getMessage());
            }

            cumulatedOffset += textChunk.length();
        }

        annotations.sort();
        // CollectionHelper.print(annotations);

        return annotations;
    }

    private HttpResult getHttpResult(String inputText) throws HttpException {
        HttpRequest request = new HttpRequest(HttpMethod.POST, "http://api.opencalais.com/tag/rs/enrich");
        request.addHeader("x-calais-licenseID", apiKey);
        request.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        request.addHeader("Accept", "application/json");
        request.addParameter("content", inputText);
        request.addParameter(
                "paramsXML",
                "<c:params xmlns:c=\"http://s.opencalais.com/1/pred/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><c:processingDirectives c:contentType=\"text/raw\" c:outputFormat=\"application/json\" c:discardMetadata=\";\"></c:processingDirectives><c:userDirectives c:allowDistribution=\"true\" c:allowSearch=\"true\" c:externalID=\"calaisbridge\" c:submitter=\"calaisbridge\"></c:userDirectives><c:externalMetadata c:caller=\"GnosisFirefox\"/></c:params>");
        return httpRetriever.execute(request);
    }

    @Override
    public String getName() {
        return "OpenCalais NER";
    }

    public static void main(String[] args) {

        OpenCalaisNer tagger = new OpenCalaisNer("");

        // HOW TO USE ////
        System.out
                .println(tagger
                        .tag("John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone."));
        System.exit(0);

        // /////////////////////////// test /////////////////////////////
        EvaluationResult er = tagger.evaluate("data/datasets/ner/politician/text/testing.tsv", TaggingFormat.COLUMN);
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());
    }
}