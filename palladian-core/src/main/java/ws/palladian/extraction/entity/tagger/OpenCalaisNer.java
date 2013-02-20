package ws.palladian.extraction.entity.tagger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.collection.MapBuilder;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;

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
        setName("OpenCalais NER");
        this.apiKey = apiKey;
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
    }

    @Override
    public String getModelFileEnding() {
        LOGGER.warn(getName() + " does not support loading models, therefore we don't know the file ending");
        return "";
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        LOGGER.warn(getName() + " does not support loading models, therefore we don't know the file ending");
        return false;
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {
        LOGGER.warn(getName() + " does not support training");
        return false;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        LOGGER.warn(getName() + " does not support loading models");
        return false;
    }

    @Override
    public Annotations getAnnotations(String inputText) {
        return getAnnotations(inputText, "");
    }

    @Override
    public Annotations getAnnotations(String inputText, String configModelFilePath) {

        Annotations annotations = new Annotations();

        // we need to build chunks of texts because we can not send very long texts at once to open calais
        List<StringBuilder> textChunks = new ArrayList<StringBuilder>();
        if (inputText.length() > MAXIMUM_TEXT_LENGTH) {
            List<String> sentences = Tokenizer.getSentences(inputText);
            StringBuilder currentTextChunk = new StringBuilder();
            for (String sentence : sentences) {

                if (currentTextChunk.length() + sentence.length() > MAXIMUM_TEXT_LENGTH) {
                    textChunks.add(currentTextChunk);
                    currentTextChunk = new StringBuilder();
                }

                currentTextChunk.append(sentence).append(" ");
            }
            textChunks.add(currentTextChunk);
        } else {
            textChunks.add(new StringBuilder(inputText));
        }

        LOGGER.debug("sending " + textChunks.size() + " text chunks, total text length " + inputText.length());

        // since the offset is per chunk we need to add the offset for each new chunk to get the real position of the
        // entity in the original text
        int cumulatedOffset = 0;
        for (StringBuilder textChunk : textChunks) {

            try {

                HttpResult httpResult = getHttpResult(textChunk.toString());
                String response = HttpHelper.getStringContent(httpResult);

                JSONObject json = new JSONObject(response);

                @SuppressWarnings("unchecked")
                Iterator<String> it = json.keys();

                while (it.hasNext()) {
                    String key = it.next();

                    JSONObject obj = json.getJSONObject(key);
                    if (obj.has("_typeGroup") && obj.getString("_typeGroup").equalsIgnoreCase("entities")) {

                        String entityName = obj.getString("name");
                        String entityTag = obj.getString("_type");

                        if (obj.has("instances")) {
                            JSONArray instances = obj.getJSONArray("instances");

                            for (int i = 0; i < instances.length(); i++) {
                                JSONObject instance = instances.getJSONObject(i);

                                // take only instances that are as long as the entity name, this way we discard
                                // co-reference resolution instances
                                if (instance.getInt("length") == entityName.length()) {
                                    int offset = instance.getInt("offset");
                                    annotations.add(new Annotation(cumulatedOffset + offset, entityName, entityTag));
                                }
                            }
                        }
                    }
                }

            } catch (JSONException e) {
                LOGGER.error(getName() + " could not parse json, " + e.getMessage());
            } catch (HttpException e) {
                LOGGER.error(getName() + " error performing HTTP POST, " + e.getMessage());
            }

            cumulatedOffset += textChunk.length();
        }

        annotations.sort();
        // CollectionHelper.print(annotations);

        return annotations;
    }

    private HttpResult getHttpResult(String inputText) throws HttpException {

        Map<String, String> headers = new MapBuilder<String, String>().add("x-calais-licenseID", apiKey)
                .add("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .add("Accept", "application/json");

        Map<String, String> content = new MapBuilder<String, String>()
                .add("content", inputText)
                .add("paramsXML",
                        "<c:params xmlns:c=\"http://s.opencalais.com/1/pred/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><c:processingDirectives c:contentType=\"text/raw\" c:outputFormat=\"application/json\" c:discardMetadata=\";\"></c:processingDirectives><c:userDirectives c:allowDistribution=\"true\" c:allowSearch=\"true\" c:externalID=\"calaisbridge\" c:submitter=\"calaisbridge\"></c:userDirectives><c:externalMetadata c:caller=\"GnosisFirefox\"/></c:params>");

        return httpRetriever.httpPost("http://api.opencalais.com/tag/rs/enrich", headers, content);
    }

    @SuppressWarnings("static-access")
    public static void main(String[] args) {

        OpenCalaisNer tagger = new OpenCalaisNer(ConfigHolder.getInstance().getConfig());

        if (args.length > 0) {

            Options options = new Options();
            options.addOption(OptionBuilder.withLongOpt("inputText").withDescription("the text that should be tagged")
                    .hasArg().withArgName("text").withType(String.class).create());
            options.addOption(OptionBuilder.withLongOpt("outputFile")
                    .withDescription("the path and name of the file where the tagged text should be saved to").hasArg()
                    .withArgName("text").withType(String.class).create());

            HelpFormatter formatter = new HelpFormatter();

            CommandLineParser parser = new PosixParser();
            CommandLine cmd = null;
            try {
                cmd = parser.parse(options, args);

                String taggedText = tagger.tag(cmd.getOptionValue("inputText"));

                if (cmd.hasOption("outputFile")) {
                    FileHelper.writeToFile(cmd.getOptionValue("outputFile"), taggedText);
                } else {
                    System.out.println("No output file given so tagged text will be printed to the console:");
                    System.out.println(taggedText);
                }

            } catch (ParseException e) {
                LOGGER.debug("Command line arguments could not be parsed!");
                formatter.printHelp("FeedChecker", options);
            }

        }

        // HOW TO USE ////
        System.out
                .println(tagger
                        .tag("John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone."));
        System.exit(0);

        // /////////////////////////// test /////////////////////////////
        EvaluationResult er = tagger
                .evaluate("data/datasets/ner/politician/text/testing.tsv", "", TaggingFormat.COLUMN);
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());
    }
}