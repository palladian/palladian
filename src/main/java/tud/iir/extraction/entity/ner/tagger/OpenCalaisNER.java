package tud.iir.extraction.entity.ner.tagger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tud.iir.extraction.entity.ner.Annotation;
import tud.iir.extraction.entity.ner.Annotations;
import tud.iir.extraction.entity.ner.NamedEntityRecognizer;
import tud.iir.extraction.entity.ner.TaggingFormat;
import tud.iir.extraction.entity.ner.evaluation.EvaluationResult;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.Tokenizer;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.web.HTTPPoster;

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
 * <p>
 * See also <a
 * href="http://www.opencalais.com/documentation/calais-web-service-api/api-metadata/entity-index-and-definitions"
 * >http://www.opencalais.com/documentation/calais-web-service-api/api-metadata/entity-index-and-definitions</a>
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class OpenCalaisNER extends NamedEntityRecognizer {

    /** The API key for the Open Calais service. */
    private final String API_KEY;

    /** The maximum number of characters allowed to send per request (actually 100,000). */
    private final int MAXIMUM_TEXT_LENGTH = 90000;

    public OpenCalaisNER() {
        setName("OpenCalais NER");

        PropertiesConfiguration config = null;

        try {
            config = new PropertiesConfiguration("config/apikeys.conf");
        } catch (ConfigurationException e) {
            LOGGER.error("could not get api key from config/apikeys.conf, " + e.getMessage());
        }

        if (config != null) {
            API_KEY = config.getString("opencalais.api.key");
        } else {
            API_KEY = "";
        }
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
        return getAnnotations(inputText,"");
    }

    @Override
    public Annotations getAnnotations(String inputText, String configModelFilePath) {

        Annotations annotations = new Annotations();

     // we need to build chunks of texts because we can not send very long texts at once to open calais
        List<String> sentences = Tokenizer.getSentences(inputText);
        List<StringBuilder> textChunks = new ArrayList<StringBuilder>();
        StringBuilder currentTextChunk = new StringBuilder();
        for (String sentence : sentences) {

            if (currentTextChunk.length() + sentence.length() > MAXIMUM_TEXT_LENGTH) {
                textChunks.add(currentTextChunk);
                currentTextChunk = new StringBuilder();
            }

            currentTextChunk.append(sentence);
        }
        textChunks.add(currentTextChunk);

        LOGGER.debug("sending " + textChunks.size() + " text chunks, total text length " + inputText.length());

        // since the offset is per chunk we need to add the offset for each new chunk to get the real position of the
        // entity in the original text
        int cumulatedOffset = 0;
        for (StringBuilder textChunk : textChunks) {

            try {
                // use get
                // Crawler c = new Crawler();
                // String parameters =
                // "<c:params xmlns:c=\"http://s.opencalais.com/1/pred/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><c:processingDirectives c:contentType=\"text/raw\" c:outputFormat=\"Application/JSON\" c:discardMetadata=\";\"></c:processingDirectives><c:userDirectives c:allowDistribution=\"true\" c:allowSearch=\"true\" c:externalID=\"calaisbridge\" c:submitter=\"calaisbridge\"></c:userDirectives><c:externalMetadata c:caller=\"GnosisFirefox\"/></c:params>";
                // String restCall = "http://api.opencalais.com/enlighten/rest/?licenseID=" + API_KEY + "&content="
                // + inputText + "&paramsXML=" + URLEncoder.encode(parameters, "UTF-8");
                // // System.out.println(restCall);
                // JSONObject json = c.getJSONDocument(restCall);

                PostMethod pm = createPostMethod(textChunk.toString());

                HTTPPoster poster = new HTTPPoster();
                String response = poster.handleRequest(pm);

                JSONObject json = new JSONObject(response);

                @SuppressWarnings("unchecked")
                Iterator<String> it = json.keys();

                while (it.hasNext()) {
                    String key = it.next();

                    JSONObject obj = json.getJSONObject(key);
                    if (obj.has("_typeGroup") && obj.getString("_typeGroup").equalsIgnoreCase("entities")) {

                        String entityName = obj.getString("name");
                        Entity namedEntity = new Entity(entityName, new Concept(obj.getString("_type")));

                        // recognizedEntities.add(namedEntity);

                        if (obj.has("instances")) {
                            JSONArray instances = obj.getJSONArray("instances");

                            for (int i = 0; i < instances.length(); i++) {
                                JSONObject instance = instances.getJSONObject(i);

                                // take only instances that are as long as the entity name, this way we discard
                                // co-reference
                                // resolution instances
                                if (instance.getInt("length") == entityName.length()) {
                                    int offset = instance.getInt("offset");

                                    Annotation annotation = new Annotation(cumulatedOffset + offset,
                                            namedEntity.getName(), namedEntity
                                            .getConcept().getName());
                                    annotations.add(annotation);
                                }
                            }

                        }

                    }

                }

            } catch (JSONException e) {
                LOGGER.error(getName() + " could not parse json, " + e.getMessage());
            }
            // catch (UnsupportedEncodingException e) {
            // LOGGER.error(getName() + " could not encode url, " + e.getMessage());
            // }

            cumulatedOffset += textChunk.length();
        }

        annotations.sort();
        CollectionHelper.print(annotations);

        return annotations;
    }

    private PostMethod createPostMethod(String inputText) {

        PostMethod method = new PostMethod("http://api.opencalais.com/tag/rs/enrich");

        // set mandatory parameters
        method.setRequestHeader("x-calais-licenseID", API_KEY);

        // set input content type
        method.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        // set response/output format
        method.setRequestHeader("Accept", "application/json");

        try {
            String paramsXML = "<c:params xmlns:c=\"http://s.opencalais.com/1/pred/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><c:processingDirectives c:contentType=\"text/raw\" c:outputFormat=\"application/json\" c:discardMetadata=\";\"></c:processingDirectives><c:userDirectives c:allowDistribution=\"true\" c:allowSearch=\"true\" c:externalID=\"calaisbridge\" c:submitter=\"calaisbridge\"></c:userDirectives><c:externalMetadata c:caller=\"GnosisFirefox\"/></c:params>";
            method.setRequestEntity(new StringRequestEntity("content=" + URLEncoder.encode(inputText, "UTF-8")
                    + "&paramsXML=" + URLEncoder.encode(paramsXML, "UTF-8"),
                    "text/raw", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("encoding is not supported, " + e.getMessage());
        }

        return method;
    }

    /**
     * Tag the input text. Open Calais does not require to specify a model.
     * 
     * @param inputText The text to be tagged.
     * @return The tagged text.
     */
    @Override
    public String tag(String inputText) {
        return super.tag(inputText);
    }

    @SuppressWarnings("static-access")
    public static void main(String[] args) {

        OpenCalaisNER tagger = new OpenCalaisNER();

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

        // // HOW TO USE ////
        // System.out.println(ot.tag("The world's largest maker of solar inverters announced Monday that it will locate its first North American manufacturing plant in Denver."));
        // System.out
        // .println(ot
        // .tag("John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone."));

        // /////////////////////////// test /////////////////////////////
        EvaluationResult er = tagger
                .evaluate("data/datasets/ner/politician/text/testing.tsv", "", TaggingFormat.COLUMN);
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());
    }
}