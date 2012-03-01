package ws.palladian.preprocessing.nlp.ner.tagger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
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
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.helper.nlp.Tokenizer;
import ws.palladian.preprocessing.nlp.ner.Annotation;
import ws.palladian.preprocessing.nlp.ner.Annotations;
import ws.palladian.preprocessing.nlp.ner.Entity;
import ws.palladian.preprocessing.nlp.ner.NamedEntityRecognizer;
import ws.palladian.preprocessing.nlp.ner.TaggingFormat;
import ws.palladian.preprocessing.nlp.ner.evaluation.EvaluationResult;
import ws.palladian.preprocessing.nlp.ner.tagger.AlchemyNER;
import ws.palladian.retrieval.HTTPPoster;

public class AlchemyNER extends NamedEntityRecognizer {

	/** The API key for the Alchemy API service. */
	private final String apiKey;

	/**
	 * The maximum number of characters allowed to send per request (actually
	 * 150,000).
	 */
	private final int MAXIMUM_TEXT_LENGTH = 140000;

	/**
	 * Constructor. Uses the API key from the configuration, at place
	 * "api.alchemy.key"
	 */
	public AlchemyNER() {
		this("");
	}

	/**
	 * This constructor should be used to specify an explicit API key.
	 * 
	 * @param apiKey
	 *            API key to use for connecting with Alchemy API
	 */
	public AlchemyNER(String apiKey) {
		setName("Alchemy API NER");

		PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();

		if (!apiKey.equals("")) {
			this.apiKey = apiKey;
		} else if (config != null) {
			this.apiKey = config.getString("api.alchemy.key");
		} else {
			this.apiKey = "";
		}
	}

	@Override
	public String getModelFileEnding() {
		LOGGER.warn(getName()
				+ " does not support loading models, therefore we don't know the file ending");
		return "";
	}

