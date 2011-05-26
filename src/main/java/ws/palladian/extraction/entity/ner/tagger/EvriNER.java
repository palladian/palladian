package ws.palladian.extraction.entity.ner.tagger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.extraction.entity.ner.Annotation;
import ws.palladian.extraction.entity.ner.Annotations;
import ws.palladian.extraction.entity.ner.NamedEntityRecognizer;
import ws.palladian.extraction.entity.ner.TaggingFormat;
import ws.palladian.extraction.entity.ner.evaluation.EvaluationResult;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.helper.nlp.Tokenizer;
import ws.palladian.retrieval.HTTPPoster;

/**
 * <p>
 * The Evri service for Named Entity Recognition. This class uses the Evri API and therefore requires the application to
 * have access to the Internet.
 * </p>
 * 
 * <p>
 * This recognizer can only detect which entities are in the given text, not, however, where exactly they are.
 * Therefore, we can not tag a text using Evri.
 * </p>
 * 
 * <p>
 * Evri can recognize the following entities:<br>
 * <ul>
 * <li>Person</li>
 * <li>Location</li>
 * <li>Concept</li>
 * <li>Product</li>
 * <li>Organization</li>
 * <li>Event</li>
 * </ul>
 * </p>
 * 
 * <p>
 * See also <a href="http://www.evri.com/developer/rest">http://www.evri.com/developer/rest</a>
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class EvriNER extends NamedEntityRecognizer {

    /** The maximum number of characters allowed to send per request (actually ???). */
    private final int MAXIMUM_TEXT_LENGTH = 50000;

    public EvriNER() {
        setName("Evri NER");
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

        Set<String> checkedEntities = new HashSet<String>();
        for (StringBuilder textChunk : textChunks) {

            try {

                // use get
                // Crawler c = new Crawler();
                // String restCall = "http://api.evri.com/v1/media/entities.json?uri=http://www.webknox.com&text=" +
                // inputText
                // + "&appId=evri.com-restdoc";
                // System.out.println(restCall);
                // JSONObject json = c.getJSONDocument(restCall);

                HttpPost pm = createPostMethod(textChunk.toString());

                HTTPPoster poster = new HTTPPoster();
                String response = poster.handleRequest(pm);

                JSONObject json = new JSONObject(response);

                JSONArray entities = new JSONArray();

                // JSONObject e1 = new JSONObject();
                // e1.put("@href", "/person/sdafas");
                // Map<String, String> m = new HashMap<String, String>();
                // m.put("$", "Milhouse");
                // e1.put("name", new JSONObject(m));
                // entities.put(e1);
                // JSONObject e2 = new JSONObject();
                // e2.put("@href", "/politician/sdafas");
                // m = new HashMap<String, String>();
                // m.put("$", "Richard Milhouse Nixon");
                // e2.put("name", new JSONObject(m));
                // entities.put(e2);

                // try to get an array of entities, if it was only one found, get the one as json object instead
                try {
                    entities = json.getJSONObject("evriThing").getJSONObject("graph").getJSONObject("entities")
                            .getJSONArray("entity");
                } catch (JSONException e) {
                    LOGGER.debug(getName() + " only one entity found, " + e.getMessage());

                    JSONObject singleEntity = json.getJSONObject("evriThing").getJSONObject("graph")
                            .getJSONObject("entities").getJSONObject("entity");
                    entities.put(singleEntity);
                }

                for (int i = 0; i < entities.length(); i++) {
                    JSONObject entity = (JSONObject) entities.get(i);

                    String concept = "";
                    String entityName = entity.getJSONObject("name").getString("$");

                    // skip entities that have been processed already
                    if (!checkedEntities.add(entityName)) {
                        continue;
                    }

                    if (entity.has("facets")) {
                        JSONObject facets = entity.getJSONObject("facets");

                        // dirty workaround, as the JSON sometimes returns facet as Array, sometimes as Object
                        try {
                            concept = facets.getJSONObject("facet").getString("$");
                        } catch (Exception e) {
                            JSONArray array = facets.getJSONArray("facet");
                            if (array.length() > 0) {
                                concept = array.getString(0);
                            }
                        }

                    } else {
                        String href = entity.getString("@href");
                        concept = StringHelper.upperCaseFirstLetter(href.substring(1, href.indexOf("/", 1)));
                    }

                    // get locations of named entity
                    String escapedEntity = StringHelper.escapeForRegularExpression(entityName);
                    Pattern pattern = Pattern.compile("(?<=\\s)" + escapedEntity + "(?![0-9A-Za-z])|(?<![0-9A-Za-z])"
                            + escapedEntity + "(?=\\s)", Pattern.DOTALL);

                    Matcher matcher = pattern.matcher(inputText);
                    while (matcher.find()) {

                        int offset = matcher.start();

                        Annotation annotation = new Annotation(offset, entityName, concept);
                        annotations.add(annotation);
                    }

                }

            } catch (JSONException e) {
                LOGGER.error(getName() + " could not parse json, " + e.getMessage());
            }

        }

        // annotations.removeNestedAnnotations();
        annotations.sort();
        CollectionHelper.print(annotations);

        FileHelper.writeToFile("data/test/ner/evriOutput.txt", tagText(inputText, annotations));

        return annotations;
    }

    private HttpPost createPostMethod(String inputText) {

        HttpPost method = new HttpPost("http://api.evri.com/v1/media/entities.json");

        // set input content type
        method.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        // set response/output format
        method.setHeader("Accept", "application/json");

        try {
            method.setEntity(new StringEntity("uri=" + URLEncoder.encode("http://www.webknox.com", "UTF-8") + "&text="
                    + URLEncoder.encode(inputText, "UTF-8") + "&appId=evri.com-restdoc", "text/raw", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("encoding is not supported, " + e.getMessage());
        }

        return method;
    }

    /**
     * Tag the input text. Evri does not require to specify a model.
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

        EvriNER tagger = new EvriNER();

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
        // System.out
        // .println(tagger
        // .tag("The world's largest maker of solar inverters announced Monday that it will locate its first North American manufacturing plant in Denver."));
        // System.out
        // .println(tagger
        // .tag("John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone."));

        // System.out
        // .println(tagger
        // .tag("The Janata Party is some kind of Party. Abraham Licoln is a politician. Richard Milhouse Nixon is a politician too."));

        // /////////////////////////// test /////////////////////////////
        EvaluationResult er = tagger
                .evaluate("data/datasets/ner/politician/text/testing.tsv", "", TaggingFormat.COLUMN);
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());
    }
}