	@Override
	public boolean setsModelFileEndingAutomatically() {
		LOGGER.warn(getName()
				+ " does not support loading models, therefore we don't know the file ending");
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
	public Annotations getAnnotations(String inputText,
			String configModelFilePath) {

		Annotations annotations = new Annotations();

		// we need to build chunks of texts because we can not send very long
		// texts at once to open calais
		List<String> sentences = Tokenizer.getSentences(inputText);
		List<StringBuilder> textChunks = new ArrayList<StringBuilder>();
		StringBuilder currentTextChunk = new StringBuilder();
		for (String sentence : sentences) {

			if (currentTextChunk.length() + sentence.length() + 1 > MAXIMUM_TEXT_LENGTH) {
				textChunks.add(currentTextChunk);
				currentTextChunk = new StringBuilder();
			}

			currentTextChunk.append(" " + sentence);
		}
		textChunks.add(currentTextChunk);

		LOGGER.debug("sending " + textChunks.size()
				+ " text chunks, total text length " + inputText.length());

		Set<String> checkedEntities = new HashSet<String>();
		for (StringBuilder textChunk : textChunks) {

			// use get
			// Crawler c = new Crawler();
			// JSONObject json = c
			// .getJSONDocument("http://access.alchemyapi.com/calls/text/TextGetRankedNamedEntities?apikey="
			// + apiKey
			// + "&text=" + inputText + "&disambiguate=0&outputMode=json");

			try {

				HttpPost pm = createPostMethod(textChunk.toString());

				HTTPPoster poster = new HTTPPoster();
				String response = poster.handleRequest(pm);
				if (response.contains("daily-transaction-limit-exceeded")) {
					System.out.println("--- LIMIT EXCEEDED ---");
					break;
				}
				JSONObject json = new JSONObject(response);

				JSONArray entities = json.getJSONArray("entities");
				for (int i = 0; i < entities.length(); i++) {

					JSONObject entity = (JSONObject) entities.get(i);

					String entityName = entity.getString("text");
					Entity namedEntity = new Entity(entityName,
							entity.getString("type"));

					List<String> subTypeList = new LinkedList<String>();
					if (entity.has("disambiguated")) {
						JSONObject disambiguated = entity
								.getJSONObject("disambiguated");
						JSONArray subTypes = disambiguated
								.getJSONArray("subType");

						for (int j = 0; j < subTypes.length(); j++) {
							subTypeList.add((String) subTypes.get(j));
						}
					}

					// skip entities that have been processed already
					if (!checkedEntities.add(entityName)) {
						continue;
					}

					// recognizedEntities.add(namedEntity);

					// get locations of named entity
					String escapedEntity = StringHelper
							.escapeForRegularExpression(entityName);
					Pattern pattern = Pattern.compile("(?<=\\s)"
							+ escapedEntity
							+ "(?![0-9A-Za-z])|(?<![0-9A-Za-z])"
							+ escapedEntity + "(?=\\s)", Pattern.DOTALL);

					Matcher matcher = pattern.matcher(inputText);
					while (matcher.find()) {

						int offset = matcher.start();

						Annotation annotation = new Annotation(offset,
								namedEntity.getName(), namedEntity.getTagName());
						annotation.addSubTypes(subTypeList);
						annotations.add(annotation);
					}

				}
			} catch (JSONException e) {
				LOGGER.error(getName() + " could not parse json, "
						+ e.getMessage());
			}
		}

		annotations.sort();
		CollectionHelper.print(annotations);

		return annotations;
	}

	private HttpPost createPostMethod(String inputText) {

		HttpPost method = new HttpPost(
				"http://access.alchemyapi.com/calls/text/TextGetRankedNamedEntities");

		// set input content type
		method.setHeader("Content-Type",
				"application/x-www-form-urlencoded; charset=UTF-8");

		// set response/output format
		method.setHeader("Accept", "application/json");

		try {
			method.setEntity(new StringEntity("text="
					+ URLEncoder.encode(inputText, "UTF-8") + "&apikey="
					+ URLEncoder.encode(apiKey, "UTF-8")
					+ "&outputMode=json&disambiguate=1", "text/raw", "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("encoding is not supported, " + e.getMessage());
		}

		return method;
	}

	/**
	 * Tag the input text. Alchemy API does not require to specify a model.
	 * 
	 * @param inputText
	 *            The text to be tagged.
	 * @return The tagged text.
	 */
	@Override
	public String tag(String inputText) {
		return super.tag(inputText);
	}

	@SuppressWarnings("static-access")
	public static void main(String[] args) {

		AlchemyNER tagger = new AlchemyNER();

		if (args.length > 0) {

			Options options = new Options();
			options.addOption(OptionBuilder.withLongOpt("inputText")
					.withDescription("the text that should be tagged").hasArg()
					.withArgName("text").withType(String.class).create());
			options.addOption(OptionBuilder
					.withLongOpt("outputFile")
					.withDescription(
							"the path and name of the file where the tagged text should be saved to")
					.hasArg().withArgName("text").withType(String.class)
					.create());

			HelpFormatter formatter = new HelpFormatter();

			CommandLineParser parser = new PosixParser();
			CommandLine cmd = null;
			try {
				cmd = parser.parse(options, args);

				String taggedText = tagger.tag(cmd.getOptionValue("inputText"));

				if (cmd.hasOption("outputFile")) {
					FileHelper.writeToFile(cmd.getOptionValue("outputFile"),
							taggedText);
				} else {
					System.out
							.println("No output file given so tagged text will be printed to the console:");
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
		// .tag("The world's largest maker of solar inverters announced Monday that it will locate its first North American manufacturing plant in Denver. Some of them are also made in Salt Lake City or Cameron."));
		// tagger.tag("John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone.");

		// /////////////////////////// test /////////////////////////////
		EvaluationResult er = tagger.evaluate(
				"data/datasets/ner/politician/text/testing.tsv", "",
				TaggingFormat.COLUMN);
		System.out.println(er.getMUCResultsReadable());
		System.out.println(er.getExactMatchResultsReadable());

	}

}
